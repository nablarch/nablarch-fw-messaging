package nablarch.fw.messaging;

import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MessagingContextTest {

    private MessagingProvider provider = new TestEmbeddedMessagingProvider();
    
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
}
