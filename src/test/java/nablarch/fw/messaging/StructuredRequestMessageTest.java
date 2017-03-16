package nablarch.fw.messaging;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.DataRecordFormatterSupport;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.util.Builder;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link StructuredRequestMessage}のテストクラス。
 *
 * @author TIS
 */
public class StructuredRequestMessageTest {

    /**
     * 応答メッセージ作成のテストを行います。<br>
     * 
     * 条件：<br>
     *   応答データ作成処理を呼び出す。<br>
     *   
     * 期待結果：<br>
     *   {@link StructuredResponseMessage}のインスタンスが取得できること。<br>
     */
    @Test
    public void testCreateResponseMessage() {
        ReceivedMessage receivedMessage = new ReceivedMessage(new byte[]{});
        FwHeader fwHeader = new FwHeader();
        StructuredRequestMessage requestMessage = new StructuredRequestMessage(fwHeader, receivedMessage);
        requestMessage.setDestination("destination");
        requestMessage.setReplyTo("replyTo");
        requestMessage.setMessageId("messageId");
        ResponseMessage responseMesssage = requestMessage.createResponseMessage();
        
        assertTrue(responseMesssage instanceof StructuredResponseMessage);
    }

    /**
     * フォーマッタ設定処理のテストを行います。<br>
     * 
     * 条件：<br>
     *   フォーマッタ設定処理を呼び出す。<br>
     *   
     * 期待結果：<br>
     *   初期化済みフォーマッタが設定されること。<br>
     */
    @Test
    public void testSetFormatterDataRecordFormatter() {
        // テスト用フォーマット
        FilePathSetting fps = FilePathSetting.getInstance()
                .addBasePathSetting("format", "file:temp")
                .addFileExtensions("format", "fmt");
        String formatFileName = 
                Builder.concat(
                    fps.getBasePathSettings().get("format").getPath(),
                    "/", "StructuredRequestMessageTest", ".", 
                    fps.getFileExtensions().get("format"));
        
        File requestFormatFile = Hereis.file(formatFileName);
        /****************************
        file-type:      "XML"
        text-encoding:  "UTF-8"
        [request]
        1 id            X
        2 name          X
        ****************************/
        requestFormatFile.deleteOnExit();

        DataRecordFormatter formatter = FormatterFactory.getInstance()
                .createFormatter(requestFormatFile);
        
        // 初期化処理が行われるまでデフォルトエンコーディングは設定されない
        assertNull(((DataRecordFormatterSupport)formatter).getDefaultEncoding());
        
        ReceivedMessage receivedMessage = new ReceivedMessage(new byte[]{});
        FwHeader fwHeader = new FwHeader();
        StructuredRequestMessage requestMessage = new StructuredRequestMessage(fwHeader, receivedMessage);
        requestMessage.setFormatter(formatter);
        
        // 初期化処理が行われるとデフォルトエンコーディングが設定される
        assertEquals(Charset.forName("UTF-8"), 
                ((DataRecordFormatterSupport)requestMessage.getFormatter()).getDefaultEncoding());
    }
    
    /**
     * フォーマッタ設定処理にnullを設定した場合のテストを行います。<br>
     * 
     * 条件：<br>
     *   フォーマッタ設定処理にnullを設定して呼び出す。<br>
     *   
     * 期待結果：<br>
     *   エラーが発生せずnullが設定されること。<br>
     */
    @Test
    public void testSetFormatterDataRecordFormatterNull() {
        ReceivedMessage receivedMessage = new ReceivedMessage(new byte[]{});
        FwHeader fwHeader = new FwHeader();
        StructuredRequestMessage requestMessage = new StructuredRequestMessage(fwHeader, receivedMessage);
        requestMessage.setFormatter(null);
        
        assertNull(requestMessage.getFormatter());
    }
}
