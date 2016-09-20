package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;

/**
 * HTTP通信時に使用するメッセージID生成。
 * @author Masaya Seko
 */
@Published(tag = "architect")
public interface HttpMessageIdGenerator {
    /**
     * HTTP通信で使用するメッセージIDを採番する。
     * @return メッセージID
     */
    String generateId();
}
