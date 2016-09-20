package nablarch.fw.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FileUtil;
import nablarch.core.util.annotation.Published;

/**
 * 対外システムに対するメッセージの同期送信を行うユーティリティクラス。
 * <p/>
 * 本ユーティリティはキューを使用した通信と、HTTP通信をサポートする。
 * <p/>
 * キューを使用した通信について<br/>
 * 本ユーティリティはフレームワーク制御ヘッダの利用を前提としており、
 * 再送電文フラグを利用した再送/タイムアウト制御等の機能を実装している。
 * <br>
 * キューを使用した通信では、カレントスレッドに紐づけられている{@link MessagingContext}を利用してメッセージ送信を行う。
 * そのため、{@link nablarch.fw.messaging.handler.MessagingContextHandler}をハンドラキューに追加する必要がある。
 * MessagingContextの設定方法については{@link MessageSenderSettings#MessageSenderSettings(String)}を参照。
 *
 * @author Kiyohito Itoh
 */
@Published
public final class MessageSender {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(MessageSender.class);

    /** デフォルトのSyncMessageConvertor */
    private static final SyncMessageConvertor DEFAULT_MESSAGE_CONVERTOR = new SyncMessageConvertor();

    /**
     * 隠蔽コンストラクタ。
     */
    private MessageSender() {
    }

    /**
     * 対外システムにメッセージを送信し、応答された電文を返す。
     * <p/>
     * 電文の設定情報について<br>
     * {@link MessageSenderSettings#MessageSenderSettings(String)}を実行して、対象リクエストの設定情報を取得する。
     * <p/>
     * 要求電文の作成について<br>
     * 要求電文の作成処理は{@link SyncMessageConvertor}に委譲する。
     * SyncMessageConvertorの取得方法は、{@link #getSyncMessageConvertor(MessageSenderSettings)}メソッドのJavaDocを参照。
     * デフォルトでは、フレームワークが提供するSyncMessageConvertorをそのまま使用する。
     * <p/>
     * メッセージの再送について<br>
     * キューを使用した通信では、設定によりリトライ回数が指定されている場合、
     * タイムアウト発生時に指定された回数まで再送を行う。<br>
     * HTTP通信では再送を行わない。
     * <p/>
     * メッセージ送受信中にエラーが発生した場合、{@link SyncMessagingEventHook}にエラー処理を委譲する。
     * SyncMessagingEventHookの設定方法は{@link MessageSenderSettings#MessageSenderSettings(String)}のJavaDocを参照。
     *
     * @param requestMessage 要求電文
     * @return 応答電文
     * @throws IllegalArgumentException 要求電文の設定情報に問題がある場合
     * @throws MessageSendSyncTimeoutException タイムアウトが発生し、同期送信が正常終了しなかった場合
     */
    public static SyncMessage sendSync(SyncMessage requestMessage) throws MessageSendSyncTimeoutException {
        MessageSenderSettings settings = new MessageSenderSettings(requestMessage.getRequestId());

        //送信前後処理について、「正常終了時」「エラー終了時」には逆順のリストが必要になるため、用意する。
        List<SyncMessagingEventHook> syncMessagingEventHookReverseList = new ArrayList<SyncMessagingEventHook>(settings.getSyncMessagingEventHookList()); 
        Collections.reverse(syncMessagingEventHookReverseList);

        //送信前処理
        for (SyncMessagingEventHook syncMessagingEventHook : settings.getSyncMessagingEventHookList()) {
            syncMessagingEventHook.beforeSend(settings, requestMessage);
        }

        SyncMessage responseMessage = null;
        try {
            if (settings.canUseMessageSenderClient()) {
                responseMessage = sendSyncWithMessageSenderClient(settings, requestMessage);
            } else {
                responseMessage = sendSyncWithProvider(settings, requestMessage);
            }
        } catch (RuntimeException e) {
            //送受信中にエラーが発生した場合は、SyncMessagingEventHookのエラー時処理を呼び出す。
            if (syncMessagingEventHookReverseList.size() > 0) {
                responseMessage = new SyncMessage(requestMessage.getRequestId());
                for (int i = 0; i < syncMessagingEventHookReverseList.size(); i++) {
                    boolean hasNext = false;
                    if (i < syncMessagingEventHookReverseList.size() - 1) {
                        hasNext = true;
                    }
                    SyncMessagingEventHook syncMessagingEventHook = syncMessagingEventHookReverseList.get(i);
                    if (!syncMessagingEventHook.onError(e, hasNext, settings, requestMessage, responseMessage)) {
                        //処理を打ち切る、。
                        throw e;
                    }
                }
                //SyncMessagingEventHookがレスポンスに値を設定しているはずなのなので、そのレスポンスを返す。
                return responseMessage;
            }
            //SyncMessagingEventHookが登録されていない場合は、キャッチした例外をそのまま送出する。
            throw e;
        }

        //送信後処理
        for (SyncMessagingEventHook syncMessagingEventHook : syncMessagingEventHookReverseList) {
            syncMessagingEventHook.afterSend(settings, requestMessage, responseMessage);
        }
        return responseMessage;
    }


