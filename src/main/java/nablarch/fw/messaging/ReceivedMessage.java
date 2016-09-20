package nablarch.fw.messaging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.util.annotation.Published;

/**
 * 対外システムから受信した電文を表すクラス。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class ReceivedMessage extends InterSystemMessage<ReceivedMessage> {
    
    // -------------------------------------------------------- structure
    /** メッセージデータ部のストリーム */
    private final ByteArrayInputStream bodyStream;
    
    /**  メッセージデータ部のバイナリ列 */
    private final byte[] bodyBytes;
    
    // -------------------------------------------------------- constructors
    /**
     * コンストラクタ。
     * @param bodyBytes メッセージデータ部のバイナリ列
     */
    public ReceivedMessage(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
        bodyStream = new ByteArrayInputStream(bodyBytes);
    }
    
    /**
     * コピーコンストラクタ。
     * @param orgMessage コピー元電文
     */
    public ReceivedMessage(ReceivedMessage orgMessage) {
        super(orgMessage);
        bodyStream = orgMessage.bodyStream;
        bodyBytes  = orgMessage.bodyBytes;  
    }
    
    // ----------------------------------------------- InterSystemMessage I/F
    /** {@inheritDoc}
     * この実装では、パース前の送信電文の内容をそのまま返す。
     */
    @Override
    public byte[] getBodyBytes() {
        return bodyBytes;
    }
    
    // -------------------------------------------------------- published api    
    /**
     * データフォーマット定義に従い、1レコードをメッセージボディから読み込んで返す。 
     * 読み込まれるレコードの種別はデータフォーマット定義に従って自動的に決定される。
     * 読み込むレコードが存在しない場合はnullを返す。
     * @return 読み込んだデータレコード
     */
    @Published(tag = "architect")
    public DataRecord readRecord() {
        DataRecordFormatter formatter = getFormatter();
        if (formatter == null) {
            throw new IllegalStateException(
               "could not read record because any formatter was not set."
            );
        }
        try {
            DataRecord result = formatter
                               .setInputStream(bodyStream)
                               .initialize()
                               .readRecord();
            if (result != null) {
                getRecords().add(result);
            }
            return result;
            
        } catch (IOException e) {
            throw new MessagingException(e); // can not happen.
        }
    }
    
    /**
     * データフォーマット定義に従い、データ部の全レコードを読み出す。
     * 読み込まれるレコードの種別はデータフォーマット定義に従って自動的に決定される。
     * 読み込むレコードが存在しない場合は空のリストを返す。
     * @return このオブジェクト自体
     */
    @Published(tag = "architect")
    public List<DataRecord> readRecords() {
        DataRecordFormatter formatter = getFormatter();
        if (formatter == null) {
            throw new IllegalStateException(
               "could not read record because any formatter was not set."
            );
        }
        try {
            List<DataRecord> result = new ArrayList<DataRecord>();
            DataRecord currRecord = null;
            while (true) {
                currRecord = formatter
                            .setInputStream(bodyStream)
                            .initialize()
                            .readRecord();
                if (currRecord == null) {
                    break;
                }
                result.add(currRecord);
            }
            getRecords().addAll(result);
            return result;
            
        } catch (IOException e) {
            throw new MessagingException(e); // can not happen.
        }
    }
}
