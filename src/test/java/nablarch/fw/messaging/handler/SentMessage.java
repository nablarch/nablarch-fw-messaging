package nablarch.fw.messaging.handler;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 送信済みメッセージ。
 */
@Entity
@Table(name = "SENT_MESSAGE_TABLE")
public class SentMessage {

    public SentMessage() {
    }

	public SentMessage(String messageId, String requestId, String replyQueue,
			String statusCode, byte[] bodyData) {
		this.messageId = messageId;
		this.requestId = requestId;
		this.replyQueue = replyQueue;
		this.statusCode = statusCode;
		this.bodyData = bodyData;
	}

	@Id
    @Column(name = "MESSAGE_ID", length = 64, nullable = false)
    public String messageId;

	@Id
    @Column(name = "REQUEST_ID", length = 64, nullable = false)
    public String requestId;

    @Column(name = "REPLY_QUEUE", length = 64)
    public String replyQueue = "";

    @Column(name = "STATUS_CODE", length = 4)
    public String statusCode = "";

    @Column(name = "BODY_DATA")
    public byte[] bodyData;
}