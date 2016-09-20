package nablarch.fw.messaging;

import nablarch.core.util.annotation.Published;
import nablarch.fw.messaging.provider.MessagingExceptionFactory;


/**
 * メッセージング機能の基本API({@link MessagingContext})の実装系を提供する
 * モジュールが実装するインターフェース。
 * <p/>
 * 本インターフェースの実装系の切り替えによって多様なメッセージングミドルウェアに
 * 対応することができる。 
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public interface MessagingProvider {
    /**
     * メッセージングコンテキストを返す。
     * 
     * @return メッセージングコンテキスト
     */
    MessagingContext createContext();
    
    /**
     * 同期送信処理における応答受信待ちのデフォルトタイムアウト値を設定する。
     * (単位:msec)
     * @param timeout デフォルトタイムアウト値 (単位:msec)
     * @return このオブジェクト自体
     */
    MessagingProvider setDefaultResponseTimeout(long timeout);
    
    /**
     * 送信電文の有効期間のデフォルト値を設定する。 (単位:msec)
     * @param timeToLive 送信電文の有効期間 (単位:msec)
     * @return このオブジェクト自体
     */
    MessagingProvider setDefaultTimeToLive(long timeToLive);
    
    /**
     * {@link MessagingException}ファクトリオブジェクトを設定する。
     * @param messagingExceptionFactory {@link MessagingException}ファクトリオブジェクト
     * @return このオブジェクト自体
     */
    MessagingProvider setMessagingExceptionFactory(MessagingExceptionFactory messagingExceptionFactory);
}

