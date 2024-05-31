package com.kcang.proxy.client.handler;

import com.kcang.proxy.common.MsgQueue;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class ClientProxyChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private Logger myLogger = LoggerFactory.getLogger(this.getClass());
    private Integer id;
    private int reqCount = 1;
    private int resCount = 1;
    public ClientProxyChannelInboundHandler(Integer id){
        this.id = id;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        myLogger.info(id+ " 连接建立成功");
        if(Parameters.requestMsgMap.containsKey(id)){
            StaticThreadPool.setTask(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            MsgQueue<Object> reqQueue = Parameters.requestMsgMap.get(id);
                            Object msg = reqQueue.take();
                            if(msg == null){
                                ctx.disconnect();
                                break;
                            }else {
                                ctx.writeAndFlush(msg);
                                myLogger.info(id+" 请求发送转发通道 "+reqCount);
                                reqCount++;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    myLogger.info(id+ " 请求转发线程结束");
                }
            });
        }else {
            throw new Exception("无通道等待转发，结束链接");
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        myLogger.info(id+" 收到响应 "+ resCount);
        resCount++;
        if(Parameters.responseMsgMap.containsKey(id)){
            Parameters.responseMsgMap.get(id).put(msg);
        }
    }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //通道断开连接
        myLogger.info(id + " 通道断开连接");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //通道处理过程异常
        myLogger.error(id +" 通道异常 "+cause.getMessage());
        cause.printStackTrace();
        ctx.disconnect();
    }
}
