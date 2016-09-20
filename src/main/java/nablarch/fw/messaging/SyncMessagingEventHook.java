package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;

/**
 * メッセージ送信の処理前後に処理を行うためのインターフェイス。<br>
 * 
 * <p>
 * {@link MessageSender#sendSync(SyncMessage)}で電文を送受信する際、処理を差し込む際に使用する。<br>
 * 本インターフェースを実装したクラスに、差し込みたい処理を記述する。
 * 
 * どの電文を送受信した際に処理を差し込むかの設定は、メッセージングプロバイダ定義ファイルと、コンポーネント定義ファイルを用いて行う。
 * </p>
 * 
 * @author TIS
 * @see MessageSender
 */
@Published(tag = "architect")
public interface SyncMessagingEventHook {
    /**
     * メッセージ送信前に呼ばれる処理。
     * 
     * @param settings メッセージ送信設定
     * @param requestMessage 送信対象メッセージ
     */
    void beforeSend(MessageSenderSettings settings, SyncMessage requestMessage);

    /**
     * メッセージ送信後、レスポンスを受け取った後に呼ばれる処理。
     * 
     * @param settings メッセージ送信設定
     * @param requestMessage リクエストメッセージ
     * @param responseMessage レスポンスメッセージ
     */
    void afterSend(MessageSenderSettings settings, SyncMessage requestMessage,
            SyncMessage responseMessage);

    /**
     * メッセージ送信中のエラー発生時に呼ばれる処理。
     * 
     * @param e 発生した例外
     * @param hasNext 次に呼び出される{@link SyncMessagingEventHook}が存在する場合にtrue
     * @param settings メッセージ送信設定
     * @param requestMessage リクエストメッセージ
     * @param responseMessage レスポンスメッセージとして使用するオブジェクト。本オブジェクトは最終的に{@link MessageSender#sendSync(SyncMessage)}の戻り値として返却される。
     * @return trueの場合は処理継続。次の{@link SyncMessagingEventHook#onError(RuntimeException, MessageSenderSettings, SyncMessage)}を呼ぶ。<br />
     * 次がない場合は、{@link MessageSender#sendSync(SyncMessage)}}の戻り値として、引数responseMessageの値を返す。<br />
     * falseの場合は、本メソッド終了後に引数eをthrowする
     */
    boolean onError(RuntimeException e,
            boolean hasNext, MessageSenderSettings settings, SyncMessage requestMessage, SyncMessage responseMessage);

}
