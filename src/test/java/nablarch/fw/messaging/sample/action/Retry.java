package nablarch.fw.messaging.sample.action;

import nablarch.fw.ExecutionContext;
import nablarch.fw.handler.retry.RetryableException;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.action.MessagingAction;

public class Retry extends MessagingAction {

    @Override protected ResponseMessage
    onReceive(RequestMessage req, ExecutionContext ctx) {
        throw new RetryableException("retry");
    }
}
