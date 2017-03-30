package nablarch.fw.messaging.action;

import nablarch.common.handler.TransactionManagementHandler;
import nablarch.core.ThreadContext;
import nablarch.core.db.connection.ConnectionFactory;
import nablarch.core.db.connection.TransactionManagerConnection;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlResultSet;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.transaction.TransactionContext;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.handler.RequestPathJavaPackageMapping;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.SendingMessage;
import nablarch.fw.messaging.StandardFwHeaderDefinition;
import nablarch.fw.messaging.handler.TestHttpMessagingRequestParsingHandler;
import nablarch.fw.messaging.handler.TestHttpMessagingResponseBuildingHandler;
import nablarch.fw.messaging.handler.MessageResendHandler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.test.core.log.LogVerifier;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * メセージング制御基盤の機能テストクラス。
 * <p/>
 * このクラスでは、メッセージング制御基板で想定する標準のハンドラ構成を用いてテストを実施する。
 * 確認ポイントとしては、正常・異常各フローで入力に対して期待するアウトプットが得られることを確認する。
 * <p/>
 * ※認可やスレッドコンテキスト設定ハンドラなど一部ハンドラは除外している。
 */
@RunWith(DatabaseTestRunner.class)
public class MessagingActionTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/fw/messaging/action/MessagingActionTest.xml");

    /** 一時ファイルを出力するための一時ディレクトリ */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public Timeout timeout = new Timeout(600000, TimeUnit.MILLISECONDS);

    @BeforeClass
    public static void setupClass() throws Exception {
        VariousDbTestHelper.createTable(Users.class);
        VariousDbTestHelper.createTable(Request.class);
        VariousDbTestHelper.createTable(SentMessage.class);
    }

    @Before
    public void setUp() throws Exception {
        ThreadContext.clear();
        ThreadContext.setRequestId("TEST_MESSAGE");
        VariousDbTestHelper.delete(Users.class);
        VariousDbTestHelper.delete(SentMessage.class);

        VariousDbTestHelper.setUpTable(new Request("UserRegisterMessagingAction", "1"));

        FilePathSetting filePathSetting = SystemRepository.get("filePathSetting");
        filePathSetting.addBasePathSetting("format", "file:" + temporaryFolder.getRoot().getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        ThreadContext.clear();
    }

    /**
     * 正常にアクションの処理が行われる場合のケース。
     * <p/>
     * アクションで生成した応答電文が返却され、データベースへの変更がコミットされること。
     *
     * @throws Exception
     */
    @Test
    public void testSuccessRequestFixed() throws Exception {
        FilePathSetting.getInstance()
        .addBasePathSetting("format", "file:./")
        .addFileExtensions("format", "fmt");

        File headerFile = Hereis.file("./header.fmt");
        /**********************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    36

        [Header]
        1   requestId   X(27)      # リクエストID
        28  userId      X(5)       # ユーザID
        33  resendFlag  X(1)       # 再送フラグ
        34  statusCode  X(3)       # ステータスコード
        ***************************************************/

        headerFile.deleteOnExit();

        Hereis.file("./UserRegisterMessagingAction_RECEIVE.fmt");
        /*****************************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    10

        [Book]
        1  id         X(1)     # 書名
        2  name       X(9)     # 書名
        ******************************************************/


        // 応答電文フォーマット
        Hereis.file("./UserRegisterMessagingAction_SEND.fmt");
        /*****************************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    88

        [Book]
        1  message       X(88)     # 書名
        ******************************************************/

        String body = Hereis.string();
        /*****************************************************
        UserRegisterMessagingAction123450   1234567890*/

        HttpServer server = createServer();

        // テストようにハンドラーの設定を書き換え
        StandardFwHeaderDefinition stdFwHeaderDef = new StandardFwHeaderDefinition();
        TestHttpMessagingRequestParsingHandler parseHandler = SystemRepository.get("handlerQueue.httpMessagingRequestParsingHandler");
        parseHandler.setFwHeaderDefinition(stdFwHeaderDef);
        TestHttpMessagingResponseBuildingHandler buildHandler = SystemRepository.get("handlerQueue.httpMessagingResponseBuildingHandler");
        buildHandler.setFwHeaderDefinition(stdFwHeaderDef);
        MessageResendHandler resendHandler = SystemRepository.get("handlerQueue.messageResendHandler");
        resendHandler.setFwHeaderDefinition(stdFwHeaderDef);

        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            FwHeaderDefinition fwHeaderDefinition = SystemRepository.get("fwHeaderDefinition");
            parseHandler.setFwHeaderDefinition(fwHeaderDefinition);
            buildHandler.setFwHeaderDefinition(fwHeaderDefinition);
            resendHandler.setFwHeaderDefinition(fwHeaderDefinition);
        }

        // ----- assert response -----
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), containsString("text/plain;charset=UTF-8"));
        assertThat(response.toString(), containsString("UserRegisterMessagingAction123450200登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));
        assertThat(response.getHeaderMap().toString(), containsString("Transfer-Encoding=chunked"));

        // ----- assert table -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(1));
        assertThat(rows.get(0).getString("id"), is("1"));
        assertThat(rows.get(0).getString("name"), is("234567890"));
        assertThat(rows.get(0).getString("mail"), is(nullValue()));

    }


    /**
     * 正常にアクションの処理が行われる場合のケース。
     * <p/>
     * アクションで生成した応答電文が返却され、データベースへの変更がコミットされること。
     *
     * @throws Exception
     */
    @Test
    public void testSuccessRequestJson() throws Exception {
        createXmlFormatFileRequiredFwHeader();

        File receiveFormatFile = new File(temporaryFolder.getRoot(), "UserRegisterMessagingAction_RECEIVE.fmt");
        Hereis.file(receiveFormatFile.getAbsolutePath());
        /*
        file-type:      "JSON"
        text-encoding:  "UTF-8"
        [user]
        1 fw   OB
        2 data OB
        [fw]
        1 id X
        2 resent X
        3 requestId X
        [data]
        1 id                X
        2 name              X
        3 mail       [0..1] X
        4 errorClass [0..1] X
        */

        // 応答電文フォーマット
        File sendFormatFile = new File(temporaryFolder.getRoot(), "UserRegisterMessagingAction_SEND.fmt");
        Hereis.file(sendFormatFile.getAbsolutePath());
        /*
         file-type:      "JSON"
         text-encoding:  "UTF-8"
         [result]
         1 message X
         2 fw   OB
         [fw]
         1 id X
         2 resent X
         3 requestId X
         4 statusCode X9
        */

        String body = Hereis.string();
        /*
        {
          "fw":{
            "id":"12345",
            "resent":"0",
            "requestId":"UserRegisterMessagingAction"
          },
          "data":{
            "id":"1",
            "name":"なまえ"
          }
        }
        */

        HttpServer server = createServer();

        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert response -----
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), containsString("application/json;charset=UTF-8"));
        assertThat(response.toString(), containsString("{\"message\":\"登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));

        // ----- assert table -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(1));
        assertThat(rows.get(0).getString("id"), is("1"));
        assertThat(rows.get(0).getString("name"), is("なまえ"));
        assertThat(rows.get(0).getString("mail"), is(nullValue()));

    }

    /**
     * 正常にアクションの処理が行われる場合のケース。
     * <p/>
     * アクションで生成した応答電文が返却され、データベースへの変更がコミットされること。
     *
     * @throws Exception
     */
    @Test
    public void testSuccessRequestFixedNoFlush() throws Exception {

        FilePathSetting.getInstance()
        .addBasePathSetting("format", "file:./")
        .addFileExtensions("format", "fmt");

        File headerFile = Hereis.file("./header.fmt");
        /**********************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    36

        [Header]
        1   requestId   X(27)      # リクエストID
        28  userId      X(5)       # ユーザID
        33  resendFlag  X(1)       # 再送フラグ
        34  statusCode  X(3)       # ステータスコード
        ***************************************************/

        headerFile.deleteOnExit();

        Hereis.file("./UserRegisterMessagingAction_RECEIVE.fmt");
        /*****************************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    10

        [Book]
        1  id         X(1)     # 書名
        2  name       X(9)     # 書名
        ******************************************************/


        // 応答電文フォーマット
        Hereis.file("./UserRegisterMessagingAction_SEND.fmt");
        /*****************************************************
        file-type:        "Fixed"
        text-encoding:    "utf-8"
        record-length:    88

        [Book]
        1  message       X(88)     # 書名
        ******************************************************/

        String body = Hereis.string();
        /*****************************************************
        UserRegisterMessagingAction123450   1234567890*/

        HttpServer server = createServer();

        // テストようにハンドラーの設定を書き換え
        StandardFwHeaderDefinition stdFwHeaderDef = new StandardFwHeaderDefinition();
        TestHttpMessagingRequestParsingHandler parseHandler = SystemRepository.get("handlerQueue.httpMessagingRequestParsingHandler");
        parseHandler.setFwHeaderDefinition(stdFwHeaderDef);
        TestHttpMessagingResponseBuildingHandler buildHandler = SystemRepository.get("handlerQueue.httpMessagingResponseBuildingHandler");
        buildHandler.setFwHeaderDefinition(stdFwHeaderDef);
        MessageResendHandler resendHandler = SystemRepository.get("handlerQueue.messageResendHandler");
        resendHandler.setFwHeaderDefinition(stdFwHeaderDef);
        HttpResponseHandler httpResponseHandler = SystemRepository.get("handlerQueue.httpResponseHandler");
        httpResponseHandler.setForceFlushAfterWritingHeaders(false);

        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            FwHeaderDefinition fwHeaderDefinition = SystemRepository.get("fwHeaderDefinition");
            parseHandler.setFwHeaderDefinition(fwHeaderDefinition);
            buildHandler.setFwHeaderDefinition(fwHeaderDefinition);
            resendHandler.setFwHeaderDefinition(fwHeaderDefinition);
            httpResponseHandler.setForceFlushAfterWritingHeaders(true);
        }

        // ----- assert response -----
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), containsString("text/plain;charset=UTF-8"));
        assertThat(response.toString(), containsString("UserRegisterMessagingAction123450200登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));
        assertThat(response.getHeaderMap().toString(), not(containsString("Transfer-Encoding=chunked")));

        // ----- assert table -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(1));
        assertThat(rows.get(0).getString("id"), is("1"));
        assertThat(rows.get(0).getString("name"), is("234567890"));
        assertThat(rows.get(0).getString("mail"), is(nullValue()));

    }

    /**
     * FW制御ヘッダーの送信が必要なメッセージの場合。
     *
     * FW制御ヘッダーの内容が、ThreadContext上に設定されること。
     */
    @Test
    public void testRequiredFwHeader() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        OnMemoryLogWriter.clear();

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert response -----
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat("ユーザIDが設定されていること", response.toString(), containsString("<message>登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345</message>"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));

        // ----- assert log -----
        List<String> msg = OnMemoryLogWriter.getMessages("writer.accessLog");
        assertThat(msg.get(0), containsString("INFO MESSAGING @@@@ HTTP RECEIVED MESSAGE @@@@"));
        assertThat(msg.get(0), containsString("message_id     = [message-id]"));
        assertThat(msg.get(1), containsString("INFO MESSAGING @@@@ HTTP SENT MESSAGE @@@@"));
        assertThat(msg.get(1), containsString("message_id     = [null]"));

        // ----- assert table -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(1));
        assertThat(rows.get(0).getString("id"), is("1"));
        assertThat(rows.get(0).getString("name"), is("なまえ"));
        assertThat(rows.get(0).getString("mail"), is(nullValue()));
    }

    /**
     * FW制御ヘッダーの形式が不正な場合。
     *
     * 不正なリクエストとして400エラーがかえされること。
     * @throws Exception
     */
    @Test
    public void testInvalidFwHeaderRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();

        // 必須のfw.idタグを持たない不正なXML
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "INFO");
                        put("message1", "invalid format request message received.");
                    }
                });
            }
        });

        HttpServer server = createServer();

        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("400エラー"));
        assertThat(response.getStatusCode(), is(400));

        // ----- assert table -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(0));
    }

    /**
     * BODY部が空の場合のケース
     * <p/>
     * メッセージ部の解析に失敗するので、400エラーが返されること
     */
    @Test
    public void testEmptyBodyRequest() throws Exception {
        createXmlFormatFileNonFwHeader();

        // ボディは空
        String body = "";

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("400エラー"));
        assertThat(response.getStatusCode(), is(400));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat("ロールバックされるので登録されていない", rows.size(), is(0));
    }

    /**
     * ターゲットが存在しないの場合のケース
     * <p/>
     * ディスパッチできないので、404エラーが返されること
     */
    @Test
    public void testNoTargetRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();

        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");

        int i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof RequestPathJavaPackageMapping){
                handlerQueue.remove(i);
                break;
            }
            i++;
        }

        HttpServer server = createServer();

        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("404エラー"));
        assertThat(response.getStatusCode(), is(404));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat("ロールバックされるので登録されていない", rows.size(), is(0));
    }

    /**
     * BODY部が上限サイズを超過しているケース
     * <p/>
     * BODY部の上限サイズチェックでエラーとなるため、413エラーが返されること
     */
    @Test
    public void testSizeOverBodyRequest() throws Exception {
        createXmlFormatFileNonFwHeader();

        String body = Hereis.string();
        /*
        <user>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テスト用にハンドラーの設定を書き換え
        TestHttpMessagingRequestParsingHandler handler = SystemRepository.get("handlerQueue.httpMessagingRequestParsingHandler");
        int setting = handler.getBodyLengthLimit();
        handler.setBodyLengthLimit(body.length() - 1 );

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            handler.setBodyLengthLimit(setting);
        }

        // ----- assert response -----
        assertThat(response.toString(), containsString("413エラー"));
        assertThat(response.getStatusCode(), is(413));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat("ロールバックされるので登録されていない", rows.size(), is(0));
    }


    /**
     * 業務メッセージ部の形式が不正な場合
     *
     * メッセージの解析に失敗するので400エラーが返される。
     */
    @Test
    public void testInvalidBodyRequest() throws Exception {
        createXmlFormatFileNonFwHeader();
        // 必須の要素を持たないボディを定義
        String body = Hereis.string();
        /*
        <user>
          <data>
            <name>なまえ</name>
            <mail>メール</mail>
            <errorClass>java.lang.IllegalArgumentException</errorClass>
          </data>
        </user>
        */

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "INFO");
                        put("message1", "invalid format request message received.");
                    }
                });
            }
        });

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("400エラー"));
        assertThat(response.getStatusCode(), is(400));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(0));
    }

    /**
     * 受信メッセージ解析用のフォーマット定義ファイルが存在しない場合
     *
     * 障害扱いなので500エラーがかえされること
     */
    @Test
    public void testReceiveFormatNotFound() throws Exception {
        createXmlFormatFileNonFwHeader();
        File[] files = temporaryFolder.getRoot().listFiles(new FilenameFilter() {
                                                               @Override
                                                               public boolean accept(File dir, String name) {
                                                                   return name.endsWith("_RECEIVE.fmt");
                                                               }
                                                           }
        );
        for (File file : files) {
            file.delete();
        }
        String body = Hereis.string();
        /*
        <user>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat(rows.size(), is(0));
    }

    /**
     * 送信メッセージ生成用にフォーマット不正エラーがでた場合(再送制御ハンドラを利用する場合)
     *
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testSentFormatNotFound() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        // 応答電文フォーマットを書き換える
        File sendFormatFile = new File(temporaryFolder.getRoot(), "UserRegisterMessagingAction_SEND.fmt");
        Hereis.file(sendFormatFile.getAbsolutePath());
        /*
         file-type:      "XML"
         text-encoding:  "UTF-8"
         [result]
         1 msg X
        */

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "INFO");
                        put("message1", "could not build the message body because of an invalid data error");
                    }
                });
            }
        });

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // この位置で障害が発生すると、標準ハンドラ構成では
        // 再送制御ハンドラでエラーが発生するため、ロールバックされている
        assertThat(rows.size(), is(0));
    }

    /**
     * 送信メッセージ生成用にフォーマット不正エラーがでた場合（再送制御ハンドラを利用しない場合）
     *
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testSentFormatNotFoundNotResent() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        // 応答電文フォーマットを書き換える
        File sendFormatFile = new File(temporaryFolder.getRoot(), "UserRegisterMessagingAction_SEND.fmt");
        Hereis.file(sendFormatFile.getAbsolutePath());
        /*
         file-type:      "XML"
         text-encoding:  "UTF-8"
         [result]
         1 msg X
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");

        int i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof MessageResendHandler){
                handlerQueue.remove(i);
                break;
            }
            i++;
        }

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "INFO");
                        put("message1", "could not build the message body because of an invalid data error");
                    }
                });
            }
        });

       HttpServer server = createServer();

        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 受信メッセージ解析中にMessagingException および InvalidDataFormatException以外の例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testErrorInParsingRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        ErrorFwHeaderDefinition errFwHeaderDef = new ErrorFwHeaderDefinition();
        TestHttpMessagingRequestParsingHandler parseHandler = SystemRepository.get("handlerQueue.httpMessagingRequestParsingHandler");
        parseHandler.setFwHeaderDefinition(errFwHeaderDef);

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            FwHeaderDefinition fwHeaderDefinition = SystemRepository.get("fwHeaderDefinition");
            parseHandler.setFwHeaderDefinition(fwHeaderDefinition);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 送信メッセージ構築中にMessagingException および InvalidDataFormatException以外の例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testErrorResponse() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        handlerQueue.add(new ErrorResponseThrowHandler(500));

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでStackOverflowErrorの例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testStackOverflow() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        //handlerQueue.add(new ExceptionThrowHandler(new StackOverflowError()));
        int i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの前でエラーを送出させる
                handlerQueue.add(i, new ExceptionThrowHandler(new StackOverflowError()));
                break;
            }
            i++;
        }
        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("text/html;charset=UTF-8"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));


        // トランザクション制御ハンドラの前後でレスポンス内容が変わる

        // テストようにハンドラーの設定を書き換え
        handlerQueue = SystemRepository.get("handlerQueue");
        i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの後でエラーを送出させる
                handlerQueue.add(i + 1, new ExceptionThrowHandler(new StackOverflowError()));
                break;
            }
            i++;
        }

        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでThreadDeathの例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testThreadDeath() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        int i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの前でエラーを送出させる
                handlerQueue.add(i, new ExceptionThrowHandler(new ThreadDeath()));
                break;
            }
            i++;
        }

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("text/html;charset=UTF-8"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));


        // トランザクション制御ハンドラの前後でレスポンス内容が変わる

        // テストようにハンドラーの設定を書き換え
        handlerQueue = SystemRepository.get("handlerQueue");
        i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの後でエラーを送出させる
                handlerQueue.add(i + 1, new ExceptionThrowHandler(new ThreadDeath()));
                break;
            }
            i++;
        }

        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでVirtualMachineErrorの例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testVirtualMachineError() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        int i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの前でエラーを送出させる
                handlerQueue.add(i, new ExceptionThrowHandler(new UnknownError()));
                break;
            }
            i++;
        }

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("text/html;charset=UTF-8"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));


        // トランザクション制御ハンドラの前後でレスポンス内容が変わる

        // テストようにハンドラーの設定を書き換え
        handlerQueue = SystemRepository.get("handlerQueue");
        i = 0;
        for(Handler handler: handlerQueue){
            if( handler instanceof TransactionManagementHandler){
                // トランザクション制御ハンドラの後でエラーを送出させる
                handlerQueue.add(i + 1, new ExceptionThrowHandler(new UnknownError()));
                break;
            }
            i++;
        }

        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでRuntimeExceptionの例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testRuntimeException() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        handlerQueue.add(new ExceptionThrowHandler(new RuntimeException()));

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.getBodyString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでMessagingExceptionの例外が発生するケース。
     * <p/>
     * 障害扱いなので500エラーが返されること
     * @throws Exception
     */
    @Test
    public void testMessagingException() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        handlerQueue.add(new ExceptionThrowHandler(new MessagingException()));

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "INFO");
                        put("message1", "could not build the message body because of an invalid message error");
                    }
                });
            }
        });

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.getBodyString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * 後続ハンドラでResult.InternalErrorの例外が発生するケース。
     * <p/>
     * デフォルトの障害通知ログ設定でログ出力されることを確認する。
     * @throws Exception
     */
    @Test
    public void testInternalErrorLog() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
            <mail>メール</mail>
          </data>
        </user>
        */

        // テストようにハンドラーの設定を書き換え
        List<Handler> handlerQueue = SystemRepository.get("handlerQueue");
        handlerQueue.add(new ExceptionThrowHandler(new nablarch.fw.results.InternalError("Test Error!!")));

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "FATAL");
                        put("message1", "Test Error!!");
                    }
                });
            }
        });

        HttpServer server = createServer();
        HttpResponse response;
        try {
            response = server.handle(createRequest(body), null);
        } finally {
            // 書き換えたハンドラーの設定を復元
            SystemRepository.clear();
            DiContainer container = new DiContainer(new XmlComponentDefinitionLoader(
                    "nablarch/fw/messaging/action/MessagingActionTest.xml"));
            SystemRepository.load(container);
        }

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("システムエラー"));
        assertThat(response.getStatusCode(), is(500));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        // 業務トランザクションはロールバックされること。
        assertThat(rows.size(), is(0));
    }

    /**
     * アクションの処理で例外が発生するケース。
     * <p/>
     * データベースがロールバックされ、エラー応答電文が返却されること。
     */
    @Test
    public void testSystemErrorRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>error</id>
            <name>なまえ</name>
            <mail>メール</mail>
            <errorClass>java.lang.IllegalArgumentException</errorClass>
          </data>
        </user>
        */

        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String,String>>() {
            {
                add(new HashMap<String, String>() {
                    {
                        put("logLevel", "FATAL");
                        put("message1", "an unexpected exception occurred");
                    }
                });
            }
        });

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        // ----- assert log -----
        LogVerifier.verify("assert log");

        // ----- assert response -----
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));
        assertThat(response.toString(),
                containsString("<message>システムエラーが発生しました。:IllegalArgumentException</message>"));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat("ロールバックされるので登録されていない", rows.size(), is(0));
    }

    /**
     * アプリケーションの処理で業務エラー({@link nablarch.core.message.ApplicationException})が発生するケース。
     * <p/>
     * データベースがロールバックされ応答電文が返却されること。
     */
    @Test
    public void testApplicationErrorRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>error</id>
            <name>なまえ</name>
            <mail>メール</mail>
            <errorClass>appError</errorClass>
          </data>
        </user>
        */

        HttpServer server = createServer();
        HttpResponse response = server.handle(createRequest(body), null);

        //----- assert response -----
        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString(
                "<message>業務エラーが発生しました。:ApplicationException</message>"));

        //----- assert database -----
        SqlResultSet rows = findTestTable();
        assertThat("ロールバックされるので登録されていない", rows.size(), is(0));
    }

    /**
     * 再送要求電文を受信したケース。
     * <p/>
     *
     *
     * @throws Exception
     */

    @Test
    public void testResentRequest() throws Exception {
        createXmlFormatFileRequiredFwHeader();
        String body = Hereis.string();
        /*
        <user>
          <fw>
            <id>12345</id>
            <resent>0</resent>
          </fw>
          <data>
            <id>1</id>
            <name>なまえ</name>
          </data>
        </user>
        */

        SqlResultSet user_rows = findTestTable();
        assertThat(user_rows.size(), is(0));

        HttpServer server = createServer();

        HttpResponse response = server.handle(createRequest(body), null);
        // ----- assert response -----
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response.toString(), containsString("<message>登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345</message>"));
        assertThat(response.getHeader("X-Correlation-Id"), is("message-id"));

        // ----- assert table -----
        SqlResultSet rows = findSentMessageTestTable();
        assertThat(rows.size(), is(1));

        SqlResultSet user_rows1 = findTestTable();
        assertThat(user_rows1.size(), is(1));

        //再送要求

        String body2 = Hereis.string();
        /*
        <user>
          <fw>
            <id>54321</id>
            <resent>1</resent>
          </fw>
          <data>
            <id>2</id>
            <name>なまえ2</name>
          </data>
        </user>
        */

        HttpResponse response2 = server.handle(createRequest(body2), null);

        // ----- assert response -----
        assertThat(response2.getStatusCode(), is(200));
        assertThat(response2.getContentType(), containsString("application/xml;charset=UTF-8"));
        assertThat(response2.getHeader("X-Correlation-Id"), is("message-id"));
        assertThat(response2.toString(), containsString("<message>登録出来ました。 リクエストID:UserRegisterMessagingAction ユーザID:12345</message>"));

        //----- assert database -----
        SqlResultSet user_rows2 = findTestTable();
        assertThat(user_rows2.size(), is(1));
        assertThat(user_rows1.get(0).toString(), is(user_rows2.get(0).toString()));
    }

    /**
     * テスト用サーバを生成する。
     *
     * @return {@link HttpServer}
     */
    private HttpServer createServer() {
        HttpServer server = new HttpServer();
        List<Handler<?, ?>> handlers = SystemRepository.get("handlerQueue");
        server.setHandlerQueue(handlers);
        server.setWarBasePath("classpath://nablarch/fw/messaging/action/web");
        server.startLocal();
        return server;
    }

    /**
     * {@link HttpRequest}を生成する。
     *
     * @param body HTTP BODY
     * @return リクエスト
     */
    private HttpRequest createRequest(String body) {
        String replacedBody = body.replaceAll("\n", "\r\n");

        int length = replacedBody.getBytes().length;
        String xml = Hereis.string(replacedBody, length);
        /*
        POST /action/UserRegisterMessagingAction HTTP/1.1
        X-Message-Id: message-id
        X-Correlation-Id: message-id
        Content-Length: ${length}
        Content-Type: application/xml; charset=utf-8

        ${replacedBody}
        */

        return new MessagingRequest(xml);
    }

    /**
     * テストで使用するフォーマットファイル(XMLのFWヘッダ無し)を作成する。
     */
    private void createXmlFormatFileNonFwHeader() throws IOException {
        File receiveFormatFile = temporaryFolder.newFile("UserRegisterMessagingAction_RECEIVE.fmt");
        Hereis.file(receiveFormatFile.getAbsolutePath());
        /*
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [user]
        1 data OB
        [data]
        1 id                X
        2 name              X
        3 mail       [0..1] X
        4 errorClass [0..1] X
        */

        // 応答電文フォーマット
        File sendFormatFile = temporaryFolder.newFile("UserRegisterMessagingAction_SEND.fmt");
        Hereis.file(sendFormatFile.getAbsolutePath());
        /*
         file-type:      "XML"
         text-encoding:  "UTF-8"
         [result]
         1 message X
        */
    }

    /**
     * テストで使用するフォーマットファイル(XMLのFWヘッダ有り)を作成する。
     */
    private void createXmlFormatFileRequiredFwHeader() throws IOException {
        File receiveFormatFile = temporaryFolder.newFile("UserRegisterMessagingAction_RECEIVE.fmt");
        Hereis.file(receiveFormatFile.getAbsolutePath());
        /*
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [user]
        1 fw   OB
        2 data OB
        [fw]
        1 id X
        2 resent X
        [data]
        1 id                X
        2 name              X
        3 mail       [0..1] X
        4 errorClass [0..1] X
        */

        // 応答電文フォーマット
        File sendFormatFile = temporaryFolder.newFile("UserRegisterMessagingAction_SEND.fmt");
        Hereis.file(sendFormatFile.getAbsolutePath());
        /*
         file-type:      "XML"
         text-encoding:  "UTF-8"
         [result]
         1 message X
        */
    }

    /**
     * テスト対象のテーブルの全データを取得する。
     */
    private SqlResultSet findTestTable() {
        ConnectionFactory connectionFactory = SystemRepository.get("connectionFactory");
        TransactionManagerConnection connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        try {
            SqlPStatement statement = connection.prepareStatement("select * from users");
            return statement.retrieve();
        } finally {
            connection.terminate();
        }
    }

    /**
     * テスト対象のテーブルの全データを取得する。
     */
    private SqlResultSet findSentMessageTestTable() {
        ConnectionFactory connectionFactory = SystemRepository.get("connectionFactory");
        TransactionManagerConnection connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        try {
            SqlPStatement statement = connection.prepareStatement("select * from sent_message");
            return statement.retrieve();
        } finally {
            connection.terminate();
        }
    }

    /**
     * サービスの状態を変更する。
     *
     * @param requestId リクエストID
     * @param state 状態
     */
    private void changeServiceState(String requestId, String state) {
        ConnectionFactory connectionFactory = SystemRepository.get("connectionFactory");
        TransactionManagerConnection connection = connectionFactory.getConnection(TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY);
        try {
            SqlPStatement statement = connection.prepareStatement("update request set state = ? where id = ?");
            statement.setString(1, state);
            statement.setString(2, requestId);
            statement.executeUpdate();
            connection.commit();
        } finally {
            connection.terminate();
        }
    }

    /**
     * BODY部にメッセージを埋め込むためのHttpRequestクラス拡張。
     */
    private static class MessagingRequest extends MockHttpRequest {

        public MessagingRequest(String message) {
            super(message);
        }

        private String body;

        @Override
        public HttpRequest setBodyReader(Reader reader) {
            CharBuffer buffer = CharBuffer.allocate(1024);
            try {
                body = "";
                while (reader.read(buffer) != -1) {
                    buffer.flip();
                    body += buffer.toString();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return super.setBodyReader(reader);
        }

        @Override
        public String toString() {
            String message = super.toString();

            if (!getParamMap().isEmpty()) {
                return message;
            }
            return message + body;
        }
    }

    /**
     * 読み込みおよび書き込み時にRuntimeExceptionを送出する
     * エラーケーステスト用FWヘッダ定義クラス。
     */
    private static class ErrorFwHeaderDefinition implements FwHeaderDefinition {
        @Override
        public RequestMessage readFwHeaderFrom(ReceivedMessage message) {
            throw new RuntimeException("Test Error!!");
        }

        @Override
        public void writeFwHeaderTo(SendingMessage message, FwHeader header) {
            throw new RuntimeException("Test Error!!");
        }
    }

    /**
     * ErrorResponseMessageを送出するハンドラ。
     */
    private static class ErrorResponseThrowHandler implements Handler<Object, Object> {
        private int statusCode;

        public ErrorResponseThrowHandler(int statusCode) {
            this.statusCode = statusCode;
        }
        @Override
        public Object handle(Object data, ExecutionContext context) {
            ResponseMessage response = (ResponseMessage) context.handleNext(data);
            response.setStatusCodeHeader(Integer.toString(statusCode)).throwAsError();
            return null;
        }
    }

    /**
     * 任意の例外を送出するハンドラ。
     */
    private static class ExceptionThrowHandler implements Handler<Object, Object> {
        private RuntimeException ex;
        private Error err;
        public ExceptionThrowHandler(RuntimeException ex) {
            this.ex = ex;
        }
        public ExceptionThrowHandler(Error err) {
            this.err = err;
        }
        @Override
        public Object handle(Object data, ExecutionContext context) {
            if (err != null) {
                throw err;
            }
            if (ex != null) {
                throw ex;
            }
            return null;
        }
    }
}

