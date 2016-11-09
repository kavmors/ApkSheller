package com.kavmors.apkunsheller;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import dalvik.system.DexClassLoader;
import jcommon.util.FileUtils;
import jcommon.util.RefInvoke;
import jcommon.util.ZipUtils;

/**
 * ShellerApplication事务实现类,方便混淆
 */
public class Unsheller {
    private static final String DEFAULT_APPLICATION = "android.app.Application";
    private File LIBS;          //依赖库位置
    private Application mApp;
    private Bundle mMetadata;

    public Unsheller(Application a) {
        mApp = a;
    }

    private void init() {
        LIBS = mApp.getDir("libs", Context.MODE_PRIVATE);
    }

    public void attachBaseContext(Context base) {
        try {
            init();
            mMetadata = mApp.getPackageManager().getApplicationInfo(mApp.getPackageName(), PackageManager.GET_META_DATA).metaData;
            if (mMetadata == null || !mMetadata.containsKey(Common.VERSION)) {
                throw new RuntimeException("No meta data.");
            }

            File classesDex = new File(LIBS, "classes.dex");
            if (!FileUtils.exists(classesDex) || !matchVersion(classesDex)) {
                decodeDex(getThisApk(), LIBS);
            }

            resetClassLoader(LIBS);
        } catch (Exception e) {
            e.printStackTrace();
            exception();
        }
    }

