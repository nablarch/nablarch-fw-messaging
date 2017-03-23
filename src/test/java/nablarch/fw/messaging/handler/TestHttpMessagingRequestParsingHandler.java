package nablarch.fw.messaging.handler;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.logging.MessagingLogUtil;
import nablarch.fw.messaging.reader.StructuredFwHeaderDefinition;
import nablarch.fw.results.RequestEntityTooLarge;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTPメッセージングデータ解析するテスト用ハンドラ
 * <p/>
 * HTTPリクエストの内容を解析し、メッセージング機能で使用される電文オブジェクトを作成することで、
 * 画面オンライン実行基盤にて使用されるハンドラ郡とメッセージング制御基盤にて使用されるハンドラ郡の
 * 橋渡し的な機能を提供する。
 * 
 * @author TIS
 */
public class TestHttpMessagingRequestParsingHandler implements Handler<HttpRequest, Object> {

    /** ロガー * */
    private static final Logger LOGGER = LoggerManager.get(TestHttpMessagingRequestParsingHandler.class);

    /** 証跡ログを出力するロガー */
    private static final Logger MESSAGING_LOGGER = LoggerManager.get("MESSAGING");
    
    /** Content-Typeヘッダから文字セットを取得するためのパターン */
    private static final Pattern CHARSET_PTN = Pattern.compile(".*charset=(.+)");
    
    /** HTTPヘッダ名・メッセージID */
    private static final String HTTP_HEADER_MESSAGE_ID = "X-Message-Id";
    
    /** HTTPヘッダ名・関連メッセージID */
    private static final String HTTP_HEADER_CORRELATION_ID = "X-Correlation-Id";

    /** フレームワーク制御ヘッダ定義 */
    private FwHeaderDefinition fwHeaderDefinition = new StructuredFwHeaderDefinition();

    /* 業務データフォーマット定義ファイル関連設定 */
    /** 業務データフォーマット定義ファイル配置ディレクトリ論理名 */
    private static final String FORMAT_FILE_DIR = "format";

    /** データ読み込み時バッファサイズ　 */
    private static final int BODY_READ_BUF_SIZE = 4096;
    
    /** リクエストのボディストリームから読み込む最大容量（単位：バイト） 　*/
    private int bodyLengthLimit = Integer.MAX_VALUE;
    
    /** {@inheritDoc}
     * この実装ではHTTPRequestオブジェクトからRequestMessageオブジェクトへの変換および
     * ResponseMesssageオブジェクトからHttpResponseオブジェクトへの変換を行う。
     *
     * @throws java.lang.ClassCastException 引数 servletContext の実際の型が ServletExecutionContext でない場合。
     */
    public Object handle(HttpRequest req, ExecutionContext ctx) throws ClassCastException {

        ServletExecutionContext servletContext = (ServletExecutionContext) ctx;

        // メッセージIDの取得
        String messageId = req.getHeader(HTTP_HEADER_MESSAGE_ID);
        if (StringUtil.isNullOrEmpty(messageId)) {
            LOGGER.logInfo("Request of empty messageId received.");
            return HttpResponse.Status.BAD_REQUEST.handle(req, ctx);
        }

        RequestMessage requestMessage;
        try {
            // 処理実行
            requestMessage = createRequestMessage(req, servletContext, messageId);
        } catch (RequestEntityTooLarge e) {
            // BODY部のサイズ超過の場合は、証跡を残し413を返す。
            LOGGER.logInfo("request entity too large.: " + e.getMessage());
            return new HttpErrorResponse(413, e).getResponse();
        } catch (MessagingException e) {
            // メッセージ（BODY部）不正の場合は、証跡を残し400を返す。
            LOGGER.logInfo("invalid request message received.: " + e.getMessage());
            return HttpResponse.Status.BAD_REQUEST.handle(req, ctx);
        } catch (InvalidDataFormatException e) {
            // メッセージ（BODY部）のフォーマット不正の場合は、証跡を残し400を返す
            LOGGER.logInfo("invalid format request message received.: " + e.getMessage());
            return HttpResponse.Status.BAD_REQUEST.handle(req, ctx);
        }
        return ctx.handleNext(requestMessage);
    }

