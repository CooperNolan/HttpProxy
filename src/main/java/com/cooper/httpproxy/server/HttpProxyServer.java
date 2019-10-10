package com.cooper.httpproxy.server;

import com.cooper.httpproxy.config.HttpProxyServerConfig;
import com.cooper.httpproxy.crt.CertPool;
import com.cooper.httpproxy.crt.CertUtil;
import com.cooper.httpproxy.handler.localhost.downloadca.DownLoadCAHandler;
import com.cooper.httpproxy.handler.localhost.downloadca.HtmlDownLoadCAHandler;
import com.cooper.httpproxy.handler.request.HttpBeforeRequestHandler;
import com.cooper.httpproxy.handler.HttpServerHandler;
import com.cooper.httpproxy.handler.localhost.LocalHostHandler;
import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.intercept.LocalHostIntercept;
import com.cooper.httpproxy.intercept.ProxyIntercept;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class HttpProxyServer {

    private static Logger logger = LoggerFactory.getLogger(HttpProxyServer.class);

    private HttpProxyServerConfig serverConfig;
    private HttpModifyIntercept httpModifyIntercept;
    private ProxyIntercept proxyIntercept;
    private LocalHostIntercept localHostIntercept;

    public HttpProxyServer() {
        serverConfig = new HttpProxyServerConfig();
        localHostIntercept = new LocalHostIntercept();
    }

    public void start(Integer port){
        serverConfig.setPort(port);
        start();
    }

    public void start(){
        init();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(HttpProxyUtil.HTTP_SERVER_CODEC, new HttpServerCodec())
                                    .addLast(HttpProxyUtil.HTTP_BEFORE_REQUEST_HANDLER, new HttpBeforeRequestHandler(serverConfig))
                                    .addLast(HttpProxyUtil.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(serverConfig.getMaxContentLengthRequest()))
                                    .addLast(HttpProxyUtil.HTTP_SERVER_HANDLER, new HttpServerHandler(serverConfig, httpModifyIntercept, proxyIntercept,localHostIntercept));
                        }
                    });
            ChannelFuture channelFuture = serverBootstrap.bind(serverConfig.getPort()).sync();
            logger.info("netty start port({})", serverConfig.getPort());
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("HttpProxyServer Start Fail:",e.toString());
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
            CertPool.clear();
        }
    }

    private void init(){
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (serverConfig.isHandleSsl()) {
            try {
                serverConfig.setClientSslCtx(
                        SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build());
                X509Certificate caCert;
                PrivateKey caPriKey;
                caCert = CertUtil.loadCert(classLoader.getResourceAsStream("cacert/ca.crt"));
                caPriKey = CertUtil.loadPriKey(classLoader.getResourceAsStream("cacert/ca_private.pem"));
                //读取CA证书使用者信息
                serverConfig.setIssuer(CertUtil.getSubject(caCert));
                //读取CA证书有效时段(server证书有效期超出CA证书的，在手机上会提示证书不安全)
                serverConfig.setCaNotBefore(caCert.getNotBefore());
                serverConfig.setCaNotAfter(caCert.getNotAfter());
                //CA私钥用于给动态生成的网站SSL证书签证
                serverConfig.setCaPriKey(caPriKey);
                //生产一对随机公私钥用于网站SSL证书动态创建
                KeyPair keyPair = CertUtil.genKeyPair();
                serverConfig.setServerPriKey(keyPair.getPrivate());
                serverConfig.setServerPubKey(keyPair.getPublic());
            } catch (Exception e) {
                serverConfig.setHandleSsl(false);
                logger.error(e.toString());
            }
        }
        try {
            Set<String> localhost = new HashSet<>();
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            NetworkInterface net;
            InetAddress inetAddress;
            while (nets.hasMoreElements()) {
                net = nets.nextElement();
                Enumeration<InetAddress> address = net.getInetAddresses();
                while (address.hasMoreElements()) {
                    inetAddress = address.nextElement();
                    if (inetAddress != null && inetAddress instanceof Inet4Address)
                        localhost.add(inetAddress.getHostAddress());
                }
            }
            serverConfig.setLocalhost(localhost);
        } catch (SocketException e) {
            logger.error(e.toString());
        }
        if (httpModifyIntercept == null) {
            httpModifyIntercept = new HttpModifyIntercept();
        }
        proxyIntercept = new ProxyIntercept();
        proxyIntercept.init();
        localHostIntercept.addLocalHostHandler("/favicon.ico",new LocalHostHandler());
        localHostIntercept.addLocalHostHandler("/" + serverConfig.getDownloadCACertPath(),new HtmlDownLoadCAHandler(serverConfig.getDownloadCACertPath()));
        localHostIntercept.addLocalHostHandler("/" + serverConfig.getDownloadCACertPath() + "/ca.crt",new DownLoadCAHandler());
    }

    public HttpProxyServer maxContentLengthRequest(Integer maxContentLengthRequest) {
        serverConfig.setMaxContentLengthRequest(maxContentLengthRequest);
        return this;
    }

    public HttpProxyServer maxContentLengthResponse(Integer maxContentLengthResponse) {
        serverConfig.setMaxContentLengthResponse(maxContentLengthResponse);
        return this;
    }

    public HttpProxyServer downloadCACertPath(String downloadCACertPath) {
        serverConfig.setDownloadCACertPath(downloadCACertPath);
        return this;
    }

    public HttpProxyServer isHandlerSsl(boolean isHandlerSsl) {
        serverConfig.setHandleSsl(isHandlerSsl);
        return this;
    }

    public HttpProxyServer httpModifyIntercept(HttpModifyIntercept httpModifyIntercept) {
        this.httpModifyIntercept = httpModifyIntercept;
        return this;
    }

    public HttpProxyServer addLocalHostHandler(String uri, LocalHostHandler localHostHandler) {
        localHostIntercept.addLocalHostHandler(uri,localHostHandler);
        return this;
    }
}