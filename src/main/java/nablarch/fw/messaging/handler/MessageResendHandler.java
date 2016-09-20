package nablarch.fw.messaging.handler;

import java.util.HashMap;
import java.util.Map;

import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.ResultSetIterator;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.db.statement.exception.DuplicateStatementException;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.StandardFwHeaderDefinition;
import nablarch.fw.messaging.tableschema.SentMessageTableSchema;

/**
 * 応答電文の再送処理制御を行うハンドラ。
 * <p/>
 * 一般に、外部システムに対する要求電文がタイムアウトした場合、その補償電文として
 * 再送要求電文、もしくは、取り消し電文を送信することになる。
 * 本ハンドラでは、このうち再送要求電文に対する制御をフレームワークレベルで実装している。
 * <p/>
 * ※ 本機能では、初回電文/再送要求電文の判定をフレームワーク制御ヘッダ中の
 *    再送要求フラグの値を用いて行う。
 *    このため、フレームワーク制御ヘッダが定義されていない電文に対しては
 *    再送制御を行うことができない。
 * 
 * <div><b>再送制御</b></div>
 * <hr/>
 * 本システムに対する処理要求メッセージ(初回電文)が、転送経路上のネットワークエラーや
 * 遅延により外部システム側でタイムアウトし、その補償電文として再送要求電文が送信されたとする。
 * この際、再送要求電文を受信した時点でのシステムの状態は、以下の4つに分類できる。
 * 
 * <pre>
 * 1. ネットワークエラーもしくは遅延により、初回電文が未受信。
 * 2. 初回電文を処理中。
 * 3. 初回電文に対する業務処理は正常終了(トランザクションをコミット)したが、
 *    ネットワークエラーもしくは遅延により応答電文が未達。
 * 4. 初回電文に対する業務処理が異常終了(トランザクションをロールバック)し、
 *    ネットワークエラーもしくは遅延によりエラー応答電文が未達。
 * </pre>
 * 
 * それぞれのケースについて、本システムの挙動は以下のようになる。
 * 
 * <pre>
 * 1. 初回電文が未受信
 *    再送要求電文を初回電文として処理する。
 *    この場合、再送要求電文を処理中に、遅延していた初回電文を並行実行する可能性があるが
 *    先にコミットされたトランザクションのみ正常終了し、それ以外はロールバックする。
 *    また、ロールバックされた要求の応答として、先に正常終了した電文の応答を再送する。
 *     
 * 2. 本システムで初回電文を処理中
 *    再送要求電文も初回電文として処理し、先に完了したトランザクションをコミットし、
 *    もう一方をロールバックする。
 *    (1.と同じ扱い。)
 *    
 * 3. 業務処理は正常終了したが応答電文が未達
 *    再送用電文テーブルから当該メッセージIDの電文データを取得し、応答電文として送信する。
 *    業務処理は実行されない。
 *    
 * 4. 業務処理が正常終了したがエラー応答電文が未達
 *    再送要求電文を初回電文として処理する。
 * </pre>
 * 
 * 本機能は大きく以下の2つの機能によって構成されている。
 * 
 * <pre>
 * 再送応答機能:
 *    接続先システムから再送要求電文が送信された場合に、
 *    後述の送信済電文保存機能が保持する過去の応答電文の内容を再送信する機能。
 *    
 * 送信済電文保存機能:
 *    ローカルキューに対するPUT命令が完了したメッセージの内容をデータベース上の
 *    送信済電文テーブルに保存する機能。
 *    送信済み電文の内容は業務トランザクションとともにコミットされる。
 *    従って、業務処理がエラー終了した場合には再送用電文は残らない。
 *   
 *    再送電文管理テーブルのスキーマ構造については {@link SentMessageTableSchema}
 *    を参照すること。
 * </pre>
 * 
 * @see SentMessageTableSchema
 * @see FwHeader#isResendingRequest()
 * @author Iwauo Tajima
 */
