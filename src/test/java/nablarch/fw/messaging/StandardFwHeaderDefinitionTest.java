package nablarch.fw.messaging;

import nablarch.core.dataformat.DataRecordFormatter;
import nablarch.core.dataformat.FormatterFactory;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.FilePathSetting;
import nablarch.test.support.tool.Hereis;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link StandardFwHeaderDefinition}のテストクラス。
 *
 * @author hisaaki sioiri
 */
public class StandardFwHeaderDefinitionTest {

    /** リポジトリを初期化する。 */
    @Before
    @After
    public void cleaningRepository() {
        SystemRepository.clear();
    }

    /**
     * {@link StandardFwHeaderDefinition#getFormatter(nablarch.core.util.FilePathSetting, nablarch.core.dataformat.FormatterFactory)}のテスト
     *
     * {@link nablarch.core.dataformat.DataRecordFormatter}が生成されること
     */
    @Test
    public void getFormatterNormalEnd() {

        // ファイルパス設定
        FilePathSetting setting = new FilePathSetting();
        setting.addBasePathSetting("format", "file:./tmp/format");
        setting.addFileExtensions("format", "format");

        // 正常系のフォーマット定義ファイル
        Hereis.file("./tmp/format/test-format.format");
        /*
        #
        # ディレクティブ定義部
        #
        file-type:     "Fixed"  # 固定長ファイル
        text-encoding: "ms932"  # 文字列型フィールドの文字エンコーディング
        record-length:  1       # 各レコードbyte長

        #
        # データレコード定義部
        #
        [Default]
        1    dataKbn       X(1)  "2"      # 1. データ区分
        */

        StandardFwHeaderDefinition definition = new StandardFwHeaderDefinition();
        definition.setFormatFileDir("format");
        definition.setFormatFileName("test-format");
        DataRecordFormatter formatter = definition.getFormatter(setting, new FormatterFactory());
        assertThat(formatter, is(notNullValue()));
    }

    /**
     * {@link StandardFwHeaderDefinition#getFormatter(nablarch.core.util.FilePathSetting, nablarch.core.dataformat.FormatterFactory)}のテスト
     *
     * 存在しないフォーマットファイルを指定した場合、異常終了すること。
     */
    @Test
    public void getFormatterFormatNotFound() {

        // ファイルパス設定
        FilePathSetting setting = new FilePathSetting();
        setting.addBasePathSetting("format", "file:./tmp/format");
        setting.addFileExtensions("format", "format");

        StandardFwHeaderDefinition definition = new StandardFwHeaderDefinition();
        definition.setFormatFileDir("format");
        definition.setFormatFileName("notFound");
        try {
            definition.getFormatter(setting, new FormatterFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(allOf(
                    containsString("invalid layout file path was specified."),
                    containsString("file path=["),
                    containsString("notFound.format")
            )));

        }
    }
}
