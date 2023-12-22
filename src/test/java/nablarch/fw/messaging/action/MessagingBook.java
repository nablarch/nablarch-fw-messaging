package nablarch.fw.messaging.action;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * MESSAGING_BOOK
 */
@Entity
@Table(name = "MESSAGING_BOOK")
public class MessagingBook {
   
    public MessagingBook() {
    };
    
    public MessagingBook(String bookId, String title, String publisher, String authors, String status) {
    	this.bookId = bookId;
		this.title = title;
		this.publisher = publisher;
		this.authors = authors;
		this.status = status;
	}
    
    @Id
    @Column(name = "BOOK_ID", length = 10, nullable = false)
    public String bookId;
	
    @Column(name = "TITLE", length = 20)
    public String title;
    
    @Column(name = "PUBLISHER", length = 20)
    public String publisher;
    
    @Column(name = "AUTHORS", length = 20)
    public String authors;
    
    @Column(name = "STATUS", length = 1)
    public String status = "0";
}