package com.kcang.proxy.client.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class ClientAloneMessageDecode extends ByteToMessageDecoder {
    private String messages = "";
    private String end = "\001";
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf bufMsg = in.readBytes(in.readableBytes());
        String getMsg = messages + bufMsg.toString(CharsetUtil.UTF_8);
        String[] ss;
        while (getMsg.contains(end)){
            //切割字符串，可以适应多条消息粘结在一起的情况
            ss = getMsg.split(end,2);
            out.add(ss[0]);
            getMsg = ss[1];
        }
        messages = getMsg;
    }
}
