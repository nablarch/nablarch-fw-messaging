package nablarch.fw.messaging.sample.action;

import nablarch.core.dataformat.InvalidDataFormatException;
import nablarch.core.db.statement.SqlRow;
import nablarch.core.message.ApplicationException;
import nablarch.core.util.map.CaseInsensitiveMap;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.MessagingException;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.action.MessagingAction;

import java.util.HashMap;
import java.util.Map;

public class BookList extends MessagingAction {
    @Override
    public ResponseMessage onReceive(RequestMessage req, ExecutionContext ctx) {
        ResponseMessage res = req.reply();
        
        int bookCount = 0;
        for (SqlRow record : getSqlPStatement("LIST_BOOKS").executeQuery()) {
            Map<String, Object> book = new CaseInsensitiveMap<Object>();
            book.putAll(record);
            res.addRecord("Book",  book);
            bookCount++;
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("bookCount", bookCount);
        res.addRecord("Summary", summary);
        return res;
    }
    
    public ResponseMessage
    onError(Throwable e, RequestMessage req, ExecutionContext ctx) {
        
        String reasonCode = (e instanceof InvalidDataFormatException) ? "2000"
                          : (e instanceof MessagingException)         ? "3000"
                          : (e instanceof ApplicationException)       ? "4000"
                          : "9999";
        
        Map<String, Object> reasonRecord = new HashMap<String, Object>();
        
        reasonRecord.put("recordType", "0");
        reasonRecord.put("reasonCode", reasonCode);
        reasonRecord.put("message", e.getMessage().substring(0, 100));
        
        ResponseMessage errorResponse = req.reply();
        errorResponse.addRecord(reasonRecord);
        return errorResponse;
    }
}
