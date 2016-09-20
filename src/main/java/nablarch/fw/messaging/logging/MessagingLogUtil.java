package nablarch.fw.messaging.logging;

import java.nio.charset.Charset;
import java.util.Map;

import nablarch.core.log.LogUtil;
import nablarch.core.log.LogUtil.ObjectCreator;
import nablarch.core.log.app.AppLogUtil;
import nablarch.core.util.ObjectUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;

/**
 * メッセージング処理中のログの出力内容に関連した処理を行うユーティリティクラス。
 * クラスローダから生成した{@link nablarch.fw.messaging.logging.MessagingLogFormatter}に処理を委譲する。
 *
 * @author Iwauo Tajima
 */
public final class MessagingLogUtil {
    /** 隠蔽コンストラクタ */
    private MessagingLogUtil() {
    }
    
    /** {@link nablarch.fw.messaging.logging.MessagingLogFormatter}のクラス名 */
    private static final String PROPS_CLASS_NAME = MessagingLogFormatter.PROPS_PREFIX + "className";
    
    /** {@link nablarch.fw.messaging.logging.MessagingLogFormatter}を生成する{@link ObjectCreator} */
    private static final ObjectCreator<MessagingLogFormatter>
        MESSAGING_LOG_FORMATTER_CREATOR = new ObjectCreator<MessagingLogFormatter>() {
        
        public MessagingLogFormatter create() {
            MessagingLogFormatter formatter = null;
            Map<String, String> props = AppLogUtil.getProps();
            if (props.containsKey(PROPS_CLASS_NAME)) {
                String className =  props.get(PROPS_CLASS_NAME);
                formatter = ObjectUtil.createInstance(className);
            } else {
                formatter = new MessagingLogFormatter();
            }
            return formatter;
        }
    };
    
    /** クラスローダに紐付く{@link nablarch.fw.messaging.logging.MessagingLogFormatter}を生成する。 */
    public static void initialize() {
        getLogWriter();
    }
    
    /**
     * クラスローダに紐付く{@link nablarch.fw.messaging.logging.MessagingLogFormatter}を取得する。
     * @return {@link nablarch.fw.messaging.logging.MessagingLogFormatter}
     */
    private static MessagingLogFormatter getLogWriter() {
        return LogUtil.getObjectBoundToClassLoader(MESSAGING_LOG_FORMATTER_CREATOR);
    }
    
    /**
     * 電文送信時に出力するログの内容を返す。
     * @param message 送信電文オブジェクト
     * @return 電文送信時に出力するログ内容
     */
    @Published(tag = "architect")
    public static String getSentMessageLog(SendingMessage message) {
        return getLogWriter().getSentMessageLog(message);
    }
    
    /**
     * 電文受信時に出力するログの内容を返す。
     * @param message 送信電文オブジェクト
     * @return 電文受信時に出力するログ内容
     */
    @Published(tag = "architect")
    public static String getReceivedMessageLog(ReceivedMessage message) {
        return getLogWriter().getReceivedMessageLog(message);
    }
    
    /**
     * HTTP電文送信時に出力するログの内容を返す。
     * @param message 送信電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return 電文送信時に出力するログ内容
     */
    @Published(tag = "architect")
    public static String getHttpSentMessageLog(SendingMessage message, Charset charset) {
        return getLogWriter().getHttpSentMessageLog(message, charset);
    }
    
    /**
     * HTTP電文受信時に出力するログの内容を返す。
     * @param message 送信電文オブジェクト
     * @param charset 出力に使用する文字セット
     * @return 電文受信時に出力するログ内容
     */
    @Published(tag = "architect")
    public static String getHttpReceivedMessageLog(ReceivedMessage message, Charset charset) {
        return getLogWriter().getHttpReceivedMessageLog(message, charset);
    }
}
