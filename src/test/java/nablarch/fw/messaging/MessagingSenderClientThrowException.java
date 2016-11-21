package nablarch.fw.messaging;

/**
 * {@link MessageSender}で例外が発生した場合をテストするためのクラス。
 * @author Masaya Seko
 */
public class MessagingSenderClientThrowException implements
        MessageSenderClient {

    @Override
    public SyncMessage sendSync(MessageSenderSettings settings,
            SyncMessage requestMessage) {
        throw new RuntimeException();
    }

}
