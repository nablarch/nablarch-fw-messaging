package nablarch.fw.messaging.reader;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import nablarch.core.ThreadContext;
import nablarch.core.dataformat.DataRecord;
import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.FilePathSetting;
import nablarch.fw.messaging.StructuredRequestMessage;
import nablarch.fw.messaging.FwHeader;
import nablarch.fw.messaging.FwHeaderDefinition;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.ReceivedMessage;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.SendingMessage;

/**
 * 構造化データのフレームワーク制御ヘッダの解析を行うデータリーダ。
 * <p/>
 * このデータリーダ実装は、MessageReaderが読み込んだ受信電文のメッセージボディから 
 * フレームワーク制御ヘッダ部分を読み込み、後続のハンドラからそれらの値を参照可能とする。
 * このリーダの戻り値の型であるRequestMessageは、フレームワーク制御ヘッダに
 * 対するアクセサを保持し、{@link nablarch.fw.Request}インターフェースを実装する。
 * <p/>
 * 
 * @author TIS
 */
public class StructuredFwHeaderDefinition implements FwHeaderDefinition {
    
    /** 初回電文を表す再送要求フラグの値 */
    private String resendFlagOffValue = "0";
    
    /** ヘッダフォーマット定義ファイル配置ディレクトリ論理名 */
    private String formatFileDir = "format";
    
    /** ヘッダフォーマット定義ファイル名パターン  */
    private String headerFormatFileNamePattern = "%s" + "_RECEIVE";
    
    /** フレームワーク制御ヘッダキー名リスト */
    private Map<String, String> fwHeaderKeys = null;
    
    /**
     * {@inheritDoc}
     */
    public RequestMessage readFwHeaderFrom(ReceivedMessage message) {
        FwHeader fwHeader = new FwHeader();
        String requestId = ThreadContext.getRequestId();
        
        fwHeader.setRequestId(requestId);
        fwHeader.setResendFlagOffValue(getResendFlagOffValue());
        message.setFormatter(getFormatter(requestId));
        
        DataRecord headerRecord = message.readRecord();
        if (headerRecord == null) {
            throw new MessagingException("there was no FW Header records.");
        }

        if (fwHeaderKeys != null) {
            for (Entry<String, String> e : fwHeaderKeys.entrySet()) {
                // キー取得(key="FW制御ヘッダ上のキー" value="電文上のキー"で定義されているため、ここではvalueをキーとする)
                String key = e.getValue();
                if (headerRecord.containsKey(key)) {
                    fwHeader.put(e.getKey(), headerRecord.get(key));
                }
            }
        }
        //StructuredRequestMessage生成にあたり、
        //ReceivedMessage#bodyStreamは読み取られていない状態である必要があるため、ReceivedMessageの複製を生成する。
        ReceivedMessage receivedMessage = new ReceivedMessage(message.getBodyBytes());
        receivedMessage.setHeaderMap(message.getHeaderMap());
        receivedMessage.setFormatter(message.getFormatter());
        receivedMessage.getRecords().addAll(message.getRecords());
        
        return new StructuredRequestMessage(fwHeader, receivedMessage);
    }
    
    /**
     * フレームワーク制御ヘッダーのフォーマット定義を返す。
     * @param dataType データ種別
     * @return フレームワーク制御ヘッダーのフォーマット定義
     */
    public synchronized DataRecordFormatter getFormatter(String dataType) {
        return getFormatter(dataType, FilePathSetting.getInstance(), FormatterFactory.getInstance());
    }

    /**
     * 指定された{@link FilePathSetting}インスタンスを使用して
     * フレームワーク制御ヘッダーのフォーマット定義を返す。
     *
     * @param dataType データ種別
     * @param filePathSetting フォーマット定義ファイルを取得するための{@link FilePathSetting}
     * @param formatterFactory フォーマット定義を生成するファクトリ
     * @return フレームワーク制御ヘッダーのフォーマット定義
     */
    public synchronized DataRecordFormatter getFormatter(String dataType, FilePathSetting filePathSetting,
                                                         FormatterFactory formatterFactory) {
        
        String formatFileName = String.format(
                headerFormatFileNamePattern, dataType
        );
        
        File formatFile = filePathSetting.getFileWithoutCreate(formatFileDir, formatFileName);
        DataRecordFormatter formatter = formatterFactory.createFormatter(formatFile);
        return formatter;
    }

    /**
     * {@inheritDoc}
     */
    public void writeFwHeaderTo(SendingMessage message, FwHeader header) {

        if (fwHeaderKeys != null) {
            Map<String, Object> rec = message.getParamMap();
            if (rec != null) {
                for (Entry<String, String> e : fwHeaderKeys.entrySet()) {
                    // キー取得(key="FW制御ヘッダ上のキー" value="電文上のキー"で定義されているため、ここではkeyをキーとする)
                    String key = e.getKey();
                    if (header.containsKey(key)) {
                        rec.put(e.getValue(), header.get(key));
                    }
                }
            }
        }
    }
    
    /**
     * 初回電文時に設定される再送要求フラグの値を設定する。
     * 
     * @param value 初回電文時に設定される再送要求フラグの値
     * @return このオブジェクト自体
     */
    public StructuredFwHeaderDefinition setResendFlagOffValue(String value) {
        resendFlagOffValue = value;
        return this;
    }
    
    /**
     * 初回電文時に設定される再送要求フラグの値を返す。
     * 
     * @return 初回電文時に設定される再送要求フラグの値
     */
    public String getResendFlagOffValue() {
        return resendFlagOffValue;
    }

    /**
     * フレームワーク制御ヘッダキー名リストを設定する
     * @param fwHeaderKeys フレームワーク制御ヘッダキー名リスト
     * @return このオブジェクト自体
     */
    public StructuredFwHeaderDefinition setFwHeaderKeys(Map<String, String> fwHeaderKeys) {
        this.fwHeaderKeys = fwHeaderKeys;
        return this;
    }
}
