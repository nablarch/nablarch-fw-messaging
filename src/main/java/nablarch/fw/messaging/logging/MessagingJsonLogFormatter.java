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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * メッセージ送受信処理の中で出力するためのログをJSON形式でフォーマットするクラス。
 *
 * @author Shuji Kitamura
 */
public class MessagingJsonLogFormatter extends MessagingLogFormatter {

    /** ラベルの項目名 */
    private static final String TARGET_NAME_LABEL = "label";
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

    /** MOM送信メッセージの出力項目のプロパティ名 */
    private static final String PROPS_SENT_MESSAGE_TARGETS = PROPS_PREFIX + "sentMessageTargets";
    /** MOM受信メッセージの出力項目のプロパティ名 */
    private static final String PROPS_RECEIVED_MESSAGE_TARGETS = PROPS_PREFIX + "receivedMessageTargets";
    /** HTTP送信メッセージの出力項目のプロパティ名 */
    private static final String PROPS_HTTP_SENT_MESSAGE_TARGETS = PROPS_PREFIX + "httpSentMessageTargets";
    /** HTTP受信メッセージの出力項目のプロパティ名 */
    private static final String PROPS_HTTP_RECEIVED_MESSAGE_TARGETS = PROPS_PREFIX + "httpReceivedMessageTargets";

    /** MOM送信メッセージのラベルのプロパティ名 */
    private static final String PROPS_SENT_MESSAGE_LABEL = PROPS_PREFIX + "sentMessageLabel";
    /** MOM受信メッセージのラベルのプロパティ名 */
    private static final String PROPS_RECEIVED_MESSAGE_LABEL = PROPS_PREFIX + "receivedMessageLabel";
    /** HTTP送信メッセージのラベルのプロパティ名 */
    private static final String PROPS_HTTP_SENT_MESSAGE_LABEL = PROPS_PREFIX + "httpSentMessageLabel";
    /** HTTP受信メッセージのラベルのプロパティ名 */
    private static final String PROPS_HTTP_RECEIVED_MESSAGE_LABEL = PROPS_PREFIX + "httpReceivedMessageLabel";

    /** デフォルトのMOM送信メッセージの出力項目 */
    private static final String DEFAULT_SENT_MESSAGE_TARGETS
            = "label,threadName,messageId,destination,correlationId,replyTo,timeToLive,messageBody";
    /** デフォルトのMOM受信メッセージの出力項目 */
    private static final String DEFAULT_RECEIVED_MESSAGE_TARGETS
            = "label,threadName,messageId,destination,correlationId,replyTo,messageBody";
    /** デフォルトのHTTP送信メッセージの出力項目 */
    private static final String DEFAULT_HTTP_SENT_MESSAGE_TARGETS
            = "label,threadName,messageId,destination,correlationId,messageHeader,messageBody";
    /** デフォルトのHTTP受信メッセージの出力項目 */
    private static final String DEFAULT_HTTP_RECEIVED_MESSAGE_TARGETS
            = "label,threadName,messageId,destination,correlationId,messageHeader,messageBody";

    /** デフォルトのMOM送信メッセージのラベル */
    private static final String DEFAULT_SENT_MESSAGE_LABEL = "SENT MESSAGE";
    /** デフォルトのMOM受信メッセージのラベル */
    private static final String DEFAULT_RECEIVED_MESSAGE_LABEL = "RECEIVED MESSAGE";
    /** デフォルトのHTTP送信メッセージのラベル */
    private static final String DEFAULT_HTTP_SENT_MESSAGE_LABEL = "HTTP SENT MESSAGE";
    /** デフォルトのHTTP受信メッセージのラベル */
    private static final String DEFAULT_HTTP_RECEIVED_MESSAGE_LABEL = "HTTP RECEIVED MESSAGE";

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

        String label = getProp(props, PROPS_SENT_MESSAGE_LABEL, DEFAULT_SENT_MESSAGE_LABEL);
        objectBuilders.put(TARGET_NAME_LABEL, new LabelBuilder(label));
        sentMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_SENT_MESSAGE_TARGETS, DEFAULT_SENT_MESSAGE_TARGETS);

        label = getProp(props, PROPS_RECEIVED_MESSAGE_LABEL, DEFAULT_RECEIVED_MESSAGE_LABEL);
        objectBuilders.put(TARGET_NAME_LABEL, new LabelBuilder(label));
        receivedMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_RECEIVED_MESSAGE_TARGETS, DEFAULT_RECEIVED_MESSAGE_TARGETS);

        label = getProp(props, PROPS_HTTP_SENT_MESSAGE_LABEL, DEFAULT_HTTP_SENT_MESSAGE_LABEL);
        objectBuilders.put(TARGET_NAME_LABEL, new LabelBuilder(label));
        httpSentMessageTargets = getStructuredTargets(objectBuilders, props, PROPS_HTTP_SENT_MESSAGE_TARGETS, DEFAULT_HTTP_SENT_MESSAGE_TARGETS);

        label = getProp(props, PROPS_HTTP_RECEIVED_MESSAGE_LABEL, DEFAULT_HTTP_RECEIVED_MESSAGE_LABEL);
        objectBuilders.put(TARGET_NAME_LABEL, new LabelBuilder(label));
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
        if (StringUtil.isNullOrEmpty(targetsStr)) {
            targetsStr = defaultTargets;
        }

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
     * {@inheritDoc}
     */
    @Override
    public String getSentMessageLog(SendingMessage message) {
        return support.getStructuredMessage(sentMessageTargets, new MessagingLogContext(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReceivedMessageLog(ReceivedMessage message) {
        return support.getStructuredMessage(receivedMessageTargets, new MessagingLogContext(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHttpSentMessageLog(SendingMessage message, Charset charset) {
        return support.getStructuredMessage(httpSentMessageTargets, new MessagingLogContext(message, charset));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHttpReceivedMessageLog(ReceivedMessage message, Charset charset) {
        return support.getStructuredMessage(httpReceivedMessageTargets, new MessagingLogContext(message, charset));
    }

    /**
     * ラベルを処理するクラス。
     * @author Shuji Kitamura
     */
    private static class LabelBuilder implements JsonLogObjectBuilder<MessagingLogContext> {

        private final String label;

        /**
         * コンストラクタ。
         * @param label ラベル
         */
        LabelBuilder(String label) {
            this.label = label;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_LABEL, label);
        }
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
        private final MessageBody messageBody;

        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBodyBuilder(char maskingChar, Pattern[] maskingPatterns) {
            messageBody = new MessageBody(maskingChar, maskingPatterns);
        }

        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_BODY, messageBody.get(context));
        }
    }

    /**
     * 出力項目(メッセージボディ内容)を処理するクラス。
     * @author Shuji Kitamura
     */
    private static class MessageBodyHexBuilder implements JsonLogObjectBuilder<MessagingLogContext> {
        private final MessageBodyHex messageBodyHex;

        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBodyHexBuilder(char maskingChar, Pattern[] maskingPatterns) {
            messageBodyHex = new MessageBodyHex(maskingChar, maskingPatterns);
        }

        @Override
        public void build(Map<String, Object> structuredObject, MessagingLogContext context) {
            structuredObject.put(TARGET_NAME_MESSAGE_BODY_HEX, messageBodyHex.get(context));
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
