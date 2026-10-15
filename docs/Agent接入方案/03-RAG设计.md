# 03 · RAG 设计

> 本方案采用 **Agentic RAG**（参考 [Agentic RAG 2026](https://sukruyusufkaya.com/en/blog/agentic-rag-langgraph-uretim-mimarisi-2026)、[Self-Correcting RAG](https://www.syuthd.com/2026/05/building-self-correcting-agentic-rag.html)），核心是：
> 1. 检索不只一次：critic 评分不通过就 refine 再检索
> 2. 答案必有引用：citation_check 强制每句话标注 doc_id
> 3. 失败透明：明示"无相关知识"，不编造

## 知识源

| 来源 | 内容 | 摄入方式 |
|------|------|---------|
| `docs/塔吊安全预测/*.md` | 塔吊 LSTM 模型说明 | 全量摄入 |
| `docs/系统接口设计文档*.md` | 后端 API 设计 | 摄入 + 元数据 `category=api` |
| `docs/建筑安全智能监控平台详细设计报告.md` | 平台架构 | 摄入 + 元数据 `category=arch` |
| `docs/4-7/*.md`, `docs/任务1-4/*.md` 等 | 任务说明、报警规则 | 摄入 |
| 数据库 `equipment_assets` | 设备资产规格 | **每日同步**（离线 ingestion） |
| 数据库 `monitor_point_alert` 历史 | 报警处理记录 | **写入时同步**（在线 ingestion） |
| 数据库 `equipment_type` | 设备类型树 | **每日同步** |
| 报警阈值配置 | `alarmUpper/Lower` | **每日同步** |
| `application.yml` 配置 | AI 配置说明 | 摄入 |
| SpringDoc OpenAPI JSON | 所有 REST API schema | 启动时全量 |

## 摄入流水线（离线）

```
    ┌────────────────┐
    │   多源数据     │
    │ md/json/db/    │
    │ openapi        │
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │   Loader       │
    │ (按源选loader) │
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │   Cleaner      │
    │ 去HTML/控制字符│
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │  Splitter      │
    │ 标题分块+长度分│
    │ chunk=512 ovl=64│
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │  Metadata      │
    │  Enricher      │
    │ doc_id/path/   │
    │ category/date  │
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │  Embedder      │
    │  中转站        │
    │  embedding API │
    └────────┬───────┘
             │
             ▼
    ┌────────────────┐
    │  ChromaDB      │
    │  collection=   │
    │  knowledge_v1  │
    └────────────────┘

    增量数据 ──► Delta Loader ──► ChromaDB
    (每日02:00)  (按 update_time)
```

## Chunking 策略

```python
# app/rag/ingestion/splitter.py
from langchain_text_splitters import (
    RecursiveCharacterTextSplitter,
    MarkdownHeaderTextSplitter,
)

# 文档类：先按 Markdown 标题分块，再按长度细分
md_splitter = MarkdownHeaderTextSplitter(headers_to_split_on=[
    ("#", "h1"), ("##", "h2"), ("###", "h3"),
])
char_splitter = RecursiveCharacterTextSplitter(
    chunk_size=512, chunk_overlap=64,
    separators=["\n\n", "\n", "。", "；", " ", ""],
)

# 配置类：按 key=value 行分块
config_splitter = RecursiveCharacterTextSplitter(chunk_size=200, chunk_overlap=0)

# 数据库记录类：每条记录一个 chunk
def db_record_chunker(record):
    return f"[{record['category']}] {json.dumps(record, ensure_ascii=False)}"
```

## 向量库选型

| 维度 | ChromaDB（开发） | Qdrant（生产可替换） |
|------|------------------|---------------------|
| 部署 | in-process，无外部依赖 | 独立服务（Docker） |
| 性能 | <10k chunks 够用 | 百万级+ |
| 元数据过滤 | 支持 | 支持，更强 |
| 切换成本 | 抽象层封装，**接口一致即可切换** | 同 |

**设计原则**：所有 RAG 调用走抽象接口 `VectorStore`（自实现 Protocol），ChromaDB 是默认实现，Qdrant/Milvus 是备选实现。

## Embedding

```python
# app/rag/embedding.py
from langchain_openai import OpenAIEmbeddings

def get_embeddings():
    return OpenAIEmbeddings(
        model=settings.EMBEDDING_MODEL,        # text-embedding-3-small 或同类
        base_url=settings.LLM_BASE_URL,        # 复用中转站
        api_key=settings.LLM_API_KEY,
        chunk_size=64,
        retry_min_seconds=2,
        retry_max_seconds=10,
    )
```

**降级方案**：若中转站无 embedding 接口 → 用本地 `bge-small-zh-v1.5`（CPU 即可，~50ms/chunk）。

## Hybrid Search 实现

```python
# app/rag/retrieval/hybrid.py
class HybridRetriever:
    def __init__(self, vector_store, bm25_index):
        self.vector = vector_store
        self.bm25 = bm25_index

    async def search(self, query: str, top_k: int = 20, filters: dict = None) -> list[dict]:
        # 并行召回
        dense_task = self.vector.similarity_search_with_score(query, k=top_k, filter=filters)
        bm25_task = asyncio.to_thread(self.bm25.search, query, top_k)
        dense_results, bm25_results = await asyncio.gather(dense_task, bm25_task)

        # RRF 融合
        fused = reciprocal_rank_fusion(
            [(r, "dense") for r in dense_results],
            [(r, "bm25") for r in bm25_results],
            k=60,
        )
        return fused[:top_k]
```

**为什么用 RRF**：Reciprocal Rank Fusion 是工业界常用的多路召回融合方法，无需训练，对各路召回的分数尺度不敏感，效果稳健。

## Reranker

```python
# app/rag/retrieval/reranker.py
from langchain_cohere import CohereRerank

def get_reranker():
    return CohereRerank(
        model="rerank-multilingual-v3.0",
        cohere_api_key=settings.COHERE_API_KEY,
        top_n=5,
    )

async def rerank(query: str, candidates: list[dict]) -> list[dict]:
    docs = [Document(page_content=c["content"], metadata=c["metadata"])
            for c in candidates]
    reranked = await get_reranker().acompress_documents(docs, query)
    return [{**r.metadata, "content": r.page_content, "score": ...} for r in reranked]
```

**降级方案**：若 Cohere key 不可用 → 用 `bge-reranker-v2-m3`（本地模型，CPU ~200ms/query）。

## Critic 节点

```python
CRITIC_PROMPT = """你是一名严格的审稿人。评估以下检索片段是否足以回答用户问题。

用户问题：{query}
检索片段（按相关度排序）：{chunks}

输出 JSON：
{{
  "relevance": 0.0-1.0,    // 片段与问题相关性
  "grounding": 0.0-1.0,     // 答案能完全基于片段支撑的程度
  "missing": "string",      // 还缺什么信息
  "refined_query": "string" // 若需重新检索，给出新查询
}}

阈值：relevance ≥ 0.5 AND grounding ≥ 0.5 才算通过。"""
```

## Composer + Citation Check

```python
COMPOSER_PROMPT = """基于以下检索到的文档片段，回答用户问题。

要求：
1. 答案中每句话必须在末尾标注引用，例如：[doc_id#chunk_3]
2. 不可编造片段中没有的信息
3. 如果片段不足，明确说"暂无可用知识"

用户问题：{query}
检索片段：{chunks}

输出 JSON：{{
  "answer": "完整回答...",
  "citations_used": ["doc_id#chunk_id", ...]
}}"""

# citation_check 规则：
# 答案中所有事实断言对应 doc_id 必须存在于 retrieved_docs
def citation_check(answer, retrieved_docs):
    # 提取所有 [xxx] 引用
    cited_ids = re.findall(r'\[([^\]]+)\]', answer)
    valid_ids = {d["doc_id"] for d in retrieved_docs}
    missing = [c for c in cited_ids if c not in valid_ids]
    return missing
```

## RAG 评估（CI/CD 必备）

```python
# tests/eval/rag_eval.py
from ragas import evaluate
from ragas.metrics import faithfulness, answer_relevancy, context_precision

def run_eval():
    testset = load_golden_qa()   # 20-50 个标注问答对
    result = evaluate(testset, metrics=[
        faithfulness,           # 答案是否基于检索内容（幻觉）
        answer_relevancy,       # 答案是否回答了问题
        context_precision,      # 检索片段是否精确
    ])
    # CI 阈值：faithfulness > 0.85, answer_relevancy > 0.80, context_precision > 0.75
    assert result["faithfulness"] > 0.85
```

## RAG Badcase 收集与标注

> 本节是 `scalable-waddling-moore.md` 3.2.6 的 RAG 侧细化。trace 骨架在主方案里定义，本节聚焦：**怎么把线上问题沉淀成可复用的改进素材**，并通过 4 个核心指标量化每次改动的收益。

### 为什么单独再写一节

CI 里跑的 ragas 评估是**守门员**（防止改坏），但要回答"线上这条回答为什么错了、下次怎么不再犯"，光有 trace 复盘不够：

- trace 只能告诉你"这一轮 critic 给 0.3、citation 失败"，**不能归因**是 embedding 召回错、还是 rerank 排错、还是 compose 编造
- 没有 badcase 库，badcase 来一个丢一个，无法转成下次回归的测试集
- 没有版本号，badcase 库里的"答错"无法关联到"哪个模型版本出的"

所以 RAG 改进闭环需要 4 块能力：badcase 入库、eval_set 沉淀、4 指标量化、版本号归因。

### 1. RAG 专属 span metadata 约定

为支撑复盘与离线重放，所有 RAG span 的 `metadata` 字段必须结构化，**用 Pydantic 强约束**（避免上游乱写 JSON）：

```python
# app/observability/rag_metadata.py
from pydantic import BaseModel
from typing import Literal

class RetrievedChunk(BaseModel):
    id: str                   # "doc_id#chunk_idx"
    source: str               # 文件路径或 db://table/id
    score: float
    rank: int
    retriever: Literal["dense", "bm25"]

class HybridSearchMetadata(BaseModel):
    query_used: str
    retrieved_chunks: list[RetrievedChunk]
    vector_topk: int
    bm25_topk: int
    fusion_method: Literal["rrf", "linear"]

class RerankMetadata(BaseModel):
    reranked_chunks: list[RetrievedChunk]

class ComposeMetadata(BaseModel):
    final_context_chunks: list[str]   # 进入 prompt 的 chunk id
    context_token_count: int
    prompt_template_version: str
    # 版本号字段为必填，否则 badcase 库无法归因
    embedding_model_version: str
    rerank_model_version: str
    chunk_strategy_version: str
```

**版本号取数**：
- `embedding_model_version` / `rerank_model_version`：从 `settings.py`（如 `EMBEDDING_MODEL=...@2026-10`）取
- `chunk_strategy_version`：从 ingestion pipeline 启动时记录的 `splitter_config` hash 派生，如 `md-h1h2-512-64@a1b2c3`

### 2. badcase 触发与入库

3 条路径并存：

| 触发源 | 触发条件 | 实时性 | 优点 | 缺点 |
|---|---|---|---|---|
| **自动规则** | `rag_critic` 分数 < 0.5；`citation_check` 失败；`final_context_chunks` 为空 | 实时 | 覆盖全量 | 误报需复核 |
| **用户反馈** | 会话结束 👍/👎，可附 1-5 星 + 文字 | 准实时 | 真实体感 | 样本稀疏 |
| **人工标注** | 后台从 `trace_id` 拉起整条 span 树 + chunk 原文，标 category | 离线 | 质量最高 | 人力成本 |

入库表：

```sql
CREATE TABLE rag_badcase (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  span_id VARCHAR(32),
  category ENUM('irrelevant','miss_recall','hallucination','wrong_citation','other'),
  severity TINYINT,                -- 1=低 2=中 3=高
  source ENUM('auto','user_feedback','manual'),
  remark TEXT,
  user_id BIGINT,
  created_at DATETIME(3),
  KEY idx_category (category),
  KEY idx_trace (trace_id),
  KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**category 字典**：
- `irrelevant`：召回的 chunk 与问题无关（dense 召回错位）
- `miss_recall`：期望的 chunk 未被召回（chunker 切碎 / 向量未覆盖）
- `hallucination`：答案编造了 context 中没有的内容（compose prompt 兜底不够）
- `wrong_citation`：引用的 chunk 不支撑对应句子（cite 模板被 LLM 忽略）
- `other`：其他（如时间逻辑错误、跨文档冲突）

**后台标注页必备能力**：
- 一键从 `trace_id` 拉起完整 span 树（含所有 chunk 原文与分数）
- 显示 metadata 里的 3 个版本号（避免人工猜）
- 一键把当前 badcase 转成 eval_set 条目（问题 + 期望 chunk + 期望答案）

### 3. 离线测评集管理

```sql
CREATE TABLE rag_eval_set (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question TEXT NOT NULL,
  expected_chunk_ids JSON,        -- ["doc_id#chunk_idx", ...]
  expected_answer TEXT,
  tags JSON,                       -- 业务标签 ["塔吊","阈值"]
  source ENUM('synthetic','real_badcase','manual'),
  enabled TINYINT DEFAULT 1,
  created_at DATETIME(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**集构建比例**（200~500 条起步）：
- **30% 真实 badcase 改写** — 直接复用 `rag_badcase` 里被人工复核过的条目，确保评估贴近线上问题
- **30% 人工设计** — 覆盖业务长尾（如"某型号塔吊的检修周期"），由领域专家产出
- **40% 从 docs 摘要合成** — 拿 chunk 原文 + LLM 反向生成 question + 期望 chunk 列表，保证广度

**回放机制**：
- 改完任一组件（embedding / rerank / chunker / prompt）→ 把 `rag_eval_set` 全量跑一遍
- 4 指标全部 ≥ 阈值才允许进灰度
- 4 指标中任意一项掉到阈值下 → 不允许合入，必须先回滚或继续改进

### 4. 4 个核心指标

| 指标 | 计算方式 | 目标 | 实现 |
|---|---|---|---|
| **Context Recall@K** | `\|expected_chunk_ids ∩ retrieved\| / \|expected_chunk_ids\|` | > 0.85 | 直接集合运算 |
| **Context Precision** | LLM-as-judge 判断 retrieved 中每条对回答该 question 是否有用，取平均 | > 0.80 | `app/eval/judge.py` |
| **Faithfulness** | 答案每句是否在 context 中能找到支撑（防幻觉） | > 0.85 | `ragas.faithfulness` |
| **Citation Accuracy** | 答案中每条 `[doc_id#chunk_idx]` 引用是否真支撑对应句子 | > 0.90 | 规则匹配 + LLM 评 |

> 与"## RAG 评估（CI/CD 必备）"小节的关系：上一节跑 ragas 通用指标（faithfulness/answer_relevancy/context_precision）做守门；本节跑业务级 4 指标做改进量化。**两者并行，不互相替代**。

### 5. 改进闭环

```
   线上 trace ──► 自动规则 / 用户反馈 ──► rag_badcase
                                          │
                                          ▼
                              人工复核 + 归类 ──► rag_eval_set（30% 真实 badcase 来源）
                                          │
                                          ▼
                          改 embedding / rerank / chunk / prompt
                                          │
                                          ▼
                          回放 eval_set ──► 4 指标对比
                                          │
                              ┌───────────┴───────────┐
                              ▼                       ▼
                          通过(>阈值)              未通过
                              │                       │
                              ▼                       ▼
                       灰度上线 + 监控            回归 badcase 库继续补充
                       线上指标观察 1 周
```

### 6. 关键文件清单（增量）

```bash
# P6 阶段增量（已存在的 RAG 工作之外）
新建 app/observability/rag_metadata.py        # Pydantic schema 强约束
新建 db/migration/V7__create_rag_badcase.sql
新建 db/migration/V7__create_rag_eval_set.sql
新建 app/rag/badcase/{detector.py,store.py}    # 自动检测 + 入库
新建 app/rag/eval/{runner.py,judge.py,metrics.py}   # 4 指标 + LLM judge
新建 app/rag/eval/eval_set_manager.py         # eval_set CRUD
新建 tests/eval/test_rag_badcase_e2e.py       # badcase 闭环 e2e 测试
新建 ConsSafetySystem/src/views/admin/RagBadcaseList.vue
新建 ConsSafetySystem/src/views/admin/RagEvalSetEditor.vue
```

### 7. 与现有"RAG 评估（CI/CD 必备）"小节的关系

| 维度 | 上一节（RAG 评估） | 本节（Badcase 收集） |
|---|---|---|
| 定位 | CI 守门员 | 改进闭环引擎 |
| 输入 | 静态 golden_qa | 动态累积：badcase + 合成 + 人工 |
| 指标 | faithfulness / answer_relevancy / context_precision | Context Recall@K / Precision / Faithfulness / Citation Accuracy |
| 触发 | 每次 PR | 每次改动 embedding/rerank/chunk/prompt |
| 失败时 | 阻断合入 | 阻断合入 + 强制回归 badcase 库 |

**两者都必跑**：CI 同时调 `tests/eval/rag_eval.py`（守门）和 `tests/eval/test_rag_badcase_e2e.py`（量化）。

## 摄入调度

| 数据源 | 调度 | 同步方式 |
|--------|------|---------|
| `docs/*.md` | git push 触发（webhook） | 全量重建受影响文件 |
| 数据库 `equipment_assets` | 每日 02:00 | 增量（按 `update_time`） |
| 数据库 `equipment_type` | 每日 02:00 | 增量 |
| 数据库 `monitor_point_alert` | 写入时（trigger） | 仅"已处理"报警（含处理意见） |
| SpringDoc OpenAPI | 启动时 | 全量 |

## 摄入流水线实现

```python
# app/rag/ingestion/pipeline.py
class IngestionPipeline:
    def __init__(self, store: VectorStore, embeddings):
        self.store = store
        self.embeddings = embeddings

    async def ingest_markdown(self, path: Path):
        loader = MarkdownLoader(path)
        cleaner = Cleaner()
        splitter = MarkdownHeaderTextSplitter(headers=[("#","h1"),("##","h2")])
        for chunk in splitter.split_text(cleaner.clean(loader.load())):
            chunk.metadata.update({
                "doc_id": str(path),
                "category": "doc",
                "source": str(path),
                "ingested_at": datetime.now().isoformat(),
            })
            await self.store.aadd_texts(
                texts=[chunk.page_content],
                metadatas=[chunk.metadata],
            )

    async def ingest_db_records(self, table: str, since: datetime = None):
        records = await self.backend_client.get(
            f"/internal/knowledge/{table}",
            params={"since": since.isoformat() if since else None}
        )
        for r in records:
            text = self._format_record(r)
            metadata = {
                "doc_id": f"db://{table}/{r.get('id')}",
                "category": "database",
                "table": table,
                "ingested_at": datetime.now().isoformat(),
            }
            await self.store.aadd_texts([text], [metadata])
```

## 共享 RAG 工具（子 Agent 内部可用）

每个子 Agent 的工具集都包含 `knowledge_search`：

```python
@tool
async def knowledge_search(query: str, top_k: int = 5) -> dict:
    """在内部知识库中检索与查询相关的文档片段。

    用于回答业务概念、API 用法、阈值定义、设备规格等问题。

    Args:
        query: 自然语言查询
        top_k: 返回片段数量（默认 5，最多 20）

    Returns:
        {chunks: [{doc_id, content, metadata, score}], summary: str}
    """
    # 调 knowledge subgraph 的 query_rewriter → hybrid_search → reranker
    # 子 Agent 走的是"快速版"：跳过 composer，直接返回原始 chunks
```

这样：
- 子 Agent 遇到概念性问题 → 自动调用 `knowledge_search`（无需 Supervisor 路由）
- Supervisor 路由到 `knowledge` 子图 → 做深度 RAG（带反思+引用校验）

## knowledge 子图构建代码

```python
# app/graph/subgraphs/knowledge.py
def build_knowledge_subgraph() -> CompiledGraph:
    g = StateGraph(KnowledgeState)

    g.add_node("rewrite", query_rewriter_node)
    g.add_node("hybrid", hybrid_search_node)
    g.add_node("rerank", rerank_node)
    g.add_node("critic", critic_node)
    g.add_node("refine", query_refiner_node)
    g.add_node("compose", composer_node)
    g.add_node("cite_check", citation_check_node)
    g.add_node("exit", exit_subgraph)

    g.set_entry_point("rewrite")
    g.add_edge("rewrite", "hybrid")
    g.add_edge("hybrid", "rerank")
    g.add_edge("rerank", "critic")

    g.add_conditional_edges(
        "critic",
        lambda s: "refine" if s["critic_score"] < 0.5 and s["refine_retries"] < 2 else "compose",
        {"refine": "refine", "compose": "compose"}
    )
    g.add_edge("refine", "hybrid")

    g.add_edge("compose", "cite_check")
    g.add_conditional_edges(
        "cite_check",
        lambda s: "refine" if s["missing_citations"] else "exit",
        {"refine": "refine", "exit": "exit"}
    )
    g.add_edge("exit", END)

    return g.compile()
```
