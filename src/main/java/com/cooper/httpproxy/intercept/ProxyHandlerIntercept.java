package com.cooper.httpproxy.intercept;

import com.cooper.httpproxy.proxy.ProxyConfig;
import com.cooper.httpproxy.proxy.ProxyHandleFactory;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.proxy.ProxyHandler;

import java.net.InetSocketAddress;

public class ProxyHandlerIntercept {

    public final ProxyHandler proxyHandler(InetSocketAddress realAddress, HttpRequest req) {
        ProxyConfig proxyConfig = ProxyServer(realAddress,req);
        return ProxyHandleFactory.build(proxyConfig);
    }

    public ProxyConfig ProxyServer(InetSocketAddress realAddress, HttpRequest req){
        //返回null不进行二次代理
        return null;
    }

}