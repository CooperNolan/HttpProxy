package com.cooper.httpproxy.handler.localhost;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

public class AuthorizationHandler extends LocalHostHandler {

    private static String authorization = "";

    @Override
    public void start(Channel clientChannel) {
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);
        String html = authorization;
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, html.getBytes().length);
        httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        HttpContent httpContent = new DefaultLastHttpContent();
        httpContent.content().writeBytes(html.getBytes());
        clientChannel.writeAndFlush(httpResponse);
        clientChannel.writeAndFlush(httpContent);
    }

    public static void setAuthorization(String authorization) {
        AuthorizationHandler.authorization = authorization;
    }
}
