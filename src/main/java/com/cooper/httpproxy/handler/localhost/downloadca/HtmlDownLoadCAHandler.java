package com.cooper.httpproxy.handler.localhost.downloadca;

import com.cooper.httpproxy.handler.localhost.LocalHostHandler;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;

public class HtmlDownLoadCAHandler extends LocalHostHandler {

    private String downloadCACertPath;

    public HtmlDownLoadCAHandler(String downloadCACertPath) {
        this.downloadCACertPath = downloadCACertPath;
    }

    @Override
    public void start(Channel clientChannel) {
        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);
        String html = "<html><body><div style=\"margin-top:100px;text-align:center;\"><a href=\"" +
                downloadCACertPath +
                "/ca.crt\">点击下载证书</a></div></body></html>";
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, html.getBytes().length);
        httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        HttpContent httpContent = new DefaultLastHttpContent();
        httpContent.content().writeBytes(html.getBytes());
        clientChannel.writeAndFlush(httpResponse);
        clientChannel.writeAndFlush(httpContent);
    }
}
