package com.cooper.httpproxy.handler.localhost;

import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;

public class LocalHostHandler {
    public void start(Channel clientChannel){
        HttpProxyUtil.channelClose(clientChannel);
    }
}
