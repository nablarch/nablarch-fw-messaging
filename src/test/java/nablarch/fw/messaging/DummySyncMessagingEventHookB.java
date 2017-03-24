package nablarch.fw.messaging;

public class DummySyncMessagingEventHookB implements SyncMessagingEventHook {

    @Override
    public void beforeSend(MessageSenderSettings settings,
            SyncMessage requestMessage) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public void afterSend(MessageSenderSettings settings,
            SyncMessage requestMessage, SyncMessage responseMessage) {
        // TODO 自動生成されたメソッド・スタブ

    }

    @Override
    public boolean onError(RuntimeException e, boolean hasNext,
            MessageSenderSettings settings, SyncMessage requestMessage,
            SyncMessage responseMessage) {
        // TODO 自動生成されたメソッド・スタブ
        return false;
    }
}
