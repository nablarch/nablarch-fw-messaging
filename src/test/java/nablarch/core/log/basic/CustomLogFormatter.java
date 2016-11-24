package nablarch.core.log.basic;

import nablarch.core.log.LogItem;

import java.util.Map;

public class CustomLogFormatter extends BasicLogFormatter {
    
    // フォーマット対象のログ出力項目を取得するメソッドをオーバーライドする。
    protected Map<String, LogItem<LogContext>> getLogItems(ObjectSettings settings) {
        
        // 起動プロセスのプレースホルダを上書きで設定する。
        Map<String, LogItem<LogContext>> logItems = super.getLogItems(settings);
        logItems.put("$bootProcess$", new CustomBootProcessItem(settings));
        return logItems;
    }
    
    // カスタムの起動プロセスを取得するクラス。
    private static final class CustomBootProcessItem implements LogItem<LogContext> {
        private String bootProcess;
        public CustomBootProcessItem(ObjectSettings settings) {
            // ログフォーマッタの設定から起動プロセスを取得する。
            bootProcess = settings.getProp("bootProcess");
        }
        public String get(LogContext context) {
            return bootProcess;
        }
    }
}
