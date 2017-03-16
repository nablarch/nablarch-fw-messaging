package nablarch.fw.messaging.action.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.log.LogItem;
import nablarch.core.log.LogTestSupport;
import nablarch.core.log.LogUtil;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.InterSystemMessage;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.logging.MessagingLogFormatter;
import nablarch.test.support.tool.Hereis;

import org.junit.Test;

/**
 * {@link MessagingLogFormatter}のテストケース
 * @author Iwauo Tajima
 */
@SuppressWarnings("serial")
public class MessagingLogFormatterTest extends LogTestSupport {
    
    private static final String LS = System.getProperty("line.separator");
    
    /**
     * 送信メッセージのテスト
     */
    @Test public void formattingSendingMessage() {
        setAppLogProperties("");
        
        MessagingLogFormatterForUT formatter = new MessagingLogFormatterForUT();
        
        SendingMessage message = createSendingMessage()
                                .addRecord(new HashMap<String, Object>(){{
                                     put("data", "0123456789");   
                                 }});
        
        assertEquals(formatter.getSentMessageLog(message), 
            "@@@@ RECEIVED MESSAGE @@@@"                          + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "reply_to            = [null]"                 + LS +
            "\t" + "timeToLive          = [0]"                    + LS +
            "\t" + "message_body        = [0123456789]"           + LS +
            "\t" + "messageBodyHex      = [30313233343536373839]" + LS +
            "\t" + "messageBodyLength   = [10]"
        );
        
        message = createSendingMessage()
                 .addRecord(new HashMap<String, Object>(){{
                     put("data", null);   
                  }});

        assertEquals(formatter.getSentMessageLog(message),
            "@@@@ RECEIVED MESSAGE @@@@"                          + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "reply_to            = [null]"                 + LS +
            "\t" + "timeToLive          = [0]"                    + LS +
            "\t" + "message_body        = [          ]"           + LS +
            "\t" + "messageBodyHex      = [20202020202020202020]" + LS +
            "\t" + "messageBodyLength   = [10]"
        );
        
        clearAppLogProperties();
    }
    
