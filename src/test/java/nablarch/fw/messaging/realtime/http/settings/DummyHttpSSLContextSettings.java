package nablarch.fw.messaging.realtime.http.settings;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import nablarch.fw.messaging.HttpSSLContextSettings;

public class DummyHttpSSLContextSettings implements HttpSSLContextSettings {

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public SSLContext getSSLContext() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException();
        }
        return sslContext;
    }


    @Override
    public String toString() {
        return name;
    }
}
