package nablarch.fw.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

import static nablarch.core.util.StringUtil.isNullOrEmpty;

/**
 * 外部システムに対する送受信電文の内容を格納するデータオブジェクト。
 * 
 * このオブジェクトは以下のデータを保持する。
 * <ul>
 * <li> プロトコルヘッダー
 * <li> メッセージボディ
 * </ul>
 * 
 * <h4>プロトコルヘッダー</h4>
 * <p>
 * プロトコルヘッダーはMOM間のメッセージ転送制御に関連する制御情報であり、
 * MOM側で自動的に解析される。
 * これらの情報の多くは、MOM製品に依存した仕様であるため、本フレームワークは
 * プロトコルヘッダーに極力依存しない設計となっている。
 * </p>
 * 本フレームワークが直接使用するプロトコルヘッダーは以下の5つのみである。
 * <ul>
 * <li> メッセージIDヘッダ
 * <li> 関連メッセージIDヘッダ
 * <li> 送信宛先ヘッダ
 * <li> 応答宛先ヘッダ
 * <li> メッセージ有効期間ヘッダ (送信電文のみ)
 * </ul>
 * 
 * <h4>メッセージボディ</h4>
 * <p>
 * プロトコルヘッダーを除いた電文のデータ部をメッセージボディと呼ぶ。
 * メッセージボディ部は、MOM側では単にバイナリデータとして扱うものとし、
 * 解析は全てフレームワーク側で行う。
 * </p>
 * <p>
 * メッセージボディのフォーマットは nablarch.core.dataformat パッケージが提供する
 * 機能を用いて定義する。解析後の電文はList-Map形式でアクセスすることが可能である。
 * </p>
 * メッセージボディ内は通常、以下に示すような階層構造をもち、その解析は各階層に
 * 対応したデータリーダもしくはハンドラによって段階的に行われる。
 * <ul>
 * <li> フレームワーク制御ヘッダ
 * <li> 業務共通ヘッダ
 * <li> 業務データ
 * </ul>
 * なお、メッセージボディの階層化はデータリーダやハンドラの構成を含め、フレームワークの
 * 全体構造に関わる重要な要素なので、可能な限り早い段階で確定させておく必要がある。
 * 
 * <h4>クラス構成</h4>
 * <p>
 * このクラスは各電文クラスに共通する機能やデータ構造を実装するものの、
 * 直接インスタンスを作成することはできない。
 * これは、Jakarta Messagingのjakarta.jms.Messageのような汎用メッセージオブジェクトを使いまわすのではなく、
 * その用途に応じて以下の4つの具象クラスを使いわける設計となっているためである。
 * <p>
 * <ol>
 * <li> ReceivedMessage (受信メッセージ)
 * <li> SendingMessage  (送信メッセージ)
 * <li> RequestMessage  (被仕向け要求受信メッセージ) : ReceivedMessageのサブクラス
 * <li> ResponseMessage (応答送信メッセージ) : SendingMessageのサブクラス
 * </ol>
 * 
 * @param <TSelf> 各具象クラスの型
 * 
 * @author Iwauo Tajima
 */
public abstract class InterSystemMessage<TSelf extends InterSystemMessage<?>> {
    // ------------------------------------------------------- structure
    /** メッセージヘッダ情報を格納するMap */
    private final Map<String, Object> headers;
    
    /** メッセージボディデータ */
    private final List<DataRecord> bodyData;
    
    /** メッセージボディのフォーマッター */
    private DataRecordFormatter formatter;
    
    /**
     * フレームワークで使用する既定ヘッダーのフィールド名
     */
    public static final class HeaderName {
        /** メッセージIDヘッダ */
        public static final String MESSAGE_ID = "MessageId";
        
        /** 送信宛先ヘッダ */
        public static final String DESTINATION = "Destination";
        
        /** 応答宛先ヘッダ */
        public static final String REPLY_TO = "ReplyTo";
        
        /** 関連メッセージIDヘッダ */
        public static final String CORRELATION_ID = "CorrelationId";
        
        /** メッセージ有効期間ヘッダ (送信電文のみ) */
        public static final String TIME_TO_LIVE = "TimeToLive";
        
        /** コンストラクタ。 */
        private HeaderName() {
        }
    }
    
