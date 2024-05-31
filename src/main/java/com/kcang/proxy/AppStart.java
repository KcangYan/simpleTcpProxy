package com.kcang.proxy;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.kcang.proxy.client.TcpNettyClientAlone;
import com.kcang.proxy.client.TcpNettyClientInner;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import com.kcang.proxy.server.TcpNettyServerInner;
import com.kcang.proxy.server.TcpNettyServerProxy;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AppStart {
    private static Logger myLogger = LoggerFactory.getLogger(AppStart.class);
    public static void main(String[] args) throws Exception {
        for(int i=0; i<args.length; i++){
            if(i == (args.length -1 )){
                break;
            }
            if(args[i].equals("-model")){
                Parameters.model = args[i+1];
                if(!Parameters.model.equals("client") && !Parameters.model.equals("server")){
                    myLogger.warn("参数 -model 异常，输入 client 或者 server");
                    throw new Exception("参数 -model 异常，输入 client 或者 server");
                }
            }
            if(args[i].equals("-port")){
                Parameters.port = Integer.parseInt(args[i+1]);
            }
            if(args[i].equals("-proxyPort")){
                Parameters.proxyPort = Integer.parseInt(args[i+1]);
            }
            if(args[i].equals("-serverIp")){
                Parameters.connectServerIp = args[i+1];
            }
            if(args[i].equals("-maxThread")){
                Parameters.maxThread = Integer.parseInt(args[i+1]);
            }
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger("root");
        logger.setLevel(Level.toLevel(Parameters.logLevel)) ;

        myLogger.info("启动模式: "+Parameters.model);
        myLogger.info("运行端口: "+Parameters.port);
        if(Parameters.model.equals("client")){
            myLogger.info("接收服务器地址: "+Parameters.connectServerIp);
        }

        if(Parameters.model.equals("server")){
            StaticThreadPool.setTask(new TcpNettyServerProxy());
            StaticThreadPool.setTask(new TcpNettyServerInner());
        }
        if(Parameters.model.equals("client")){
            StaticThreadPool.setTask(new TcpNettyClientAlone(Parameters.connectServerIp, Parameters.port));
        }
    }
}
