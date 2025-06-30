# 简易聊天室

这是一个基于Java Socket的简易多人聊天室应用程序。

## 功能特性

- 用户注册和登录
- 多人实时聊天
- 在线用户列表显示
- 用户加入/离开提醒

## 项目结构

```
simple-chat/
├── common/          # 公共模块（消息类型、用户类等）
├── server/          # 服务器端
├── client/          # 客户端
└── pom.xml         # Maven父项目配置
```

## 运行说明

### 1. 编译项目
```bash
cd simple-chat
mvn clean compile
```

### 2. 启动服务器
```bash
cd server
mvn exec:java -Dexec.mainClass="com.simplechat.server.Server"
```
或者运行编译后的jar文件：
```bash
java -cp target/classes:../common/target/classes com.simplechat.server.Server
```

### 3. 启动客户端
```bash
cd client
mvn exec:java -Dexec.mainClass="com.simplechat.client.LoginWindow"
```
或者运行编译后的jar文件：
```bash
java -cp target/classes:../common/target/classes com.simplechat.client.LoginWindow
```

## 使用方法

1. 启动服务器
2. 启动客户端程序
3. 注册新用户或使用已有账户登录
4. 在聊天窗口中输入消息并发送
5. 查看右侧在线用户列表

## 技术特点

- 使用Java Socket进行网络通信
- Swing GUI界面
- 多线程处理客户端连接
- 消息广播机制
- 用户状态管理

## 注意事项

- 服务器默认运行在8888端口
- 客户端默认连接本地服务器(127.0.0.1)
- 用户数据存储在服务器端的users.txt文件中