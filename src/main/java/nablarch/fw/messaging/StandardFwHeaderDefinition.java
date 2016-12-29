package nablarch.fw.messaging;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Nablarch標準のフレームワーク制御ヘッダ定義。
 * 
 * 本実装では、各電文のメッセージボディの先頭レコード上に
 * 全てのフレームワーク制御ヘッダが定義されていることを前提としており、
 * JMSヘッダー等のメッセージングプロバイダ実装に依存する項目は使用しない。
 * <p/>
 * 以下は本クラスで使用できるフレームワーク制御ヘッダの定義例である。
 * このデータレコードが電文の先頭に位置している必要がある。
 * <pre>
 * #====================================================================
 * # フレームワーク制御ヘッダ部 (50byte)
 * #====================================================================
 * [NablarchHeader]
 * 1   requestId   X(10)       # リクエストID
 * 11  userId      X(10)       # ユーザID
 * 21  resendFlag  X(1)  "0"   # 再送要求フラグ (0: 初回送信 1: 再送要求)
 * 22  statusCode  X(4)  "200" # ステータスコード
 * 26 ?filler      X(25)       # 予備領域
 * #====================================================================
 * </pre>
 * フォーマット定義にフレームワーク制御ヘッダ以外の項目を含めた場合、
 * {@link FwHeader}クラスの任意属性としてアクセスすることができる。
 * これらの属性は、PJ毎にフレームワーク制御ヘッダを簡易的に拡張する場合に
 * 利用することができる。
 * <p/>
 * なお、将来的な任意項目の追加およびフレームワークの機能追加に伴うヘッダ追加
 * に対応するために、予備領域を設けておくことを強く推奨する。
 * 
 * @author Iwauo Tajima
 */
public class StandardFwHeaderDefinition implements FwHeaderDefinition {
    // ------------------------------------------ Structure
    /** フォーマット定義ファイル配置ディレクトリ論理名 */
    private String formatFileDir = "format";
    
    /** フォーマット定義ファイル名 */
    private String formatFileName = "header";
    
    /** 初回電文を表す再送要求フラグの値 */
    private Object resendFlagOffValue = 0;
    
    /**{@inheritDoc}
     * この実装ではメッセージボディの先頭レコードを取得し、
     * フレームワーク制御ヘッダの各項目を読み込む。
     */
    public RequestMessage
    readFwHeaderFrom(ReceivedMessage message) {
        FwHeader header = new FwHeader();
        DataRecordFormatter bodyFormatter = message.getFormatter();
        message.setFormatter(getFormatter());
        DataRecord headerRecord = message.readRecord();
        if (headerRecord == null) {
            throw new MessagingException("there was no FW Header records.");
        }
        header.putAll(headerRecord);
        header.setResendFlagOffValue(resendFlagOffValue);
        message.getRecords().remove(headerRecord);
        message.setFormatter(bodyFormatter);
        
        return new RequestMessage(header, message);
    }
    
    /** {@inheritDoc}
     * この実装では、メッセージボディ部のバイト列の先頭にフレームワーク制御ヘッダ
     * のバイト列を連結する。
     */
    public void writeFwHeaderTo(SendingMessage message, FwHeader header) {
        ByteArrayOutputStream bodyStream = message.getBodyStream();
        byte[] bodyBytes = bodyStream.toByteArray();
        bodyStream.reset();
        try {
            getFormatter().setOutputStream(bodyStream)
                     .initialize()
                     .writeRecord(header);
            bodyStream.write(bodyBytes);
        } catch (IOException e) {
            throw new RuntimeException(e); // can not happen;
        }
    }
    
    // ----------------------------------------- Accessors
    /**
     * フレームワーク制御ヘッダーのフォーマット定義ファイルが配置されている
     * ディレクトリの論理名を設定する。
     * 設定を省略した場合のデフォルト値は"format"である。
     * @param dirName フォーマット定義ファイル配置ディレクトリの論理名
     * @return このオブジェクト自体
     */
    public StandardFwHeaderDefinition setFormatFileDir(String dirName) {
        formatFileDir = dirName;
        return this;
    }
    
    /**
     * フレームワーク制御ヘッダーのフォーマット定義ファイルのファイル名
     * を設定する。
     * 設定を省略した場合のデフォルト値は"header.fmt"となる。
     * @param fileName フォーマット定義ファイル名
     * @return このオブジェクト自体
     */
    public StandardFwHeaderDefinition setFormatFileName(String fileName) {
        formatFileName = fileName;
        return this;
    }

    /**
     * フレームワーク制御ヘッダーのフォーマット定義を返す。
     * @return フレームワーク制御ヘッダーのフォーマット定義
     */
    public synchronized DataRecordFormatter getFormatter() {
        return getFormatter(FilePathSetting.getInstance(), FormatterFactory.getInstance());
    }

    /**
     * 指定された{@link FilePathSetting}インスタンスを使用して
     * フレームワーク制御ヘッダーのフォーマット定義を返す。
     *
     * @param filePathSetting フォーマット定義ファイルを取得するための{@link FilePathSetting}
     * @param formatterFactory フォーマット定義を生成するファクトリ
     * @return フレームワーク制御ヘッダーのフォーマット定義
     */
    public synchronized DataRecordFormatter getFormatter(FilePathSetting filePathSetting,
                                                         FormatterFactory formatterFactory) {
        File formatFile = filePathSetting.getFileWithoutCreate(formatFileDir, formatFileName);
        DataRecordFormatter formatter = formatterFactory.createFormatter(formatFile);
        return formatter;
    }

    /**
     * 初回電文時に設定される再送要求フラグの値を設定する。
     * 
     * @param value 初回電文時に設定される再送要求フラグの値
     * @return このオブジェクト自体
     */
    public StandardFwHeaderDefinition setResendFlagOffValue(Object value) {
        resendFlagOffValue = value;
        return this;
    }
    
    /**
     * 初回電文時に設定される再送要求フラグの値を返す。
     * 
     * @return 初回電文時に設定される再送要求フラグの値
     */
    public Object getResendFlagOffValue() {
        return resendFlagOffValue;
    }
}
