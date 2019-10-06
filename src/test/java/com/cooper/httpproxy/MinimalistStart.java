package com.cooper.httpproxy;


import com.cooper.httpproxy.server.HttpProxyServer;

public class MinimalistStart {
    public static void main(String[] args) {
        HttpProxyServer server = new HttpProxyServer();
        server.start();
    }
}