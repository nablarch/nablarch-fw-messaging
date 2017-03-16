package nablarch.fw.messaging;

public class StubSyncMessagingEventHookB implements SyncMessagingEventHook {

    /**beforeSendの呼び出し回数*/
    public static int callBeforeSendCount = 0;
    /**afterSendの呼び出し回数*/
    public static int callAfterSendCount = 0;
    /**onErrorの呼び出し回数*/
    public static int callOnErrorCount = 0;
    
    public static void restCallCount(){
        callBeforeSendCount = 0;
        callAfterSendCount = 0;
        callOnErrorCount = 0;
    }
    
    @Override
    public void beforeSend(MessageSenderSettings settings,
            SyncMessage requestMessage) {
        callBeforeSendCount++;
    }

    @Override
    public void afterSend(MessageSenderSettings settings,
            SyncMessage requestMessage, SyncMessage responseMessage) {
        callAfterSendCount++;
    }

    @Override
    public boolean onError(RuntimeException e, boolean hasNext,
            MessageSenderSettings settings, SyncMessage requestMessage,
            SyncMessage responseMessage) {
        callOnErrorCount++;
        return true;
    }
}
