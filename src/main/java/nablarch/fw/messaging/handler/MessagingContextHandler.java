package nablarch.fw.messaging.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.MessagingProvider;
import static nablarch.core.util.FileUtil.closeQuietly;

/**
 * メッセージコンテキストの初期化、スレッドコンテキストへの登録、および終端処理の実行
 * 行うハンドラクラス。
 * 
 * @author Iwauo Tajima
 */
public class MessagingContextHandler implements Handler<Object, Object> {
    // ---------------------------------------------------- structure
    /** メッセージング機能の実装 */
    private MessagingProvider provider = null;
    
    
    // ---------------------------------------------------- Handler API
    /**{@inheritDoc}
     * この実装では、後続ハンドラへの処理移譲の前後で、メッセージコンテキストの
     * 初期化および終端処理を行う。
     */
    public Object handle(Object data, ExecutionContext context) {
        
        MessagingContext messagingCtx = null;
        try {
            messagingCtx = provider.createContext();
            MessagingContext.attach(messagingCtx);
            return context.handleNext(data);
            
        } finally {
            closeQuietly(messagingCtx);
            MessagingContext.detach();
        }
    }
    
    // ------------------------------------------------- accessors
    /**
     * メッセージング機能実装を設定する。
     * @param provider メッセージング機能実装実体
     * @return このインスタンス自体
     */
    public MessagingContextHandler
    setMessagingProvider(MessagingProvider provider) {
        this.provider = provider;
        return this;
    }
}
