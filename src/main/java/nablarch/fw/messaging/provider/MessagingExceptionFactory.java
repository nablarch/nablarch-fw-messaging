package nablarch.fw.messaging.provider;

import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.MessagingException;

/**
 * 発生した例外の内容に応じた{@link MessagingException}を生成するインタフェース。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public interface MessagingExceptionFactory {

    /**
     * 発生した例外の内容に応じた{@link MessagingException}を生成する。
     * @param message エラーメッセージ
     * @param cause 発生した例外
     * @return 発生した例外の内容に応じた{@link MessagingException}
     */
    MessagingException createMessagingException(String message, Throwable cause);
}
