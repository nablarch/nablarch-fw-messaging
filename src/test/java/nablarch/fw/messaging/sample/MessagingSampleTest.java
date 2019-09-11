package nablarch.fw.messaging.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.launcher.CommandLine;
import nablarch.fw.launcher.Main;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.MessagingProvider;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import nablarch.fw.messaging.provider.TestJmsMessagingProvider.Context.JmsHeaderName;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.log.app.OnMemoryLogWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * メッセージ同期送信関連機能の結合テスト
 *
 * @author Iwauo Tajima
 */
@SuppressWarnings("serial")
@RunWith(DatabaseTestRunner.class)
public class MessagingSampleTest {

    @BeforeClass
    public static void setupClass() throws Exception {
        VariousDbTestHelper.createTable(SampleSentMessage.class);
        VariousDbTestHelper.createTable(BookData.class);
        VariousDbTestHelper.createTable(ErrorLog.class);

        VariousDbTestHelper.createTable(ProcessStatus.class);
        VariousDbTestHelper.createTable(MessagingTestMessage.class);
    }

    @Before
    public void setup() throws Exception {
        VariousDbTestHelper.delete(SampleSentMessage.class);
        VariousDbTestHelper.delete(BookData.class);
        VariousDbTestHelper.delete(ErrorLog.class);

        setupServer();
    }

    @After
    public void tearDown() throws Exception {
        try {
            TestEmbeddedMessagingProvider.stopServer();
            serverRunning = false;
            serverCausedError = false;
        } catch (Exception e) {
            throw e;
        } finally {
            System.clearProperty("receiveQueueName");
        }
    }

    /**MessagingSampleTest
     * 同期送受信の正常系処理
     */
    @Test public void testUsualRequestAndResponse() throws Exception {
        assertFalse(serverCausedError);

        // --------------- 書籍一覧を取得(0件) ----------------------- //
        new RetrivingBookListClient().execute();

        // プロトコルヘッダー(JMSヘッダ)の確認
        assertNotNull(clientReceivedMessage.getMessageId());
        assertNotNull(clientSentMessage.getMessageId());
        assertEquals(clientSentMessage.getMessageId(), clientReceivedMessage.getCorrelationId());
        assertEquals("BOOKKEEPER.REQUEST", clientSentMessage.getDestination());
        assertEquals("CLIENT.REPLY_FROM_BOOKKEEPER", clientSentMessage.getReplyTo());
        assertEquals("CLIENT.REPLY_FROM_BOOKKEEPER", clientReceivedMessage.getDestination());
        assertNull(clientReceivedMessage.getReplyTo());

        // 有効期限設定値の確認
        Long expire = clientReceivedMessage.getHeader(JmsHeaderName.EXPIRATION);
        Long created = clientReceivedMessage.getHeader(JmsHeaderName.TIMESTAMP);
        assertEquals(100 * 1000, expire - created);

        // FW制御ヘッダの確認
        DataRecord header = clientReceivedMessage
                           .setFormatter(getHeaderFormatter())
                           .readRecord();
        assertEquals("BookList", header.get("requestId"));
        assertEquals(null,       header.get("resendFlag"));
        assertEquals(null,       header.get("userId"));
        assertEquals("200",      header.get("statusCode"));

        // 業務データレコードの確認
        List<DataRecord> records = clientReceivedMessage
                                  .setFormatter(getBookDataFormatter())
                                  .readRecords();
        assertEquals(1, records.size());
        assertEquals("Summary", records.get(0).getRecordType());
        assertEquals(0, clientReceivedMessage.getRecordOf("Summary")
                                             .getBigDecimal("bookCount")
                                             .intValue());

        // --------------- 書籍情報を登録(2件) ----------------------- //
        new RegisteringBookClient().execute1();

        // FW制御ヘッダの確認
        header = clientReceivedMessage
                .setFormatter(getHeaderFormatter())
                .readRecord();
        assertEquals("RegisterBook", header.get("requestId"));
        assertEquals("0",            header.get("resendFlag"));
        assertEquals(null,           header.get("userId"));
        assertEquals("200",          header.get("statusCode"));

        // 業務データレコードの確認
        DataRecord summary = clientReceivedMessage
                            .setFormatter(getBookDataFormatter())
                            .readRecord();
        assertEquals("Summary", summary.getRecordType());
        // 2件正常終了
        assertEquals(2, summary.getBigDecimal("bookCount").intValue());

        // DB上に2件登録されたことを確認
        List<BookData> savedRows = VariousDbTestHelper.findAll(BookData.class);
        assertEquals(2, savedRows.size());

        // --------------- 書籍情報の一覧を再度取得 ----------------------- //
        new RetrivingBookListClient().execute();

        // FW制御ヘッダの確認
        header = clientReceivedMessage
                .setFormatter(getHeaderFormatter())
                .readRecord();
        assertEquals("200", header.get("statusCode"));
        assertEquals(null,  header.get("resendFlag"));
        assertEquals(null,  header.get("userId"));
        assertEquals("200", header.get("statusCode"));

        records = clientReceivedMessage
                 .setFormatter(getBookDataFormatter())
                 .readRecords();

        assertEquals(3, records.size());
        DataRecord trailer = records.get(2);
        assertEquals(2, trailer.getBigDecimal("bookCount").intValue());
    }

