package nablarch.fw.messaging.reader;

import java.io.File;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.core.util.StringUtil;
import nablarch.fw.DataReader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.MessageReadError;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.StandardFwHeaderDefinition;

/**
 * 受信電文のフレームワーク制御ヘッダの解析を行うデータリーダ。
 * <p/>
 * {@link MessageReader}が読み込んだ受信電文のメッセージボディから
 * フレームワーク制御ヘッダ部分を読み込み、後続のハンドラからそれらの値を参照可能とする。<br/>
 * このデータリーダの戻り値の型である{@link RequestMessage}は、フレームワーク制御ヘッダに
 * 対するアクセサを保持し、{@link nablarch.fw.Request}インターフェースを実装する。
 * <p/>
 * 受信電文読み込み時にエラーが発生した場合は、必ず例外({@link MessageReadError})を送出する。
 * この場合、業務処理へのディスパッチは発生せず、フレームワークが直接エラー応答を行うことになる。
 * 
 * @author Iwauo Tajima
 */
public class FwHeaderReader implements DataReader<RequestMessage> {
    // ------------------------------------------------------- Structure    
    /** 受信電文のリーダ */
    private DataReader<ReceivedMessage> messageReader = null;
    
    /** フレームワーク制御ヘッダー定義 */
    private FwHeaderDefinition fwHeaderDef = new StandardFwHeaderDefinition();
       
    /* 業務データフォーマット定義ファイル関連設定 */
    /** 業務データフォーマット定義ファイル配置ディレクトリ論理名 */
    private String formatFileDir = "format";
    
    /** 業務データフォーマット定義ファイル名パターン  */
    private String messageFormatFileNamePattern = "%s" + "_RECEIVE";
    
    /** 応答電文のフォーマット定義ファイル名のパターン */
    private String replyMessageFormatFileNamePattern = "%s" + "_SEND";
    
    // ------------------------------------------------------ DataReader I/F
    /**
     * 受信電文のフレームワーク制御ヘッダ部分を読み込む。
     * <p/>
     * {@link MessageReader}で取得した受信電文オブジェクトの
     * フレームワーク制御項目を読み込み、下記項目をスレッドコンテキストに設定する。
     * <ul>
     *     <li>リクエストID</li>
     *     <li>内部リクエストID</li>
     *     <li>ユーザID(フレームワーク制御項目に設定されている場合のみ)</li>
     * </ul>
     * また、受信電文の業務データ部の読み込みと応答電文の業務データ部生成に使用する
     * フォーマッタを決定し、要求電文オブジェクトに設定する。
     *
     * @param ctx 実行コンテキスト
     * @return 要求電文オブジェクト（受信電文オブジェクトが{@code null}の場合は{@code null}を返す）
     * @throws MessageReadError フレームワーク制御ヘッダのパースに失敗した場合
     */
    public RequestMessage read(ExecutionContext ctx) {
         ReceivedMessage message = messageReader.read(ctx);
            
        if (message == null) {
            return null;
        }
        
        RequestMessage req = null;
        try {
            req = fwHeaderDef.readFwHeaderFrom(message);
       
            FwHeader header = req.getFwHeader();
            ThreadContext.setRequestId(header.getRequestId());
            ThreadContext.setInternalRequestId(header.getRequestId());
            if (header.hasUserId()) {
                ThreadContext.setUserId(header.getUserId());
            }
            prepareMessageFormatter(req);
            prepareMessageFormatterOfReply(req);
            return req;
            
        } catch (RuntimeException e) {
            throw new MessageReadError(message, e);
            
        } catch (Error e) {
            throw new MessageReadError(message, e);
        }
    }
    
    /**
     * 次に読み込むデータが存在するかどうかを返却する。
     * @param ctx 実行コンテキスト
     * @return 次に読み込むデータが存在する場合は{@code true}
     */
    public boolean hasNext(ExecutionContext ctx) {
        return messageReader.hasNext(ctx);
    }
    
    /**
     * このリーダの利用を停止し、内部的に保持している各種リソースを解放する。
     * @param ctx
     */
    public void close(ExecutionContext ctx) {
        messageReader.close(ctx);
    }

    // ----------------------------------------------------------- helper
    /**
     * 受信電文の業務データ部を読み込む際に使用するフォーマッターを設定する。
     * @param req 要求電文オブジェクト
     */
    private void prepareMessageFormatter(RequestMessage req) {
        String requestId = req.getRequestPath();
        String fileName = String.format(
            messageFormatFileNamePattern, requestId
        );
        DataRecordFormatter formatter = formatterAt(formatFileDir, fileName);
        if (formatter != null) {
            req.setFormatter(formatter);
        }
    }
    
