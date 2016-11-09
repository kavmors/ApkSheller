package com.kavmors.apksheller;

import java.io.File;

import jcommon.util.FileUtils;


public class Main {
    /**
     * 命令行:
     * <li>加壳: java -jar ShellerProject.jar [apk] [config...]</li>
     * <li>清理所有临时目录: java -jar ShellerProject.jar -clean</li>
     * <br><br>
     *
     * config选项:
     * <li>-config [config.properties]: 指定配置文件,默认当前目录下的config.properties</li>
     * <li>-keystore [keystore.properties]: 指定签名配置文件,默认不签名</li>
     * <li>-o [target.apk]: 指定加壳后的apk位置</li>
     * <li>-keeptmp: 加壳后保留临时工作区</li>
     */
    public static void main(String[] args) {
        if ("-clean".equals(args[0])) {
            System.out.println("Project clean...");
            System.out.println(ShellerProject.clean() ? "Completed." : "Failed.");
            return;
        }

        File apk = new File(args[0]);
        File config = null;
        File keystore = null;
        File output = new File(FileUtils.getParent(apk), FileUtils.getFileNameWithoutExtension(apk) + "_shelled.apk");
        boolean keepTmp = false;

        for (int i = 0; i < args.length; i++) {
            if (i == 0) {
                continue;
            }
            String arg = args[i];

            if ("-config".equals(arg)) {
                config = new File(args[i+1]);
            } else if ("-keystore".equals(arg)) {
                keystore = new File(args[i+1]);
            } else if ("-o".equals(arg)) {
                output = new File(args[i+1]);
            } else if ("-keeptmp".equals(arg)) {
                keepTmp = true;
            }
        }

        boolean ret = new ShellerProject(keepTmp, output).shell(apk, config, keystore);
        System.out.println(ret ? "Completed." : "Failed.");
    }
}
