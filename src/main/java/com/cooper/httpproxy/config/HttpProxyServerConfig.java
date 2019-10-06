package com.cooper.httpproxy.config;

import io.netty.handler.ssl.SslContext;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Set;

public class HttpProxyServerConfig {

    private Integer port = 8081;

    private Integer maxContentLengthRequest = 1024 * 1024;
    private Integer maxContentLengthResponse = 1024 * 1024 * 8;
    private String downloadCACertPath = "download";

    private SslContext clientSslCtx;
    private String issuer;
    private Date caNotBefore;
    private Date caNotAfter;
    private PrivateKey caPriKey;
    private PrivateKey serverPriKey;
    private PublicKey serverPubKey;
    private boolean handleSsl = false;

    private Set<String> localhost;

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setMaxContentLengthRequest(Integer maxContentLengthRequest) {
        this.maxContentLengthRequest = maxContentLengthRequest;
    }

    public void setMaxContentLengthResponse(Integer maxContentLengthResponse) {
        this.maxContentLengthResponse = maxContentLengthResponse;
    }

    public void setDownloadCACertPath(String downloadCACertPath) {
        this.downloadCACertPath = downloadCACertPath;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getMaxContentLengthRequest() {
        return maxContentLengthRequest;
    }

    public Integer getMaxContentLengthResponse() {
        return maxContentLengthResponse;
    }

    public String getDownloadCACertPath() {
        return downloadCACertPath;
    }

    public SslContext getClientSslCtx() {
        return clientSslCtx;
    }

    public void setClientSslCtx(SslContext clientSslCtx) {
        this.clientSslCtx = clientSslCtx;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Date getCaNotBefore() {
        return caNotBefore;
    }

    public void setCaNotBefore(Date caNotBefore) {
        this.caNotBefore = caNotBefore;
    }

    public Date getCaNotAfter() {
        return caNotAfter;
    }

    public void setCaNotAfter(Date caNotAfter) {
        this.caNotAfter = caNotAfter;
    }

    public PrivateKey getCaPriKey() {
        return caPriKey;
    }

    public void setCaPriKey(PrivateKey caPriKey) {
        this.caPriKey = caPriKey;
    }

    public PrivateKey getServerPriKey() {
        return serverPriKey;
    }

    public void setServerPriKey(PrivateKey serverPriKey) {
        this.serverPriKey = serverPriKey;
    }

    public PublicKey getServerPubKey() {
        return serverPubKey;
    }

    public void setServerPubKey(PublicKey serverPubKey) {
        this.serverPubKey = serverPubKey;
    }

    public Set<String> getLocalhost() {
        return localhost;
    }

    public void setLocalhost(Set<String> localhost) {
        this.localhost = localhost;
    }

    public boolean isHandleSsl() {
        return handleSsl;
    }

    public void setHandleSsl(boolean handleSsl) {
        this.handleSsl = handleSsl;
    }
}