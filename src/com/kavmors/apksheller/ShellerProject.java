package com.kavmors.apksheller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.dom4j.Element;

import jcommon.util.ApkSigner;
import jcommon.util.Cmd;
import jcommon.util.FileUtils;
import jcommon.util.ManifestUtils;
import jcommon.util.PropertiesIO;
import jcommon.util.ZipUtils;

public class ShellerProject {
    private static final File PROJECT_TPL = new File("bin", "ApkUnsheller");
    private static final File BUILD_TPL = new File("bin", "build");

    private static final String APKTOOL = "bin/apktool_2.1.1.jar";

    private File mWorkspace;        //指定临时工作目录
    private File mApkpackage;       //指定apk解压后的目录

    private boolean keepTmp;        //是否保留tmp目录
    private File output;            //输出目录
    private long mStartTime;        //起始时间

    private Map<String, String> mBin = new HashMap<String, String>();
    private Map<String, String> mConfig = new HashMap<String, String>();
    private Map<String, String> mKeystore = new HashMap<String, String>();

    /**
     * 清理所有tmp目录
     */
    public static boolean clean() {
        boolean ret = true;
        System.out.println(System.getProperty("user.dir"));
        for (File f : FileUtils.listDir(new File(System.getProperty("user.dir")), FileUtils.TYPE_DIR)) {
            if (f.getName().startsWith("tmp_")) {
                System.out.println("Cleaning " + f.getAbsolutePath());
                ret = ret && FileUtils.delete(f);
            }
        }
        return ret;
    }

    /**
     * 加壳工程入口
     * @param apk apk文件
     * @param config 配置
     * @param keystore 签名配置
     * @return
     */
    public boolean shell(File apk, File config, File keystore) {
        try {
            if (!apk.isFile() || !"apk".equalsIgnoreCase(FileUtils.getExtension(apk))) {
                projectError("apk文件无效: " + apk.getName());
            }
            String apkName = FileUtils.getFileNameWithoutExtension(apk);
            projectStart(apkName);

            //创工作目录,生成密钥
            if (mWorkspace == null) {
                mWorkspace = createWorkspace();
            }
            String secret = Common.randomStr(Common.SECRET_LEN);
            String assetName = Common.randomStr(Common.DEXNAME_LEN);
            File tmp = new File(mWorkspace, "tmp");
            System.out.println("当前工作区: " + mWorkspace.getName());

            //1. 解压源apk
            File apkPackage = extractApk(apk, new File(tmp, apkName));
            File outputApk = new File(tmp, apkPackage.getName() + "_shelled.apk");

            //2. 读取配置
            if (config == null) {
                config = new File("config.properties");
            }
            setWorkspaceConfig(apkPackage, config, keystore);

            //3. 源apk中提取classes.dex
            extractClasses(apk, tmp);

            //4. 配置工程模板
            orderProject(mWorkspace, apkPackage);

            //5. 打包classes.dex -> assetName.dex
            zipClasses(tmp, secret, assetName);

            //6. gradle aR
            gradleBuild(mWorkspace);

            //7. 清理临时目录/移动打包apk
            File shellerApk = prepareShell(mWorkspace);

            //8. 加壳算法
            File shelledApk = performShell(tmp, shellerApk, secret, assetName);

            FileUtils.rename(shelledApk, outputApk.getName());

            //8. 签名 zipalign
            if (mKeystore != null) {
                FileUtils.move(signAndAlign(outputApk), outputApk, true);
            }

            boolean ret = FileUtils.move(outputApk, this.output, true);
            projectComplete();
            return ret;
        } catch (Exception e) {
            projectError(e.getMessage());
            return false;
        }
    }

    /**
     * 初始化工作区目录/路径变量
     * @return 工作区路径(tmp_)
     */
    private File createWorkspace() {
        String tmp = new Random().nextInt(100) + "";
        tmp = "tmp_" + System.currentTimeMillis() + tmp;
        File workspace = new File(tmp);
        if (FileUtils.exists(workspace)) {
            FileUtils.delete(workspace);
        }
        FileUtils.createDir(workspace);
        return workspace;
    }

