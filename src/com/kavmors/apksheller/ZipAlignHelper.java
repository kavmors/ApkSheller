package com.kavmors.apksheller;

import jcommon.util.Cmd;

public class ZipAlignHelper {
    public static String align(String exe, String apkPath) {
        String destination = apkPath.substring(0, apkPath.lastIndexOf("."))+"_aligned.apk";
        String cmd = exe + " -f 4 " + apkPath + " " + destination;
        Cmd.exec(cmd);
        return destination;
    }
}
