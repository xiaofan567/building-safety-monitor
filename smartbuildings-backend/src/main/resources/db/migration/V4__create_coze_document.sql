DROP TABLE IF EXISTS `coze_document`;

CREATE TABLE `coze_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_id` varchar(128) DEFAULT NULL COMMENT 'Coze 文档唯一标识（优先使用记录 id，缺失时回退 doc_id）',
  `doc_name` varchar(256) DEFAULT NULL COMMENT '文档名称',
  `doc_url` varchar(512) DEFAULT NULL COMMENT '本地存储的相对路径（用于 /doc-files/ 资源访问）',
  `bstudio_create_time` datetime DEFAULT NULL COMMENT 'Coze 平台上的文档创建时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '本地记录创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '本地记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_id` (`doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Coze 知识库文档本地镜像表';