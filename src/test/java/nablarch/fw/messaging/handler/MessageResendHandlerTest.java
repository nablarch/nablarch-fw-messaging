package nablarch.fw.messaging.handler;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.MessagingProvider;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.StandardFwHeaderDefinition;
import nablarch.fw.messaging.provider.TestEmbeddedMessagingProvider;
import nablarch.fw.messaging.tableschema.SentMessageTableSchema;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.tool.Hereis;
import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import javax.persistence.Table;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(DatabaseTestRunner.class)
public class MessageResendHandlerTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/fw/messaging/handler/MessageResendHandlerTest.xml");

    /** 180秒で終わって欲しい */
    @Rule
    public Timeout timeout = new Timeout(180000);

    @BeforeClass
    public static void setUpClass() {
        FormatterFactory.getInstance().setCacheLayoutFileDefinition(false);
        VariousDbTestHelper.createTable(SentMessage.class);
        VariousDbTestHelper.createTable(TblSentMessage.class);
    }

    @Before
    public void setup() {
        VariousDbTestHelper.delete(SentMessage.class);
        VariousDbTestHelper.delete(TblSentMessage.class);
    }

    @Test
    public void savingReplyMessageToDB() throws Exception {
        setupFormatter();
        context = setupExecutionContext();
        actionCalled = false;

        // 送信済み電文テーブルは空
        List<SentMessage> records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 0);

        // ハンドラキューを実行
        RequestMessage  req = createRequest();
        ResponseMessage res = context.handleNext(req);

        // 末端の業務アクションの実行を確認
        assertTrue(actionCalled);
        // 応答電文の内容を確認
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());
        assertEquals("RegisterBook", res.getFwHeader().getRequestId());

        // 送信済み電文テーブルの内容を確認
        records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);

        assertEquals("request_alice_to_bob_0001", records.get(0).messageId);
        assertEquals("RegisterBook", records.get(0).requestId);
        assertEquals("ALICE.REPLY_FROM_BOB", records.get(0).replyQueue);
        assertEquals("200", records.get(0).statusCode);
        InputStream in = new ByteArrayInputStream(records.get(0).bodyData);
        assertEquals("RegisterBook", req.getFwHeader().getRequestId());

        // 実行コンテキストを再構築
        context = setupExecutionContext();
        actionCalled = false;

        // 同じ内容の電文を送信
        res = context.handleNext(createRequest());

        // 再送制御により、業務アクションの実行はスキップされる。
        assertFalse(actionCalled);

        // 応答電文の内容を確認(前回と同じ内容)
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());

        // 送信済み電文テーブルの内容を確認(前回と変わらず。)
        records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);
        assertEquals("request_alice_to_bob_0001", records.get(0).messageId);
        assertEquals("RegisterBook", records.get(0).requestId);
    }


    /**
     * 再送応答フラグが設定されていない電文については、何もせずに後続処理に委譲する。
     */
    @Test
    public void ignoringReceivedMessagesIfTheyDoNotHaveResendingFlag() throws Exception {
        setupFormatter();
        context = setupExecutionContext();
        actionCalled = false;

        // 送信済み電文テーブルは空
        List<SentMessage> records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 0);

        // ハンドラキューを実行
        RequestMessage  req = createRequest();

        // 再送応答フラグを除去
        req.getFwHeader().remove("resendFlag");

        ResponseMessage res = context.handleNext(req);

        // 末端の業務アクションの実行を確認
        assertTrue(actionCalled);

        // 応答電文の内容を確認
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());
        assertEquals("RegisterBook", res.getFwHeader().getRequestId());

        // 電文は保存されていない。
        records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 0);
    }

    /**
     * 再送要求電文を受信した時点でまだ初回電文を受信していなかった場合は、再送要求電文側を
     * 初回要求として扱う。
     * その後、初回電文を受信した場合はこの結果を再送する。
     */
    @Test
    public void aResendRequestIsTreatedAsAFirstRequestIfThatMessageHasNotReachedByTheTime()
    throws Exception {
        setupFormatter();
        context = setupExecutionContext();
        actionCalled = false;

        // ハンドラキューを実行
        RequestMessage  req = createResendRequest(); // 再送要求電文
        ResponseMessage res = context.handleNext(req); // 送信

        // 初回電文が未実行であるので、この電文を初回電文として
        // 業務アクションまで通常どおり実行する。
        assertTrue(actionCalled);

        // 応答電文の内容を確認
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());
        assertEquals("RegisterBook", res.getFwHeader().getRequestId());

        // 送信済み電文テーブルの内容を確認
        List<SentMessage> records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);

        // 実行コンテキストを再構築
        context = setupExecutionContext();
        actionCalled = false;

        // 初回電文を後から受信
        res = context.handleNext(createRequest());

        // 再送制御により、業務アクションの実行はスキップされる。
        assertFalse(actionCalled);

        // 応答電文の内容を確認(前回と同じ内容)
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());

        // 送信済み電文テーブルの内容を確認(前回と変わらず。)
        records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);

        assertEquals("request_alice_to_bob_0001", records.get(0).messageId);
        assertEquals("RegisterBook", records.get(0).requestId);

    }

    /**
     * 初回電文と再送要求電文が並行実行された場合、より先に完了した処理をコミットし、
     * もう一方はロールバックする。
     * ロールバックされた電文は再送電文を応答する。
     */
    @Test
    public void ifFirstRequestAndResentOneAreProcessedConcurrentlyOnlyTheMessageCompletedMoreEarlierHasToBeCommited() throws Exception {
        setupFormatter();
        actionCalled = false;

        // 業務処理のロックを立てる。
        actionWorkingLatch   = new CountDownLatch(1);
        actionCompletedLatch = new CountDownLatch(1);

        // 初回電文
        final RequestMessage req = createRequest();
        // 初回電文の応答
        final List<ResponseMessage> replyOfFirstRequest = new ArrayList<ResponseMessage>(1);

        // 初回電文を送信。(処理途中でブロック)
        new Thread() {
            public void run() {
                try {
                    replyOfFirstRequest.add(
                        (ResponseMessage) setupExecutionContext().handleNext(req)
                    ); // 送信
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();

        actionWorkingLatch.await();
        actionWorkingLatch = null;

        // 再送電文を送信。
        // -> 初回電文よりも先に完了する。
        ResponseMessage res = setupExecutionContext().handleNext(createResendRequest()); // 送信

        // 応答電文の内容を確認(初回電文として正常終了)
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());

        // 送信済み電文テーブルの内容を確認(初回電文として正常終了)
        List<SentMessage> records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);

        assertEquals("request_alice_to_bob_0001", records.get(0).messageId);
        assertEquals("RegisterBook", records.get(0).requestId);

        // 初回電文のロックを開放する。
        actionCompletedLatch.countDown();

        // 応答電文の内容を確認(応答再送により正常終了)
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());

        // 送信済み電文テーブルの内容を確認(再送なので追加なし。)
        records = VariousDbTestHelper.findAll(SentMessage.class);
        assertEquals(records.size(), 1);
    }

    /**
     * カスタムのテーブルスキーマを使用した場合のテスト
     */
    @Test
    public void settingTableSchemaThroughItsAccessors() throws Exception {
        MessageResendHandler resendHandler = new MessageResendHandler()
           .setSentMessageTableSchema(new SentMessageTableSchema()
                .setTableName("TBL_SENT_MESSAGE")
                .setMessageIdColumnName("CLM_MESSAGE_ID")
                .setRequestIdColumnName("CLM_REQUEST_ID")
                .setReplyQueueColumnName("CLM_REPLY_QUEUE")
                .setStatusCodeColumnName("CLM_STATUS_CODE")
                .setBodyDataColumnName("CLM_BODY_DATA")
            );

        setupFormatter();
        context = setupExecutionContext(resendHandler);
        actionCalled = false;

        // 送信済み電文テーブルは空
        List<TblSentMessage> records = VariousDbTestHelper.findAll(TblSentMessage.class);
        assertEquals(records.size(), 0);

        // ハンドラキューを実行
        RequestMessage  req = createRequest();
        ResponseMessage res = context.handleNext(req);

        // 末端の業務アクションの実行を確認
        assertTrue(actionCalled);
        // 応答電文の内容を確認
        assertEquals(200, res.getStatusCode());
        assertEquals("request_alice_to_bob_0001", res.getCorrelationId());
        assertEquals("RegisterBook", res.getFwHeader().getRequestId());

        // 送信済み電文テーブルの内容を確認
        records = VariousDbTestHelper.findAll(TblSentMessage.class);
        assertEquals(records.size(), 1);

        assertEquals("request_alice_to_bob_0001", records.get(0).clmMessageId);
        assertEquals("RegisterBook", records.get(0).clmRequestId);
        assertEquals("ALICE.REPLY_FROM_BOB", records.get(0).clmReplyQueue);
        assertEquals("200", records.get(0).clmStatusCode);
        InputStream in = new ByteArrayInputStream(records.get(0).clmBodyData);
        assertEquals("RegisterBook", req.getFwHeader().getRequestId());
    }

    public ExecutionContext setupExecutionContext() {
        MessageResendHandler resendHandler = new MessageResendHandler();
        resendHandler.setSentMessageTableSchema(tableDef);
        return setupExecutionContext(resendHandler);
    }

    public ExecutionContext setupExecutionContext(MessageResendHandler resendHandler) {
        class Responder implements Handler<RequestMessage, ResponseMessage> {
            public ResponseMessage handle(RequestMessage req, ExecutionContext ctx) {
                MessageResendHandlerTest.this.actionCalled = true;
                if (actionWorkingLatch != null) {
                    try {
                        actionWorkingLatch.countDown();
                        // a long running task...
                        actionCompletedLatch.await();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return req.reply();
            }
        }
        return new ExecutionContext()
            .addHandler(repositoryResource.getComponent("dbConnectionManagementHandler"))
            .addHandler(repositoryResource.getComponent("transactionManagementHandler"))
            .addHandler(resendHandler)
            .addHandler(new Responder());
    }

    /** デフォルトのテスト自のスキーマ設定 */
    private SentMessageTableSchema tableDef =
            new SentMessageTableSchema()
                    .setTableName(SentMessage.class.getAnnotation(Table.class).name());

    private ExecutionContext context     = null;
    private CountDownLatch   actionWorkingLatch   = null;
    private CountDownLatch   actionCompletedLatch = null;
    private boolean actionCalled = false;

    private DataRecordFormatter dataFormatter;
    private DataRecordFormatter headerFormatter;

    public RequestMessage createRequest() throws Exception {
        Map<String, Object> fwHeader = new HashMap<String, Object>() {{
            put("requestId",  "RegisterBook");
            put("resendFlg",  "0");
            put("resultCode", "");
        }};
        return createRequest(fwHeader);
    }

    public RequestMessage createResendRequest() throws Exception {
        Map<String, Object> fwHeader = new HashMap<String, Object>() {{
            put("requestId",  "RegisterBook");
            put("resendFlg",  "1");  // 再送要求
            put("resultCode", "");
        }};
        return createRequest(fwHeader);
    }


    public RequestMessage createRequest(Map<String, Object> fwHeader)
    throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Object> book1 = new HashMap<String, Object>() {{
            put("title",     "Programming with POSIX Threads");
            put("publisher", "Addison-Wesley");
            put("authors",   "David R. Butenhof");
        }};
        Map<String, Object> book2 = new HashMap<String, Object>() {{
            put("title",     "HACKING (2nd ed)");
            put("publisher", "no starch press");
            put("authors",   "Jon Erickson");
        }};
        Map<String, Object> trailer = new HashMap<String, Object>() {{
            put("bookCount", 2);
        }};
        headerFormatter.setOutputStream(out).initialize()
                       .writeRecord(fwHeader);
        dataFormatter.setOutputStream(out).initialize()
                     .writeRecord(book1);
        dataFormatter.writeRecord(book2);
        dataFormatter.writeRecord(trailer);


        FwHeaderDefinition def = new StandardFwHeaderDefinition()
                                .setFormatFileDir("format")
                                .setFormatFileName("header");

        RequestMessage req = def.readFwHeaderFrom(
                                 new ReceivedMessage(out.toByteArray())
                             );
        req.setFormatter(dataFormatter)
           .setMessageId("request_alice_to_bob_0001")
           .setDestination("BOB.REQUEST_FROM_ALICE")
           .setReplyTo("ALICE.REPLY_FROM_BOB")
           .readRecords();

        return req;
    }

    public void setupFormatter() throws Exception {
        File headerFormat = Hereis.file("./header.fmt");
        /**********************************************
        file-type:        "Fixed"
        text-encoding:    "sjis"
        record-length:    30
        record-separator: "\n"

        [Header]
        1   requestId   X(20)      # リクエストID
        21  resendFlag  X(1)  "0"  # 再送要求フラグ (0: 初回送信 1: 再送要求)
        22  resultCode  X(4)       # 処理結果コード
        26 ?reserved    X(5)       # 予備領域
        ***************************************************/

        FilePathSetting.getInstance()
                       .addBasePathSetting("format", "file:./")
                       .addFileExtensions("format", "fmt");

        headerFormatter = FormatterFactory.getInstance().createFormatter(
            FilePathSetting.getInstance()
                           .getFileIfExists("format", "header")
        );

        File dataFormat = Hereis.file("./RegisterBook.fmt");
        /**********************************************
        file-type:        "Fixed"
        text-encoding:    "sjis"
        record-length:    200
        record-separator: "\n"

        [Data]
        1   title      X(50)    # 書名
        51  publisher  X(50)    # 出版社
        101 authors    X(100)   # 著者

        [Trailer]
        1  bookCount   Z(5)    # レコード数
        6 ?reserved    X(195)  # 予備領域
        **********************************************/
        dataFormat.deleteOnExit();

        dataFormatter = FormatterFactory.getInstance().createFormatter(
            FilePathSetting.getInstance()
                           .getFileIfExists("format", "RegisterBook")
        );
    }

    public void setupMessagingProvider() throws Exception {
        messagingProvider = new TestEmbeddedMessagingProvider()
            .setQueueNames(Arrays.asList(new String[] {
               "BOB.REQUEST_FROM_ALICE" // 要求受信キュー(BOB)
             , "ALICE.REPLY_FROM_BOB"   // 応答受信キュー(ALICE)
             , "BOB.POISON"             // 電文退避キュー(BOB)
             }));
    }
    private MessagingProvider messagingProvider = null;
}
