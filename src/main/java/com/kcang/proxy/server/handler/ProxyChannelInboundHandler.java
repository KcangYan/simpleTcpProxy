package com.kcang.proxy.server.handler;

import com.kcang.proxy.common.MsgQueue;
import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.common.StaticThreadPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

public class ProxyChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private Logger myLogger = LoggerFactory.getLogger(this.getClass());

    private int id;

    private int reqCount = 1;
    private int resCount = 1;
    public ProxyChannelInboundHandler(){
        this.id = Parameters.handlerCount.getAndIncrement();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        myLogger.info(id + " 请求连接成功");
        Parameters.requestMsgMap.put(id, Parameters.getArrayQueue());
        MsgQueue<Object> res = Parameters.getArrayQueue();
        Parameters.responseMsgMap.put(id, res);
        StaticThreadPool.setTask(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Object msg = res.take();
                        if(msg == null){
                            ctx.disconnect();
                            break;
                        }else {
                            ctx.writeAndFlush(msg);
                            myLogger.info(id+" 响应转发成功 "+resCount);
                            resCount++;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                myLogger.info(id+" 响应转发线程结束");
            }
        });
        Parameters.proxyIdQueue.put(id);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        myLogger.info(id+" 收到请求 "+reqCount);
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

    /*@Override
    protected void finalize() throws Throwable {
        myLogger.info(id+" 通道销毁");
        super.finalize();
    }*/
}
