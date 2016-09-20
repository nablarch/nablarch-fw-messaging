package nablarch.core.util;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * マップユーティリティ
 * 
 * @author TIS
 */
public final class MapUtil {
    
    /**
     * 隠蔽コンストラクタ
     */
    private MapUtil() {
        // NOP
    }
    

    /**
     * オブジェクトから階層構造をキーで表現したマップを作成します。
     * 
     * @see MapUtil#createFlatMap(String, Object)
     * 
     * @param form フォーム
     * @return マップ
     */
    public static Map<String, Object> createFlatMap(Object form) {
        return createFlatMap("", form);
    }
    
    /**
     * オブジェクトから階層構造をキーで表現したマップを作成します。
     * <p/>
     * 本メソッドではオブジェクト内の全てのゲッターメソッドを対象として、データ取得を行い、
     * 以下の規則に従って、取得したデータをマップに格納します。<br/>
     * プロパティ名はゲッターメソッド名からgetを除き先頭を大文字に変換した名称とします。
     * 
     * <table border="1">
     * <tr bgcolor="#cccccc">
     *   <th>対象データ型</th>
     *   <th>格納キー</th>
     *   <th>格納データ型</th>
     *   <th>格納データ内容</th>
     *   <th>備考</th>
     * </tr>
     * <tr>
     *   <td>String</td>
     *   <td>"${プリフィックス}." + プロパティ名</td>
     *   <td>String</td>
     *   <td>取得データそのまま</td>
     *   <td>&nbsp</td>
     * </tr>
     * <tr>
     *   <td>String[]</td>
     *   <td>"${プリフィックス}." + プロパティ名</td>
     *   <td>String[]</td>
     *   <td>取得データそのまま</td>
     *   <td>&nbsp</td>
     * </tr>
     * <tr>
     *   <td>Number</td>
     *   <td>"${プリフィックス}." + プロパティ名</td>
     *   <td>String</td>
     *   <td>取得データを文字列化したもの</td>
     *   <td>&nbsp</td>
     * </tr>
     * <tr>
     *   <td>Boolean</td>
     *   <td>"${プリフィックス}." + プロパティ名</td>
     *   <td>String</td>
     *   <td>取得データを文字列化したもの</td>
     *   <td>&nbsp</td>
     * </tr>
     * <tr>
     *   <td>その他オブジェクト</td>
     *   <td>"${プリフィックス}." + プロパティ名  + "." + オブジェクト内のプロパティ名</td>
     *   <td>StringまたはString[]</td>
     *   <td>オブジェクト内のプロパティデータ型による</td>
     *   <td>再帰的に処理が行われる</td>
     * </tr>
     * <tr>
     *   <td>その他オブジェクトの配列</td>
     *   <td>"${プリフィックス}." + プロパティ名  + "[${要素番号}]" + "." + オブジェクト内のプロパティ名</td>
     *   <td>StringまたはString[]</td>
     *   <td>オブジェクト内のプロパティデータ型による</td>
     *   <td>再帰的に処理が行われる</td>
     * </tr>
     * </table>
     * 
     * @param prefix プリフィックス
     * @param form フォーム
     * @return マップ
     */
    public static Map<String, Object> createFlatMap(String prefix, Object form) {
        Map<String, Object> map = new HashMap<String, Object>();
        
        if (form == null) {
            // 空のマップを返却
            return map;
        }
        
        // 内部で使用するプリフィックスの調整
        String innerPrefix;
        if (StringUtil.isNullOrEmpty(prefix)) {
            innerPrefix = "";
        } else {
            innerPrefix = prefix + ".";
        }

        // Mapの場合は値をつめなおして返却
        if (form instanceof Map<?, ?>) {
            for (Entry<?, ?> e : ((Map<?, ?>) form).entrySet()) {
                if (e.getKey() != null) {
                    if (e.getValue() instanceof Map<?, ?>) {
                        map.putAll(createFlatMap(innerPrefix + e.getKey(), e.getValue()));
                    } else {
                        map.put(innerPrefix + e.getKey(), e.getValue());
                    }
                }
            }
            return map;
        }
        
        // 全てのゲッターメソッドを走査しマップに格納
        for (Method m : ObjectUtil.getGetterMethods(form.getClass())) {
            String propName = ObjectUtil.getPropertyNameFromGetter(m);
            Object o = ObjectUtil.getProperty(form, propName);
            
            if (o == null) {
                map.put(innerPrefix + propName, null);
                
            } else if (o instanceof String) {
                map.put(innerPrefix + propName, o);
                
            } else if (o instanceof String[]) {
                map.put(innerPrefix + propName, o);
                
            } else if (o instanceof Number) {
                map.put(innerPrefix + propName, StringUtil.toString(o));
                
            } else if (o instanceof Boolean) {
                map.put(innerPrefix + propName, o.toString());
                
            } else if (m.getReturnType().isArray()) {
                int length = Array.getLength(o);
                for (int i = 0; i < length; i++) {
                    map.putAll(createFlatMap(innerPrefix + propName + "[" + i + "]", Array.get(o, i)));
                }
                
            } else {
                map.putAll(createFlatMap(innerPrefix + propName, o));
            }
        }
        return map;
    }

}
