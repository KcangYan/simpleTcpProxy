package com.kcang.proxy.common;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Parameters {
    //public static String model="server";
    public static String model="client";
    public static int port = 1081;//内部通信端口
    public static int proxyPort = 8033;//server模式下就是接收公网请求的端口 client模式下就是待转发的服务的端口
    public static String connectServerIp = "127.0.0.1"; //客户端链接的公网(内部交互)服务器的地址
    public static String connectProxyIp = "47.92.24.185"; //客户端需要转发的服务的地址
    public static AtomicInteger handlerCount = new AtomicInteger(1);
    public static int maxThread = 50;
    public static String logLevel = "info";
    /**
     * 主线程用来交互的通道，通知客户端建立链接转发消息
     */
    private static ChannelHandlerContext innerCtx = null;
    public static long liveTime = 0;
    private static ReentrantLock lock = new ReentrantLock();
    public static void setInnerCtx(ChannelHandlerContext ctx){
        lock.lock();
        try {
            innerCtx = ctx;
        }finally {
            lock.unlock();
        }
    }
    public static ChannelHandlerContext getInnerCtx(){
        lock.lock();
        try {
            return innerCtx;
        }finally {
            lock.unlock();
        }
    }
    public static boolean checkNullInnerCtx(){
        lock.lock();
        try {
            return innerCtx == null;
        }finally {
            lock.unlock();
        }
    }

    public static MsgQueue<Object> getArrayQueue(){
        return new MsgQueue<Object>(439);
    }

    /**
     * 给每个链接proxy服务端的链接分配id，然后存到队列里面等待内部建立新的链接消费消息
     */
    public static MsgQueue<Integer> proxyIdQueue = new MsgQueue<>(4396);
    /**
     * 每个proxy的链接的id分配一条内部建立的链接消费队列里的消息
     */
    public static ConcurrentHashMap<Integer, MsgQueue<Object>> requestMsgMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, MsgQueue<Object>> responseMsgMap = new ConcurrentHashMap<>();
}
