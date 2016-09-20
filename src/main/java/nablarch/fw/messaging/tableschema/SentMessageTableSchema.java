package nablarch.fw.messaging.tableschema;

/**
 * 再送電文管理テーブルスキーマ定義クラス。
 * <p/>
 * 以下のようなテーブル構造を想定している。
 * <pre>
 * =====================================
 * メッセージID   VARCHAR PK
 * リクエストID   VARCHAR PK
 * 応答宛先キュー VARCHAR
 * 処理結果コード VARCHAR
 * 電文データ部   BLOB
 * =====================================
 * </pre>
 * 以下にデフォルト設定でのテーブル名、カラム名に沿ったテーブルスキーマの
 * サンプルを示す。
 * 
 * <pre>
 * CREATE TABLE SENT_MESSAGE (
 *     MESSAGE_ID  VARCHAR(64)
 *   , REQUEST_ID  VARCHAR(64)
 *   , REPLY_QUEUE VARCHAR(64)
 *   , STATUS_CODE CHAR(4)
 *   , BODY_DATA   BLOB
 *   , CONSTRAINT pk_SENT_MESSAGE
 *       PRIMARY KEY(MESSAGE_ID, REQUEST_ID)
 * );
 * </pre>
 * 
 * @author Iwauo Tajima
 */
public class SentMessageTableSchema {
    // ------------------------------------------------------- Structure
    /** 再送電文管理テーブルのテーブル名 */
    private String tableName = "SENT_MESSAGE";
    
    /** メッセージIDを保持するカラムの名称 */
    private String messageIdColumn = "MESSAGE_ID";
    
    /** 要求電文のリクエストIDを保持するカラムの名称 */
    private String requestIdColumn = "REQUEST_ID";
    
    /** 応答電文の宛先キューの論理名を保持するカラムの名称 */
    private String replyQueueColumn = "REPLY_QUEUE";
    
    /** 処理結果のステータスコードを保持するカラムの名称 */
    private String statusCodeColumn = "STATUS_CODE";
    
    /** メッセージボディデータの内容をバイト配列で保持するカラムの名称 */
    private String bodyDataColumn = "BODY_DATA";
    
    // -------------------------------------------------------- Accessors
    /**
     * 再送電文管理テーブルの名称を設定する。
     * @param tableName テーブル名
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
    
    /**
     * 再送電文管理テーブルの名称を返す。
     * @return テーブルの名称
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * 応答電文の宛先キューの論理名を保持するカラムの名称を設定する。
     * (デフォルトは"REPLY_QUEUE")
     * @param columnName カラムの名称
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema
    setReplyQueueColumnName(String columnName) {
        replyQueueColumn = columnName;
        return this;
    }
    
    /**
     * 応答電文の宛先キューの論理名を保持するカラムの名称を返す。
     * @return カラムの名称
     */
    public String getReplyQueueColumnName() {
        return replyQueueColumn;
    }

    /**
     * メッセージIDを保持するカラムの名称を設定する。
     * (デフォルトは"MESSAGE_ID")
     * @param columnName カラムの名称
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema
    setMessageIdColumnName(String columnName) {
        messageIdColumn = columnName;
        return this;
    }
    
    /**
     * メッセージIDを保持するカラムの名称をを返す。
     * @return カラムの名称
     */
    public String getMessageIdColumnName() {
        return messageIdColumn;
    }
    
    /**
     * メッセージボディデータの内容をバイト配列で保持するカラムの名称を設定する。
     * (デフォルトは"BODY_DATA")
     * @param columnName カラムの名称
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema
    setBodyDataColumnName(String columnName) {
        bodyDataColumn = columnName;
        return this;
    }
    
    /**
     * メッセージボディデータの内容をバイト配列で保持するカラムの名称をを返す。
     * @return カラムの名称
     */
    public String getBodyDataColumnName() {
        return bodyDataColumn;
    }

    /**
     * 要求電文のリクエストIDを保持するカラムの名称を設定する。
     * (デフォルトは"REQUEST_ID")
     * @param columnName カラムの名称
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema
    setRequestIdColumnName(String columnName) {
        requestIdColumn = columnName;
        return this;
    }
    
    /**
     * 要求電文のリクエストIDを保持するカラムの名称をを返す。
     * @return カラムの名称
     */
    public String getRequestIdColumnName() {
        return requestIdColumn;
    }
    
    /**
     * 要求電文のユーザIDを保持するカラムの名称を設定する。
     * (デフォルトは"USER_ID")
     * @param columnName カラムの名称
     * @return このオブジェクト自体
     */
    public SentMessageTableSchema
    setStatusCodeColumnName(String columnName) {
        statusCodeColumn = columnName;
        return this;
    }
    
    /**
     * 要求電文のユーザIDを保持するカラムの名称をを返す。
     * @return カラムの名称
     */
    public String getStatusCodeColumnName() {
        return statusCodeColumn;
    }
}