    /**
     * 解压apk
     * @param apk apk文件
     * @param target 解压包路径
     * @return 返回target
     * @throws Exception
     */
    public File extractApk(File apk, File target) throws Exception {
        System.out.println("解压源apk...");
        if (mApkpackage != null) {
            return FileUtils.move(mApkpackage, target, true) ? target : null;
        } else {
            String cmd = "java -jar %s d %s -f -o %s";
            cmd = String.format(cmd, APKTOOL, apk.getAbsolutePath(), target.getAbsolutePath());
            if (Cmd.exec(cmd)) {
                return target;
            } else {
                throw new Exception("解压源apk失败");
            }
        }
    }

    /**
     * 用户配置/签名配置/apk配置 读取
     * @return
     * @throws Exception
     */
    private void setWorkspaceConfig(File apkPackage, File fConfig, File fKeystore) throws Exception {
        System.out.println("读取配置...");
        Map<String, String> config = PropertiesIO.read(fConfig);
        Map<String, String> keystore = fKeystore == null ? null : PropertiesIO.read(fKeystore);

        String sdk_dir = config.get("sdk.dir").replaceAll("\\\\", "/");
        String gradle_dir = config.get("gradle.dir").replaceAll("\\\\", "/");
        String build_version = config.get("build.version");
        String proguard = config.get("proguard");
        String compile_version = build_version.substring(0, build_version.indexOf("."));

        //配置参数
        mConfig.put("sdk_dir", sdk_dir);
        mConfig.put("gradle_dir", gradle_dir);
        mConfig.put("build_version", build_version);
        mConfig.put("compile_version", compile_version);
        mConfig.put("proguard", proguard);
        System.out.println("--sdk_dir=" + sdk_dir);
        System.out.println("--gradle_dir=" + gradle_dir);
        System.out.println("--build_version=" + build_version);
        System.out.println("--compile_version=" + compile_version);
        System.out.println("--proguard=" + proguard);

        //运行文件路径
        mBin.put("aapt", sdk_dir + "/build-tools/" + build_version + "/aapt");
        mBin.put("zipalign", sdk_dir + "/build-tools/" + build_version + "/zipalign");
        if (Cmd.isWindowsOS()) {
            mBin.put("gradle", gradle_dir + "/bin/gradle.bat");
        } else {
            mBin.put("gradle", gradle_dir + "/bin/gradle");
        }

        //签名配置
        if (keystore == null) {
            mKeystore = null;
            System.out.println("--keystore=null");
        } else {
            mKeystore.put("keystore", keystore.get("keystore.dir").replaceAll("\\\\", "/"));
            mKeystore.put("keypass", keystore.get("keystore.pass"));
            mKeystore.put("alias", keystore.get("keystore.alias"));
            mKeystore.put("aliaspass", keystore.get("keystore.aliaspass"));
            System.out.println("--keystore="+keystore.get("keystore.dir"));
        }

        //apk配置
        File apkManifest = new File(apkPackage, "/AndroidManifest.xml");
        if (!FileUtils.exists(apkManifest)) {
            throw new Exception("读取配置失败");
        }
        ManifestUtils manifest = ManifestUtils.load(apkManifest);
        mConfig.put("package_name", manifest.getPackageName());
        mConfig.put("version_code", manifest.getVersionCode());
        mConfig.put("version_name", manifest.getVersionName());
        System.out.println("--package_name=" + mConfig.get("package_name"));
        System.out.println("--version_code=" + mConfig.get("version_code"));
        System.out.println("--version_name=" + mConfig.get("version_name"));
    }

    /**
     * 提取classes.dex
     * @param apk 源apk
     * @param tmp 临时目录
     * @throws Exception
     */
    private void extractClasses(File apk, File tmp) throws Exception {
        System.out.println("提取dex...");
        if (!ZipUtils.extract(apk, "classes[0-9]*\\.dex", tmp)) {
            throw new Exception("提取classes.dex失败");
        }
    }

