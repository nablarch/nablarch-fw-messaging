package nablarch.fw.messaging;

import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;

public class CustomRealTimeMessagingClient extends HttpMessagingClient {

    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public SyncMessage sendSync(MessageSenderSettings settings, SyncMessage requestMessage) {
        throw new UnsupportedOperationException();
    }

}
