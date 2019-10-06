package com.cooper.httpproxy;

import com.cooper.httpproxy.intercept.ProxyHandlerIntercept;
import com.cooper.httpproxy.proxy.ProxyConfig;
import com.cooper.httpproxy.server.HttpProxyServer;
import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;

public class SecondaryProxyStart {

    public static void main(String[] args) {
        HttpProxyServer server = new HttpProxyServer();
        server.proxyHandlerIntercept(new ProxyHandlerIntercept(){
            @Override
            public ProxyConfig ProxyServer(InetSocketAddress realAddress, HttpRequest req) {
                //返回null不进行二次代理
                if (realAddress.getHostString().matches("^.*baidu.*$")) {
                    return new ProxyConfig("127.0.0.1", 8082);
                }
                return null;
            }
        }).start(8081);
    }
}
