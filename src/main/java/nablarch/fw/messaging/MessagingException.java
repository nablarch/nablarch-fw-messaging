package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;

/**
 * メッセージ処理において問題が発生した場合に送出される実行時例外。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class MessagingException extends RuntimeException {
    /**
     * デフォルトコンストラクタ。
     */
    public MessagingException() {
        super();
    }

    /**
     * コンストラクタ。
     * @param message エラーメッセージ
     * @param cause   起因となる例外
     */
    public MessagingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * コンストラクタ。
     * @param message エラーメッセージ
     */
    public MessagingException(String message) {
        super(message);
    }

    /**
     * コンストラクタ。
     * @param cause   起因となる例外
     */
    public MessagingException(Throwable cause) {
        super(cause);
    }
}
