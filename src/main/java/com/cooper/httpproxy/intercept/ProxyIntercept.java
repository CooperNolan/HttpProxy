package com.cooper.httpproxy.intercept;

import com.cooper.httpproxy.proxy.ProxyConfig;
import com.cooper.httpproxy.proxy.ProxyHandleFactory;
import com.cooper.httpproxy.util.HttpProxyUtil;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.proxy.ProxyHandler;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class ProxyIntercept {

    private static Logger logger = LoggerFactory.getLogger(ProxyIntercept.class);

    private Map<String, ProxyConfig> proxyConfigMap = new HashMap<>();
    private Map<String, HttpHost> urlProxyConfigMap = new HashMap<>();

    public void init() {
        InputStream ins = null;
        Properties properties = new Properties();
        //域名代理
        try {
            ins = Thread.currentThread().getContextClassLoader().getResourceAsStream("proxy.properties");
            properties.load(ins);
            Iterator<String> it=properties.stringPropertyNames().iterator();
            while(it.hasNext()){
                String host=it.next();
                if (host.equals(HttpProxyUtil.DEFAULT_PROXY)) {
                    if (properties.getProperty(HttpProxyUtil.DEFAULT_PROXY).equals("null")) {
                        proxyConfigMap.put(HttpProxyUtil.DEFAULT_PROXY, null);
                        continue;
                    }
                }
                ProxyConfig proxyConfig = new ProxyConfig();
                String[] configs = properties.getProperty(host).split(":");
                for (int i = 0; i < configs.length; i++) {
                    proxyConfig.set(i, configs[i]);
                }
                proxyConfigMap.put(host, proxyConfig);
            }
        } catch (Exception e) {
            logger.error(e.toString());
        } finally {
            properties.clear();
            if (ins != null) {
                try {
                    ins.close();
                }catch (IOException e) {
                    logger.error(e.toString());
                }
            }

        }
        //url代理
        try {
            ins = Thread.currentThread().getContextClassLoader().getResourceAsStream("urlproxy.properties");
            properties.load(ins);
            Iterator<String> it=properties.stringPropertyNames().iterator();
            while(it.hasNext()){
                String uri=it.next();
                String[] configs = properties.getProperty(uri).split(":");
                HttpHost urlProxy = new HttpHost(configs[0], Integer.parseInt(configs[1]));
                urlProxyConfigMap.put(uri, urlProxy);
            }
        } catch (Exception e) {
            logger.error(e.toString());
        } finally {
            properties.clear();
            if (ins != null) {
                try {
                    ins.close();
                }catch (IOException e) {
                    logger.error(e.toString());
                }
            }

        }
    }

    public ProxyHandler proxyHandler(InetSocketAddress realAddress, HttpRequest req) {
        ProxyConfig proxyConfig = ProxyServer(realAddress,req);
        return ProxyHandleFactory.build(proxyConfig);
    }

    public ProxyConfig ProxyServer(InetSocketAddress realAddress, HttpRequest req){
        //返回null不进行二次代理
        if (proxyConfigMap.containsKey(realAddress.getHostString())) {
            return proxyConfigMap.get(realAddress.getHostString());
        } else if (proxyConfigMap.containsKey(HttpProxyUtil.DEFAULT_PROXY)) {
            return proxyConfigMap.get(HttpProxyUtil.DEFAULT_PROXY);
        }
        return null;
    }

    public HttpHost urlProxyServer(InetSocketAddress realAddress, HttpRequest req) {
        String url = HttpProxyUtil.getUrl(req.uri());
        int index = url.indexOf("?");
        if (index > -1) {
            url = url.substring(0, index);
        }
        if (urlProxyConfigMap.containsKey(realAddress.getHostString() + url)) {
            return urlProxyConfigMap.get(realAddress.getHostString() + url);
        }
        return null;
    }

}