    /**
     * 受信メッセージのテスト
     */
    @Test public void formattingReceivedMessage() {
        setAppLogProperties("");
        
        MessagingLogFormatterForUT formatter = new MessagingLogFormatterForUT();
        
        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");

        assertEquals(formatter.getReceivedMessageLog(message), 
                "@@@@ RECEIVED MESSAGE @@@@"                          + LS +
                "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id          = [messagingId]"          + LS +
                "\t" + "destination         = [null]"                 + LS +
                "\t" + "correlation_id      = [null]"                 + LS +
                "\t" + "reply_to            = [null]"                 + LS +
                "\t" + "timeToLive          = [-]"                    + LS +
                "\t" + "message_body        = [0123456789]"           + LS +
                "\t" + "messageBodyHex      = [30313233343536373839]" + LS +
                "\t" + "messageBodyLength   = [10]"
                );
        
        message = createReceivedMessage("          ", "UTF-8");
        
        assertEquals(formatter.getReceivedMessageLog(message),
                "@@@@ RECEIVED MESSAGE @@@@"                          + LS +
                "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id          = [messagingId]"          + LS +
                "\t" + "destination         = [null]"                 + LS +
                "\t" + "correlation_id      = [null]"                 + LS +
                "\t" + "reply_to            = [null]"                 + LS +
                "\t" + "timeToLive          = [-]"                    + LS +
                "\t" + "message_body        = [          ]"           + LS +
                "\t" + "messageBodyHex      = [20202020202020202020]" + LS +
                "\t" + "messageBodyLength   = [10]"
                );
        
        clearAppLogProperties();
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
    
    /**
     * テスト用フォーマッタでHTTP送信メッセージのテスト
     */
    @Test public void formattingHttpSendingMessageWithCustomFormatter() {
        setAppLogProperties("");
        
        HttpMessagingLogFormatterForUT formatter = new HttpMessagingLogFormatterForUT();
        
        SendingMessage message = createSendingMessage()
                                .addRecord(new HashMap<String, Object>(){{
                                     put("data", "0123456789");   
                                 }});
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
            "@@@@ HTTP RECEIVED MESSAGE @@@@"                     + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "message_body        = [0123456789]"           + LS +
            "\t" + "messageBodyHex      = [30313233343536373839]" + LS +
            "\t" + "messageBodyLength   = [10]"                   + LS +
            "\t" + "messageHeader       = [{MessageId=messagingId}]"
        );
        
        message = createSendingMessage()
                 .addRecord(new HashMap<String, Object>(){{
                     put("data", null);   
                  }});

        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)),
            "@@@@ HTTP RECEIVED MESSAGE @@@@"                     + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "message_body        = [          ]"           + LS +
            "\t" + "messageBodyHex      = [20202020202020202020]" + LS +
            "\t" + "messageBodyLength   = [10]"                   + LS +
            "\t" + "messageHeader       = [{MessageId=messagingId}]"
        );
        
        clearAppLogProperties();
    }
    
    /**
     * テスト用フォーマッタでHTTP受信メッセージのテスト
     */
    @Test public void formattingHttpReceivedMessageWithCustomFormatter() {
        setAppLogProperties("");
        
        HttpMessagingLogFormatterForUT formatter = new HttpMessagingLogFormatterForUT();
        
        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");
        
        assertEquals(formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP RECEIVED MESSAGE @@@@"                     + LS +
                "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id          = [messagingId]"          + LS +
                "\t" + "destination         = [null]"                 + LS +
                "\t" + "correlation_id      = [null]"                 + LS +
                "\t" + "message_body        = [0123456789]"           + LS +
                "\t" + "messageBodyHex      = [30313233343536373839]" + LS +
                "\t" + "messageBodyLength   = [10]"                   + LS +
                "\t" + "messageHeader       = [{MessageId=messagingId}]"
                );
        
        message = createReceivedMessage("          ", "UTF-8");
        
        assertEquals(formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message)),
                "@@@@ HTTP RECEIVED MESSAGE @@@@"                     + LS +
                "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id          = [messagingId]"          + LS +
                "\t" + "destination         = [null]"                 + LS +
                "\t" + "correlation_id      = [null]"                 + LS +
                "\t" + "message_body        = [          ]"           + LS +
                "\t" + "messageBodyHex      = [20202020202020202020]" + LS +
                "\t" + "messageBodyLength   = [10]"                   + LS +
                "\t" + "messageHeader       = [{MessageId=messagingId}]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(固定長)のテスト
     */
    @Test public void formattingHttpSendingMessageWithDefaultFormatter() {
        setAppLogProperties("");
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createSendingMessage()
                                .addRecord(new HashMap<String, Object>(){{
                                     put("data", "0123456789");   
                                 }});
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
            "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
            "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id     = [messagingId]"             + LS +
            "\t" + "destination    = [null]"                    + LS +
            "\t" + "correlation_id = [null]"                    + LS +
            "\t" + "message_header = [{MessageId=messagingId}]" + LS +
            "\t" + "message_body   = [0123456789]"
        );
        
        message = createSendingMessage()
                 .addRecord(new HashMap<String, Object>(){{
                     put("data", null);   
                  }});

        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)),
            "@@@@ HTTP SENT MESSAGE @@@@"                      + LS +
            "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id     = [messagingId]"            + LS +
            "\t" + "destination    = [null]"                   + LS +
            "\t" + "correlation_id = [null]"                   + LS +
            "\t" + "message_header = [{MessageId=messagingId}]"+ LS +
            "\t" + "message_body   = [          ]"
        );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP受信メッセージ(固定長)のテスト
     */
    @Test public void formattingHttpReceivedMessageWithDefaultFormatter() {
        setAppLogProperties("");
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        ReceivedMessage message = createReceivedMessage("0123456789", "UTF-8");
        
        assertEquals(formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP RECEIVED MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [0123456789]"
                );
        
        message = createReceivedMessage("          ", "UTF-8");
        
        assertEquals(formatter.getHttpReceivedMessageLog(message, getCharsetFromMessage(message)),
                "@@@@ HTTP RECEIVED MESSAGE @@@@"                      + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"            + LS +
                "\t" + "destination    = [null]"                   + LS +
                "\t" + "correlation_id = [null]"                   + LS +
                "\t" + "message_header = [{MessageId=messagingId}]"+ LS +
                "\t" + "message_body   = [          ]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML)のテスト
     */
    @Test public void formattingHttpXmlSendingMessage() {
        setAppLogProperties("");
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlSendingMessage()
                                .addRecord(new HashMap<String, Object>(){{
                                     put("data", "0123456789");   
                                 }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data>0123456789</data></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
            "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
            "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id     = [messagingId]"             + LS +
            "\t" + "destination    = [null]"                    + LS +
            "\t" + "correlation_id = [null]"                    + LS +
            "\t" + "message_header = [{MessageId=messagingId}]" + LS +
            "\t" + "message_body   = [" + expectedBody + "]"
        );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON)のテスト
     */
    @Test public void formattingHttpJsonSendingMessage() {
        setAppLogProperties("");
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"data":"0123456789"}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * 送信メッセージ(固定長本文マスク)のテスト
     */
    @Test public void formattingBodyMaskFixedMaskingByLength() {
        setAppLogProperties("");
        
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        ^.{3}(.{4})
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatterForUT formatter = new MessagingLogFormatterForUT();
        
        SendingMessage message = createSendingMessage()
                                .addRecord(new HashMap<String, Object>(){{
                                     put("data", "0123456789");   
                                 }});
        
        assertEquals(formatter.getSentMessageLog(message), 
            "@@@@ RECEIVED MESSAGE @@@@"                          + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "reply_to            = [null]"                 + LS +
            "\t" + "timeToLive          = [0]"                    + LS +
            "\t" + "message_body        = [012****789]"           + LS +
            "\t" + "messageBodyHex      = [3031322A2A2A2A373839]" + LS +
            "\t" + "messageBodyLength   = [10]"
        );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML本文マスク)のテスト
     * ・プロパティに項目指定が１つ、実電文に該当項目が一つのパターン
     */
    @Test public void formattingBodyMaskXmlSingleSingle() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data>(.*?)</data>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data>**********</data></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML本文マスク)のテスト
     * ・プロパティに項目指定が３つ、実電文に該当項目がそれぞれ一つのパターン
     */
    @Test public void formattingBodyMaskXmlMultiSingle() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data.+?>(.*?)</data.+?>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlMultiItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", "0123456789");   
                    put("data2", "0123456789");   
                    put("data3", "0123456789");   
                    put("nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML本文マスク)のテスト
     * ・プロパティに項目指定が１つ、実電文に該当項目が３つのパターン
     */
    @Test public void formattingBodyMaskXmlSingleMulti() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data1>(.*?)</data1>,<data2>(.*?)</data2>,<data3>(.*?)</data3>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlMultiItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", "0123456789");   
                    put("data2", "0123456789");   
                    put("data3", "0123456789");   
                    put("nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML本文マスク)のテスト
     * ・プロパティに項目指定が３つ、実電文に該当項目が３つのパターン
     */
    @Test public void formattingBodyMaskXmlMultiMulti() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data1>(.*?)</data1>,<data2>(.*?)</data2>,<data3>(.*?)</data3>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlGroupItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("group[0].data1", "0123456789");   
                    put("group[0].data2", "0123456789");   
                    put("group[0].data3", "0123456789");   
                    put("group[0].nomask", "0123456789");   
                    put("group[1].data1", "0123456789");   
                    put("group[1].data2", "0123456789");   
                    put("group[1].data3", "0123456789");   
                    put("group[1].nomask", "0123456789");   
                    put("group[2].data1", "0123456789");   
                    put("group[2].data2", "0123456789");   
                    put("group[2].data3", "0123456789");   
                    put("group[2].nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><group><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></group><group><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></group><group><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></group></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(XML本文マスク)のテスト
     * ・プロパティに項目指定が３つ、実電文に該当項目が３つのパターン
     */
    @Test public void formattingBodyMaskXmlArray() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data1>(.*?)</data1>,<data2>(.*?)</data2>,<data3>(.*?)</data3>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlArrayItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("data2", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("data3", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("nomask", new String[]{"0123456789", "0123456789", "0123456789"});   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data1>**********</data1><data1>**********</data1><data1>**********</data1><data2>**********</data2><data2>**********</data2><data2>**********</data2><data3>**********</data3><data3>**********</data3><data3>**********</data3><nomask>0123456789</nomask><nomask>0123456789</nomask><nomask>0123456789</nomask></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON本文マスク)のテスト
     * ・プロパティに項目指定が１つ、実電文に該当項目が一つのパターン
     */
    @Test public void formattingBodyMaskJsonSingleSingle() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        "data":"(.*?)"
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"data":"**********"}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON本文マスク)のテスト
     * ・プロパティに項目指定が３つ、実電文に該当項目がそれぞれ一つのパターン
     */
    @Test public void formattingBodyMaskJsonMultiSingle() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        "data.+?":"(.*?)"
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonMultiItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", "0123456789");   
                    put("data2", "0123456789");   
                    put("data3", "0123456789");   
                    put("nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"data1":"**********","data2":"**********","data3":"**********","nomask":"0123456789"}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON本文マスク)のテスト
     * ・プロパティに項目指定が１つ、実電文に該当項目が３つのパターン
     */
    @Test public void formattingBodyMaskJsonSingleMulti() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        "data1":"(.*?)","data2":"(.*?)","data3":"(.*?)"
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonMultiItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", "0123456789");   
                    put("data2", "0123456789");   
                    put("data3", "0123456789");   
                    put("nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"data1":"**********","data2":"**********","data3":"**********","nomask":"0123456789"}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON本文マスク)のテスト
     * ・プロパティに項目指定が３つ、実電文に該当項目が３つのパターン
     */
    @Test public void formattingBodyMaskJsonMultiMulti() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        ".+?data1":"(.*?)",".+?data2":"(.*?)",".+?data3":"(.*?)"
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonGroupItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("group[0].data1", "0123456789");   
                    put("group[0].data2", "0123456789");   
                    put("group[0].data3", "0123456789");   
                    put("group[0].nomask", "0123456789");   
                    put("group[1].data1", "0123456789");   
                    put("group[1].data2", "0123456789");   
                    put("group[1].data3", "0123456789");   
                    put("group[1].nomask", "0123456789");   
                    put("group[2].data1", "0123456789");   
                    put("group[2].data2", "0123456789");   
                    put("group[2].data3", "0123456789");   
                    put("group[2].nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"group":[{"data1":"**********","data2":"**********","data3":"**********","nomask":"0123456789"},{"data1":"**********","data2":"**********","data3":"**********","nomask":"0123456789"},{"data1":"**********","data2":"**********","data3":"**********","nomask":"0123456789"}]}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(JSON本文マスク)のテスト
     * ・JSON配列のパターン
     */
    @Test public void formattingBodyMaskJsonArray() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        "data1":\\[(.*?)\\],"data2":\\[(.*?)\\],"data3":\\[(.*?)\\]
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createJsonArrayItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("data2", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("data3", new String[]{"0123456789", "0123456789", "0123456789"});   
                    put("nomask", new String[]{"0123456789", "0123456789", "0123456789"});   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        {"data1":[**************************************],"data2":[**************************************],"data3":[**************************************],"nomask":["0123456789","0123456789","0123456789"]}
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(本文マスク)のテスト
     * ・空のパターンを含む
     */
    @Test public void formattingBodyMaskEmptyPattern() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data1>(.*?)</data1>,,<data2>(.*?)</data2>,<data3>(.*?)</data3>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlMultiItemSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data1", "0123456789");   
                    put("data2", "0123456789");   
                    put("data3", "0123456789");   
                    put("nomask", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data1>**********</data1><data2>**********</data2><data3>**********</data3><nomask>0123456789</nomask></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(本文マスク)のテスト
     * ・マスク文字列を変更
     */
    @Test public void formattingChangeMaskingChar() {
        String maskingChar = "#";
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data>(.*?)</data>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns, maskingChar);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data>##########</data></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(本文マスク)のテスト
     * ・マスク文字列に２文字指定
     */
    @Test public void formattingChangeMaskingCharToMultiChar() {
        String maskingChar = "**";
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data>(.*?)</data>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns, maskingChar);
        
        try {
            new MessagingLogFormatter();
            fail("例外が発生する");
        } catch (IllegalArgumentException e) {
            assertEquals("maskingChar was not char type. maskingChar = [**]", e.getMessage());
        }
        
        System.clearProperty("nablarch.appLog.filePath");
        LogUtil.removeAllObjectsBoundToContextClassLoader();
    }
    
    /**
     * HTTP送信メッセージ(本文マスク)のテスト
     * ・マスク文字列に空文字指定
     */
    @Test public void formattingChangeMaskingCharToEmptyChar() {
        String maskingChar = "";
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data>(.*?)</data>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns, maskingChar);
        
        try {
            new MessagingLogFormatter();
            fail("例外が発生する");
        } catch (IllegalArgumentException e) {
            assertEquals("maskingChar was not char type. maskingChar = []", e.getMessage());
        }
        
        clearAppLogProperties();
    }
    
    /**
     * HTTP送信メッセージ(本文マスク)のテスト
     * ・キャプチャ指定が無いパターンを指定
     */
    @Test public void formattingNoCapturePattern() {
        String maskingPatterns = Hereis.string().trim();
        /********************************************************************************
        <data1>.*?</data1>
        *********************************************************************************/
        setAppLogProperties(maskingPatterns);
        
        MessagingLogFormatter formatter = new MessagingLogFormatter();
        
        SendingMessage message = createXmlSendingMessage()
                .addRecord(new HashMap<String, Object>(){{
                    put("data", "0123456789");   
                }});
        
        String expectedBody = Hereis.string().trim();
        /********************************************************************************
        <?xml version="1.0" encoding="UTF-8"?><request><data>0123456789</data></request>
        *********************************************************************************/
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
                "@@@@ HTTP SENT MESSAGE @@@@"                       + LS +
                "\t" + "thread_name    = [" + Thread.currentThread().getName() + "]" + LS +
                "\t" + "message_id     = [messagingId]"             + LS +
                "\t" + "destination    = [null]"                    + LS +
                "\t" + "correlation_id = [null]"                    + LS +
                "\t" + "message_header = [{MessageId=messagingId}]" + LS +
                "\t" + "message_body   = [" + expectedBody + "]"
                );
        
        clearAppLogProperties();
    }
    
    /**
     * テスト用フォーマッタでHTTP送信メッセージ(空電文)のテスト
     */
    @Test public void formattingEmptyHttpSendingMessageWithCustomFormatter() {
        setAppLogProperties("");
        
        HttpMessagingLogFormatterForUT formatter = new HttpMessagingLogFormatterForUT();
        
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "Fixed"
        text-encoding:    "utf8"
        record-length:    0
        [Data]
        1 data  X(0)    # データ
        *****************************/
        
        FilePathSetting.getInstance()
                       .addBasePathSetting("format", "file:./")
                       .addFileExtensions("format", ".fmt");
        
        DataRecordFormatter dataformatter = FormatterFactory
                                       .getInstance()
                                       .setCacheLayoutFileDefinition(false)
                                       .createFormatter(formatFile);
        
        SendingMessage message = new SendingMessage()
                                .setMessageId("messagingId")
                                .setFormatter(dataformatter);
        
        assertEquals(formatter.getHttpSentMessageLog(message, getCharsetFromMessage(message)), 
            "@@@@ HTTP RECEIVED MESSAGE @@@@"                     + LS +
            "\t" + "thread_name         = [" + Thread.currentThread().getName() + "]" + LS +
            "\t" + "message_id          = [messagingId]"          + LS +
            "\t" + "destination         = [null]"                 + LS +
            "\t" + "correlation_id      = [null]"                 + LS +
            "\t" + "message_body        = []"                     + LS +
            "\t" + "messageBodyHex      = []"                     + LS +
            "\t" + "messageBodyLength   = [0]"                    + LS +
            "\t" + "messageHeader       = [{MessageId=messagingId}]"
        );
    }
    
    public SendingMessage createEmptySendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "Fixed"
        text-encoding:    "utf8"
        record-length:    0
        [Data]
        1 data  X(0)    # データ
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


    /**
     * マスクパターンとを指定したapp-log.propertiesを作成し、パスを設定する
     * @param maskingPatterns マスクパターン
     */
    private void setAppLogProperties(String maskingPatterns) {
        File propertyFile = Hereis.file("./temp/app-log.properties", maskingPatterns);
        /*********************************************************************************
        messagingLogFormatter.maskingPatterns=${maskingPatterns}
        **********************************************************************************/
        propertyFile.deleteOnExit();
        
        LogUtil.removeAllObjectsBoundToContextClassLoader();
        System.setProperty("nablarch.appLog.filePath", propertyFile.toURI().toString());
    }
    
    /**
     * マスクパターンとマスク文字列を指定したapp-log.propertiesを作成し、パスを設定する
     * @param maskingPatterns マスクパターン
     * @param maskingChar マスク文字列
     */
    private void setAppLogProperties(String maskingPatterns, String maskingChar) {
        File propertyFile = Hereis.file("./temp/app-log.properties", maskingPatterns, maskingChar);
        /*********************************************************************************
        messagingLogFormatter.maskingChar=${maskingChar}
        messagingLogFormatter.maskingPatterns=${maskingPatterns}
        **********************************************************************************/
        propertyFile.deleteOnExit();
        
        LogUtil.removeAllObjectsBoundToContextClassLoader();
        System.setProperty("nablarch.appLog.filePath", propertyFile.toURI().toString());
    }
    
    /**
     * app-log.propertiesのパスをクリアする
     */
    private void clearAppLogProperties() {
        System.clearProperty("nablarch.appLog.filePath");
        LogUtil.removeAllObjectsBoundToContextClassLoader();
    }
    
    public SendingMessage createSendingMessage() {
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
                  .setMessageId("messagingId")
                  .setFormatter(formatter);
    }

    public ReceivedMessage createReceivedMessage(String body, String charset) {
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
                  .setMessageId("messagingId")
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

    public SendingMessage createXmlMultiItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [request]
        1 data1 [0..1]  X  # データ1
        2 data2 [0..1]  X  # データ2
        3 data3 [0..1]  X  # データ3
        4 nomask [0..1] X  # データ4
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

    public SendingMessage createXmlArrayItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [request]
        1 data1 [0..3]  X  # データ1
        2 data2 [0..3]  X  # データ2
        3 data3 [0..3]  X  # データ3
        4 nomask [0..3] X  # データ4
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

    public SendingMessage createXmlGroupItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "XML"
        text-encoding:    "UTF-8"
        [request]
        1 group [*]     OB
        [group]
        1 data1         X  # データ1
        2 data2         X  # データ2
        3 data3         X  # データ3
        4 nomask        X  # データ4
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

    public SendingMessage createJsonSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "JSON"
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

    public SendingMessage createJsonMultiItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 data1 [0..1]  X  # データ1
        2 data2 [0..1]  X  # データ2
        3 data3 [0..1]  X  # データ3
        4 nomask [0..1] X  # データ4
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

    public SendingMessage createJsonArrayItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 data1 [0..3]  X  # データ1
        2 data2 [0..3]  X  # データ2
        3 data3 [0..3]  X  # データ3
        4 nomask [0..3] X  # データ4
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

    public SendingMessage createJsonGroupItemSendingMessage() {
        File formatFile = Hereis.file("./data.fmt");
        /***************************
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 group [*]     OB
        [group]
        1 data1         X  # データ1
        2 data2         X  # データ2
        3 data3         X  # データ3
        4 nomask        X  # データ4
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

    public static class
    MessagingLogFormatterForUT extends MessagingLogFormatter {
        protected LogItem<MessagingLogContext>[]
        getFormattedLogItems(Map<String, LogItem<MessagingLogContext>> logItems,
                             Map<String, String> unused,
                             String formatPropName, 
                             String defaultFormat) {
            Map<String, String> props = new HashMap<String, String>();
            props.put(formatPropName, getFormatStr());
            return super.getFormattedLogItems(
                logItems, props, formatPropName, defaultFormat
            );
        }
        
        private String getFormatStr() {
            return "@@@@ RECEIVED MESSAGE @@@@"
            + "\n\tthread_name         = [$threadName$]"
            + "\n\tmessage_id          = [$messageId$]"
            + "\n\tdestination         = [$destination$]"
            + "\n\tcorrelation_id      = [$correlationId$]" 
            + "\n\treply_to            = [$replyTo$]"
            + "\n\ttimeToLive          = [$timeToLive$]"
            + "\n\tmessage_body        = [$messageBody$]"
            + "\n\tmessageBodyHex      = [$messageBodyHex$]"
            + "\n\tmessageBodyLength   = [$messageBodyLength$]";
        }
    }
    
    public static class
    HttpMessagingLogFormatterForUT extends MessagingLogFormatter {
        protected LogItem<MessagingLogContext>[]
        getFormattedLogItems(Map<String, LogItem<MessagingLogContext>> logItems,
                             Map<String, String> unused,
                             String formatPropName, 
                             String defaultFormat) {
            Map<String, String> props = new HashMap<String, String>();
            props.put(formatPropName, getFormatStr());
            return super.getFormattedLogItems(
                logItems, props, formatPropName, defaultFormat
            );
        }
        
        private String getFormatStr() {
            return "@@@@ HTTP RECEIVED MESSAGE @@@@"
            + "\n\tthread_name         = [$threadName$]"
            + "\n\tmessage_id          = [$messageId$]"
            + "\n\tdestination         = [$destination$]"
            + "\n\tcorrelation_id      = [$correlationId$]" 
            + "\n\tmessage_body        = [$messageBody$]"
            + "\n\tmessageBodyHex      = [$messageBodyHex$]"
            + "\n\tmessageBodyLength   = [$messageBodyLength$]"
            + "\n\tmessageHeader       = [$messageHeader$]";
        }
    }
}
