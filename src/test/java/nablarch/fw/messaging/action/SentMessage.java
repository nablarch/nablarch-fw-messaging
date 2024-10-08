package nablarch.fw.messaging.action;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * テストユーザー情報
 *
 */
@Entity
@Table(name = "SENT_MESSAGE")
public class SentMessage {

    public SentMessage() {
    };

    public SentMessage(String messageId, String requestId, String replyQueue, String statusCode) {
        this.messageId = messageId;
        this.requestId = requestId;
        this.replyQueue = replyQueue;
        this.statusCode = statusCode;
    }

    @Id
    @Column(name = "MESSAGE_ID", length = 255,  nullable = false)
    public String messageId;

    @Column(name = "REQUEST_ID", length = 64,  nullable = false)
    public String requestId;

    @Column(name = "REPLY_QUEUE", length = 64,  nullable = false)
    public String replyQueue;

    @Column(name = "STATUS_CODE", length = 64,  nullable = false)
    public String statusCode;

    @Column(name = "BODY_DATA", length = 4000,  nullable = false)
    public byte[] bodyData;
}