    // -------------------------------------------------------- constructors
    /**
     * デフォルトコンストラクタ
     */
    public InterSystemMessage() {
        this.headers   = new HashMap<String, Object>();
        this.bodyData  = new ArrayList<DataRecord>();
        this.formatter = null;
    }
    
    /**
     * コピーコンストラクタ
     * @param orgMessage コピー元電文
     */
    public InterSystemMessage(InterSystemMessage<?> orgMessage) {
        this.headers   = orgMessage.headers;
        this.bodyData  = orgMessage.bodyData;
        this.formatter = orgMessage.formatter;
    }
    
    // --------------------------------------------- accessors for message body
    /**
     * メッセージボディのフォーマット定義を設定する。
     * @param formatter フォーマット定義オブジェクト
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    @Published(tag = "architect")
    public TSelf setFormatter(DataRecordFormatter formatter) {
        this.formatter = formatter;
        return (TSelf) this;
    }
    
    /**
     * メッセージボディのフォーマット定義を返す。
     * @return メッセージボディのフォーマット定義
     */
    public DataRecordFormatter getFormatter() {
        return formatter;
    }
    
    /**
     * 指定された種別のレコードを返す。
     * 複数存在する場合は、その先頭のレコードを返す。
     * 存在しない場合はnullを返す。
     * 
     * @param recordType レコード名
     * @return 指定した種別のデータレコード (存在しない場合はnull)
     */
    @Published(tag = "architect")
    public DataRecord getRecordOf(String recordType) {
        for (DataRecord record : getRecords()) {
            if (record.getRecordType().equals(recordType)) {
                return record;
            }
        }
        return null;
    }
    
    /**
     * メッセージボディに含まれる全レコードを返す。
     * @return メッセージボディに含まれる全てのレコード
     */
    @Published(tag = "architect")
    public List<DataRecord> getRecords() {
        return bodyData;
    }
    
    /**
     * メッセージボディに含まれる指定された種別の全レコードを返す。
     * 該当するレコードが存在しない場合は空のリストを返す。
     * 
     * @param recordType レコード種別
     * @return メッセージボディに含まれる指定された種別の全レコード
     */
    @Published(tag = "architect")
    public List<DataRecord> getRecordsOf(String recordType) {
        if (StringUtil.isNullOrEmpty(recordType)) {
            throw new IllegalArgumentException("recordType must not be blank.");
        }
        List<DataRecord> results = new ArrayList<DataRecord>();
        for (DataRecord record : getRecords()) {
            if (record.getRecordType().equals(recordType)) {
                results.add(record);
            }
        }
        return results;
    }
    
    /**
     * 電文のデータ部の末尾レコードを返す。
     * <p/>
     * 主にシングルレコード形式の電文で使用することを想定している。
     * レコードが存在しない場合はnullを返す。
     * 
     * @return 受信電文内の各フィールドの値を格納したMap
     */
    @Published(tag = "architect")
    public Map<String, Object> getParamMap() {
        return (bodyData.size() == 0) ? null
                                      : bodyData.get(bodyData.size() - 1);
    }
    
    /**
     * 電文のデータ部の末尾レコードの中から指定されたフィールドの値を取得して返す。
     * <p/>
     * 主にシングルレコード形式の電文で使用することを想定している。
     * レコードが存在しない場合、もしくは、レコードに指定された項目が存在しない
     * 場合はnullを返す。
     * 。
     * @param name 取得するフィールドの名称
     * @return フィールドの値
     */
    @Published(tag = "architect")
    public Object getParam(String name) {
        Map<String, Object> record = getParamMap();
        return (record == null) ? null : record.get(name);
    }

    /**
     * メッセーボディのバイナリ表現を返す。
     * 
     * 送信(仕向)電文の場合はデータレコードをレコードフォーマッタで直列化したものを返す。
     * 受信(被仕向)電文の場合はパース前の送信電文の内容をそのまま返す。
     * 
     * @return メッセーボディのバイナリ表現
     */
    public abstract byte[] getBodyBytes();
    
    // ------------------------------------------- accessors for message header
    /**
     * ヘッダーの一覧をMap形式で返す。
     * @return ヘッダーの一覧
     */
    @Published(tag = "architect")
    public Map<String, Object> getHeaderMap() {
        return headers;
    }
    
