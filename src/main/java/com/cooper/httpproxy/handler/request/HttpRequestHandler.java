package com.cooper.httpproxy.handler.request;

import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.intercept.ProxyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private Channel clientChannel;
    private Channel serverChannel;
    private InetSocketAddress realAddress;
    private HttpModifyIntercept httpModifyIntercept;
    private ProxyIntercept proxyIntercept;

    public HttpRequestHandler(Channel serverChannel, InetSocketAddress realAddress, HttpModifyIntercept httpModifyIntercept, ProxyIntercept proxyIntercept) {
        this.serverChannel = serverChannel;
        this.realAddress = realAddress;
        this.httpModifyIntercept = httpModifyIntercept;
        this.proxyIntercept = proxyIntercept;
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
                HttpHost urlProxy = proxyIntercept.urlProxyServer(realAddress, (HttpRequest) msg);
                if (urlProxy != null) {
                    FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
                    CloseableHttpClient httpClient = null;
                    CloseableHttpResponse httpResponse = null;
                    try {
                        //采用绕过验证的方式处理https请求
                        SSLContext sslcontext = HttpProxyUtil.createIgnoreVerifySSL();
                        //设置协议http和https对应的处理socket链接工厂的对象
                        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                                .register("http", PlainConnectionSocketFactory.INSTANCE)
                                .register("https", new SSLConnectionSocketFactory(sslcontext))
                                .build();
                        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
                        httpClient = HttpClients.custom().setConnectionManager(connManager).build();
                        HttpUriRequest httpUriRequest = HttpProxyUtil.createHttpUriRequest(realAddress,urlProxy,fullHttpRequest);
                        httpResponse = httpClient.execute(httpUriRequest);
                        HttpResponse nettyHttpResponse = HttpProxyUtil.httpResponseConversion(httpResponse);
                        httpModifyIntercept.beforeResponse(nettyHttpResponse);
                        httpModifyIntercept.beforeResponse((FullHttpResponse) nettyHttpResponse);
                        clientChannel.writeAndFlush(nettyHttpResponse);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    } finally {
                        ReferenceCountUtil.release(msg);
                        httpClient.close();
                        httpResponse.close();
                    }
                }
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