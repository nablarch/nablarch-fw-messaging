package nablarch.fw.messaging.logging;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FixedLengthDataRecordFormatter;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.log.app.JsonLogFormatterSupport;
import nablarch.core.log.basic.JsonLogObjectBuilder;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.util.StringUtil;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * メッセージ送受信処理の中でログ出力を行うクラス。
 *
 * @author Shuji Kitamura
 */
public class MessagingJsonLogFormatter extends MessagingLogFormatter {

    /** 出力項目(スレッド名)の項目名 */
    private static final String TARGET_NAME_THREAD_NAME = "threadName";
    /** 出力項目(メッセージID)の項目名 */
    private static final String TARGET_NAME_MESSAGE_ID = "messageId";
    /** 出力項目(宛先キュー名)の項目名 */
    private static final String TARGET_NAME_DESTINATION = "destination";
    /** 出力項目(関連メッセージID)の項目名 */
    private static final String TARGET_NAME_CORRELATION_ID = "correlationId";
    /** 出力項目(応答宛先キュー名)の項目名 */
    private static final String TARGET_NAME_REPLY_TO = "replyTo";
    /** 出力項目(メッセージ有効期間)の項目名 */
    private static final String TARGET_NAME_TIME_TO_LIVE = "timeToLive";
    /** 出力項目(メッセージボディ内容)の項目名 */
    private static final String TARGET_NAME_MESSAGE_BODY = "messageBody";
    /** 出力項目(メッセージボディ内容)の項目名 */
    private static final String TARGET_NAME_MESSAGE_BODY_HEX = "messageBodyHex";
    /** 出力項目(メッセージボディバイト長)の項目名 */
    private static final String TARGET_NAME_MESSAGE_BODY_LENGTH = "messageBodyLength";
    /** 出力項目(メッセージヘッダ)の項目名 */
    private static final String TARGET_NAME_MESSAGE_HEADER = "messageHeader";

    /** リクエスト処理開始時の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_SENT_MESSAGE_TARGETS = PROPS_PREFIX + "sentMessageTargets";
    /** hiddenパラメータ復号後の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_RECEIVED_MESSAGE_TARGETS = PROPS_PREFIX + "receivedMessageTargets";
    /** ディスパッチ先クラス決定後の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_HTTP_SENT_MESSAGE_TARGETS = PROPS_PREFIX + "httpSentMessageTargets";
    /** リクエスト処理終了時の出力項目を取得する際に使用するプロパティ名 */
    private static final String PROPS_HTTP_RECEIVED_MESSAGE_TARGETS = PROPS_PREFIX + "httpReceivedMessageTargets";

    /** デフォルトのリクエスト処理開始時のフォーマット */
    private static final String DEFAULT_SENT_MESSAGE_TARGETS
            = "threadName,messageId,destination,correlationId,replyTo,timeToLive,messageBody";
    /** デフォルトのhiddenパラメータ復号後のフォーマット */
    private static final String DEFAULT_RECEIVED_MESSAGE_TARGETS
            = "threadName,messageId,destination,correlationId,replyTo,messageBody";
    /** デフォルトのディスパッチ先クラス決定後のフォーマット */
    private static final String DEFAULT_HTTP_SENT_MESSAGE_TARGETS
            = "threadName,messageId,destination,correlationId,messageHeader,messageBody";
    /** デフォルトのリクエスト処理終了時のフォーマット */
    private static final String DEFAULT_HTTP_RECEIVED_MESSAGE_TARGETS
            = "threadName,messageId,destination,correlationId,messageHeader,messageBody";

    /** リクエスト処理開始時のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<MessagingLogContext>> sentMessageTargets;
    /** hiddenパラメータ復号後のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<MessagingLogContext>> receivedMessageTargets;
    /** ディスパッチ先クラス決定後のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<MessagingLogContext>> httpSentMessageTargets;
    /** リクエスト処理終了時のフォーマット済みのログ出力項目 */
    private List<JsonLogObjectBuilder<MessagingLogContext>> httpReceivedMessageTargets;

    /** 各種ログのJSONフォーマット支援オブジェクト */
    private JsonLogFormatterSupport support;

    /**
     * コンストラクタ。
     */
    public MessagingJsonLogFormatter() {
        initialize(AppLogUtil.getProps());
    }

