package nablarch.fw.messaging.logging;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.log.LogTestSupport;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MessagingJsonLogFormatterTest extends LogTestSupport {

    @Before
    public void setup() {
        System.clearProperty("messagingLogFormatter.sentMessageTargets");
        System.clearProperty("messagingLogFormatter.receivedMessageTargets");
        System.clearProperty("messagingLogFormatter.maskingPatterns");
    }

    @After
    public void teardown() {
        System.clearProperty("messagingLogFormatter.sentMessageTargets");
        System.clearProperty("messagingLogFormatter.receivedMessageTargets");
        System.clearProperty("messagingLogFormatter.maskingPatterns");
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog}メソッドのテスト。
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
        assertThat(log.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withJsonPath("$", hasEntry("replyTo", "replyToTest")),
                withJsonPath("$", hasEntry("timeToLive", 0)),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog}メソッドのテスト。
     */
    @Test
    public void testGetReceivedMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingIdTest")),
                withJsonPath("$", hasEntry("destination", "destinationTest")),
                withJsonPath("$", hasEntry("correlationId", "correlationIdTest")),
                withoutJsonPath("$.replyTo"),
                withJsonPath("$", hasEntry("messageBody", "0123456789")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog}メソッドのテスト。
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
        assertThat(log.substring(6), isJson(allOf(
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
     * {@link MessagingJsonLogFormatter#getHttpReceivedMessageLog}メソッドのテスト。
     */
    @Test
    public void testGetHttpReceivedMessageLog() {
        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        String log = formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message));
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring(6), isJson(allOf(
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
     * 不正なターゲットのテスト。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTargets() {
        System.setProperty("messagingLogFormatter.sentMessageTargets", "threadName,messageId,dummy,messageBody");

        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog}メソッドのテスト。
     */
    @Test
    public void testGetReceivedMessageLogWithTargets() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "timeToLive,messageBody,, , messageBodyLength ,messageBodyHex,messageBody");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");

        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("0123456789?", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring(6), isJson(allOf(
                withoutJsonPath("$.timeToLive"),
                withJsonPath("$", hasEntry("messageBodyLength", 11)),
                withJsonPath("$", hasEntry("messageBodyHex", "30312A2A2A35363738393F")),
                withJsonPath("$", hasEntry("messageBody", "01***56789?")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getSentMessageLog}メソッドのテスト。
     */
    @Test
    public void testNoMessageBody() {
        System.setProperty("messagingLogFormatter.receivedMessageTargets", "messageBodyLength,messageBodyHex,messageBody");
        System.setProperty("messagingLogFormatter.maskingPatterns", "(234)");

        MessagingLogFormatter formatter = new MessagingJsonLogFormatter();

        ReceivedMessage message = createReceivedMessage("", "UTF-8");

        String log = formatter.getReceivedMessageLog(message);
        assertThat(log.startsWith("$JSON$"), is(true));
        assertThat(log.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("messageBodyLength", 0)),
                withJsonPath("$", hasEntry("messageBodyHex", "")),
                withJsonPath("$", hasEntry("messageBody", "")))));
    }

    /**
     * {@link MessagingJsonLogFormatter#getHttpSentMessageLog}メソッドのテスト。
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
        assertThat(log.substring(6), isJson(allOf(
                withJsonPath("$", hasEntry("threadName", Thread.currentThread().getName())),
                withJsonPath("$", hasEntry("messageId", "messagingId")),
                withJsonPath("$.messageHeader", hasEntry("MessageId", "messagingId")),
                withJsonPath("$", hasEntry("messageBody", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><request><data>0123456789</data></request>")))));
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
