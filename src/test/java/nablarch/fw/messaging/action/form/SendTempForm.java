package nablarch.fw.messaging.action.form;

import java.sql.Timestamp;
import java.util.Map;

import nablarch.core.db.statement.autoproperty.CurrentDateTime;
import nablarch.core.db.statement.autoproperty.UserId;

/**
 * メッセージ送信テンポラリのフォーム。
 *
 * @author hisaaki sioiri
 */
public class SendTempForm {

    private String messageId;

    @UserId
    private String updatedUserId;

    @CurrentDateTime
    private Timestamp updatedDate;

    public SendTempForm(Map<String, ?> data) {
        messageId = (String) data.get("messageId");
        updatedUserId = (String) data.get("updatedUserId");
        updatedDate = (Timestamp) data.get("updatedDate");
    }

    public String getMessageId() {
        return messageId;
    }

    public String getUpdatedUserId() {
        return updatedUserId;
    }

    public Timestamp getUpdatedDate() {
        return updatedDate;
    }
}
