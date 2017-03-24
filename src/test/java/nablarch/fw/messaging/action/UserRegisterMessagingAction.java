package nablarch.fw.messaging.action;

import nablarch.core.ThreadContext;
import nablarch.core.db.connection.AppDbConnection;
import nablarch.core.db.connection.DbConnectionContext;
import nablarch.core.db.statement.SqlPStatement;
import nablarch.core.message.ApplicationException;
import nablarch.core.message.Message;
import nablarch.core.message.MessageLevel;
import nablarch.core.message.StringResource;
import nablarch.core.util.StringUtil;
import nablarch.core.validation.ValidateFor;
import nablarch.core.validation.ValidationContext;
import nablarch.core.validation.ValidationUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.messaging.RequestMessage;
import nablarch.fw.messaging.ResponseMessage;
import nablarch.fw.messaging.action.MessagingAction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Httpメッセージングの機能テストで使用するアクション
 *
 * @author hisaaki sioiri
 */
public class UserRegisterMessagingAction extends MessagingAction {

    @Override
    protected ResponseMessage onReceive(RequestMessage request, ExecutionContext context) {
        UserRegisterForm form = UserRegisterForm.validate(request.getParamMap());

        AppDbConnection connection = DbConnectionContext.getConnection();
        SqlPStatement statement = connection.prepareStatement(
                "insert into users (id, name, mail)values (?, ?, ?)");
        statement.setString(1, form.getId());
        statement.setString(2, form.getName());
        statement.setString(3, form.getMail());
        statement.executeUpdate();

        if (StringUtil.hasValue(form.getErrorClass())) {
            if ("appError".equals(form.getErrorClass())) {
                throw new ApplicationException(new Message(MessageLevel.ERROR, new StringResource() {
                    @Override
                    public String getId() {
                        return "error";
                    }

                    @Override
                    public String getValue(Locale locale) {
                        return "業務エラーが発生しました。";
                    }
                }));
            }
            Throwable t;
            try {
                t = (Throwable) Class.forName(form.errorClass).newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }

        final String requestId;
        if (ThreadContext.getRequestId() == null) {
            requestId = "";
        } else {
        	requestId = "リクエストID:" + ThreadContext.getRequestId();
        }
        
        final String userId;
        if (ThreadContext.getUserId() == null) {
            userId = "";
        } else {
            userId = "ユーザID:" + ThreadContext.getUserId();
        }
        ResponseMessage reply = request.reply();
        reply.setStatusCodeHeader("200");
        reply.addRecord(new HashMap<String, Object>() {
            {
            put("message", "登録出来ました。 " + requestId + " " + userId);
            }
        });
        return reply;
    }

    @Override
    protected ResponseMessage onError(Throwable e, RequestMessage request, ExecutionContext context) {
        ResponseMessage reply = request.reply();

        String message;
        if (e instanceof ApplicationException) {
            reply.setStatusCodeHeader("400");
            message = "業務エラーが発生しました。";
        } else {
            reply.setStatusCodeHeader("500");
            message = "システムエラーが発生しました。";
        }
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", message + ":" + e.getClass().getSimpleName());
        reply.addRecord(body);
        return reply;
    }

    public static class UserRegisterForm {

        private String id;

        private String name;

        private String mail;

        private String errorClass;

        public UserRegisterForm(Map<String, ?> params) {
            id = (String) params.get("id");
            name = (String) params.get("name");
            mail = (String) params.get("mail");
            errorClass = (String) params.get("errorClass");
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMail() {
            return mail;
        }

        public void setMail(String mail) {
            this.mail = mail;
        }

        public String getErrorClass() {
            return errorClass;
        }

        public void setErrorClass(String errorClass) {
            this.errorClass = errorClass;
        }

        public static UserRegisterForm validate(Map<String, ?> param) {
        	
        	ValidationContext<UserRegisterForm> context;
			if (param.containsKey("data.id")) {
				context = ValidationUtil.validateAndConvertRequest("data",
						UserRegisterForm.class, param, "register");
			} else {
				context = ValidationUtil.validateAndConvertRequest(null,
						UserRegisterForm.class, param, "register");
			}
            
            context.abortIfInvalid();
            return context.createObject();
        }

        @ValidateFor("register")
        public static void validateRegister(ValidationContext<UserRegisterForm> context) {
            ValidationUtil.validate(context, new String[]{"id", "name", "mail", "errorClass"});
        }

        @Override
        public String toString() {
            return "UserRegisterForm{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", mail='" + mail + '\'' +
                    '}';
        }
    }
}

