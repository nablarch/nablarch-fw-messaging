package nablarch.fw.messaging.handler;


import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.log.app.FailureLogUtil;
import nablarch.core.message.ApplicationException;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.NoMoreHandlerException;
import nablarch.fw.Result;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.results.ServiceError;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpErrorHandler;

/**
 * HTTPメッセージングサービスにおけるエラー制御を透過的に実装するテスト用ハンドラー。
 * 
 * このハンドラーでは、後続の各ハンドラーで発生した実行時例外およびおよびエラーを捕捉し、
 * その内容に基づいてログ出力を行ったのち、HttpErrorResponseオブジェクトとしてリターンする。
 * 
 * @author Iwauo Tajima
 */
public class TestHttpMessagingErrorHandler extends HttpErrorHandler {

    @Override
    public HttpResponse
    handle(HttpRequest req, ExecutionContext ctx) {
        
        req.getHeaderMap().put("X-Requested-With", "XMLHttpRequest");
        HttpResponse res = null;
        
        try {
            res = ctx.handleNext(req);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.logTrace("HTTP Response: ", res, res.getContentPath());
            }
            
        } catch (HttpErrorResponse e) {
            ctx.setException(e.getCause());
            Throwable cause = e.getCause();
            if (cause != null) {
                handleError(e.getCause(), req, ctx);
            }
            res = e.getResponse();

        } catch (ThreadDeath e) {
            throw e;
        } catch (VirtualMachineError e) {
            throw e;
            // アプリケーション側で対処すべき状況ではないので、
            // このハンドラでは特段の処理を行わず上位にリスローする。            
        } catch (Throwable e) {
            res = handleError(e, req, ctx);
        }
        
        if (res.isBodyEmpty()) {
            res.setContentPath(getDefaultPageFor(res.getStatusCode()));
        }
        return res;
    }
    
    
    /**
     * 発生した例外に応じたログ出力処理を行う。
     * 
     * @param e 発生した例外
     * @param req HTTPリクエスト
     * @param ctx 実行時コンテキスト
     * @return HTTPレスポンス
     */
    protected HttpResponse
    handleError(Throwable e, HttpRequest req, ExecutionContext ctx) {
        if (e instanceof NoMoreHandlerException) {
            // ハンドラキューが空になり、後続ハンドラに処理を
            // 委譲できなかった場合は、404エラーをレスポンスする。
            LOGGER.logInfo(Builder.concat("There were no Handlers in handlerQueue.",
                                          " uri = [", req.getRequestUri(), "]"));
            return HttpResponse.Status.NOT_FOUND.handle(req, ctx);
        }
        
        if (e instanceof Result.Error) {
            // 共通ハンドラ等から送出される汎用例外。
            // 対応するHTTPステータスコードのエラー画面をレスポンスする。
            Result.Error err = (Result.Error) e;
            if (writeFailureLogPattern.matcher(String.valueOf(err.getStatusCode())).matches()) {
                if (err instanceof ServiceError) {
                    ((ServiceError) err).writeLog(ctx);
                } else {
                    FailureLogUtil.logFatal(err, ctx.getDataProcessedWhenThrown(err), null);
                }
            }
            ctx.setException(err.getCause());
            return new HttpResponse(err.getStatusCode());
        }

        
        if (e instanceof MessagingException
         || e instanceof ApplicationException) {
            
            if (LOGGER.isTraceEnabled()) {
                LOGGER.logTrace("Error due to an invalid request message: ", e);
            }
            return new HttpErrorResponse(400, e).getResponse();
        }

        if (e instanceof RuntimeException) {
            // 未捕捉の実行時例外が発生した場合はエラーログを出力し、
            // HTTPステータス500のレスポンスオブジェクトを返却する。
            // Uncaught runtime exception:
            FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
            ctx.setException(e);
            return HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);
        }
        
        if (e instanceof StackOverflowError) {
            // 無限ループのバグの可能性が高いので、通常のエラー扱い。
            // Uncaught Error:
            FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
            ctx.setException(e);
            return HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);
        }
        
        // 上記以外のエラーについてはログ出力後、
        // ステータスコード500のレスポンスを返す。
        FailureLogUtil.logFatal(e, ctx.getDataProcessedWhenThrown(e), null);
        ctx.setException(e);
        return HttpResponse.Status.INTERNAL_SERVER_ERROR.handle(req, ctx);        
    }    
    
    /** ロガー */
    private static final Logger
    LOGGER = LoggerManager.get(TestHttpMessagingErrorHandler.class);
}
