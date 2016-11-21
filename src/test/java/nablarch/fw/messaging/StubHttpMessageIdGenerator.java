package nablarch.fw.messaging;

public class StubHttpMessageIdGenerator implements HttpMessageIdGenerator {
    private String name;
    
    @Override
    public String generateId() {
        return Long.toString(System.currentTimeMillis());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
