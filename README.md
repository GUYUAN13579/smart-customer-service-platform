# 智能客服与工单自动化平台

这是一个用于简历展示的全栈项目，包含 Spring Boot 后端和 Vue 3 前端。

## 项目结构

```text
.
├── smart-customer-service-platform   # Spring Boot 后端
└── smart-customer-service-frontend   # Vue 3 + Vite 前端
```

## 核心功能

- 用户注册、登录、JWT 鉴权、刷新 Token、退出登录
- 用户权限、角色、菜单接口
- 客户资料 CRUD
- 派单规则 CRUD
- 会话中心
- 用户端测试聊天页
- 客服接管、退出接管、关闭会话
- 文本、图片、文件消息发送
- MinIO 文件上传与后端代理读取

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- Spring Security + JWT
- MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- MinIO
- Springdoc OpenAPI

### 前端

- Vue 3
- Vite
- Vue Router
- 原生 CSS 组件化样式

## 本地启动

### 后端

进入后端目录：

```bash
cd smart-customer-service-platform
```

配置环境变量后启动 Spring Boot。

主要环境变量：

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

### 前端

进入前端目录：

```bash
cd smart-customer-service-frontend
npm install
npm run dev
```

默认前端地址：

- 管理端：`http://localhost:5173`
- 用户端测试聊天页：`http://localhost:5173/customer-chat`

如果 `5173` 被占用，Vite 可能自动切换为 `5174` 等端口。

## 当前状态

项目目前已完成基础鉴权、客户管理、派单规则、会话中心、文件上传与前端主要页面。后续计划继续完善工单创建/查询、AI 工单草稿、自动派单、SLA、消息队列通知等模块。

