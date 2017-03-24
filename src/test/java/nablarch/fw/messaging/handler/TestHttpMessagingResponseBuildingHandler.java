package nablarch.fw.messaging.handler;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.messaging.ErrorResponseMessage;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.reader.StructuredFwHeaderDefinition;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

/**
 * HTTPメッセージングレスポンス電文構築するテスト用ハンドラ
 * <p/>
 * 業務アクションの作成した応答電文(ResponseMessage)をHTTPレスポンスオブジェクトに変換するハンドラ。
 * <p/>
 * 応答電文構築中にフォーマットエラーが発生した場合は、業務処理の不具合と考えられるため、
 * システムエラー(ステータスコード500)として送出する。
 * 
 * @author TIS
 */
public class TestHttpMessagingResponseBuildingHandler implements Handler<Object, Object> {

    /** ロガー * */
    private static final Logger LOGGER = LoggerManager.get(TestHttpMessagingResponseBuildingHandler.class);

    /** 証跡ログを出力するロガー */
    private static final Logger MESSAGING_LOGGER = LoggerManager.get("MESSAGING");
        
    /** HTTPヘッダ名・関連メッセージID */
    private static final String HTTP_HEADER_CORRELATION_ID = "X-Correlation-Id";

    /** フレームワーク制御ヘッダ定義 */
    private FwHeaderDefinition fwHeaderDefinition = new StructuredFwHeaderDefinition();

    
    /** {@inheritDoc}
     * この実装ではHTTPRequestオブジェクトからRequestMessageオブジェクトへの変換および
     * ResponseMesssageオブジェクトからHttpResponseオブジェクトへの変換を行う。
     *
     * @throws java.lang.ClassCastException 引数 servletContext の実際の型が ServletExecutionContext でない場合。
     */
    public Object handle(Object req, ExecutionContext ctx) throws ClassCastException {

        try {
            Object res = ctx.handleNext(req);
            if (res instanceof ResponseMessage) {
                return createResponseMessage((ResponseMessage) res);
            } else {
                return res;
            }
            
        } catch (MessagingException e) {
            // メッセージ（BODY部）不正の場合は、応答電文データ作成処理の不具合とみなし、証跡を残した上で500を返す。
            LOGGER.logInfo("could not build the message body because of an invalid message error: " + e.getMessage());
            throw new HttpErrorResponse(500, e);
            
        } catch (InvalidDataFormatException e) {
            // メッセージ（BODY部）のフォーマット不正の場合は、応答電文データ作成処理の不具合とみなし、証跡を残した上で500を返す。
            LOGGER.logInfo("could not build the message body because of an invalid data error: " + e.getMessage());
            throw new HttpErrorResponse(500, e);

        } catch (ErrorResponseMessage e) {
            // エラー応答電文の返却処理
            HttpErrorResponse errorResponse = new HttpErrorResponse(e.getCause());
            errorResponse.setResponse(createResponseMessage(e.getResponse()));
            throw errorResponse;
        }
    }

    /**
     * HttpResponseオブジェクトの生成 
     * @param responseMessage 業務応答電文
     * @return HttpResponseオブジェクト
     */
    private HttpResponse createResponseMessage(ResponseMessage responseMessage) {
        HttpResponse httpResponse = new HttpResponse();
        
        // コンテントタイプ設定
        DataRecordFormatter formatter = responseMessage.getFormatter();
        Charset charset = null;
        if (formatter instanceof DataRecordFormatterSupport) {
            DataRecordFormatterSupport drfs = (DataRecordFormatterSupport) formatter;
            String mimeType = drfs.getMimeType();
            charset = drfs.getDefaultEncoding();
            String contentType = String.format("%s;charset=%s", mimeType, charset.name());
            httpResponse.setContentType(contentType);
        }

        // 応答データストリーム設定
        responseMessage.setFwHeaderDefinition(fwHeaderDefinition);
        httpResponse.setBodyStream(new ByteArrayInputStream(responseMessage.getBodyBytes()));
        
        // ステータスコード設定
        httpResponse.setStatusCode(getStatusCode(responseMessage));

        httpResponse.setHeader(HTTP_HEADER_CORRELATION_ID,
                (String) responseMessage.getHeader("CorrelationId"));
        
        if (MESSAGING_LOGGER.isInfoEnabled()) {
            emitLog(responseMessage, charset);
        }
        
        return httpResponse;
    }

    /**
     * ステータスコードを取得する。
     *
     * レスポンスメッセージ内のFW制御ヘッダー部にステータスコードが設定されている場合には、その値をステータスコードとする。
     * 設定されていない場合には、レスポンスメッセージが保持しているステータスコードを返却する。
     * @param responseMessage レスポンスメッセージ
     * @return ステータスコード
     */
    private int getStatusCode(ResponseMessage responseMessage) {
        String headerStatusCode = responseMessage.getFwHeader().getStatusCode();
        if (!StringUtil.isNullOrEmpty(headerStatusCode)) {
            return Integer.parseInt(headerStatusCode);
        }
        return responseMessage.getStatusCode();
    }

    /**
     * フレームワーク制御ヘッダ定義を設定する。
     * @param fwHeaderDefinition フレームワーク制御ヘッダ定義
     */
    public void setFwHeaderDefinition(FwHeaderDefinition fwHeaderDefinition) {
        this.fwHeaderDefinition = fwHeaderDefinition;
    }

    /**
     * メッセージングの証跡ログを出力する。
     * @param message メッセージオブジェクト
     * @param charset 出力に使用する文字レット
     */
    private void emitLog(InterSystemMessage<?> message, Charset charset) {
        
        String log = (message instanceof ReceivedMessage)
                   ? MessagingLogUtil.getHttpReceivedMessageLog((ReceivedMessage) message, charset)
                   : MessagingLogUtil.getHttpSentMessageLog((SendingMessage) message, charset);
        
        MESSAGING_LOGGER.logInfo(log);
    }
}

