# 智能客服与工单自动化平台

这是一个用于简历展示的 Spring Boot 后端项目骨架，目标是实现“多渠道会话接入、AI 问答与工单草稿、知识库检索、工单流转、自动派单、SLA 提醒、通知、质检、统计看板、审计日志”的完整业务闭环。

## 技术栈

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL 8
- Redis
- RabbitMQ
- Elasticsearch
- Spring AI
- Spring Security + JWT
- Springdoc OpenAPI / Swagger
- Actuator

## 当前工程状态

已完成基础工程、依赖、配置文件和三层包结构；业务接口、实体、Mapper XML 和具体实现后续按阶段补充。

## 包结构

```text
com.example.smartcustomerservice
├── controller        # Controller 层：接收 HTTP 请求，参数校验，返回统一响应
├── service           # Service 层：业务编排、状态流转、事务控制
│   └── impl          # Service 实现
├── mapper            # Mapper 层：MyBatis-Plus Mapper 接口
├── domain
│   ├── entity        # 数据库实体
│   ├── dto           # 请求 DTO
│   ├── vo            # 响应 VO
│   └── enums         # 业务枚举
├── common
│   ├── result        # 统一返回体
│   ├── exception     # 全局异常
│   └── constants     # 常量
├── config            # 配置类
├── security          # JWT、认证、权限
├── mq                # RabbitMQ 消息生产与消费
├── ai                # Spring AI Agent、工具调用
└── es                # Elasticsearch 文档与检索
```

## 配置说明

`src/main/resources/application.yml` 已使用环境变量读取数据库、中间件、MinIO 和 JWT 配置。请不要把真实密码写入 Git 仓库。

常用环境变量：

```bash
MYSQL_URL=jdbc:mysql://localhost:3306/smart_customer_service?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PASSWORD=your_password
RABBITMQ_HOST=localhost
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
JWT_SECRET=please-change-to-a-long-random-secret
```
