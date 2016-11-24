package nablarch.fw.messaging.action;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.DataRecord;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.fw.messaging.MessageSenderSettings;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.MessagingProvider;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.core.messaging.EmbeddedMessagingProvider;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class SyncMessageSendTestUtil {

    /**
     * リクエストID:RM11AD0101に対する応答電文を作成し送信する。
     */
    @SuppressWarnings("serial")
    public static boolean createReceivedMessage(final HttpRequest request) {
        final MessageSenderSettings settings = new MessageSenderSettings("RM11AD0101");
        MessagingContext messagingContext = settings.getMessagingProvider().createContext();
        try {
            final ReceivedMessage receivedMessage = messagingContext.receiveSync(settings.getDestination(), 100L);
            if (receivedMessage == null) {
                return false;
            }
    
            final DataRecord header = receivedMessage.setFormatter(settings.getHeaderFormatter()).readRecord();
            final DataRecord data = receivedMessage.setFormatter(settings.getSendingDataFormatter()).readRecord();
            SendingMessage sendingMessage = new SendingMessage()
                    .setDestination(settings.getReplyTo())
                    .setCorrelationId(receivedMessage.getMessageId())
                    .setFormatter(settings.getHeaderFormatter())
                    .addRecord(header)
                    .setFormatter(settings.getReceivedDataFormatter())
                    .addRecord(new HashMap<String, Object>() {{
                        put("failureCode", request.getParam("failureCode")[0]);
                        put("userInfoId", Builder.join(new Object[] {
                                data.get("title"),
                                data.get("publisher"),
                                data.get("authors")
                        }, "_"));
                    }});
            messagingContext.send(sendingMessage);
            return true;
        } finally {
            FileUtil.closeQuietly(messagingContext);
        }
    }

    /**
     * 1秒間隔で10回{@link SyncMessageSendTestUtil#createReceivedMessage(HttpRequest)}メソッドを呼び出すRunnable。
     * メッセージ同期送信の正常動作時のテストに使用する。
     * 1回応答電文を作成した時点で終了する。
     */
    public static class CreateReceivedMessage implements Runnable {
        public void run() {
            ThreadContext.setRequestId("CREATE_RECEIVED_MESSAGE");
            attatcMessagingContext();
            try {
                HttpRequest request = new MockHttpRequest().setParam("failureCode", "1234567890");
                int count = 100;
                while (--count != 0) {
                    boolean received = createReceivedMessage(request);
                    if (received) {
                        break;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                MessagingContext.detach();
            }
        }
    }

    /**
     * メッセージが受信できなくなるまで{@link SyncMessageSendTestUtil#createReceivedMessage(HttpRequest)}メソッドを呼び出す。
     * タイムアウトのテスト後に受信キューを空にするために使用する。
     */
    public static void clearReceivedQueue() {
        attatcMessagingContext();
        try {
            HttpRequest request = new MockHttpRequest().setParam("failureCode", "1234567890");
            boolean received = true;
            while (received) {
                received = createReceivedMessage(request);
            }
        } finally {
            MessagingContext.detach();
        }
    }
    
    private static void attatcMessagingContext() {
        MessagingProvider provider = null;
        while (provider == null) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            provider = SystemRepository.get("messagingProvider");
        }
        try {
            EmbeddedMessagingProvider.waitUntilServerStarted();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MessagingContext.attach(provider.createContext());
    }
}
