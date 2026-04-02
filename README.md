# 个人知识分享与交流平台的设计与实现

本项目为毕业设计系统实现，定位为一个以用户生成内容为核心驱动的知识社区平台，支持知识发布、内容审核、检索推荐、社区互动、个人空间、分类标签与数据激励等主要能力。

## 项目结构

- `backend`：Spring Boot 后端服务
- `frontend`：Vue 3 前端应用
- `scripts`：演示数据初始化与维护脚本
- `docs`：答辩说明与发布文档
- `docker-compose.yml`：前后端与数据库缓存的一体化 Docker 编排

## 当前核心能力

- 用户注册、登录、JWT 鉴权与管理员分流
- 文章、教程、问答等知识内容发布与编辑
- 敏感词拦截、管理员审核、驳回与下架
- 关键词检索、分类筛选、标签筛选与排序
- 点赞、收藏、评论、回复与问答互动
- 个人空间、粉丝关注、通知与基础推荐
- 数据报告、积分成长与后台管理

## 启动方式

### Docker 一键启动

```powershell
.\scripts\start_docker_stack.ps1
```

上面的脚本会直接执行：

```powershell
docker compose up --build -d
```

执行前请先确认 Docker Desktop 已启动，并且界面中显示 `Engine running`。

启动完成后可访问：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`

常用辅助命令：

```powershell
docker compose ps
docker compose logs -f
docker compose down
```

### 本地开发启动

#### 后端

```powershell
cd backend
mvn spring-boot:run
```

#### 前端

```powershell
cd frontend
npm install
npm run dev
```

#### 仅启动数据库与缓存依赖

```powershell
docker compose up -d mysql redis
```

## 演示数据初始化

默认启动后端与前端时，系统只会初始化表结构、分类、标签与敏感词等基础数据。
README 中列出的演示账号、文章、评论与推荐测试数据不会自动写入本地 H2 数据库，
需要在项目根目录额外执行以下脚本：

```powershell
.\scripts\setup_demo_environment.ps1
```

上面的脚本会创建基础演示账号，并生成首批演示文章、评论、点赞与收藏数据。

如果还需要登录页中的推荐测试账号与补充文章，请继续执行：

```powershell
.\scripts\seed_recommendation_data.ps1
```

如果 PowerShell 提示脚本执行被禁用，可仅对当前进程临时放行后再运行：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\setup_demo_environment.ps1
.\scripts\seed_recommendation_data.ps1
```

## 演示账号

### 基础演示账号

执行 `scripts/setup_demo_environment.ps1` 后可用：

- 管理员：`admin_master / Admin@123456`
- 作者账号：`author_lin / User@123456`
- 作者账号：`author_qin / User@123456`
- 作者账号：`author_song / User@123456`
- 普通用户：`reader_xu / User@123456`
- 普通用户：`reader_he / User@123456`

### 推荐功能测试账号

执行 `scripts/seed_recommendation_data.ps1` 后可用：

- 新增作者：`author_chen / User@123456`
- 新增作者：`author_luo / User@123456`
- 新增作者：`author_gu / User@123456`
- 新增读者：`reader_tan / User@123456`
- 新增读者：`reader_zhou / User@123456`
- 新增读者：`reader_wu / User@123456`

## 演示数据脚本

- 重置基础演示环境：`scripts/setup_demo_environment.ps1`
- 替换为中文演示数据：`scripts/replace_demo_data_cn.sql`
- 补充推荐测试数据：`scripts/seed_recommendation_data.ps1`

## 说明

当前仓库已经统一采用“个人知识分享与交流平台”的对外命名。
为了保证系统稳定，后端 Java 包路径仍保留原有命名空间，暂未进行整仓级包名重构。

当前 `scripts/setup_demo_environment.ps1` 与 `scripts/seed_recommendation_data.ps1` 默认面向本地 H2 开发模式。
如果你使用 Docker 一键启动，后端会改为连接 Docker 中的 MySQL 与 Redis，因此现有演示数据脚本不会自动写入 Docker 的 MySQL 数据库。
