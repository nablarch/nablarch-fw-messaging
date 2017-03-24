package nablarch.fw.messaging.action;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * テストユーザー情報
 *
 */
@Entity
@Table(name = "USERS")
public class Users {

    public Users() {
    };

    public Users(String id, String name, String mail) {
        this.id = id;
        this.name = name;
        this.mail = mail;
    }

    @Id
    @Column(name = "ID", length = 5,  nullable = false)
    public String id;

    @Column(name = "NAME", length = 10,  nullable = false)
    public String name;

    @Column(name = "MAIL", length = 20)
    public String mail;
}
