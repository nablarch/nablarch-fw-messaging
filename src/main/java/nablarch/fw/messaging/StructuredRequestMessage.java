package nablarch.fw.messaging;

import nablarch.core.dataformat.DataRecordFormatter;

/**
 * 構造化されたデータを取り扱うために、返信用オブジェクトとして{@link StructuredResponseMessage}を返却する要求メッセージ。
 * 
 * @author TIS
 */
public class StructuredRequestMessage extends RequestMessage {

    // --------------------------------------------------------- constructor
    /**
     * コンストラクタ
     * @param header フレームワーク制御ヘッダ
     * @param message 受信電文オブジェクト
     */
    public StructuredRequestMessage(FwHeader header, ReceivedMessage message) {
        super(header, message);
    }
    
    /**
     * {@inheritDoc}<br/>
     * この実装では{@link StructuredResponseMessage}を返却する。
     */
    protected ResponseMessage createResponseMessage() {
        return new StructuredResponseMessage(this);
    }

    /**
     * {@inheritDoc}<br/>
     * この実装ではフォーマッタの初期化処理を行う。
     */
    @Override
    public StructuredRequestMessage setFormatter(DataRecordFormatter formatter) {
        if (formatter != null) {
            formatter.initialize();
        }
        super.setFormatter(formatter);
        return this;
    }


}