    /**
     * 再送要求処理(正常系)
     */
    @Test public void testResendingRequest() throws Exception {
        assertFalse(serverCausedError);

        // リストは空
        List<BookData> books = VariousDbTestHelper.findAll(BookData.class);
        assertEquals(0, books.size());

        // --------------- 書籍情報を登録(2件) ----------------------- //
        new RegisteringBookClient().execute1();

        // 2件登録されている。
        books = VariousDbTestHelper.findAll(BookData.class);
        assertEquals(2, books.size());

        // FW制御ヘッダの確認
        Map<String, Object> header = clientReceivedMessage
                                    .setFormatter(getHeaderFormatter())
                                    .readRecord();

        assertEquals("RegisterBook", header.get("requestId"));
        assertEquals("0",            header.get("resendFlag"));
        assertEquals(null,           header.get("userId"));
        assertEquals("200",          header.get("statusCode"));

        // 業務データレコードの確認 (2件登録完了)
        DataRecord summary = clientReceivedMessage
                            .setFormatter(getBookDataFormatter())
                            .readRecord();
        assertEquals(2, summary.getBigDecimal("bookCount").intValue());

        // 送信した電文のメッセージID
        String sentMessageId = clientSentMessage.getMessageId();

        // 初回要求電文に対する応答電文が、
        // 再送電文テーブル上に1件だけ保存されていることを確認。
        List<SampleSentMessage> rows = VariousDbTestHelper.findAll(SampleSentMessage.class);
        assertEquals(1, rows.size());
        assertEquals(sentMessageId,  rows.get(0).messageId);
        assertEquals("RegisterBook", rows.get(0).requestId);


        // 再送要求電文を送信
        new RegisteringBookClient(sentMessageId, "1").execute1();

        // FW制御ヘッダの確認 (正常終了)
        header = clientReceivedMessage
                .setFormatter(getHeaderFormatter())
                .readRecord();
        assertEquals("RegisterBook", header.get("requestId"));
        assertEquals("0",            header.get("resendFlag"));
        assertEquals(null,           header.get("userId"));
        assertEquals("200",          header.get("statusCode"));

        // 業務データレコードの確認 (2件登録完了)
        summary = clientReceivedMessage
                 .setFormatter(getBookDataFormatter())
                 .readRecord();
        assertEquals(2, summary.getBigDecimal("bookCount").intValue());

        // ...しかし再送ハンドラで折り返しているので、
        //    実際にはデータは追加登録されていない。(2件のまま)
        books = VariousDbTestHelper.findAll(BookData.class);
        assertEquals(2, books.size());

        // 再送用電文も1件のまま
        rows = VariousDbTestHelper.findAll(SampleSentMessage.class);
        assertEquals(1, rows.size());
    }