public class MessageResendHandler
implements Handler<RequestMessage, ResponseMessage> {
    // ------------------------------------------------------- Structure    
    /** 再送電文管理テーブルスキーマ定義 */
    private SentMessageTableSchema schema = null;
    
    /** 再送対象の電文を検索する際に使用するSQLクエリー */
    private String findAlreadySentMessageQuery = null;
    
    /** 再送電文管理テーブルに送信電文を新規登録するSQL文 */
    private String insertNewSentMessageDml = null;
    
    /** 応答電文中のフレームワークヘッダ定義 */
    private FwHeaderDefinition fwHeaderDefinition = new StandardFwHeaderDefinition();

    // ------------------------------------------------------- Constructor
    /**
     * デフォルトコンストラクタ。
     */
    public MessageResendHandler() {
        schema = new SentMessageTableSchema();
    }
    
    /**
     * 本ハンドラで使用するSQL文を構築する。
     */
    public synchronized void initialize() {
        if (findAlreadySentMessageQuery != null) {
            return;
        }
        findAlreadySentMessageQuery = Builder.join(new String[] {
            "SELECT", schema.getReplyQueueColumnName(), "AS replyQueue",  ","
                    , schema.getMessageIdColumnName(),  "AS messageId" ,  ","
                    , schema.getBodyDataColumnName(),   "AS bodyData"  ,  ","
                    , schema.getRequestIdColumnName(),  "AS requestId" ,  ","
                    , schema.getStatusCodeColumnName(), "AS statusCode"
        ,   "FROM"  , schema.getTableName()
        ,   "WHERE" , schema.getMessageIdColumnName(), "= ?"
        ,   "AND"   , schema.getRequestIdColumnName(), "= ?"
        }, " ");
        
        if (insertNewSentMessageDml != null) {
            return;
        }
        insertNewSentMessageDml = Builder.join(new String[]{
            "INSERT INTO", schema.getTableName()
        ,   "("
        ,       schema.getMessageIdColumnName(),  ","
        ,       schema.getRequestIdColumnName(),  ","
        ,       schema.getReplyQueueColumnName(), ","
        ,       schema.getStatusCodeColumnName(), ","
        ,       schema.getBodyDataColumnName()
        ,   ") VALUES ( ?, ?, ?, ?, ? )"
        }, " ");
    }
    
    
    // ------------------------------------------------------- Handler I/F
    /** {@inheritDoc}
     * 本ハンドラの実装では、再送電文管理テーブルを確認し、当該の受信電文に対する
     * 応答電文が登録されているかどうかを確認する。
     * 応答電文が登録されていればそれを返却する。
     * 登録されていない場合は、要求電文を初回電文として処理し、その応答電文を返す。
     */
    public ResponseMessage handle(RequestMessage request, ExecutionContext context) {
        if (findAlreadySentMessageQuery == null) {
            initialize();
        }
        if (!request.getFwHeader().isResendingSupported()) {
            return context.handleNext(request);
        }
        ResponseMessage reply = getAlreadySentReply(request);
        if (reply != null) {
            reply.setCorrelationId(request.getMessageId());
            return reply;
        }
        reply = context.handleNext(request);

        try {
            // 業務処理が正常終了(業務トランザクションがコミット)した場合、
            // 応答電文オブジェクトを再送用に保存する。
            // これにより当該メッセージIDに対するwrite-lockを獲得することになり、
            // 同一電文に対する並行処理は先行する1つを除いてwaitする。
            // waitしたスレッドのその後の挙動は以下のいずれかの動作となる。
            // 1. ロックを獲得した先行スレッドがロールバックした場合。
            //    開放されたロックを獲得できれば、処理を再開する。
            //    獲得できなければ再びwaitする。
            // 2. ロックを獲得した先行スレッドが正常にコミットした場合。
            //    開放されたロックを獲得後、処理を再開するが、insert時に一意制約違反
            //    が発生し、下のcatchブロックに進む。
            saveReply(request, reply);
            return reply;
            
        } catch (DuplicateStatementException e) {
            // 並行処理中の先行電文によって処理が完了し、既に再送用電文が
            // 登録されていた場合はinsert時に一意制約エラーが発生し
            // このcatchブロックに突入する。
            // この場合、業務処理自体は別スレッドで正常終了しているので、
            // 再送電文を取得して返却する。
            ResponseMessage alreadySent = getAlreadySentReply(request);
            if (alreadySent != null) {
                alreadySent.setCorrelationId(request.getMessageId());
                alreadySent.throwAsError();
            }
            // PK以外の一意制約が定義されていなければ、ここにはこないはず。
            throw e;
        }
    }
    
    // --------------------------------------------------- DB Access
    /**
     * 応答電文を再送電文テーブルに格納する。
     * @param request  要求電文オブジェクト
     * @param response 応答電文オブジェクト
     */
    public void saveReply(RequestMessage request, ResponseMessage response) {
        // 初回電文が遅延もしくはロストした場合、再送要求が初回実行の対象となる。
        // この場合、初回電文のリクエストIDをキーとして保存しなければならない。
        // 初回電文のリクエストIDは再送電文の関連IDに一致するため、ここでは
        // その値を使用する。
        FwHeader fwHeader = request.getFwHeader();
        String messageId = fwHeader.isResendingRequest()
                         ? request.getCorrelationId()
                         : request.getMessageId();
        response.setFwHeaderDefinition(fwHeaderDefinition);

        Map<String, Object> record = new HashMap<String, Object>();
        record.put("messageId",  messageId);
        record.put("requestId",  fwHeader.getRequestId());
        record.put("replyQueue", response.getDestination());
        record.put("statusCode", response.getStatusCode());
        record.put("bodyData",   response.getBodyBytes());
        insertNewSentMessage(record);
    }
    
    /**
     * 再送電文管理テーブルに送信電文を新規登録する。
     * @param values 登録するレコード
     */
    public void insertNewSentMessage(Map<String, Object> values) {
        SqlPStatement stmt = DbConnectionContext
                            .getConnection()
                            .prepareStatement(insertNewSentMessageDml);
        stmt.setString(1, values.get("messageId").toString());
        stmt.setString(2, values.get("requestId").toString());
        stmt.setString(3, values.get("replyQueue").toString());
        stmt.setString(4, values.get("statusCode").toString());
        stmt.setBytes(5,  (byte[]) values.get("bodyData"));
        stmt.execute();
    }

    /**
     * 再送電文テーブルの内容を確認し、メッセージIDが一致する電文があれば
     * その内容をもとに応答電文を作成して返す
     * 該当する電文が存在しなければnullを返す。
     * 
     * @param request 要求電文オブジェクト
     * @return 再送用応答電文オブジェクト。
     *          メッセージIDが一致するものが存在しない場合はnull
     */
    public ResponseMessage getAlreadySentReply(RequestMessage request) {
        String correlationId = request.getCorrelationId();
        if (correlationId == null) {
            correlationId = request.getMessageId();
        }
        SqlRow record = findAlreadySentMessage(
                            correlationId, request.getRequestPath()
                        );
        return (record != null) ? new ResentResponse(record, request)
                                : null;
    }
    
    /**
     * 再送対象の電文レコードを検索し、当該の電文のレコードを返却する。
     * レコードが存在しなかった場合はnullを返却する。
     * 
     * @param messageId 初回電文のメッセージID (=再送要求電文の関連ID)
     * @param requestId 要求電文のリクエストID
     * @return 当該電文のレコード
     */
    public SqlRow
    findAlreadySentMessage(String messageId, String requestId) {
        SqlPStatement stmt = DbConnectionContext
                            .getConnection()
                            .prepareStatement(findAlreadySentMessageQuery);
        stmt.setString(1, messageId);
        stmt.setString(2, requestId);
        ResultSetIterator results = stmt.executeQuery();
        if (!results.next()) {
            return null;
        }
        return results.getRow();
    }
    
    // ------------------------------------------------------------ helper
    /**
     * 再送信応答電文
     */
    private static class ResentResponse extends ResponseMessage {
        
        /** 再送信応答電文のボディ部 */
        private byte[] body;
        
        /**
         * 再送管理テーブル上 のレコードからインスタンスを構成する。
         * @param record  レコード
         * @param request 要求電文オブジェクト
         */
        public ResentResponse(SqlRow record, RequestMessage request) {
            super(request);
            setDestination(record.getString("replyQueue"));
            this.record = record;
            getFwHeader().setStatusCode(record.getString("statusCode").trim());
            body = record.getBytes("bodyData");
        }
        /** 送信済み電文レコード */
        private final SqlRow record;
        @Override
        public byte[] getBodyBytes() {
            return body;
        }
    }

    // ------------------------------------------------ Accessors
    /**
     * 再送電文管理テーブルのスキーマ定義を設定する。
     * @param schema スキーマ定義
     * @return このオブジェクト自体
     */
    public MessageResendHandler
    setSentMessageTableSchema(SentMessageTableSchema schema) {
        this.schema = schema;
        return this;
    }

    /**
     * 応答電文中のフレームワーク制御ヘッダ定義を設定する。
     * @param def フレームワーク制御ヘッダ定義
     * @return このオブジェクト自体
     */
    public MessageResendHandler setFwHeaderDefinition(FwHeaderDefinition def) {
        fwHeaderDefinition = def;
        return this;
    }
}
