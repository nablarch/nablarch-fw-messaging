package nablarch.fw.messaging.action;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * テストリクエスト
 *
 */
@Entity
@Table(name = "REQUEST")
public class Request {

    public Request() {
    };

    public Request(String id, String state) {
        this.id = id;
        this.state = state;
    }

    @Id
    @Column(name = "ID", length = 30,  nullable = false)
    public String id;

    @Column(name = "STATE", length = 1,  nullable = false)
    public String state;
}
