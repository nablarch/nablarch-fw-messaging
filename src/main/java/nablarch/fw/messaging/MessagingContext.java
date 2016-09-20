package nablarch.fw.messaging;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

import java.io.Closeable;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FileUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.InterSystemMessage.HeaderName;
import nablarch.fw.messaging.logging.MessagingLogUtil;

/**
 * メッセージングサーバとの間に開かれるセッションに対するラッパー。
 * このクラスのインスタンスはスレッドローカル変数上で管理されており、
 * {@link #getInstance()}を用いてインスタンスを獲得する。
 * 
 * 本クラスでは、以下の機能を提供する。
 * <ul>
 * <li> メッセージ送信
 * <li> メッセージ同期送信
 * <li> メッセージ受信
 * </ul>
 * 
 * @see MessagingProvider
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public abstract class MessagingContext implements Closeable {
    // ------------------------------------- Managing instance on each thread
    /** 各リクエストスレッドに紐づけられているメッセージコンテキスト */
    private static final ThreadLocal<MessagingContext>
        CONTEXT_FOR_EACH_THREAD = new ThreadLocal<MessagingContext>();
    
    /**
     * カレントスレッドに紐づけられているメッセージングコンテキストを返す。
     * 
     * メッセージングコンテキストが現在のスレッド上に存在しない場合は実行時例外
     * を送出する。
     * @return メッセージングコンテキスト
     * @throws IllegalStateException
     *              メッセージングコンテキストが現在のスレッド上に存在しない場合
     */
    public static MessagingContext getInstance() throws IllegalStateException {
        MessagingContext ctx = CONTEXT_FOR_EACH_THREAD.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "there is no messaging context on the current thread."
            );
        }
        return ctx;
    }
    
    /**
     * カレントスレッドにメッセージコンテキストを紐づける。
     * 
     * @param context カレントスレッドに紐づけるメッセージングコンテキスト
     */
    public static void attach(MessagingContext context) {
        CONTEXT_FOR_EACH_THREAD.set(context);
    }
    
    /**
     * カレントスレッド上のメッセージコンテキストを閉じた上で除去する。
     * メッセージングコンテキストがカレントスレッド上に存在しない場合はなにもしない。
     */
    public static void detach() {
        MessagingContext context =  CONTEXT_FOR_EACH_THREAD.get();
        if (context != null) {
            FileUtil.closeQuietly(context);
            CONTEXT_FOR_EACH_THREAD.remove();
        }
    }
    
    // ---------------------------------------------------- Sending a message
    /**
     * メッセージを送信する。
     * 
     * @param message 送信メッセージ
     * @return 送信メッセージのメッセージID
     */
    public String send(SendingMessage message) {
        String messageId = sendMessage(message);
        if (LOGGER.isInfoEnabled()) {
            emitLog(message);
        }
        return messageId;
    }
    
    /**
     * メッセージの同期送信を行う。
     * 
     * プロバイダ側設定のタイムアウト時間を経過した場合はnullを返す。
     * 
     * @param message 送信メッセージ
     * @return 応答メッセージ
     */
    public ReceivedMessage sendSync(SendingMessage message) {
        return sendSync(message, 0);
    }
    
    /**
     * メッセージの同期送信を行う。
     * 
     * メッセージ送信後、応答電文を受信するか、指定した時間が経過するまでブロックする。
     * タイムアウトした場合はnullを返す。
     * 
     * タイムアウト時間に0以下の数値を設定した場合、
     * プロバイダ側のデフォルトタイムアウト時間を経過した場合はnullを返す。
     * 
     * タイムアウト時間が指定された場合は有効期間ヘッダにタイムアウト時間を指定する。
     * 
     * @param message 送信メッセージ
     * @param timeout 応答タイムアウト (単位：ミリ秒、0以下の数値の場合はブロックし続ける)    
     * @return 応答受信メッセージ（タイムアウトした場合はnull）
     */
    public ReceivedMessage sendSync(SendingMessage message, long timeout) {
        String replyQueueName = message.getReplyTo();
        if (isNullOrEmpty(replyQueueName)) {
            throw new IllegalArgumentException("replyTo header must be set.");
        }
        if (0 < timeout) {
            // タイムアウト時間が指定された場合は
            // 有効期間ヘッダにタイムアウト時間を指定する。
            message.setHeader(HeaderName.TIME_TO_LIVE, timeout);
        }
        String messageId = send(message);
        Thread.yield();
        ReceivedMessage reply = receiveSync(replyQueueName, messageId, timeout);
        if (reply == null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.logInfo(
                    "response timeout: could not receive a reply to the message below within " + timeout + "msec. "
                    + MessagingLogUtil.getSentMessageLog(message));
            }
        }
        return reply;
    }
    
    // --------------------------------------------------- Receiving message
    /**
     * 指定した受信キュー上のメッセージを取得する。
     * 
     * キュー上にメッセージが存在していない場合は、メッセージを受信するまで
     * ブロックする。
     * 
     * @param receiveQueue 受信キューの論理名
     * @return 受信したメッセージ
     */
    public ReceivedMessage receiveSync(String receiveQueue) {
        return receiveSync(receiveQueue, null, 0);
    }
    
    /**
     * 指定した受信キュー上のメッセージを取得する。
     * 
     * キュー上にメッセージが存在していない場合は、メッセージを受信するまで
     * ブロックする。
     * 
     * @param receiveQueue 受信キューの論理名
     * @param timeout      応答タイムアウト
     *                      (単位：ミリ秒、0以下の数値の場合はブロックし続ける)
     * @return 受信したメッセージ
     */
    public ReceivedMessage receiveSync(String receiveQueue, long timeout) {
        return receiveSync(receiveQueue, null, timeout);
    }
    
    
    /**
     * 指定した受信キュー上のメッセージを取得する。
     * messageIdが指定されている場合は、当該のメッセージに対する応答電文を
     * 取得する。messageIdが指定されていないばあいは、受信キュー上の任意の電文
     * を取得する。
     * キュー上に取得対象のメッセージが存在しない場合、メッセージを受信するか、
     * 指定した時間が経過する（タイムアウトする）までブロックする。
     * 
     * タイムアウトした場合はnullをかえす。
     * タイムアウト時間に0以下の数値を設定した場合は
     * 応答電文を受信するまでブロックし続ける。
     * 
     * @param receiveQueue 受信キューの論理名
     * @param messageId    送信電文のメッセージID (応答受信でない場合はnull)
     * @param timeout      応答タイムアウト
     *                      (単位：ミリ秒、0以下の数値の場合はブロックし続ける)
     * @return 受信したメッセージ（タイムアウトした場合はnull）
     */
    public ReceivedMessage
    receiveSync(String receiveQueue, String messageId, long timeout) {
        ReceivedMessage received = receiveMessage(receiveQueue, messageId, timeout);
        if (received == null) {
            return null;
        }
        if (LOGGER.isInfoEnabled()) {
            emitLog(received);
        }
        return received;
    }
    
    
    // ------------------------------- must be implemented by MessagingProvider
    /**
     * メッセージを送信する。
     * 
     * @param message 送信メッセージ
     * @return 送信メッセージのメッセージID
     */
    public abstract String sendMessage(SendingMessage message);
    
    /**
     * 指定した受信キュー上のメッセージを取得する。
     * messageIdが指定されている場合は、当該のメッセージに対する応答電文を
     * 取得する。messageIdが指定されていないばあいは、受信キュー上の任意の電文
     * を取得する。
     * キュー上に取得対象のメッセージが存在しない場合、メッセージを受信するか、
     * 指定した時間が経過する（タイムアウトする）までブロックする。
     * 
     * タイムアウトした場合はnullをかえす。
     * タイムアウト時間に0以下の数値を設定した場合は
     * 応答電文を受信するまでブロックし続ける。
     * 
     * @param receiveQueue 受信キューの論理名
     * @param messageId    送信電文のメッセージID (応答受信でない場合はnull)
     * @param timeout      応答タイムアウト
     *                      (単位：ミリ秒、0以下の数値の場合はブロックし続ける)
     * @return 受信したメッセージ（タイムアウトした場合はnull）
     */
    public abstract ReceivedMessage
    receiveMessage(String receiveQueue, String messageId, long timeout);
    
    // ----------------------------------------------------- Termination
    /**
     * 現在のセッションを終了し、保持しているリソースを開放する。
     */
    public abstract void close();
    
    
    // ----------------------------------------------------- helpers
    /**
     * メッセージングの証跡ログを出力する。
     * @param message メッセージオブジェクト
     */
    protected void emitLog(InterSystemMessage<?> message) {
        
        String log = (message instanceof ReceivedMessage)
                   ? MessagingLogUtil.getReceivedMessageLog((ReceivedMessage) message)
                   : MessagingLogUtil.getSentMessageLog((SendingMessage) message);
        LOGGER.logInfo(log);
    }
    
    /** メッセージングログを出力するロガー */
    private static final Logger LOGGER = LoggerManager.get("MESSAGING");
}
