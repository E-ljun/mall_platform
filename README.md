# 智能商品详情与营销文案生成系统

前后端分离：Spring Boot + Vue3 + MySQL，默认接入阿里云通义千问（DashScope）。

## 本地开发

### 1. 数据库

```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
```

### 2. 配置 AI Key

复制 `backend/.env.example`，设置环境变量 `DASHSCOPE_API_KEY`（[百炼控制台](https://dashscope.aliyun.com/) 申请）。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 ，默认管理员 `admin` / `admin123`。

## 服务器部署（Docker）

```bash
cd mall-platform
cp .env.example .env
# 编辑 .env 填写 DASHSCOPE_API_KEY 和 JWT_SECRET

docker compose up -d --build
```

浏览器访问 `http://服务器IP` 即可使用。

## 浏览器兼容

构建已启用 `@vitejs/plugin-legacy`，兼容 Chrome 64+、Edge 79+、QQ 浏览器（Chromium 内核）、Firefox 68+。

## API 文档

开发环境：http://localhost:8080/api/doc.html
