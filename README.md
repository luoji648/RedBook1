# Redbook（小红书风格全栈示例）

面向学习与演示的 **生活方式社区 + 电商导购** 一体化 Web 应用：用户发布图文笔记、浏览关注/推荐流、互动（点赞/收藏/评论/关注）、私信与群聊、商品与购物车/订单/优惠券/钱包，以及基于 **Dify** 的帖子 AI 助手等能力。

> 本项目为个人/课程作品性质的全栈实现，**非**小红书官方产品。

---

## 仓库结构

```
redbook/
├── redbook-backend/    # Spring Boot 3 后端 API
└── redbook-frontend/   # Vue 3 + Vite 单页应用
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios |
| 后端 | Java 21、Spring Boot 3.5、Spring Web / Validation、WebClient |
| 持久化 | MySQL 8、MyBatis-Plus |
| 缓存与分布式 | Redis（Lettuce）、Redisson |
| 消息 | RabbitMQ（Spring AMQP） |
| 对象存储 | 阿里云 OSS（预签名上传） |
| 接口文档 | SpringDoc OpenAPI 3、Knife4j |
| 认证 | JWT（Bearer Token） |
| AI | Dify HTTP 对话接口（可配置关闭或占位） |

其他依赖（如 Caffeine 本地缓存、Canal Starter 等）以 `pom.xml` 为准。

---

## 已实现能力（概览）

- **账号**：注册/登录（含验证码相关逻辑）、个人资料与隐私、修改密码、每日签到
- **内容**：笔记发布（多图、关联商品）、编辑/删除、发现页关注流与推荐流、详情与相关推荐
- **社交**：点赞、收藏、二级评论、关注、分享与短链跳转
- **消息**：通知（赞藏/关注/评论等）、私信、群聊
- **电商**：商品发布与管理、购物车、下单、订单、优惠券、钱包与支付相关扩展点
- **其他**：商品足迹、OSS 直传预签名、帖子场景下对接 Dify 的 AI 接口

前端开发环境下通过 Vite 将 `/user`、`/note`、`/order` 等前缀代理到后端，避免跨域。

---

## 环境要求

- **JDK 21**、**Maven 3.8+**
- **Node.js 18+**（推荐 LTS）
- **MySQL 8.0**
- **Redis**
- **RabbitMQ**（需与配置中的 `virtual-host` 等一致）

---

## 数据库初始化

1. 创建数据库（默认库名在配置中为 `redbook`，可按需修改）。
2. 执行基础结构脚本：  
   `redbook-backend/src/main/resources/db/schema.sql`
3. 若需与仓库演进后的表结构一致，请再按需执行同目录下 `migration_*.sql` 增量脚本（按你本地报错或注释说明依次补齐即可）。

---

## 后端运行

```bash
cd redbook-backend
# 按需修改 src/main/resources/application.yaml（及 application-dev.yaml）
mvn spring-boot:run
```

默认服务端口：**8081**（见 `application.yaml`）。

### 必改 / 建议改的配置

- **MySQL**：`spring.datasource.url`、`username`、`password`
- **Redis**：`spring.data.redis.*`
- **RabbitMQ**：`spring.rabbitmq.*`（含 `virtual-host`）
- **JWT**：生产环境务必更换 `jwt.secret`
- **阿里云 OSS**：在 `application-dev.yaml` 中通过环境变量配置 `ALIYUN_OSS_*`（不上传文件可先占位，但相关接口可能不可用）
- **Dify**：`dify.url`、`dify.key.note-assistant` 或环境变量 `DIFY_NOTE_KEY`

### API 文档（Knife4j）

后端启动后，浏览器访问：

`http://127.0.0.1:8081/doc.html`

（若上下文路径有变更，以实际部署为准。）

---

## 前端运行

```bash
cd redbook-frontend
npm install
npm run dev
```

默认开发服务器一般为 **http://localhost:5173**。Vite 已将 API 代理到 `http://127.0.0.1:8081`，详见 `vite.config.js`。

### 生产构建与接口基址

```bash
npm run build
npm run preview   # 本地预览；已配置与 dev 相同的 API 代理
```

生产环境推荐 **同源反向代理**（Nginx 等）转发 API；若前后端分离部署，可设置环境变量 **`VITE_API_BASE`** 为后端根地址（注意跨域与 HTTPS）。

---

## 接口约定（给联调同学）

- 统一响应体字段风格见后端 `Result` / `ScrollResult` 等 DTO。
- 需登录的接口在请求头携带：`Authorization: Bearer <token>`
- 前端 Axios 封装在 `redbook-frontend/src/api/http.js`，401 时会清理登录态并跳转登录页。

---

## 开发说明

- 后端包根：`com.zhiyan.redbookbackend`，入口类 `RedbookBackendApplication`。
- 业务接口主要位于 `controller` 包；复杂 SQL 可在 `resources/mapper/**/*.xml` 中查找。
- 项目规则与协作约定可参考仓库内 `.cursor/rules`。

---

## 许可证与声明

若你在 GitHub 上开源，请自行补充 **License** 文件（如 MIT、Apache-2.0 等），并注意：

- 勿将真实 **密钥、数据库密码、OSS Key** 提交到公开仓库；请使用环境变量或私有配置。
- 第三方商标与产品名（如「小红书」）仅用于描述产品形态，本项目与官方无关。

---

如在本地启动时遇到端口占用、代理或中间件连接失败，优先检查 `application.yaml` 与 `vite.config.js` 中的地址是否与本机一致（Windows 下 `localhost` 与 `127.0.0.1` 的 IPv4/IPv6 行为也需注意）。
