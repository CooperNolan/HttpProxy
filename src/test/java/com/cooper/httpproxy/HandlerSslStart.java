package com.cooper.httpproxy;

import com.cooper.httpproxy.intercept.HttpModifyIntercept;
import com.cooper.httpproxy.server.HttpProxyServer;
import io.netty.handler.codec.http.*;

public class HandlerSslStart {
    public static void main(String[] args) {
        HttpProxyServer server = new HttpProxyServer();
        server.isHandlerSsl(true)
                .httpModifyIntercept(new HttpModifyIntercept() {
                    @Override
                    public void beforeResponse(HttpResponse resp) throws Exception {
                        resp.headers().add("test", "success");
                    }
                })
                .start();
    }
}
