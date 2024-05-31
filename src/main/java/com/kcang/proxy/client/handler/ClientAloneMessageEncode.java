package com.kcang.proxy.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class ClientAloneMessageEncode extends MessageToMessageEncoder {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List out) throws Exception {
        String message = msg.toString();
        message = message+"\001";
        ByteBuf buf = ctx.alloc().buffer();
        out.add(buf.writeBytes(message.getBytes(CharsetUtil.UTF_8)));
        //writeAndFlush(Unpooled.copiedBuffer(" ", CharsetUtil.UTF_8))
    }
}
