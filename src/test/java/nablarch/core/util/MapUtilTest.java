/**
 * 
 */
package nablarch.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import nablarch.core.dataformat.SimpleDataConvertUtil;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link MapUtil}のテストを行います。
 * 
 * @author TIS
 */
public class MapUtilTest {
    
    /**
     * １階層のデータを保持したオブジェクトの変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, booleanを対象としてフィールドに値を設定する。<br>
     *   
     * 期待結果：<br>
     *   String, short, int, long, float, double, BigDecimal, booleanは、各値の文字列表現がMapに設定されること。<br>
     *   String[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     */
    @Test
    public void testOneStratum() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        SimpleTestData data = new SimpleTestData();
        data.setStringField("testString");
        data.setStringArrayField(testStringArray);
        data.setShortField((short)1);
        data.setIntField(2);
        data.setLongField(3L);
        data.setFloatField(1.1f);
        data.setDoubleField(2.2d);
        data.setBigDecimalField(testBigDecimal);
        data.setBooleanField(true);
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", "1");
        expected.put("intField", "2");
        expected.put("longField", "3");
        expected.put("floatField", "1.1");
        expected.put("doubleField", "2.2");
        expected.put("bigDecimalField", testBigDecimal.toString());
        expected.put("booleanField", Boolean.TRUE.toString());
        
        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }
    
    /**
     * nullを保持したオブジェクトの変換テストです。
     * 
     * 条件：<br>
     *   Stringにフィールドにnullを設定する。<br>
     *   
     * 期待結果：<br>
     *   nullがMapに設定されること。<br>
     */
    @Test
    public void testNullField() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal("0.0000000001");
        SimpleTestData data = new SimpleTestData();
        data.setStringField(null);
        data.setStringArrayField(testStringArray);
        data.setShortField((short)1);
        data.setIntField(2);
        data.setLongField(3L);
        data.setFloatField(1.1f);
        data.setDoubleField(2.2d);
        data.setBigDecimalField(testBigDecimal);
        data.setBooleanField(true);
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", null);
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", "1");
        expected.put("intField", "2");
        expected.put("longField", "3");
        expected.put("floatField", "1.1");
        expected.put("doubleField", "2.2");
        expected.put("bigDecimalField", "0.0000000001");
        expected.put("booleanField", Boolean.TRUE.toString());
        
        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }

    /**
     * ２階層のデータを保持したオブジェクトの変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, boolean, Object, Object[]を対象としてフィールドに値を設定する。<br>
     *   
     * 期待結果：<br>
     *   String, short, int, long, float, double, BigDecimal, booleanは、各値の文字列表現がMapに設定されること。<br>
     *   String[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     *   Objectは、プロパティ名をプリフィックスとしたキーが生成され、オブジェクト内の各プロパティがMapに設定されること。<br>
     *   Object[]は、プロパティ名をプリフィックスとし、かつ配列添字を付加したキーが生成され、オブジェクト内の各プロパティがMapに設定されること。<br>
     */
    @Test
    public void testTwoStratum() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        SimpleTestData nestData = new SimpleTestData();
        nestData.setStringField("testString");
        nestData.setStringArrayField(testStringArray);
        nestData.setShortField((short)1);
        nestData.setIntField(2);
        nestData.setLongField(3L);
        nestData.setFloatField(1.1f);
        nestData.setDoubleField(2.2d);
        nestData.setBigDecimalField(testBigDecimal);
        nestData.setBooleanField(true);
        
        SimpleTestData nestData1 = new SimpleTestData();
        nestData1.setStringField("testString1");
        nestData1.setStringArrayField(testStringArray);
        nestData1.setShortField((short)1);
        nestData1.setIntField(2);
        nestData1.setLongField(3L);
        nestData1.setFloatField(1.1f);
        nestData1.setDoubleField(2.2d);
        nestData1.setBigDecimalField(testBigDecimal);
        nestData1.setBooleanField(true);
        
        SimpleTestData nestData2 = new SimpleTestData();
        nestData2.setStringField("testString2");
        nestData2.setStringArrayField(testStringArray);
        nestData2.setShortField((short)1);
        nestData2.setIntField(2);
        nestData2.setLongField(3L);
        nestData2.setFloatField(1.1f);
        nestData2.setDoubleField(2.2d);
        nestData2.setBigDecimalField(testBigDecimal);
        nestData2.setBooleanField(true);
        
        StratumData data = new StratumData();
        data.setStringField("testString");
        data.setStringArrayField(testStringArray);
        data.setShortField((short)1);
        data.setIntField(2);
        data.setLongField(3L);
        data.setFloatField(1.1f);
        data.setDoubleField(2.2d);
        data.setBigDecimalField(testBigDecimal);
        data.setBooleanField(true);
        data.setObjectField(nestData);
        data.setObjectArrayField(new SimpleTestData[]{nestData1, nestData2});
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", "1");
        expected.put("intField", "2");
        expected.put("longField", "3");
        expected.put("floatField", "1.1");
        expected.put("doubleField", "2.2");
        expected.put("bigDecimalField", testBigDecimal.toString());
        expected.put("booleanField", Boolean.TRUE.toString());
        expected.put("objectField.stringField", "testString");
        expected.put("objectField.stringArrayField", testStringArray);
        expected.put("objectField.shortField", "1");
        expected.put("objectField.intField", "2");
        expected.put("objectField.longField", "3");
        expected.put("objectField.floatField", "1.1");
        expected.put("objectField.doubleField", "2.2");
        expected.put("objectField.bigDecimalField", testBigDecimal.toString());
        expected.put("objectField.booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayField[0].stringField", "testString1");
        expected.put("objectArrayField[0].stringArrayField", testStringArray);
        expected.put("objectArrayField[0].shortField", "1");
        expected.put("objectArrayField[0].intField", "2");
        expected.put("objectArrayField[0].longField", "3");
        expected.put("objectArrayField[0].floatField", "1.1");
        expected.put("objectArrayField[0].doubleField", "2.2");
        expected.put("objectArrayField[0].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayField[0].booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayField[1].stringField", "testString2");
        expected.put("objectArrayField[1].stringArrayField", testStringArray);
        expected.put("objectArrayField[1].shortField", "1");
        expected.put("objectArrayField[1].intField", "2");
        expected.put("objectArrayField[1].longField", "3");
        expected.put("objectArrayField[1].floatField", "1.1");
        expected.put("objectArrayField[1].doubleField", "2.2");
        expected.put("objectArrayField[1].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayField[1].booleanField", Boolean.TRUE.toString());
        
        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }

    /**
     * ３階層のデータを保持したオブジェクトの変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, boolean, Object, Object[]を対象としてフィールドに値を設定する。<br>
     *   Objectフィールド内でさらに階層構造を持たせた値を設定する。<br>
     *   
     * 期待結果：<br>
     *   String, short, int, long, float, double, BigDecimal, booleanは、各値の文字列表現がMapに設定されること。<br>
     *   String[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     *   Objectは、プロパティ名をプリフィックスとしたキーが生成され、オブジェクト内の各プロパティがMapに設定されること。<br>
     *   Object[]は、プロパティ名をプリフィックスとし、かつ配列添字を付加したキーが生成され、オブジェクト内の各プロパティがMapに設定されること。<br>
     */
    @Test
    public void testThreeStratum() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        SimpleTestData nestNestData = new SimpleTestData();
        nestNestData.setStringField("testNestString");
        nestNestData.setStringArrayField(testStringArray);
        nestNestData.setShortField((short)1);
        nestNestData.setIntField(2);
        nestNestData.setLongField(3L);
        nestNestData.setFloatField(1.1f);
        nestNestData.setDoubleField(2.2d);
        nestNestData.setBigDecimalField(testBigDecimal);
        nestNestData.setBooleanField(true);
        
        SimpleTestData nestNestData1 = new SimpleTestData();
        nestNestData1.setStringField("testNestString1");
        nestNestData1.setStringArrayField(testStringArray);
        nestNestData1.setShortField((short)1);
        nestNestData1.setIntField(2);
        nestNestData1.setLongField(3L);
        nestNestData1.setFloatField(1.1f);
        nestNestData1.setDoubleField(2.2d);
        nestNestData1.setBigDecimalField(testBigDecimal);
        nestNestData1.setBooleanField(true);
        
        SimpleTestData nestNestData2 = new SimpleTestData();
        nestNestData2.setStringField("testNestString2");
        nestNestData2.setStringArrayField(testStringArray);
        nestNestData2.setShortField((short)1);
        nestNestData2.setIntField(2);
        nestNestData2.setLongField(3L);
        nestNestData2.setFloatField(1.1f);
        nestNestData2.setDoubleField(2.2d);
        nestNestData2.setBigDecimalField(testBigDecimal);
        nestNestData2.setBooleanField(true);
        
        StratumData nestData = new StratumData();
        nestData.setStringField("testString");
        nestData.setStringArrayField(testStringArray);
        nestData.setShortField((short)1);
        nestData.setIntField(2);
        nestData.setLongField(3L);
        nestData.setFloatField(1.1f);
        nestData.setDoubleField(2.2d);
        nestData.setBigDecimalField(testBigDecimal);
        nestData.setBooleanField(true);
        nestData.setObjectField(nestNestData);
        nestData.setObjectArrayField(new SimpleTestData[]{nestNestData1, nestNestData2});

        StratumData nestData1 = new StratumData();
        nestData1.setStringField("testString1");
        nestData1.setStringArrayField(testStringArray);
        nestData1.setShortField((short)1);
        nestData1.setIntField(2);
        nestData1.setLongField(3L);
        nestData1.setFloatField(1.1f);
        nestData1.setDoubleField(2.2d);
        nestData1.setBigDecimalField(testBigDecimal);
        nestData1.setBooleanField(true);
        nestData1.setObjectField(nestNestData);
        nestData1.setObjectArrayField(new SimpleTestData[]{nestNestData1, nestNestData2});
        
        StratumData nestData2 = new StratumData();
        nestData2.setStringField("testString2");
        nestData2.setStringArrayField(testStringArray);
        nestData2.setShortField((short)1);
        nestData2.setIntField(2);
        nestData2.setLongField(3L);
        nestData2.setFloatField(1.1f);
        nestData2.setDoubleField(2.2d);
        nestData2.setBigDecimalField(testBigDecimal);
        nestData2.setBooleanField(true);
        nestData2.setObjectField(nestNestData);
        nestData2.setObjectArrayField(new SimpleTestData[]{nestNestData1, nestNestData2});

        StratumData2 data = new StratumData2();
        data.setStringField("testString");
        data.setStringArrayField(testStringArray);
        data.setShortField((short)1);
        data.setIntField(2);
        data.setLongField(3L);
        data.setFloatField(1.1f);
        data.setDoubleField(2.2d);
        data.setBigDecimalField(testBigDecimal);
        data.setBooleanField(true);
        data.setObjectNestField(nestData);
        data.setObjectArrayNestField(new StratumData[]{nestData1, nestData2});
        
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", "1");
        expected.put("intField", "2");
        expected.put("longField", "3");
        expected.put("floatField", "1.1");
        expected.put("doubleField", "2.2");
        expected.put("bigDecimalField", testBigDecimal.toString());
        expected.put("booleanField", Boolean.TRUE.toString());
        
        expected.put("objectNestField.stringField", "testString");
        expected.put("objectNestField.stringArrayField", testStringArray);
        expected.put("objectNestField.shortField", "1");
        expected.put("objectNestField.intField", "2");
        expected.put("objectNestField.longField", "3");
        expected.put("objectNestField.floatField", "1.1");
        expected.put("objectNestField.doubleField", "2.2");
        expected.put("objectNestField.bigDecimalField", testBigDecimal.toString());
        expected.put("objectNestField.booleanField", Boolean.TRUE.toString());
        expected.put("objectNestField.objectField.stringField", "testNestString");
        expected.put("objectNestField.objectField.stringArrayField", testStringArray);
        expected.put("objectNestField.objectField.shortField", "1");
        expected.put("objectNestField.objectField.intField", "2");
        expected.put("objectNestField.objectField.longField", "3");
        expected.put("objectNestField.objectField.floatField", "1.1");
        expected.put("objectNestField.objectField.doubleField", "2.2");
        expected.put("objectNestField.objectField.bigDecimalField", testBigDecimal.toString());
        expected.put("objectNestField.objectField.booleanField", Boolean.TRUE.toString());
        expected.put("objectNestField.objectArrayField[0].stringField", "testNestString1");
        expected.put("objectNestField.objectArrayField[0].stringArrayField", testStringArray);
        expected.put("objectNestField.objectArrayField[0].shortField", "1");
        expected.put("objectNestField.objectArrayField[0].intField", "2");
        expected.put("objectNestField.objectArrayField[0].longField", "3");
        expected.put("objectNestField.objectArrayField[0].floatField", "1.1");
        expected.put("objectNestField.objectArrayField[0].doubleField", "2.2");
        expected.put("objectNestField.objectArrayField[0].bigDecimalField", testBigDecimal.toString());
        expected.put("objectNestField.objectArrayField[0].booleanField", Boolean.TRUE.toString());
        expected.put("objectNestField.objectArrayField[1].stringField", "testNestString2");
        expected.put("objectNestField.objectArrayField[1].stringArrayField", testStringArray);
        expected.put("objectNestField.objectArrayField[1].shortField", "1");
        expected.put("objectNestField.objectArrayField[1].intField", "2");
        expected.put("objectNestField.objectArrayField[1].longField", "3");
        expected.put("objectNestField.objectArrayField[1].floatField", "1.1");
        expected.put("objectNestField.objectArrayField[1].doubleField", "2.2");
        expected.put("objectNestField.objectArrayField[1].bigDecimalField", testBigDecimal.toString());
        expected.put("objectNestField.objectArrayField[1].booleanField", Boolean.TRUE.toString());

        expected.put("objectArrayNestField[0].stringField", "testString1");
        expected.put("objectArrayNestField[0].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[0].shortField", "1");
        expected.put("objectArrayNestField[0].intField", "2");
        expected.put("objectArrayNestField[0].longField", "3");
        expected.put("objectArrayNestField[0].floatField", "1.1");
        expected.put("objectArrayNestField[0].doubleField", "2.2");
        expected.put("objectArrayNestField[0].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[0].booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[0].objectField.stringField", "testNestString");
        expected.put("objectArrayNestField[0].objectField.stringArrayField", testStringArray);
        expected.put("objectArrayNestField[0].objectField.shortField", "1");
        expected.put("objectArrayNestField[0].objectField.intField", "2");
        expected.put("objectArrayNestField[0].objectField.longField", "3");
        expected.put("objectArrayNestField[0].objectField.floatField", "1.1");
        expected.put("objectArrayNestField[0].objectField.doubleField", "2.2");
        expected.put("objectArrayNestField[0].objectField.bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[0].objectField.booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[0].objectArrayField[0].stringField", "testNestString1");
        expected.put("objectArrayNestField[0].objectArrayField[0].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[0].objectArrayField[0].shortField", "1");
        expected.put("objectArrayNestField[0].objectArrayField[0].intField", "2");
        expected.put("objectArrayNestField[0].objectArrayField[0].longField", "3");
        expected.put("objectArrayNestField[0].objectArrayField[0].floatField", "1.1");
        expected.put("objectArrayNestField[0].objectArrayField[0].doubleField", "2.2");
        expected.put("objectArrayNestField[0].objectArrayField[0].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[0].objectArrayField[0].booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[0].objectArrayField[1].stringField", "testNestString2");
        expected.put("objectArrayNestField[0].objectArrayField[1].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[0].objectArrayField[1].shortField", "1");
        expected.put("objectArrayNestField[0].objectArrayField[1].intField", "2");
        expected.put("objectArrayNestField[0].objectArrayField[1].longField", "3");
        expected.put("objectArrayNestField[0].objectArrayField[1].floatField", "1.1");
        expected.put("objectArrayNestField[0].objectArrayField[1].doubleField", "2.2");
        expected.put("objectArrayNestField[0].objectArrayField[1].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[0].objectArrayField[1].booleanField", Boolean.TRUE.toString());

        expected.put("objectArrayNestField[1].stringField", "testString2");
        expected.put("objectArrayNestField[1].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[1].shortField", "1");
        expected.put("objectArrayNestField[1].intField", "2");
        expected.put("objectArrayNestField[1].longField", "3");
        expected.put("objectArrayNestField[1].floatField", "1.1");
        expected.put("objectArrayNestField[1].doubleField", "2.2");
        expected.put("objectArrayNestField[1].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[1].booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[1].objectField.stringField", "testNestString");
        expected.put("objectArrayNestField[1].objectField.stringArrayField", testStringArray);
        expected.put("objectArrayNestField[1].objectField.shortField", "1");
        expected.put("objectArrayNestField[1].objectField.intField", "2");
        expected.put("objectArrayNestField[1].objectField.longField", "3");
        expected.put("objectArrayNestField[1].objectField.floatField", "1.1");
        expected.put("objectArrayNestField[1].objectField.doubleField", "2.2");
        expected.put("objectArrayNestField[1].objectField.bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[1].objectField.booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[1].objectArrayField[0].stringField", "testNestString1");
        expected.put("objectArrayNestField[1].objectArrayField[0].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[1].objectArrayField[0].shortField", "1");
        expected.put("objectArrayNestField[1].objectArrayField[0].intField", "2");
        expected.put("objectArrayNestField[1].objectArrayField[0].longField", "3");
        expected.put("objectArrayNestField[1].objectArrayField[0].floatField", "1.1");
        expected.put("objectArrayNestField[1].objectArrayField[0].doubleField", "2.2");
        expected.put("objectArrayNestField[1].objectArrayField[0].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[1].objectArrayField[0].booleanField", Boolean.TRUE.toString());
        expected.put("objectArrayNestField[1].objectArrayField[1].stringField", "testNestString2");
        expected.put("objectArrayNestField[1].objectArrayField[1].stringArrayField", testStringArray);
        expected.put("objectArrayNestField[1].objectArrayField[1].shortField", "1");
        expected.put("objectArrayNestField[1].objectArrayField[1].intField", "2");
        expected.put("objectArrayNestField[1].objectArrayField[1].longField", "3");
        expected.put("objectArrayNestField[1].objectArrayField[1].floatField", "1.1");
        expected.put("objectArrayNestField[1].objectArrayField[1].doubleField", "2.2");
        expected.put("objectArrayNestField[1].objectArrayField[1].bigDecimalField", testBigDecimal.toString());
        expected.put("objectArrayNestField[1].objectArrayField[1].booleanField", Boolean.TRUE.toString());

        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
        
    }

    /**
     * Mapが指定された場合の変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, booleanを対象としてMapに値を設定する。<br>
     *   
     * 期待結果：<br>
     *   String, short, int, long, float, double, BigDecimal, booleanは、各値がそのままMapに設定されること。<br>
     *   String[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     */
    @Test
    public void testMapData() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("stringField", "testString");
        data.put("stringArrayField", testStringArray);
        data.put("shortField", (short)1);
        data.put("intField", 2);
        data.put("longField", 3L);
        data.put("floatField", 1.1f);
        data.put("doubleField", 2.2d);
        data.put("bigDecimalField", testBigDecimal);
        data.put("booleanField", true);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", (short)1);
        expected.put("intField", 2);
        expected.put("longField", 3L);
        expected.put("floatField", 1.1f);
        expected.put("doubleField", 2.2d);
        expected.put("bigDecimalField", testBigDecimal);
        expected.put("booleanField", true);
        
        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }
    
    /**
     * Form内にMapが指定された場合の変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, booleanを対象としてMapに値を設定する。<br>
     *   値が設定されたMapをフォームに設定する。<br>
     *   
     * 期待結果：<br>
     *   Map内のString, short, int, long, float, double, BigDecimal, booleanは、各値がそのままMapに設定されること。<br>
     *   Map内のString[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     *   Map内のデータについてはフォーム上のフィールド名をプリフィックスとしたキーが生成され、MAp内の各データがMapに設定されること。<br>
     */
    @Test
    public void testFormMapData() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        Map<String, Object> mapData = new HashMap<String, Object>();
        mapData.put("stringField", "testString");
        mapData.put("stringArrayField", testStringArray);
        mapData.put("shortField", (short)1);
        mapData.put("intField", 2);
        mapData.put("longField", 3L);
        mapData.put("floatField", 1.1f);
        mapData.put("doubleField", 2.2d);
        mapData.put("bigDecimalField", testBigDecimal);
        mapData.put("booleanField", true);

        HasMapData data = new HasMapData();
        data.setStringField("testString");
        data.setMapField(mapData);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("mapField.stringField", "testString");
        expected.put("mapField.stringArrayField", testStringArray);
        expected.put("mapField.shortField", (short)1);
        expected.put("mapField.intField", 2);
        expected.put("mapField.longField", 3L);
        expected.put("mapField.floatField", 1.1f);
        expected.put("mapField.doubleField", 2.2d);
        expected.put("mapField.bigDecimalField", testBigDecimal);
        expected.put("mapField.booleanField", true);

        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }
    
    /**
     * Map内にMapが指定された場合の変換テストです。
     * 
     * 条件：<br>
     *   String, String[], short, int, long, float, double, BigDecimal, booleanを対象としてMapに値を設定する。<br>
     *   値が設定されたMapをMapに設定する。<br>
     *   
     * 期待結果：<br>
     *   Map内のString, short, int, long, float, double, BigDecimal, booleanは、各値がそのままMapに設定されること。<br>
     *   Map内のString[]は、配列オブジェクトがそのままがMapに設定されること。<br>
     *   Map内のデータについては親Map上のキー名をプリフィックスとしたキーが生成され、MAp内の各データがMapに設定されること。<br>
     */
    @Test
    public void testMapMapData() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        Map<String, Object> mapData = new HashMap<String, Object>();
        mapData.put("stringField", "testString");
        mapData.put("stringArrayField", testStringArray);
        mapData.put("shortField", (short)1);
        mapData.put("intField", 2);
        mapData.put("longField", 3L);
        mapData.put("floatField", 1.1f);
        mapData.put("doubleField", 2.2d);
        mapData.put("bigDecimalField", testBigDecimal);
        mapData.put("booleanField", true);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("stringField", "testString");
        data.put("mapField", mapData);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("mapField.stringField", "testString");
        expected.put("mapField.stringArrayField", testStringArray);
        expected.put("mapField.shortField", (short)1);
        expected.put("mapField.intField", 2);
        expected.put("mapField.longField", 3L);
        expected.put("mapField.floatField", 1.1f);
        expected.put("mapField.doubleField", 2.2d);
        expected.put("mapField.bigDecimalField", testBigDecimal);
        expected.put("mapField.booleanField", true);

        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }
    
    /**
     * nullキーを持つMapが指定された場合の変換テストです。
     * 
     * 条件：<br>
     *   キーにnullを指定した要素を持つMapを変換する。
     *   
     * 期待結果：<br>
     *   キーにnullを指定した項目は変換対象とならないこと。
     */
    @Test
    public void testMapHasNullKey() {
        String[] testStringArray = new String[]{"testArrayString1", "testArrayString2"};
        BigDecimal testBigDecimal = new BigDecimal(3.333333333);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("stringField", "testString");
        data.put("stringArrayField", testStringArray);
        data.put("shortField", (short)1);
        data.put("intField", 2);
        data.put("longField", 3L);
        data.put("floatField", 1.1f);
        data.put("doubleField", 2.2d);
        data.put("bigDecimalField", testBigDecimal);
        data.put("booleanField", true);
        data.put(null, "null key");

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("stringField", "testString");
        expected.put("stringArrayField", testStringArray);
        expected.put("shortField", (short)1);
        expected.put("intField", 2);
        expected.put("longField", 3L);
        expected.put("floatField", 1.1f);
        expected.put("doubleField", 2.2d);
        expected.put("bigDecimalField", testBigDecimal);
        expected.put("booleanField", true);
        
        Map<String, Object> actual = MapUtil.createFlatMap(data);
        
        assertEquals(expected, actual);
    }
    
    /**
     * 引数にnullが指定された場合の変換テストです。
     * 
     * 条件：<br>
     *   引数にnullを設定する。
     *   
     * 期待結果：<br>
     *   空のマップが返却されること。
     */
    @Test
    public void testNullValue() {
        
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> actual = MapUtil.createFlatMap(null);
        
        assertEquals(expected, actual);
    }
    


    /**
     * テストデータクラス
     */
    public static class SimpleTestData {
        private String stringField;
        private String[] stringArrayField;
        private short shortField;
        private long longField;
        private int intField;
        private float floatField;
        private double doubleField;
        private BigDecimal bigDecimalField;
        private boolean booleanField;
        /**
         * stringFieldを取得する。
         * @return stringField
         */
        public String getStringField() {
            return stringField;
        }
        /**
         * stringFieldをセットする。
         * @param stringField セットする stringField
         */
        public void setStringField(String stringField) {
            this.stringField = stringField;
        }
        /**
         * stringArrayFieldを取得する。
         * @return stringArrayField
         */
        public String[] getStringArrayField() {
            return stringArrayField;
        }
        /**
         * stringArrayFieldをセットする。
         * @param stringArrayField セットする stringArrayField
         */
        public void setStringArrayField(String[] stringArrayField) {
            this.stringArrayField = stringArrayField;
        }
        /**
         * shortFieldを取得する。
         * @return shortField
         */
        public short getShortField() {
            return shortField;
        }
        /**
         * shortFieldをセットする。
         * @param shortField セットする shortField
         */
        public void setShortField(short shortField) {
            this.shortField = shortField;
        }
        /**
         * intFieldを取得する。
         * @return intField
         */
        public int getIntField() {
            return intField;
        }
        /**
         * intFieldをセットする。
         * @param intField セットする intField
         */
        public void setIntField(int intField) {
            this.intField = intField;
        }
        /**
         * longFieldを取得する。
         * @return longField
         */
        public long getLongField() {
            return longField;
        }
        /**
         * longFieldをセットする。
         * @param longField セットする longField
         */
        public void setLongField(long longField) {
            this.longField = longField;
        }
        /**
         * floatFieldを取得する。
         * @return floatField
         */
        public float getFloatField() {
            return floatField;
        }
        /**
         * floatFieldをセットする。
         * @param floatField セットする floatField
         */
        public void setFloatField(float floatField) {
            this.floatField = floatField;
        }
        /**
         * doubleFieldを取得する。
         * @return doubleField
         */
        public double getDoubleField() {
            return doubleField;
        }
        /**
         * doubleFieldをセットする。
         * @param doubleField セットする doubleField
         */
        public void setDoubleField(double doubleField) {
            this.doubleField = doubleField;
        }
        /**
         * bigDecimalFieldを取得する。
         * @return bigDecimalField
         */
        public BigDecimal getBigDecimalField() {
            return bigDecimalField;
        }
        /**
         * bigDecimalFieldをセットする。
         * @param bigDecimalField セットする bigDecimalField
         */
        public void setBigDecimalField(BigDecimal bigDecimalField) {
            this.bigDecimalField = bigDecimalField;
        }
        /**
         * booleanFieldを取得する。
         * @return booleanField
         */
        public boolean getBooleanField() {
            return booleanField;
        }
        /**
         * booleanFieldをセットする。
         * @param booleanField セットする booleanField
         */
        public void setBooleanField(boolean booleanField) {
            this.booleanField = booleanField;
        }
    }
    
    /**
     * 階層テストデータクラス
     */
    public static class StratumData extends SimpleTestData {
        private SimpleTestData objectField;
        private SimpleTestData[] objectArrayField;
        /**
         * objectFieldを取得する。
         * @return objectField
         */
        public SimpleTestData getObjectField() {
            return objectField;
        }
        /**
         * objectFieldをセットする。
         * @param objectField セットする objectField
         */
        public void setObjectField(SimpleTestData objectField) {
            this.objectField = objectField;
        }
        /**
         * objectArrayFieldを取得する。
         * @return objectArrayField
         */
        public SimpleTestData[] getObjectArrayField() {
            return objectArrayField;
        }
        /**
         * objectArrayFieldをセットする。
         * @param objectArrayField セットする objectArrayField
         */
        public void setObjectArrayField(SimpleTestData[] objectArrayField) {
            this.objectArrayField = objectArrayField;
        }
    }
    
    /**
     * 階層テストデータクラス2
     */
    public static class StratumData2 extends SimpleTestData {
        private StratumData objectNestField;
        private StratumData[] objectArrayNestField;
        /**
         * objectFieldを取得する。
         * @return objectField
         */
        public SimpleTestData getObjectNestField() {
            return objectNestField;
        }
        /**
         * objectFieldをセットする。
         * @param objectField セットする objectField
         */
        public void setObjectNestField(StratumData objectField) {
            this.objectNestField = objectField;
        }
        /**
         * objectArrayFieldを取得する。
         * @return objectArrayField
         */
        public StratumData[] getObjectArrayNestField() {
            return objectArrayNestField;
        }
        /**
         * objectArrayFieldをセットする。
         * @param objectArrayField セットする objectArrayField
         */
        public void setObjectArrayNestField(StratumData[] objectArrayField) {
            this.objectArrayNestField = objectArrayField;
        }
    }

    /**
     * Mapテストデータクラス
     */
    public static class HasMapData {
        private String stringField;
        private Map<String, Object> mapField;
        /**
         * stringFieldを取得する。
         * @return stringField
         */
        public String getStringField() {
            return stringField;
        }
        /**
         * stringFieldをセットする。
         * @param stringField セットする stringField
         */
        public void setStringField(String stringField) {
            this.stringField = stringField;
        }
        /**
         * mapFieldを取得する。
         * @return mapField
         */
        public Map<String, Object> getMapField() {
            return mapField;
        }
        /**
         * mapFieldをセットする。
         * @param mapField セットする mapField
         */
        public void setMapField(Map<String, Object> mapField) {
            this.mapField = mapField;
        }
    }

}