    /**
     * RequestMessageオブジェクトを作成する
     * @param request HTTPリクエスト
     * @param context 実行コンテキスト
     * @param messageId メッセージID
     * @return RequestMessageオブジェクト
     */
    private RequestMessage createRequestMessage(
            HttpRequest request, ServletExecutionContext context, String messageId) {

        // メッセージング基盤用メッセージ作成
        ReceivedMessage receivedMessage = read(context, messageId);

        if (MESSAGING_LOGGER.isInfoEnabled()) {
            emitLog(receivedMessage, getCharsetFromContentType(request.getHeaderMap()));
        }

        RequestMessage requestMessage = fwHeaderDefinition.readFwHeaderFrom(receivedMessage);

        FwHeader header = requestMessage.getFwHeader();
        if (header.hasUserId()) {
            ThreadContext.setUserId(header.getUserId());
        }
        String requestId = ThreadContext.getRequestId();
        requestMessage.setFormatter(createFormatter(requestId + "_RECEIVE"));
        requestMessage.setFormatterOfReply(createFormatter(requestId + "_SEND"));

        requestMessage.setRequestPath(requestId); //メッセージング基盤ではリクエストIDとリクエストパスを同一視する。

        String correlationId = request.getHeader(HTTP_HEADER_CORRELATION_ID);
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            requestMessage.setCorrelationId(correlationId);
        }

        return requestMessage;
    }

    /**
     * HTTPリクエストの入力ストリームから全てのデータを取得し、{@link ReceivedMessage}を返却する。
     *
     * @param context 実行コンテキスト
     * @param messageId メッセージID
     * @return 受信メッセージ
     */
    private ReceivedMessage read(ServletExecutionContext context, String messageId) {
        HttpRequestWrapper request = context.getHttpRequest();

        ReceivedMessage receivedMessage = new ReceivedMessage(readHttpBody(request));
        receivedMessage.setHeaderMap(request.getHeaderMap());
        receivedMessage.setReplyTo(request.getRequestPath());
        receivedMessage.setMessageId(messageId);
        return receivedMessage;
    }

    /**
     * HTTPリクエストのBODY部を読み込む。
     *
     * @param request リクエスト
     * @return BODY部の内容
     */
    private byte[] readHttpBody(HttpRequestWrapper request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = request.getInputStream();
        try {
            int len;
            int readCount = 0;
            byte[] buf = new byte[BODY_READ_BUF_SIZE];
            while ((len = is.read(buf, 0, buf.length)) > 0) {
                baos.write(buf, 0, len);
                readCount += len;
                if (readCount > bodyLengthLimit) {
                    throw new RequestEntityTooLarge();
                }
            }
        } catch (IOException e) {
            throw new MessagingException(e); // can not happen.
        }
        return baos.toByteArray();
    }

    // ----------------------------------------------------------- helper
    /**
     * フォーマットファイル名からレコードフォーマットを生成する。
     *
     * @param formatFileName フォーマットファイル名
     * @return レコードフォーマット
     */
    private DataRecordFormatter createFormatter(String formatFileName) {
        File file = FilePathSetting.getInstance().getFileIfExists(FORMAT_FILE_DIR, formatFileName);
        if (file == null) {
            return null;
        }
        return FormatterFactory.getInstance().createFormatter(file);
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

    /**
     * ContentTypeに設定されている文字セットを取得する。
     * @param headerMap ヘッダ
     * @return Content-Typeヘッダに設定された文字セット、取得できない場合はデフォルト文字セット
     */
    private Charset getCharsetFromContentType(Map<String, String> headerMap) {
        String contentTypeHeader = StringUtil.nullToEmpty(headerMap.get("Content-Type"));
        Matcher matcher = CHARSET_PTN.matcher(contentTypeHeader);
        if (matcher.find()) {
            return Charset.forName(matcher.group(1));
        }
        return Charset.defaultCharset();
    }
    
    /**
     * リクエストのボディストリームから読み込む最大容量を取得する。
     *
     * @return bodyLengthLimit リクエストのボディストリームから読み込む最大容量（単位：バイト）
     */
    public int getBodyLengthLimit() {
        return this.bodyLengthLimit;
    }

    /**
     * リクエストのボディストリームから読み込む最大容量を設定する。
     *
     * @param bodyLengthLimit リクエストのボディストリームから読み込む最大容量（単位：バイト）
     */
    public void setBodyLengthLimit(int bodyLengthLimit) {
        if (bodyLengthLimit < 0) {
            throw new IllegalArgumentException(
                    "bodyLengthLimit must not be negative.");
        }
        this.bodyLengthLimit = bodyLengthLimit;
    }

}
