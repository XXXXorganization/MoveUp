系统架构图（使用mermaid绘制）
mermaid代码如下：

flowchart TB
    %% 样式类定义
    classDef client fill:#e1f5fe,stroke:#01579b,stroke-width:2px,color:#01579b,font-weight:bold;
    classDef gateway fill:#fff9c4,stroke:#fbc02d,stroke-width:2px,color:#b26a00,font-weight:bold;
    classDef service fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#1b5e20,font-weight:bold;
    classDef data fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#4a148c,font-weight:bold;
    classDef async fill:#ffebee,stroke:#c62828,stroke-width:2px,color:#b71c1c,font-weight:bold;

    subgraph Client["📱 客户端层"]
        direction LR
        A1["React Native App<br/>iOS / Android"]
        A2["Web 管理后台"]
        A3["第三方 SDK<br/>微信 / Apple"]
    end

    subgraph Gateway["🚪 API 网关层"]
        direction LR
        B["Nginx / Express Gateway<br/>限流 · 鉴权 · 日志 · 负载均衡"]
    end

    subgraph Service["⚙️ 业务服务层"]
        direction LR
        C1["👤 用户服务"]
        C2["🏃 运动服务"]
        C3["💬 社交服务"]
        C4["🧠 智能指导服务"]
        C5["🏆 挑战与激励服务"]
    end

    subgraph StorageLayer["🗄️ 存储与异步层"]
        direction LR
        subgraph Data["数据服务"]
            direction TB
            D1[("PostgreSQL")]
            D2[("InfluxDB")]
            D3[("Redis")]
            D4[("OSS")]
        end
        subgraph Async["异步处理"]
            direction TB
            E["RabbitMQ"]
        end
    end

    Client --> Gateway --> Service
    Service --> StorageLayer

    class A1,A2,A3 client;
    class B gateway;
    class C1,C2,C3,C4,C5 service;
    class D1,D2,D3,D4 data;
    class E async;
