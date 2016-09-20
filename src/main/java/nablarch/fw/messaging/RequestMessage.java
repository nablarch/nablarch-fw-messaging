package nablarch.fw.messaging;

import static nablarch.core.util.StringUtil.isNullOrEmpty;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.util.annotation.Published;
import nablarch.fw.Request;

/**
 * 外部システムから受信した処理要求電文の内容を格納し、対応する応答電文を作成するクラス。
 * <p/>
 * {@link ReceivedMessage}と比べて、以下の処理が追加されている。
 * <ul>
 *     <li>フレームワーク制御ヘッダ({@link FwHeader})を保持する</li>
 *     <li>応答電文({@link ResponseMessage})オブジェクトを作成する</li>
 * </ul>
 * <p/>
 * 本クラスは以下のデータを格納する。
 * <pre>
 *   1. プロトコルヘッダ (Map)
 *        - メッセージID (String)
 *        - 宛先キュー名 (String)   ...等
 *                 
 *   2.  フレームワーク制御ヘッダ (Map)
 *        - リクエストID (String)
 *        - ユーザID (String)
 *        - 再送制御フラグ (Boolean)
 *        - 処理結果ステータスコード (String)  ...等
 *                 
 *   3.  メッセージボディ(byte[])
 *      ※ フレームワーク制御ヘッダに相当するデータは含まれない。
 * </pre>
 * <p/>
 * このクラスは{@link Request}インタフェースを実装しており、後続業務処理の入力となる。
 * (リクエストパスとして、フレームワーク制御ヘッダのリクエストIDを使用する。)
 * 
 * @author Iwauo Tajima
 */
@Published
public class RequestMessage extends ReceivedMessage implements Request<Object> {
    // --------------------------------------------------------- Structure
    /** フレームワーク制御ヘッダ */
    private final FwHeader fwHeader;
    
    /** 応答電文のフォーマッタ **/
    private DataRecordFormatter formatterOfReply;

    // --------------------------------------------------------- constructor
    /**
     * @{link RequestMessage}のインスタンスを生成する。
     *
     * @param header フレームワーク制御ヘッダ
     * @param message 受信電文オブジェクト(フレームワーク制御ヘッダに相当するデータが抜き出し済みであること)
     */
    public RequestMessage(FwHeader header, ReceivedMessage message) {
        super(message);
        fwHeader = header;
    }
    
    // ------------------------------------------------------- published APIs
    /**
     * この電文に対する応答電文({@link ResponseMessage})オブジェクトを作成する。
     * <p/>
     * {@link RequestMessage#setFormatter}で応答電文のフォーマットが指定されている場合はそれを設定する。
     * 指定がなければ、{@link InterSystemMessage#getFormatter}を実行し、電文共通のフォーマットを取得して設定する。
     * <p/>
     * 応答電文オブジェクトの生成については、{@link #createResponseMessage}を参照。
     *
     * @return 返信用電文オブジェクト
     * @throws MessagingException この電文にReplyToヘッダが指定されていない場合。
     */
    public ResponseMessage reply() throws UnsupportedOperationException {
        if (getReplyTo() == null) {
            throw new MessagingException(
              "can not reply to this message "
            + "because there was no 'replyTo' header on it."
            );
        }
        ResponseMessage response = createResponseMessage();
        // 応答電文のフォーマットが指定されている場合はそれを設定
        // そうでなければ、要求電文のフォーマットを流用する。
        response.setFormatter(
            (formatterOfReply == null) ? getFormatter()
                                       : formatterOfReply
        );
        return response;
    }
    
    /**
     * 応答電文オブジェクトを作成する。
     * <p/>
     * この実装では、応答電文オブジェクトのヘッダの設定は
     * {@link ResponseMessage#ResponseMessage(RequestMessage)}にて行われる。
     * <p/>
     * デフォルト以外の応答電文クラスを使用する場合はサブクラスで本メソッドをオーバーライドすること。
     * 
     * @return 応答電文オブジェクト
     */
    protected ResponseMessage createResponseMessage() {
        return new ResponseMessage(this);
    }
    
    // --------------------------------------------------------- Request I/F
    /** {@inheritDoc}
     * @throws IllegalArgumentException {@code requestPath}が{@code null}か空文字である場合
     */
    public String getRequestPath() {
        return fwHeader.getRequestId();
    }
    
    /** {@inheritDoc} */
    public RequestMessage setRequestPath(String requestPath) {
        if (isNullOrEmpty(requestPath)) {
            throw new IllegalArgumentException("requestPath must not be blank.");
        }
        fwHeader.setRequestId(requestPath);
        return this;
    }
    
    // --------------------------------------------------------- Accessors    
    /**
     * フレームワーク制御ヘッダの内容を返す。
     * @return フレームワーク制御ヘッダ部の内容
     */
    public FwHeader getFwHeader() {
        return fwHeader;
    }

    /**
     * 応答電文のフォーマットを指定する。
     * @param formatter 応答電文のフォーマット
     * @return このオブジェクト自体
     */
    public RequestMessage setFormatterOfReply(DataRecordFormatter formatter) {
        formatterOfReply = formatter;
        return this;
    }
}
