package jcommon.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    private static final String separator = File.separator;

    /**
     * 用于选择zip包文件路径
     */
    public interface ZipFilter {
        /**
         * 选择zip包中文件的路径
         * @param path 文件相对路径
         * @return true则表示接受该路径
         */
        boolean accept(String path);
    }

    /**
     * 压缩文件(或文件夹)
     * @param srcfile 需要压缩的文件列表
     * @param zipfile 压缩后的zip文件
     * @return
     */
    public static boolean zip(File[] srcfile, File zipfile) {
        FileUtils.createFile(zipfile);
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipfile));
            for (int i = 0; i < srcfile.length; i++) {
                if (srcfile[i].isDirectory()) {
                    addFolder(out, srcfile[i], "");
                } else if (srcfile[i].isFile()) {
                    addFile(out, srcfile[i], "");
                }
            }
            return true;
        } catch (IOException e) {
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
        }
    }

    /**
     * 压缩一个目录下的所有文件(不包含该目录的路径)
     * @param folder 需要压缩的目录
     * @param zipfile 压缩后的zip文件
     * @return
     */
    public static boolean zipFolder(File folder, File zipfile) {
        try {
            if (!folder.isDirectory()) {
                throw new IOException("folder is not a directory");
            }
            return zip(folder.listFiles(), zipfile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 解压缩
     * @param zipfile 需要解压缩的zip包
     * @param descDir 解压后的目标目录
     */
    public static boolean unzip(File zipfile, File descDir) {
        InputStream in = null;
        OutputStream out = null;
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipfile);
            if (!descDir.isDirectory()) {
                descDir.mkdirs();
            }
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String zipEntryName = entry.getName();
                in = zf.getInputStream(entry);
                File outFile = new File(descDir.getAbsolutePath() + File.separator + zipEntryName);
                if (!outFile.exists()) {
                    FileUtils.createFile(outFile);
                }
                out = new FileOutputStream(outFile);
                byte[] buf1 = new byte[1024];
                int len;
                while ((len = in.read(buf1)) > 0) {
                    out.write(buf1, 0, len);
                }
                in.close();
                out.close();
            }
            in = null;
            out = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (zf != null) {
                try {
                    zf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取zip包文件
     * @parma zip 需要读取文件的zip包
     * @param path 文件在zip包的相对路径
     * @return 文件字节内容
     */
    public static byte[] read(File zip, String path) {
        path = path.replace('\\', '/');
        ByteArrayOutputStream bos = null;
        ZipInputStream zis = null;
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            bos = new ByteArrayOutputStream();
            fis = new FileInputStream(zip);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (entry.getName().equals(path)) {
                    int i;
                    byte[] buffer = new byte[1024];
                    while ((i = zis.read(buffer)) != -1) {
                        bos.write(buffer, 0, i);
                    }
                }
                zis.closeEntry();
            }
            byte[] ret = bos.toByteArray();
            if (ret.length == 0) {
                throw new IOException("file not exists in zip.");
            } else {
                return ret;
            }
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
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从zip包提取文件
     * @param zip 需要提取文件的zip包
     * @param path 文件在zip包的相对路径
     * @param target 目标文件
     * @return
     */
    public static boolean extractFile(File zip, String path, File target) {
        path = path.replace('\\', '/');
        FileOutputStream fos = null;
        ZipInputStream zis = null;
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        boolean searched = false;
        try {
            fis = new FileInputStream(zip);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (entry.getName().equals(path)) {
                    FileUtils.createFile(target);
                    fos = new FileOutputStream(target);
                    searched = true;
                    int i;
                    byte[] buffer = new byte[1024];
                    while ((i = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, i);
                    }
                    fos.close();
                    fos = null;
                }
                zis.closeEntry();
            }
            if (!searched) {
                throw new IOException("file not exists in zip.");
            } else {
                return true;
            }
        } catch (IOException e) {
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
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zis != null) {
                try {
                    zis.close();
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
     * 从zip包提取多个文件
     * @param zip 需要提取文件的zip包
     * @param path 要提取的多个文件在zip包的相对路径
     * @param outputDir 目标目录
     * @return
     */
    public static boolean extract(File zip, final String[] paths, File outputDir) {
        return extract(zip, new ZipFilter() {
            @Override
            public boolean accept(String path) {
                for (String p : paths) {
                    if (path.equals(p.replace('\\', '/'))) {
                        return true;
                    }
                }
                return false;
            }
        }, outputDir);
    }

    /**
     * 从zip包提取符合要求的文件
     * @param zip 需要提取文件的zip包
     * @param regex 正则表达式,用于匹配相对路径
     * @param dest 目标目录
     * @return
     */
    public static boolean extract(File zip, final String regex, File outputDir) {
        return extract(zip, new ZipFilter() {
            @Override
            public boolean accept(String path) {
                return "".equals(regex) || regex == null || path.matches(regex);
            }
        }, outputDir);
    }

    /**
     * 从zip包提取单个或多个文件
     * @param zip 需要提取文件的zip包
     * @param filter 根据相对路径选择文件
     * @param outputDir 目标目录
     * @return
     */
    public static boolean extract(File zip, ZipFilter filter, File outputDir) {
        outputDir.mkdirs();
        FileOutputStream fos = null;
        ZipInputStream zis = null;
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        boolean searched = false;
        try {
            fis = new FileInputStream(zip);
            bis = new BufferedInputStream(fis);
            zis = new ZipInputStream(bis);
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (filter == null || filter.accept(entry.getName())) {
                    File f = new File(outputDir, entry.getName());
                    FileUtils.createFile(f);
                    fos = new FileOutputStream(f);
                    searched = true;
                    int i;
                    byte[] buffer = new byte[1024];
                    while ((i = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, i);
                    }
                    fos.close();
                    fos = null;
                }
                zis.closeEntry();
            }
            if (!searched) {
                throw new IOException("file not exists in zip.");
            } else {
                return true;
            }
        } catch (IOException e) {
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
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (zis != null) {
                try {
                    zis.close();
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


    //zipoutput添加文件
    private static void addFile(ZipOutputStream out, File file, String prefix) throws IOException {
        FileInputStream in = new FileInputStream(file);
        out.putNextEntry(new ZipEntry(prefix + file.getName()));
        int len;
        byte[] buf = new byte[1024];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.closeEntry();
        in.close();
    }

    //zipoutput添加文件夹
    private static void addFolder(ZipOutputStream out, File folder, String prefix) throws IOException {
        File[] subfiles = folder.listFiles();
        prefix += folder.getName();
        for (File subfile : subfiles) {
            if (subfile.isDirectory()) {
                addFolder(out, subfile, prefix+separator);
            } else if (subfile.isFile()) {
                addFile(out, subfile, prefix+separator);
            }
        }
    }

    private ZipUtils() {}
}
