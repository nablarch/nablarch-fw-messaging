package nablarch.fw.messaging.reader;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import nablarch.test.support.tool.Hereis;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class FwHeaderReaderTest {
    private static TestEmbeddedMessagingProvider provider;
    
    private MessageReader reader;
    private FwHeaderReader fwReader;
    private ExecutionContext context;
    private DataRecordFormatter headerFormatter;
    private DataRecordFormatter bodyFormatter;
    
    @BeforeClass
    public static void startMessagingServer() {
        provider = new TestEmbeddedMessagingProvider()
                      .setQueueNames(Arrays.asList("LOCAL.QUEUE", "LOCAL.REPLY"));
    }
    
    @AfterClass
    public static void stopMessagingServer() {
        TestEmbeddedMessagingProvider.stopServer();
    }

    @Before
    public void setUp() {
        MessagingContext.detach();
        SystemRepository.clear();

        SystemRepository.load(new ObjectLoader() {
            public Map<String, Object> load() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("formatterFactory", new FormatterFactory());
                return data;
            }
        });
    }

    public void setupReader() {
        MessagingContext.attach(provider.createContext());
        reader = new MessageReader()
                    .setReadTimeout(500)
                    .setReceiveQueueName("LOCAL.QUEUE");
        context = new ExecutionContext();
    }
    
    public void setupFormatter() throws Exception {
        File formatFile = Hereis.file("./NBL_bookdata.fmt");
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

        
        File replyFormatFile = Hereis.file("./NBL_bookdata_REPLY.fmt");
        /**************************************************
        file-type:        "Fixed"
        text-encoding:    "sjis"
        record-length:    10
        record-separator: "\n"
        
        [Reply]
        1   statusCode  X(5)     # 処理結果コード
        6   processedCount Z(5)  # 処理対象レコード数
        ****************************************************/

        
        bodyFormatter = FormatterFactory.getInstance()
                                        .createFormatter(formatFile);
        
        FilePathSetting.getInstance()
                       .addBasePathSetting("format", "file:./")
                       .addFileExtensions("format", "fmt");
        
        File headerFile = Hereis.file("./header.fmt");
        /*******************************************************************
        file-type:        "Fixed"
        text-encoding:    "sjis"
        record-length:    30
        record-separator: "\n"
        
        [Classifier]
        11  withUser    X(1)      # ユーザID使用フラグ (1:使用 0:不使用)
        
        [Type1Header]
        withUser = "1"
        1   requestId   X(10)     # リクエストID
        11  withUser    X(1)  "1" # ユーザID使用フラグ (1:使用 0:不使用)
        12  userId      X(10)     # ユーザID
        22  resendFlag  X(1)      # 再送要求フラグ
                                  # (0: 初回送信 / 1: 再送要求 / blank: 再送不要)
        23  statusCode  X(4)      # 処理結果コード
        27 ?reserved    X(4)      # 予備領域
        
        [Type2Header]
        withUser = "0"
        1   requestId   X(10)     # リクエストID
        11  withUser    X(1)  "0" # ユーザID使用フラグ (1:使用 0:不使用)
        12  resendFlag  X(1)      # 再送要求フラグ
                                  # (0: 初回送信 / 1: 再送要求 / blank: 再送不要)
        13  statusCode  X(4)      # 処理結果コード
        17 ?reserved    X(14)      # 予備領域
        *********************************************************************/
        headerFile.deleteOnExit();
        
        headerFormatter = FormatterFactory.getInstance()
                                          .createFormatter(headerFile);
    }
    
    public void sendBookData(String headerType) {
        String sentMessageId = provider.createContext().send(
            new SendingMessage()
               .setDestination("LOCAL.QUEUE")
               .setReplyTo("LOCAL.REPLY")
               .setFormatter(headerFormatter)
               .addRecord(headerType, new HashMap<String, Object>() {{
                    put("requestId", "bookdata");
                    put("userId",    "testUser");
                }})
               .setFormatter(bodyFormatter)
               .addRecord(new HashMap<String, Object>() {{
                    put("recordType", 1);
                    put("title",     "Principle of Transaction Processing");
                    put("publisher", "Morgan Kaufmann Publishers");
                    put("authors",   "Philip A. Bernstein");
                }})
        );
    }
    
    /**
     * フレームワーク制御ヘッダ項目をスレッドコンテキスへ設定する機能のテスト
     */
    @Test public void testSettingThreadContextVars() throws Exception {
        setupReader();
        setupFormatter();
        
        ThreadContext.setUserId("MSG_SVR"); // サーバ起動ユーザID
        ThreadContext.setRequestId(null);   // 未設定
        
        fwReader = new FwHeaderReader().setMessageReader(reader);
                
        // 送信してみる。
        sendBookData("Type1Header");
        RequestMessage req = fwReader.read(context);
        
        assertEquals("bookdata", req.getFwHeader().getRequestId());
        assertEquals("testUser", req.getFwHeader().getUserId());

        // スレッドコンテキストにフレームワーク制御ヘッダの値が設定される。
        assertEquals("testUser", ThreadContext.getUserId());
        assertEquals("bookdata", ThreadContext.getRequestId());
        assertEquals("bookdata", ThreadContext.getInternalRequestId());
        

        
        // ユーザIdを含まないフレームワーク制御ヘッダ定義を使用
        fwReader = new FwHeaderReader()
                      .setMessageReader(reader);
        
        // スレッドコンテキストの値をクリア
        ThreadContext.setUserId("MSG_SVR"); // サーバ起動ユーザID
        ThreadContext.setRequestId(null);   // 未設定
        ThreadContext.setInternalRequestId(null);
        
        // 送信してみる。(ユーザIDなしヘッダ)
        sendBookData("Type2Header");
        req = fwReader.read(context);
        
        assertEquals("MSG_SVR", ThreadContext.getUserId());
        assertEquals("bookdata", ThreadContext.getRequestId());
    }
    
    /**
     * 電文解析処理のテスト
     */
    @Test public void testRead() throws Exception {
        setupReader();
        setupFormatter();
        
        fwReader = new FwHeaderReader().setMessageReader(reader);
        
        // 送信してみる。
        sendBookData("Type1Header");
        
        // よめるはず。
        assertTrue(reader.hasNext(context));
        
        // よめた。
        RequestMessage req = fwReader.read(context);
        
        // フレームワーク制御ヘッダの確認
        assertEquals(req.getRequestPath(), "bookdata");
        
        FwHeader header = req.getFwHeader();
        assertEquals(header.getRequestId(), "bookdata");
        assertEquals(header.getUserId(),    "testUser");
        
        // ボディ部のフォーマッタは設定されていない。
        assertNull(req.getFormatter());
        // ボディ部はパースされていない。
        assertEquals(0, req.getRecords().size());
        
        req.setFormatter(bodyFormatter);
        
        // ボディ部を指定されたフォーマッタで読み込む。
        req.readRecords();
        
        assertEquals(1, req.getRecords().size());
        
        Map<String, Object> bookData = req.getRecordOf("Book");
        
        assertEquals("Principle of Transaction Processing", bookData.get("title"));
    }
    
    /**
     * フォーマッタ自動設定機能のテスト
     */
    @Test public void testSettingDefaultFormatter() throws Exception {
        setupReader();
        setupFormatter();
        
        fwReader = new FwHeaderReader()
                  .setMessageReader(reader)
                  .setFormatFileDir("format")
                  .setMessageFormatFileNamePattern("NBL_%s") //NBL_(リクエストID).fmt のファイルを読み込む。
                  .setReplyMessageFormatFileNamePattern("NBL_%s_REPLY"); //NBL_(リクエストID)_REPLY.fmt のファイルを読み込む。
        
        // メッセージ送信
        sendBookData("Type1Header");
        
        // メッセージ受信
        RequestMessage req = fwReader.read(context);
        
        // リクエストIDを確認
        String requestId = req.getFwHeader().getRequestId();
        assertEquals("bookdata", requestId);
        
        // [format]/NBL_bookdata.fmt がフォーマット定義として使用される。  
        // 自動読み込みは無効なので、ボディデータは読み込まれていない。
        assertEquals(0, req.getRecords().size());
        
        // ボディ部を指定されたフォーマッタで読み込む。
        req.readRecords();
        assertEquals(1, req.getRecords().size());
        Map<String, Object> bookData = req.getRecordOf("Book");
        assertEquals("Principle of Transaction Processing", bookData.get("title"));
        
        ResponseMessage reply = req.reply();
        assertNotNull(reply.getFormatter());
        
        assertEquals(0, reply.getRecords().size());
        //応答電文のフォーマッタを使用してデータを書き込む
        reply.addRecord(new HashMap() {{
            put("resultCode", "00000");
            put("processedCount", 100);
        }});
        
        assertEquals(1, reply.getRecords().size());
    }
    
    /**
     * 不正値を設定した場合の挙動のテスト
     */
    @Test public void testSettingIllegalValue() throws Exception {
        setupReader();
        setupFormatter();
        
        fwReader = new FwHeaderReader()
                  .setMessageReader(reader);

        try {
            fwReader.setFormatFileDir(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        try {
            fwReader.setMessageFormatFileNamePattern(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        try {
            fwReader.setReplyMessageFormatFileNamePattern(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        
        try {
            fwReader.setFwHeaderDefinition(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
        
        try {
            fwReader.setMessageReader(null);
            fail();
            
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    } 
}
