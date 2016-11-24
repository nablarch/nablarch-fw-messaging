package nablarch.fw.messaging;

public class NullHttpMessageIdGenerator implements HttpMessageIdGenerator {

    @Override
    public String generateId() {
        return null;
    }

}
