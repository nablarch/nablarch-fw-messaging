package nablarch.fw.messaging.sample.action;

import nablarch.core.dataformat.DataRecord;
import nablarch.core.db.statement.ParameterizedSqlPStatement;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.action.MessagingAction;
import nablarch.fw.results.BadRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterBook extends MessagingAction {
    @Override
    public ResponseMessage onReceive(RequestMessage req, ExecutionContext ctx) {
        ResponseMessage res = req.reply();
        
        req.readRecords(); // 全件読み込む。
        List<DataRecord> bookRecords = req.getRecordsOf("Book");
        DataRecord summary = req.getRecordOf("Summary");
        int bookCount = summary.getBigDecimal("bookCount").intValue();
        if (bookCount != bookRecords.size()) {
            throw new BadRequest();
        }
        
        int updatedCount = 0;
        ParameterizedSqlPStatement stmt = getParameterizedSqlStatement("REGISTER_BOOK");
        for (DataRecord bookRecord : bookRecords) {
            updatedCount += stmt.executeUpdateByMap(bookRecord);
        }
        Map<String, Object> summaryRecord = new HashMap<String, Object>();
        summaryRecord.put("bookCount", updatedCount);
        res.addRecord("Summary", summaryRecord);
        return res;
    }

    @Override
    protected ResponseMessage onError(Throwable e, RequestMessage request, ExecutionContext context) {
        SqlPStatement statement = getSqlPStatement("INSERT_ERROR_LOG");
        statement.setString(1, "failed to register for book.");
        statement.executeUpdate();
        return super.onError(e, request, context);
    }

    /**
     * 登録情報電文はマルチレコード形式なので、業務ロジック側で読み込みを制御する。
     */
    public boolean usesAutoRead() {
        return false;
    }
}
