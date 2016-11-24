package nablarch.fw.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.Request;
import nablarch.test.support.tool.Hereis;

import org.junit.Test;

/**
 * {@link RequestMessage} のテスト
 * @author Iwauo Tajima
 */
public class RequestMessageTest {
    
    private DataRecordFormatter formatter;
    
    public void setupFormatter() {
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
        
        formatter = FormatterFactory.getInstance()
                                    .setCacheLayoutFileDefinition(false)
                                    .createFormatter(formatFile);
    }
    
    public RequestMessage createRequestMessage() throws Exception {
        setupFormatter();

        FwHeader header = new FwHeader()
                         .setRequestId("testId")
                         .setUserId("testUser");

        System.out.println("####################################################################################################");
        BufferedReader reader = new BufferedReader(new FileReader("./test.fmt"));
        String line = null;
        while ((line = reader.readLine()) != null) {
            System.out.println("line = " + line);
        }

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
        );
        return request;
    }
        
    /**
     * {@link Request}インターフェースとの互換性テスト
     */
    @Test public void beingAbleToAccessAsARequest() throws Exception {
        Request<Object> req = createRequestMessage();
        assertEquals("testId", req.getRequestPath());
        req.setRequestPath("testId2");
        assertEquals("testId2", req.getRequestPath());
        
        try {
            req.setRequestPath(null);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        // データ部読み込み前のパラメータアクセス
        assertNull(req.getParamMap());
        assertNull(req.getParam("title"));
        
        // データ部読み込み
        ((RequestMessage) req).readRecord();
        
        Map<String, Object> params = req.getParamMap();
        assertEquals(3, params.size());
        assertEquals("Writin Effective UseCases", params.get("title"));
        assertEquals("Addison-Wesley",            params.get("publisher"));
        assertEquals("Alistair Cockburn",         params.get("authors"));
        
        assertEquals("Writin Effective UseCases", req.getParam("title"));
        assertEquals("Addison-Wesley",            req.getParam("publisher"));
        assertEquals("Alistair Cockburn",         req.getParam("authors"));
    }
    
    /**
     * 応答電文作成のテスト
     */
    @Test public void replyingToARequestMessage() throws Exception {
        RequestMessage req = createRequestMessage();
        req.setMessageId("requestMessageId");
        req.setReplyTo("destinationReplyTo");
        req.setFormatter(formatter);
        
        ResponseMessage res = req.reply();
        assertEquals("destinationReplyTo", res.getDestination());
        assertEquals("requestMessageId",   res.getCorrelationId());
        
        assertSame(req.getFormatter(),     res.getFormatter());
        assertSame(req.getFwHeader(), res.getFwHeader());
        
        // replyToヘッダが空の場合は応答を作成できない。
        try {
            req.setHeader("ReplyTo", null);
            req.reply();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof MessagingException);
        }
    }
    
    /**
     * ヘッダ項目に対するアクセサのテスト
     */
    @Test public void accessingStandardHeaders() throws Exception {
        RequestMessage req = createRequestMessage();
        
        // メッセージID
        req.setMessageId("testId");
        assertEquals("testId", req.getMessageId());
        try {
            req.setMessageId(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        // 関連ID
        req.setCorrelationId("testCorrId");
        assertEquals("testCorrId", req.getCorrelationId());
        try {
            req.setCorrelationId(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        // 宛先キュー
        req.setDestination("testDest");
        assertEquals("testDest", req.getDestination());
        try {
            req.setDestination(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        // 応答宛先キュー
        req.setReplyTo("replyTo");
        assertEquals("replyTo", req.getReplyTo());
        try {
            req.setReplyTo(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        // Bulk oparation
        Map<String, Object> headers = req.getHeaderMap();
        assertEquals(4, headers.size());
        assertEquals("testId",     headers.get("MessageId"));
        assertEquals("testCorrId", headers.get("CorrelationId"));
        assertEquals("testDest",   headers.get("Destination"));
        assertEquals("replyTo",    headers.get("ReplyTo"));

        
        req.getHeaderMap().clear();
        assertEquals(0, req.getHeaderMap().size());
        
        headers = new HashMap<String, Object>() {{
            put("MessageId",     "testId");
            put("CorrelationId", "testCorrId");
            put("Destination",   "testDest");
            put("ReplyTo",       "replyTo");
            put("userHeader1",   "user1");
        }};
        
        req.setHeaderMap(headers);
        
        assertEquals(5, headers.size());
        assertEquals("testId",      req.getMessageId());
        assertEquals("testCorrId",  req.getCorrelationId());
        assertEquals("testDest",    req.getDestination());
        assertEquals("replyTo",     req.getReplyTo());
        assertEquals("user1",       req.getHeader("userHeader1"));
    }
    
    /**
     * フレームワーク制御ヘッダに対するアクセサのテスト
     */
    @Test public void accessingFwHeader() throws Exception {
        RequestMessage request = createRequestMessage();
        
        FwHeader fwHeader = request.getFwHeader();
        assertFalse(fwHeader.containsKey("resendFlag"));
        assertFalse(fwHeader.isResendingSupported());
        assertFalse(fwHeader.isResendingRequest());
        
        fwHeader.setResendFlag(0); // 初回電文
        assertTrue(fwHeader.isResendingSupported());
        assertFalse(fwHeader.isResendingRequest());
        
        fwHeader.setResendFlag(100); // 再送要求電文
        assertTrue(fwHeader.isResendingSupported());
        assertTrue(fwHeader.isResendingRequest());
        
        // 不正値設定
        try {
            fwHeader.setUserId(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof MessagingException);
        }
    }
    
    /**
     * データ部の読み込み処理のテスト
     */
    @Test public void readingDataRecords() throws Exception {
        RequestMessage req = createRequestMessage();
        // ------------------ 異常系 ------------------- //
        
        // フォーマッタが設定されていない状態で読み込みを行うと実行時例外を送出
        req.setFormatter(null);
        try {
            req.readRecord();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
        req.setFormatter(null);
        try {
            req.readRecords();
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }
    
    /**
     * 読み込み済みデータレコードの取得機能のテスト
     */
    @Test public void acquiringAlreadyReadData() throws Exception {
        RequestMessage req = createRequestMessage();
        
        req.readRecords();
        
        assertEquals(1, req.getRecords().size());
        assertEquals("Writin Effective UseCases", req.getRecordOf("Book").get("title"));
        
                // 存在しないレコード種別
        assertNull(req.getRecordOf("unknown"));
        assertEquals(0, req.getRecordsOf("unknown").size());
        
        // 不正引数
        try {
            req.getRecordsOf("");
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
}
