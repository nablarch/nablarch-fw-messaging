package nablarch.fw.messaging.reader;

import java.io.File;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.DataReader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.MessageReadError;
import nablarch.fw.messaging.MessagingContext;
import nablarch.fw.messaging.ReceivedMessage;

/** 
 * 指定されたメッセージキューを監視し、受信した電文オブジェクトを返すデータリーダ。
 * <p/>
 * 受信電文読み込み時にエラーが発生した場合は、例外({@link MessageReadError})を送出する。
 *
 * @author Iwauo Tajima
 * @see DataReader
 */
public class MessageReader implements DataReader<ReceivedMessage> {
    // ----------------------------------------------------- structure
    /** このリーダが監視するキューの論理名 */
    private String receiveQueueName;    
    
    /** このリーダが閉じられたかどうか。 */
    private boolean closed = false;
    
    /** キューが空の場合に待機する最大時間。(単位：ミリ秒)  */
    private long timeout = 5000; 
    
    /** フォーマット定義ファイルが配置されているディレクトリの論理名 */
    private String formatFileDirName = "format";
    
    /** フォーマット定義ファイルの名前 */
    private String formatFileName = null;
    
    // ----------------------------------------------------- DataReader I/F
    /**
     * 受信電文を読み込む。
     * <p/>
     * カレントスレッドに紐づけられた{@link MessagingContext}
     * オブジェクトを使用して受信キューから電文を取得し返却する。<br/>
     * 受信キュー上に電文が1件も無い場合は、新規電文を受信するか、タイムアウト時間まで待機する。<br/>
     * (このタイムアウトは各スレッドが開閉局やプロセス停止フラグ等の実行制御系の
     * ステータスを確認するために必要となる。)<br/>
     * 既にリーダが閉じられていた場合、またはタイムアウトした場合は{@code null}を返却する。
     *
     * @param ctx 実行コンテキスト
     * @return 受信電文オブジェクト
     * @throws IllegalStateException 受信キューの論理名が{@code null}の場合
     * @throws RuntimeException 実行時例外が発生した場合
     * @throws Error エラーが発生した場合
     * @throws MessageReadError 受信電文オブジェクトの設定中に
     *                           実行時例外またはエラーが発生した場合
     */
    public ReceivedMessage read(ExecutionContext ctx) {
        if (closed) {
            return null;
        }
        if (receiveQueueName == null) {
            throw new IllegalStateException(
                "the name of the queue this reader watches must be assigned."
            );
        }
        ReceivedMessage message = null;
        try {
            message = MessagingContext.getInstance()
                                      .receiveSync(receiveQueueName, timeout);
            if (message == null) {
                return null; // timeout
            }
            if (formatFileName != null) {
                message.setFormatter(getFormatter());
            }
            return message;
            
        } catch (RuntimeException e) {
            if (message == null) {
                throw e;
            }
            throw new MessageReadError(message, e);
            
        } catch (Error e) {
            if (message == null) {
                throw e;
            }
            throw new MessageReadError(message, e);
        }
    }
    
    /**
     * 次に読み込むデータが存在するかどうかを返却する。
     * <p/>
     * この実装では、リーダが開いているかどうかで次のデータを読めるかどうか判定する。
     *
     * @return 次に読み込むデータが存在する場合は {@code true}
     */
    public boolean hasNext(ExecutionContext ctx) {
        return !closed;
    }

    /**
     * このリーダのクローズフラグを立て新規電文の受信を停止する。
     * <p/>
     * 受信イベント待ちで待機中のスレッドについてはそのまま放置する。<br/>
     * それらのスレッドは、新規電文を受信するかタイムアウトした時点で待機が解除される。
     */
    public void close(ExecutionContext ctx) {
        this.closed = true;
    }
    
    // -------------------------------------------------------- Internal APIs
    /**
     * このスレッドで使用中のフォーマッターを取得する。
     * @return フォーマッター
     */
    private DataRecordFormatter getFormatter() {
        File formatFile = FilePathSetting
                         .getInstance()
                         .getFileWithoutCreate(formatFileDirName, formatFileName);
        DataRecordFormatter formatter = FormatterFactory.getInstance()
                                                        .createFormatter(formatFile);
        return formatter;
    }
    
    // ---------------------------------------------------------- accessors
    /**
     * このリーダが監視する受信キューの論理名を設定する。
     * @param queueName 受信キューの論理名
     * @return このオブジェクト自体
     */
    public MessageReader setReceiveQueueName(String queueName) {
        this.receiveQueueName = queueName;
        return this;
    }
    
    /**
     * 受信キューが空の場合に待機する最大時間を設定する。
     * <p/>
     * 0以下の値を設定した場合はタイムアウトせずに
     * 新規電文を受信するまで待機し続ける。
     * 
     * @param timeout 受信タイムアウト(単位：ミリ秒) 
     * @return このオブジェクト自体
     */
    public MessageReader setReadTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }
    
    /**
     * 受信電文のフォーマット定義ファイル名を設定する。
     * @param fileName フォーマット定義ファイル名
     * @return このオブジェクト自体
     */
    public MessageReader setFormatFileName(String fileName) {
        formatFileName = fileName;
        return this;
    }
    
    /**
     * 受信電文のフォーマット定義ファイルが配置されているディレクトリの
     * 論理名を指定する。
     * @param dirName フォーマット定義ファイル配置ディレクトリの論理名
     * @return このオブジェクト自体
     */
    public MessageReader setFormatFileDirName(String dirName) {
        formatFileDirName = dirName;
        return this;
    }
}
