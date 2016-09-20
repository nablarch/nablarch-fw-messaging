package nablarch.fw.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.util.MapUtil;
import nablarch.core.util.annotation.Published;

/**
 * 構造化されたデータを取り扱うために、addRecord時にデータをすぐに書き込まず、メモリ上にキャッシュする応答メッセージ。
 * 
 * @author TIS
 */
public class StructuredResponseMessage extends ResponseMessage {

    // ----------------------------------------------------- Constructor
    /**
     * 要求電文に対する応答電文を作成する。
     * RequestMessage#reply() から呼ばれることを想定している。
     * @param message 要求電文オブジェクト
     */
    @Published(tag = "architect")
    public StructuredResponseMessage(RequestMessage message) {
        super(message);
    }

    /**
     * {@inheritDoc}<br/>
     * この実装ではフォーマッタの初期化処理を行う。
     */
    @Override
    public StructuredResponseMessage setFormatter(DataRecordFormatter formatter) {
        if (formatter != null) {
            formatter.initialize();
        }
        super.setFormatter(formatter);
        return this;
    }

    /**{@inheritDoc}
     * この実装ではこの時点でキャッシュされたメッセージをフォーマッタに書き込み、バイト列を生成する。
     */
    @Override
    public byte[] getBodyBytes() {
        // FW制御ヘッダの書き込みを目的として、一度親クラスのバイト列取得処理を呼び出す
        super.getBodyBytes();
        
        // ストリームをリセット
        ByteArrayOutputStream bodyStream = super.getBodyStream();
        bodyStream.reset();
        
        // 保持しているデータを変換しながらストリームに書き込む
        DataRecordFormatter formatter = getFormatter()
                .setOutputStream(bodyStream)
                .initialize();
        try {
            for (DataRecord record : getRecords()) {
                String recordType = record.getRecordType();
                if (recordType == null) {
                    formatter.writeRecord(record);
                } else {
                    formatter.writeRecord(recordType, record);
                }
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);  // can not occur.
        }
        
        return super.getBodyBytes();
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(Map<String, ?> record) {
        addRecord(null, record);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(String recordType, Map<String, ?> record) {
        DataRecord dr = new DataRecord();
        dr.setRecordType(recordType);
        dr.putAll(record);
        getRecords().add(dr);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(Object recordObj) {
        addRecord(null, recordObj);
        return this;
    }
    
    /**{@inheritDoc} */
    @Override
    @Published(tag = "architect")
    public ResponseMessage addRecord(String recordType, Object recordObj) {
        addRecord(recordType, MapUtil.createFlatMap(recordObj));
        return this;
    }
}
