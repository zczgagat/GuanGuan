# 图谱构建

一个支持**族谱**、**ER图**、**AOE网**三种模板的可视化图谱构建工具，基于 Java + Vue 开发。

## 功能特色

### 🏠 多模板支持
| 模板 | 说明 |
|------|------|
| **族谱** | 自动计算五代内亲属关系，支持堂/表区分、兄弟姐妹排序 |
| **ER图** | 实体-关系图，支持实体/属性/关系节点，基数标注与爪型线 |
| **AOE网** | 活动网络图，自动计算拓扑排序、关键路径、事件时间表 |

### 🎨 可视化
- 基于 Cytoscape.js 的交互式图谱
- 节点拖拽、缩放、选中高亮
- 悬停放大动效 + 点击彩虹火花特效
- 5 种主题色切换（蓝/绿/橙/紫/暗夜）
- 鼠标周围绿色光晕跟随

### 👤 用户系统
- 注册 / 登录
- 个人中心：修改用户名、密码、头像
- 多图谱管理：创建、删除、切换

### 📦 数据导入导出
- 导出为 `.mygraph` 文件（自定义格式）
- 导入 `.mygraph` 文件自动重建图谱
- 完整保留模板类型、节点类型、关系基数、工期等数据

### 🧠 图谱特色

**族谱模式**
- 定义 6 种基础关系（父/母/子/女/夫/妻）
- 自动推理 7 代以内亲属关系（爷爷/奶奶/叔叔/堂哥/表哥等）
- 父系/母系自动判定（堂 vs 表）
- 兄弟姐妹出生排名编辑
- 双击关系可编辑计算关系
- 头像上传（圆形裁剪），无头像显示名字首字

**ER 图模式**
- 实体（圆角矩形）、属性（椭圆）、关系（菱形）三种节点类型
- 实体↔实体：自动生成菱形关系节点 + 两条边
- 基数标注：1:1 / 1:N / N:M
- 爪型线：任意端数量 > 1 自动显示
- 单端基数与双端基数智能切换

**AOE 网模式**
- 事件节点（绿色圆形）+ 活动边（含工期）
- 拓扑排序计算
- 关键路径分析（自动标识关键活动）
- 事件时间表（ES / EF / LS / LF / 时差）
- 活动表（耗时 / 是否关键）

### ✨ 交互特效
- 点击任意位置 → 彩虹色火花四溅（中心闪光 + 粒子拖尾）
- 鼠标光晕 → 绿色径向渐变跟随
- 悬停动效 → 组件放大 + 阴影加深
- 信息面板滑入动画

## 技术栈

### 后端
- Java 17 + Spring Boot 3.2
- Spring Security（Token 认证）
- Spring Data JPA + H2（内置数据库）
- H2 控制台：`http://localhost:8080/h2-console`

### 前端
- Vue 3 + Vite
- Element Plus（UI 组件库）
- Cytoscape.js（图谱可视化）
- Axios（HTTP 请求）

## 快速启动

### 环境要求
- JDK 17+
- Node.js 18+
- Maven 3.8+

### 方式一：一键启动
双击 `startv2.bat`，自动完成：
1. 启动后端（`mvn spring-boot:run`）
2. 安装前端依赖（`npm install`）
3. 启动前端（`npm run dev`）
4. 打开浏览器

### 方式二：手动启动

**后端**：
```bash
cd backend
mvn spring-boot:run
```

**前端**：
```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`

## 项目结构

```
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/familytree/
│   │   ├── controller/               # REST API 控制器
│   │   ├── service/                  # 业务逻辑层
│   │   ├── model/                    # 数据实体
│   │   ├── repository/               # 数据访问层
│   │   ├── graph/                    # 图结构与路径查找
│   │   ├── relationship/             # 关系推理算法
│   │   └── config/                   # 安全与跨域配置
│   └── src/main/resources/
│       └── application.yml           # 应用配置
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── components/               # 公共组件
│   │   ├── views/                    # 页面视图
│   │   ├── api/                      # API 接口
│   │   └── styles/                   # 全局样式
├── startv2.bat                       # 一键启动脚本
└── README.md
```

## API 接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/me` | 当前用户信息 |
| PUT | `/api/auth/password` | 修改密码 |
| PUT | `/api/auth/username` | 修改用户名 |
| POST | `/api/auth/avatar` | 上传头像 |

### 图谱管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/trees` | 获取用户图谱列表 |
| POST | `/api/trees` | 创建图谱 |
| DELETE | `/api/trees/{id}` | 删除图谱 |

### 人物/事件/实体
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/persons` | 获取人物列表 |
| POST | `/api/persons` | 新增人物 |
| PUT | `/api/persons/{id}` | 修改信息 |
| DELETE | `/api/persons/{id}` | 删除人物 |
| PUT | `/api/persons/{id}/sibling-rank` | 设置兄弟姐妹排名 |

### 关系/边
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/relationships` | 获取关系列表 |
| POST | `/api/relationships` | 新增关系 |
| DELETE | `/api/relationships/{id}` | 删除关系 |

### 家族计算
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/family/graph` | 获取图谱数据 |
| GET | `/api/family/calculate-relationship` | 计算两人关系 |
| POST | `/api/family/auto-connect` | 添加人物并自动关联 |
| POST | `/api/family/recompute` | 重新计算所有关系 |
| GET | `/api/family/aoe/{treeId}` | AOE 网关键路径计算 |

### 导入导出
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/export-tree/{treeId}` | 导出图谱 |
| POST | `/api/import-tree` | 导入图谱 |

### AI
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/query-relationship` | AI 查询关系介绍 |

## 配置说明

数据库默认使用 **H2 文件数据库**（无需安装），数据存储在 `backend/data/` 目录。

切换为 MySQL：取消 `application.yml` 中 MySQL 配置的注释，注释掉 H2 配置。

头像上传目录：`backend/uploads/avatars/`

## 许可证

MIT
