package nablarch.core.log.app;

import nablarch.core.log.basic.LogWriterSupport;
import nablarch.core.util.Builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OnMemoryLogWriter extends LogWriterSupport {
    
    private static final Map<String, List<String>> messagesMap = new HashMap<String, List<String>>();
    
    public static void clear() {
        messagesMap.clear();
    }
    
    public static List<String> getMessages(String name) {
        if (!messagesMap.containsKey(name)) {
            messagesMap.put(name, new ArrayList<String>());
        }
        return messagesMap.get(name);
    }


    public static void assertLogContains(String name, String... expected) {
        List<String> origExpected = Arrays.asList(expected);

        // まだ発見されていない期待ログ
        Set<String> expectedRest = new HashSet<String>(origExpected);

        List<String> actualLogs = OnMemoryLogWriter.getMessages(name);

        for (String actualLog : actualLogs) {
            Set<String> ok = new HashSet<String>();
            for (String expectedLog : expectedRest) {
                if (actualLog.contains(expectedLog)) {
                    ok.add(expectedLog);
                }
            }
            // 出現した文言を消し込み
            expectedRest.removeAll(ok);
            ok.clear();
        }
        // 期待したログ全てが、実際のログに含まれていること
        if (!expectedRest.isEmpty()) {
            throw new AssertionError(Builder.concat(
                    "expected log not found. \n",
                    "expected = ", origExpected.toString(), "\n",
                    "actual   = ", actualLogs.toString())
            );
        }
    }


    protected void onWrite(String formattedMessage) {
        if (formattedMessage.contains("initialized.")) {
            return;
        }
        getMessages(getName()).add(formattedMessage);
    }

    protected void onTerminate() {
        getMessages(getName()).add("@@@END@@@");
    }
}
