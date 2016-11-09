package jcommon.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

/**
 * 修正dex header部分
 */
public class DexSigner {
    /**
     * 计算dex文件大小
     * @param dex
     */
    public static void fixFileSize(byte[] dex) {
        byte[] dexSize = intToByte(dex.length);
        byte[] v = new byte[4];
        //reverse
        for (int i = 0; i < 4; i++) {
            v[i] = dexSize[3 - i];
        }
        System.arraycopy(v, 0, dex, 32, 4);
    }

    /**
     * 计算除sha1和checksum外的sha1签名
     * @param dex
     * @throws NoSuchAlgorithmException
     */
    public static void fixSha1Signature(byte[] dex) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dex, 32, dex.length-32);      //32-End部分sha1
        System.arraycopy(md.digest(), 0, dex, 12, 20);
    }

    /**
     * 计算除checksum的Adler32签名
     * @param dex
     */
    public static void fixChecksum(byte[] dex) {
        Adler32 adler = new Adler32();
        adler.update(dex, 12, dex.length-12);   //12-End部分adler
        byte[] digest = intToByte((int)(adler.getValue()));
        //reverse
        byte[] v = new byte[4];
        for (int i = 0; i < 4; i++) {
            v[i] = digest[3 - i];
        }
        System.arraycopy(v, 0, dex, 8, 4);
    }

    /**
     * 整型转换Hex字符
     * @param n
     * @return
     */
    public static byte[] intToByte(int n) {
        byte[] b = new byte[4];
        for (int i = 3; i >= 0; i--) {
            b[i] = (byte) (n % 256);
            n >>= 8;
        }
        return b;
    }

    private DexSigner() {}
}