    /**
     * 整理工程模板
     * @param apkPackage apk解压包目录
     * @throws Exception
     */
    private void orderProject(File workspace, File apkPackage) throws Exception {
        System.out.println("配置工程模板...");
        boolean ret = true;
        File projectDir = new File(workspace, "ApkUnsheller");
        File buildDir = new File(workspace, "build");
        File tmp = new File(workspace, "tmp");

        //复制工程模板
        ret = ret && FileUtils.copy(PROJECT_TPL, projectDir, true);
        ret = ret && FileUtils.copy(BUILD_TPL, buildDir, true);
        ret = ret && FileUtils.merge(buildDir, workspace, true);
        System.out.println("--复制工程模板:" + ret);

        //meta-data常量取值
        String proxyApplication = Common.getProxyApplication(new File(projectDir, "AndroidManifest.xml"));
        String dexMd5 = FileUtils.md5(new File(tmp, "classes.dex"));  //计算classes.dex MD5,作为当前版本代号

        //整理工程模板
        File gradle = new File(workspace, "build.gradle");
        File local = new File(workspace, "local.properties");
        File proguard = new File(workspace, "proguard.txt");

        //set sdk.dir(local.properties)/proguard.txt
        ret = ret && FileUtils.write(local, ("sdk.dir=" + mConfig.get("sdk_dir")).getBytes());
        ret = ret && FileUtils.write(proguard, Common.getProguardConfig(proxyApplication).getBytes());
        System.out.println("--重写Gradle配置:" + ret);

        //set placeholder(build.gradle)
        String content = new String(FileUtils.read(gradle));
        ret = ret && FileUtils.write(gradle, PlaceholderHelper.replace(content, mConfig).getBytes());
        System.out.println("--配置PlaceHolder:" + ret);

        //move res/lib/assets/AndroidManifest -> ApkUnsheller && update public.xml
        ret = ret && FileUtils.move(new File(apkPackage, "assets"), new File(projectDir, "assets"), true);
        ret = ret && FileUtils.move(new File(apkPackage, "lib"), new File(projectDir, "libs"), true);
        ret = ret && FileUtils.move(new File(apkPackage, "res"), new File(projectDir, "res"), true);
        ret = ret && FileUtils.move(new File(apkPackage, "AndroidManifest.xml"), new File(projectDir, "AndroidManifest.xml"), true);
        ret = ret && Common.updatePublicXml(new File(projectDir, "res/values/public.xml"));
        System.out.println("--添加母包资源:" + ret);

        //reset application
        ManifestUtils manifest = ManifestUtils.load(new File(projectDir, "AndroidManifest.xml"));
        //main application
        Element application = manifest.getApplication();
        String applicationName = manifest.getApplication().attributeValue("name");
        if (applicationName != null) {
            Element metadata = application.addElement("meta-data");
            metadata.addAttribute("android:name", Common.MAIN_APPLICATION);
            metadata.addAttribute("android:value", applicationName);
            application.remove(application.attribute("name"));
        }
        application.addAttribute("android:name", proxyApplication);
        //version
        Element version = application.addElement("meta-data");
        version.addAttribute("android:name", Common.VERSION);
        version.addAttribute("android:value", dexMd5);
        ret = ret && manifest.commit();
        System.out.println("--修改Manifest:" + ret);

        //log
        System.out.println("--dex version=" + dexMd5);

        //清理apkPackage
        ret = ret && FileUtils.delete(apkPackage);

        if (!ret) {
            throw new Exception("配置工程模板失败");
        }
    }

    /**
     * 压缩classes并加密
     * @param tmp 工作区临时目录
     * @return classes.zip对象
     * @throws Exception
     */
    private void zipClasses(File tmp, String secret, String assetName) throws Exception {
        System.out.println("打包classes...");
        boolean ret = true;

        File projectDir = new File(FileUtils.getParent(tmp), "ApkUnsheller");
        File dexSrc = new File(tmp, "dexSrc.zip");
        File dex = new File(tmp, assetName + ".zip");
        File target = new File(projectDir, "assets/" + assetName + ".dex");

        ret = ret && ZipUtils.zipFolder(tmp, dexSrc);
//        ret = ret && FileUtils.encrypt(dexSrc, dex, secret);
        ret = ret && FileUtils.copy(dexSrc, dex, true);
        ret = ret && FileUtils.move(dex, target, true);

        FileUtils.delete(dexSrc);
        System.out.println("--classes.zip: " + FileUtils.getSize(target, FileUtils.UNIT_BYTE) + " byte");

        if (!ret) {
            throw new Exception("打包classes.zip失败");
        }
    }

