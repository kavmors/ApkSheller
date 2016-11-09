package jcommon.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;


/**
 * Manifest解析库
 */
public class ManifestUtils {
    private File f;
    private Document document;
    private Element root;

    public static ManifestUtils load(String path) {
        return load(new File(path));
    }

    public static ManifestUtils load(File file) {
        try {
            return new ManifestUtils(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ManifestUtils(File file) throws DocumentException {
        f = file;
        document = new SAXReader().read(file);
        root = document.getRootElement();
    }

    /**
     * 获取根元素
     * @return Element对象
     */
    public Element getRoot() {
        return root;
    }

    /**
     * 获取包名
     * @return 包名
     */
    public String getPackageName() {
        return root.attributeValue("package");
    }

    /**
     * 设置版本号
     * @param versionCode 版本代号,null表示删除原有的versionCode
     */
    public void setVersionCode(String versionCode) {
        Attribute attr = root.attribute("versionCode");
        if (attr != null) {
            attr.detach();
        }
        if (versionCode != null) {
            root.addAttribute("android:versionCode", versionCode);
        }
    }

    /**
     * 设置版本名称
     * @param versionName 版本名称,null表示删除原有的versionName
     */
    public void setVersionName(String versionName) {
        Attribute attr = root.attribute("versionName");
        if (attr != null) {
            attr.detach();
        }
        if (versionName != null) {
            root.addAttribute("android:versionName", versionName);
        }
    }

    /**
     * 获取版本号
     * @return 版本代号
     */
    public String getVersionCode() {
        String code = root.attributeValue("versionCode");
        return code == null ? "1" : code;
    }

    /**
     * 获取版本名称
     * @return 版本名称
     */
    public String getVersionName() {
        String name = root.attributeValue("versionName");
        return name == null ? "1.0" : name;
    }

    /**
     * 获取uses-permission列表
     * @return 权限列表
     */
    public List<String> getPermission() {
        List<Element> p = root.elements("uses-permission");
        List<String> permissions = new ArrayList<String>(p.size());
        Iterator<Element> i = p.iterator();
        while (i.hasNext()) {
            permissions.add(i.next().attributeValue("name"));
        }
        return permissions;
    }

    /**
     * 以parent为父元素添加meta-data标签(标签已存在则覆盖)
     * @param parent 父元素
     * @param name android:name
     * @param value android:value,null表示删除改meta-data元素
     */
    public void addMetadata(Element parent, String name, String value) {
        List<Element> m = parent.elements("meta-data");
        Iterator<Element> i = m.iterator();
        while (i.hasNext()) {
            Element element = i.next();
            if (name.equals(element.attributeValue("name"))) {
                if (value != null) {
                    element.attribute("value").detach();
                    element.addAttribute("android:value", value);
                } else {
                    element.detach();
                }
                return;
            }
        }
        if (name != null && value != null) {
            parent.addElement("meta-data").addAttribute("android:name", name).addAttribute("android:value", value);
        }
    }

    /**
     * 获取meta-data集合
     * @param parent 父元素(application,activity,service,receiver)
     * @return
     */
    public Map<String, String> getMetadatas(Element parent) {
        List<Element> m = parent.elements("meta-data");
        Map<String, String> metadatas = new HashMap<String, String>(m.size());
        Iterator<Element> i = m.iterator();
        while (i.hasNext()) {
            Element element = i.next();
            metadatas.put(element.attributeValue("name"), element.attributeValue("value"));
        }
        return metadatas;
    }

    /**
     * 根据名称获取meta-data值
     * @param parent 父元素(application,activity,service,receiver)
     * @param name 名称
     * @return
     */
    public String getMetadata(Element parent, String name) {
        List<Element> m = parent.elements("meta-data");
        Iterator<Element> i = m.iterator();
        while (i.hasNext()) {
            Element element = i.next();
            if (name.equals(element.attributeValue("name"))) {
                return element.attributeValue("value");
            }
        }
        return null;
    }

    /**
     * 设置application android:name属性
     * @param name 属性值,null表示删除原有的name属性
     */
    public void setApplication(String name) {
        Element application = getApplication();
        Attribute attr = application.attribute("name");
        if (attr != null) {
            attr.detach();
        }
        if (name != null) {
            application.addAttribute("android:name", name);
        }
    }

    /**
     * 获取application元素
     * @return 元素对象
     */
    public Element getApplication() {
        return root.element("application");
    }

    /**
     * 获取activity元素集合
     * @return 元素列表
     */
    public List<Element> getActivities() {
        return getApplicationElements("activity");
    }

    /**
     * 获取activity元素
     * @param name activity的android:name属性
     * @return 元素对象
     */
    public Element getActivity(String name) {
        return getApplicationElement("activity", "name", name);
    }

    /**
     * 删除一个Activity
     * @param name activity的android:name属性
     * @return
     */
    public boolean removeActivity(String name) {
        Element a = getActivity(name);
        if (a != null) {
            a.detach();
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取主Activity
     * @return Activity元素
     */
    public Element getMainActivity() {
        List<Element> activities = getActivities();
        Iterator<Element> i = activities.iterator();
        while (i.hasNext()) {
            Element element = i.next();
            Element intentFilter = element.element("intent-filter");
            if (intentFilter != null) {
                Element action = intentFilter.element("action");
                Element category = intentFilter.element("category");
                if (action != null && "android.intent.action.MAIN".equals(action.attributeValue("name"))
                        && category != null && "android.intent.category.LAUNCHER".equals(category.attributeValue("name"))) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * 设置主Activity
     * @param activity 需要设置为主入口的activity对象,null表示删除原有的主activity
     * @return
     */
    public void setMainActivity(Element activity) {
        Element mainActivity = getMainActivity();
        Element intentFilter;
        if (mainActivity != null) {
            intentFilter = mainActivity.element("intent-filter");
            intentFilter.detach();
            if (activity != null) {
                activity.add(intentFilter);
            }
        } else {
            if (activity != null) {
                intentFilter = activity.addElement("intent-filter");
                Element action = intentFilter.addElement("action");
                Element category = intentFilter.addElement("category");
                action.addAttribute("android:name", "android.intent.action.MAIN");
                category.addAttribute("android:name", "android.intent.category.LAUNCHER");
            }
        }
    }

    /**
     * 获取service元素集合
     * @return 元素列表
     */
    public List<Element> getServices() {
        return getApplicationElements("service");
    }

    /**
     * 获取service元素
     * @param name 名称
     * @return 元素对象
     */
    public Element getService(String name) {
        return getApplicationElement("service", "name", name);
    }

    /**
     * 获取receiver元素集合
     * @return 元素列表
     */
    public List<Element> getReceivers() {
        return getApplicationElements("receiver");
    }

    /**
     * 获取receiver元素
     * @param name 名称
     * @return 元素对象
     */
    public Element getReceiver(String name) {
        return getApplicationElement("receiver", "name", name);
    }

    /**
     * 获取application内的元素
     * @param tag 元素标签
     * @return 元素集合
     */
    public List<Element> getApplicationElements(String tag) {
        return getApplication().elements(tag);
    }

    /**
     * 根据属性获取application内的元素
     * @param tag 元素标签
     * @param attribute 属性
     * @param name 属性值
     * @return 元素对象
     */
    public Element getApplicationElement(String tag, String attribute, String value) {
        value = value.replace("android:", "");
        List<Element> list = getApplicationElements(tag);
        Iterator<Element> i = list.iterator();
        while (i.hasNext()) {
            Element element = i.next();
            if (value.equals(element.attributeValue(attribute))) {
                return element;
            }
        }
        return null;
    }

    /**
     * 把Manifest修改写入文件
     * @return 写入成功则为true
     */
    public boolean commit() {
        OutputFormat outformat = new OutputFormat();
        outformat.setEncoding("UTF-8");
        outformat.setNewlines(true);
        outformat.setIndent(true);
        outformat.setTrimText(true);
        FileOutputStream out = null;
        XMLWriter xmlwriter = null;
        try {
            out = new FileOutputStream(f);
            xmlwriter = new XMLWriter(out, outformat);
            xmlwriter.write(document);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (xmlwriter != null) {
                try {
                    xmlwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
