package nablarch.fw.messaging.reader;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.MessageReadError;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import nablarch.test.support.tool.Hereis;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * メッセージリーダのテスト
 * @author Iwauo Tajima
 */
public class MessagingReaderTest {
    
    private static TestEmbeddedMessagingProvider provider;
    
    private MessageReader reader;
    private ExecutionContext context;
    
    @BeforeClass
    public static void startMessagingServer() {
        provider = new TestEmbeddedMessagingProvider()
                      .setQueueNames(Arrays.asList("LOCAL.QUEUE", "LOCAL.REPLY"));
    }
    
    @AfterClass
    public static void stopMessagingServer() {
        TestEmbeddedMessagingProvider.stopServer();
    }
    
    public void setupReader() {
        reader = new MessageReader()
                    .setReadTimeout(500)
                    .setReceiveQueueName("LOCAL.QUEUE");
        context = new ExecutionContext();
        MessagingContext.attach(provider.createContext());
    }

    @Before
    public void setUp() {
        MessagingContext.detach();
    }

    /**
     * メッセージ受信処理のテスト
     */
    @Test public void testRead() {
        setupReader();
        
        // 送信してみる。
        String sentMessageId = provider.createContext().send(
            new SendingMessage().setDestination("LOCAL.QUEUE")
        );

        // よめる。
        ReceivedMessage received = reader.read(context);
        assertNotNull(received);
        assertEquals(sentMessageId, received.getMessageId());
        
        // もう一度読むと読めない。-> タイムアウト(500msec)でnullが返る。
        received = reader.read(context);
        assertNull(received);
        
        
        // もう一度送信
        sentMessageId = provider.createContext().send(
            new SendingMessage().setDestination("LOCAL.QUEUE")
        );
        
        // やっぱり読める。
        received = reader.read(context);
        assertNotNull(received);
        assertEquals(sentMessageId, received.getMessageId());

        // キューが空でも、明示的にclose()するまではとじない。
        assertTrue(reader.hasNext(context));
        
        // 閉じる。
        reader.close(context);
        
        // 閉じた。
        assertFalse(reader.hasNext(context));
        
        // 閉じてるのでnullが返る。
        received = reader.read(context);
        assertNull(received);
    }
    
    /**
     * 読み出したメッセージに対してデフォルトフォーマッターを設定する機能のテスト
     */
    @Test public void settingDefaultFormatterToMessage() throws Exception {
        setupReader();
        File formatFile = Hereis.file("./format.fmt");
        /**************************************************
        file-type:        "Fixed"
        text-encoding:    "sjis"
        record-length:    210
        record-separator: "\n"
        
        [Classifier]
        1  recordType X(1)    # レコードタイプ判定
        
        [Book]
        recordType = "1"
        1   recordType  X(1)  "1" # トレーラレコード判定
        2  ?filler      X(9)      # 未使用
        11  title       X(50)     # 書名
        61  publisher   X(50)     # 出版社
        111 authors     X(100)    # 著者
        
        [Summary]
        recordType = "9"
        1   recordType  X(1)   "9" # トレーラレコード判定
        2  ?filler      X(9)       # 未使用
        11  bookCount   Z(5)       # 処理対象レコード数
        16 ?reserved    X(195)     # 予備領域
        ****************************************************/
        
        FilePathSetting.getInstance()
                       .addBasePathSetting("format", "file:./")
                       .addFileExtensions("format", "fmt");
        
        
        // 送信してみる。
        provider.createContext().send(
            new SendingMessage().setDestination("LOCAL.QUEUE")
        );
        
        // よめる。
        ReceivedMessage received = reader.read(context);
        
        // デフォルトではフォーマッターに何も指定しない。
        assertNull(received.getFormatter());
        
        
        // 存在しないフォーマッターを指定してみる。
        reader.setFormatFileDirName("format")
              .setFormatFileName("unknown");
        
        // 送信してみる。
        provider.createContext().send(
            new SendingMessage().setDestination("LOCAL.QUEUE")
                                .setReplyTo("LOCAL.REPLY")
        );
        
        try {
            reader.read(context);
            fail();
        } catch (MessageReadError e) {
            Throwable cause = e.getCause();
            assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
            assertThat(cause.getMessage(), is(allOf(
                    containsString("invalid layout file path was specified."),
                    containsString("file path=["),
                    containsString(new File("./unknown.fmt").getAbsolutePath())
                    )));
        }
        
        // 存在するフォーマッターを指定してみる。
        reader.setFormatFileDirName("format")
              .setFormatFileName("format");
        
        // 送信してみる。
        provider.createContext().send(
            new SendingMessage().setDestination("LOCAL.QUEUE")
                                .setReplyTo("LOCAL.REPLY")
        );
        
        received = reader.read(context);
        assertTrue(received.getFormatter() instanceof DataRecordFormatter);
    }
}
