# Android加固工程 #

APK加固（加壳）工具

## APK加壳 ##

**java -jar ApkSheller.jar <apk\> [config...]**

config选项：

- **-config <config.properties>**：指定配置文件，配置选项可参照根目录下的config.properties（可选，默认使用根目录下config.properties）
- **-keystore <keystore.properties>**：指定签名配置，配置选项可参照根目录下的debug.properties（可选，默认不签名）
- **-o <output.apk>**：加壳后的文件路径（可选，默认在源apk的目录生成新文件）
- **-keeptmp**：可选，若添加此选项，加固后保留临时工作目录

## 清理工作目录 ##

**java -jar ApkSheller.jar -clean**

该命令将清理根目录下所有临时目录（tmp_**）