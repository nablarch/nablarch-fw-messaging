package nablarch.fw.messaging.handler;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 送信済みメッセージ
 */
@Entity
@Table(name = "TBL_SENT_MESSAGE")
public class TblSentMessage {
    
    public TblSentMessage() {
    }
    
	@Id
    @Column(name = "CLM_MESSAGE_ID", length = 64, nullable = false)
    public String clmMessageId;
    
	@Id
    @Column(name = "CLM_REQUEST_ID", length = 64, nullable = false)
    public String clmRequestId;
    
    @Column(name = "CLM_REPLY_QUEUE", length = 64)
    public String clmReplyQueue = "";
    
    @Column(name = "CLM_STATUS_CODE", length = 4)
    public String clmStatusCode = "";
    
    @Column(name = "CLM_BODY_DATA")
    public byte[] clmBodyData;
}