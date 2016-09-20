package nablarch.fw.messaging;

import javax.net.ssl.SSLContext;

/**
 * SSL情報取得用クラス
 * 
 * @author Masaya Seko
 */
public interface HttpSSLContextSettings {
    /**
     * SSLContextを取得する。
     * @return SSLContext
     */
    SSLContext getSSLContext();
}
