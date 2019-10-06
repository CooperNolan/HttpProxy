package com.cooper.httpproxy.handler;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;


public class HttpBeforeRequestHandler extends ChannelInboundHandlerAdapter {

    private Channel clientChannel;
    private HttpProxyServerConfig serverConfig;

    public HttpBeforeRequestHandler(HttpProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            try {
                Integer contentLength = Integer.parseInt(((HttpRequest) msg).headers().get(HttpHeaderNames.CONTENT_LENGTH));
                if (contentLength > serverConfig.getMaxContentLengthRequest()) {
                    HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_BEFORE_REQUEST_HANDLER);
                    HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_OBJECT_AGGREGATOR);
                    clientChannel.pipeline().fireChannelRead(msg);
                    return;
                }
            } catch (Exception e) {
                HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_BEFORE_REQUEST_HANDLER);
                clientChannel.pipeline().fireChannelRead(msg);
                return;
            }
        }
        ctx.fireChannelRead(msg);
    }
}