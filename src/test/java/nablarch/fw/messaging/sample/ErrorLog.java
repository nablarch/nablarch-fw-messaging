package nablarch.fw.messaging.sample;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * ERROR_LOG
 */
@Entity
@Table(name = "ERROR_LOG")
public class ErrorLog {
    
    public ErrorLog() {
    }
    
	public ErrorLog(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Id
    @Column(name = "ERROR_MESSAGE", length = 100, nullable = false)
    public String errorMessage;
}