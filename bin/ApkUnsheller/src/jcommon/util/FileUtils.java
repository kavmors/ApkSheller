package jcommon.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FileUtils {
    public static final int TYPE_NULL = 0;
    public static final int TYPE_DIR = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_BOTH = 3;

    public static final char UNIT_BYTE = 'B';
    public static final char UNIT_KB = 'K';
    public static final char UNIT_MB = 'M';
    public static final char UNIT_GB = 'G';
    public static final char UNIT_TB = 'T';

    /**
    * 判断文件是否存在
    * @param f 文件对象
    * @return boolean 文件不存在时返回false
    */
    public static boolean exists(File f) {
        return f != null && f.exists();
    }

    /**
    * 判断该文件为目录或文件类型
    * @param f 文件对象
    * @return 类型为文件则{@link #TYPE_FILE},为目录则{@link #TYPE_DIR},文件不存在返回{@link #TYPE_NULL}
    */
    public static int dirOrFile(File f) {
        if (f.isDirectory()) {
            return TYPE_DIR;
        }
        if (f.isFile()) {
            return TYPE_FILE;
        }
        return 0;
    }

    /**
    * 创建空目录
    * @param dir 目录对象
    * @return 创建成功则为true
    */
    public static boolean createDir(File dir) {
        return !exists(dir) && dir.mkdirs();
    }

    /**
    * 创建文件
    * @param f 文件对象
    * @return 创建成功则为true
    */
    public static boolean createFile(File f) {
        if (exists(f)) {
            return false;
        }
        createDir(getParent(f));
        try {
            return f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
    * 删除文件(或文件夹)
    * @param f 文件对象
    * @return 删除成功则为true
    */
    public static boolean delete(File f) {
        int type = dirOrFile(f);
        if (type == TYPE_FILE) {
            return f.delete();
        } else if (type == TYPE_DIR) {
            for (File sub : f.listFiles()) {
                delete(sub);
            }
            return f.delete();
        } else {
            return false;
        }
    }

    /**
    * 重命名文件
    *    (只改变路径下的文件名,不改变目录路径,需要修改路径参考{@link #move}方式)
    * @param f 文件
    * @param name 新文件名
    * @return 重命名成功则为true
    */
    public static boolean rename(File f, String name) {
        if (!exists(f)) {
            return false;
        }
        return f.renameTo(new File(getParent(f), name));
    }

    /**
    * 复制文件(或文件夹)
    * @param source 源文件对象
    * @param destination 目标文件对象
    * @param overwrite 是否覆盖已有文件
    * @return 复制成功则为true
    */
    public static boolean copy(File source, File destination, boolean overwrite) {
        try {
            checkSame(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int type = dirOrFile(source);

        if (type == 0) {
            return false;
        }

        if (exists(destination)) {
            if (overwrite) {
                delete(destination);
            } else {
                return false;
            }
        }

        if (type == TYPE_FILE) {
            createDir(getParent(destination));
            return fileCopy(source, destination, 0, source.length());
        } else if (type == TYPE_DIR) {
            createDir(destination);
            for (File sub : source.listFiles()) {
                copy(sub, new File(destination, sub.getName()), overwrite);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
    * 移动文件(或文件夹)
    * @param source 源文件对象
    * @param destination 目标文件对象
    * @param overwrite 是否覆盖已有文件
    * @return 复制成功则为true
    */
    public static boolean move(File source, File destination, boolean overwrite) {
        try {
            checkSame(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int type = dirOrFile(source);

        if (type == 0) {
            return false;
        }

        if (exists(destination)) {
            if (overwrite) {
                delete(destination);
            } else {
                return false;
            }
        }

        if (type == TYPE_FILE) {
            createDir(getParent(destination));
            return source.renameTo(destination);
        } else if (type == TYPE_DIR) {
            createDir(destination);
            for (File sub : source.listFiles()) {
                move(sub, new File(destination, sub.getName()), overwrite);
            }
            return source.delete();
        } else {
            return false;
        }
    }

    /**
    * 合并两个目录(源目录将被删除)
    * @param source 源目录对象
    * @param destination 目标目录对象
    * @param overwrite 是否覆盖同目录下的已有文件
    * @return 合并成功则为true
    */
    public static boolean merge(File source, File destination, boolean overwrite) {
        try {
            checkSame(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        int srcType = dirOrFile(source);
        int dtnType = dirOrFile(destination);
        if (srcType == 0) {
            return false;
        }
        if (srcType == TYPE_DIR && dtnType == 0) {
            return move(source, destination, true);
        }
        if (srcType != TYPE_DIR || dtnType != TYPE_DIR) {
            return false;
        }

        for (File sub : source.listFiles()) {
            if (sub.isDirectory()) {
                merge(sub, new File(destination, sub.getName()), overwrite);
            } else if (sub.isFile()) {
                if (!move(sub, new File(destination, sub.getName()), overwrite)) {
                    delete(sub);
                }
            }
        }
        source.delete();
        return true;
    }

    /**
    * 对于needle文件夹中文件,若haystack中有相同文件(相对路径相同),则在haystack中删除
    * @param haystack
    * @param needle
    * @return 有文件被删除则返回true
    */
    public static boolean diff(File haystack, File needle) {
        boolean searched = false;
        for (File f : needle.listFiles()) {
            if (f.isFile()) {
                searched = searched || new File(f.getPath().replace(needle.getPath(), haystack.getPath())).delete();
            } else if (f.isDirectory()) {
                diff(new File(f.getPath().replace(needle.getPath(), haystack.getPath())), f);
            }
        }

        return searched;
    }

    /**
    * 获取文件后缀
    * @param f 文件对象
    * @return 后缀名
    */
    public static String getExtension(File f) {
        if (!exists(f)) {
            return null;
        }

        int extensionPosition = f.getName().lastIndexOf(".");
        int separatorPosition = f.getName().lastIndexOf(File.separator);
        if (extensionPosition == -1) {
            return "";
        }
        return (separatorPosition >= extensionPosition) ? "" : f.getName().substring(extensionPosition + 1);
    }

    /**
     * 获取文件名(无后缀名)
     * @param f 文件对象
     * @return 文件名
     */
    public static String getFileNameWithoutExtension(File f) {
        String path = f.getName();
        int exten = path.lastIndexOf(".");
        return exten == -1 ? path : path.substring(0, exten);
    }

    /**
    * 获取文件大小,以字节为单位
    * @param f 文件对象
    * @return 文件大小,单位字节
    */
    public static long getSize(File f) {
        return (long)getSize(f, UNIT_BYTE);
    }

    /**
    * 获取文件大小,可选单位
    * @param f 文件对象
    * @param unit 单位,可选{@link #UNIT_BYTE}、{@link #UNIT_KB}、{@link #UNIT_MB}、{@link #UNIT_GB}、{@link #UNIT_TB}
    * @return 文件大小
    */
    public static double getSize(File f, int unit) {
        double size = 0.0;

        int type = dirOrFile(f);
        if (type == TYPE_FILE) {
            size = f.length();
        } else if (type == TYPE_DIR) {
            for (File sub : f.listFiles()) {
                size += getSize(sub, UNIT_BYTE);
            }
        } else {
            return 0.0;
        }

        switch (unit) {
            case UNIT_BYTE: break;
            case UNIT_KB: size = size / 1024.0; break;
            case UNIT_MB: size = size / 1024.0 / 1024.0; break;
            case UNIT_GB: size = size / 1024.0 / 1024.0 / 1024.0; break;
            case UNIT_TB: size = size / 1024.0 / 1024.0 / 1024.0 / 1024.0; break;
            default: break;
        }
        return size;
    }

    /**
     * 列出目录下文件(不遍历子目录)
     * @param dir 目录对象
     * @param type 限定子文件类型,可选文件{@link #TYPE_FILE}、目录{@link #TYPE_DIR}或不限定{@link #TYPE_BOTH}
     * @return 子文件集合
     */
    public static File[] listDir(File dir, int type) {
        final boolean includeFile = (type & 2) ==  2;
        final boolean includeDir = (type & 1) == 1;
        return list(dir, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile() && includeFile) {
                    return true;
                }
                if (pathname.isDirectory() && includeDir) {
                    return true;
                }
                return false;
            }
        }, false);
    }

    /**
     * 列出目录下所有文件(包含子目录下的文件)
     * @param dir 目录对象
     * @return 子文件对象
     */
    public static File[] listChildren(File dir) {
        return list(dir, null, true);
    }

    /**
     * 列出符合要求的子文件(可包含子目录下的文件)
     * @param dir 目录对象
     * @param regex 正则表达式,用于匹配相对于指定目录的路径.
     *      如/.*\.txt/可匹配所有txt文件
     * @return 匹配到的文件集合
     */
    public static File[] list(File dir, final String regex) {
        final String dirpath = dir.getAbsolutePath() + File.separator;
        return list(dir, new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getAbsolutePath().replace(dirpath, "");
                return "".equals(regex) || regex == null || name.matches(regex);
            }
        }, true);
    }

    /**
     * 选择性遍历目录下所有文件
     * @param dir 目录对象
     * @param filter 文件过滤器
     * @param recursive 是否遍历子文件夹
     * @return 遍历的文件集合
     */
    public static File[] list(File dir, FileFilter filter, boolean recursive) {
      int type = dirOrFile(dir);
      if (type != TYPE_DIR) {
          return null;
      }
      if (!recursive) {
          return dir.listFiles(filter);
      } else {
          ArrayList<File> fileArr = new ArrayList<File>();
          for (File f : dir.listFiles()) {
              if (f.isFile() && (filter == null || filter.accept(f))) {
                  fileArr.add(f);
              } else if (f.isDirectory()) {
                  fileArr.addAll(Arrays.asList(list(f, filter, true)));
              }
          }
          return fileArr.toArray(new File[fileArr.size()]);
      }
    }

    /**
     * 获取父目录的绝对路径
     * @param f 文件对象
     * @return 父目录对象
     */
    public static File getParent(File f) {
        String path = f.getAbsolutePath();
        int separator = path.lastIndexOf(File.separator);
        return separator == -1 ? new File(path) : new File(path.substring(0, separator));
    }

    /**
    * 从源文件中提取内容到目标文件
    * @param source 源文件
    * @param destination 目标文件
    * @return 复制内容成功则返回true
    */
    public static boolean extract(File source, File destination, long offset, long length) {
        try {
            checkSame(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return fileCopy(source, destination, offset, length);
    }

    /**
    * 写入文件
    * @param f 文件对象
    * @param data 写入内容
    * @return 写入成功则为true
    */
    public static boolean write(File f, byte[] data) {
        return fileWrite(f, data, false);
    }

    /**
    * 读取文件内容
    * @param f 文件对象
    * @return 字节内容
    */
    public static byte[] read(File f) {
        return read(f, 0, getSize(f));
    }

    /**
    * 读取文件内容
    * @param f 文件对象
    * @param offset 跳过的字节数
    * @param length 读取字节数
    * @return 字节内容
    */
    public static byte[] read(File f, long offset, long length) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            fis.skip(offset);
            byte[] b = new byte[(int) length];
            fis.read(b);
            return b;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
    * 以添加内容的形式写入文件
    * @param f 文件对象
    * @param data 写入内容
    * @return 写入成功则为true
    */
    public static boolean append(File f, byte[] data) {
        return fileWrite(f, data, true);
    }

    /**
    * 加密文件
    * @param source 源文件对象
    * @param destination 目标文件对象
    * @param key 加密密钥
    * @return 加密成功则为true
    */
    public static boolean encrypt(File source, File destination, String key) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            checkSame(source, destination);
            createFile(destination);

            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            return performCrypt(fis.getChannel(), fos.getChannel(), getCipher(key, Cipher.ENCRYPT_MODE));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
    * 解密文件
    * @param source 源文件对象
    * @param destination 目标文件对象
    * @param key 解密密钥
    * @return 解密成功则为true
    */
    public static boolean decrypt(File source, File destination, String key) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            checkSame(source, destination);
            createFile(destination);

            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            return performCrypt(fis.getChannel(), fos.getChannel(), getCipher(key, Cipher.DECRYPT_MODE));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 计算文件md5值(小写方式)
     * @param f 文件对象
     * @return md5字符串
     */
    public static String md5(File f) {
        return md5(f, false);
    }

    /**
     * 计算文件md5值(小写方式)
     * @param f 文件对象
     * @param brief true则以16位表示方式返回结果
     * @return md5字符串
     */
    public static String md5(File f, boolean brief) {
        String md5 = hash(f, "MD5");
        return brief ? md5.substring(8, 16) : md5;
    }

    /**
     * 计算文件sha1值(小写方式)
     * @param f 文件对象
     * @return sha1字符串
     */
    public static String sha1(File f) {
        return hash(f, "SHA1");
    }

    /**
     * 检查两个文件是否相同,或两个目录是否有相同内容
     * @param f1
     * @param f2
     * @return 相同则返回true
     */
    public static boolean isSame(File f1, File f2) {
        int t1 = dirOrFile(f1);
        int t2 = dirOrFile(f2);

        if (t1 != t2) {
            return false;
        }
        if (f1.equals(f2)) {
            return true;
        }

        switch (t1) {
            case TYPE_FILE:     //md5 && sha1
                return md5(f1).equals(md5(f2)) && sha1(f1).equals(sha1(f2));
            case TYPE_DIR:      //子文件相对路径 && md5 && sha1
                File[] f1List = listChildren(f1);
                File[] f2List = listChildren(f2);
                Map<String, File> f2Map = new HashMap<String, File>(f2List.length);
                for (File f : f2List) {
                    f2Map.put(f.getAbsolutePath(), f);
                }
                for (File f1File : f1List) {
                    String relativePath = f1File.getAbsolutePath().replace(f1.getAbsolutePath(), f2.getAbsolutePath());
                    File f2File = f2Map.get(relativePath);
                    if (f2File == null) {
                        return false;
                    }
                    if (!md5(f1File).equals(md5(f2File)) || !sha1(f1File).equals(sha1(f2File))) {
                        return false;
                    }
                    f2Map.remove(relativePath);
                }
                return f2Map.size() == 0 ? true : false;
        }
        return false;
    }

    /**
     * 判断目录是否为空(没有子文件且所有子文件夹都没有文件)
     * @param dir 目录对象
     * @return 目录为空则返回true
     */
    public static boolean empty(File dir) {
        File[] fs = listChildren(dir);
        return fs == null || fs.length == 0;
    }

    private static boolean fileWrite(File f, byte[] data, boolean append) {
        FileOutputStream fos = null;
        try {
            createFile(f);
            fos = new FileOutputStream(f, append);
            fos.write(data);
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean fileCopy(File source, File destination, long offset, long length) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel s = null;
        FileChannel d = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            s = fis.getChannel();
            d = fos.getChannel();
            s.transferTo(offset, length, d);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                d.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Cipher getCipher(String key, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        MessageDigest md = MessageDigest.getInstance("md5");
        byte[] digestOfPassword = md.digest(key.getBytes());
        byte[] keyBytes = new byte[24];
        System.arraycopy(digestOfPassword, 0, keyBytes, 0, Math.min(digestOfPassword.length, 24));
        for (int j = 0, k = 16; j < 8;) {
            keyBytes[k++] = keyBytes[j++];
        }

        SecretKey secret = new SecretKeySpec(keyBytes, "DESede");
        IvParameterSpec iv = new IvParameterSpec(new byte[8]);
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(mode, secret, iv);


//        KeyGenerator kgen = KeyGenerator.getInstance("AES");
//        kgen.init(128, new SecureRandom(key.getBytes()));
//        SecretKey secretKey = kgen.generateKey();
//        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
//        cipher.init(mode, secretKey);

        return cipher;
    }

    private static boolean performCrypt(FileChannel fis, FileChannel fos, Cipher cipher) {
        try {
            ByteBuffer byteData = ByteBuffer.allocate(1024);
            while (fis.read(byteData) != -1) {
                byteData.flip();

                byte[] byteList = new byte[1024];
                int remain = byteData.remaining();
                byteData.get(byteList, 0, remain);
                for (;remain < 1024; remain++) {
                    byteList[remain] = -1;
                }

                byte[] bytes = cipher.doFinal(byteList);
                fos.write(ByteBuffer.wrap(bytes));
                byteData.clear();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //计算文件hash
    private static String hash(File f, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(read(f));
            byte[] b = digest.digest();
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                int val = (b[i]) & 0xff;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    //检查源文件与目标文件是否相同,相同则抛出异常
    private static void checkSame(File source, File destination) throws IOException {
        if (source.equals(destination)) {
            throw new IOException("Source is the same as destination");
        }
    }

    private FileUtils() {}
}
