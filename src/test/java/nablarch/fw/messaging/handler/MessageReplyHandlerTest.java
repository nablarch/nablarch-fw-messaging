package nablarch.fw.messaging.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.DataReader;
import nablarch.fw.DataReader.NoMoreRecord;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Result;
import nablarch.fw.handler.DataReadHandler;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.tool.Hereis;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link MessageReplyHandlerTest}のテスト。
 * @author Iwauo Tajima
 */
public class MessageReplyHandlerTest {

    private DataRecordFormatter formatter;
    private ExecutionContext    executionContext;
    private RequestMessage      requestMessage;
    private SendingMessage      sentMessage;
    
    @Before public void setup() throws Exception {
        formatter        = createFormatter();
        requestMessage   = createRequestMessage();
        executionContext = createExecutionContext();
        sentMessage = null;
        setupMessagingContext();
    }
    
    /**
     * エラー発生時のメッセージ応答処理のテスト。
     */
    @Test public void replyingErrorResponseWhenAnErrorOccurs()
    throws Exception {
        
        // ---- 後続ハンドラからErrorオブジェクトが送出されてくる場合---- //
        executionContext.addHandler(
            new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage
                handle(RequestMessage data, ExecutionContext context) {
                    throw new OutOfMemoryError("test"); // Errorを送出
                }
            }
        );
        try {
            executionContext.handleNext(null);
            fail();
        } catch (Error e) {
            // 元例外が再送出される。
            assertTrue(e instanceof OutOfMemoryError);
            assertEquals("test", e.getMessage());
        }
        
        // 自動的に構成したエラー応答を送信する。
        assertTrue(sentMessage instanceof ResponseMessage);
        assertEquals("testReplyTo", sentMessage.getDestination());
        assertEquals("testMessageId", sentMessage.getCorrelationId());
        ResponseMessage sentResponse = (ResponseMessage) sentMessage;
        assertEquals("500", sentResponse.getFwHeader().getStatusCode());
        assertEquals("testId", sentResponse.getFwHeader().getRequestId());
        assertEquals("testUser", sentResponse.getFwHeader().getUserId());
        
        
     // ---- 後続ハンドラがDataReadHandle.NoMoreRecordをリターンする場合　---- //
        executionContext = createExecutionContext();
        sentMessage = null;
        
