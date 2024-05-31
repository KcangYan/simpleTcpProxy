package com.kcang.proxy.server.handler;

import com.kcang.proxy.common.MsgQueue;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private Logger myLogger = LoggerFactory.getLogger(this.getClass());

    private Runnable checkTaskSize = null;

    private Integer id;
    private int reqCount = 1;
    private int resCount = 1;
    public ServerChannelInboundHandler(){
        if(Parameters.checkNullInnerCtx()){
            id = 0;
            checkTaskSize = new Runnable() {
                @Override
                public void run() {
                    int oldSize = 0;
                    while (true){
                        try {
                            Parameters.liveTime = System.currentTimeMillis();
                            int size = Parameters.proxyIdQueue.size(2000 , TimeUnit.MILLISECONDS);
                            if(!Parameters.checkNullInnerCtx()){
                                if(size > 0 && size != oldSize){
                                    myLogger.info("通知客户端发起转发链接");
                                    Parameters.getInnerCtx().writeAndFlush(Unpooled.copiedBuffer("1"+"\001", CharsetUtil.UTF_8));
                                }
                                oldSize = size;
                            }else {
                                myLogger.error("当前无内部通信线程等待获取指令！");
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(!Parameters.checkNullInnerCtx()){
            id = Parameters.proxyIdQueue.poll();
            if(id == null) throw new Exception("无通道等待转发，结束链接");
            if(!Parameters.requestMsgMap.containsKey(id)){
                throw new Exception("无消息需要转发，结束链接");
            }
            /*MsgQueue<Object> msgQueue = Parameters.requestMsgMap.get(id);
            if(msgQueue == null || msgQueue.size() == 0){
                throw new Exception("无消息需要转发，结束链接");
            }*/
            StaticThreadPool.setTask(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            MsgQueue<Object> resQueue = Parameters.requestMsgMap.get(id);
                            Object msg = resQueue.take();//阻塞等待 有消息会自动唤醒
                            if(msg == null){
                                ctx.disconnect();
                                break;
                            }else {
                                ctx.writeAndFlush(msg);
                                myLogger.info(id+" 请求发送内部通道 "+reqCount);
                                reqCount++;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    myLogger.info(id+ " 请求转发线程结束");
                }
            });
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(id == 0){ //id为零的通道不处理客户端发来的消息
            if(Parameters.checkNullInnerCtx()){
                Parameters.setInnerCtx(ctx);
            }
            //判断id队列监听线程是否存活
            long currentTimeMillis = System.currentTimeMillis() - Parameters.liveTime;
            if(currentTimeMillis > 5*1000){
                myLogger.info("启动id队列监听线程");
                StaticThreadPool.setTask(checkTaskSize);
            }
            ctx.writeAndFlush(msg);
        }else {
            myLogger.info(id+" 收到内部转发响应 " + resCount);
            resCount++;
            //处理转发消息
            if(Parameters.responseMsgMap.containsKey(id)){
                Parameters.responseMsgMap.get(id).put(msg);
            }else {
                throw new Exception("无通道等待转发，结束链接");
            }
        }

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //通道断开连接
        myLogger.error(id+" 通道断开连接");
        if(id == 0){
            Parameters.setInnerCtx(null);
        }else {
            if(Parameters.responseMsgMap.containsKey(id)){
                Parameters.proxyIdQueue.put(id);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //通道处理过程异常
        myLogger.error(id +" 通道异常 "+cause.getMessage());
        cause.printStackTrace();
        ctx.disconnect();
    }

    /*@Override
    protected void finalize() throws Throwable {
        myLogger.info(id+" 通道销毁");
        super.finalize();
    }*/
}
