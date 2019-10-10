package com.cooper.httpproxy.intercept;


import com.cooper.httpproxy.handler.localhost.LocalHostHandler;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

public class LocalHostIntercept {

    private Map<String, LocalHostHandler> localhostHandlerMap = new HashMap<>();

    public void localhostHandler(Channel clientChannel, String uri){
        if (localhostHandlerMap.containsKey(uri)) {
            LocalHostHandler localHostHandler = localhostHandlerMap.get(uri);
            localHostHandler.start(clientChannel);
        } else {
            HttpProxyUtil.channelClose(clientChannel);
        }
    }

    public void addLocalHostHandler(String uri, LocalHostHandler localHostHandler){
        if (uri.indexOf("/") != 0) {
            uri = "/" + uri;
        }
        localhostHandlerMap.put(uri, localHostHandler);
    }

}