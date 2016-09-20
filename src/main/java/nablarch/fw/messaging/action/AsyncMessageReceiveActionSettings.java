package nablarch.fw.messaging.action;

import nablarch.common.idgenerator.IdFormatter;
import nablarch.common.idgenerator.IdGenerator;
import nablarch.core.transaction.TransactionContext;

/**
 * {@link AsyncMessageReceiveAction}用設定クラス。
 *
 * @author hisaaki sioiri
 */
public class AsyncMessageReceiveActionSettings {

    /** フォームクラス配置パッケージ */
    private String formClassPackage;

    /** 受信電文連番を採番するオブジェクト */
    private IdGenerator receivedSequenceGenerator;

    /** 受信電文連番を採番する際に使用するフォーマッタ */
    private IdFormatter receivedSequenceFormatter;

    /** 受信電文連番を採番するためのID */
    private String targetGenerateId;

    /** 受信テーブル登録用のトランザクション名 */
    private String dbTransactionName = TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY;

    /** SQLファイル配置パッケージ */
    private String sqlFilePackage;

    /** Formクラスのサフィックス */
    private String formClassSuffix = "Form";

    /**
     * Formクラスの配置パッケージを取得する。
     *
     * @return Formクラスの配置パッケージ
     */
    public String getFormClassPackage() {
        return formClassPackage;
    }

    /**
     * Formクラスの配置パッケージを設定する。
     *
     * @param formClassPackage Formクラスの配置パッケージ
     */
    public void setFormClassPackage(String formClassPackage) {
        this.formClassPackage = formClassPackage;
    }

    /**
     * 受信電文連番を採番するための{@link IdGenerator}を取得する。
     *
     * @return {@link IdGenerator}
     */
    public IdGenerator getReceivedSequenceGenerator() {
        return receivedSequenceGenerator;
    }

    /**
     * 受信電文連番を採番するための{@link IdGenerator}を設定する。
     *
     * @param receivedSequenceGenerator {@link IdGenerator}
     */
    public void setReceivedSequenceGenerator(IdGenerator receivedSequenceGenerator) {
        this.receivedSequenceGenerator = receivedSequenceGenerator;
    }

    /**
     * 受信電文連番を採番する際に使用するフォーマッタを取得する。
     * @return {@link IdFormatter}
     */
    public IdFormatter getReceivedSequenceFormatter() {
        return receivedSequenceFormatter;
    }

    /**
     * 受信電文連番を採番する際に使用するフォーマッタを設定する。
     * @param receivedSequenceFormatter {@link IdFormatter}
     */
    public void setReceivedSequenceFormatter(
            IdFormatter receivedSequenceFormatter) {
        this.receivedSequenceFormatter = receivedSequenceFormatter;
    }

    /**
     * 受信電文連番を採番するためのIDを取得する。
     *
     * @return 受信電文連番を採番するためのID
     */
    public String getTargetGenerateId() {
        return targetGenerateId;
    }

    /**
     * 受信電文連番を採番するためのIDを設定する。
     * <p/>
     * 設定されたIDは、{@link IdGenerator#generateId(String)}の引数として使用する。
     *
     * @param targetGenerateId 受信電文連番を採番するためのID
     */
    public void setTargetGenerateId(String targetGenerateId) {
        this.targetGenerateId = targetGenerateId;
    }

    /**
     * DBトランザクション名を取得する。
     *
     * @return DBトランザクション名
     */
    public String getDbTransactionName() {
        return dbTransactionName;
    }

    /**
     * DBトランザクション名を設定する。
     * <p/>
     * 本設定を省略した場合、DBトランザクション名は{@link TransactionContext#DEFAULT_TRANSACTION_CONTEXT_KEY}となる。
     *
     * @param dbTransactionName DBトランザクション名
     */
    public void setDbTransactionName(String dbTransactionName) {
        this.dbTransactionName = dbTransactionName;
    }

    /**
     * SQLファイルの配置パッケージを取得する。
     *
     * @return SQLファイルの配置パッケージ
     */
    public String getSqlFilePackage() {
        return sqlFilePackage;
    }

    /**
     * SQLファイルの配置パッケージを設定する。
     *
     * @param sqlFilePackage SQLファイルの配置パッケージ
     */
    public void setSqlFilePackage(String sqlFilePackage) {
        this.sqlFilePackage = sqlFilePackage;
    }

    /**
     * Formクラスのサフィックスを取得する。
     *
     * @return Formクラスのサフィックス
     */
    public String getFormClassSuffix() {
        return formClassSuffix;
    }

    /**
     * Formクラスのサフィックスを設定する。
     * <p/>
     * メッセージをテーブルに登録する際に使用するFormクラスのクラス名のサフィックスとして使用する。
     * 本設定を省略した場合、デフォルト値で「Form」が使用される。
     *
     * @param formClassSuffix Formクラスのサフィックス。
     */
    public void setFormClassSuffix(String formClassSuffix) {
        this.formClassSuffix = formClassSuffix;
    }
}