    /**
     * 初期化。
     * フォーマット済みのログ出力項目を初期化する。
     * @param props 各種ログ出力の設定情報
     */
    protected void initialize(Map<String, String> props) {

        support = new JsonLogFormatterSupport(
                new JsonSerializationSettings(props, PROPS_PREFIX, AppLogUtil.getFilePath()));

        Map<String, JsonLogObjectBuilder<MessagingLogContext>> objectBuilders = getObjectBuilders(props);

        sentMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_SENT_MESSAGE_TARGETS, DEFAULT_SENT_MESSAGE_TARGETS);
        receivedMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_RECEIVED_MESSAGE_TARGETS, DEFAULT_RECEIVED_MESSAGE_TARGETS);
        httpSentMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_HTTP_SENT_MESSAGE_TARGETS, DEFAULT_HTTP_SENT_MESSAGE_TARGETS);
        httpReceivedMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_HTTP_RECEIVED_MESSAGE_TARGETS, DEFAULT_HTTP_RECEIVED_MESSAGE_TARGETS);
    }

    /**
     * フォーマット対象のログ出力項目を取得する。
     * @param props 各種ログ出力の設定情報
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, JsonLogObjectBuilder<MessagingLogContext>> getObjectBuilders(Map<String, String> props) {

        Map<String, JsonLogObjectBuilder<MessagingLogContext>> objectBuilders
                = new HashMap<String, JsonLogObjectBuilder<MessagingLogContext>>();

        char maskingChar = getMaskingChar(props);
        Pattern[] maskingPatterns = getBodyMaskingPatterns(props);

        objectBuilders.put(TARGET_NAME_THREAD_NAME, new ThreadNameBuilder());
        objectBuilders.put(TARGET_NAME_MESSAGE_ID, new MessageIdBuilder());
        objectBuilders.put(TARGET_NAME_DESTINATION, new DestinationBuilder());
        objectBuilders.put(TARGET_NAME_CORRELATION_ID, new CorrelationIdBuilder());
        objectBuilders.put(TARGET_NAME_REPLY_TO, new ReplyToBuilder());
        objectBuilders.put(TARGET_NAME_TIME_TO_LIVE, new TimeToLiveBuilder());
        objectBuilders.put(TARGET_NAME_MESSAGE_BODY, new MessageBodyBuilder(maskingChar, maskingPatterns));
        objectBuilders.put(TARGET_NAME_MESSAGE_BODY_HEX, new MessageBodyHexBuilder(maskingChar, maskingPatterns));
        objectBuilders.put(TARGET_NAME_MESSAGE_BODY_LENGTH, new MessageBodyLengthBuilder());
        objectBuilders.put(TARGET_NAME_MESSAGE_HEADER, new MessageHeaderBuilder());
        
        return objectBuilders;
    }

    /**
     * ログ出力項目を取得する。
     * @param objectBuilders オブジェクトビルダー
     * @param props 各種ログ出力の設定情報
     * @param targetsPropName 出力項目のプロパティ名
     * @param defaultTargets デフォルトの出力項目
     * @return ログ出力項目
     */
    private List<JsonLogObjectBuilder<MessagingLogContext>> getStructuredTargets(
            Map<String, JsonLogObjectBuilder<MessagingLogContext>> objectBuilders,
            Map<String, String> props,
            String targetsPropName, String defaultTargets) {

        String targetsStr = props.get(targetsPropName);
        if (StringUtil.isNullOrEmpty(targetsStr)) targetsStr = defaultTargets;

        List<JsonLogObjectBuilder<MessagingLogContext>> structuredTargets
                = new ArrayList<JsonLogObjectBuilder<MessagingLogContext>>();

        String[] targets = targetsStr.split(",");
        Set<String> keys = new HashSet<String>(targets.length);
        for (String target: targets) {
            String key = target.trim();
            if (!StringUtil.isNullOrEmpty(key) && !keys.contains(key)) {
                keys.add(key);
                if (objectBuilders.containsKey(key)) {
                    structuredTargets.add(objectBuilders.get(key));
                } else {
                    throw new IllegalArgumentException(
                            String.format("[%s] is unknown target. property name = [%s]", key, targetsPropName));
                }
            }
        }

        return structuredTargets;
    }

    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @return ログ文字列
     */
    @Override
    public String getSentMessageLog(SendingMessage message) {
        return support.getStructuredMessage(sentMessageTargets, new MessagingLogContext(message));
    }

    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @return ログ文字列
     */
    @Override
    public String getReceivedMessageLog(ReceivedMessage message) {
        return support.getStructuredMessage(receivedMessageTargets, new MessagingLogContext(message));
    }

    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return ログ文字列
     */
    @Override
    public String getHttpSentMessageLog(SendingMessage message, Charset charset) {
        return support.getStructuredMessage(httpSentMessageTargets, new MessagingLogContext(message, charset));
    }

    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return ログ文字列
     */
    @Override
    public String getHttpReceivedMessageLog(ReceivedMessage message, Charset charset) {
        return support.getStructuredMessage(httpReceivedMessageTargets, new MessagingLogContext(message, charset));
    }

    /**
     * 出力項目(スレッド名)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ThreadNameBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_THREAD_NAME, Thread.currentThread().getName());
        }
    }

    /**
     * 出力項目(メッセージID)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageIdBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_ID, context.getMessage().getMessageId());
        }
    }

    /**
     * 出力項目(宛先キュー名)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class DestinationBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_DESTINATION, context.getMessage().getDestination());
        }
    }

    /**
     * 出力項目(関連メッセージID)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class CorrelationIdBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_CORRELATION_ID, context.getMessage().getCorrelationId());
        }
    }

    /**
     * 出力項目(応答宛先キュー名)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class ReplyToBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_REPLY_TO, context.getMessage().getReplyTo());
        }
    }

    /**
     * 出力項目(メッセージ有効期間)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class TimeToLiveBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            if (!(context.getMessage() instanceof SendingMessage)) {
                structuredObject.put(TARGET_NAME_TIME_TO_LIVE, null);
            } else {
                SendingMessage message = (SendingMessage) context.getMessage();
                structuredObject.put(TARGET_NAME_TIME_TO_LIVE, message.getTimeToLive());
            }
        }
    }

    /**
     * 出力項目(メッセージボディ内容)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBodyBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /** マスク文字 */
        private final char maskingChar;
        /** マスク対象のパターン */
        private final Pattern[] maskingPatterns;

        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBodyBuilder(char maskingChar, Pattern[] maskingPatterns) {
            this.maskingPatterns = maskingPatterns;
            this.maskingChar = maskingChar;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_BODY, getMaskedBodyText(context));
        }

        protected String getMaskedBodyText(MessagingLogContext context) {
            InterSystemMessage<?> message = context.getMessage();

            Charset charset = context.getCharset();
            if (charset == null) {
                charset = getCharset(message);
            }
            byte[] bodyBytes = message.getBodyBytes();

            return maskBodyText(new String(bodyBytes, charset));
        }

        /**
         * メッセージからエンコーディングを取得する。
         * 取得できない場合は"iso-8859-1"を返却する。
         * @param message 対象のメッセージ
         * @return エンコーディング
         */
        protected Charset getCharset(InterSystemMessage<?> message) {
            DataRecordFormatter formatter = message.getFormatter();
            String encoding = "iso-8859-1";
            if (formatter instanceof FixedLengthDataRecordFormatter) {
                formatter.initialize();
                encoding = ((FixedLengthDataRecordFormatter) formatter)
                        .getDefaultEncoding()
                        .name();
            }
            return Charset.forName(encoding);
        }

        /**
         * マスキングパターンに従い、メッセージ本文のマスク処理を行う。
         * @param bodyText メッセージ本文
         * @return マスク済みのメッセージ本文
         */
        private String maskBodyText(String bodyText) {
            for (Pattern p : maskingPatterns) {
                StringBuilder sb = new StringBuilder();

                Matcher m = p.matcher(bodyText);
                int lastEnd = 0;
                while (m.find(lastEnd)) {
                    int start = m.start(1);
                    int end = m.end(1);
                    sb.append(bodyText, lastEnd, start);
                    sb.append(StringUtil.repeat(maskingChar, (end - start)));
                    lastEnd = end;
                }
                sb.append(bodyText.substring(lastEnd));

                bodyText = sb.toString();
            }

            return bodyText;
        }
    }

    /**
     * 出力項目(メッセージボディ内容)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBodyHexBuilder extends MessageBodyBuilder {

        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBodyHexBuilder(char maskingChar, Pattern[] maskingPatterns) {
            super(maskingChar, maskingPatterns);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            String bodyString = getMaskedBodyText(context);
            if (StringUtil.isNullOrEmpty(bodyString)) {
                structuredObject.put(TARGET_NAME_MESSAGE_BODY_HEX, "");
            } else {
                Charset charset = getCharset(context.getMessage());
                byte[] bodyBytes = bodyString.getBytes(charset);
                String value = new BigInteger(bodyBytes).toString(16).toUpperCase();
                structuredObject.put(TARGET_NAME_MESSAGE_BODY_HEX, value);
            }
        }
    }

    /**
     * 出力項目(メッセージボディバイト長)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBodyLengthBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_BODY_LENGTH, context.getMessage().getBodyBytes().length);
        }
    }

    /**
     * 出力項目(メッセージヘッダ)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageHeaderBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_HEADER, context.getMessage().getHeaderMap());
        }
    }
}