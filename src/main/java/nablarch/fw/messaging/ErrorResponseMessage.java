package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;
import nablarch.fw.Result;

/**
 * エラー応答として送信する電文情報を含んだ実行時例外。
 * 
 * 本クラスを送出することで、業務トランザクションはロールバックしつつ、
 * 任意の内容の応答電文を送信することができる。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class ErrorResponseMessage extends Result.Error {
    // ----------------------------------------------- Structure
    /** エラー応答電文の内容 */
    private final ResponseMessage response;
    
    // ---------------------------------------------- Constructor
    /**
     * コンストラクタ。
     * @param response エラー応答電文の内容
     */
    public ErrorResponseMessage(ResponseMessage response) {
        this.response = response;
    }
    
    /**
     * 元例外とエラー応答電文の内容指定し、インスタンスを生成する。
     * @param response エラー応答電文の内容
     * @param e        元例外
     */
    public ErrorResponseMessage(ResponseMessage response, Throwable e) {
        super(e);
        Result result = (e instanceof Result)
                      ? (Result) e
                      : new nablarch.fw.results.InternalError(e);
        response.setResult(result);
        this.response = response;
    }
    
    // ----------------------------------------------- Result I/F
    @Override
    public String getMessage() {
        return response.getMessage();
    }

    @Override
    public int getStatusCode() {
        return response.getStatusCode();
    }

    @Override
    public String toString() {
        return response.toString();
    }

    @Override
    public boolean isSuccess() {
        return response.isSuccess();
    }
    
    // ------------------------------------------------ Accessor

    /**
     * エラー応答電文オブジェクトを返す。
     * @return エラー応答電文オブジェクト
     */
    public ResponseMessage getResponse() {
        return response;
    }
}
