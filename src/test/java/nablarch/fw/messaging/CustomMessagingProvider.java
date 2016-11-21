package nablarch.fw.messaging;

import nablarch.fw.messaging.provider.MessagingExceptionFactory;

public class CustomMessagingProvider implements MessagingProvider {

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public MessagingContext createContext() {
        // テストクラス側でアタッチしておくこと。
        return MessagingContext.getInstance();
    }

    public MessagingProvider setDefaultResponseTimeout(long timeout) {
        throw new UnsupportedOperationException();
    }

    public MessagingProvider setDefaultTimeToLive(long timeToLive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return name;
    }

    public MessagingProvider setMessagingExceptionFactory(
            MessagingExceptionFactory messagingExceptionFactory) {
        return this;
    }
}