    /**
     * 再送要求処理のエラー応答がらみのテスト。
     */
    @Test public void testResendingRequestToWhicthServserRepliedAsError() throws Exception {
        assertFalse(serverCausedError);

        // データレコードと一致しないサマリーレコードをもつ登録要求電文を送信。
        // （サーバ側で業務エラーになる）
        new RegisteringBookClient(new HashMap<String, Object>(){{
            put("bookCount", 1000); //全然ちがう。
        }}).execute1();

        // 送信した電文のメッセージID
        String sentMessageId = clientSentMessage.getMessageId();

        Map<String, Object> header = clientReceivedMessage
                                    .setFormatter(getHeaderFormatter())
                                    .readRecord();
        assertEquals("RegisterBook", header.get("requestId"));
        assertEquals("0",            header.get("resendFlag"));
        assertEquals(null,           header.get("userId"));
        assertEquals("400",          header.get("statusCode"));

        // エラーによる業務処理がロールバックされているので
        // DBへの登録はなし。
        List<BookData> books = VariousDbTestHelper.findAll(BookData.class);
        assertEquals(0, books.size());

        // エラー処理で、ERR_LOGテーブルへ登録しているため、
        // エラーログにレコードが1レコード追加される。
        List<ErrorLog> errorMessages = VariousDbTestHelper.findAll(ErrorLog.class);
        assertEquals(1, errorMessages.size());
        assertEquals("failed to register for book.", errorMessages.get(0).errorMessage);

        // 再送用電文の保存は業務トランザクションが正常終了した場合のみ。
        // そのため、今回は再送用電文テーブルには保存されない。
        List<SampleSentMessage> sentMessages = VariousDbTestHelper.findAll(SampleSentMessage.class);
        assertEquals(0, sentMessages.size());

        // 正しいデータ部を用いて再送。
        // 再送電文送信時点で、当該の電文が登録されていない場合、
        // 再送電文を初回電文として扱う。
        new RegisteringBookClient(sentMessageId, "1").execute1();

        // FW制御ヘッダの確認 (正常終了)
        header = clientReceivedMessage
                .setFormatter(getHeaderFormatter())
                .readRecord();
        assertEquals("RegisterBook", header.get("requestId"));
        assertEquals("1",            header.get("resendFlag"));
        assertEquals(null,           header.get("userId"));
        assertEquals("200",          header.get("statusCode"));

        // 業務データレコードの確認 (2件登録完了)
        DataRecord summary = clientReceivedMessage
                            .setFormatter(getBookDataFormatter())
                            .readRecord();
        assertEquals(2, summary.getBigDecimal("bookCount").intValue());

        // 再送用電文が初回実行されたため、その応答電文が再送用電文テーブルに格納される。
        sentMessages = VariousDbTestHelper.findAll(SampleSentMessage.class);
        assertEquals(1, sentMessages.size());

        // 再送電文により初回実行された場合、後から到着した初回電文は再送要求と同じ
        // 扱いとなる。   
    }

    /**
     * 不正電文に対するエラー制御のテスト
     */
    @Test public void testHandlingOfInvalidRequest() throws Exception {
        assertFalse(serverCausedError);

        MessagingContext messaging = getMessagingContext();

        // フレームワーク制御ヘッダなどが一切ない空の電文を送信。
        SendingMessage message = new SendingMessage()
                                .setDestination("BOOKKEEPER.REQUEST")
                                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER");

        ReceivedMessage reply = messaging.sendSync(message);
        DataRecord header = reply.setFormatter(getHeaderFormatter())
                                 .readRecord();
        // エラー応答電文が返る。
        assertEquals("500", header.get("statusCode"));
    }

