package nablarch.fw.messaging.sample.action;

import nablarch.fw.ExecutionContext;
import nablarch.fw.launcher.ProcessAbnormalEnd;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.action.MessagingAction;

public class HorribleError extends MessagingAction {

    @Override protected ResponseMessage
    onReceive(RequestMessage req, ExecutionContext ctx) {
        throw new ProcessAbnormalEnd(199, "10001");
    }
}