    /**
     * ヘッダーの値を返す。
     * @param <T> 期待するヘッダの型
     * @param headerName 値を取得するヘッダーの名前
     * @return ヘッダーの値
     */
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String headerName) {
        return (T) headers.get(headerName);
    }
    
    /**
     * ヘッダーの値を設定する。
     * 
     * @param name  値を設定するヘッダーの名前
     * @param value ヘッダーの値
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    public TSelf setHeader(String name, Object value) {
        this.headers.put(name, value);
        return (TSelf) this;
    }
    
    /**
     * ヘッダーの一覧を設定する。
     * (既存のヘッダーは全て削除される。)
     * 
     * @param headers ヘッダーの一覧
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    public TSelf setHeaderMap(Map<String, ?> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
        return (TSelf) this;
    }
    
    // --------------------------------------------- protocol headers
    /**
     * この電文に割り当てられた識別子(メッセージID)を返す。
     * 
     * メッセージIDは電文送信時にMOMによって自動的に割り振られるため、
     * 書式や一意性の範囲は製品依存となる。
     * また、送信前の電文にはnullが設定されている。
     * 
     * @return この電文のID文字列 (送信前はnull)
     */
    @Published(tag = "architect")
    public String getMessageId() {
        return getHeader(HeaderName.MESSAGE_ID);
    }
    
    /**
     * メッセージIDを設定する。
     * 
     * メッセージIDはMOM側で採番される値であり、
     * このメソッドは単体テスト用に便宜的に容易されているものである。
     * 
     * @param messageId メッセージIDとして指定する文字列
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    @Published(tag = "architect")
    public TSelf setMessageId(String messageId) {
        if (isNullOrEmpty(messageId)) {
            throw new IllegalArgumentException("messageId must not be blank.");
        }
        setHeader(HeaderName.MESSAGE_ID, messageId);
        return (TSelf) this;
    }
    
    /**
     * この電文の宛先キューの論理名を取得する。
     * 
     * MessagingContext.send(SendingMessage) メソッドでは、この戻り値に対応する
     * 宛先に送信される。 
     * 
     * @return 宛先キューの論理名
     */
    @Published(tag = "architect")
    public String getDestination() {
        return getHeader(HeaderName.DESTINATION);
    }
    
    /**
     * 送信宛先キューの論理名を設定する。
     * 
     * @param destination 応答宛先キューの論理名
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    @Published(tag = "architect")
    public TSelf setDestination(String destination) {
        if (isNullOrEmpty(destination)) {
            throw new IllegalArgumentException(
                "destination header must not be blank."
            );
        }
        setHeader(HeaderName.DESTINATION, destination);
        return (TSelf) this;
    }

    /**
     * この電文に関連付けられているメッセージのメッセージIDを返す。
     * @return この電文のID文字列
     */
    @Published(tag = "architect")
    public String getCorrelationId() {
        Object value = getHeader(HeaderName.CORRELATION_ID);
        return (value == null) ? null : value.toString();
    }
    
    /**
     * この電文に既存のメッセージのIDを関連付ける。
     * @param messageId 関連付けるメッセージのID
     * @return この電文のID文字列
     */
    @SuppressWarnings("unchecked")
    @Published(tag = "architect")
    public TSelf setCorrelationId(String messageId) {
        if (isNullOrEmpty(messageId)) {
            throw new IllegalArgumentException("messageId must not be blank.");
        }
        setHeader(HeaderName.CORRELATION_ID, messageId);
        return (TSelf) this;
    }
    
    /**
     * 応答宛先キューの論理名を返す。
     * @return この電文のID文字列
     */
    @Published(tag = "architect")
    public String getReplyTo() {
        return getHeader(HeaderName.REPLY_TO);
    }
    
    /**
     * この電文に対する応答宛先となるキューの論理名を設定する。
     * 
     * @param replyTo 応答宛先キューの論理名
     * @return このオブジェクト自体
     */
    @SuppressWarnings("unchecked")
    @Published(tag = "architect")
    public TSelf setReplyTo(String replyTo) {
        if (isNullOrEmpty(replyTo)) {
            throw new IllegalArgumentException("replyTo header not be blank.");
        }
        setHeader(HeaderName.REPLY_TO, replyTo);
        return (TSelf) this;
    }
}
