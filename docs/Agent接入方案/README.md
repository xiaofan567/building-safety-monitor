# Agent 接入方案

本目录是「智慧工地安全监测系统」接入 LangGraph 1.0 多 Agent + Agentic RAG 的设计文档。

> 📌 **本目录所有架构图均为 Unicode 框图**，可在 VSCode / Typora / 任何纯文本编辑器中正常显示。
> 每张图后附带等价 Mermaid 源码块（如需嵌入到支持 Mermaid 的平台渲染）。

## 文档导航

| 文件 | 内容 |
|------|------|
| [00-总览](./00-总览.md) | 设计背景、目标、关键决策、版本策略 |
| [01-架构总览](./01-架构总览.md) | 顶层架构图、组件关系、数据流 |
| [02-图设计](./02-图设计.md) | Supervisor + 5 个子图（State、节点、条件边） |
| [03-RAG设计](./03-RAG设计.md) | Agentic RAG 完整设计（ingestion、retrieval、critic、composer） |
| [04-稳定性与可观测性](./04-稳定性与可观测性.md) | Checkpoint 双层存储、Trace、Time-Travel、Guardrails、**§4.7 Conflict Resolution HITL**（目标模糊 / 多源冲突 / 低置信度仲裁） |
| [05-工具集](./05-工具集.md) | 工具清单 + 与 Spring Boot REST 的映射 |
| [06-实施阶段](./06-实施阶段.md) | P0 ~ P7 阶段任务清单与验收 |
| [07-定时日报](./07-定时日报.md) | 定时自动日报：cron + 收件人 + 提示词模板（前端可配） |

## 快速概览

### 三个核心痛点

1. **Coze 工具受限**：无法直接查后端业务数据（设备、报警、监测点）
2. **知识/插件非本地化**：调试、审计、版本管理困难
3. **SSE 未真正流式**：后端把流合并为单次响应，前端体验差

### 改造目标

用 **LangGraph 1.0 多 Agent + Agentic RAG** 替代 Coze。

### 关键决策

| 维度 | 决策 |
|------|------|
| 场景 | 多 Agent 协同（5 子 Agent + Supervisor） |
| 部署 | 独立 Python 微服务 `agent-service/` |
| LangGraph | **1.0+**（2025-12-12 GA） |
| LLM | 中转站 OpenAI 兼容 API |
| RAG | **Agentic RAG**（knowledge 子 Agent + 共享 retrieval 工具） |
| 向量库 | ChromaDB（开发）→ Qdrant（生产） |
| Checkpoint | **Redis 主存 + MySQL 持久化**（双层） |
| 输出 | SSE 流式 + 中间过程事件 |
| 稳定性 | Trace + Time-Travel + Guardrails 全栈 |

### 项目结构（增量）

```
building-safety-monitor/
├── agent-service/                        [新建]
├── smartbuildings-backend/               [修改]
├── ConsSafetySystem/                     [修改]
└── docs/Agent接入方案/                   [本目录]
```

## 关联资源

- [LangGraph 1.0 GA Changelog](https://changelog.langchain.com/announcements/langgraph-1-0-is-now-generally-available-f3a4b2)
- [LangGraph v1.0 Migration Guide](https://medium.com/@qasimwani618/langgraph-v1-0-migrating-from-0-x-to-1-0-c7a1faf6408d)
- [Agentic RAG 2026 Best Practices](https://sukruyusufkaya.com/en/blog/agentic-rag-langgraph-uretim-mimarisi-2026)
- [Self-Correcting RAG with Neo4j](https://www.syuthd.com/2026/05/building-self-correcting-agentic-rag.html)
