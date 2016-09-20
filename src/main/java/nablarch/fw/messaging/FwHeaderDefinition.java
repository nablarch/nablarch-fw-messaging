package nablarch.fw.messaging;

/**
 * 送受信電文中のフレームワーク制御ヘッダ項目に対する読み書きを行うモジュールが
 * 実装するインターフェース。
 * 具体的に電文中のどの部分をフレームワーク制御ヘッダの各項目に対応させるかについては、
 * 各具象クラスごとに異なる。
 */
public interface FwHeaderDefinition {
    /**
     * 受信電文中のフレームワーク制御ヘッダ部を読み込み、
     * RequestMessageオブジェクトを生成する。
     * @param message 受信電文オブジェクト
     * @return 要求電文オブジェクト
     */
    RequestMessage readFwHeaderFrom(ReceivedMessage message);
    
    /**
     * 応答電文オブジェクトに設定されたフレームワーク制御ヘッダの内容を
     * 送信電文に反映する。
     * @param message 応答電文オブジェクト
     * @param header  フレームワーク制御ヘッダー
     */
    void writeFwHeaderTo(SendingMessage message, FwHeader header);
}
