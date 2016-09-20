package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;
import nablarch.fw.Result;

/**
 * メッセージデータリーダによる受信電文の読み込み処理の最中にエラーが発生した場合に
 * 送出される例外。
 * <p/>
 * 受信電文のGETには成功したが、フレームワークヘッダ領域の読み込みに失敗した場合に
 * 送出される。
 * <p/>
 * この例外が送出された場合、業務処理へのディスパッチは発生しない。
 * 
 * @see nablarch.fw.messaging.reader.FwHeaderReader
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class MessageReadError extends Result.Error {

    // ----------------------------------------- structure
    /** 受信メッセージ */
    private final ReceivedMessage message;
     
    /**
     * コンストラクタ。
     * @param message 受信電文オブジェクト
     * @param e       元例外
     */
    public MessageReadError(ReceivedMessage message, Throwable e) {
        super("could not read 'FWHeader' region in the received message body "
          + "so could not determine the Action class "
          + "to which delegates process of this message.", e
        );
        this.message = message;
    }
        
    /**
     * 受信電文オブジェクトを取得する。
     * @return 受信電文オブジェクト
     */
    public ReceivedMessage getReceivedMessage() {
        return message;
    }

    // ----------------------------------------------- Result I/F
    @Override
    public int getStatusCode() {
        return 500;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }
}
