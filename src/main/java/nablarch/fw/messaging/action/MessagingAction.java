// MOVE: nablarch.fw.actionから移動。（メッセージングでしか使わないので）
package nablarch.fw.messaging.action;


import nablarch.common.handler.TransactionManagementHandler;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.db.support.DbAccessSupport;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.TransactionEventCallback;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;

/**
 * 被仕向同期応答処理を行う業務処理で使用するテンプレートクラス。
 * <p/>
 * 業務アクションハンドラは、本クラスを継承し、本クラスに定義されたテンプレートメソッドを必要に応じて実装する。
 *
 * @author Iwauo Tajima
 */
public abstract class MessagingAction
extends    DbAccessSupport 
implements Handler<RequestMessage, ResponseMessage>,
           TransactionEventCallback<RequestMessage> {

    /**
     * MessagingActionオブジェクトを生成する。
     */
    @Published
    public MessagingAction() {
        super();
    }

    /**
     * 要求電文毎にフレームワークから起動され、要求電文の内容をもとに業務処理を実行する。
     * <p/>
     *
     * @param request 要求電文オブジェクト
     * @param context 実行コンテキスト
     * @return 応答電文オブジェクト
     */
    @Published
    protected abstract ResponseMessage
    onReceive(RequestMessage request, ExecutionContext context);
    
    /**
     * 業務トランザクションが異常終了した場合の処理を実行する。
     * <p/>
     * 本メソッドは、業務トランザクションがロールバックされると起動される。
     * 任意のエラー応答電文({@link ResponseMessage})を返却したい場合は、本メソッドをオーバーライドすること。
     * デフォルト実装ではなにもしない。
     * <p/>
     * 注意:
     *   以下のケースでは業務アクションへのディスパッチが行われる前にエラーが
     *   発生するため、エラー応答電文の内容を制御することはできない。
     *   <ul>
     *       <li>フレームワーク制御ヘッダの形式不正</li>
     *       <li>MOMミドルウェアのレイヤーで発生したエラー (JMSException等)</li>
     *   </ul>
     *
     * @param e       発生したエラーオブジェクト
     * @param request 要求電文オブジェクト
     * @param context 実行コンテキスト
     * @return エラー応答用電文。{@code null}を返した場合はフレームワークが編集した電文が送信される。
     */
    @Published
    protected ResponseMessage
    onError(Throwable e, RequestMessage request, ExecutionContext context) {
        return null;
    }
    
    /**
     * 電文のメッセージボディの内容をデフォルトのフォーマッタを使用して
     * 自動的に読み込むかどうかを指定する。
     * <p/>
     * 一般に使用される電文は
     * (ヘッダーレコード) + (業務レコード)
     * の2レイアウトで構成されることが多い。
     * そのようなケースでは本機能を有効にすることにより、
     * 業務処理に制御が移った時点で、業務データ部の解析が全て完了した状態となり、
     * 業務側で電文のレイアウトを意識した処理を実装する必要が無くなる。
     * (このオプションはデフォルトで有効となっている。)
     * <p/>
     * 業務データ部が複数のレコードによって構成されている場合や、
     * 単一のフォーマット定義ファイルで定義できない場合、さらには、電文レイアウトが
     * 動的に変更されるといったケースでは、本メソッドをオーバーライドしfalseを返すことで、
     * 業務データ部の読み込みを業務ロジック側で制御することができる。
     * 
     * @return 電文のメッセージボディの内容を自動的に読み込む場合は true
     */
    protected boolean usesAutoRead() {
        return true;
    }
    
    /**{@inheritDoc}
     * 本クラスの実装では、まず最初に{@link #usesAutoRead()}を呼ぶ。
     * その結果がfalseであれば{@link #onReceive(RequestMessage, ExecutionContext)}
     * に処理を移譲し、その結果を返す。
     * <p/>
     * {@link #usesAutoRead()}がtrueを返す場合は、業務ロジックに入る前に業務データ部の
     * フォーマッタを使用して、メッセージボディから1レコード分のデータを読み込む。
     * 業務データ部からレコードが読み込めない場合、または、1レコードの読み込みが完了
     * した時点でデータ終端に達しない場合はフォーマット不正のエラーを送出する。
     */
    public ResponseMessage handle(RequestMessage req, ExecutionContext ctx) {
        if (usesAutoRead()) {
            req.readRecord(); // 1件分のデータレコードを読み込む。
            if (req.readRecord() != null) {
                throw new InvalidDataFormatException(
                    "There remains unread bytes after a record has read. "
                +   "if multi-record messages are allowed, "
                +   "you must unset 'autoRead' option of MessageAction.'"         
                );
            }
        }
        return onReceive(req, ctx);
    }
    
    /** {@inheritDoc}
     * このクラスでは、{@link TransactionManagementHandler} 上のトランザクションを監視する。
     */
    public Class<TransactionManagementHandler> transactionCallBackWatchType() {
        return TransactionManagementHandler.class;
    }

    /** {@inheritDoc} */
    public void transactionAbnormalEnd(Throwable e, RequestMessage data, ExecutionContext ctx) {
        ResponseMessage errorResponse = onError(e, data, ctx);
        if (errorResponse != null) {
            errorResponse.throwAsError(e);
        }
    }

    /** {@inheritDoc} */
    public void transactionNormalEnd(RequestMessage data, ExecutionContext ctx) {
    }
}
