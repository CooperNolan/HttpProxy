package com.cooper.httpproxy.handler.response;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;

public class HttpBeforeResponseHandler extends ChannelInboundHandlerAdapter {

    private Channel serverChannel;
    private HttpProxyServerConfig serverConfig;

    public HttpBeforeResponseHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.serverChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            try {
                Integer contentLength = Integer.parseInt(((HttpResponse) msg).headers().get(HttpHeaderNames.CONTENT_LENGTH));
                if (contentLength > serverConfig.getMaxContentLengthResponse()) {
                    HttpProxyUtil.removePipeline(serverChannel,HttpProxyUtil.HTTP_BEFORE_RESPONSE_HANDLER);
                    HttpProxyUtil.removePipeline(serverChannel,HttpProxyUtil.HTTP_OBJECT_AGGREGATOR);
                    serverChannel.pipeline().fireChannelRead(msg);
                    return;
                }
            } catch (Exception e) {
                HttpProxyUtil.removePipeline(serverChannel,HttpProxyUtil.HTTP_BEFORE_RESPONSE_HANDLER);
                serverChannel.pipeline().fireChannelRead(msg);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}