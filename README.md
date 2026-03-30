# 个人知识分享与交流平台的设计与实现

本项目为毕业设计系统实现，定位为一个以用户生成内容为核心驱动的知识社区平台，支持知识发布、内容审核、检索推荐、社区互动、个人空间、分类标签与数据激励等主要能力。

## 项目结构

- `backend`：Spring Boot 后端服务
- `frontend`：Vue 3 前端应用
- `scripts`：演示数据初始化与维护脚本
- `docs`：答辩与说明文档
- `docker-compose.yml`：本地 MySQL 与 Redis 依赖

## 当前核心能力

- 用户注册、登录、JWT 鉴权与管理员分流
- 文章、教程、问答等知识内容发布与编辑
- 敏感词拦截、管理员审核、驳回与下架
- 关键词检索、分类筛选、标签筛选与排序
- 点赞、收藏、评论、回复与问答互动
- 个人空间、粉丝关注、通知与基础推荐
- 数据报告、积分成长与后台管理

## 启动方式

### 后端

```powershell
cd backend
mvn spring-boot:run
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

### Docker 依赖

```powershell
docker compose up -d
```

## 演示账号

- 管理员：`admin_master / Admin@123456`
- 作者账号：`author_lin / User@123456`
- 作者账号：`author_qin / User@123456`
- 作者账号：`author_song / User@123456`
- 普通用户：`reader_xu / User@123456`
- 普通用户：`reader_he / User@123456`

## 说明

当前仓库已将项目对外名称统一为“个人知识分享与交流平台的设计与实现”。  
为保证系统稳定，后端 Java 包路径仍保留原有命名空间，暂未进行整仓级包名重构。
