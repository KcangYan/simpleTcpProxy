package com.kcang.proxy.client.handler;

import com.kcang.proxy.client.TcpNettyClientProxy;
import com.kcang.proxy.common.MsgQueue;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class ClientInnerChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private Logger myLogger = LoggerFactory.getLogger(this.getClass());
    private Integer id;
    private int reqCount=1;
    private int resCount=1;
    public ClientInnerChannelInboundHandler(){
        this.id = Parameters.handlerCount.getAndIncrement();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        myLogger.info(id+" 连接建立成功");
        Parameters.requestMsgMap.put(id, Parameters.getArrayQueue());
        Parameters.responseMsgMap.put(id, Parameters.getArrayQueue());
        StaticThreadPool.setTask(new TcpNettyClientProxy(Parameters.connectProxyIp, Parameters.proxyPort, id));
        StaticThreadPool.setTask(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        MsgQueue<Object> resQueue = Parameters.responseMsgMap.get(id);
                        Object msg = resQueue.take();
                        if(msg == null){
                            ctx.disconnect();
                            break;
                        }else {
                            ctx.writeAndFlush(msg);
                            myLogger.info(id+" 响应发送内部通道 "+ resCount);
                            resCount++;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                myLogger.info(id+ " 响应转发线程结束");
            }
        });
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        myLogger.info(id+" 收到请求 "+ reqCount);
        reqCount++;
        Parameters.requestMsgMap.get(id).put(msg);
    }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //通道断开连接
        myLogger.info(id+" 通道断开连接");
        Parameters.responseMsgMap.get(id).destroy();
        Parameters.responseMsgMap.remove(id);
        Parameters.requestMsgMap.get(id).destroy();
        Parameters.requestMsgMap.remove(id);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //通道处理过程异常
        myLogger.error(id +" 通道异常 "+cause.getMessage());
        cause.printStackTrace();
        ctx.disconnect();
    }
}
