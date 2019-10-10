package com.cooper.httpproxy.handler;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.crt.CertPool;
import com.cooper.httpproxy.handler.initializer.HttpProxyInitializer;
import com.cooper.httpproxy.handler.request.HttpRequestHandler;
import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.intercept.LocalHostIntercept;
import com.cooper.httpproxy.intercept.ProxyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

    private Channel clientChannel;
    private Channel serverChannel;
    private InetSocketAddress realAddress;
    private boolean isConnect = false;
    private List requestList;

    private HttpProxyServerConfig serverConfig;
    private HttpModifyIntercept httpModifyIntercept;
    private ProxyIntercept proxyIntercept;
    private LocalHostIntercept localHostIntercept;


    public HttpServerHandler(HttpProxyServerConfig serverConfig, HttpModifyIntercept httpModifyIntercept, ProxyIntercept proxyIntercept, LocalHostIntercept localHostIntercept) {
        this.serverConfig = serverConfig;
        this.httpModifyIntercept = httpModifyIntercept;
        this.proxyIntercept = proxyIntercept;
        this.localHostIntercept = localHostIntercept;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            realAddress = HttpProxyUtil.getAddressByRequest(req);
            HttpMethod httpMethod = req.method();

            boolean isConnect = httpMethod.equals(HttpMethod.CONNECT);
            //防止http请求的method是CONNECT
            //是否需要ssl处理
            boolean isSsl = isConnect && realAddress.getPort() == 443 ? true : false;

            /**
             * 指定本地url请求处理
             * 如下载本地ca证书
             * http://serverIP:serverPost/HttpServerConfig.downloadCACertPath
             * 例： http://192.168.1.1:8081/download
             */
            if (httpMethod.equals(HttpMethod.GET)) {
                String addressToString = realAddress.getAddress().toString();
                int index = addressToString.lastIndexOf("/");
                if (isLocalHost(addressToString.substring(index + 1), realAddress.getPort())) {
                    localHostIntercept.localhostHandler(clientChannel, HttpProxyUtil.getUrl(req.uri()));
                    return;
                }
            }
            //连接目标服务器
            ChannelFuture channelFuture = connect(isSsl,req);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("connect {}", realAddress.getHostString());
                        serverChannel = future.channel();
                        HttpServerHandler.this.isConnect = true;
                        HttpProxyUtil.removePipeline(clientChannel, HttpProxyUtil.HTTP_SERVER_HANDLER);
                        clientChannel.pipeline().addLast(HttpProxyUtil.HTTP_REQUEST_HANDLER,
                                new HttpRequestHandler(serverChannel, realAddress, httpModifyIntercept,proxyIntercept));

                        if (isConnect) {
                            HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                            clientChannel.writeAndFlush(httpResponse);
                            if (serverConfig.isHandleSsl()) {
                                if (isSsl) {
                                    SslContext sslCtx = SslContextBuilder
                                            .forServer(serverConfig.getServerPriKey(), CertPool.getCert(serverConfig.getPort(),realAddress.getHostString(), serverConfig))
                                            .build();
                                    clientChannel.pipeline().addFirst(HttpProxyUtil.HTTP_SERVER_SSL_HANDLER, sslCtx.newHandler(clientChannel.alloc()));
                                }
                            } else {
                                HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_SERVER_CODEC);
                            }
                            ReferenceCountUtil.release(msg);
                        } else{
                            clientChannel.pipeline().fireChannelRead(msg);
                        }

                        if (requestList != null) {
                            requestList.forEach(obj -> clientChannel.pipeline().fireChannelRead(obj));
                            requestList.clear();
                        }

                    } else {
                        logger.warn("fail connect {}", realAddress.getHostString());
                        ReferenceCountUtil.release(msg);
                        if (requestList != null) {
                            requestList.forEach(obj -> ReferenceCountUtil.release(obj));
                            requestList.clear();
                        }
                        HttpProxyUtil.channelClose(clientChannel, serverChannel);
                    }
                }
            });
        } else {
            if (isConnect) {
                clientChannel.pipeline().fireChannelRead(msg);
            } else {
                if (requestList == null) {
                    requestList = new LinkedList();
                }
                requestList.add(msg);
            }
        }
    }

    public boolean isLocalHost(String host, Integer port) {
        if (serverConfig.getLocalhost().contains(host) && port.equals(serverConfig.getPort())) {
            return true;
        }
        return false;
    }

    private ChannelFuture connect(boolean isSsl,HttpRequest req) {
        //获取代理服务器
        ProxyHandler proxyHandler = proxyIntercept.proxyHandler(realAddress,req);
        ChannelInitializer channelInitializer =
                new HttpProxyInitializer(clientChannel, realAddress, proxyHandler, isSsl, serverConfig);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(clientChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(channelInitializer);
        if (proxyHandler != null) {
            //代理服务器解析DNS和连接
            bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        }
        return bootstrap.connect(realAddress);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //logger.warn("{} Unregistered, channel close ", realAddress.getHostString());
        HttpProxyUtil.channelClose(clientChannel, serverChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("{} channel close {}", realAddress.getHostString(), cause.toString());
        HttpProxyUtil.channelClose(clientChannel, serverChannel);
    }

    public HttpModifyIntercept getHttpModifyIntercept() {
        return httpModifyIntercept;
    }
}