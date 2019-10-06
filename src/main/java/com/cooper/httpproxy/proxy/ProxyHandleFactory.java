package com.cooper.httpproxy.proxy;

import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;

public class ProxyHandleFactory {

    public static ProxyHandler build(ProxyConfig proxyConfig) {
        ProxyHandler proxyHandler = null;
        if (proxyConfig != null) {
            boolean isAuth = proxyConfig.getUser() != null && proxyConfig.getPwd() != null;
            InetSocketAddress inetSocketAddress = new InetSocketAddress(proxyConfig.getHost(),
                    proxyConfig.getPort());
            switch (proxyConfig.getProxyType()) {
                case HTTP:
                    if (isAuth) {
                        proxyHandler = new HttpProxyHandler(inetSocketAddress,
                                proxyConfig.getUser(), proxyConfig.getPwd());
                    } else {
                        proxyHandler = new HttpProxyHandler(inetSocketAddress);
                    }
                    break;
                case SOCKS4:
                    proxyHandler = new Socks4ProxyHandler(inetSocketAddress);
                    break;
                case SOCKS5:
                    if (isAuth) {
                        proxyHandler = new Socks5ProxyHandler(inetSocketAddress,
                                proxyConfig.getUser(), proxyConfig.getPwd());
                    } else {
                        proxyHandler = new Socks5ProxyHandler(inetSocketAddress);
                    }
                    break;
            }
        }
        return proxyHandler;

    }
}