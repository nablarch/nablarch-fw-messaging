package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;


/**
 * 送信した電文に対する応答電文をタイムアウト時間内に受信することができなかった場合に
 * 送出される例外。
 * 
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class MessageSendSyncTimeoutException extends MessagingException {

    /** リトライ回数 */
    private final int retryCount;

    /**
     * コンストラクタ。
     * @param message エラーメッセージ
     * @param retryCount リトライ回数
     */
    public MessageSendSyncTimeoutException(String message, int retryCount) {
        super(message);
        this.retryCount = retryCount;
    }

    /**
     * リトライ回数を取得する。
     * @return リトライ回数
     */
    public int getRetryCount() {
        return retryCount;
    }
}
