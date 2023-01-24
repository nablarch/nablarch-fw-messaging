package nablarch.fw.messaging.sample;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Messaging用テストメッセージ
 */
@Entity
@Table(name = "MESSAGING_TEST_MESSAGE")
public class MessagingTestMessage {
    
    public MessagingTestMessage() {
    };
    
    public MessagingTestMessage(String messageId, String lang, String message) {
        this.messageId = messageId;
        this.lang = lang;
        this.message = message;
    }

    @Id
    @Column(name = "MESSAGE_ID", length = 10, nullable = false)
    public String messageId;
    
    @Id
    @Column(name = "LANG", length = 2, nullable = false)
    public String lang;
    
    @Column(name = "MESSAGE", length = 200)
    public String message;
}
