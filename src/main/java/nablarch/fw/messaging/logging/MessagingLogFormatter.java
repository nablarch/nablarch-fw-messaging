package nablarch.fw.messaging.logging;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FixedLengthDataRecordFormatter;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogUtil;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;

/**
 * メッセージ送受信処理の中でログ出力を行うクラス。
 * 
 * ログファイルはキュー毎に個別に設定できる。
 * ログが出力されるタイミングは以下のとおり
 * <pre>
 * 1. 送信処理完了時 (ローカルキューへのPUT完了直後)
 * 2. 電文受信時
 * </pre>
 * 
 * 出力可能な項目は以下のとおり。
 * <pre>
 * 1. 共通プロトコルヘッダ
 *   - メッセージID (String)
 *   - 関連メッセージID (String)
 *   - 送信宛先キュー論理名 (String)
 *   - 応答宛先キュー論理名 (String)
 *   
 * 2. メッセージボディデータ
 *   - メッセージボディのバイト長 (int)
 *   - メッセージボディ
 *   - メッセージボディのヘキサダンプ
 *   
 *   ※メッセージボディに含まれる個人情報や機密情報はマスクして出力することが可能である(マスク用の設定が必要)
 *   
 * 3. MOM固有プロトコルヘッダ(以下はJmsMessagingProviderの場合)
 *     JMSType
 *     JMSDeliveryMode
 *     JMSPriority
 *     JMSTimestamp
 *     JMSExpiration   
 *     JMSRedelivered
 *     JMSXDeliveryCount
 *     JMSXGroupID
 *     JMSXGroupSeq
 *     JMSXProducerTXID
 *      
 * 4. そのほか
 *     - スレッド名
 *     - メッセージヘッダ
 * </pre>
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class MessagingLogFormatter {
    /** プロパティ名のプレフィックス */
    public static final String PROPS_PREFIX = "messagingLogFormatter.";
    
    /** マスク文字を取得する際に使用するプロパティ名 */
    private static final String PROPS_MASKING_CHAR = PROPS_PREFIX + "maskingChar";
    
    /** 本文のマスク対象のパターンを取得する際に使用するプロパティ名 */
    private static final String PROPS_MASKING_PATTERNS = PROPS_PREFIX + "maskingPatterns";
    
    /** デフォルトのマスク文字 */
    private static final String DEFAULT_MASKING_CHAR = "*";
    
    /** デフォルトのマスク対象のパターン */
    private static final Pattern[] DEFAULT_MASKING_PATTERNS = new Pattern[0];

    /** 多値指定(カンマ区切り)のプロパティを分割する際に使用するパターン */
    private static final Pattern MULTIVALUE_SEPARATOR_PATTERN = Pattern.compile(",");

    /** プロパティ */
    private Map<String, String> props = AppLogUtil.getProps();
    
    /** ログ出力項目  */
    private Map<String, LogItem<MessagingLogContext>> logItems = getLogItems();

    // 送信処理(MessagingContext#send()に関連したログ出力
    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @return ログ文字列
     */
    public String getSentMessageLog(SendingMessage message) {
        return LogUtil.formatMessage(
            sentMessageLogItems, new MessagingLogContext(message)
        );
    }
    
    /** ログ出力項目 */
    private final LogItem<MessagingLogContext>[]
        sentMessageLogItems = getFormattedLogItems(
            logItems, props,
            PROPS_SENT_MESSAGE_FORMAT,
            DEFAULT_SENT_MESSAGE_FORMAT
        );
    
    /** フォーマット定義のプロパティ名 */
    private static final String
        PROPS_SENT_MESSAGE_FORMAT = PROPS_PREFIX + "sentMessageFormat";

    /** デフォルトのフォーマット */
    private static final String DEFAULT_SENT_MESSAGE_FORMAT
        = "@@@@ SENT MESSAGE @@@@"
        + "\n\tthread_name    = [$threadName$]"
        + "\n\tmessage_id     = [$messageId$]"
        + "\n\tdestination    = [$destination$]"
        + "\n\tcorrelation_id = [$correlationId$]" 
        + "\n\treply_to       = [$replyTo$]"
        + "\n\ttime_to_live   = [$timeToLive$]"
        + "\n\tmessage_body   = [$messageBody$]";
    
    // 受信処理(MessagingContext#receive()に関連したログ出力
    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @return ログ文字列
     */
    public String getReceivedMessageLog(ReceivedMessage message) {
        return LogUtil.formatMessage(
            receivedMessageLogItems, new MessagingLogContext(message)
        );
    }
    
    /** ログ出力項目 */
    private final LogItem<MessagingLogContext>[] receivedMessageLogItems = getFormattedLogItems(
        logItems, props,
        PROPS_RECEIVED_MESSAGE_FORMAT,
        DEFAULT_RECEIVED_MESSAGE_FORMAT
    );
    
    /** フォーマット定義のプロパティ名 */
    private static final String
        PROPS_RECEIVED_MESSAGE_FORMAT = PROPS_PREFIX + "receivedMessageFormat";

    /** デフォルトのフォーマット */
    private static final String DEFAULT_RECEIVED_MESSAGE_FORMAT
        = "@@@@ RECEIVED MESSAGE @@@@"
        + "\n\tthread_name    = [$threadName$]"
        + "\n\tmessage_id     = [$messageId$]"
        + "\n\tdestination    = [$destination$]"
        + "\n\tcorrelation_id = [$correlationId$]" 
        + "\n\treply_to       = [$replyTo$]"
        + "\n\tmessage_body   = [$messageBody$]";
    
    // HTTP送信処理(HttpMessagingClient#sendSync(), HttpMessagingDataParseHandler#handle())に関連したログ出力
    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return ログ文字列
     */
    public String getHttpSentMessageLog(SendingMessage message, Charset charset) {
        return LogUtil.formatMessage(
            httpSentMessageLogItems, new MessagingLogContext(message, charset)
        );
    }
    
    /** ログ出力項目 */
    private final LogItem<MessagingLogContext>[]
            httpSentMessageLogItems = getFormattedLogItems(
            logItems, props,
            PROPS_HTTP_SENT_MESSAGE_FORMAT,
            DEFAULT_HTTP_SENT_MESSAGE_FORMAT
        );
    
    /** フォーマット定義のプロパティ名 */
    private static final String
        PROPS_HTTP_SENT_MESSAGE_FORMAT = PROPS_PREFIX + "httpSentMessageFormat";

    /** デフォルトのフォーマット */
    private static final String DEFAULT_HTTP_SENT_MESSAGE_FORMAT
        = "@@@@ HTTP SENT MESSAGE @@@@"
        + "\n\tthread_name    = [$threadName$]"
        + "\n\tmessage_id     = [$messageId$]"
        + "\n\tdestination    = [$destination$]"
        + "\n\tcorrelation_id = [$correlationId$]" 
        + "\n\tmessage_header = [$messageHeader$]"
        + "\n\tmessage_body   = [$messageBody$]";

    // HTTP送信処理(HttpMessagingClient#sendSync(), HttpMessagingDataParseHandler#handle())に関連したログ出力
    /**
     * 同期送信処理開始時に出力されるログ文字列を生成する。
     * @param message 電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return ログ文字列
     */
    public String getHttpReceivedMessageLog(ReceivedMessage message, Charset charset) {
        return LogUtil.formatMessage(
            httpReceivedMessageLogItems, new MessagingLogContext(message, charset)
        );
    }
    
    /** ログ出力項目 */
    private final LogItem<MessagingLogContext>[] 
        httpReceivedMessageLogItems = getFormattedLogItems(
            logItems, props,
            PROPS_HTTP_RECEIVED_MESSAGE_FORMAT,
            DEFAULT_HTTP_RECEIVED_MESSAGE_FORMAT
        );
    
    /** フォーマット定義のプロパティ名 */
    private static final String
        PROPS_HTTP_RECEIVED_MESSAGE_FORMAT = PROPS_PREFIX + "httpReceivedMessageFormat";

    /** デフォルトのフォーマット */
    private static final String DEFAULT_HTTP_RECEIVED_MESSAGE_FORMAT
        = "@@@@ HTTP RECEIVED MESSAGE @@@@"
        + "\n\tthread_name    = [$threadName$]"
        + "\n\tmessage_id     = [$messageId$]"
        + "\n\tdestination    = [$destination$]"
        + "\n\tcorrelation_id = [$correlationId$]" 
        + "\n\tmessage_header = [$messageHeader$]"
        + "\n\tmessage_body   = [$messageBody$]";
    
    /**
     * フォーマット済みのログ出力項目を取得する。
     * @param logItems フォーマット対象のログ出力項目
     * @param props 各種ログ出力の設定情報
     * @param formatPropName フォーマットのプロパティ名
     * @param defaultFormat デフォルトのフォーマット
     * @return フォーマット済みのログ出力項目
     */
    protected LogItem<MessagingLogContext>[]
    getFormattedLogItems(Map<String, LogItem<MessagingLogContext>> logItems,
                         Map<String, String> props,
                         String formatPropName, 
                         String defaultFormat) {
        String format = defaultFormat;
        if (props.containsKey(formatPropName)) {
            format = props.get(formatPropName);
        }
        return LogUtil.createFormattedLogItems(logItems, format);
    }
    
    /**
     * フォーマット対象のログ出力項目を取得する。
     * @return フォーマット対象のログ出力項目
     */
    protected Map<String, LogItem<MessagingLogContext>> getLogItems() {
        Map<String, LogItem<MessagingLogContext>>
            logItems = new HashMap<String, LogItem<MessagingLogContext>>();
        logItems.put("$threadName$",        new ThreadName());
        logItems.put("$messageId$",         new MessageId());
        logItems.put("$destination$",       new Destination());
        logItems.put("$correlationId$",     new CorrelationId());
        logItems.put("$replyTo$",           new ReplyTo());
        logItems.put("$timeToLive$",        new TimeToLive());
        char maskingChar = getMaskingChar(props);
        Pattern[] bodyMaskingPatterns = getBodyMaskingPatterns(props);
        logItems.put("$messageBody$",       new MessageBody(maskingChar, bodyMaskingPatterns));
        logItems.put("$messageBodyHex$",    new MessageBodyHex(maskingChar, bodyMaskingPatterns));
        logItems.put("$messageBodyLength$", new MessageBodyLength());
        logItems.put("$messageHeader$",     new MessageHeader());
        return logItems;
    }
    
    /**
     * マスク文字を取得する。
     * @param props 各種ログの設定情報
     * @return マスク文字
     */
    protected char getMaskingChar(Map<String, String> props) {
        String maskingChar = getProp(props, PROPS_MASKING_CHAR, DEFAULT_MASKING_CHAR);
        if (maskingChar.toCharArray().length != 1) {
            throw new IllegalArgumentException(
                String.format("maskingChar was not char type. maskingChar = [%s]", maskingChar));
        }
        return maskingChar.charAt(0);
    }
    
    /**
     * 本文のマスク対象のパラメータ名を取得する。<br>
     * プロパティの指定がない場合はデフォルト値を返す。
     * @param props 各種ログの設定情報
     * @return マスク対象のパラメータ名
     */
    protected Pattern[] getBodyMaskingPatterns(Map<String, String> props) {
        String patterns = props.get(PROPS_MASKING_PATTERNS);
        if (patterns == null) {
            return DEFAULT_MASKING_PATTERNS;
        }
        String[] splitPatterns = MULTIVALUE_SEPARATOR_PATTERN.split(patterns);
        List<Pattern> maskingPatterns = new ArrayList<Pattern>();
        for (String regex : splitPatterns) {
            regex = regex.trim();
            if (StringUtil.isNullOrEmpty(regex)) {
                continue;
            }
            maskingPatterns.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
        return maskingPatterns.toArray(new Pattern[maskingPatterns.size()]);
    }
    

    /**
     * プロパティを取得する。<br>
     * プロパティの指定がない場合はデフォルト値を返す。
     * @param props 各種ログの設定情報
     * @param propName プロパティ名
     * @param defaultValue プロパティのデフォルト値
     * @return プロパティ
     */
    private String getProp(Map<String, String> props, String propName, String defaultValue) {
        String value = props.get(propName);
        return value != null ? value : defaultValue;
    }
    
    /**
     * ログの出力内容を保持するクラス。
     */
    public static class MessagingLogContext {
        /** 電文オブジェクト */
        private final InterSystemMessage<?> message;
        
        /** 出力に使用する文字セット */
        private final Charset charset;
        
        /**
         * コンストラクタ。
         * @param message 電文オブジェクト
         */
        public MessagingLogContext(InterSystemMessage<?> message) {
            this.message = message;
            this.charset = null;
        }
        
        /**
         * コンストラクタ。
         * @param message 電文オブジェクト
     * @param charset 出力に使用する文字セット
         */
        public MessagingLogContext(InterSystemMessage<?> message, Charset charset) {
            this.message = message;
            this.charset = charset;
        }
        
        /**
         * 電文オブジェクトを取得する。
         * @return 電文オブジェクト
         */
        public InterSystemMessage<?> getMessage() {
            return message;
        }
        
        /**
         * 出力に使用する文字セットを取得する。
         * @return 出力に使用する文字セット
         */
        public Charset getCharset() {
            return charset;
        }
    }
    
    /** 出力項目(スレッド名) */
    public static class ThreadName implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return Thread.currentThread().getName();
        }
    }
    
    /** 出力項目(メッセージID) */
    public static class MessageId implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return ctx.getMessage().getMessageId();
        }
    }
    
    /** 出力項目(宛先キュー名) */
    public static class Destination implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return ctx.getMessage().getDestination();
        }
    }
    
    /** 出力項目(関連メッセージID) */
    public static class CorrelationId implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return ctx.getMessage().getCorrelationId();
        }
    }
    
    /** 出力項目(応答宛先キュー名) */
    public static class ReplyTo implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return ctx.getMessage().getReplyTo();
        }
    }
    
    /** 出力項目(メッセージ有効期間) */
    public static class TimeToLive implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            if (!(ctx.getMessage() instanceof SendingMessage)) {
                return "-";
            }
            SendingMessage message = (SendingMessage) ctx.getMessage();
            return Long.toString(message.getTimeToLive());
        }
    }
    
    /** 出力項目(メッセージボディバイト長) */
    public static class MessageBodyLength implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */
        public String get(MessagingLogContext ctx) {
            return Integer.toString(ctx.getMessage().getBodyBytes().length);
        }
    }

    /**
     * 出力項目(メッセージボディ内容)
     * 
     * 出力文字列はフォーマッターの文字エンコーディングで出力される。
     * このため、データタイプ P/B のフィールド部分は文字化けする。
     * 
     * また、マスク対象パターンが設定されている場合、該当箇所がマスクされて出力される。
     */
    public static class MessageBody implements LogItem<MessagingLogContext> {
        /** マスク文字 */
        private char maskingChar;
        /** マスク対象のパターン */
        private Pattern[] maskingPatterns;
        
        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBody(char maskingChar, Pattern[] maskingPatterns) {
            this.maskingPatterns = maskingPatterns;
            this.maskingChar = maskingChar;
        }
        
        /** {@inheritDoc} */ 
        public String get(MessagingLogContext ctx) {
            InterSystemMessage<?> message = ctx.getMessage();
            
            Charset charset = ctx.getCharset();
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
                    sb.append(bodyText.substring(lastEnd, start));
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
     * 出力項目(メッセージボディ内容)
     * 
     * メッセージボディのヘキサダンプを出力する。
     * 
     * また、マスク対象パターンが設定されている場合、該当箇所がマスクされた後のヘキサダンプが出力される。
     */
    public static class MessageBodyHex extends MessageBody {
        
        /**
         * コンストラクタ。
         * @param maskingChar マスク文字
         * @param maskingPatterns マスク対象のパターン
         */
        public MessageBodyHex(char maskingChar, Pattern[] maskingPatterns) {
            super(maskingChar, maskingPatterns);
        }
        
        /** {@inheritDoc} */ 
        public String get(MessagingLogContext ctx) {
            String bodyString = super.get(ctx);
            if (StringUtil.isNullOrEmpty(bodyString)) {
                return "";
            }
            
            Charset charset = getCharset(ctx.getMessage());
            byte[] bodyBytes = bodyString.getBytes(charset);
            
            return new BigInteger(bodyBytes).toString(16).toUpperCase();
        }
    }
    
    /**
     * 出力項目(メッセージヘッダ)
     * 
     * メッセージヘッダの内容を出力する。
     */
    public static class MessageHeader implements LogItem<MessagingLogContext> {
        /** {@inheritDoc} */ 
        public String get(MessagingLogContext ctx) {
            return ctx.getMessage().getHeaderMap().toString();
        }
    }
}
