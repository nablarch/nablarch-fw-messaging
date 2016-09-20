package nablarch.fw.messaging;

import java.util.HashMap;

import nablarch.core.util.annotation.Published;

/**
 * 電文中のフレームワーク制御ヘッダ部の内容をMapとして格納するクラス。
 * 
 * <div><b>フレームワーク制御ヘッダ</b></div>
 * <hr/>
 * 本フレームワークが提供する機能の中には、電文中に特定の制御項目が定義されている
 * ことを前提として設計されているものが多く存在する。
 * そのような制御項目のことを「フレームワーク制御ヘッダ」とよぶ。
 * 
 * フレームワーク制御ヘッダの一覧とそれを使用するハンドラの対応は以下のとおり。
 * <pre>
 * ========================= ========================================
 * フレームワーク制御ヘッダ             関連する主要なハンドラ                     
 * ========================= ========================================
 * リクエストID                 nablarch.fw.handler.RequestPathJavaPackageMapping
 *                            nablarch.fw.RequestHandlerEntry
 *                            nablarch.common.handler.ServiceAvailabilityCheckHandler
 *                            nablarch.common.handler.PermissionCheckHandler
 *                             ...他
 * ユーザID                    nablarch.common.handler.PermissionCheckHandler
 * 再送要求フラグ                nablarch.fw.messaging.handler.MessageResendHandler
 * ステータスコード              {@link nablarch.fw.messaging.handler.MessageReplyHandler}
 * ========================= ========================================
 * </pre>
 * なお、フレームワーク制御ヘッダは、上記に挙げたハンドラ以外にも、
 * ログ出力やデータベースの共通項目といった部分においても使用されている。
 * 
 * 電文中のフレームワーク制御ヘッダ部を解析して本クラスを生成する責務は
 * FwHeaderDefinitionインタフェースの各実装クラスが持つ。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class FwHeader extends HashMap<String, Object> {
    // --------------------------------------------------------- constants
    /** リクエストIDのキー名 */
    private static final String REQUEST_ID_KEY_NAME  = "requestId";
    
    /** ユーザIDのキー名 */
    private static final String USER_KEY_NAME        = "userId";
    
    /** 再送要求フラグのキー名 */
    private static final String RESEND_FLAG_KEY_NAME = "resendFlag";
    
    /** 処理結果コードのキー名 */
    private static final String STATUS_CODE_KEY_NAME = "statusCode";
    
    // ----------------------------------------------------- headers
    /**
     * リクエストIDヘッダの値を返す。
     * @return リクエストIDヘッダの値
     */
    public String getRequestId() {
        return (String) get(REQUEST_ID_KEY_NAME);
    }
    
    /**
     * リクエストIDヘッダの値を設定する。
     * @param requestId 設定する値
     * @return このオブジェクト自体
     */
    public FwHeader setRequestId(String requestId) {
        nullSafeSet(REQUEST_ID_KEY_NAME, requestId);
        return this;
    }
    
    /**
     * ユーザIDヘッダの値を返す。
     * @return ユーザIDヘッダの値
     */
    public String getUserId() {
        return (String) get(USER_KEY_NAME);
    }
    
    /**
     * ユーザIDヘッダ項目が電文レイアウトに定義されているかどうか。
     * @return ユーザIDヘッダ項目が電文レイアウトに定義されていればtrue
     */
    public boolean hasUserId() {
        return containsKey(USER_KEY_NAME);
    }
    
    /**
     * ユーザIDヘッダの値を設定する。
     * @param userId 設定する値
     * @return このオブジェクト自体
     */
    public FwHeader setUserId(String userId) {
        nullSafeSet(USER_KEY_NAME, userId);
        return this;
    }
    
    /**
     * この電文が再送電文であるかどうかを返す。
     * この電文が初回電文、もしくは、そもそも再送要求をサポートしていない場合
     * falseを返す。
     * @return この電文が再送電文であるかどうか。
     */
    public boolean isResendingRequest() {
        Object flag = get(RESEND_FLAG_KEY_NAME);
        if (flag == null) {
            return false;
        }
        return !flag.toString().equals(resendFlagOffValue.toString());
    }
    
    /**
     * この電文が再送要求をサポートしているかどうかを返す。
     * @return この電文が再送要求をサポートしているかどうか。
     */
    public boolean isResendingSupported() {
        Object flag = get(RESEND_FLAG_KEY_NAME);
        return (flag != null && flag.toString().length() != 0);
    }
    
    /**
     * 再送電文フラグの値を設定する。
     * @param flag 設定する値
     * @return このオブジェクト自体
     */
    public FwHeader setResendFlag(Object flag) {
        put(RESEND_FLAG_KEY_NAME, flag);
        return this;
    }
    
    /**
     * 要求電文に対する処理結果を表すコード値を返す。
     * @return ステータスコード
     */
    public String getStatusCode() {
        return (String) get(STATUS_CODE_KEY_NAME);
    }
    
    /**
     * 要求電文に対する処理結果を表すコード値を返す。
     * @param flag 設定する値
     * @return このオブジェクト自体
     */
    public FwHeader setStatusCode(String flag) {
        nullSafeSet(STATUS_CODE_KEY_NAME, flag);
        return this;
    }
    
    /**
     * 初回送信電文に設定される再送フラグの値を設定する。
     * @param offValue 初回送信電文に設定される再送フラグの値
     * @return このオブジェクト自体
     */
    public FwHeader setResendFlagOffValue(Object offValue) {
        resendFlagOffValue = offValue;
        return this;
    }
    
    /** 初回送信電文に設定される再送フラグの値 */
    private Object resendFlagOffValue = 0;
    
    // ------------------------------------------------------------ helper
    /**
     * 指定されたキーに対する値を設定する。
     * @param key   キー文字列
     * @param value 設定する値
     */
    private void nullSafeSet(String key, Object value) {
        if (value == null
            || value instanceof String && value.toString().length() == 0) {
            throw new MessagingException(
                "Could not read the fw header of " + key
            );
        }
        put(key, value);
    }
}
