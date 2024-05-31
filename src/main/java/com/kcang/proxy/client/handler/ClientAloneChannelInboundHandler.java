package com.kcang.proxy.client.handler;

import com.kcang.proxy.client.TcpNettyClientAlone;
import com.kcang.proxy.client.TcpNettyClientInner;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientAloneChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private Logger myLogger = LoggerFactory.getLogger(this.getClass());

    private Runnable heatBeatTask = new Runnable() {
        @Override
        public void run() {
            myLogger.info("进入心跳维护");
            while (true){
                Parameters.liveTime = System.currentTimeMillis();
                try {
                    Parameters.getInnerCtx().writeAndFlush("heartBeat");
                }catch (Exception e){
                    myLogger.error("心跳保持异常");
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(Parameters.checkNullInnerCtx()){
                    myLogger.error("主通信链接异常，正在尝试重连");
                    try {
                        StaticThreadPool.setTask(new TcpNettyClientAlone(Parameters.connectServerIp, Parameters.port));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        myLogger.info("连接建立成功");
        Parameters.setInnerCtx(ctx);
        long currentTimeMillis = System.currentTimeMillis() - Parameters.liveTime;
        if(currentTimeMillis > 5*1000){
            StaticThreadPool.setTask(heatBeatTask);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //myLogger.info("收到消息: "+ msg);
        if(!msg.equals("heartBeat")){
            myLogger.info("收到服务端指令，发起转发请求链接");
            StaticThreadPool.setTask(new TcpNettyClientInner(Parameters.connectServerIp,Parameters.port));
        }
        long currentTimeMillis = System.currentTimeMillis() - Parameters.liveTime;
        if(currentTimeMillis > 5*1000){
            StaticThreadPool.setTask(heatBeatTask);
        }
    }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //通道断开连接
        myLogger.info("通道断开连接");
        Parameters.setInnerCtx(null);
        long currentTimeMillis = System.currentTimeMillis() - Parameters.liveTime;
        if(currentTimeMillis > 5*1000){
            StaticThreadPool.setTask(heatBeatTask);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //通道处理过程异常
        myLogger.error(ctx.toString() +" 通道异常 "+cause.getMessage());
        Parameters.setInnerCtx(null);
        long currentTimeMillis = System.currentTimeMillis() - Parameters.liveTime;
        if(currentTimeMillis > 5*1000){
            StaticThreadPool.setTask(heatBeatTask);
        }
        cause.printStackTrace();
        ctx.disconnect();
    }
}
