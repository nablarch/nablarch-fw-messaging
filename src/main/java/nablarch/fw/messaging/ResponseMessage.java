package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;
import nablarch.fw.Result;

import java.util.Map;

/** 
 * 本システムに対する要求電文への応答電文を表すクラス。
 * 
 * 本クラスでは、RequestMessageと同様、応答電文の内容をフレームワークヘッダ部と
 * 業務データ部に分離して管理しており、業務ロジックからは業務データ部しか操作できない
 * ようになっている。
 * 
 * メッセージボディを直列化する際には、フレームワークヘッダ部と業務データ部をそれぞれ
 * 直列化して連結したものを返す。
 * 
 * @author Iwauo Tajima
 */
public class ResponseMessage extends SendingMessage implements Result {
    // ---------------------------------------------------- Structure
    /** フレームワーク制御ヘッダ */
    private final FwHeader fwHeader;
    
    /** フレームワーク制御ヘッダ定義     */
    private FwHeaderDefinition fwHeaderDefinition;
    
    /** 業務処理の結果 */
    private Result result = new Result.Success();
    
    // ----------------------------------------------------- Constructor
    /**
     * 受信電文に対する応答電文を作成する。
     * @param fwHeader 応答電文に付加するフレームワーク制御ヘッダ
     * @param message  受信電文
     */
    public ResponseMessage(FwHeader fwHeader, ReceivedMessage message) {
        // プロトコルヘッダを設定
        setDestination(message.getReplyTo());
        setCorrelationId(message.getMessageId());
        // 受信電文のフォーマッターを流用する。
        setFormatter(message.getFormatter());
        this.fwHeader = fwHeader;
        setResult(new Result.Success());
    }
    
    /**
     * 要求電文に対する応答電文を作成する。
     * RequestMessage#reply() から呼ばれることを想定している。
     * @param message 要求電文オブジェクト
     */
    @Published(tag = "architect")
    public ResponseMessage(RequestMessage message) {
        // プロトコルヘッダを設定
        setDestination(message.getReplyTo());
        setCorrelationId(message.getMessageId());
        // 受信電文のフォーマッターを流用する。
        setFormatter(message.getFormatter());
        this.fwHeader = message.getFwHeader();
        setResult(new Result.Success());
    }
   
    /**{@inheritDoc}
     * この実装では、フレームワーク制御ヘッダ部と業務データ部をそれぞれ直列化し、
     * 連結したものを返す。
     * ただし、フレームワーク制御ヘッダを直列化する際、フレームワーク制御ヘッダ定義
     * を使用する。これは、通常{@link nablarch.fw.messaging.handler.MessageReplyHandler}によって設定される。
     */
    @Override
    public byte[] getBodyBytes() {
        if (fwHeaderDefinition == null) {
            throw new IllegalStateException(
                "Could not serialize fwHeader "
              + "because fwHeaderDefinition was not assigned to this response."
            );
        }
        if (!wroteHeader) {
            wroteHeader = true;
            fwHeaderDefinition.writeFwHeaderTo(this, fwHeader);
        }
        return super.getBodyBytes();
    }
    /** フレームワーク制御ヘッダが電文に反映されたか否か */
    private boolean wroteHeader = false;
    
    
    /**
     * 実行時例外を送出し、現在の業務トランザクションをロールバックさせ、
     * この電文の内容をエラー応答として送信する。
     * @param e 起因となる例外
     */
    public void throwAsError(Throwable e) {
        throw new ErrorResponseMessage(this, e);
    }
    
    /**
     * 実行時例外を送出し、現在の業務トランザクションをロールバックさせ、
     * この電文の内容をエラー応答として送信する。
     */
    public void throwAsError() {
        throw new ErrorResponseMessage(this);
    }

    // ------------------------------------------------------- Result I/F
    /** {@inheritDoc} */
    public int getStatusCode() {
        return result.getStatusCode();
    }

    /** {@inheritDoc} */
    public String getMessage() {
        return result.getMessage();
    }

    /** {@inheritDoc} */
    public boolean isSuccess() {
        return result.isSuccess();
    }
    
    
    // --------------------------------------------------------- Accessors
    /**
     * 業務処理の結果を設定する。
     * @param result 業務処理結果
     * @return このオブジェクト自体
     */
    public ResponseMessage setResult(Result result) {
        this.result = result;
        return this;
    }
    
    /**
     * フレームワーク制御ヘッダを取得する。
     * @return フレームワーク制御ヘッダ
     */
    public FwHeader getFwHeader() {
        return fwHeader;
    }
    
    /**
     * フレームワークヘッダ定義を設定する。
     * @param def フレームワーク制御ヘッダ
     * @return このオブジェクト自体
     */
    @Published(tag = "architect")
    public ResponseMessage setFwHeaderDefinition(FwHeaderDefinition def) {
        fwHeaderDefinition = def;
        return this;
    }
    
    /**
     * フレームワーク制御ヘッダの処理結果コードの値を設定する。
     * @param statusCode 処理結果コード
     * @return このオブジェクト自体
     */
    @Published(tag = "architect")
    public ResponseMessage setStatusCodeHeader(String statusCode) {
        fwHeader.setStatusCode(statusCode);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(Map<String, ?> record) {
        super.addRecord(record);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(String recordType, Map<String, ?> record) {
        super.addRecord(recordType, record);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(Object recordObj) {
        super.addRecord(recordObj);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(String recordType, Object recordObj) {
        super.addRecord(recordType, recordObj);
        return this;
    }
}