    /**
     * 業務アクション側でプロセス停止エラーを送出するケース。
     */
    @Test
    public void testHaltsRunningProcessIfProcessAbnormalEndErrorIsThrown() throws Exception {
        assertFalse(serverCausedError);

        OnMemoryLogWriter.clear();

        new HorribleClient().execute();

        // 停止制御完了まち
        Thread.sleep(3000);

        // FATALログの出力は1回のみ
        List<String> logs = OnMemoryLogWriter.getMessages("writer.monitorLog");
        List<String> fatalLogs = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains("FATAL")) {
                fatalLogs.add(log);
            }
        }
        assertEquals(1, fatalLogs.size());

        // プロセスが停止していることを確認
        assertFalse(serverRunning);
        assertFalse(serverCausedError);
        assertEquals(199, resultCode.intValue()); // ProcessAbnormalEnd                 
    }

    /**
     * 要求電文の受信処理中にリトライ可能エラー(DBコネクションの切断など)
     * が発生するケース。
     */
    @Test
    public void testOccursRetriableError() throws Exception {
        assertFalse(serverCausedError);
        OnMemoryLogWriter.clear();

        new RetryClient().execute(); // プロセス継続
        Thread.sleep(1000);
        new RetryClient().execute(); // プロセス継続
        Thread.sleep(1000);
        new RetryClient().execute(); // プロセス継続 (リトライ上限)
        Thread.sleep(1000);
        new RetryClient().execute(); // プロセス異常停止 (リトライ上限超過)

        Thread.sleep(5000);

        // FATALログの出力は1回のみ
        List<String> logs = OnMemoryLogWriter.getMessages("writer.monitorLog");
        List<String> fatalLogs = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains("FATAL")) {
                fatalLogs.add(log);
            }
        }
        assertEquals(1, fatalLogs.size());

        // リトライ実施ごとにアプリケーションログ上にWARNログがでる。
        logs = OnMemoryLogWriter.getMessages("writer.appLog");
        List<String> warnLogs = new ArrayList<String>();
        for (String log : logs) {
            if (log.contains("WARN") && log.contains("retryCount")) {
                warnLogs.add(log);
            }
        }
        assertEquals(3, warnLogs.size());

        // プロセス停止確認
        assertFalse(serverRunning);
        assertFalse(serverCausedError);
        assertEquals(180, resultCode.intValue()); // ProcessAbnormalEnd
    }

    /**
     * 要求電文の受信処理中にシステムエラーが発生する場合の挙動の確認。
     */
    @Test public void testOccursErrorDuringWaitingForReplayMessage() throws Exception {
        assertFalse(serverCausedError);

        Thread.sleep(3000);
        // 受信用キューを削除する。
        TestEmbeddedMessagingProvider provider = SystemRepository.get("messagingProvider");
        provider.setQueueNames(Arrays.asList(new String[] {"hoge"}));

        // リトライハンドラのリトライ上限設定(3回)のエラーののち、
        // プロセスが異常終了する。(abnormalerror)
        Thread.sleep(20000);
        assertFalse(serverRunning);
        assertFalse(serverCausedError);
        assertEquals(180, resultCode.intValue()); // ProcessAbnormalEnd

        resetSystemRepository();
    }

    public void resetSystemRepository() throws Exception {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("classpath:nablarch/fw/messaging/sample/diConfig.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    private ReceivedMessage clientReceivedMessage = null;
    private SendingMessage  clientSentMessage = null;

    private DataRecordFormatter getBookDataFormatter() {
        return FormatterFactory
              .getInstance()
              .createFormatter(
                   FilePathSetting.getInstance()
                                  .getFileIfExists("format", "BookData.fmt")
               );
    }


    private DataRecordFormatter getHeaderFormatter() {
        return FormatterFactory
              .getInstance()
              .createFormatter(
                   FilePathSetting.getInstance()
                                  .getFileIfExists("format", "header.fmt")
               );
    }


    /**
     * 同期応答サーバを起動する。
     * (以下のコマンド同等)
     * java                                                                \
     *   -DreceiveQueueName=BOOKKEEPER.REQUEST                             \
     *   nablarch.fw.launcher.Main                                         \
     *   -diConfig     classpath:nablarch/fw/messaging/sample/diConfig.xml \
     *   -requestPath  BookkeeperMessagingServer/serverNode01              \
     *   -userId       nobody
     */
    public static void setupServer() throws Exception {

        VariousDbTestHelper.setUpTable(
                new ProcessStatus("serverNode01", "0", "1"),
                new ProcessStatus("BookList", "0", "1"),
                new ProcessStatus("RegisterBook", "0", "1"),
                new ProcessStatus("HorribleError", "0", "1"),
                new ProcessStatus("Retry", "0", "1"));

        VariousDbTestHelper.setUpTable(
                new MessagingTestMessage("10001", "ja", "メッセージ001"),
                new MessagingTestMessage("10001", "en", "Message001"));

        new Thread() {
            public void run() {
                serverRunning = true;
                serverCausedError = false;
                resultCode = null;

                try {
                    System.setProperty("receiveQueueName", "BOOKKEEPER.REQUEST");
                    resultCode = Main.execute(new CommandLine(
                    "-diConfig",    "classpath:nablarch/fw/messaging/sample/diConfig.xml",
                    "-requestPath", "BookkeeperMessagingServer/serverNode01",
                    "-userId",      "nobody"
                    ));

                } catch (Exception e) {
                    serverCausedError = true;
                } catch (Error e) {
                    serverCausedError = true;
                }
                serverRunning = false;
            }
        }.start();
        TestEmbeddedMessagingProvider.waitUntilServerStarted();
    }
    private static boolean serverRunning     = false;
    private static boolean serverCausedError = false;
    private static Integer resultCode        = null;

    private MessagingContext getMessagingContext() throws Exception {
        MessagingProvider messagingProvider = null;
        while(messagingProvider == null) {
            // リポジトリの初期化が間に合わない場合がある。
            messagingProvider = SystemRepository.get("messagingProvider");
            Thread.sleep(2000);
        }
        return messagingProvider.createContext();
    }

    /**
     * データ一覧を要求するクライアント
     */
    private class RetrivingBookListClient  {
        public void execute() throws Exception {
            MessagingContext messaging = getMessagingContext();

            SendingMessage message = new SendingMessage()
                .setFormatter(getHeaderFormatter())
                .addRecord(new HashMap<String, Object>() {{
                     put("requestId",  "BookList"); // 蔵書一覧
                     put("resendFlag", "");         // 照会電文なので再送制御は使わなくてもいい。
                 }})
                .setDestination("BOOKKEEPER.REQUEST")
                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER");

            ReceivedMessage reply = messaging.sendSync(message);

            MessagingSampleTest.this.clientReceivedMessage = reply;
            MessagingSampleTest.this.clientSentMessage = message;
        }
    }

    /**
     * データ登録電文を送信するクライアント
     */
    private class RegisteringBookClient {
        /**
         * 初回電文を送信する。
         */
        public RegisteringBookClient() {
            this(null, "0");
        }

        /**
         * トレーラレコード付の初回電文を送信する。
         * @param summaryRecord トレーラレコードの内容
         */
        public RegisteringBookClient(Map<String, Object> summaryRecord) {
            super();
            this.summaryRecord = summaryRecord;
            this.messageId     = null;
            this.resendFlag    = "0";
        }

        /**
         * 再送要求を送信する。
         * @param messageId  再送対象のメッセージID
         * @param resendFlag 再送フラグ
         */
        public RegisteringBookClient(String messageId, String resendFlag) {
            super();
            this.messageId  = messageId;
            this.resendFlag = resendFlag;
        }

        private final String messageId;
        private final String resendFlag;
        private Map<String, Object> summaryRecord = new HashMap<String, Object>();

        public void execute1() throws Exception {
            MessagingContext messaging = getMessagingContext();

            if (!summaryRecord.containsKey("bookCount")) {
                summaryRecord.put("bookCount", 2);
            }

            SendingMessage message = new SendingMessage()
                .setFormatter(getHeaderFormatter())
                .setDestination("BOOKKEEPER.REQUEST")
                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER")
                .addRecord(new HashMap<String, Object>() {{
                     put("requestId",  "RegisterBook"); // 書籍情報登録
                     put("resendFlag", resendFlag);     // 再送フラグ
                 }})
                .setFormatter(getBookDataFormatter())
                .addRecord("Book", new HashMap<String, Object>() {{
                     put("title",     "Learning the vi and vim Editors");
                     put("publisher", "OReilly");
                     put("authors",   "Robbins Hanneah and Lamb");
                 }})
                .addRecord("Book", new HashMap<String, Object>() {{
                     put("title",     "Programming with POSIX Threads");
                     put("publisher", "Addison-Wesley");
                     put("authors",   "David R. Butenhof");
                 }})
                .addRecord("Summary", summaryRecord);

            if (messageId != null) {
                message.setCorrelationId(messageId);
            }
            ReceivedMessage reply = messaging.sendSync(message);
            MessagingSampleTest.this.clientReceivedMessage = reply;
            MessagingSampleTest.this.clientSentMessage = message;
        }

        public void execute2() throws Exception {
            MessagingContext messaging = getMessagingContext();

            if (!summaryRecord.containsKey("bookCount")) {
                summaryRecord.put("bookCount", 1);
            }

            SendingMessage message = new SendingMessage()
                .setFormatter(getHeaderFormatter())
                .setDestination("BOOKKEEPER.REQUEST")
                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER")
                .addRecord(new HashMap<String, Object>() {{
                     put("requestId",  "RegisterBook"); // 書籍情報登録
                     put("resendFlag", resendFlag);     // 再送フラグ
                 }})
                .setFormatter(getBookDataFormatter())
                .addRecord("Book", new HashMap<String, Object>() {{
                    put("title",     "HACKING (2nd ed)");
                    put("publisher", "no starch press");
                    put("authors",   "Jon Erickson");
                 }})
                .addRecord("Summary", summaryRecord);

            if (messageId != null) {
                message.setCorrelationId(messageId);
            }
            ReceivedMessage reply = messaging.sendSync(message);
            MessagingSampleTest.this.clientReceivedMessage = reply;
            MessagingSampleTest.this.clientSentMessage = message;
        }
    }

    /**
     * 深刻なエラーを引き起こす電文を送信するクライアント
     */
    private class HorribleClient  {
        public void execute() throws Exception {
            MessagingContext messaging = getMessagingContext();

            SendingMessage message = new SendingMessage()
                .setFormatter(getHeaderFormatter())
                .addRecord(new HashMap<String, Object>() {{
                     put("requestId",  "HorribleError");
                     put("resendFlag", "");
                 }})
                .setDestination("BOOKKEEPER.REQUEST")
                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER");

            ReceivedMessage reply = messaging.sendSync(message);

            MessagingSampleTest.this.clientReceivedMessage = reply;
            MessagingSampleTest.this.clientSentMessage = message;
        }
    }

    /**
     * リトライ(DBコネクションの一時切断等)が発生するケース
     */
    private class RetryClient  {
        public void execute() throws Exception {
            MessagingContext messaging = getMessagingContext();

            SendingMessage message = new SendingMessage()
                .setFormatter(getHeaderFormatter())
                .addRecord(new HashMap<String, Object>() {{
                     put("requestId",  "Retry");
                     put("resendFlag", "");
                 }})
                .setDestination("BOOKKEEPER.REQUEST")
                .setReplyTo("CLIENT.REPLY_FROM_BOOKKEEPER");

            ReceivedMessage reply = messaging.sendSync(message);

            MessagingSampleTest.this.clientReceivedMessage = reply;
            MessagingSampleTest.this.clientSentMessage = message;
        }
    }
}
