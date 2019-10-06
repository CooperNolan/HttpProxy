package com.cooper.httpproxy.handler;

import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private Channel clientChannel;
    private Channel serverChannel;
    private InetSocketAddress realAddress;
    private HttpModifyIntercept httpModifyIntercept;

    public HttpRequestHandler(Channel serverChannel, InetSocketAddress realAddress, HttpModifyIntercept httpModifyIntercept) {
        this.serverChannel = serverChannel;
        this.realAddress = realAddress;
        this.httpModifyIntercept = httpModifyIntercept;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            httpModifyIntercept.beforeRequest((HttpRequest) msg);
            if (msg instanceof FullHttpRequest) {
                httpModifyIntercept.beforeRequest((FullHttpRequest) msg);
            }
        } else if (msg instanceof HttpContent) {
            httpModifyIntercept.beforeRequest((HttpContent) msg);
        }
        serverChannel.writeAndFlush(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //logger.warn("{} Unregistered, channel close ", realAddress.getHostString());
        HttpProxyUtil.channelClose(clientChannel,serverChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("{} channel close {}", realAddress.getHostString(), cause.toString());
        HttpProxyUtil.channelClose(clientChannel,serverChannel);
    }
}