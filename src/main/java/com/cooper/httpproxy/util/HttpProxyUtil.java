package com.cooper.httpproxy.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Map;
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

    public static final String DEFAULT_PROXY = "DEFAULT_PROXY";

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

    public static String getUrl(String uri) {
        if (uri.matches("^http.*$")) {
            return uri.substring(uri.indexOf("/", 7));
        } else {
            return uri;
        }
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

    /**
     *根据netty HttpRequest生成apache HttpRequest
     * @param address
     * @param urlProxy
     * @param nettyHttpRequest
     * @return
     * @throws Exception
     */
    public static HttpUriRequest createHttpUriRequest(InetSocketAddress address, HttpHost urlProxy,FullHttpRequest nettyHttpRequest) throws Exception{
        HttpUriRequest httpUriRequest = null;
        String uri;
        String schemas = "Http";
        if (address.getPort() == 443) {
            schemas = "Https";
        }
        if(address.getHostString().matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")){
            uri = schemas + "://" + address.getHostString() + ":" + address.getPort() + HttpProxyUtil.getUrl(nettyHttpRequest.uri());
        }else{
            uri = schemas + "://" + address.getHostString() + HttpProxyUtil.getUrl(nettyHttpRequest.uri());
        }
        RequestConfig requestConfig = RequestConfig.custom().setProxy(urlProxy).build();
        HttpRequestBase httpRequestBase = null;
        if(nettyHttpRequest.method().equals(HttpMethod.GET)) {
            httpRequestBase = new HttpGet(uri);
        }else if(nettyHttpRequest.method().equals(HttpMethod.POST)){
            httpRequestBase = new HttpPost(uri);
        }else if(nettyHttpRequest.method().equals(HttpMethod.PUT)){
            httpRequestBase = new HttpPut(uri);
        }else if(nettyHttpRequest.method().equals(HttpMethod.DELETE)){
            httpRequestBase = new HttpDelete(uri);
        }
        httpRequestBase.setConfig(requestConfig);
        if (nettyHttpRequest.content().readableBytes() > 0) {
            ByteBuf byteBuf = nettyHttpRequest.content();
            byte[] content = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(content);
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(content);
            ((HttpEntityEnclosingRequest)httpRequestBase).setEntity(byteArrayEntity);
        }
        httpUriRequest = httpRequestBase;
        Iterator iterator = nettyHttpRequest.headers().iteratorAsString();
        while (iterator.hasNext()) {
            Map.Entry header = (Map.Entry) iterator.next();
            httpUriRequest.setHeader((String)header.getKey(),(String)header.getValue());
        }
        if (httpUriRequest.containsHeader(HttpHeaderNames.CONTENT_LENGTH.toString())) {
            httpUriRequest.removeHeaders(HttpHeaderNames.CONTENT_LENGTH.toString());
        }
        return httpUriRequest;
    }


    /**
     * 将apache HttpResponse转换成netty HttpResponse
     * @param httpResponse
     * @return
     * @throws IOException
     */
    public static FullHttpResponse httpResponseConversion(org.apache.http.HttpResponse httpResponse) throws IOException {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(
                HttpVersion.valueOf(httpResponse.getStatusLine().getProtocolVersion().toString()),
                new HttpResponseStatus(httpResponse.getStatusLine().getStatusCode(), httpResponse.getStatusLine().getReasonPhrase()));
        HeaderIterator headerIterator = httpResponse.headerIterator();
        while (headerIterator.hasNext()) {
            Header header = headerIterator.nextHeader();
            fullHttpResponse.headers().set(header.getName(),header.getValue());
        }
        HttpEntity httpEntity = httpResponse.getEntity();
        if (httpEntity != null) {
            fullHttpResponse.content().writeBytes(EntityUtils.toByteArray(httpEntity));
        }
        return fullHttpResponse;
    }

    /**
     * 绕过ssl验证
     *
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static SSLContext createIgnoreVerifySSL() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getInstance("SSLv3");
        // 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                    String paramString) throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        sc.init(null, new TrustManager[] { trustManager }, null);
        return sc;
    }
}