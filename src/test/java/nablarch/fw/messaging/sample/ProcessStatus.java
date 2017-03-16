package nablarch.fw.messaging.sample;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * PROCESS_STATUS
 */
@Entity
@Table(name = "PROCESS_STATUS")
public class ProcessStatus {
    
    public ProcessStatus() {
    }

    public ProcessStatus(String requestId, String status,
			String serviceAvailable) {
		this.requestId = requestId;
		this.status = status;
		this.serviceAvailable = serviceAvailable;
	}

	@Id
    @Column(name = "REQUEST_ID", length = 20, nullable = false)
    public String requestId;

    @Column(name = "STATUS", length = 1, nullable = false)
    public String status;

    @Column(name = "SERVICE_AVAILABLE", length = 1, nullable = false)
    public String serviceAvailable = "1";
}
