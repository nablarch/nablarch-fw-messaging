package nablarch.fw.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.MapUtil;
import nablarch.fw.messaging.StructuredResponseMessageTest.TestData;
import nablarch.test.support.tool.Hereis;

import org.junit.Test;

/**
 * {@link ResponseMessage}のテスト。
 * 
 * @author Iwauo Tajima
 */
public class ResponseMessageTest {

    @Test public void throwingAsError() {
        ResponseMessage res = cereateResponseMessage();
        try {
            res.throwAsError(new MessagingException("a messaging error"));
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof ErrorResponseMessage);
            ErrorResponseMessage errorRes = (ErrorResponseMessage) e;
            
            assertEquals(500, errorRes.getStatusCode());
            assertFalse(errorRes.isSuccess());
            assertTrue(errorRes.getMessage().contains("a messaging error"));
        }
    }
    
    public ResponseMessage cereateResponseMessage() {
        return new ResponseMessage(
            new FwHeader(),
            new ReceivedMessage(new byte[]{})
               .setMessageId("messageId")
               .setDestination("dest")
               .setReplyTo("replyTo")
        );
    }
    
    /**
     * レコードとしてオブジェクトを追加した場合のテストを行います。<br>
     * 
     * 条件：<br>
     *   オブジェクトを追加する。<br>
     *   
     * 期待結果：<br>
     *   追加したオブジェクトがマップに変換されメモリ上に保持されること。<br>
     */
    @Test
    public void testAddRecordObject() {
        ResponseMessage responseMesssage = cereateResponseMessage();
        List<DataRecord> records = responseMesssage.getRecords();
        
        // テスト用フォーマット
        File requestFormatFile = Hereis.file(getFormatFileName());
        
        /****************************
        file-type:       "Variable"
        text-encoding:   "MS932"
        field-separator: ","
        record-separator: "\n"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        
        responseMesssage.setFormatter(formatter);
        
        TestData record1 = new TestData();
        record1.setId("1");
        record1.setName("name1");
        responseMesssage.addRecord(record1);
        assertEquals(1, records.size());
        assertDataObject(record1, null, records.get(0));

        TestData record2 = new TestData();
        record2.setId("2");
        record2.setName("name2");
        responseMesssage.addRecord("request", record2);
        assertEquals(2, records.size());
        assertDataObject(record2, "request", records.get(1));
    }
    
    private String getFormatFileName() {
        FilePathSetting fps = FilePathSetting.getInstance()
            .addBasePathSetting("format", "file:temp")
            .addFileExtensions("format", "fmt");
        return Builder.concat(
                   fps.getBasePathSettings().get("format").getPath(),
                   "/", "RequestMessageTest", ".", 
                   fps.getFileExtensions().get("format"));
    }

    /**
     * データレコードを検証します。
     * @param expectedObject 期待オブジェクト
     * @param expectedRecordType 期待レコード種別
     * @param actualRecord 結果レコード
     */
    private void assertDataObject(Object expectedObject, String expectedRecordType, DataRecord actualRecord) {
        Map<String, Object> expectedRecord = MapUtil.createFlatMap(expectedObject);
        assertEquals(expectedRecord.size(), actualRecord.size());
        for(String key : actualRecord.keySet()) {
            assertEquals(expectedRecord.get(key), actualRecord.get(key));
        }
        for(String key : expectedRecord.keySet()) {
            assertEquals(expectedRecord.get(key), actualRecord.get(key));
        }
        assertEquals(expectedRecordType, actualRecord.getRecordType());
    }
}
