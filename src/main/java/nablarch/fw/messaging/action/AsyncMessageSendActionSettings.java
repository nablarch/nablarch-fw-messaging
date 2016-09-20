package nablarch.fw.messaging.action;

import java.util.Collections;
import java.util.List;

import nablarch.core.transaction.TransactionContext;

/**
 * {@link AsyncMessageSendAction}用設定クラス。
 *
 * @author hisaaki sioiri
 */
public class AsyncMessageSendActionSettings {

    /** SQLファイル配置パッケージ */
    private String sqlFilePackage;

    /** 送信キュー名称 */
    private String queueName;

    /** ヘッダフォーマット名 */
    private String headerFormatName;

    /** フォーマット定義ファイルの格納ディレクトリ(論理名) */
    private String formatDir = "format";

    /** トランザクション名 */
    private String transactionName = TransactionContext.DEFAULT_TRANSACTION_CONTEXT_KEY;

    /** Formクラス名 */
    private String formClassName;

    /** ヘッダに格納する項目名のリスト */
    private List<String> headerItemList = Collections.emptyList();

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
     * 送信キュー名称を設定する。
     *
     * @return 送信キュー名称
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * 送信キュー名称を取得する。
     *
     * @param queueName 送信キュー名称
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * ヘッダフォーマット名を取得する。
     *
     * @return ヘッダフォーマット名
     */
    public String getHeaderFormatName() {
        return headerFormatName;
    }

    /**
     * ヘッダフォーマット名を設定する。
     *
     * @param headerFormatName ヘッダフォーマット名
     */
    public void setHeaderFormatName(String headerFormatName) {
        this.headerFormatName = headerFormatName;
    }

    /**
     * フォーマット定義ファイルの格納ディレクトリ(論理名)を取得する。
     *
     * @return フォーマット定義ファイルの格納ディレクトリ(論理名)
     */
    public String getFormatDir() {
        return formatDir;
    }

    /**
     * フォーマット定義ファイルの格納ディレクトリ(論理名)を設定する。
     *
     * @param formatDir フォーマット定義ファイルの格納ディレクトリ(論理名)
     */
    public void setFormatDir(String formatDir) {
        this.formatDir = formatDir;
    }

    /**
     * トランザクション名を設定する。
     *
     * @param transactionName トランザクション名
     */
    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    /**
     * トランザクション名を取得する。
     *
     * @return トランザクション名
     */
    public String getTransactionName() {
        return transactionName;
    }


    /**
     * フォームクラス名を取得する。
     *
     * @return フォームクラス名
     */
    public String getFormClassName() {
        return formClassName;
    }

    /**
     * フォームクラス名を設定する。
     *
     * @param formClassName フォームクラス名
     */
    public void setFormClassName(String formClassName) {
        this.formClassName = formClassName;
    }

    /**
     * ヘッダに設定する項目のリストを設定する。
     *
     * @param headerItemList ヘッダに設定する項目のリスト
     */
    public void setHeaderItemList(List<String> headerItemList) {
        this.headerItemList = headerItemList;
    }

    /**
     * ヘッダに設定する項目のリストを取得する。
     *
     * @return ヘッダに設定する項目のリスト
     */
    public List<String> getHeaderItemList() {
        return headerItemList;
    }
}

