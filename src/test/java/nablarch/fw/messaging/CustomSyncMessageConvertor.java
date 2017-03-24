package nablarch.fw.messaging;

import nablarch.core.ThreadContext;
import nablarch.core.date.SystemTimeUtil;

/**
 * フレームワーク制御ヘッダをカスタマイズするクラス。
 * @author Kiyohito Itoh
 */
public class CustomSyncMessageConvertor extends SyncMessageConvertor {

    /**
     * {@inheritDoc}
     * <pre>
     * 下記のフレームワーク制御ヘッダをヘッダMapに追加後にスーパークラスの処理を呼び出す。
     * 
     *     ユーザID: {@link ThreadContext}から取得したユーザID
     *     送信日時: {@link SystemTimeUtil}から取得したシステム日時
     * </pre>
     */
    @Override
    protected SendingMessage createSendingMessage(MessageSenderSettings settings, SyncMessage requestMessage) {
        requestMessage.getHeaderRecord().put("userId", ThreadContext.getUserId());
        requestMessage.getHeaderRecord().put("sentDateTime", SystemTimeUtil.getDateTimeString());
        return super.createSendingMessage(settings, requestMessage);
    }

    /**
     * {@inheritDoc}
     * <pre>
     * スーパークラスの処理を呼び出し取得した応答電文に対して、
     * 下記のフレームワーク制御ヘッダをヘッダMapに追加する。
     * 
     *     receivedDate: "20111020"
     * </pre>
     */
    @Override
    public SyncMessage convertOnReceiveSync(MessageSenderSettings settings, SyncMessage requestMessage,
                                             SendingMessage sendingMessage, ReceivedMessage receivedMessage) {
        SyncMessage responseMessage = super.convertOnReceiveSync(settings, requestMessage, sendingMessage, receivedMessage);
        responseMessage.getHeaderRecord().put("receivedDate", "20111011");
        return responseMessage;
    }
}
