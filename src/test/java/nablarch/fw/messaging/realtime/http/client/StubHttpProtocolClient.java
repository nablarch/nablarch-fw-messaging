package nablarch.fw.messaging.realtime.http.client;

import nablarch.fw.messaging.realtime.http.dto.HttpResult;
import nablarch.fw.messaging.realtime.http.streamio.HttpInputStreamReader;
import nablarch.fw.messaging.realtime.http.streamio.HttpOutputStreamWriter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;

/**
 * HTTP通信のユニットテスト用のスタブ。<br>
 * 通信を行わずに折り返す。
 * 
 * @author Masaya Seko
 */
public class StubHttpProtocolClient implements HttpProtocolClient {

    @Override
    public void setProxyInfo(String url, int port) {
    }

    @Override
    public void setSslContext(SSLContext sslContext) {
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
    }

    @Override
    public void setReadTimeout(int httpReadTimeout) {
    }

    @Override
    public void setContentType(String contentType) {
    }

    @Override
    public void setAccept(String accept) {
    }

    @Override
    public void setQueryStringEncoding(String queryStringEncoding) {
    }

    /**
     * HTTP通信用Stub。<br>
     * 常にステータスコード200を返す。
     */
    @Override
    public HttpResult execute(HttpRequestMethodEnum httpMethod, String url,
            Map<String, List<String>> headerInfo,
            Map<String, String> urlParams, HttpOutputStreamWriter writer,
            HttpInputStreamReader reader) {
        HttpResult httpResult = new HttpResult();
        httpResult.setHeaderInfo(new TreeMap<String, List<String>>());
        httpResult.setResponseCode(200);
        return httpResult;
    }
}
