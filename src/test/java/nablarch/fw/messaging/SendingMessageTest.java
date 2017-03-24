package nablarch.fw.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.MapUtil;
import nablarch.fw.messaging.StructuredResponseMessageTest.TestData;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * SendingMessageのテスト
 * @author Iwauo Tajima
 */
public class SendingMessageTest {
    /**
     * このクラス固有のヘッダのテスト
     */
    @Test public void testHeader() {
        SendingMessage message = new SendingMessage();
        
        message.setTimeToLive(60 * 1000); // 60秒間に設定
        assertEquals(Long.valueOf(60 * 1000), Long.valueOf(message.getTimeToLive()));
        
        message.setTimeToLive(-1); //負数を設定したら
        assertEquals(Long.valueOf(0), Long.valueOf(message.getTimeToLive())); // 0が設定される。
    }
    
    /**
     * コンストラクタのテスト
     */
    @Test public void testConstructor() {
        SendingMessage message = new SendingMessage()
                                .setReplyTo("LOCAL.QUEUE");
        
        SendingMessage other = new SendingMessage(message);
        assertEquals("LOCAL.QUEUE", other.getReplyTo());
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
        SendingMessage responseMesssage = new SendingMessage();
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
