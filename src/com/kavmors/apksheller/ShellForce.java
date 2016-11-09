package com.kavmors.apksheller;

import java.io.File;

import jcommon.util.DexSigner;
import jcommon.util.FileUtils;
import jcommon.util.ZipUtils;

/**
 * 加壳规则: sheller+dex.zip解密密钥
 */
public class ShellForce {
    /**
     * 加壳apk
     * @param sheller 脱壳apk
     * @param shelledDex 目标dex
     * @param secret 解密密钥
     * @return
     */
    public static boolean shell(File sheller, File shelledDex, String secret, String assetName) {
        System.out.println("--secretKey=" + secret);
        System.out.println("--dexName=" + assetName);
        return shell(ZipUtils.read(sheller, "classes.dex"), shelledDex, secret.getBytes(), assetName.getBytes());
    }

    private static boolean shell(byte[] sheller, File shelledDex, byte[] secret, byte[] assetName) {
        try {
            if (secret.length != Common.SECRET_LEN || assetName.length != Common.DEXNAME_LEN) {
                throw new Exception("invalid secret key or dex name");
            }

            if (!shelledDex.exists()) {
                shelledDex.createNewFile();
            }

            int shelledDexSize = sheller.length + Common.SECRET_LEN + Common.DEXNAME_LEN;    //shelledDex = 脱壳dex+16位密钥+32位dex名
            byte[] shelled = new byte[shelledDexSize];

            System.arraycopy(sheller, 0, shelled, 0, sheller.length);       //sheller-->[0, sheller.length)
            System.arraycopy(secret, 0, shelled, sheller.length, secret.length);    //secret-->[sheller.length, sheller.length+secret.length)
            System.arraycopy(assetName, 0, shelled, sheller.length+secret.length, assetName.length);      //dexname-->[sheller.length+secret.length, sheller.length+secret.length+dexname.length)

            //fix header && write file
            DexSigner.fixFileSize(shelled);
            DexSigner.fixSha1Signature(shelled);
            DexSigner.fixChecksum(shelled);
            FileUtils.write(shelledDex, shelled);

            System.out.println("加壳dex: " + shelled.length + " byte");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
