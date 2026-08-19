# 角色与权限说明

## 1. 权限模型

本项目使用 RBAC 权限模型：

```text
sys_user
  -> sys_user_role
  -> sys_role
  -> sys_role_permission
  -> sys_permission
```

含义：

- 一个用户可以拥有多个角色。
- 一个角色可以拥有多个权限。
- 登录成功后，根据用户 ID 查询角色，再根据角色查询权限。
- 前端可用菜单权限控制页面入口，后端可用权限编码控制接口访问。

## 2. 角色表：sys_role

建议初始化 4 个角色。

| role_code | role_name | 作用 |
| --- | --- | --- |
| ADMIN | 系统管理员 | 拥有系统全部权限，负责用户、角色、权限、规则、知识库等管理 |
| SUPERVISOR | 客服主管 | 管理客服业务，负责工单、派单、质检、看板和知识库发布 |
| AGENT | 客服坐席 | 处理客户会话，使用 AI 辅助生成工单，查看和处理自己的工单 |
| OPS | 运维处理人 | 处理故障类、技术类、运维类工单，查看知识库和工单详情 |

注册功能中默认会尝试绑定 `AGENT` 角色。如果数据库里没有 `AGENT`，注册仍然成功，只是不绑定默认角色。

## 3. 权限表：sys_permission

你当前的 `sys_permission` 字段为：

| 字段 | 作用 |
| --- | --- |
| id | 权限主键 |
| permission_code | 权限编码，后端鉴权和前端按钮控制主要使用这个值 |
| permission_name | 权限中文名称，用于后台展示 |
| resource_type | 资源类型，建议使用 MENU、API、BUTTON |
| resource_path | 菜单路由、接口路径或按钮标识 |
| parent_id | 父权限 ID，可用于生成菜单树 |
| sort_order | 排序值，越小越靠前 |
| created_at | 创建时间 |

`resource_type` 建议含义：

- `MENU`：菜单权限，前端控制左侧菜单或页面入口。
- `API`：接口权限，后端控制接口能否访问。
- `BUTTON`：按钮权限，前端控制页面按钮，例如转派、挂起、质检审核。

## 4. 菜单权限

| permission_code | permission_name | resource_path | 作用 |
| --- | --- | --- | --- |
| dashboard:view | 运营看板 | /dashboard | 查看核心运营指标 |
| customer:menu | 客户管理 | /customers | 进入客户管理页面 |
| conversation:menu | 会话中心 | /conversations | 进入客服会话页面 |
| ai:menu | AI工作台 | /ai-workbench | 使用 AI 问答和工单草稿能力 |
| knowledge:menu | 知识库管理 | /knowledge | 进入知识库管理页面 |
| ticket:menu | 工单中心 | /tickets | 进入工单管理页面 |
| assignment:menu | 派单规则 | /assignment-rules | 进入派单规则配置页面 |
| quality:menu | 质检管理 | /quality | 进入质检任务页面 |
| system:menu | 系统管理 | /system | 进入用户、角色、权限管理页面 |

## 5. 核心 API 与按钮权限

认证与当前用户：

| permission_code | 作用 |
| --- | --- |
| auth:register | 用户注册 |
| auth:login | 用户登录 |
| user:me | 查询当前用户信息 |
| menu:me | 查询当前用户菜单权限 |

客户与会话：

| permission_code | 作用 |
| --- | --- |
| customer:create | 创建客户 |
| customer:list | 客户分页查询 |
| customer:detail | 客户详情 |
| customer:update | 修改客户 |
| customer:delete | 删除客户 |
| conversation:list | 会话分页查询 |
| conversation:create | 创建会话 |
| conversation:detail | 会话详情 |
| conversation:message:list | 查看会话消息 |
| conversation:message:send | 发送会话消息 |
| conversation:takeover | 人工接管会话按钮 |
| conversation:release | 退出接管会话 |
| conversation:close | 关闭会话 |
| file:upload | 上传普通附件 |
| file:image:upload | 上传图片 |
| file:detail | 查看文件资源详情 |

AI 与知识库：

| permission_code | 作用 |
| --- | --- |
| ai:chat | AI 会话问答 |
| ai:ticket-draft | AI 生成工单草稿 |
| knowledge:search | 知识检索 |
| knowledge:article:create | 创建知识文章 |
| knowledge:article:list | 知识文章分页 |
| knowledge:article:publish | 发布知识文章 |
| knowledge:article:offline | 下线知识文章 |

工单与派单：

| permission_code | 作用 |
| --- | --- |
| ticket:create | 创建工单 |
| ticket:list | 工单分页查询 |
| ticket:detail | 工单详情 |
| ticket:assign | 工单派单 |
| ticket:close | 关闭工单 |
| ticket:transfer | 转派工单按钮 |
| ticket:hold | 挂起工单按钮 |
| assignment-rule:create | 创建派单规则 |
| assignment-rule:list | 派单规则分页查询 |
| assignment-rule:detail | 派单规则详情 |
| assignment-rule:update | 修改派单规则 |
| assignment-rule:delete | 删除派单规则 |

看板、质检与系统管理：

| permission_code | 作用 |
| --- | --- |
| statistics:dashboard | 查看运营看板 |
| quality:list | 质检任务列表 |
| quality:review | 质检审核按钮 |
| system:user:list | 用户列表 |
| system:user:assign-role | 分配用户角色 |
| system:role:list | 角色列表 |
| system:role:assign-permission | 分配角色权限 |
| system:permission:tree | 权限树 |

## 6. 角色与权限关系

| 角色 | 权限范围 |
| --- | --- |
| ADMIN | 全部权限 |
| SUPERVISOR | 客服主管权限，包含客户、会话、AI、知识库、工单、派单规则、统计看板、质检 |
| AGENT | 客服坐席权限，包含客户基础操作、会话、AI、知识检索、工单创建/查询/关闭 |
| OPS | 运维处理权限，包含工单查询/详情/关闭、知识检索、运营看板 |

建议分配逻辑：

- `ADMIN`：用于项目演示和系统管理，绑定所有权限。
- `SUPERVISOR`：用于体现真实客服主管角色，可以配置规则和看报表。
- `AGENT`：注册默认角色，适合普通客服登录。
- `OPS`：用于体现跨团队派单，例如账号、系统故障、运维问题。

## 7. 登录后如何使用

登录时建议返回：

- `roleCodes`：角色编码列表，例如 `["AGENT"]`。
- `permissionsCodes`：权限编码列表，例如 `["ticket:create", "knowledge:search"]`。

前端用途：

- 根据 `MENU` 权限控制侧边栏菜单。
- 根据 `BUTTON` 权限控制按钮显隐。

后端用途：

- JWT Filter 解析用户身份。
- 从 Redis 或数据库加载权限。
- 使用 `@PreAuthorize("hasAuthority('ticket:create')")` 控制接口访问。

## 8. 面试表达

可以这样讲：

“系统采用 RBAC 权限模型，用户通过 `sys_user_role` 绑定角色，角色通过 `sys_role_permission` 绑定菜单、按钮和接口权限。登录时系统会加载用户角色和权限编码，返回给前端用于菜单与按钮控制；后端后续可以结合 Spring Security 的 `@PreAuthorize` 做接口级权限控制。这样权限既能支持页面展示，也能支持后端安全校验。”
