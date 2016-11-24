package nablarch.common.handler;

import nablarch.core.log.basic.LogWriterSupport;
import nablarch.test.support.db.helper.VariousDbTestHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 元例外をアサート出来るようにするためのLogWriter。
 */
public class DbConnectionManagementHandlerLog extends LogWriterSupport {
    private static List<String> log = new ArrayList<String>();

    @Override
    protected void onWrite(String formattedMessage) {
        log.add(formattedMessage);
    }

    private static void clear() {
        log.clear();
    }

    private static List<String> getLog() {
        return log;
    }

    /** テスト用のテーブルをセットアップ。 */
    private void setUpTable() {
        VariousDbTestHelper.createTable(TestTable.class);
        VariousDbTestHelper.setUpTable(new TestTable("00001"));
    }
}
