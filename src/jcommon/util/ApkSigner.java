package jcommon.util;

import jcommon.util.Cmd;

/**
 * Apk签名
 */
public class ApkSigner {
    /**
     * 签名apk
     * @param source
     * @param keystore
     * @param storepass
     * @param alias
     * @param aliaspass
     * @return 目标apk路径
     */
    public static String sign(String source, String keystore, String storepass, String alias, String keypass) {
        String destination = source.substring(0, source.lastIndexOf("."))+"_signed.apk";
        return sign(source, keystore, storepass, alias, keypass, destination);
    }

    /**
     * 签名apk
     * @param source
     * @param keystore
     * @param storepass
     * @param alias
     * @param aliaspass
     * @param destination 签名后apk的路径
     * @return 目标apk路径
     */
    public static String sign(String source, String keystore, String storepass, String alias, String keypass, String destination) {
        String cmd = "jarsigner -keystore %s -storepass %s -keypass %s -digestalg SHA1 -sigalg MD5withRSA -signedjar %s %s %s";
        cmd = String.format(cmd, keystore, storepass, keypass, destination, source, alias);
        Cmd.exec(cmd);
        return destination;
    }

    /**
     * Runnable jar
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 5) {
            sign(args[0], args[1], args[2], args[3], args[4]);
        } else {
            sign(args[0], args[1], args[2], args[3], args[4], args[5]);
        }
    }

    private ApkSigner() {}
}