    /**
     * MessageClientを使用した通信を行う。
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @return 応答電文
     */
    private static SyncMessage sendSyncWithMessageSenderClient(MessageSenderSettings settings, SyncMessage requestMessage) {
        MessageSenderClient messagingClient = settings.getMessageSenderClient();
        return messagingClient.sendSync(settings, requestMessage);
    }

    /**
     * キューを用いた通信を行う。
     * @param settings {@link MessageSender}の設定情報
     * @param requestMessage 要求電文
     * @return 応答電文
     */
    private static SyncMessage sendSyncWithProvider(MessageSenderSettings settings, SyncMessage requestMessage) {
        SyncMessageConvertor messageConvertor = getSyncMessageConvertor(settings);
        MessagingContext context = settings.getMessagingProvider().createContext();

        SendingMessage timeoutSendingMessage = null;
        SendingMessage sendingMessage;
        ReceivedMessage receivedMessage = null;
        try {
            int retryCount = 0;
            do {

                if (timeoutSendingMessage == null) { // 初回送信
                    sendingMessage = messageConvertor.convertOnSendSync(settings, requestMessage);
                } else { // 再送
                    sendingMessage = messageConvertor.convertOnRetry(settings, requestMessage, timeoutSendingMessage, retryCount);
                }
    
                receivedMessage = context.sendSync(sendingMessage, settings.getTimeout());
                if (receivedMessage != null) { // 応答電文あり
                    break;
                }
    
                // 応答電文なし(=タイムアウト)
                timeoutSendingMessage = sendingMessage;
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.logTrace(String.format("timeout occurred while synchronous sending message. "
                                                + "requestId = [%s], messageId = [%s], retryCount = [%s]",
                                                  requestMessage.getRequestId(),
                                                  timeoutSendingMessage.getMessageId(), retryCount));
                }
                retryCount++;
            } while (retryCount <= settings.getRetryCount());
        } finally {
            FileUtil.closeQuietly(context);
        }

        if (receivedMessage == null) { // タイムアウトにより送信失敗
            throw new MessageSendSyncTimeoutException(
                String.format("caused by timeout, failed to send message. "
                            + "requestId = [%s], retryCount = [%s]",
                              requestMessage.getRequestId(), settings.getRetryCount()),
                settings.getRetryCount());
        }

        // 応答電文の作成
        return messageConvertor.convertOnReceiveSync(settings, requestMessage, sendingMessage, receivedMessage);
    }

    /**
     * SyncMessageConvertorを取得する。
     * <pre>
     * 設定情報で指定されたSyncMessageConvertorを返す。
     * 設定情報でSyncMessageConvertorの指定がない場合は、
     * フレームワークが提供するSyncMessageConvertorをそのまま使用する。
     * 
     * SyncMessageConvertorの処理内容を変更したい場合は、
     * SyncMessageConvertorを継承したクラスをリポジトリに登録し、
     * 設定情報に指定する。
     * </pre>
     * @param settings 送信電文の設定情報
     * @return SyncMessageConvertor
     */
    private static SyncMessageConvertor getSyncMessageConvertor(MessageSenderSettings settings) {
        SyncMessageConvertor messageConvertor = settings.getMessageConvertor();
        return messageConvertor == null ? DEFAULT_MESSAGE_CONVERTOR : messageConvertor;
    }
}