        executionContext.addHandler(
            new Handler<RequestMessage, Result>() {
                public Result
                handle(RequestMessage data, ExecutionContext context) {
                    return new NoMoreRecord();
                }
            }
        );
        Result result = executionContext.handleNext(null);
        assertTrue(result instanceof NoMoreRecord);
        assertNull(sentMessage); // このばあいは応答を返さない。
    }

    /**
     * エラーレスポンス作成時にRuntimeExceptionが発生した場合のテスト。
     * 元例外をロストしないこと。
     */
    @Test
    public void occursRuntimeExceptionWhileReplyingErrorResponse() throws Exception {

        requestMessage   = createRequestMessageWithErrorReply(new RuntimeException("occurs runtime exception while replying error response"));

        // ---- 後続ハンドラからErrorオブジェクトが送出されてくる場合---- //
        executionContext.addHandler(
            new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage
                handle(RequestMessage data, ExecutionContext context) {
                    throw new OutOfMemoryError("test"); // Errorを送出
                }
            }
        );
        OnMemoryLogWriter.getMessages("writer.appLog").clear();
        try {
            executionContext.handleNext(null);
            fail();
        } catch (Throwable th) {
            // 元例外が再送出される。
            assertTrue(th instanceof OutOfMemoryError);
            assertEquals("test", th.getMessage());
        }
        String warnMsg = OnMemoryLogWriter.getMessages("writer.appLog").get(1); // 0番目はDataReadHandler.handle(Object, ExecutionContext)がはくWARNログ
        assertTrue(warnMsg.contains("WARN"));
        assertTrue(warnMsg.contains("an error occurred while replying error response. "));
        assertTrue(warnMsg.contains("Stack Trace Information :"));
        assertTrue(warnMsg.contains("java.lang.RuntimeException: occurs runtime exception while replying error response"));
    }

    /**
     * エラーレスポンス作成時にErrorが発生した場合のテスト。
     * 元例外をロストしないこと。
     */
    @Test
    public void occursErrorWhileReplyingErrorResponse() throws Exception {

        requestMessage   = createRequestMessageWithErrorReply(new UnknownError("occurs error while replying error response"));

        // ---- 後続ハンドラからErrorオブジェクトが送出されてくる場合---- //
        executionContext.addHandler(
            new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage
                handle(RequestMessage data, ExecutionContext context) {
                    throw new OutOfMemoryError("test"); // Errorを送出
                }
            }
        );
        OnMemoryLogWriter.getMessages("writer.appLog").clear();
        try {
            executionContext.handleNext(null);
            fail();
        } catch (Throwable th) {
            // 元例外が再送出される。
            assertTrue(th instanceof OutOfMemoryError);
            assertEquals("test", th.getMessage());
        }
        String warnMsg = OnMemoryLogWriter.getMessages("writer.appLog").get(1); // 0番目はDataReadHandler.handle(Object, ExecutionContext)がはくWARNログ
        assertTrue(warnMsg.contains("an error occurred while replying error response. "));
        assertTrue(warnMsg.contains("Stack Trace Information :"));
        assertTrue(warnMsg.contains("java.lang.UnknownError: occurs error while replying error response"));
    }

    /**
     * 応答電文送信時に例外が送出された場合の挙動に対するテスト。
     */
    @Test public void handlingErrorWhileSendingAReply() throws Exception {
        
        // ----- 送信時に実行時例外が送出されるケース ------- //
        MessagingContext.attach(new MessagingContext() {
            @Override
            public String sendMessage(SendingMessage message) {
                throw new MessagingException("test");
            }
            @Override
            public ReceivedMessage receiveMessage(String receiveQueue,
                                                  String messageId,
                                                  long   timeout) {
                return new ReceivedMessage(new byte[]{});
            }
            @Override
            public void close() {
                // nop
            }
        });
        executionContext.addHandler(
            new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage
                handle(RequestMessage request, ExecutionContext context) {
                    return request.reply();
                }
            }
        );
        
        try {
            executionContext.handleNext(null);
            fail();
            
        } catch (Exception e) {
            // 元例外が再送出される。
            assertTrue(e instanceof MessagingException);
            assertEquals("test", e.getMessage());
        }
        assertNull(sentMessage); // 送信には失敗する。
        
        
        // ----- 送信時にエラーオブジェクトが送出されるケース ------- //
        executionContext = createExecutionContext();
        
        MessagingContext.attach(new MessagingContext() {
            @Override
            public String sendMessage(SendingMessage message) {
                throw new OutOfMemoryError("test");
            }
            @Override
            public ReceivedMessage receiveMessage(String receiveQueue,
                                                  String messageId,
                                                  long   timeout) {
                return new ReceivedMessage(new byte[]{});
            }
            @Override
            public void close() {
                // nop
            }
        });
        executionContext.addHandler(
            new Handler<RequestMessage, ResponseMessage>() {
                public ResponseMessage
                handle(RequestMessage request, ExecutionContext context) {
                    return request.reply();
                }
            }
        );
        
        try {
            executionContext.handleNext(null);
            fail();
            
        } catch (Error e) {
            // 元例外が再送出される。
            assertTrue(e instanceof OutOfMemoryError);
            assertEquals("test", e.getMessage());
        }
        assertNull(sentMessage); // 送信には失敗する。
    }
        
    public void setupMessagingContext() {
        MessagingContext.attach(new MessagingContext() {
            @Override
            public String sendMessage(SendingMessage message) {
                sentMessage = message;
                return "testMessageId";
            }
            @Override
            public ReceivedMessage receiveMessage(String receiveQueue,
                                                  String messageId,
                                                  long   timeout) {
                return new ReceivedMessage(new byte[]{});
            }
            @Override
            public void close() {
                // nop
            }
        });
    }
    
    public ExecutionContext createExecutionContext() {
        return new ExecutionContext()
                  .addHandler(new MessageReplyHandler())
                  .addHandler(new DataReadHandler())
                  .setDataReader(new DataReader<RequestMessage>() {
                       public RequestMessage read(ExecutionContext ctx) {
                           return requestMessage;
                       }
                       public boolean hasNext(ExecutionContext ctx) {
                           return true;
                       }
                       public void close(ExecutionContext ctx) {
                           // nop
                       }
                   });
    }
    
    public DataRecordFormatter createFormatter() {
        FilePathSetting.getInstance()
                       .addBasePathSetting("format", "file:./")
                       .addFileExtensions("format", ".fmt");
        
        File formatFile = Hereis.file("./test.fmt");
        /*****************************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    210
        record-separator: "\n"
        
        [Book]
        1  ?filler      X(10)     # 未使用
        11  title       X(50)     # 書名
        61  publisher   X(50)     # 出版社
        111 authors     X(100)    # 著者
        ******************************************************/
        return FormatterFactory.getInstance()
                               .createFormatter(formatFile);
    }
    
    public RequestMessage createRequestMessage() throws Exception {
        FwHeader header = new FwHeader()
                         .setRequestId("testId")
                         .setUserId("testUser");
            
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        formatter.setOutputStream(dataStream)
                 .initialize()
                 .writeRecord("Book", new HashMap(){{
                      put("title", "Writin Effective UseCases");
                      put("publisher", "Addison-Wesley");
                      put("authors", "Alistair Cockburn");
                  }});
        RequestMessage request = new RequestMessage(
            header,
            new ReceivedMessage(dataStream.toByteArray())
               .setFormatter(formatter)
               .setReplyTo("testReplyTo")
               .setMessageId("testMessageId")
        );
        request.readRecord();
        return request;
    }

    private RequestMessage createRequestMessageWithErrorReply(final Throwable th) throws Exception {
        FwHeader header = new FwHeader()
                         .setRequestId("testId")
                         .setUserId("testUser");
            
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        formatter.setOutputStream(dataStream)
                 .initialize()
                 .writeRecord("Book", new HashMap(){{
                      put("title", "Writin Effective UseCases");
                      put("publisher", "Addison-Wesley");
                      put("authors", "Alistair Cockburn");
                  }});
        RequestMessage request = new RequestMessage(
            header,
            new ReceivedMessage(dataStream.toByteArray())
               .setFormatter(formatter)
               .setReplyTo("testReplyTo")
               .setMessageId("testMessageId")
        ) {
            @Override
            public ResponseMessage reply() throws UnsupportedOperationException {
                if (th instanceof Error) {
                    throw (Error) th;
                } else {
                    throw (RuntimeException) th;
                }
            }
        };
        request.readRecord();
        return request;
    }

}
