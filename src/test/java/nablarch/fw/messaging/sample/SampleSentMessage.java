package nablarch.fw.messaging.sample;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 送信済みメッセージ
 */
@Entity
@Table(name = "SAMPLE_SENT_MESSAGE")
public class SampleSentMessage {
    
    public SampleSentMessage() {
    }
    
	public SampleSentMessage(String messageId, String requestId, String replyQueue,
			String statusCode, byte[] bodyData) {
		this.messageId = messageId;
		this.requestId = requestId;
		this.replyQueue = replyQueue;
		this.statusCode = statusCode;
		this.bodyData = bodyData;
	}

	@Id
    @Column(name = "MESSAGE_ID", length = 255, nullable = false)
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
