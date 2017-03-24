package nablarch.fw.messaging;

import java.util.HashMap;

/**
 * MessageClientのモック。
 */
public class MockMessageSenderClient implements MessageSenderClient {

    public static final String CUSTOM_KEY = "CUSTOM_KEY";

    public static final String CUSTOM_VALUE = "CUSTOM_VALUE";

    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public SyncMessage sendSync(MessageSenderSettings settings, SyncMessage requestMessage) {
        SyncMessage ret = new SyncMessage(requestMessage.getRequestId());
        HashMap<String, Object> header = new HashMap<String, Object>();
        header.put(CUSTOM_KEY, CUSTOM_VALUE);
        ret.setHeaderRecord(header);
        return ret;
    }
}
