package com.cooper.httpproxy.handler.initializer;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.handler.response.HttpBeforeResponseHandler;
import com.cooper.httpproxy.handler.response.HttpResponseHandler;
import com.cooper.httpproxy.handler.HttpServerHandler;
import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.proxy.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class HttpProxyInitializer extends ChannelInitializer {

    private static Logger logger = LoggerFactory.getLogger(HttpProxyInitializer.class);

    private Channel clientChannel;
    private InetSocketAddress realAddress;
    private ProxyHandler proxyHandler;
    private boolean isSsl;
    private HttpProxyServerConfig serverConfig;

    public HttpProxyInitializer(Channel clientChannel,
                                InetSocketAddress realAddress,
                                ProxyHandler proxyHandler,
                                boolean isSsl,
                                HttpProxyServerConfig serverConfig) {
        this.clientChannel = clientChannel;
        this.realAddress = realAddress;
        this.proxyHandler = proxyHandler;
        this.isSsl = isSsl;
        this.serverConfig = serverConfig;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        HttpModifyIntercept httpModifyIntercept =
                ((HttpServerHandler) clientChannel.pipeline().get(HttpProxyUtil.HTTP_SERVER_HANDLER)).getHttpModifyIntercept();
        if (proxyHandler != null) {
            channel.pipeline().addLast(proxyHandler);
        }
        if (isSsl && !serverConfig.isHandleSsl()) {
            channel.pipeline().addLast(HttpProxyUtil.HTTP_RESPONSE_HANDLER, new HttpResponseHandler(clientChannel,realAddress,httpModifyIntercept));
            return;
        }
        if (isSsl && serverConfig.isHandleSsl()) {
            channel.pipeline().addLast(HttpProxyUtil.HTTP_CLIENT_SSL_HANDLER,serverConfig.getClientSslCtx().newHandler(channel.alloc()));
        }
        channel.pipeline().addLast(HttpProxyUtil.HTTP_CLIENT_CODEC, new HttpClientCodec());
        channel.pipeline().addLast(HttpProxyUtil.HTTP_BEFORE_RESPONSE_HANDLER,new HttpBeforeResponseHandler(serverConfig));
        channel.pipeline().addLast(HttpProxyUtil.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(serverConfig.getMaxContentLengthResponse()));
        channel.pipeline().addLast(HttpProxyUtil.HTTP_RESPONSE_HANDLER, new HttpResponseHandler(clientChannel,realAddress,httpModifyIntercept));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("{} channel close {}", realAddress.getHostString(), cause.toString());
    }
}