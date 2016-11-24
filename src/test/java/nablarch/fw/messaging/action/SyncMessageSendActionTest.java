package nablarch.fw.messaging.action;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.List;


import nablarch.fw.launcher.CommandLine;
import nablarch.fw.launcher.Main;
import nablarch.fw.messaging.MessageSender;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.SyncMessage;
import nablarch.test.core.messaging.EmbeddedMessagingProvider;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import nablarch.test.support.log.app.OnMemoryLogWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * {@link MessageSender}をバッチ処理で使用できることのテスト。
 * @author Kiyohito Itoh
 */
@RunWith(DatabaseTestRunner.class)
public class SyncMessageSendActionTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("nablarch/fw/messaging/action/SyncMessageSendActionTest.xml");

    private MessagingContext context;

    /**
     * テストケース単位の事前準備処理。
     * <p/>
     * テストで使用するテーブルの構築を行う。
     */
    @Before
    public void setUp() {

    	VariousDbTestHelper.createTable(MessagingBatchRequest.class);
    	VariousDbTestHelper.createTable(MessagingBook.class);
    	
    	VariousDbTestHelper.setUpTable(
    			new MessagingBatchRequest("R000000001", "リクエスト０１", "0", "0", "1"),
    			new MessagingBatchRequest("R000000002", "リクエスト０２", "0", "0", "1"),
    			new MessagingBatchRequest("R000000003", "リクエスト０３", "0", "0", "1"),
    			new MessagingBatchRequest("R000000004", "リクエスト０４", "0", "0", "1"));
    	
    	VariousDbTestHelper.setUpTable(
    			new MessagingBook("B000000001", "title001", "publisher002", "authors003", "0"));
    }

    /** 本テストクラスの終了処理 */
    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
        EmbeddedMessagingProvider.stopServer();
    }

    /**
     * タイムアウトした場合
     */
    @Test
    public void testTimeout() throws Exception {
        String userId = "batchUser1";
        String requestId = "R000000002";
        String actionClassName = SyncMessageSendAction.class.getSimpleName();
        OnMemoryLogWriter.clear();
        int statusCode = executeBatchAction(userId, requestId, actionClassName);
        assertThat(statusCode, is(100));
        assertNull(SyncMessageSendAction.lastResponseMessage);
        assertMessagingLog("INFO MESSAGING response timeout: could not receive a reply to the message below within 3000msec. @@@@ SENT MESSAGE @@@@", 4);
    }

    private void assertMessagingLog(String expectedLogHeader, int expectedLogCount) {
        List<String> logs = OnMemoryLogWriter.getMessages("writer.accessLog");
        int logCount = 0;
        for (String log : logs) {
            if (log.indexOf(expectedLogHeader) != -1) {
                logCount++;
            }
        }
        assertThat(logCount, is(expectedLogCount));
    }

    /**
     * メッセージが送信されること。
     */
    @Test
    public void testSendSync() throws Exception {
        new Thread(new SyncMessageSendTestUtil.CreateReceivedMessage()).start();
        String userId = "batchUser1";
        String requestId = "R000000001";
        String actionClassName = SyncMessageSendAction.class.getSimpleName();
        OnMemoryLogWriter.clear();
        int statusCode = executeBatchAction(userId, requestId, actionClassName);
        assertThat(statusCode, is(0));
        SyncMessage responseMessage = SyncMessageSendAction.lastResponseMessage;
        SyncMessageSendAction.lastResponseMessage = null;
        assertThat(responseMessage.getHeaderRecord().get("requestId").toString(), is("RM11AD0101"));
        assertNotNull(responseMessage.getHeaderRecord().get("resendFlag"));
        assertThat(responseMessage.getDataRecord().get("failureCode").toString(), is("1234567890"));
        assertThat(responseMessage.getDataRecord().get("userInfoId").toString(), is("title001_publisher002_authors003"));
        // テスト対象の送信->受信1回ずつと応答電文を返すための受信->送信1回ずつ
        assertMessagingLog("INFO MESSAGING @@@@ SENT MESSAGE @@@@", 2);
        assertMessagingLog("INFO MESSAGING @@@@ RECEIVED MESSAGE @@@@", 2);
    }

    /**
     * テスト対象のバッチアクションを実行する。
     *
     * @param userId ユーザID
     * @param requestId リクエストID
     * @param actionClassName アクションクラス名
     * @return 処理結果
     */
    private static int executeBatchAction(String userId, String requestId, String actionClassName) {
    	
    	VariousDbTestHelper.setUpTable(
    			new MessagingBatchRequest("R000000001", "リクエスト０１", "0", "0", "1"),
    			new MessagingBatchRequest("R000000002", "リクエスト０２", "0", "0", "1"),
    			new MessagingBatchRequest("R000000003", "リクエスト０３", "0", "0", "1"),
    			new MessagingBatchRequest("R000000004", "リクエスト０４", "0", "0", "1"));
    	
        CommandLine commandLine = new CommandLine(
                "-diConfig",
                "nablarch/fw/messaging/action/SyncMessageSendActionTest.xml",
                "-userId", userId,
                "-requestPath",
                actionClassName + "/" + requestId);
        return Main.execute(commandLine);
    }
}
