**启动参数解释**
  
    1. 公共参数
    -model: client 或 server;
    -maxThread: 线程池最大线程数
    
    2. server模式下
    -port: 服务端接受内部转发的端口 不对外请求使用
    -proxyPort: 接收需要转发的请求的端口
    
    3. client模式下
    -serverIp: 服务端地址
    -port: 服务端内部通信的端口
    -proxyIp: 待转发服务的地址
    -proxyPort: 待转发服务的端口
    
**启动命令示例**

    服务端 java -jar proxy-1.0.jar -model server -port 1080 -proxyPort 8033
    客户端 java -jar proxy-1.0.jar -model client -serverIp 127.0.0.1 -port 1080 -proxyIp hsupup.com -proxyPort 8033
    
