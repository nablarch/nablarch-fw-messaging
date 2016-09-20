package nablarch.fw.messaging;

import java.util.Map;

import nablarch.core.util.annotation.Published;

/**
 * メッセージ同期送信に使用する電文を変換するクラス。
 * <pre>
 * 本クラスは下記の変換を行う。
 * 
 *     要求電文({@link SyncMessage})→送信電文({@link SendingMessage})[初回送信時、再送時]
 *     受信電文({@link ReceivedMessage})→応答電文({@link SyncMessage})[受信時]
 * 
 * </pre>
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class SyncMessageConvertor {

    /**
     * 要求電文を送信電文に変換する。(初回送信時)
     * <pre>
     * 指定された設定情報をもとに送信電文を作成する。
     * フレームワーク制御ヘッダ以外の設定は
     * {@link #createSendingMessage(MessageSenderSettings, SyncMessage)}メソッドに委譲する。
     * 
     * {@link FwHeader}を使用して下記のフレームワーク制御ヘッダを設定する。
     * 
     *     リクエストIDヘッダ: 送信電文の設定情報が保持している送信用リクエストID
     *     再送電文フラグ: 初回を表す"0"。再送しない場合は設定しない
     * </pre>
     * @param settings 送信電文の設定情報
     * @param requestMessage 要求電文
     * @return 送信電文
     */
    public SendingMessage convertOnSendSync(MessageSenderSettings settings, SyncMessage requestMessage) {
        FwHeader fwHeader = new FwHeader();
        fwHeader.setRequestId(settings.getSendingRequestId());
        if (settings.getRetryCount() > 0) {
            fwHeader.setResendFlag("0");
        }
        requestMessage.getHeaderRecord().putAll(fwHeader);
        return createSendingMessage(settings, requestMessage);
    }

    /**
     * 要求電文を送信電文に変換する。(再送時)
     * <pre>
     * 指定された設定情報をもとに送信電文を作成する。
     * フレームワーク制御ヘッダ以外の設定は
     * {@link #createSendingMessage(MessageSenderSettings, SyncMessage)}メソッドに委譲する。
     * 
     * {@link FwHeader}を使用して下記のフレームワーク制御ヘッダを設定する。
     * 
     *     リクエストIDヘッダ: 送信電文の設定情報が保持している送信用リクエストID
     *     再送電文フラグ: 再送を表す"1"
     * 
     * 再送する送信電文には、タイムアウトした送信電文と関連付けるために、
     * タイムアウトした送信電文のメッセージIDを設定する。
     * 
     * 本実装ではリトライ回数を使用しない。
     * </pre>
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @param timeoutMessage タイムアウトした送信電文
     * @param retryCount リトライ回数。初回送信時は0
     * @return 送信電文
     */
    public SendingMessage convertOnRetry(MessageSenderSettings settings, SyncMessage requestMessage,
                                          SendingMessage timeoutMessage, int retryCount) {
        FwHeader fwHeader = new FwHeader();
        fwHeader.setRequestId(settings.getSendingRequestId());
        fwHeader.setResendFlag("1");
        requestMessage.getHeaderRecord().putAll(fwHeader);
        SendingMessage sendingMessage = createSendingMessage(settings, requestMessage);
        sendingMessage.setCorrelationId(timeoutMessage.getMessageId());
        return sendingMessage;
    }

    /**
     * 指定された設定情報をもとに送信電文を作成する。
     * <pre>
     * 設定情報から下記の項目を送信電文に設定する。
     * 
     *     送信宛先キューの論理名
     *     応答宛先キューの論理名
     * 
     * メッセージボディ部にヘッダとデータを追加する。
     * 下記のレコードタイプを使用する。
     * 
     *     ヘッダ: "header"
     *     データ: "data"
     * 
     * </pre>
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @return 送信電文
     */
    protected SendingMessage createSendingMessage(MessageSenderSettings settings, SyncMessage requestMessage) {
        SendingMessage message = new SendingMessage();
        message.setDestination(settings.getDestination());
        message.setReplyTo(settings.getReplyTo());
        message.setFormatter(settings.getHeaderFormatter()).addRecord("header", requestMessage.getHeaderRecord());
        message.setFormatter(settings.getSendingDataFormatter());
        for (Map<String, Object> dataRecord : requestMessage.getDataRecords()) {
            message.addRecord("data", dataRecord);
        }
        return message;
    }

    /**
     * 受信電文を応答電文に変換する。(受信時)
     * <pre>
     * 設定情報が提供するフォーマッタを使用して、
     * 受信電文のメッセージボディ部からヘッダとデータを取り出し、応答電文を作成する。
     * 
     * 本実装では送信電文を使用しない。
     * </pre>
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @param sendingMessage 送信電文
     * @param receivedMessage 受信電文
     * @return 応答電文
     */
    public SyncMessage convertOnReceiveSync(
            MessageSenderSettings settings, SyncMessage requestMessage,
            SendingMessage sendingMessage, ReceivedMessage receivedMessage) {
        SyncMessage responseMessage = new SyncMessage(requestMessage.getRequestId());
        responseMessage.setHeaderRecord(
                           receivedMessage.setFormatter(settings.getHeaderFormatter()).readRecord())
                       .getDataRecords().addAll(
                           receivedMessage.setFormatter(settings.getReceivedDataFormatter()).readRecords());
        return responseMessage;
    }
}
