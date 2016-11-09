package jcommon.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 命令行辅助类
 */
public class Cmd {
    public static void main(String[] args) {
        exec(args[0]);
    }

    /**
     * 执行cmd,并打印执行结果
     * @param cmd 命令
     * @return 执行成功则为true
     */
	public static boolean exec(String cmd) {
	    try {
            String ret = receiveInfo(createProcess(cmd, null), true);
            if (ret == null) {
                throw new IOException();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(cmd + ", 执行失败");
            return false;
        }
	}

	/**
     * 带起始位置执行cmd,并打印执行结果
     * @param cmd 命令
     * @param baseDir 起始目录对象
     * @return 执行成功则为true
     */
	public static boolean exec(String cmd, File baseDir) {
	    try {
            String ret = receiveInfo(createProcess(cmd, baseDir), true);
            if (ret == null) {
                throw new IOException();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(cmd + ", 执行失败");
            return false;
        }
	}

	/**
     * 执行cmd,并获取执行结果
     * @param cmd 命令
     * @return 执行结果
     */
    public static String execForResult(String cmd) {
        try {
            String ret = receiveInfo(createProcess(cmd, null), false);
            if (ret == null) {
                throw new IOException();
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(cmd + ", 执行失败");
            return null;
        }
    }

    /**
     * 带起始位置执行cmd,并获取执行结果
     * @param cmd 命令
     * @param baseDir 起始目录对象
     * @return 执行结果
     */
    public static String execForResult(String cmd, File baseDir) {
        try {
            String ret = receiveInfo(createProcess(cmd, baseDir), false);
            if (ret == null) {
                throw new IOException();
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(cmd + ", 执行失败");
            return null;
        }
    }

    /**
     * 判断当前操作系统是否为Windows
     * @return Windows则为true
     */
    public static boolean isWindowsOS() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().indexOf("windows") > -1) {
            return true;
        }
        return false;
    }

    private static Process createProcess(String cmd, File base) throws IOException {
        String[] cmds = cmd.split(" ");
        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.redirectErrorStream(true);
        if (base != null) {
            builder.directory(base);
        }
        return builder.start();
    }

    //处理cmd执行结果
    private static String receiveInfo(Process p, boolean shouldPrint) {
        InputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        String line = null;
        StringBuffer ret = new StringBuffer("");
        try {
            fis = p.getInputStream();
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);

            while ((line = br.readLine()) != null) {
                ret.append(line).append("\n");
                if (shouldPrint) {
                    System.out.println(line);
                }
            }
            return 0 == p.waitFor() ? ret.toString() : null;
        } catch (Exception e) {
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
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Cmd() {}
}
