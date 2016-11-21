package nablarch.fw.messaging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.dataformat.LayoutDefinition;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.MapUtil;
import nablarch.test.support.tool.Hereis;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link StructuredResponseMessage}のテストクラス。
 *
 * @author TIS
 */
public class StructuredResponseMessageTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("format", "fmt");
    }
    
    /**
     * 応答メッセージを作成します。
     * @return　応答メッセージ
     */
    private StructuredResponseMessage createResponseMessage() {
        ReceivedMessage receivedMessage = new ReceivedMessage(new byte[]{});
        FwHeader fwHeader = new FwHeader();
        StructuredRequestMessage requestMessage = new StructuredRequestMessage(fwHeader, receivedMessage);
        requestMessage.setDestination("destination");
        requestMessage.setReplyTo("replyTo");
        requestMessage.setMessageId("messageId");
        
        StructuredResponseMessage responseMessage = new StructuredResponseMessage(requestMessage);
        responseMessage.setFwHeaderDefinition(new FwHeaderDefinition() {
            @Override
            public void writeFwHeaderTo(SendingMessage message, FwHeader header) {
            }
            
            @Override
            public RequestMessage readFwHeaderFrom(ReceivedMessage message) {
                return null;
            }
        });
        return responseMessage;
    }
    
    private String getFormatFileName() {
        FilePathSetting fps = FilePathSetting.getInstance();
        return Builder.concat(
                   fps.getBasePathSettings().get("format").getPath(),
                   "/", "StructuredRequestMessageTest", ".", 
                   fps.getFileExtensions().get("format"));
    }
    /**
     * フォーマッタ設定処理のテストを行います。<br>
     * 
     * 条件：<br>
     *   フォーマッタ設定処理を呼び出す。<br>
     *   
     * 期待結果：<br>
     *   初期化済みフォーマッタが設定されること。<br>
     */
    @Test
    public void testSetFormatterDataRecordFormatter() {
        StructuredResponseMessage responseMesssage = createResponseMessage();
        
        // テスト用フォーマット
        File requestFormatFile = Hereis.file(getFormatFileName());
        
        /****************************
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        
        // 初期化処理が行われるまでデフォルトエンコーディングは設定されない
        assertNull(((DataRecordFormatterSupport)formatter).getDefaultEncoding());
        
        responseMesssage.setFormatter(formatter);
        
        // 初期化処理が行われるとデフォルトエンコーディングが設定される
        assertEquals(Charset.forName("UTF-8"), 
                ((DataRecordFormatterSupport)responseMesssage.getFormatter()).getDefaultEncoding());
    }
    
    /**
     * フォーマッタ設定処理にnullを設定した場合のテストを行います。<br>
     * 
     * 条件：<br>
     *   フォーマッタ設定処理にnullを設定して呼び出す。<br>
     *   
     * 期待結果：<br>
     *   エラーが発生せずnullが設定されること。<br>
     */
    @Test
    public void testSetFormatterDataRecordFormatterNull() {
        StructuredResponseMessage responseMesssage = createResponseMessage();
        responseMesssage.setFormatter(null);
        assertNull(responseMesssage.getFormatter());
    }
    
    /**
     * レコードとしてMapを追加した場合のテストを行います。<br>
     * 
     * 条件：<br>
     *   Mapを追加する。<br>
     *   
     * 期待結果：<br>
     *   追加したマップがメモリ上に保持されること。<br>
     */
    @Test
    public void testAddRecordMap() {
        StructuredResponseMessage responseMesssage = createResponseMessage();
        List<DataRecord> records = responseMesssage.getRecords();
        
        Map<String, Object> record1 = new HashMap<String, Object>();
        record1.put("id", "1");
        record1.put("name", "name1");
        responseMesssage.addRecord(record1);
        assertEquals(1, records.size());
        assertDataRecord(record1, null, records.get(0));

        Map<String, Object> record2 = new HashMap<String, Object>();
        record1.put("id", "2");
        record1.put("name", "name2");
        responseMesssage.addRecord("testtype", record2);
        assertEquals(2, records.size());
        assertDataRecord(record2, "testtype", records.get(1));
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
        StructuredResponseMessage responseMesssage = createResponseMessage();
        List<DataRecord> records = responseMesssage.getRecords();
        
        TestData record1 = new TestData();
        record1.setId("1");
        record1.setName("name1");
        responseMesssage.addRecord(record1);
        assertEquals(1, records.size());
        assertDataObject(record1, null, records.get(0));

        TestData record2 = new TestData();
        record2.setId("2");
        record2.setName("name2");
        responseMesssage.addRecord("testtype", record2);
        assertEquals(2, records.size());
        assertDataObject(record2, "testtype", records.get(1));
    }
   
    /**
     * バイト配列生成のテストを行います。<br>
     * 
     * 条件：<br>
     *   Mapを追加し、バイト配列を取得する。<br>
     *   
     * 期待結果：<br>
     *   追加したオブジェクトがマップに変換されメモリ上に保持されること。<br>
     */
    @Test
    public void testGetBodyBytes() throws Exception{
        StructuredResponseMessage responseMesssage = createResponseMessage();
        
        // テスト用フォーマット
        File requestFormatFile = Hereis.file(getFormatFileName());
        
        /****************************
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        responseMesssage.setFormatter(formatter);
        
        // 期待XML
        String expectedXml = Hereis.string();
        /****************************
        <?xml version="1.0" encoding="UTF-8"?><request><id>1</id><name>name1</name></request>
        ****************************/
        
        TestData record1 = new TestData();
        record1.setId("1");
        record1.setName("name1");
        responseMesssage.addRecord(record1);

        byte[] actualBytes = responseMesssage.getBodyBytes();
        
        assertArrayEquals(expectedXml.trim().getBytes("UTF-8"), actualBytes);
    }

    /**
     * バイト配列生成のテストを行います。<br>
     * 
     * 条件：<br>
     *   Mapを追加し、バイト配列を取得する。<br>
     *   
     * 期待結果：<br>
     *   追加したオブジェクトがマップに変換されメモリ上に保持されること。<br>
     */
    @Test
    public void testGetBodyBytesWithRecordType() throws Exception{
        StructuredResponseMessage responseMesssage = createResponseMessage();
        
        // テスト用フォーマット
        File requestFormatFile = Hereis.file(getFormatFileName());
        
        /****************************
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        responseMesssage.setFormatter(formatter);
        
        // 期待XML
        String expectedXml = Hereis.string();
        /****************************
        <?xml version="1.0" encoding="UTF-8"?><request><id>1</id><name>name1</name></request>
        ****************************/
        
        TestData record1 = new TestData();
        record1.setId("1");
        record1.setName("name1");
        responseMesssage.addRecord("recordtype", record1);

        byte[] actualBytes = responseMesssage.getBodyBytes();
        
        assertArrayEquals(expectedXml.trim().getBytes("UTF-8"), actualBytes);
    }

    
    /**
     * 強制的にIOExceptionを発生させるテストを行います。<br>
     * 
     * 条件：<br>
     *   読み込み時および書き込み時にIOExceptionを発生するのテスト用フォーマッタを使用する。<br>
     *   
     * 期待結果：<br>
     *   RuntimeExceptionが発生すること<br>
     */
    @Test
    public void testUsingIOExceptionFormatter() throws Exception {
        // テスト用のリポジトリ構築
        File diConfigFile = Hereis.file("temp/StructuredResponseMessageTest.xml");
        /*****
        <?xml version="1.0" encoding="UTF-8"?>
        <component-configuration
            xmlns="http://tis.co.jp/nablarch/component-configuration"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://tis.co.jp/nablarch/component-configuration  ./../component-configuration.xsd">
        
          <component name="formatterFactory"
                     class="nablarch.fw.messaging.StructuredResponseMessageTest$TestFormatterFactory" />
                     
        </component-configuration>
        */
        diConfigFile.deleteOnExit();
        SystemRepository.clear();
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(diConfigFile.toURI().toString())));
        
        StructuredResponseMessage responseMesssage = createResponseMessage();
        
        // テスト用フォーマット
        File requestFormatFile = Hereis.file(getFormatFileName());
        
        /****************************
        file-type:      "IoEx"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        responseMesssage.setFormatter(formatter);
        
        // テストデータ
        TestData record1 = new TestData();
        record1.setId("1");
        record1.setName("name1");
        responseMesssage.addRecord("recordtype", record1);

        // テスト実施
        try {
            responseMesssage.getBodyBytes();
            fail("RuntimeExceptionが発生する");
        } catch (RuntimeException e) {
        }
        
        SystemRepository.clear();
    }
    

    /**
     * データレコードを検証します。
     * @param expectedRecord 期待レコード
     * @param expectedRecordType 期待レコード種別
     * @param actualRecord 結果レコード
     */
    private void assertDataRecord(Map<String, Object> expectedRecord, String expectedRecordType, DataRecord actualRecord) {
        assertEquals(expectedRecord.size(), actualRecord.size());
        for(String key : actualRecord.keySet()) {
            assertEquals(expectedRecord.get(key), actualRecord.get(key));
        }
        for(String key : expectedRecord.keySet()) {
            assertEquals(expectedRecord.get(key), actualRecord.get(key));
        }
        assertEquals(expectedRecordType, actualRecord.getRecordType());
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
    
    public static class TestData {
        private String id;
        private String name;
        public void setId(String id) {
            this.id = id;
        }
        public String getId() {
            return this.id;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getName() {
            return this.name;
        }
    }
    
    public static class TestFormatterFactory extends FormatterFactory {
        @Override
        protected DataRecordFormatter createFormatter(String fileType,
                String formatFilePath) {
            if (fileType.equals("Hoge")) {
                return new HogeFormatter();
            } else if (fileType.equals("IoEx")) {
                    return new IOExFormatter();
            }
            return super.createFormatter(fileType, formatFilePath);
        }
    }
    
    public static class HogeFormatter implements DataRecordFormatter {
        @Override
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            return null;
        }
        @Override
        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
        }
        @Override
        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
        }
        @Override
        public DataRecordFormatter initialize() {
            return this;
        }
        @Override
        public DataRecordFormatter setInputStream(InputStream stream) {
            return this;
        }
        @Override
        public void close() {
        }

        @Override
        public DataRecordFormatter setDefinition(LayoutDefinition definition) {
            return this;
        }
        @Override
        public DataRecordFormatter setOutputStream(OutputStream stream) {
            return this;
        }
        @Override
        public boolean hasNext() throws IOException {
            return false;
        }

        @Override
        public int getRecordNumber() {
            return 0;
        }
    }
    
    public static class IOExFormatter extends HogeFormatter {
        @Override
        public DataRecord readRecord() throws IOException,
                InvalidDataFormatException {
            throw new IOException("DummyException");
        }
        @Override
        public void writeRecord(Map<String, ?> record) throws IOException,
                InvalidDataFormatException {
            throw new IOException("DummyException");
        }
        @Override
        public void writeRecord(String recordType, Map<String, ?> record)
                throws IOException, InvalidDataFormatException {
            throw new IOException("DummyException");
        }
    }

}
