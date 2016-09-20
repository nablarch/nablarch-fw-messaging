package nablarch.fw.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.util.MapUtil;
import nablarch.core.util.annotation.Published;

/**
 * 対外システムに対する仕向け要求電文を表すクラス。
 * 
 * @author Iwauo Tajima
 */
public class SendingMessage extends InterSystemMessage<SendingMessage> {
        
    // -------------------------------------------------------- structure
    /** メッセージボディ部分のバイナリ表現 */
    private final ByteArrayOutputStream bodyStream;
    
    // --------------------------------------------------------- constructors
    /**
     * {@code SendingMessage}を生成する。
     */
    @Published(tag = "architect")
    public SendingMessage() {
        super();
        bodyStream = new ByteArrayOutputStream(4096);
    }
    
    /**
     * 元となる{@code SendingMessage}からインスタンスを生成する。
     *
     * @param original 元電文オブジェクト
     */
    public SendingMessage(SendingMessage original) {
        super(original);
        bodyStream = new ByteArrayOutputStream(4096);
    }
    
    // --------------------------------------------------------- accessors
    /**
     * 電文のデータ部に、指定したレコードを追加する。
     * <p/>
     * 出力時に使用するデータレイアウト（レコードタイプ）は、{@code record}の内容から自動的に判別される。
     * 
     * @param  record レコード内容
     * @return このオブジェクト自体
     * @throws InvalidDataFormatException
     *          レコードの内容がフォーマット定義に違反している場合
     */
    @Published
    public SendingMessage addRecord(Map<String, ?> record)
    throws InvalidDataFormatException {
        return addRecord(null, record);
    }
    
    /**
     * 電文のデータ部に、出力時に使用するデータレイアウト（レコードタイプ）を指定してレコードを追加する。
     * <p/>
     * {@code recordType}に{@code null}を渡した場合、{@link #addRecord(Map)}と同様の処理を行う。
     * 
     * @param recordType レコードタイプ
     * @param record     レコード内容
     * @return このオブジェクト自体
     * @throws InvalidDataFormatException
     *          レコードの内容がフォーマット定義に違反している場合
     */
    @Published
    public SendingMessage addRecord(String recordType, Map<String, ?> record) 
    throws InvalidDataFormatException {
        DataRecord typedRecord = new DataRecord().setRecordType(recordType);
        typedRecord.putAll(record);
        DataRecordFormatter formatter = getFormatter()
                                       .setOutputStream(bodyStream)
                                       .initialize();
        try {
            if (recordType == null) {
                formatter.writeRecord(typedRecord);
            } else {
                formatter.writeRecord(recordType, typedRecord);
            }
            getRecords().add(typedRecord);
            return this;
            
        } catch (IOException e) {
            throw new RuntimeException(e);  // can not occur.
        }
    }
    
    /**
     * 電文のデータ部に指定したレコードを追加する。
     * <p/>
     * 出力時に使用するデータレイアウト（レコードタイプ）は、渡されたデータの内容から自動的に判別される。
     * 
     * @param  recordObj レコード内容を表現したオブジェクト
     * @return このオブジェクト自体
     * @throws InvalidDataFormatException
     *          レコードの内容がフォーマット定義に違反している場合
     */
    @Published
    public SendingMessage addRecord(Object recordObj)
    throws InvalidDataFormatException {
        return addRecord(null, recordObj);
    }
    
    /**
     * 電文のデータ部に、出力時に使用するデータレイアウト（レコードタイプ）を指定してレコードを追加する。
     * <p/>
     * {@code recordType}に{@code null}を渡した場合、{@link #addRecord(Object)}と同様の処理を行う。
     * 
     * @param recordType レコードタイプ
     * @param recordObj  レコード内容を表現したオブジェクト
     * @return このオブジェクト自体
     * @throws InvalidDataFormatException
     *          レコードの内容がフォーマット定義に違反している場合
     */
    @Published
    public SendingMessage addRecord(String recordType, Object recordObj) 
    throws InvalidDataFormatException {
        Map<String, ?> record = MapUtil.createFlatMap(recordObj);
        return addRecord(recordType, record);
    }
    
    /** {@inheritDoc}
     */
    @Override
    @Published(tag = "architect")
    public byte[] getBodyBytes() {
        return bodyStream.toByteArray();
    }
    
    /**
     * 電文のボディ部の出力ストリームを返す。
     * @return 電文のボディ部の出力ストリーム
     */
    @Published(tag = "architect")
    public ByteArrayOutputStream getBodyStream() {
        return this.bodyStream;
    }
    
    // ------------------------------------------------- well-known header
    /**
     * 電文の有効期間をmsec単位で返す。
     * @return 電文の有効期間(msec)
     */
    public long getTimeToLive() {
        Long value = getHeader(HeaderName.TIME_TO_LIVE);
        return (value == null) ? 0
             : (value <= 0)    ? 0
             : value;
    }
    
    /**
     * 電文の有効期間をmsec単位で設定する。
     * <p/>
     * 0以下の数値を指定した場合は無期限となる。
     * 本メソッドで有効期間を指定しなかった場合は、プロバイダ側で定めたデフォルト値が設定される。
     * 
     * @param timeToLive この電文の有効期間(msec)
     * @return このオブジェクト自体
     */
    @Published(tag = "architect")
    public SendingMessage setTimeToLive(long timeToLive) {
        timeToLive = (timeToLive < 0) ? 0
                                      : timeToLive;
        setHeader(HeaderName.TIME_TO_LIVE, timeToLive);
        return this;
    }
}
