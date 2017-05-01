package nablarch.fw.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class MessagingContextTest {

    private MessagingProvider provider = new TestEmbeddedMessagingProvider();

    @Before
    public void setUp() throws Exception {
        OnMemoryLogWriter.clear();
    }

    @AfterClass public static void stopMessagingServer() {
        TestEmbeddedMessagingProvider.stopServer();
    }
    
    @Test public void atachingANewContextToCurrentThread() {
        MessagingContext context1 = provider.createContext();
        MessagingContext.attach(context1);
        
        assertSame(context1, MessagingContext.getInstance());
        
        MessagingContext.detach();
        MessagingContext.detach(); // 複数回よんでもよい。
        
        try {
            MessagingContext.getInstance();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            assertEquals(
                "there is no messaging context on the current thread.",
                e.getMessage()
            );
        }
    }
    
    @Test public void errorHandlingWhenAnInvalidArgumentWasAssigned() {
        MessagingContext context = provider.createContext();
        try {
            context.sendSync(new SendingMessage(), 0);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
            assertEquals("replyTo header must be set.", e.getMessage());
        }
        
    }

    @Test
    public void timeoutWithSendSync_outputTimeoutLog() throws Exception {
        final MessagingContext sut = new MessagingContext() {
            @Override
            public String sendMessage(final SendingMessage message) {
                return null;
            }

            @Override
            public ReceivedMessage receiveMessage(final String receiveQueue, final String messageId, final long timeout) {
                // タイムアウトをとするためにnullを返す
                return null;
            }

            @Override
            public void close() {
                // nop
            }
        };


        final SendingMessage message = new SendingMessage();
        message.setReplyTo("queue");
        message.setMessageId("timeout_test");
        sut.sendSync(message, 1);

        OnMemoryLogWriter.assertLogContains("writer.accessLog", "response timeout: could not receive a reply to the message below.");
        
    }
}
