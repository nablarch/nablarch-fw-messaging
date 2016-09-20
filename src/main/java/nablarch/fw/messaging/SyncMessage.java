package nablarch.fw.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import nablarch.core.util.MapUtil;
import nablarch.core.util.annotation.Published;

/**
 * 電文(同期送信、同期応答)を保持するクラス。
 * @author Kiyohito Itoh
 */
@Published
public class SyncMessage {

    /** 要求電文のリクエストID */
    private final String requestId;

    /** ヘッダレコード */
    private final Map<String, Object> headerRecord;

    /** データレコードリスト */
    private final List<Map<String, Object>> dataRecords;

    /**
     * {@code SyncMessage}のインスタンスを生成する。
     * @param requestId 要求電文のリクエストID
     */
    public SyncMessage(String requestId) {
        this.requestId = requestId;
        this.headerRecord = new TreeMap<String, Object>();
        this.dataRecords = new ArrayList<Map<String, Object>>();
    }

    /**
     * 要求電文のリクエストIDを取得する。
     * @return 要求電文のリクエストID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * ヘッダレコードを取得する。
     * @return ヘッダレコード
     */
    public Map<String, Object> getHeaderRecord() {
        return headerRecord;
    }

    /**
     * ヘッダレコードを設定する。
     * @param headerRecord ヘッダレコード
     * @return このオブジェクト自体
     */
    public SyncMessage setHeaderRecord(Map<String, Object> headerRecord) {
        this.headerRecord.putAll(headerRecord);
        return this;
    }

    /**
     * データレコード(1件目)を取得する。
     * @return データレコード(1件目)。データレコードが追加されていない場合{@code null}を返す
     */
    public Map<String, Object> getDataRecord() {
        return dataRecords.isEmpty() ? null : dataRecords.get(0);
    }

    /**
     * データレコードを全件取得する。
     * @return データレコードリスト。データレコードが追加されていない場合は空のListを返す。
     */
    public List<Map<String, Object>> getDataRecords() {
        return dataRecords;
    }

    /**
     * データレコードを追加する。
     * @param dataRecord データレコード
     * @return このオブジェクト自体
     */
    public SyncMessage addDataRecord(Map<String, Object> dataRecord) {
        dataRecords.add(dataRecord);
        return this;
    }

    /**
     * データレコードを追加する。
     * @param form データレコードを表すオブジェクト
     * @return このオブジェクト自体
     */
    public SyncMessage addDataRecord(Object form) {
        Map<String, Object> data = MapUtil.createFlatMap(form);
        dataRecords.add(data);
        return this;
    }

}
