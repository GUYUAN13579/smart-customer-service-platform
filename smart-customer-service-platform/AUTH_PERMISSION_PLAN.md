# 注册、登录与用户权限实施方案

## 1. 目标

先完成后台系统最基础的一套身份链路：

- 用户注册
- 用户登录
- JWT 访问令牌与刷新令牌
- 当前用户信息查询
- 当前用户菜单和按钮权限查询
- 基于角色的接口访问控制
- 登录、注册、权限变更等关键操作审计

建议先做“能跑通、能演示、能解释”的版本，再逐步补验证码、登录风控、多端登录控制等增强能力。

## 2. 使用技术

- Spring Boot Web：提供 REST API。
- Spring Security：处理认证、授权、接口放行、登录态识别。
- JWT：实现无状态登录，前端通过 `Authorization: Bearer <token>` 调用接口。
- MyBatis-Plus：操作用户、角色、权限、用户角色表。
- BCrypt：存储密码摘要，禁止明文保存密码。
- Redis：保存刷新令牌、登录状态、权限缓存、验证码、登录失败计数。
- Bean Validation：校验注册、登录请求参数。
- 全局异常处理：统一返回错误码和错误信息。
- Swagger / OpenAPI：调试登录、注册、权限接口。
- audit_log：记录注册、登录、登出、权限变更等关键行为。

## 3. 相关数据表

第一批使用这些表：

- `sys_user`：用户账号、密码摘要、真实姓名、状态等。
- `sys_role`：角色，例如 `ADMIN`、`SUPERVISOR`、`AGENT`。
- `sys_permission`：菜单权限和接口权限。
- `sys_user_role`：用户和角色的多对多关系。
- `audit_log`：审计日志。

建议后续补充：

- `sys_role_permission`：如果你当前建表脚本里还没有这张表，建议加上，用于维护角色与权限的关系。

## 4. 接口设计

建议第一批实现：

- `POST /api/v1/auth/register`：注册用户。
- `POST /api/v1/auth/login`：用户登录。
- `POST /api/v1/auth/refresh`：刷新访问令牌。
- `POST /api/v1/auth/logout`：退出登录。
- `GET /api/v1/users/me`：查询当前用户信息。
- `GET /api/v1/menus/me`：查询当前用户菜单权限。

后台管理增强接口：

- `GET /api/v1/users`：分页查询用户。
- `POST /api/v1/users/{id}/roles`：给用户分配角色。
- `GET /api/v1/roles`：角色列表。
- `POST /api/v1/roles/{id}/permissions`：给角色分配权限。
- `GET /api/v1/permissions/tree`：权限树。

## 5. 注册流程

推荐流程：

1. Controller 接收 `RegisterRequest`。
2. 使用 Bean Validation 校验用户名、密码、手机号、邮箱等字段。
3. Service 查询 `sys_user`，判断用户名是否已存在。
4. 使用 `BCryptPasswordEncoder` 对密码加密。
5. 写入 `sys_user`，默认状态为启用。
6. 给新用户绑定默认角色，例如 `AGENT`。
7. 写入 `audit_log`，记录注册动作。
8. 返回用户基础信息，不返回密码摘要。

注意点：

- 密码永远不明文入库。
- 注册接口是否开放要看项目定位。简历项目中可以开放注册，也可以只允许管理员创建账号。
- 用户名唯一性既要在代码判断，也要依赖数据库唯一索引兜底。

## 6. 登录流程

推荐流程：

1. Controller 接收 `LoginRequest`。
2. Service 根据 username 查询用户。
3. 判断用户是否存在、是否禁用、是否逻辑删除。
4. 使用 `BCryptPasswordEncoder.matches` 校验密码。
5. 查询用户角色和权限。
6. 生成 accessToken 和 refreshToken。
7. refreshToken 存入 Redis，TTL 建议 7 天。
8. 可将用户权限缓存到 Redis，减少每次请求查库。
9. 更新 `last_login_at`。
10. 写入 `audit_log`。
11. 返回 `TokenResponse`。

