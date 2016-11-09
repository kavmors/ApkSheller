package jcommon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class PropertiesIO {
    /**
     * 读取properties文件key-value值
     * @param file properties文件对象
     * @return
     */
    public static Map<String, String> read(File file) {
        Map<String, String> map = new HashMap<String, String>();
        Properties p = new Properties();
        try {
            InputStream in = new FileInputStream(file);
            p.load(in);
            Iterator<?> it = p.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>) it.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                map.put(key, value);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return map;
    }

    /**
     * key-value值写入properties文件
     * @param file properties文件对象
     * @param data 数据
     * @return 写入成功则返回true
     */
    public static boolean write(File file, Map<String, String> data) {
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            buffer.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        return FileUtils.write(file, buffer.toString().getBytes());
    }

    /**
     * properties文件添加一个属性
     * @param file properties文件对象
     * @param key 属性名
     * @param value 属性值
     * @return 写入成功则返回true
     */
    public static boolean append(File file, String key, String value) {
        String lastStr = new String(FileUtils.read(file, file.length()-1, 1));
        String content = "\n".equals(lastStr) ? "" : "\n";
        content = content + String.format("%s=%s\n", key, value);
        return FileUtils.append(file, content.getBytes());
    }

    private PropertiesIO() {}
}
