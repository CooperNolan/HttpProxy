package com.cooper.httpproxy.handler.localhost.downloadca;

import com.cooper.httpproxy.crt.CertUtil;
import com.cooper.httpproxy.handler.localhost.LocalHostHandler;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CertificateException;

public class DownLoadCAHandler extends LocalHostHandler {

    private static Logger logger = LoggerFactory.getLogger(DownLoadCAHandler.class);

    @Override
    public void start(Channel clientChannel) {
        try {
            HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK);
            byte[] bts = CertUtil
                    .loadCert(Thread.currentThread().getContextClassLoader().getResourceAsStream("cacert/ca.crt"))
                    .getEncoded();
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-x509-ca.crt-cert");
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, bts.length);
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            HttpContent httpContent = new DefaultLastHttpContent();
            httpContent.content().writeBytes(bts);
            clientChannel.writeAndFlush(httpResponse);
            clientChannel.writeAndFlush(httpContent);
            HttpProxyUtil.channelClose(clientChannel);
        } catch (CertificateException e) {
            logger.error(e.toString());
        }

    }
}
