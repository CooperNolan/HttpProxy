package com.cooper.httpproxy.handler;

import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


public class HttpResponseHandler extends ChannelInboundHandlerAdapter {


    private static Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    private Channel clientChannel;
    private Channel serverChannel;
    private InetSocketAddress realAddress;
    private HttpModifyIntercept httpModifyIntercept;

    public HttpResponseHandler(Channel clientChannel, InetSocketAddress realAddress, HttpModifyIntercept httpModifyIntercept) {
        this.clientChannel = clientChannel;
        this.realAddress = realAddress;
        this.httpModifyIntercept = httpModifyIntercept;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        serverChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpResponse) {
            httpModifyIntercept.beforeResponse((HttpResponse) msg);
            if (msg instanceof FullHttpResponse) {
                httpModifyIntercept.beforeResponse((FullHttpResponse) msg);
            }
        } else if (msg instanceof HttpContent) {
            httpModifyIntercept.beforeResponse((HttpContent) msg);
        }
        clientChannel.writeAndFlush(msg);

        //websocket转发原始报文
        if (msg instanceof HttpResponse) {
            if (HttpHeaderValues.WEBSOCKET.toString()
                    .equals(((HttpResponse)msg).headers().get(HttpHeaderNames.UPGRADE))) {
                HttpProxyUtil.removePipeline(serverChannel,HttpProxyUtil.HTTP_OBJECT_AGGREGATOR);
                HttpProxyUtil.removePipeline(serverChannel,HttpProxyUtil.HTTP_CLIENT_CODEC);
                HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_OBJECT_AGGREGATOR);
                HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_SERVER_CODEC);
            }
        }
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