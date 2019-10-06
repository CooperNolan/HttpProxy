package com.cooper.httpproxy.intercept;

import io.netty.handler.codec.http.*;

public class HttpModifyIntercept {

    public void beforeRequest(FullHttpRequest req) throws Exception {
    }
    public void beforeRequest(HttpRequest req) throws Exception {
    }
    public void beforeRequest(HttpContent reqContent) throws Exception {
    }
    public void beforeResponse(FullHttpResponse resp) throws Exception {
    }
    public void beforeResponse(HttpResponse resp) throws Exception {
    }
    public void beforeResponse(HttpContent respContent) throws Exception {
    }

}