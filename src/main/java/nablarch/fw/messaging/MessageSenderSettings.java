package nablarch.fw.messaging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * {@link MessageSender}の設定情報を保持するクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class MessageSenderSettings {

    /** 設定情報キーのセパレータ */
    private static final String KEY_SEPARATOR = ".";

    /** 設定情報キーのプレフィックス */
    private static final String KEY_PREFIX = "messageSender";

    /** 設定情報キーのデフォルト設定に使用するターゲット */
    private static final String KEY_DEFAULT_TARGET = "DEFAULT";

    /** 設定情報キーのリクエストID */
    private final String settingRequestId;

    /** 送信用リクエストID */
    private final String sendingRequestId;

    /** {@link nablarch.fw.messaging.MessagingProvider} */
    private MessagingProvider messagingProvider = null;

    /** 送信キュー名(論理名) */
    private String destination = null;

    /** 受信キュー名(論理名) */
    private String replyTo = "";

    /** リトライ回数 */
    private int retryCount = -1;

    /** 応答タイムアウト(単位:ミリ秒) */
    private long timeout = -1L;

    /** ヘッダのフォーマッタ(送信電文と受信電文で共通) */
    private DataRecordFormatter headerFormatter = null;

    /** 送信電文データのフォーマッタ */
    private DataRecordFormatter sendingDataFormatter = null;

    /** 受信電文データのフォーマッタ */
    private DataRecordFormatter receivedDataFormatter = null;

    /** {@link nablarch.fw.messaging.SyncMessageConvertor} */
    private SyncMessageConvertor messageConvertor = null;

    /**メッセージ送信の処理前後に処理を行うためのインターフェイス*/
    private final List<SyncMessagingEventHook> syncMessagingEventHookList;
    
    /** MessageSenderから呼び出される基本APIを実装したインターフェース*/
    private final MessageSenderClient messageSenderClient;

    /** HTTP通信に使用するユーザID*/
    private String httpMessagingUserId = "";

    /** HTTP通信で使用する接続タイムアウト*/
    private int httpConnectTimeout = 0;

    /** HTTP通信で使用する読み取りタイムアウト*/
    private int httpReadTimeout = 0;

    /** HTTP通信用URI*/
    private String uri = "";
    
    /** HTTP通信で使用するHTTPメソッド*/
    private String httpMethod = "";
    
    /** HTTP通信で使用するメッセージID採番クラス*/
    private HttpMessageIdGenerator httpMessageIdGenerator = null;

    /** HTTP通信で使用するSSL情報*/
    private HttpSSLContextSettings sslContextSettings = null;

    /** HTTP通信で使用するプロキシのホスト*/
    private String httpProxyHost = null;

    /** HTTP通信で使用するプロキシのホスト*/
    private Integer httpProxyPort = null;;

    /**
     * コンストラクタ。
     * <pre>
     * リポジトリから設定値を取得し初期化を行う。
     * 
     * &lt;キューを使用した通信で使用する設定項目&gt;
     * デフォルト設定
     * messageSender.DEFAULT.messagingProviderName=MessagingProviderをリポジトリから取得する際に使用するコンポーネント名
     * messageSender.DEFAULT.destination=送信キュー名(論理名)
     * messageSender.DEFAULT.replyTo=受信キュー名(論理名)
     * messageSender.DEFAULT.retryCount=タイムアウト発生時の再送回数。再送しない場合は0以下を指定。デフォルトは-1
     * messageSender.DEFAULT.formatDir=フォーマット定義ファイルの格納ディレクトリ(論理名)。デフォルトはformat
     * messageSender.DEFAULT.headerFormatName=ヘッダフォーマット名
     * messageSender.DEFAULT.messageConvertorName=SyncMessageConvertorをリポジトリから取得する際に使用するコンポーネント名
     *
     * 個別設定
     * messageSender.リクエストID.messagingProviderName=MessagingProviderをリポジトリから取得する際に使用するコンポーネント名。デフォルト設定を指定しない場合は必須
     * messageSender.リクエストID.destination=送信キュー名(論理名)。デフォルト設定を指定しない場合は必須
     * messageSender.リクエストID.replyTo=受信キュー名(論理名)。デフォルト設定を指定しない場合は必須
     * messageSender.リクエストID.timeout=応答タイムアウト(単位:ミリ秒)。デフォルトは-1。0以下または指定がない場合はMessagingProviderの設定値となる
     * messageSender.リクエストID.retryCount=タイムアウト発生時の再送回数。再送しない場合は0以下を指定
     * messageSender.リクエストID.headerFormatName=ヘッダフォーマット名。デフォルト設定を指定しない場合は必須
     * messageSender.リクエストID.sendingRequestId=送信用リクエストID。メッセージ処理用のリクエストIDが重複する場合に使用する。
     *                                             送信用リクエストIDが指定された場合は、送信用リクエストIDの値をヘッダのリクエストIDに設定する。
     * messageSender.リクエストID.messageConvertorName=SyncMessageConvertorをリポジトリから取得する際に使用するコンポーネント名
     * 
     * 
     * &lt;HTTP通信で使用する設定項目&gt;
     * 以下の項目が定義されているリクエストについて、「HTTP通信を行う」とみなす。
     * ・messageSender.リクエストID.messageSenderClient
     * 
     * デフォルト設定
     * messageSender.DEFAULT.httpMessagingUserId=フレームワーク制御ヘッダーに設定するユーザID
     * messageSender.DEFAULT.httpMethod=通信に使用するHTTPメソッド
     * messageSender.DEFAULT.httpConnectTimeout=コネクションタイムアウト(単位:ミリ秒)。0の場合は、サーバから切断されるまで待ち続ける。
     * messageSender.DEFAULT.httpReadTimeout=読み取りタイムアウト(単位:ミリ秒)。0の場合は、サーバからデータを読み終わるまで待ち続ける。
     * messageSender.DEFAULT.sslContextComponentName=SSLContext取得コンポーネント(論理名)。SSL通信時、証明書の設定を行いたい場合に設定する。
     * messageSender.DEFAULT.httpProxyHost=プロキシサーバ
     * messageSender.DEFAULT.httpProxyPort=プロキシサーバのポート
     * messageSender.DEFAULT.httpMessageIdGeneratorComponentName=HTTPヘッダに付与するメッセージID(キー名：X-Message-Id)の採番コンポーネント(任意項目)。
     * 
     * 個別設定
     * messageSender.リクエストID.messageSenderClient=MessageSenderClient通信クライアント(論理名)。HTTP通信時は必須。
     * messageSender.リクエストID.httpMessagingUserId=フレームワーク制御ヘッダーに設定するユーザID。任意項目。
     * messageSender.リクエストID.uri=接続先uri。必須項目
     * messageSender.リクエストID.httpMethod=通信に使用するHTTPメソッド
     * messageSender.リクエストID.httpConnectTimeout=コネクションタイムアウト(単位:ミリ秒)。デフォルト値にも、本項目にも指定がない場合は、0を設定したとみなす。
     * messageSender.リクエストID.httpReadTimeout=コネクションタイムアウト(単位:ミリ秒)。デフォルト値にも、本項目にも指定がない場合は、0を設定したとみなす。
     * messageSender.リクエストID.sslContextComponentName=SSLContext取得コンポーネント(論理名)。任意項目。
     * messageSender.リクエストID.httpProxyHost=プロキシサーバ
     * messageSender.リクエストID.httpProxyPort=プロキシサーバのポート
     * messageSender.リクエストID.httpMessageIdGeneratorComponentName=HTTPヘッダに付与するメッセージID(キー名：X-Message-Id)の採番コンポーネント(任意項目)。
     * 
     * 
     * &lt;キューを使用した通信、HTTP通信共通事項&gt;
     * デフォルト設定
     * messageSender.DEFAULT.syncMessagingEventHookNames=同期送信の前後処理をリポジトリから取得する際に使用するコンポーネント名(論理名)。複数指定可（「,」で区切って指定）。任意項目。
     * 
     * 個別設定
     * messageSender.リクエストID.syncMessagingEventHookNames=同期送信の前後処理をリポジトリから取得する際に使用するコンポーネント名(論理名)。複数指定可（「,」で区切って指定）。任意項目。
     * 
     * 送信電文データと受信電文データのフォーマッタは下記のフォーマット名から取得する。
     * 
     *     送信電文データのフォーマット名: リクエストID＋"_SEND"
     *     受信電文データのフォーマット名: リクエストID＋"_RECEIVE"
     * </pre>
     * @param requestId リクエストID
     */
    public MessageSenderSettings(String requestId) {

        this.settingRequestId = requestId;

        SettingType settingType;
        boolean required;

        //キューを用いた通信、HTTP通信に共通する設定を取得する。
        settingType = SettingType.BOTH;
        required = false;
        List<Object> insertionObjectList = getComponentList("syncMessagingEventHookNames", settingType, required);
        syncMessagingEventHookList = new ArrayList<SyncMessagingEventHook>();
        for (Object insertion : insertionObjectList) {
            //明示的に型チェックを行う(このタイミングで型チェックを行わないと、設定ファイルの記述ミスが存在した旨がわかりにくいため)。
            if (insertion instanceof SyncMessagingEventHook) {
                syncMessagingEventHookList.add((SyncMessagingEventHook) insertion);
            } else {
                String componentNames = getStringSetting("syncMessagingEventHookNames", settingType, required, null);
                throw new IllegalArgumentException(
                        String.format(
                                "syncMessagingEventHookNames could not be converted to List<SyncMessagingEventHook> type. value = [%s], %s",
                                componentNames, createSettingKeyMessage(settingType, "syncMessagingEventHookNames")));
            }
        }
        
        //messageSenderClientが設定されているか否かを取得する。
        settingType = SettingType.REQUEST_ID_ONLY;
        required = false;
        messageSenderClient = getComponent("messageSenderClient", settingType, required);
        if (canUseMessageSenderClient()) {
            //HTTP通信で使用する項目を読み込む。

            settingType = SettingType.REQUEST_ID_ONLY;
            required = false;
            sendingRequestId = getStringSetting("sendingRequestId", settingType, required, requestId);

            settingType = SettingType.BOTH;
            required = false;
            httpMessagingUserId = getStringSetting("httpMessagingUserId", settingType, required, "");
            
            settingType = SettingType.REQUEST_ID_ONLY;
            required = true;
            uri = getStringSetting("uri", settingType, required, null);

            settingType = SettingType.BOTH;
            required = false;
            httpReadTimeout =  getIntSetting("httpReadTimeout", settingType, required, 0);
            
            settingType = SettingType.BOTH;
            required = false;
            httpConnectTimeout =  getIntSetting("httpConnectTimeout", settingType, required, 0);

            settingType = SettingType.BOTH;
            required = false;
            sslContextSettings = getComponent("sslContextComponentName", settingType, required);

            settingType = SettingType.BOTH;
            required = false;
            httpProxyHost = getStringSetting("httpProxyHost", settingType, required, null);
            httpProxyPort = getIntSetting("httpProxyPort", settingType, required, null);

            settingType = SettingType.BOTH;
            required = true;
            httpMethod = getStringSetting("httpMethod", settingType, required, null);
            
            settingType = SettingType.BOTH;
            required = false;
            httpMessageIdGenerator = getComponent("httpMessageIdGeneratorComponentName", settingType, required);
        } else {
            //キューを用いた通信用の項目を読み込む
            
            // デフォルト設定と個別設定の両方、かつ必須
            settingType = SettingType.BOTH;
            required = true;
            messagingProvider = getComponent("messagingProviderName", settingType, required);
            destination = getStringSetting("destination", settingType, required, null);
            replyTo = getStringSetting("replyTo", settingType, required, null);
            String headerFormatName = getStringSetting("headerFormatName", settingType, required, null);

            // デフォルト設定と個別設定の両方、かつオプション
            settingType = SettingType.BOTH;
            required = false;
            retryCount = getIntSetting("retryCount", settingType, required, -1);
            messageConvertor = getComponent("messageConvertorName", settingType, required);

            // 個別設定のみ、かつオプション
            settingType = SettingType.REQUEST_ID_ONLY;
            required = false;
            timeout = getLongSetting("timeout", settingType, required, -1L);
            sendingRequestId = getStringSetting("sendingRequestId", settingType, required, requestId);

            // デフォルト設定のみ、かつオプション
            settingType = SettingType.DEFAULT_ONLY;
            required = false;
            String formatDir = getStringSetting("formatDir", settingType, required, "format");
            try {
                FilePathSetting.getInstance().getBaseDirectory(formatDir);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("formatDir was not found. formatDir = [%s], %s",
                                  formatDir,
                                  createSettingKeyMessage(SettingType.DEFAULT_ONLY, "formatDir")),
                    e);
            }

            // フォーマッタの初期化
            headerFormatter = getFormatter("headerFormatName", settingType, formatDir, headerFormatName);
            sendingDataFormatter = getFormatter(null, null, formatDir, requestId + "_SEND");
            receivedDataFormatter = getFormatter(null, null, formatDir, requestId + "_RECEIVE");
        }
    }

    /**
     * messageSenderClientを使用した通信を行うか否かを取得する。
     * @return trueの場合にmessageSenderClientを使用した通信を行う。
     */
    public boolean canUseMessageSenderClient() {
        //messageSenderClientが設定されていれば、messageSenderClientを用いた通信を行うべきとみなす。
        return messageSenderClient != null;
    }

    /**
     * コンポーネント名の設定値を使用してリポジトリからコンポーネントを取得する。
     * <pre>
     * {@link #getStringSetting(String, SettingType, boolean, String)}メソッドを使用して
     * 取得したコンポーネント名を使用してリポジトリからコンポーネントを取得する。
     * コンポーネント名取得の詳細は{@link #getStringSetting(String, SettingType, boolean, String)}メソッド
     * のJavaDocを参照。
     * 
     * コンポーネント名が指定され、かつrequired属性がtrueの場合に、
     * リポジトリからコンポーネントが取得できない場合は実行時例外を送出する。
     * </pre>
     * @param <T> コンポーネントの型
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param required 必須の場合はtrue
     * @return コンポーネント
     */
    public <T> T getComponent(String propertyName, SettingType settingType, boolean required) {
        String componentName = getStringSetting(propertyName, settingType, required, null);
        if (componentName == null && !required) {
            return null;
        }
        T component = SystemRepository.<T>get(componentName);
        if (component == null) {
            throw new IllegalArgumentException(
                    String.format("component was not found. "
                                + "componentName = [%s], %s",
                                  componentName,
                                  createSettingKeyMessage(settingType, propertyName)));
        }
        return component;
    }

    /**
     * コンポーネント名の設定値を使用してリポジトリからコンポーネントを取得する(「,」区切りで定義された複数コンポーネントの読み込みに対応)。
     * <pre>
     * {@link #getStringSetting(String, SettingType, boolean, String)}メソッドを使用して
     * 取得したコンポーネント名を使用してリポジトリからコンポーネントを取得する。
     * コンポーネント名取得の詳細は{@link #getStringSetting(String, SettingType, boolean, String)}メソッド
     * のJavaDocを参照。
     * 
     * コンポーネント名が指定され、かつrequired属性がtrueの場合に、
     * リポジトリからコンポーネントが取得できない場合は実行時例外を送出する。
     * </pre>
     * @param <T> コンポーネントの型
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param required 必須の場合はtrue
     * @return コンポーネント
     */
    public <T> List<T> getComponentList(String propertyName, SettingType settingType, boolean required) {
        List<T> componentList = new ArrayList<T>();
        String componentNames = getStringSetting(propertyName, settingType, required, null);
        if (componentNames != null) {
            //必須/任意問わず、項目が設定されている場合に処理する
            //(必須項目なのに、値が設定されていない場合は、前段のgetStringSetting呼び出し時に例外が発生する)
            String[] componentNameArray = componentNames.split(",");
            for (String componentName : componentNameArray) {
                T t = SystemRepository.<T>get(componentName.trim());
                if (t == null) {
                    throw new IllegalArgumentException(
                            String.format("component was not found. "
                                        + "componentName = [%s], %s",
                                          componentName.trim(),
                                          createSettingKeyMessage(settingType, propertyName)));
                }
                componentList.add(SystemRepository.<T>get(componentName.trim()));
            }
        }
        return componentList;
    }

    /**
     * 指定されたフォーマット名に対応するフォーマッタを取得する。
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param formatDir フォーマット定義ファイルの格納ディレクトリ(論理名)
     * @param formatName フォーマット名
     * @return 指定されたフォーマット名に対応するフォーマッタ
     */
    public DataRecordFormatter getFormatter(String propertyName, SettingType settingType,
                                             String formatDir, String formatName) {
        File formatFile = FilePathSetting.getInstance().getFileWithoutCreate(formatDir, formatName);
        try {
            return FormatterFactory.getInstance().createFormatter(formatFile);
        } catch (RuntimeException e) {
            String message = "failed to parse format file. requestId = [" + settingRequestId + ']';
            if (propertyName != null) {
                message += ", " + createSettingKeyMessage(settingType, propertyName);
            }
            throw new IllegalArgumentException(message, e);
        }
    }

    /**
     * 設定情報キーのリクエストIDを取得する。
     * @return 設定情報キーのリクエストID
     */
    public String getSettingRequestId() {
        return settingRequestId;
    }

    /**
     * 送信用リクエストIDを取得する。
     * <p/>
     * 送信用リクエストIDが設定されない場合は、設定情報キーのリクエストIDが返される。
     * 
     * @return 送信用リクエストID
     */
    public String getSendingRequestId() {
        return sendingRequestId;
    }

    /**
     * {@link nablarch.fw.messaging.MessagingProvider}を取得する。
     * @return {@link nablarch.fw.messaging.MessagingProvider}
     */
    public MessagingProvider getMessagingProvider() {
        return messagingProvider;
    }

    /**
     * {@link nablarch.fw.messaging.SyncMessageConvertor}を取得する。
     * @return {@link nablarch.fw.messaging.SyncMessageConvertor}。
     *          指定がない場合はnull
     */
    public SyncMessageConvertor getMessageConvertor() {
        return messageConvertor;
    }

    /**
     * 送信キュー名(論理名)を取得する。
     * @return 送信キュー名(論理名)
     */
    public String getDestination() {
        return destination;
    }

    /**
     * 受信キュー名(論理名)を取得する。
     * @return 受信キュー名(論理名)
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * リトライ回数を取得する。
     * @return リトライ回数
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * 応答タイムアウト(単位:ミリ秒)を取得する。
     * @return 応答タイムアウト(単位:ミリ秒)
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * ヘッダのフォーマッタ(送信電文と受信電文で共通)を取得する。
     * @return ヘッダのフォーマッタ(送信電文と受信電文で共通)
     */
    public DataRecordFormatter getHeaderFormatter() {
        return headerFormatter;
    }


    /**
     * 送信電文データのフォーマッタを取得する。
     * @return 送信電文データのフォーマッタ
     */
    public DataRecordFormatter getSendingDataFormatter() {
        return sendingDataFormatter;
    }

    /**
     * 受信電文データのフォーマッタを取得する。
     * @return 受信電文データのフォーマッタ
     */
    public DataRecordFormatter getReceivedDataFormatter() {
        return receivedDataFormatter;
    }

    /**
     * メッセージ送信の処理前後に行う処理を取得する。
     * @return メッセージ送信の処理前後に行う処理
     */
    public List<SyncMessagingEventHook> getSyncMessagingEventHookList() {
        return syncMessagingEventHookList;
    }

    /**
     * MessageSenderから呼び出される基本APIを実装したインターフェースを取得する。<br>
     * <p>
     * {@link MessagingProvider}と{@link MessageSenderClient}が共に本クラスに設定されている場合は、{@link MessageSenderClient}を優先的に使用する。
     * </p>
     * </br>
     * @return MessageSenderから呼び出される基本APIを実装したインターフェース
     */
    public MessageSenderClient getMessageSenderClient() {
        return messageSenderClient;
    }

    /**
     * リアルタイム通信で使用するユーザIDを取得する。
     * @return リアルタイム通信で使用するユーザID
     */
    public String getHttpMessagingUserId() {
        return httpMessagingUserId;
    }

    /**
     * HTTP通信用接続タイムアウトを取得する。
     * @return 接続タイムアウト
     */
    public int getHttpConnectTimeout() {
        return httpConnectTimeout;
    }

    /**
     * HTTP通信用読み取りタイムアウトを取得する。
     * @return 読み取りタイムアウト
     */
    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }

    /**
     * HTTPMethodを取得する。
     * @return HTTPMethod
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * HTTP通信時に使用するメッセージID採番コンポーネントを取得する。
     * @return HTTP通信の接続先URI
     */
    public HttpMessageIdGenerator getHttpMessageIdGenerator() {
        return httpMessageIdGenerator;
    }

    /**
     * HTTP通信の接続先URIを取得する。
     * @return HTTP通信の接続先URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * HTTP通信時に使用するSSLContextを取得する。
     * @return HTTP通信の接続先URI
     */
    public HttpSSLContextSettings getSslContextSettings() {
        return sslContextSettings;
    }

    /**
     * HTTP通信時に使用するProxyのホストを取得する。
     * @return HTTP通信時に使用するProxyのホスト
     */
    public String getHttpProxyHost() {
        return httpProxyHost;
    }

    /**
     * HTTP通信時に使用するProxyのポートを取得する。
     * @return HTTP通信時に使用するProxyのポート
     */
    public Integer getHttpProxyPort() {
        return httpProxyPort;
    }

    /**
     * 設定情報キーを作成する。
     * <pre>
     * 設定情報キーの形式は下記のとおり。
     * 
     *     "messageSender" + "." + ターゲット名 + "." + プロパティ名
     * 
     * ターゲット名の値は下記のとおり。
     * 
     *     デフォルト設定の場合: "DEFAULT"
     *     個別設定の場合: リクエストID
     * 
     * </pre>
     * @param targetName ターゲット名
     * @param propertyName プロパティ名
     * @return 設定情報キー
     */
    private String createSettingKey(String targetName, String propertyName) {
        return Builder.join(new Object[] {KEY_PREFIX, targetName, propertyName}, KEY_SEPARATOR);
    }

    /**
     * Integer型の設定値を取得する。
     * <pre>
     * {@link #getStringSetting(String, SettingType, boolean, String)}メソッドを使用して
     * 取得した設定値をInteger型に変換して返す。
     * 設定値取得の詳細は{@link #getStringSetting(String, SettingType, boolean, String)}メソッド
     * のJavaDocを参照。
     * </pre>
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param required 必須の場合はtrue
     * @param defaultValue デフォルト値
     * @return Integer型の設定値
     */
    public Integer getIntSetting(String propertyName, SettingType settingType, boolean required, Integer defaultValue) {
        String string = getStringSetting(propertyName, settingType, required, null);
        if (StringUtil.isNullOrEmpty(string)) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("%s could not be converted to Integer type. value = [%s], %s",
                        propertyName, string,
                        createSettingKeyMessage(settingType, propertyName)),
                    e);
        }
    }

    /**
     * Long型の設定値を取得する。
     * <pre>
     * {@link #getStringSetting(String, SettingType, boolean, String)}メソッドを使用して
     * 取得した設定値をLong型に変換して返す。
     * 設定値取得の詳細は{@link #getStringSetting(String, SettingType, boolean, String)}メソッド
     * のJavaDocを参照。
     * </pre>
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param required 必須の場合はtrue
     * @param defaultValue デフォルト値
     * @return Long型の設定値
     */
    public Long getLongSetting(String propertyName, SettingType settingType, boolean required, Long defaultValue) {
        String string = getStringSetting(propertyName, settingType, required, null);
        if (StringUtil.isNullOrEmpty(string)) {
            return defaultValue;
        }
        try {
            return Long.valueOf(string);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format("%s could not be converted to Long type. value = [%s], %s",
                        propertyName, string,
                        createSettingKeyMessage(settingType, propertyName)),
                    e);
        }
    }

    /**
     * String型の設定値を取得する。
     * <pre>
     * {@link #createSettingKey(String, String)}メソッドを使用し設定情報キーを取得する。
     * 
     * はじめに個別設定の取得を試み、取得できない場合はデフォルト設定の取得を試みる。
     * ただし、settingType引数の指定に応じて個別設定とデフォルト設定の取得を行う。
     * 
     * 設定値を取得できない、かつrequired引数がtrueの場合は、実行時例外を送出する。
     * 設定値を取得できない、かつrequired引数がfalseの場合は、デフォルト値を返す。
     * </pre>
     * @param propertyName プロパティ名
     * @param settingType 設定値のタイプ
     * @param required 必須の場合はtrue
     * @param defaultValue デフォルト値
     * @return String型の設定値
     */
    public String getStringSetting(String propertyName, SettingType settingType, boolean required, String defaultValue) {

        String value = null;

        String key = null;
        if (SettingType.REQUEST_ID_ONLY == settingType || SettingType.BOTH == settingType) {
            // 個別設定の取得
            key = createSettingKey(settingRequestId, propertyName);
            value = SystemRepository.getString(key);
        }

        String defaultKey = null;
        if (StringUtil.isNullOrEmpty(value)
                && (SettingType.DEFAULT_ONLY == settingType || SettingType.BOTH == settingType)) {
            // デフォルト設定の取得
            defaultKey = createSettingKey(KEY_DEFAULT_TARGET, propertyName);
            value = SystemRepository.getString(defaultKey);
        }

        if (StringUtil.isNullOrEmpty(value)) {

            if (required) {
                // 設定値を取得できない、かつ必須
                throw new IllegalArgumentException(
                    String.format("%s was not specified. %s",
                        propertyName, createSettingKeyMessage(settingType, propertyName)));
            }

            // 設定値を取得できない、かつ必須でない
            value = defaultValue;
        }

        return value;
    }

    /**
     * エラーメッセージに付加する設定値のタイプに応じた設定情報キーのメッセージを作成する。
     * <pre>
     * メッセージの形式は下記のとおり。
     * 
     *     デフォルト設定のみ            : "defaultKey = [デフォルト設定キー]"
     *     個別設定のみ                  : "key = [個別設定キー]"
     *     デフォルト設定と個別設定の両方: "defaultKey = [デフォルト設定キー] or key = [個別設定キー]"
     * 
     * キーは{@link #createSettingKey(String, String)}メソッドを使用して取得する。
     * </pre>
     * @param settingType 設定値のタイプ
     * @param propertyName プロパティ名
     * @return エラーメッセージに付加する設定値のタイプに応じた設定情報キーのメッセージ
     */
    public String createSettingKeyMessage(SettingType settingType, String propertyName) {
        switch (settingType) {
            case DEFAULT_ONLY:
                return String.format("defaultKey = [%s]", createSettingKey(KEY_DEFAULT_TARGET, propertyName));
            case REQUEST_ID_ONLY:
                return String.format("key = [%s]", createSettingKey(settingRequestId, propertyName));
            default:
                return String.format("defaultKey = [%s] or key = [%s]",
                                      createSettingKey(KEY_DEFAULT_TARGET, propertyName),
                                      createSettingKey(settingRequestId, propertyName));
        }
    }

    /**
     * 設定値のタイプを表す列挙型。
     * @author Kiyohito Itoh
     */
    public static enum SettingType {
        /** デフォルト設定のみ */
        DEFAULT_ONLY,
        /** 個別設定のみ */
        REQUEST_ID_ONLY,
        /** デフォルト設定と個別設定の両方 */
        BOTH;
    }
}
