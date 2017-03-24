package nablarch.fw.messaging;

import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static nablarch.core.util.StringUtil.rpad;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link MessageSender}のテスト。
 * @author Kiyohito Itoh
 */
public class MessageSenderTest {

    public void initRepository(String suffix) {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                MessageSenderTest.class.getName().replace('.', '/') + suffix + ".xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.clear();
        SystemRepository.load(container);
    }

    @After
    public void tearDown() {
        MessagingContext.detach();
        SystemRepository.clear();
    }

    /**
     * 設定情報に基づき要求電文と応答電文が処理されること。
     */
    @Test
    public void testMessageBody() {

        initRepository("");

        Map<String, Object> data = new TreeMap<String, Object>();
        data.put("title", "title_test");
        data.put("publisher", "publisher_test");
        data.put("authors", "authors_test1");

        MockMessagingContext context;
        SyncMessage responseMessage;

        /********************************************************
        デフォルト設定を使用する場合
        ********************************************************/

        context = new MockMessagingContext(
                        new StringBuilder()
                            .append(rpad("RM21AA0100", 20, ' '))
                            .append("0") // 初回送信
                            .append(rpad("", 9, ' '))
                            .append(rpad("failureCode_test", 20, ' '))
                            .append(rpad("userInfoId_test", 20, ' '))
                            .append(rpad(" ", 20, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0100").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0100", 20, ' '))
                      .append("0") // 初回送信
                      .append(rpad("", 9, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));

        // 応答電文のヘッダとデータ(1件)のアサート

        assertThat(responseMessage.getHeaderRecord().keySet().size(), is(2));
        assertThat(responseMessage.getHeaderRecord().get("requestId").toString(), is("RM21AA0100"));
        assertThat(responseMessage.getHeaderRecord().get("resendFlag").toString(), is("0"));
        assertThat(responseMessage.getDataRecords().size(), is(1));
        assertThat(responseMessage.getDataRecord().keySet().size(), is(2));
        assertThat(responseMessage.getDataRecord().get("failureCode").toString(), is("failureCode_test"));
        assertThat(responseMessage.getDataRecord().get("userInfoId").toString(), is("userInfoId_test"));

        /********************************************************
        すべて個別設定を使用する場合
        ********************************************************/

        context = new MockMessagingContext(
                        new StringBuilder()
                            .append("0") // 初回送信
                            .append(rpad("RM21AA0101", 20, ' '))
                            .append(rpad("", 39, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0101").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE2"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY2"));
        assertThat(context.specifiedTimeout, is(5000L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append("0") // 初回送信
                      .append(rpad("RM21AA0101", 20, ' '))
                      .append(rpad("", 39, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .toString()));
        
        // 応答電文のヘッダとデータ(0件)のアサート
        assertThat(responseMessage.getHeaderRecord().keySet().size(), is(2));
        assertThat(responseMessage.getHeaderRecord().get("requestId").toString(), is("RM21AA0101"));
        assertThat(responseMessage.getHeaderRecord().get("resendFlag").toString(), is("0"));
        assertTrue(responseMessage.getDataRecords().isEmpty());
        assertNull(responseMessage.getDataRecord());

        /********************************************************
        一部だけデフォルト設定をオーバーライドする場合(タイムアウトとリトライ回数のみ)
        ********************************************************/

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0202", 20, ' '))
                    .append("0") // 初回送信
                    .append(rpad("", 9, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0202").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(1000L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0202", 20, ' '))
                      .append("0") // 初回送信
                      .append(rpad("", 9, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));

        /********************************************************
        再送しない場合
        ********************************************************/

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0303", 20, ' '))
                    .append(" ") // 再送不要
                    .append(rpad("", 9, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0303").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0303", 20, ' '))
                      .append(" ") // 再送不要
                      .append(rpad("", 9, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));

        /********************************************************
        複数データの場合
        ********************************************************/

        List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < 3; i++) {
            dataList.add(new TreeMap<String, Object>());
            dataList.get(i).put("title", "title_test" + i);
            dataList.get(i).put("publisher", "publisher_test" + i);
            dataList.get(i).put("authors", "authors_test" + i);
        }

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0303", 20, ' '))
                    .append(" ") // 再送不要
                    .append(rpad("", 9, ' '))
                    .append(rpad("failureCode_test1", 20, ' '))
                    .append(rpad("userInfoId_test1", 20, ' '))
                    .append(rpad(" ", 20, ' '))
                    .append(rpad("failureCode_test2", 20, ' '))
                    .append(rpad("userInfoId_test2", 20, ' '))
                    .append(rpad(" ", 20, ' '))
                    .append(rpad("failureCode_test3", 20, ' '))
                    .append(rpad("userInfoId_test3", 20, ' '))
                    .append(rpad(" ", 20, ' ')).toString());
        MessagingContext.attach(context);

        SyncMessage requestMessage = new SyncMessage("RM21AA0303");
        requestMessage.getDataRecords().addAll(dataList);
        responseMessage = MessageSender.sendSync(requestMessage);

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0303", 20, ' '))
                      .append(" ") // 再送不要
                      .append(rpad("", 9, ' '))
                      .append(rpad("title_test0", 20, ' '))
                      .append(rpad("publisher_test0", 20, ' '))
                      .append(rpad("authors_test0", 20, ' '))
                      .append(rpad("title_test1", 20, ' '))
                      .append(rpad("publisher_test1", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .append(rpad("title_test2", 20, ' '))
                      .append(rpad("publisher_test2", 20, ' '))
                      .append(rpad("authors_test2", 20, ' '))
                      .toString()));

        // 応答電文のヘッダとデータ(複数件)のアサート
        assertThat(responseMessage.getHeaderRecord().keySet().size(), is(2));
        assertThat(responseMessage.getHeaderRecord().get("requestId").toString(), is("RM21AA0303"));
        assertThat(responseMessage.getHeaderRecord().get("resendFlag"), is(nullValue()));
        assertThat(responseMessage.getDataRecords().size(), is(3));
        assertThat(responseMessage.getDataRecord().keySet().size(), is(2));
        assertThat(responseMessage.getDataRecord().get("failureCode").toString(), is("failureCode_test1"));
        assertThat(responseMessage.getDataRecord().get("userInfoId").toString(), is("userInfoId_test1"));
        assertThat(responseMessage.getDataRecords().get(1).keySet().size(), is(2));
        assertThat(responseMessage.getDataRecords().get(1).get("failureCode").toString(), is("failureCode_test2"));
        assertThat(responseMessage.getDataRecords().get(1).get("userInfoId").toString(), is("userInfoId_test2"));
        assertThat(responseMessage.getDataRecords().get(2).keySet().size(), is(2));
        assertThat(responseMessage.getDataRecords().get(2).get("failureCode").toString(), is("failureCode_test3"));
        assertThat(responseMessage.getDataRecords().get(2).get("userInfoId").toString(), is("userInfoId_test3"));

        /********************************************************
        リクエストIDが重複した場合
        ********************************************************/

        context = new MockMessagingContext(
                        new StringBuilder()
                            .append("0") // 初回送信
                            .append(rpad("RM21AA0101", 20, ' '))
                            .append(rpad("", 39, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0707").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE7"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY7"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append("0") // 初回送信
                      .append(rpad("RM21AA0101", 20, ' ')) // 送信されるリクエストIDは別。
                      .append(rpad("", 39, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .toString()));
    }

    /**
     * 設定情報に基づきタイムアウト時の再送が行われること。
     */
    @Test
    public void testRetryOnTimeout() {

        initRepository("");

        Map<String, Object> data = new TreeMap<String, Object>();
        data.put("title", "title_test");
        data.put("publisher", "publisher_test");
        data.put("authors", "authors_test1");

        MockMessagingContext context;
        SyncMessage responseMessage;

        /********************************************************
        リトライ回数の設定値が3の場合
        ********************************************************/

        context = new MockMessagingContext("unused", 4); // 4回タイムアウトする
        MessagingContext.attach(context);

        try {
            MessageSender.sendSync(new SyncMessage("RM21AA0100").addDataRecord(data));
            fail("MessageSendSyncTimeoutException");
        } catch (MessageSendSyncTimeoutException e) {
            assertThat(context.sentMessage.getMessageId(), is("MID004"));
            assertThat(context.sentMessage.getCorrelationId(), is("MID003"));
            assertThat(e.getRetryCount(), is(3));
        }

        /********************************************************
        リトライ回数の設定値が4の場合
        ********************************************************/

        context = new MockMessagingContext(
                new StringBuilder()
                    .append("1") // 再送
                    .append(rpad("RM21AA0101", 20, ' '))
                    .append(rpad("", 39, ' ')).toString(),
                4); // 4回タイムアウトする
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0101").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID005"));
        assertThat(context.sentMessage.getCorrelationId(), is("MID004"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE2"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY2"));
        assertThat(context.specifiedTimeout, is(5000L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append("1") // 再送
                      .append(rpad("RM21AA0101", 20, ' '))
                      .append(rpad("", 39, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .toString()));

        /********************************************************
        リトライ回数の設定値が2の場合
        ********************************************************/

        context = new MockMessagingContext("unused", 4); // 4回タイムアウトする
        MessagingContext.attach(context);

        try {
            MessageSender.sendSync(new SyncMessage("RM21AA0202").addDataRecord(data));
            fail("MessageSendSyncTimeoutException");
        } catch (MessageSendSyncTimeoutException e) {
            assertThat(context.sentMessage.getMessageId(), is("MID003"));
            assertThat(context.sentMessage.getCorrelationId(), is("MID002"));
            assertThat(e.getRetryCount(), is(2));
        }

        /********************************************************
        再送しない場合
        ********************************************************/

        context = new MockMessagingContext("unused", 4); // 4回タイムアウトする
        MessagingContext.attach(context);

        try {
            MessageSender.sendSync(new SyncMessage("RM21AA0303").addDataRecord(data));
            fail("MessageSendSyncTimeoutException");
        } catch (MessageSendSyncTimeoutException e) {
            assertThat(context.sentMessage.getMessageId(), is("MID001"));
            assertNull(context.sentMessage.getCorrelationId());
            assertThat(e.getRetryCount(), is(-1));
        }
    }

    /**
     * フレームワーク制御ヘッダをカスタマイズできること。
     */
    @Test
    public void testCustomizeFwHeader() {

        Map<String, Object> data = new TreeMap<String, Object>();
        data.put("title", "title_test");
        data.put("publisher", "publisher_test");
        data.put("authors", "authors_test1");

        ThreadContext.setUserId("userId_test");

        MockMessagingContext context;
        SyncMessage responseMessage;

        initRepository("");

        /********************************************************
        業務アクションでヘッダーを指定する場合
        ********************************************************/

        Map<String, Object> header = new TreeMap<String, Object>();
        header.put("test", "test606");

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0606", 20, ' '))
                    .append("0") // 初回送信
                    .append(rpad("test606", 9, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0606").setHeaderRecord(header).addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0606", 20, ' '))
                      .append("0") // 初回送信
                      .append(rpad("test606", 9, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));

        initRepository("_custom");

        /********************************************************
        デフォルト設定用のカスタマイズサポートクラスを使用する場合
        ********************************************************/

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0505", 20, ' '))
                    .append("0") // 初回送信
                    .append(rpad("test606", 9, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0505").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(responseMessage.getHeaderRecord().get("receivedDate").toString(), is("20111020")); // 受信時のカスタマイズをアサート
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0505", 20, ' '))
                      .append("0") // 初回送信
                      .append(rpad("test", 9, ' ')) // 送信時のカスタマイズをアサート
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));

        /********************************************************
        個別設定用のカスタマイズサポートクラスを使用する場合
        ********************************************************/

        context = new MockMessagingContext(
                new StringBuilder()
                    .append(rpad("RM21AA0404", 20, ' '))
                    .append("0") // 初回送信
                    .append(rpad("userId_test", 20, ' ')) // 送信時のカスタマイズをアサート
                    .append("20111013174932") // 送信時のカスタマイズをアサート
                    .append(rpad("", 5, ' ')).toString());
        MessagingContext.attach(context);

        responseMessage = MessageSender.sendSync(new SyncMessage("RM21AA0404").addDataRecord(data));

        assertNotNull(responseMessage);
        assertThat(responseMessage.getHeaderRecord().get("receivedDate").toString(), is("20111011")); // 受信時のカスタマイズをアサート
        assertThat(context.sentMessage.getMessageId(), is("MID001"));
        assertThat(context.sentMessage.getDestination(), is("QUEUE1"));
        assertThat(context.sentMessage.getReplyTo(), is("REPLY1"));
        assertThat(context.specifiedTimeout, is(-1L));
        assertThat(new String(context.sentMessage.getBodyBytes()),
                  is(new StringBuilder()
                      .append(rpad("RM21AA0404", 20, ' '))
                      .append("0") // 初回送信
                      .append(rpad("userId_test", 20, ' '))
                      .append("20111013174932")
                      .append(rpad("", 5, ' '))
                      .append(rpad("title_test", 20, ' '))
                      .append(rpad("publisher_test", 20, ' '))
                      .append(rpad("authors_test1", 20, ' '))
                      .toString()));
    }
    private static class MockMessagingContext extends MessagingContext {

        private static final String MESSAGE_ID_PREFIX = "MID";

        private int messageIdCount = 0;
        private SendingMessage sentMessage;
        private long specifiedTimeout;
        private int timeoutCount;
        private String receivedBody;

        private MockMessagingContext(String receivedBody) {
            this(receivedBody, -1); // タイムアウトなし
        }

        private MockMessagingContext(String receivedBody, int timeoutCount) {
            this.receivedBody = receivedBody;
            this.timeoutCount = timeoutCount;
        }

        @Override
        public ReceivedMessage sendSync(SendingMessage message, long timeout) {
            String messageId = MESSAGE_ID_PREFIX + StringUtil.lpad(String.valueOf(++messageIdCount), 3, '0');
            message.setMessageId(messageId);
            sentMessage = message;
            specifiedTimeout = timeout;
            if (--timeoutCount > -1) {
                return null; // タイムアウト
            }
            ReceivedMessage responseMessage = new ReceivedMessage(receivedBody.getBytes());
            return responseMessage;
        }

        @Override
        public String sendMessage(SendingMessage message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReceivedMessage receiveMessage(String receiveQueue, String messageId, long timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * 拡張したMessageSenderClientで独自に設定した情報が取得できること。
     */
    @Test
    public void testRealtimeMessageBody() {
        initRepository("_realtime");
        prepareFormatFileRealtimeMessageBody();
        
        SyncMessage requestMessage = null;
        SyncMessage reciveMessage = null;

        //Mapを使用する場合
        requestMessage = new SyncMessage("RM21AB0100");
        Map<String, Object> dataRecord = new TreeMap<String, Object>();
        dataRecord.put("firstName", "太郎");
        dataRecord.put("lastName", "ナブラ");
        reciveMessage = MessageSender.sendSync(requestMessage);
        assertNotNull(reciveMessage);
        //独自設定項目を確認する。
        assertThat((String)reciveMessage.getHeaderRecord().get(MockMessageSenderClient.CUSTOM_KEY), is(MockMessageSenderClient.CUSTOM_VALUE));

        //Formを使用する場合
        requestMessage = new SyncMessage("RM21AB0100");
        requestMessage.addDataRecord(new Form());
        reciveMessage = MessageSender.sendSync(requestMessage);
        assertNotNull(reciveMessage);
        //独自設定項目を確認する。
        assertThat((String)reciveMessage.getHeaderRecord().get(MockMessageSenderClient.CUSTOM_KEY), is(MockMessageSenderClient.CUSTOM_VALUE));
    }

    private void prepareFormatFileRealtimeMessageBody(){
        // フォーマットファイル生成(HTTP通信　送信用)
        File formatFile = null;
        formatFile = Hereis.file(getFormatFileName("RM21AB0100_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 firstName N
        2 lastName N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(HTTP通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0100_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }


    /**
     * SyncMessagingEventHookが呼び出されること。
     */
    @Test
    public void testCallSyncMessagingEventHook() {
        initRepository("_realtime");
        prepareFormatFileCallSyncMessagingEventHook();
        
        //呼び出し回数カウント初期化
        StubSyncMessagingEventHookA.restCallCount();
        
        SyncMessage requestMessage = null;
        SyncMessage reciveMessage = null;
        Map<String, Object> dataRecord = null;
        
        //呼び出し回数カウント初期化
        StubSyncMessagingEventHookA.restCallCount();

        //正常終了時
        requestMessage = new SyncMessage("RM21AB0200");
        dataRecord = new TreeMap<String, Object>();
        dataRecord.put("firstName", "太郎");
        dataRecord.put("lastName", "ナブラ");
        reciveMessage = MessageSender.sendSync(requestMessage);
        assertNotNull(reciveMessage);
        //独自設定項目を確認する。
        assertThat((String)reciveMessage.getHeaderRecord().get(MockMessageSenderClient.CUSTOM_KEY), is(MockMessageSenderClient.CUSTOM_VALUE));
        //MessagingInsertionの呼び出し回数を確認する。
        assertThat(StubSyncMessagingEventHookA.callBeforeSendCount, is(1));
        assertThat(StubSyncMessagingEventHookA.callAfterSendCount, is(1));
        assertThat(StubSyncMessagingEventHookA.callOnErrorCount, is(0));
    }

    private void prepareFormatFileCallSyncMessagingEventHook(){
        // フォーマットファイル生成(HTTP通信　送信用)
        File formatFile = null;
        formatFile = Hereis.file(getFormatFileName("RM21AB0200_SEND"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [request]
        1 firstName N
        2 lastName N
        *******/
        formatFile.deleteOnExit();
        
        // フォーマットファイル生成(HTTP通信 受信用)
        formatFile = Hereis.file(getFormatFileName("RM21AB0200_RECEIVE"));
        /*******
        file-type:        "JSON"
        text-encoding:    "UTF-8"
        [response]
        1 messageCode X9
        2 message N
        *******/
        formatFile.deleteOnExit();
    }

    /**
     * 例外発生時、SyncMessagingEventHookが呼び出されること。
     */
    @Test
    public void testCallSyncMessagingEventHookException() {
        initRepository("_realtime");
        prepareFormatFileCallSyncMessagingEventHook();

        SyncMessage requestMessage = null;
        Map<String, Object> dataRecord = null;

        //呼び出し回数カウント初期化
        StubSyncMessagingEventHookA.restCallCount();
        StubSyncMessagingEventHookB.restCallCount();
        StubSyncMessagingEventHookC.restCallCount();

        //例外発生時
        requestMessage = new SyncMessage("RM21AB0300");
        dataRecord = new TreeMap<String, Object>();
        dataRecord.put("firstName", "太郎");
        dataRecord.put("lastName", "ナブラ");
        try{
            MessageSender.sendSync(requestMessage);
            fail();
        }catch (Exception e) {
            //MessagingInsertionの呼び出し回数を確認する。
            assertThat(StubSyncMessagingEventHookA.callBeforeSendCount, is(1));
            assertThat(StubSyncMessagingEventHookA.callAfterSendCount, is(0));
            assertThat(StubSyncMessagingEventHookA.callOnErrorCount, is(1));
        }

        //呼び出し回数カウント初期化
        StubSyncMessagingEventHookA.restCallCount();
        StubSyncMessagingEventHookB.restCallCount();
        StubSyncMessagingEventHookC.restCallCount();

        //例外発生時(複数StubSyncMessagingEventHook定義時)
        requestMessage = new SyncMessage("RM21AB0310");
        dataRecord = new TreeMap<String, Object>();
        dataRecord.put("firstName", "太郎");
        dataRecord.put("lastName", "ナブラ");
        SyncMessage recive = MessageSender.sendSync(requestMessage);
        assertNotNull(recive);
        //MessagingInsertionの呼び出し回数を確認する。
        assertThat(StubSyncMessagingEventHookB.callBeforeSendCount, is(1));
        assertThat(StubSyncMessagingEventHookB.callAfterSendCount, is(0));
        assertThat(StubSyncMessagingEventHookB.callOnErrorCount, is(1));
        assertThat(StubSyncMessagingEventHookC.callBeforeSendCount, is(1));
        assertThat(StubSyncMessagingEventHookC.callAfterSendCount, is(0));
        assertThat(StubSyncMessagingEventHookC.callOnErrorCount, is(1));
    }

    private String getFormatFileName(String formatName) {
        return FilePathSetting.getInstance().getBasePathSettings().get("format").getPath() +
                "/" + formatName + "." + 
                FilePathSetting.getInstance().getFileExtensions().get("format");
        
    }

    static public class Form{
        public String getFirstName(){
            return "太郎";
        }
        public String getLastName(){
            return "ナブラ";
        }
    }
}
