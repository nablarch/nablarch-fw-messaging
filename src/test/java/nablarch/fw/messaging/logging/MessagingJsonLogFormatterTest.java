package nablarch.fw.messaging.logging;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.log.LogTestSupport;
import nablarch.core.text.json.BasicJsonSerializationManager;
import nablarch.core.text.json.JsonSerializationManager;
import nablarch.core.text.json.JsonSerializationSettings;
import nablarch.core.text.json.JsonSerializer;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.test.support.SystemPropertyCleaner;
import nablarch.test.support.tool.Hereis;
import org.junit.Rule;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

/**
 * {@link MessagingJsonLogFormatter}のテストクラス。
 *
 * @author Shuji Kitamura
 */
public class MessagingJsonLogFormatterTest extends LogTestSupport {
    @Rule
    public SystemPropertyCleaner systemPropertyCleaner = new SystemPropertyCleaner();

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog(SendingMessage)}メソッドのテスト。
     * <p>
     * targets を指定しないデフォルトの場合。
     * </p>
     */
    @Test
    public void testGetSentMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getSentMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "SENT MESSAGE")),
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withJsonPath("$", hasEntry("replyTo", "replyToTest")),
                withJsonPath("$", hasEntry("timeToLive", 0)),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog(SendingMessage)}メソッドのテスト。
     * <p>
     * targets を指定した場合。
     * </p>
     */
    @Test
    public void testGetSentMessageLogWithTargets() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "label,messageId");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getSentMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("label", "SENT MESSAGE")),
            withJsonPath("$", hasEntry("messageId", "messagingIdTest"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog(SendingMessage)}メソッドのテスト。
     * <p>
     * labelの値を指定した場合。
     * </p>
     */
    @Test
    public void testGetSentMessageLogWithLabelValue() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "label");
        System.setProperty("messagingLogFormatter.sentMessageLabel", "sent-message");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage();

        String log = formatter.getSentMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "sent-message"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog(SendingMessage)} のマスク処理のテスト。
     */
    @Test
    public void testGetSentMessageLogForMasking() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getSentMessageLog(message);
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$", hasEntry("messageBody", "01***56789")),
            withJsonPath("$", hasEntry("messageBodyHex", "30312A2A2A3536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog(SendingMessage)} のマスク処理のテスト（マスク文字差し替え）。
     */
    @Test
    public void testGetSentMessageLogForMaskingWithCustomMaskingChar() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        System.setProperty("messagingLogFormatter.maskingChar", "#");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getSentMessageLog(message);
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01###56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312323233536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)}メソッドのテスト。
     * <p>
     * targets を指定しないデフォルトの場合。
     * </p>
     */
    @Test
    public void testGetReceivedMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "RECEIVED MESSAGE")),
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withoutJsonPath("$.replyTo"),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)}メソッドのテスト。
     * <p>
     * targets を指定した場合。
     * </p>
     */
    @Test
    public void testGetReceivedMessageLogWithTargets() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "label,destination");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("label", "RECEIVED MESSAGE")),
            withJsonPath("$", hasEntry("destination", "destinationTest"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testGetReceivedMessageLogWithLabelValue() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "label");
        System.setProperty("messagingLogFormatter.receivedMessageLabel", "received-message");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "received-message"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)} のマスク処理のテスト。
     */
    @Test
    public void testGetReceivedMessageLogForMasking() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01***56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312A2A2A3536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)} のマスク処理のテスト（マスク文字差し替え）。
     */
    @Test
    public void testGetReceivedMessageLogForMaskingWithCustomMaskingChar() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        System.setProperty("messagingLogFormatter.maskingChar", "#");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01###56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312323233536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)}メソッドのテスト。
     * <p>
     * targets を指定しないデフォルトの場合。
     * </p>
     */
    @Test
    public void testGetHttpSendingMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "HTTP SENT MESSAGE")),
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withJsonPath("$.messageHeader", hasEntry("MessageId", "messagingIdTest")),
                withJsonPath("$.messageHeader", hasEntry("Destination", "destinationTest")),
                withJsonPath("$.messageHeader", hasEntry("CorrelationId", "correlationIdTest")),
                withJsonPath("$.messageHeader", hasEntry("ReplyTo", "replyToTest")),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)}メソッドのテスト。
     * <p>
     * targets を指定した場合。
     * </p>
     */
    @Test
    public void testGetHttpSendingMessageLogWithTargets() {
        System.setProperty("messagingLogFormatter.httpSentMessageTargets", "label,threadName");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("label", "HTTP SENT MESSAGE")),
            withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName()))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testGetHttpSendingMessageLogWithLabelValue() {
        System.setProperty("messagingLogFormatter.httpSentMessageTargets", "label");
        System.setProperty("messagingLogFormatter.httpSentMessageLabel", "http-sent-message");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage();

        String log = formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "http-sent-message"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)} のマスク処理のテスト。
     */
    @Test
    public void testGetHttpSentMessageLogForMasking() {
        System.setProperty("messagingLogFormatter.httpSentMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01***56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312A2A2A3536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)} のマスク処理のテスト（マスク文字差し替え）。
     */
    @Test
    public void testGetHttpSentMessageLogForMaskingWithCustomMaskingChar() {
        System.setProperty("messagingLogFormatter.httpSentMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        System.setProperty("messagingLogFormatter.maskingChar", "#");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01###56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312323233536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog}メソッドのテスト。
     * <p>
     * targets を指定しないデフォルトの場合。
     * </p>
     */
    @Test
    public void testGetHttpReceivedMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("label", "HTTP RECEIVED MESSAGE")),
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withJsonPath("$.messageHeader", hasEntry("MessageId", "messagingIdTest")),
                withJsonPath("$.messageHeader", hasEntry("Destination", "destinationTest")),
                withJsonPath("$.messageHeader", hasEntry("CorrelationId", "correlationIdTest")),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog}メソッドのテスト。
     * <p>
     * targets を指定した場合。
     * </p>
     */
    @Test
    public void testGetHttpReceivedMessageLogWithTargets() {
        System.setProperty("messagingLogFormatter.httpReceivedMessageTargets", "label,correlationId");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
            withJsonPath("$.*", hasSize(2)),
            withJsonPath("$", hasEntry("label", "HTTP RECEIVED MESSAGE")),
            withJsonPath("$", hasEntry("correlationId", "correlationIdTest"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog}メソッドのテスト。
     * <p>
     * label の値を指定した場合。
     * </p>
     */
    @Test
    public void testGetHttpReceivedMessageLogWithLabelValue() {
        System.setProperty("messagingLogFormatter.httpReceivedMessageTargets", "label");
        System.setProperty("messagingLogFormatter.httpReceivedMessageLabel", "http-received-message");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$.*", hasSize(1)),
                withJsonPath("$", hasEntry("label", "http-received-message"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog(ReceivedMessage, Charset)} のマスク処理のテスト。
     */
    @Test
    public void testGetHttpReceivedMessageLogForMasking() {
        System.setProperty("messagingLogFormatter.httpReceivedMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01***56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312A2A2A3536373839"))
        )));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog(ReceivedMessage, Charset)} のマスク処理のテスト（マスク文字差し替え）。
     */
    @Test
    public void testGetHttpReceivedMessageLogForMaskingWithCustomMaskingChar() {
        System.setProperty("messagingLogFormatter.httpReceivedMessageTargets", "messageBody,messageBodyHex");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");
        System.setProperty("messagingLogFormatter.maskingChar", "#");
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBody", "01###56789")),
                withJsonPath("$", hasEntry("messageBodyHex", "30312323233536373839"))
        )));
    }

    /**
     * 不正なターゲットのテスト。
     */
    @Test
    public void testIllegalTargets() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "threadName,messageId,dummy,messageBody");

        Exception e = assertThrows(IllegalArgumentException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                MessagingLogFormatter formatter = new MessagingJsonLogFormatter();
            }
        });

        assertThat(e.getMessage(), is("[dummy] is unknown target. property name = [messagingLogFormatter.sentMessageTargets]"));
    }

    /**
     * {@link MessagingJsonLogFormatter#getReceivedMessageLog(ReceivedMessage)}メソッドのテスト。
     * <p>
     * メッセージボディが無い場合。
     * </p>
     */
    @Test
    public void testNoMessageBody() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "messageBodyLength,messageBodyHex,messageBody");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");

        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("messageBodyLength", 0)),
                withJsonPath("$", hasEntry("messageBodyHex", "")),
                withJsonPath("$", hasEntry("messageBody", "")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog(SendingMessage, Charset)}メソッドのテスト。
     * <p>
     * XML データの場合。
     * </p>
     */
    @Test
    public void testGetHttpSendingMessageLog2() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        SendingMessage message = createXmlSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");
                }});

        String log = formatter.getHttpSentMessageLog(message, null);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring("$JSON$".length()), isJson(allOf(
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingId")),
                withJsonPath("$.messageHeader", hasEntry("MessageId", "messagingId")),
                withJsonPath("$", hasEntry("messageBody", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><data>0123456789</data></request>")))));
    }

    /**
     * {@link nablarch.core.text.json.JsonSerializationManager}の実装を変更できることをテスト。
     */
    @Test
    public void testChangeJsonSerializationManager() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "threadName,messageId,messageBody");

        MessagingLogFormatter formatter = new MessagingJsonLogFormatter() {
            @Override
            protected JsonSerializationManager createSerializationManager(JsonSerializationSettings settings) {
                assertThat(settings.getProp("sentMessageTargets"), is("threadName,messageId,messageBody"));
                return new MockJsonSerializationManager();
            }
        };

        final SendingMessage sendingMessage = createSendingMessage();
        final ReceivedMessage receivedMessage = createReceivedMessage("body", "UTF-8");

        assertThat(formatter.getSentMessageLog(sendingMessage),
                is("$JSON$mock serialization"));
        assertThat(formatter.getReceivedMessageLog(receivedMessage),
                is("$JSON$mock serialization"));
        assertThat(formatter.getHttpSentMessageLog(sendingMessage, getCharsetFromMessage(sendingMessage)),
                is("$JSON$mock serialization"));
        assertThat(formatter.getHttpReceivedMessageLog(receivedMessage, getCharsetFromMessage(receivedMessage)),
                is("$JSON$mock serialization"));
    }

    /**
     * {@link nablarch.core.text.json.JsonSerializationManager}のモッククラス。
     */
    private static class MockJsonSerializationManager extends BasicJsonSerializationManager {
        @Override
        public JsonSerializer getSerializer(Object value) {
            return new JsonSerializer() {

                @Override
                public void serialize(Writer writer, Object value) throws IOException {
                    writer.write("mock serialization");
                }

                @Override
                public void initialize(JsonSerializationSettings settings) {
                }

                @Override
                public boolean isTarget(Class<?> valueClass) {
                    return false;
                }
            };
        }
    }

    /**
     * メッセージから文字セットを取得する
     * @param message メッセージ
     * @return 文字セット
     */
    private Charset getCharsetFromMessage(InterSystemMessage<?> message) {
        DataRecordFormatter formatter = message.getFormatter();
        Charset charset = Charset.defaultCharset();
        if (formatter instanceof DataRecordFormatterSupport) {
            charset = ((DataRecordFormatterSupport)formatter).getDefaultEncoding();
        }
        return charset;
    }

    private SendingMessage createSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
         file-type:        "Fixed"
         text-encoding:    "utf8"
         record-length:    10
         [Data]
         1 data  X(10)    # データ
         *****************************/

        FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:./")
                .addFileExtensions("format", ".fmt");

        DataRecordFormatter formatter = FormatterFactory
                .getInstance()
                .setCacheLayoutFileDefinition(false)
                .createFormatter(formatFile);
        return new SendingMessage()
                .setMessageId("messagingIdTest")
                .setDestination("destinationTest")
                .setCorrelationId("correlationIdTest")
                .setReplyTo("replyToTest")
                .setFormatter(formatter);
    }

    private ReceivedMessage createReceivedMessage(String body, String charset) {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
         file-type:        "Fixed"
         text-encoding:    "utf8"
         record-length:    10
         [Data]
         1 data  X(10)    # データ
         *****************************/

        FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:./")
                .addFileExtensions("format", ".fmt");

        DataRecordFormatter formatter = FormatterFactory
                .getInstance()
                .setCacheLayoutFileDefinition(false)
                .createFormatter(formatFile);

        return new ReceivedMessage(body.getBytes(Charset.forName(charset)))
                .setMessageId("messagingIdTest")
                .setDestination("destinationTest")
                .setCorrelationId("correlationIdTest")
                .setFormatter(formatter);
    }

    public SendingMessage createXmlSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
         file-type:        "XML"
         text-encoding:    "UTF-8"
         [request]
         1 data [0..1]  X  # データ
         *****************************/

        FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:./")
                .addFileExtensions("format", ".fmt");

        DataRecordFormatter formatter = FormatterFactory
                .getInstance()
                .setCacheLayoutFileDefinition(false)
                .createFormatter(formatFile);
        return new SendingMessage()
                .setMessageId("messagingId")
                .setFormatter(formatter);
    }
}
