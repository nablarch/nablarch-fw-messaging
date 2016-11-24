package nablarch.fw.messaging.realtime.http.client;

import java.util.List;
import java.util.Map;

import nablarch.fw.messaging.realtime.http.client.HttpProtocolClient.HttpRequestMethodEnum;
import nablarch.fw.messaging.realtime.http.dto.HttpResult;

/**
 * HTTP通信のユニットテスト用のスタブ。<br>
 * 通信を行わずに折り返す。
 * 
 * @author Masaya Seko
 */
public class StubHTTPMessagingClient extends HttpMessagingClient {

    /** 最後に送信したHTTPメソッド*/
    private HttpRequestMethodEnum lastHttpMethod;
    /** 最後に送信に使用したUri*/
    private String lastUri;
    /** 最後に送信に使用したヘッダ情報*/
    private Map<String, List<String>> lastHeaderInfo;
    /** 最後に送信に使用したURLパラメータ*/
    private Map<String, String> lastUrlParams;
    /** 最後に送信に使用した文字コード*/
    private String lastCharset;
    /** 最後に送信した本文*/
    private String lastBodyText;
    /** 最後に受信した通信内容*/
    private HttpResult lastHttpResult;
    
    @Override
    protected HttpProtocolClient createHttpProtocolClient() {
        return new StubHttpProtocolClient();
    }

    /**
     * 通信内容の確認のために、送信の前後に情報を保存する通信メソッド。
     */
    @Override
    protected HttpResult execute(HttpProtocolClient httpProtocolClient,
            HttpRequestMethodEnum httpMethod, String uri,
            Map<String, List<String>> headerInfo,
            Map<String, String> urlParams, String charset, String bodyText) {
        lastHttpMethod = httpMethod;
        lastUri = uri;
        lastHeaderInfo = headerInfo;
        lastUrlParams = urlParams;
        lastCharset = charset;
        lastBodyText = bodyText;
        HttpResult httpResult = super.execute(httpProtocolClient, httpMethod, uri, headerInfo, urlParams, charset, bodyText);
        lastHttpResult = httpResult;
        return httpResult;
    }

    public HttpRequestMethodEnum getLastHttpMethod() {
        return lastHttpMethod;
    }

    public String getLastUri() {
        return lastUri;
    }

    public Map<String, List<String>> getLastHeaderInfo() {
        return lastHeaderInfo;
    }

    public Map<String, String> getLastUrlParams() {
        return lastUrlParams;
    }

    public String getLastCharset() {
        return lastCharset;
    }

    public String getLastBodyText() {
        return lastBodyText;
    }

    public HttpResult getLastHttpResult() {
        return lastHttpResult;
    }
}
