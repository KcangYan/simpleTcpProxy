package com.kcang.proxy.server;

import com.kcang.proxy.common.Parameters;
import com.kcang.proxy.server.handler.ServerChannelInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class TcpNettyServerInner {
    private Logger myLogger = LoggerFactory.getLogger(TcpNettyServerProxy.class);

    private ChannelInitializer<SocketChannel> channelChannelInitializer;
    private boolean isRestart = true;
    private ChannelFuture channelFuture;

    public TcpNettyServerInner(ChannelInitializer<SocketChannel> channelChannelInitializer){
        this.channelChannelInitializer = channelChannelInitializer;
    }
    public TcpNettyServerInner(){
        this.channelChannelInitializer = new ChannelInitializer<SocketChannel>(){

            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ServerChannelInboundHandler());
            }
        };
    }

    public void run() throws InterruptedException {
        myLogger.info("正在启动NettyServer: "+ Parameters.port);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.localAddress(new InetSocketAddress("0.0.0.0",Parameters.port));

            serverBootstrap.childHandler(this.channelChannelInitializer);//填充channelHandler

            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            this.channelFuture = channelFuture;
            myLogger.info("NettyServer服务启动成功端口: " + Parameters.port);
            channelFuture.channel().closeFuture().sync();
        }catch (Exception e){
            myLogger.error("NettyServer启动失败: "+e.getMessage() + "port: "+Parameters.port);
            e.printStackTrace();
        }finally {
            workGroup.shutdownGracefully().sync();
            bossGroup.shutdownGracefully().sync();
            myLogger.error("NettyServer服务终止！端口: "+Parameters.port);
            if(this.isRestart){
                myLogger.info("正在重启NettyServer服务器 端口: "+Parameters.port);
                Thread.sleep(5000);
                this.run();
            }
        }
    }
    public void shutdown(){
        this.isRestart = false;
        this.channelFuture.channel().close();
        myLogger.info("成功关闭服务器");
    }

    public Channel getChannel(){
        return this.channelFuture.channel();
    }
}
