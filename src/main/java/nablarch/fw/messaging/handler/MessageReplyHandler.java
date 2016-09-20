package nablarch.fw.messaging.handler;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.annotation.Published;
import nablarch.fw.DataReader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Result;
import nablarch.fw.messaging.ErrorResponseMessage;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.MessageReadError;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.StandardFwHeaderDefinition;

/**   
 * 受信電文に設定された宛先に対して応答電文を送信するハンドラ。 
 * 
 * 本ハンドラは、後続ハンドラの処理結果であるResponseMessageオブジェクトの内容を
 * もとに応答電文を構築し送信する。
 * 送信した応答電文オブジェクトをこのハンドラの戻り値として返す。
 * 
 * <div><b>他のハンドラとの前後関係</b></div>
 * <hr/>
 * 
 * <pre>
 * - トランザクション制御ハンドラ
 *    同ハンドラとの前後関係は、2相コミットを使用するか否かで変わる。
 *   
 *    a) 2相コミットを使用する場合。
 *       DBトランザクションとJMSトランザクションをトランザクションマネージャー側で
 *       まとめてコミットするので、トランザクション制御ハンドラは、このハンドラより
 *       先に実行されなければならない。
 *      
 *    b) 2相コミットを使用しない場合。
 *      このハンドラが応答を送信する前に、DBトランザクション終了させ、業務処理の結果を
 *      確定させる必要がある。
 *      このため、トランザクション制御ハンドラはこのハンドラの後に実行されなければ
 *      ならない。
 *      
 * - データリードハンドラ
 *    要求電文のフォーマット不正に起因する例外はデータリーダで発生する可能性があり、
 *    そのエラー応答電文をこのハンドラで送出する必要がある。
 *    このため、データリードハンドラはこのハンドラの後に実行されなければならない。
 * </pre>
 * 
 * 
 * <div><b>例外制御</b></div>
 * <hr/>
 * このハンドラでは、全てのエラー(java.lang.Error)及び実行時例外を補足しエラー応答を行う。
 * (従って、本ハンドラではいかなる場合においても応答電文の送信処理を実行することになる。)
 * エラー応答が正常に終了した場合は、捕捉した例外を再送出する。
 * Fatalログの出力およびサーバプロセス停止要否の判断はnablarch.fw.handler.RequestThreadLoopHandlerで行う。
 * </p>
 * 応答電文の送信処理中にエラーが発生した場合は、以下の処理を行う。
 * いずれのケースにおいても、Fatalログの出力はnablarch.fw.handler.RequestThreadLoopHandlerで行われる。
 * <pre>
 * 1. 後続ハンドラが正常終了していた(=応答電文オブジェクトがリターンされていた)場合。
 *      送信エラーを再送出する。
 *      
 * 2. 後続ハンドラが異常終了していた(=実行時例外/エラーが送出されていた)場合。
 *      送信エラーの内容をWarningレベルのログに出力した上で、
 *      後続ハンドラが送出した例外を再送出する。　
 * </pre>
 * 
 * @author Iwauo Tajima
 */
public class MessageReplyHandler implements Handler<Object, Result> {
    // ---------------------------------------------------- structure
    /** 応答電文中のフレームワークヘッダ定義 */
    private FwHeaderDefinition fwHeaderDefinition = new StandardFwHeaderDefinition();

    /**
     * コンストラクタ。
     */
    @Published(tag = "architect")
    public MessageReplyHandler() {
    }

    // ---------------------------------------------------- Handler I/F
    /** {@inheritDoc}
     * この実装では後続ハンドラの処理結果であるResponseMessageのオブジェクトの内容を
     * もとに応答電文を構築し送信する。
     * また、送信した応答電文オブジェクトをハンドラの戻り値として返す。
     */
    public Result handle(Object data, ExecutionContext ctx) {
        Result   result = null;
        Throwable error = null;
        try {
            result = ctx.handleNext(data);
            if (result instanceof DataReader.NoMoreRecord) {
                return result; // メッセージリーダがタイムアウトもしくは閉局。
            }
            
        } catch (Throwable e) {
            error = e;
            try {
                result = errorResponseOf(e, ctx);
            } catch (Throwable th) {
                LOGGER.logWarn("an error occurred while replying error response. ", th);
                rethrow(e);
            }
        }
        
        try {
            ResponseMessage res = (ResponseMessage) result;
            res.setFwHeaderDefinition(fwHeaderDefinition);
            if (res.getFwHeader().getStatusCode() == null) {
                res.getFwHeader().setStatusCode(getStatusCode(res));
            }
            MessagingContext.getInstance().send(res);

        } catch (Throwable th) {
            if (error != null) {
                LOGGER.logWarn("an error occurred while sending response. ", th);
                rethrow(error);
            }
            rethrow(th);
        }
        if (error != null) {
            rethrow(error);
        }
        return result;
    }
    
    /**
     * 後続ハンドラの処理中に未捕捉の例外が発生した場合に応答するエラー電文を作成する。
     * 
     * @param ctx 実行コンテキスト
     * @param e   発生した例外
     * @return エラー応答電文
     */
    @Published(tag = "architect")
    protected ResponseMessage errorResponseOf(Throwable e, ExecutionContext ctx) {
        if (e instanceof ErrorResponseMessage) {
            return ((ErrorResponseMessage) e).getResponse();
        }
        RequestMessage req = ctx.getLastReadData();
        if (req == null) {
            if (e instanceof MessageReadError) {
                ReceivedMessage message = ((MessageReadError) e).getReceivedMessage();
                req = new RequestMessage(new FwHeader(), message);
            } else {
                // ここにはこないはず。
                throw new MessagingException(
                    "An unexpected error occurred "
                  + "and could not respond any message to client.", e
                );
            }
        }
        ResponseMessage res = req.reply();
        Result result = (e instanceof Result)
                      ? (Result) e
                      : new nablarch.fw.results.InternalError(e);
        res.setStatusCodeHeader(Integer.toString(result.getStatusCode()));
        return res.setResult(result);
    }
    
    /**
     * 応答電文のフレームワーク制御ヘッダに設定するステータスコードを取得する。
     * <p/>
     * デフォルトの設定では、Result.getStatusCode() の戻り値と同じ値を
     * フレームワーク制御ヘッダーに指定する。
     * 
     * プロジェクト固有のステータスコード体系を定義している場合は、本メソッドを
     * オーバーライドすること。
     * 
     * @param message 応答電文オブジェクト
     * @return フレームワーク制御ヘッダに設定するステータスコード
     */
    protected String getStatusCode(ResponseMessage message) {
        return Integer.toString(message.getStatusCode());
    }
    
    /**
     * 渡された例外を再送出する。
     * @param e 例外
     */
    private void rethrow(Throwable e) {
        if (e instanceof Error) {
            throw (Error) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e); //can not happen.
    }
    
    // --------------------------------------------------------- accessors
    /**
     * 応答電文中のフレームワーク制御ヘッダ定義を設定する。
     * @param def フレームワーク制御ヘッダ定義
     * @return このオブジェクト自体
     */
    public MessageReplyHandler setFwHeaderDefinition(FwHeaderDefinition def) {
        fwHeaderDefinition = def;
        return this;
    }
    
    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(MessageReplyHandler.class);
}