建议 Token 内容：

- userId
- username
- roleCodes
- tokenType
- issuedAt
- expiresAt

注意点：

- accessToken 有效期建议 2 小时。
- refreshToken 有效期建议 7 天。
- JWT secret 必须换成足够长的随机字符串。
- 登录失败次数可以用 Redis 计数，后续做限流或短暂锁定。

## 7. 请求鉴权流程

每次访问受保护接口：

1. 前端在请求头携带 `Authorization: Bearer <accessToken>`。
2. JWT 过滤器解析 Token。
3. 校验签名、过期时间、tokenType。
4. 从 Token 或 Redis 中拿到用户身份和权限。
5. 构造 Spring Security 的 Authentication 对象。
6. 放入 SecurityContext。
7. Controller 执行前，Spring Security 判断是否有权限访问。

建议放行接口：

- `/api/v1/auth/register`
- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/actuator/health`

## 8. 权限模型

建议使用 RBAC：

- 用户拥有多个角色。
- 角色拥有多个权限。
- 权限分为菜单权限和接口权限。

权限字段建议：

- `permission_code`：权限编码，例如 `ticket:create`、`ticket:assign`。
- `permission_type`：权限类型，例如 `MENU`、`BUTTON`、`API`。
- `parent_id`：支持菜单树。
- `path`：前端路由或后端接口路径。
- `status`：是否启用。

接口权限控制方式：

- 简单版本：在 Security 配置里按路径配置权限。
- 推荐版本：使用 `@PreAuthorize("hasAuthority('ticket:create')")` 标注 Controller 方法。

## 9. 需要创建的类

Controller：

- `AuthController`
- `UserController`
- `MenuController`

Service：

- `AuthService`
- `UserService`
- `RoleService`
- `PermissionService`
- `AuditLogService`

Mapper：

- `SysUserMapper`
- `SysRoleMapper`
- `SysPermissionMapper`
- `SysUserRoleMapper`
- `SysRolePermissionMapper`
- `AuditLogMapper`

DTO：

- `RegisterRequest`
- `LoginRequest`
- `RefreshTokenRequest`
- `LogoutRequest`
- `UserRoleAssignRequest`

VO：

- `TokenResponse`
- `UserProfileVO`
- `MenuTreeVO`
- `PermissionVO`
- `RoleVO`

Security：

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `JwtTokenProvider`
- `UserDetailsServiceImpl`
- `LoginUser`
- `PasswordEncoderConfig`

## 10. 推荐开发顺序

1. 先写实体和 Mapper：`SysUser`、`SysRole`、`SysPermission`、`SysUserRole`。
2. 写 `PasswordEncoderConfig` 和 `JwtTokenProvider`。
3. 写注册接口，只完成用户入库和默认角色绑定。
4. 写登录接口，返回 accessToken 和 refreshToken。
5. 写 JWT 过滤器，让受保护接口能识别当前用户。
6. 写 `/users/me`，验证登录态是否可用。
7. 写权限查询，返回当前用户菜单树和按钮权限。
8. 接入 `@PreAuthorize`，验证接口级权限。
9. 加 Redis 保存 refreshToken 和权限缓存。
10. 加 audit_log，补关键操作日志。

## 11. 面试讲法

可以这样表达：

“我在认证模块里使用 Spring Security + JWT 做无状态登录，密码使用 BCrypt 摘要存储。权限模型采用 RBAC，用户关联角色，角色关联菜单和接口权限；登录成功后生成短期 accessToken 和长期 refreshToken，refreshToken 以及用户权限缓存放在 Redis 中。接口层通过 JWT Filter 解析用户身份，结合 `@PreAuthorize` 做权限控制。所有登录、登出、权限变更等关键操作都会写入 audit_log，便于审计和问题排查。”
