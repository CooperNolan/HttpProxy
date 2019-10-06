package com.cooper.httpproxy.util;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;

import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpProxyUtil {

    public static final String HTTP_SERVER_CODEC = "HTTP_SERVER_CODEC";
    public static final String HTTP_CLIENT_CODEC = "HTTP_CLIENT_CODEC";

    public static final String HTTP_OBJECT_AGGREGATOR = "HTTP_OBJECT_AGGREGATOR";

    public static final String HTTP_SERVER_HANDLER = "HTTP_SERVER_HANDLER";
    public static final String HTTP_REQUEST_HANDLER = "HTTP_REQUEST_HANDLER";
    public static final String HTTP_RESPONSE_HANDLER = "HTTP_RESPONSE_HANDLER";

    public static final String HTTP_BEFORE_REQUEST_HANDLER = "HTTP_BEFORE_REQUEST_HANDLER";
    public static final String HTTP_BEFORE_RESPONSE_HANDLER = "HTTP_BEFORE_RESPONSE_HANDLER";

    public static final String HTTP_SERVER_SSL_HANDLER = "HTTP_SERVER_SSL_HANDLER";
    public static final String HTTP_CLIENT_SSL_HANDLER = "HTTP_CLIENT_SSL_HANDLER";

    public static InetSocketAddress getAddressByRequest(HttpRequest req) {
        int port = -1;
        String hostStr = req.headers().get(HttpHeaderNames.HOST);
        if (hostStr == null) {
            Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^/]*)/?.*$");
            Matcher matcher = pattern.matcher(req.uri());
            if (matcher.find()) {
                hostStr = matcher.group("host");
            } else {
                return null;
            }
        }
        String uriStr = req.uri();
        Pattern pattern = Pattern.compile("^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$");
        Matcher matcher = pattern.matcher(hostStr);
        //先从host上取端口号没取到再从uri上取端口号
        String portTemp = null;
        if (matcher.find()) {
            hostStr = matcher.group("host");
            portTemp = matcher.group("port");
            if (portTemp == null) {
                matcher = pattern.matcher(uriStr);
                if (matcher.find()) {
                    portTemp = matcher.group("port");
                }
            }
        }
        if (portTemp != null) {
            port = Integer.parseInt(portTemp);
        }
        if (port == -1) {
            boolean isSsl = uriStr.indexOf("https") == 0 || hostStr.indexOf("https") == 0;
            if (isSsl) {
                port = 443;
            } else {
                port = 80;
            }
        }
        return new InetSocketAddress(hostStr, port);
    }

    public static void removePipeline(Channel channel,String pipelineName) {
        if (channel.pipeline().get(pipelineName) != null) {
            channel.pipeline().remove(pipelineName);
        }
    }

    public static void channelClose(Channel... channels) {
        for (Channel channel:channels) {
            if (channel != null) {
                channel.close();
            }
        }
    }
}