package nablarch.core.dataformat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FilePathSetting;


/**
 * 各種データとMapの相互変換を行うユーティリティクラス。
 *
 * @author TIS
 */
public final class SimpleDataConvertUtil {

    /** ロガー * */
    private static final Logger LOGGER = LoggerManager.get(SimpleDataConvertUtil.class);

    /**
     * 隠蔽コンストラクタ
     */
    private SimpleDataConvertUtil() {
    }

    /**
     * Mapから構造化データの文字列を生成する。
     * 変換後の構造化データ形式はフォーマット定義ファイルにて指定される。
     *
     * @param formatName フォーマット定義ファイル
     * @param data 変換対象データ
     * @return 変換結果
     * @throws InvalidDataFormatException 入力データが不正な場合。
     */
    public static SimpleDataConvertResult buildData(String formatName, Map<String, ?> data) throws InvalidDataFormatException {
        // フォーマッタ取得
        DataRecordFormatter formatter = getFormatter(formatName);

        // データを生成し返却
        Charset charset = getCharset(formatter);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            return buildData(formatName, data, baos)
                    .setResultText(new String(baos.toByteArray(), charset));
        } catch (IOException wontHappen) {
            // データフォーマッターに渡す出力ストリームとして、ヒープ上のバイト配列を渡しているので
            // レコードの書き込み際してI/Oエラーは発生しえない。
            // (このことはJDKのJavadocに仕様として記載されている。)
            throw new RuntimeException(wontHappen);
        }
    }

    /**
     * Mapから構造化データの文字列を生成し、出力ストリームに書き込む。
     * 変換後の構造化データ形式はフォーマット定義ファイルにて指定される。
     *
     * @param formatName フォーマット定義ファイル
     * @param data 変換対象データ
     * @param out 出力先ストリーム
     * @return 変換結果
     * @throws InvalidDataFormatException 入力データが不正な場合。
     * @throws IOException 書き込みに伴うIO処理で問題が発生した場合。
     */
    public static SimpleDataConvertResult buildData(String formatName, Map<String, ?> data, OutputStream out) throws InvalidDataFormatException, IOException {
        // フォーマッタ取得
        DataRecordFormatter formatter = getFormatter(formatName);

        // データを生成
        formatter.setOutputStream(out);
        formatter.initialize();
        formatter.writeRecord(data);
        formatter.close();

        return createResult(formatter);
    }

    /**
     * 構造化データの文字列からMapを生成する。
     * 変換前の構造化データ形式はフォーマット定義ファイルにて指定される。
     *
     * @param formatName フォーマット定義ファイル
     * @param data 変換対象データ
     * @return 変換結果
     * @throws InvalidDataFormatException 入力データが不正な場合。
     */
    public static SimpleDataConvertResult parseData(String formatName, String data) throws InvalidDataFormatException {
        // フォーマッタ取得
        DataRecordFormatter formatter = getFormatter(formatName);

        // データを解析し、返却
        Charset charset = getCharset(formatter);
        try {
            return parseData(formatName,
                    new ByteArrayInputStream(data.getBytes(charset)));
        } catch (IOException wontHappen) {
            // データフォーマッターに渡す入力ストリームとして、ヒープ上のバイト列を使用して
            // レコードの読み取りに際してI/Oエラーは発生しえない。
            // (このことはJDKのJavadocに仕様として記載されている。)
            throw new RuntimeException(wontHappen);
        }
    }

    /**
     * 構造化データのストリームからMapを生成する。
     * 変換前の構造化データ形式はフォーマット定義ファイルにて指定される。
     *
     * @param formatName フォーマット定義ファイル
     * @param in 変換対象データ読み込み用ストリーム
     * @return 変換結果
     * @throws InvalidDataFormatException 入力データが不正な場合。
     * @throws IOException 読み込みに伴うIO処理で問題が発生した場合。
     */
    public static SimpleDataConvertResult parseData(String formatName, InputStream in) throws InvalidDataFormatException, IOException {
        // フォーマッタ取得
        DataRecordFormatter formatter = getFormatter(formatName);

        // データを解析
        formatter.setInputStream(in);
        formatter.initialize();
        Map<String, ?> resultMap = formatter.readRecord();
        formatter.close();

        return createResult(formatter)
               .setResultMap(resultMap);
    }

    /**
     * フォーマット名に対応したフォーマッタを取得する。
     *
     * @param formatName フォーマット名
     * @return フォーマッタ
     */
    private static DataRecordFormatter getFormatter(String formatName) {
        // フォーマットファイルを論理パスから取得
        File formatFile = FilePathSetting
                .getInstance()
                .getFileWithoutCreate("format", formatName);

        // フォーマッタを生成・初期化
        DataRecordFormatter formatter = FormatterFactory
                .getInstance()
                .createFormatter(formatFile);

        formatter.initialize();

        return formatter;
    }

    /**
     * 変換結果オブジェクトを生成する。
     *
     * @param formatter フォーマッタ
     * @return 変換結果
     */
    private static SimpleDataConvertResult createResult(DataRecordFormatter formatter) {
        SimpleDataConvertResult result = new SimpleDataConvertResult();

        // フォーマッタの各種設定値を取得
        if (formatter instanceof DataRecordFormatterSupport) {
            DataRecordFormatterSupport drfs = ((DataRecordFormatterSupport) formatter);
            result.setCharset(drfs.getDefaultEncoding());
            result.setDataType(drfs.getFileType());
            result.setMimeType(drfs.getMimeType());
        }

        return result;
    }

    /**
     * フォーマッタに定義されている文字セットを取得する。
     * 取得できない場合はプラットフォームのデフォルト文字セットを取得する。
     * @param formatter フォーマッタ
     * @return 文字セット
     */
    private static Charset getCharset(DataRecordFormatter formatter) {
        Charset charset = Charset.defaultCharset();
        if (formatter instanceof DataRecordFormatterSupport) {
            charset = ((DataRecordFormatterSupport) formatter).getDefaultEncoding();
        }
        return charset;
    }

}
