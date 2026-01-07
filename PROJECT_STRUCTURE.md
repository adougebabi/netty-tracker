# 项目结构说明

## 📁 完整目录树

```
nettytest/
├── README.md                    # 项目主文档
├── TESTING.md                   # qBittorrent 完整测试指南
├── pom.xml                      # Maven 项目配置
│
├── docker/                      # Docker 测试环境
│   ├── README.md               # Docker 环境说明
│   ├── docker-compose.yml      # 完整测试栈（Tracker + 2个qBittorrent）
│   ├── Dockerfile              # Tracker 镜像定义
│   ├── test-files/             # 测试文件目录（自动生成）
│   ├── config-seeder/          # Seeder 配置（自动生成）
│   ├── config-leecher/         # Leecher 配置（自动生成）
│   └── downloads/              # 下载目录（自动生成）
│
├── src/
│   ├── main/
│   │   ├── java/com/zhaoyang/boot/nettytest/
│   │   │   ├── bittorrent/              # BitTorrent Tracker 实现
│   │   │   │   ├── server/
│   │   │   │   │   └── BitTorrentTrackerServer.java
│   │   │   │   ├── handler/
│   │   │   │   │   └── TrackerHttpHandler.java
│   │   │   │   ├── model/
│   │   │   │   │   ├── Peer.java
│   │   │   │   │   └── TorrentManager.java
│   │   │   │   └── util/
│   │   │   │       ├── SimpleBencode.java
│   │   │   │       ├── BencodeUtil.java
│   │   │   │       └── TorrentCreator.java
│   │   │   ├── server/                  # 自定义 TCP Tracker（可选）
│   │   │   │   └── TrackerServer.java
│   │   │   ├── handler/
│   │   │   │   └── TrackerServerHandler.java
│   │   │   ├── manager/
│   │   │   │   └── ClientManager.java
│   │   │   └── protocol/
│   │   │       └── TrackerMessage.java
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── java/com/zhaoyang/boot/nettytest/
│           ├── TrackerClient.java       # 测试客户端
│           ├── TrackerBenchmark.java    # 性能测试工具
│           └── TrackerMessageTest.java  # 单元测试
│
├── start-qb-test.sh            # 一键启动 qBittorrent 测试
├── start-bt-tracker.sh         # 启动 BitTorrent Tracker
├── start-server.sh             # 启动自定义 TCP Tracker
├── create-torrent.sh           # 生成 Torrent 文件
└── run-benchmark.sh            # 运行性能测试
```

## 📄 文档说明

### 主要文档

| 文件 | 说明 | 用途 |
|------|------|------|
| **README.md** | 项目主文档 | 快速了解项目特性和使用方式 |
| **TESTING.md** | 完整测试指南 | qBittorrent 测试的详细步骤 |
| **docker/README.md** | Docker 环境说明 | Docker Compose 使用指南 |

### 代码结构

| 目录 | 说明 |
|------|------|
| `src/main/java/.../bittorrent/` | BitTorrent HTTP Tracker 实现 |
| `src/main/java/.../server/` | 自定义 TCP Tracker（可选）|
| `src/test/java/` | 测试代码 |
| `docker/` | Docker 测试环境 |

## 🚀 使用场景

### 场景 1: 快速测试 BitTorrent Tracker

```bash
./start-qb-test.sh
```

**用途**: 一键启动完整测试环境，包括 Tracker 和 2 个 qBittorrent

### 场景 2: 仅启动 Tracker

```bash
./start-bt-tracker.sh
```

**用途**: 单独运行 Tracker，配合其他 BitTorrent 客户端

### 场景 3: 自定义 TCP Tracker

```bash
./start-server.sh
```

**用途**: 运行自定义协议的 Tracker（非 BitTorrent）

### 场景 4: 性能测试

```bash
./run-benchmark.sh
```

**用途**: 测试自定义 TCP Tracker 的性能

## 🎯 核心组件

### BitTorrent Tracker (推荐)

- **端口**: 6969
- **协议**: HTTP (BEP 3 & BEP 23)
- **兼容**: qBittorrent, Transmission, Deluge 等
- **功能**: 完整的 BitTorrent Tracker

### 自定义 TCP Tracker (可选)

- **端口**: 8888 (默认)
- **协议**: 自定义文本协议 `TYPE|CLIENT_ID|PAYLOAD`
- **用途**: 学习 Netty 或自定义应用

## 📊 文件说明

### 配置文件

- `pom.xml` - Maven 依赖和构建配置
- `docker/docker-compose.yml` - Docker 服务编排
- `docker/Dockerfile` - Tracker 镜像构建
- `.gitignore` - Git 忽略规则（包含测试文件）

### 脚本文件

| 脚本 | 说明 |
|------|------|
| `start-qb-test.sh` | 一键启动完整测试环境 |
| `start-bt-tracker.sh` | 启动 BitTorrent Tracker |
| `start-server.sh` | 启动自定义 TCP Tracker |
| `create-torrent.sh` | 生成 .torrent 文件 |
| `run-benchmark.sh` | 运行性能测试套件 |

## 🔄 工作流程

### 开发流程

1. 修改代码
2. `mvn clean compile` 编译
3. `./start-bt-tracker.sh` 测试
4. 提交代码

### 测试流程

1. `./start-qb-test.sh` 启动环境
2. 访问 Web UI 添加 torrent
3. 观察下载过程
4. `cd docker && docker-compose down` 清理

### Docker 构建流程

1. `mvn clean package -DskipTests` 编译
2. `cd docker`
3. `docker-compose up -d --build` 构建并启动
4. 测试功能
5. `docker-compose down` 停止

## 💡 提示

- **测试文件**: 自动生成在 `docker/test-files/`
- **日志查看**: `docker-compose logs -f`
- **完全清理**: `cd docker && docker-compose down -v && rm -rf config-* downloads test-files`
- **修改端口**: 编辑 `docker/docker-compose.yml`

## 📖 学习路径

1. **入门**: 阅读 README.md 了解项目
2. **快速测试**: 运行 `./start-qb-test.sh`
3. **深入学习**: 阅读 TESTING.md 了解细节
4. **代码研究**: 查看 `src/main/java/.../bittorrent/`
5. **自定义**: 修改代码并测试

## 🎓 学习重点

### BitTorrent Tracker 核心

- **Announce 协议**: 如何处理 peer 注册
- **Peer 管理**: 如何存储和分配 peers
- **Bencode 编码**: BitTorrent 数据格式
- **Compact Format**: 高效的 peer 列表

### Netty 应用

- **HTTP 服务器**: HttpServerCodec 使用
- **事件驱动**: ChannelHandler 处理流程
- **异步编程**: ChannelFuture 和 Promise

### Docker 实践

- **多容器编排**: docker-compose.yml 配置
- **网络通信**: Docker 容器间通信
- **数据持久化**: 卷挂载和配置管理

---

更新日期: 2026-01-07