    public void onCreate() {
        String main = DEFAULT_APPLICATION;
        try {
            if (mMetadata.containsKey(Common.MAIN_APPLICATION)) {
                main = mMetadata.getString(Common.MAIN_APPLICATION);
                invokeMainApplication(main).onCreate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            exception();
        }
    }

    //获取壳apk
    private File getThisApk() {
        return new File(mApp.getApplicationInfo().sourceDir);
    }

    //比对版本
    private boolean matchVersion(File classDex) {
        String dexMd5 = FileUtils.md5(classDex);
        String sourceMd5 = mMetadata.getString(Common.VERSION);
        return dexMd5.equals(sourceMd5);
    }

    /**
     * 从assets/shell.zip解压classes.dex
     * @param thisApk
     * @param libs
     * @throws IOException
     */
    private void decodeDex(File thisApk, File libs) throws IOException {
        File thisDex = new File(libs, "thisApk.dex");
        File classesEncrypt = new File(libs, "classes_encrypt.dex");
        File classesZip = new File(libs, "classes.zip");

        //apk提取classes.dex
        ZipUtils.extractFile(thisApk, "classes.dex", thisDex);

        //classes.dex提取.dex名称和密钥
        long len = thisDex.length();
        String dexName = new String(FileUtils.read(thisDex, len-Common.DEXNAME_LEN, Common.DEXNAME_LEN));
        String secret = new String(FileUtils.read(thisDex, len-Common.DEXNAME_LEN-Common.SECRET_LEN, Common.SECRET_LEN));
        System.out.println("--secret=" + secret);
        System.out.println("--dexName=" + dexName);

        //提取classes.dex+解密
        ZipUtils.extractFile(thisApk, "assets/" + dexName + ".dex", classesEncrypt);
//        FileUtils.decrypt(classesEncrypt, classesZip, secret);
        FileUtils.copy(classesEncrypt, classesZip, true);

        //解压classes.zip
        ZipUtils.unzip(classesZip, libs);

        //删临时文件
        FileUtils.delete(thisApk);
        FileUtils.delete(thisDex);
        FileUtils.delete(classesEncrypt);
        FileUtils.delete(classesZip);

//        extractNativeSo(sourceApk);
    }


    //注入ClassLoader
    // ActivityThread.currentActivityThread().mPackages.get(packagename).get().mClassLoader = dexloader;
    private void resetClassLoader(File libs) {
        File nativeLib = new File(FileUtils.getParent(LIBS), "lib");
        String classActivityThread = "android.app.ActivityThread";
        String classLoadedApk = "android.app.LoadedApk";
        DexClassLoader loader;

        Object activityThread = RefInvoke.invokeStaticMethod(classActivityThread, "currentActivityThread", new Class[]{}, new Object[]{});
        Map<?,?> mPackage = (Map<?,?>)RefInvoke.getField(activityThread, classActivityThread, "mPackages");
        WeakReference<?> wr = (WeakReference<?>) mPackage.get(mApp.getPackageName());

        StringBuffer dexPathList = new StringBuffer();
        for (File dex : FileUtils.list(libs, "classes[0-9]*\\.dex")) {
            dexPathList.append(":").append(dex.getAbsolutePath());
            System.out.println("loaded " + dex.getName());
        }
        loader = new DexClassLoader(dexPathList.toString(), mApp.getCacheDir().getAbsolutePath(), nativeLib.getAbsolutePath(), (ClassLoader) RefInvoke.getField(wr.get(), "android.app.LoadedApk", "mClassLoader"));
        RefInvoke.setField(wr.get(), classLoadedApk, "mClassLoader", loader);
    }

    //执行Application
    private Application invokeMainApplication(String applicationName) {
        String classActivityThread = "android.app.ActivityThread";
        String classLoadedApk = "android.app.LoadedApk";

        //ActivityThread.currentActivityThread().mBoundApplication.info.mApplication = null;
        Object currentActivityThread = RefInvoke.invokeStaticMethod(classActivityThread, "currentActivityThread", new Class[]{}, new Object[]{});
        Object mBoundApplication = RefInvoke.getField(currentActivityThread, classActivityThread, "mBoundApplication");
        Object loadedApkInfo = RefInvoke.getField(mBoundApplication, classActivityThread+"$AppBindData", "info");
        RefInvoke.setField(loadedApkInfo, classLoadedApk, "mApplication", null);

        //currentActivityThread.mAllApplications.remove(currentActivityThread.mInitialApplication)
        Object mInitApplication = RefInvoke.getField(currentActivityThread, classActivityThread, "mInitialApplication");
        List<Application> mAllApplications = (List<Application>) RefInvoke.getField(currentActivityThread, classActivityThread, "mAllApplications");
        mAllApplications.remove(mInitApplication);

        //(LoadedApk) loadedApkInfo.mApplicationInfo.className = applicationName
        ((ApplicationInfo) RefInvoke.getField(loadedApkInfo, classLoadedApk, "mApplicationInfo")).className = applicationName;

        //(ActivityThread$AppBindData) mBoundApplication.appInfo.className = applicationName
        ((ApplicationInfo) RefInvoke.getField(mBoundApplication, classActivityThread+"$AppBindData", "appInfo")).className = applicationName;

        //currentActivityThread.mInitApplication = loadedApkInfo.makeApplication(false, null)
        Application makeApplication = (Application) RefInvoke.invokeMethod(loadedApkInfo, classLoadedApk, "makeApplication", new Class[]{boolean.class, Instrumentation.class}, new Object[]{false, null});
        RefInvoke.setField(currentActivityThread, classActivityThread, "mInitialApplication", makeApplication);

        //currentActivityThread.mProviderMap
        Map<?,?> mProviderMap = (Map<?,?>) RefInvoke.getField(currentActivityThread, classActivityThread, "mProviderMap");
        for (Entry<?, ?> entry : mProviderMap.entrySet()) {
            Object providerClientRecord = entry.getValue();
            Object mLocalProvider = RefInvoke.getField(providerClientRecord, classActivityThread+"$ProviderClientRecord", "mLocalProvider");
            RefInvoke.setField(mLocalProvider, "android.content.ContentProvider", "mContext", makeApplication);
        }
        return makeApplication;
    }

    private int byteToInt(byte[] b) {
        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        DataInputStream dis = new DataInputStream(bais);
        try {
            int sourceLen = dis.readInt();
            bais.close();
            dis.close();
            return sourceLen;
        } catch (IOException e) {
          e.printStackTrace();
          return 0;
        }
    }

    private void exception() {
//        FileUtils.delete(LIBS);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    //提取.so本地库
//    private void extractNativeSo(File sourceApk) throws IOException {
//      //分析被加壳的apk文件
//        ZipInputStream localZipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(sourceApk)));
//        while (true) {
//            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
//            if (localZipEntry == null) {
//                localZipInputStream.close();
//                break;
//            }
//            //提取.so文件到app_libs
//            String name = localZipEntry.getName();
//            if (name.startsWith("lib/") && name.endsWith(".so")) {
//                File storeFile = new File(libs, name.substring(name.lastIndexOf('/')));
//                storeFile.createNewFile();
//                FileOutputStream fos = new FileOutputStream(storeFile);
//                byte[] arrayOfByte = new byte[1024];
//                while (true) {
//                    int i = localZipInputStream.read(arrayOfByte);
//                    if (i == -1)
//                        break;
//                    fos.write(arrayOfByte, 0, i);
//                }
//                fos.flush();
//                fos.close();
//            }
//            localZipInputStream.closeEntry();
//        }
//        localZipInputStream.close();
//    }
}