    /**
     * 応答電文の業務データ部を作成する際に使用するフォーマッターを設定する。
     * @param req 要求電文オブジェクト
     */
    private void prepareMessageFormatterOfReply(RequestMessage req) {
        String requestId = req.getRequestPath();
        String fileName = String.format(
            replyMessageFormatFileNamePattern, requestId
        );
        DataRecordFormatter formatter = formatterAt(formatFileDir, fileName);
        if (formatter != null) {
            req.setFormatterOfReply(formatter);
        }
    }

    /**
     * 指定されたパス上のフォーマット定義ファイルを元に
     * レコードフォーマッタを構成して返す。
     * <p/>
     * 指定されたパス上にファイルが存在しなかった場合は{@code null}を返す。
     * 
     * @param dirName  フォーマット定義ファイルの配置先ディレクトリ(論理名)
     * @param fileName フォーマット定義ファイルのファイル名
     * @return レコードフォーマッター
     */
    private DataRecordFormatter formatterAt(String dirName, String fileName) {
        File file = FilePathSetting.getInstance()
                                   .getFileIfExists(dirName, fileName);
        if (file == null) {
            return null;
        }
        return FormatterFactory.getInstance().createFormatter(file);
    }
    

    // ------------------------------------------------------ Accessors
    /**
     * 受信電文を読み込むリーダを設定する。
     * @param messageReader データリーダ
     * @return このオブジェクト自体
     * @throws IllegalArgumentException データリーダが{@code null}の場合
     */
    public FwHeaderReader
    setMessageReader(DataReader<ReceivedMessage> messageReader) {
        if (messageReader == null) {
            throw new IllegalArgumentException("messageReader must not be null.");
        }
        this.messageReader = messageReader;
        return this;
    }
    
    /**
     * フレームワーク制御ヘッダ定義を設定する。
     * 
     * @param def フレームワーク制御ヘッダ設定
     * @return このオブジェクト自体
     * @throws IllegalArgumentException フレームワーク制御ヘッダ設定が{@code null}の場合
     */
    public FwHeaderReader setFwHeaderDefinition(FwHeaderDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException(
               "fwHeaderDefinition must not be null."
            );
        }
        fwHeaderDef = def;
        return this;
    }
    
    /**
     * 業務データ部のフォーマット定義ファイルの配置先ディレクトリ論理名を設定する。
     * <p/>
     * デフォルト値は"format"。
     * @param dirName フォーマット定義ファイルの配置先ディレクトリ論理名
     * @return このオブジェクト自体
     * @throws IllegalArgumentException 配置先ディレクトリ論理名が無効な場合
     */
    public FwHeaderReader setFormatFileDir(String dirName) {
        if (StringUtil.isNullOrEmpty(dirName)) {
            throw new IllegalArgumentException(
                "'formatFileDir' must not be blank."
            );
        }
        formatFileDir = dirName;
        return this;
    }
    
    /**
     * 受信電文のフォーマット定義ファイル名のパターン文字列を設定する。
     * <p/>
     * デフォルトの設定では、以下の名称のフォーマット定義ファイルを取得する。
     * <pre>
     * (リクエストID) + "_RECEIVE.fmt"
     * </pre>
     * @param pattern フォーマット定義ファイル名のパターン文字列
     * @return このオブジェクト自体
     * @throws IllegalArgumentException パターン文字列が無効な場合
     */
    public FwHeaderReader setMessageFormatFileNamePattern(String pattern) {
        if (StringUtil.isNullOrEmpty(pattern)) {
            throw new IllegalArgumentException(
                "'formatFileNamePattern' must not be blank."
            );
        }
        messageFormatFileNamePattern = pattern;
        return this;
    }
    
    /**
     * 応答電文のフォーマット定義ファイル名のパターン文字列を設定する。
     * <p/>
     * デフォルトの設定では、以下の名称のフォーマット定義ファイルを取得する。
     * <pre>
     * (リクエストID) + "_SEND.fmt"
     * </pre>
     * @param pattern フォーマット定義ファイル名のパターン文字列
     * @return このオブジェクト自体
     * @throws IllegalArgumentException パターン文字列が無効な場合
     */
    public FwHeaderReader setReplyMessageFormatFileNamePattern(String pattern) {
        if (StringUtil.isNullOrEmpty(pattern)) {
            throw new IllegalArgumentException(
                "'formatFileNamePattern' must not be blank."
            );
        }
        replyMessageFormatFileNamePattern = pattern;
        return this;
    }
}