    /**
     * gradle aR
     * @param workspace 临时目录
     * @throws Exception
     */
    private void gradleBuild(File workspace) throws Exception {
        System.out.println("打包壳程序...");
        String cmd = mBin.get("gradle") + " -q assembleRelease";
        if (!Cmd.exec(cmd, workspace)) {
            throw new Exception("gradle执行失败");
        }
    }

    /**
     * 加壳前整理资源
     * 清理临时目录下的classesx.dex,移动gradle release资源
     * @param tmp 临时目录
     * @param source classes.zip
     * @return gradle打包后的apk(壳apk)
     * @throws Exception
     */
    private File prepareShell(File workspace) throws Exception {
        System.out.println("整理加壳资源...");
        File tmp = new File(workspace, "tmp");
        boolean ret = true;

        //清理多余文件
        for (File f : FileUtils.list(tmp, ".*\\.dex")) {
            ret = ret && FileUtils.delete(f);
        }

        //搬动release资源
        String name = workspace.getName() + "-release-unsigned.apk";
        String buildApk = "build/outputs/apk/" + name;
        File sheller = new File(tmp, "ApkUnsheller.apk");
        ret = ret && FileUtils.move(new File(workspace, buildApk), sheller, true);

        if (ret) {
            return sheller;
        } else {
            throw new Exception("整理加壳资源失败");
        }
    }

    /**
     * 加壳
     * @param source 源apk
     * @param sheller 壳程序apk
     * @param tmp 临时目录
     * @return 已加壳的apk对象
     * @throws Exception
     */
    private File performShell(File tmp, File sheller, String secret, String assetName) throws Exception {
        System.out.println("加壳中...");
        boolean ret = true;
        String aapt = mBin.get("aapt");
        File shelledDex = new File(tmp, "classes.dex");
        File shelledApk = new File(tmp,  "shelled.apk");

        //dex加入密钥
        ShellForce.shell(sheller, shelledDex, secret, assetName);

        //更换合并的classes.dex
        ret = ret && FileUtils.copy(sheller, shelledApk, true);
        String cmd = "%s r %s %s";
        ret = ret && Cmd.exec(String.format(cmd, aapt, shelledApk.getName(), "classes.dex"), tmp);
        cmd = "%s a %s %s";
        ret = ret && Cmd.exec(String.format(cmd, aapt, shelledApk.getName(), shelledDex.getName()), tmp);

        System.out.println("--更换classes.dex:" + ret);

        ret = ret && FileUtils.delete(shelledDex);

        if (ret) {
            return shelledApk;
        } else {
            throw new Exception("加壳运行失败");
        }
    }

    /**
     * sign/zipalign
     * @param result
     * @return
     */
    private File signAndAlign(File result) throws Exception {
    	System.out.println("签名中...");
        File signed = new File(ApkSigner.sign(result.getAbsolutePath(), mKeystore.get("keystore"), mKeystore.get("keypass"), mKeystore.get("alias"), mKeystore.get("aliaspass")));
        File aligned = new File(ZipAlignHelper.align(mBin.get("zipalign"), signed.getAbsolutePath()));
        if (!FileUtils.exists(aligned)) {
        	throw new Exception("签名失败");
        }
        FileUtils.delete(signed);
        return aligned;
    }

    private void projectStart(String apkName) {
        System.out.println("开始加壳工程: " + apkName);
        mStartTime = System.currentTimeMillis();
    }

    private void projectComplete() {
        System.out.println("加壳完成,用时" + (float)(System.currentTimeMillis()-mStartTime)/1000 + "s");
        if (!keepTmp) {
            FileUtils.delete(mWorkspace);
        }
    }

    private void projectError(String msg) {
        System.err.println(msg);
        if (!keepTmp) {
            FileUtils.delete(mWorkspace);
        }
        System.exit(-1);
    }

    public ShellerProject(boolean keepTmp, File output) {
        this.keepTmp = keepTmp;
        this.output = output;
    }

    /**
     * 指定临时工作目录
     * @param workspace 工作目录对象
     */
    protected void setWorkspace(File workspace) {
        mWorkspace = workspace;
        if (FileUtils.exists(workspace)) {
            FileUtils.delete(workspace);
        }
    }

    /**
     * 指定apk解压后的目录
     * @param apkpackage 解包后的目录
     */
    protected void setApkpackage(File apkpackage) {
        mApkpackage = apkpackage;
    }
}
