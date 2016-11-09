package com.kavmors.apksheller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import jcommon.util.ManifestUtils;

public class Common {
    public static final int SECRET_LEN = 16;
    public static final int DEXNAME_LEN = 32;

    public static final String MAIN_APPLICATION = "SHELL_MAINAPPLICATION";
    public static final String VERSION = "SHELL_VERSION";

    //ProxyApplication包名路径
    public static String getProxyApplication(File manifest) {
        ManifestUtils m = ManifestUtils.load(manifest);
        return m.getApplication().attributeValue("name");
    }

    //public.xml排重
    public static boolean updatePublicXml(File f) {
        List<String> nameList = new ArrayList<String>();
        List<Element> nodeList = new ArrayList<Element>();

        SAXReader reader = new SAXReader();
        Document document;
        try {
            document = reader.read(f);
            // 获取根节点元素对象
            Element node = document.getRootElement();
            Iterator<Element> itor = node.elementIterator();
            while (itor.hasNext()) {
                Element publicNode = itor.next();
                String name = publicNode.attribute(1).getValue();
                if (nameList.contains(name)) {
                    nodeList.add(publicNode);
                } else {
                    nameList.add(name);
                }
            }
            for (int i = 0; i < nodeList.size(); i++) {
                nodeList.get(i).detach();
            }
            if (nodeList.size() > 0) {
                OutputFormat outformat = new OutputFormat();
                outformat.setEncoding("UTF-8");
                outformat.setNewlines(true);
                outformat.setIndent(true);
                outformat.setTrimText(true);
                FileOutputStream out = new FileOutputStream(f);
                XMLWriter xmlwriter = new XMLWriter(out, outformat);
                xmlwriter.write(document);

                out.close();
                xmlwriter.close();
            }
        } catch (DocumentException e) {
            e.printStackTrace();
            System.out.println("读取渠道参数文件失败");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("重写public.xml失败");
            return false;
        } catch (Exception e) {
            System.out.println("重写public.xml失败");
            return false;
        }

        return true;
    }

    //随机字符串
    public static String randomStr(int length) {
        String list = "abcdefghijklmnopqrstuvwxyz1234567890";
        int len = list.length();
        StringBuffer buffer = new StringBuffer();
        while (length-- > 0) {
            int p = new Random().nextInt(len);
            buffer.append(list.charAt(p));
        }
        return buffer.toString();
    }

    //混淆配置选项
    public static String getProguardConfig(String application) {
    	String config = "-dontwarn %s\n-keep class %s{*;}\n";
    	return String.format(config, application, application);
    }
}
