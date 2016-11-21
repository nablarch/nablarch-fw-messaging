package nablarch.fw.messaging.action;

import nablarch.core.db.statement.ParameterizedSqlPStatement;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.db.statement.SqlRow;
import nablarch.fw.DataReader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Result;
import nablarch.fw.results.TransactionAbnormalEnd;
import nablarch.fw.action.BatchAction;
import nablarch.fw.messaging.MessageSendSyncTimeoutException;
import nablarch.fw.messaging.MessageSender;
import nablarch.fw.messaging.SyncMessage;
import nablarch.fw.reader.DatabaseRecordReader;

public class SyncMessageSendAction extends BatchAction<SqlRow> {

    public static SyncMessage lastResponseMessage;

    @Override
    public Result handle(SqlRow inputData, ExecutionContext ctx) {
        try {
            lastResponseMessage = MessageSender.sendSync(new SyncMessage("RM11AD0101").addDataRecord(inputData));
        } catch (MessageSendSyncTimeoutException e) {
            throw new TransactionAbnormalEnd(100, e, "RM11AD0101");
        }
        return new Result.Success();
    }

    @Override
    public DataReader<SqlRow> createReader(ExecutionContext ctx) {
        DatabaseRecordReader reader = new DatabaseRecordReader();
        SqlPStatement statement = getSqlPStatement("GET_BATCH_INPUT_DATA");
        reader.setStatement(statement);
        return reader;
    }

    @Override
    protected void transactionSuccess(SqlRow inputData, ExecutionContext context) {
        ParameterizedSqlPStatement statement = getParameterizedSqlStatement("UPDATE_STATUS_NORMAL_END");
        statement.executeUpdateByMap(inputData);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * 処理ステータスを異常終了に更新する。
     */
    @Override
    protected void transactionFailure(SqlRow inputData, ExecutionContext context) {
        ParameterizedSqlPStatement statement = getParameterizedSqlStatement("UPDATE_STATUS_ABNORMAL_END");
        statement.executeUpdateByMap(inputData);
    }
}
