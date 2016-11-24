package nablarch.fw.messaging;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;

import nablarch.fw.messaging.realtime.http.client.HttpMessagingClient;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Test;

/**
 * {@link MessageSenderSettings}のテスト。
 * @author Kiyohito Itoh
 */
public class MessageSenderSettingsTest {

    public void initRepository(String suffix) {
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                MessageSenderSettingsTest.class.getName().replace('.', '/') + suffix + ".xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    @After
    public void tearDown() {
        SystemRepository.clear();
    }

    /**
     * リポジトリ設定に基づいた設定情報が作成されること。
     */
    @Test
    public void testSettings() {

        initRepository("");

        String requestId;
        MessageSenderSettings settings;

        // デフォルト設定を使用する場合

        requestId = "RM21AA0100";
        settings = new MessageSenderSettings(requestId);

        assertThat(settings.getMessagingProvider().toString(), is("default"));
        assertThat(settings.getSyncMessagingEventHookList().size(), is(1));
        assertThat(settings.getDestination(), is("QUEUE1"));
        assertThat(settings.getReplyTo(), is("REPLY1"));
        assertThat(settings.getTimeout(), is(-1L));
        assertThat(settings.getRetryCount(), is(3));
        assertNotNull(settings.getHeaderFormatter());
        assertNotNull(settings.getSendingDataFormatter());
        assertNotNull(settings.getReceivedDataFormatter());

        // すべてのデフォルト設定をオーバーライドする場合

        requestId = "RM21AA0101";
        settings = new MessageSenderSettings(requestId);

        assertThat(settings.getMessagingProvider().toString(), is("custom"));
        assertThat(settings.getSyncMessagingEventHookList().size(), is(2));
        assertThat(settings.getDestination(), is("QUEUE2"));
        assertThat(settings.getReplyTo(), is("REPLY2"));
        assertThat(settings.getTimeout(), is(5000L));
        assertThat(settings.getRetryCount(), is(4));
        assertNotNull(settings.getHeaderFormatter());
        assertNotNull(settings.getSendingDataFormatter());
        assertNotNull(settings.getReceivedDataFormatter());

        // 一部のデフォルト設定のみオーバーライドする場合(タイムアウトとリトライ回数のみ)

        requestId = "RM21AA0202";
        settings = new MessageSenderSettings(requestId);

        assertThat(settings.getMessagingProvider().toString(), is("default"));
        assertThat(settings.getSyncMessagingEventHookList().size(), is(1));
        assertThat(settings.getDestination(), is("QUEUE1"));
        assertThat(settings.getReplyTo(), is("REPLY1"));
        assertThat(settings.getTimeout(), is(1000L));
        assertThat(settings.getRetryCount(), is(2));
        assertNotNull(settings.getHeaderFormatter());
        assertNotNull(settings.getSendingDataFormatter());
        assertNotNull(settings.getReceivedDataFormatter());

        assertNull(settings.getComponent("hoge", MessageSenderSettings.SettingType.BOTH, false));

        try {
            settings.getComponent("hoge", MessageSenderSettings.SettingType.BOTH, true);
            fail();
        } catch (Exception e) {
            assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        }
    }

    /**
     * HTTP通信についてリポジトリ設定に基づいた設定情報が作成されること。
     */
    @Test
    public void testSettingsForRealTimeMessaging() {
        initRepository("_realtime");

        String requestId;
        MessageSenderSettings settings;

        // デフォルト設定を使用する場合
        requestId = "RM21AB0100";
        settings = new MessageSenderSettings(requestId);
        assertThat(settings.getSettingRequestId(), is(requestId));
        assertThat(settings.getSendingRequestId(), is(requestId));
        assertThat(settings.getSyncMessagingEventHookList().size(), is(1));
        assertThat(settings.getHttpMessagingUserId(), is("user01"));
        assertThat(settings.getHttpMethod(), is("POST"));
        assertThat(settings.getHttpConnectTimeout(), is(0));
        assertThat(settings.getHttpReadTimeout(), is(1));
        assertThat(settings.getHttpMessageIdGenerator().toString(), is("default"));
        assertThat(settings.getHttpProxyHost(), is("a.com"));
        assertThat(settings.getHttpProxyPort(), is(10080));
        assertThat(settings.getSslContextSettings().toString(), is("default"));
        assertThat(settings.getMessageSenderClient().toString(), is("default"));
        assertThat(((HttpMessagingClient)(settings.getMessageSenderClient())).getUserIdToFormatKey(), is("userId"));
        assertThat(settings.getUri(), is("http://localhost:8090/rm21ab0100"));

        // すべてのデフォルト設定をオーバーライドする場合
        requestId = "RM21AB0101";
        settings = new MessageSenderSettings(requestId);
        assertThat(settings.getSettingRequestId(), is(requestId));
        assertThat(settings.getSendingRequestId(), is(requestId));
        assertThat(settings.getSyncMessagingEventHookList().size(), is(2));
        assertThat(settings.getHttpMessagingUserId(), is("user02"));
        assertThat(settings.getHttpMethod(), is("GET"));
        assertThat(settings.getHttpConnectTimeout(), is(2));
        assertThat(settings.getHttpReadTimeout(), is(3));
        assertThat(settings.getHttpMessageIdGenerator().toString(), is("custom"));
        assertThat(settings.getHttpProxyHost(), is("b.com"));
        assertThat(settings.getHttpProxyPort(), is(20080));
        assertThat(settings.getSslContextSettings().toString(), is("custom"));
        assertThat(settings.getMessageSenderClient().toString(), is("default"));
        assertThat(settings.getUri(), is("http://localhost:8090/rm21ab0101"));

        // 一部のデフォルト設定のみオーバーライドする場合(HTTPメソッドのみ)
        requestId = "RM21AB0102";
        settings = new MessageSenderSettings(requestId);
        assertThat(settings.getSettingRequestId(), is(requestId));
        assertThat(settings.getSendingRequestId(), is(requestId));
        assertThat(settings.getHttpMessagingUserId(), is("user01"));
        assertThat(settings.getHttpMethod(), is("PUT"));
        assertThat(settings.getHttpConnectTimeout(), is(0));
        assertThat(settings.getHttpReadTimeout(), is(1));
        assertThat(settings.getHttpProxyHost(), is("a.com"));
        assertThat(settings.getHttpProxyPort(), is(10080));
        assertThat(settings.getSslContextSettings().toString(), is("default"));
        assertThat(settings.getMessageSenderClient().toString(), is("default"));
        assertThat(settings.getUri(), is("http://localhost:8090/rm21ab0102"));
    }
    
    /**
     * HTTP通信について、任意項目は設定しなくても問題ないこと。
     */
    @Test
    public void testSettingsForRealTimeMessagingValid1() {
        initRepository("_realtime_valid1");

        String requestId;
        MessageSenderSettings settings;

        // デフォルト設定を使用する場合
        requestId = "RM21AB0100";
        settings = new MessageSenderSettings(requestId);
        assertThat(settings.getSettingRequestId(), is(requestId));
        assertThat(settings.getSendingRequestId(), is(requestId));
        assertThat(settings.getHttpMessagingUserId(), is("user01"));
        assertThat(settings.getHttpMethod(), is("GET"));
        assertThat(settings.getHttpConnectTimeout(), is(0));
        assertThat(settings.getHttpReadTimeout(), is(0));
        assertNull(settings.getHttpProxyHost());
        assertNull(settings.getHttpProxyPort());
        assertNull(settings.getSslContextSettings());
        assertNotNull(settings.getMessageSenderClient());
        assertThat(settings.getUri(), is("http://localhost:8090/rm21ab0100"));
    }

    /**
     * HTTP通信用の項目について、不正な設定に対するエラーメッセージが出力されること。
     */
    @Test
    public void testInvalidSettingsForRealTimeMessaging() {
        initRepository("_realtime_invalid1");

        String requestId;

        try {
            requestId = "RM21AB0100";
            new MessageSenderSettings(requestId);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("component was not found. componentName = [invalidMessagingInsertion], defaultKey = [messageSender.DEFAULT.syncMessagingEventHookNames] or key = [messageSender.RM21AB0100.syncMessagingEventHookNames]"));
        }

        try {
            requestId = "RM21AB0101";
            new MessageSenderSettings(requestId);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("syncMessagingEventHookNames could not be converted to List<SyncMessagingEventHook> type. value = [nopSSLContextSettings], defaultKey = [messageSender.DEFAULT.syncMessagingEventHookNames] or key = [messageSender.RM21AB0101.syncMessagingEventHookNames]"));
        }
    }
        
    /**
     * 不正な設定に対するエラーメッセージが出力されること。
     */
    @Test
    public void testInvalidSettings() {

        initRepository("_invalid");

        // 必須の設定値が指定されていない場合
        try {
            new MessageSenderSettings("PROVIDER_COMPONENT_NAME");
            fail("PROVIDER_COMPONENT_NAME");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("messagingProviderName was not specified. defaultKey = [messageSender.DEFAULT.messagingProviderName] or key = [messageSender.PROVIDER_COMPONENT_NAME.messagingProviderName]"));
        }
        try {
            new MessageSenderSettings("DESTINATION_REQUIRED");
            fail("DESTINATION_REQUIRED");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("destination was not specified. defaultKey = [messageSender.DEFAULT.destination] or key = [messageSender.DESTINATION_REQUIRED.destination]"));
        }
        try {
            new MessageSenderSettings("REPLY_TO_REQUIRED");
            fail("REPLY_TO_REQUIRED");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("replyTo was not specified. defaultKey = [messageSender.DEFAULT.replyTo] or key = [messageSender.REPLY_TO_REQUIRED.replyTo]"));
        }
        try {
            new MessageSenderSettings("HEADER_FORMAT_NAME_REQUIRED");
            fail("HEADER_FORMAT_NAME_REQUIRED");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("headerFormatName was not specified. defaultKey = [messageSender.DEFAULT.headerFormatName] or key = [messageSender.HEADER_FORMAT_NAME_REQUIRED.headerFormatName]"));
        }

        // Integer型の設定値が変換できない場合
        try {
            new MessageSenderSettings("INVALID_INTEGER");
            fail("INVALID_INTEGER");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("retryCount could not be converted to Integer type. value = [aaa], defaultKey = [messageSender.DEFAULT.retryCount] or key = [messageSender.INVALID_INTEGER.retryCount]"));
            assertThat(e.getCause().getClass().getName(), is(NumberFormatException.class.getName()));
        }

        // Long型の設定値が変換できない場合
        try {
            new MessageSenderSettings("INVALID_LONG");
            fail("INVALID_LONG");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("timeout could not be converted to Long type. value = [bbb], key = [messageSender.INVALID_LONG.timeout]"));
            assertThat(e.getCause().getClass().getName(), is(NumberFormatException.class.getName()));
        }

        // フォーマッタが取得できない場合
        try {
            new MessageSenderSettings("HEADER_FORMAT_FILE_NOT_FOUND");
            fail("HEADER_FORMAT_FILE_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
            assertThat(cause.getMessage(), is(allOf(
                    containsString("invalid layout file path was specified."),
                    containsString("file path=["),
                    containsString("unknown.fmt")
            )));

            assertThat(e.getMessage(), CoreMatchers.is("failed to parse format file."
                    + " requestId = [HEADER_FORMAT_FILE_NOT_FOUND],"
                    + " defaultKey = [messageSender.DEFAULT.headerFormatName]"));
        }
        try {
            new MessageSenderSettings("SENDING_DATA_FORMAT_FILE_NOT_FOUND");
            fail("SENDING_DATA_FORMAT_FILE_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
            assertThat(cause.getMessage(), is(allOf(
                    containsString("invalid layout file path was specified."),
                    containsString("file path=["),
                    containsString("SENDING_DATA_FORMAT_FILE_NOT_FOUND_SEND.fmt")
            )));

            assertThat(e.getMessage(), CoreMatchers.is("failed to parse format file."
                    + " requestId = [SENDING_DATA_FORMAT_FILE_NOT_FOUND]"));
        }
        try {
            new MessageSenderSettings("SENDING_DATA_FORMAT_FILE_NOT_FOUND");
            fail("SENDING_DATA_FORMAT_FILE_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            Throwable cause = e.getCause();
            assertThat(cause, is(instanceOf(IllegalArgumentException.class)));
            assertThat(cause.getMessage(), is(allOf(
                    containsString("invalid layout file path was specified."),
                    containsString("file path=["),
                    containsString("SENDING_DATA_FORMAT_FILE_NOT_FOUND_SEND.fmt")
            )));

            assertThat(e.getMessage(), CoreMatchers.is("failed to parse format file."
                    + " requestId = [SENDING_DATA_FORMAT_FILE_NOT_FOUND]"));
        }

        initRepository("_invalid2");

        try {
            new MessageSenderSettings("FORMAT_DIR_NOT_FOUND");
            fail("FORMAT_DIR_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("formatDir was not found. formatDir = [unknown], defaultKey = [messageSender.DEFAULT.formatDir]"));
            assertThat(e.getCause().getClass().getName(), is(IllegalArgumentException.class.getName()));
        }

        initRepository("_invalid3");

        try {
            new MessageSenderSettings("MESSAGING_PROVIDER_NOT_FOUND");
            fail("MESSAGING_PROVIDER_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("component was not found. componentName = [unknown], defaultKey = [messageSender.DEFAULT.messagingProviderName] or key = [messageSender.MESSAGING_PROVIDER_NOT_FOUND.messagingProviderName]"));
        }

        initRepository("_invalid4");

        try {
            new MessageSenderSettings("MESSAGE_CONVERTOR_NOT_FOUND");
            fail("MESSAGE_CONVERTOR_NOT_FOUND");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("component was not found. componentName = [unknown], defaultKey = [messageSender.DEFAULT.messageConvertorName] or key = [messageSender.MESSAGE_CONVERTOR_NOT_FOUND.messageConvertorName]"));
        }
    }
}