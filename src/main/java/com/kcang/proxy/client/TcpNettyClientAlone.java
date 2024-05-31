package com.kcang.proxy.client;

import com.kcang.proxy.client.handler.ClientAloneChannelInboundHandler;
import com.kcang.proxy.client.handler.ClientAloneMessageDecode;
import com.kcang.proxy.client.handler.ClientAloneMessageEncode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TcpNettyClientAlone {
    private String serverAddress;
    private int port;
    private Logger myLogger;
    private ChannelInitializer<SocketChannel> channelChannelInitializer;
    public TcpNettyClientAlone(String serverAddress, int port){
        this.serverAddress = serverAddress;
        this.port=port;
        this.channelChannelInitializer = new ChannelInitializer<SocketChannel>(){

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ClientAloneMessageDecode());
                ch.pipeline().addLast(new ClientAloneChannelInboundHandler());
                ch.pipeline().addFirst(new ClientAloneMessageEncode());
            }
        };
        this.myLogger = LoggerFactory.getLogger(this.getClass());
    }

    public void run(){
        myLogger.info("正在连接主机 "+serverAddress+":"+port);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            clientBootstrap.handler(this.channelChannelInitializer);
            clientBootstrap.remoteAddress(new InetSocketAddress(serverAddress, port));
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            myLogger.info("连接成功: "+serverAddress+":"+port);
            channelFuture.channel().closeFuture().sync();
        }catch (Exception e){
            myLogger.error("服务器主机连接失败: "+e.toString());
        }finally {
            group.shutdownGracefully();
            myLogger.info("连接结束: "+serverAddress+":"+port);
        }
    }
}
