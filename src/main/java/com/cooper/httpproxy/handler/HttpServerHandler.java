package com.cooper.httpproxy.handler;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.crt.CertPool;
import com.cooper.httpproxy.crt.CertUtil;
import com.cooper.httpproxy.handler.initializer.HttpProxyInitializer;
import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.intercept.ProxyHandlerIntercept;
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
import java.security.cert.CertificateException;
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
    private ProxyHandlerIntercept proxyHandlerIntercept;


    public HttpServerHandler(HttpProxyServerConfig serverConfig,
                             HttpModifyIntercept httpModifyIntercept,
                             ProxyHandlerIntercept proxyHandlerIntercept) {
        this.serverConfig = serverConfig;
        this.httpModifyIntercept = httpModifyIntercept;
        this.proxyHandlerIntercept = proxyHandlerIntercept;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            HttpMethod httpMethod = req.method();

            realAddress = HttpProxyUtil.getAddressByRequest(req);

            /**
             * 指定本地请求下载证书
             * http://serverIP:serverPost/HttpServerConfig.downloadCACertPath
             * 例： http://192.168.1.1:8081/download
             */
            if (serverConfig.isHandleSsl() && httpMethod.equals(HttpMethod.GET)) {
                String addressToString = realAddress.getAddress().toString();
                //  192.168.1.1:8081/download  "/"下标
                int index = addressToString.lastIndexOf("/");
                if (isLocalHost(addressToString.substring(index + 1), realAddress.getPort())) {
                    downloadCACert(req.uri());
                    return;
                }
            }
            boolean isHttp = !httpMethod.equals(HttpMethod.CONNECT);
            //连接目标服务器
            ChannelFuture channelFuture = connect(isHttp,req);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info("connect {}", realAddress.getHostString());
                        serverChannel = future.channel();
                        isConnect = true;
                        HttpProxyUtil.removePipeline(clientChannel, HttpProxyUtil.HTTP_SERVER_HANDLER);
                        clientChannel.pipeline().addLast(HttpProxyUtil.HTTP_REQUEST_HANDLER,
                                new HttpRequestHandler(serverChannel, realAddress, httpModifyIntercept));

                        if (isHttp) {
                            clientChannel.pipeline().fireChannelRead(msg);
                        } else {
                            HttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                            clientChannel.writeAndFlush(httpResponse);
                            if (serverConfig.isHandleSsl()) {
                                SslContext sslCtx = SslContextBuilder
                                        .forServer(serverConfig.getServerPriKey(), CertPool.getCert(serverConfig.getPort(),realAddress.getHostString(), serverConfig))
                                        .build();
                                clientChannel.pipeline().addFirst(HttpProxyUtil.HTTP_SERVER_SSL_HANDLER, sslCtx.newHandler(clientChannel.alloc()));
                            } else {
                                HttpProxyUtil.removePipeline(clientChannel,HttpProxyUtil.HTTP_SERVER_CODEC);
                            }
                            ReferenceCountUtil.release(msg);
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

    private ChannelFuture connect(boolean isHttp,HttpRequest req) {
        //获取代理服务器
        ProxyHandler proxyHandler = proxyHandlerIntercept.proxyHandler(realAddress,req);
        ChannelInitializer channelInitializer =
                new HttpProxyInitializer(clientChannel, realAddress, proxyHandler, isHttp, serverConfig);
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

    public void downloadCACert(String uri) throws CertificateException {
        if (uri.matches("^.*" + serverConfig.getDownloadCACertPath() + "/ca.crt$")) {
            HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK);
            byte[] bts = CertUtil
                    .loadCert(Thread.currentThread().getContextClassLoader().getResourceAsStream("cacert/ca.crt"))
                    .getEncoded();
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-x509-ca.crt-cert");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bts.length);
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            HttpContent httpContent = new DefaultLastHttpContent();
            httpContent.content().writeBytes(bts);
            clientChannel.writeAndFlush(httpResponse);
            clientChannel.writeAndFlush(httpContent);
            clientChannel.close();
        } else if (uri.matches("^.*" + serverConfig.getDownloadCACertPath() + "$")) {
            HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK);
            String html = "<html><body><div style=\"margin-top:100px;text-align:center;\"><a href=\"" +
                    serverConfig.getDownloadCACertPath() +
                    "/ca.crt\">点击下载证书</a></div></body></html>";
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, html.getBytes().length);
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            HttpContent httpContent = new DefaultLastHttpContent();
            httpContent.content().writeBytes(html.getBytes());
            clientChannel.writeAndFlush(httpResponse);
            clientChannel.writeAndFlush(httpContent);
        } else if (uri.matches("^.*/favicon.ico$")) {
            clientChannel.close();
        }
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