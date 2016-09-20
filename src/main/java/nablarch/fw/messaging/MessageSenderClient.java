package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;

/**
 * {@link MessageSender}から呼び出される通信機能の基本APIの実装系を提供するモジュールが実装するインターフェース。<br />
 * <p>
 * 類似の機能を持つインターフェースとして、{@link MessagingProvider}が存在する。<br />
 * {@link MessagingProvider}との主な相違点は、キューの存在を仮定した実装が存在していないことである。
 * </p>
 * 
 * @author Masaya Seko
 */
@Published(tag = "architect")
public interface MessageSenderClient {
    /**
     * 同期通信を行う。
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @return 応答電文
     */
    SyncMessage sendSync(MessageSenderSettings settings, SyncMessage requestMessage);
}
