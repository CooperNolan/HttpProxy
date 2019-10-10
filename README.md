# HttpProxy

###	基于Netty的轻量级Http代理服务器

#####	可修改请求和响应内容

###	Https支持

#####	需要将CA证书(src/main/resources/cacert/ca.crt)导入至受信任的根证书颁发机构

```java
/**
 * 生成私钥
 * openssl genrsa -des3 -out ca.key 2048
 * openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.pem
 *
 * 通过私钥生成CA证书
 * openssl req -sha256 -new -x509 -days 365 -key ca.key -out ca.crt \
 *     -subj "/C=CN/ST=CN/L=CN/O=Cooper/OU=Cooper/CN=HttpProxy"
 */
```

### 二级域名代理

```properties
#需要代理域名=代理IP:代理端口:代理类型（HTTP，SOCKS4，SOCKS5 可选 默认HTTP）:用户名（可选）:密码（可选）
#如 www.baidu.com=127.0.0.1:8081
#   www.baidu.com=127.0.0.1:8081:HTTP:123:123
#www.baidu.com=127.0.0.1:8080

#默认二级代理  输入null不走二级代理
#DEFAULT_PROXY=null
#DEFAULT_PROXY=127.0.0.1:8080
```

###二级url代理（未完善）

```properties
#二级url代理
#www.baidu.com/s=127.0.0.1:8081
```

###	启动

#####	启动代码参照 test 包

