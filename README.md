# 测试源代码生成插件

## 说明
本插件可以根据源文件自动生成测试代码模板，用于统一测试代码编写风格。

**是什么**
* 是一个快速生成测试代码的模板的工具。
* 是一个规范测试代码的编写风格的工具。

**不是什么**
* 不是一个开箱即用的测试代码生成工具，需要结合需求修改生成后的代码的**方法输入参数**和**方法预想执行结果**。
* 不是一个用于快速实现覆盖率要求指标的工具，需要结合需求修改生成后的代码来达到覆盖率指标要求。

## 依赖
源码工程依赖
* [强制] junit4
* [可选] mockito

## 安装
```shell
cd genetest-maven-plugin
mvn install
```

## 使用

* 在 pom.xml 引用插件。示例：[genetest-sample/pom.xml](./genetest-sample/pom.xml)。
```xml
<!-- project > build > plugins -->
<plugin>
    <groupId>github.plugin</groupId>
    <artifactId>genetest-maven-plugin</artifactId>
    <version>1.0</version>
</plugin>
```
* 运行命令生成测试文件。

```shell
cd genetest-sample

# 查看插件使用方法
mvn genetest:help

# 常用生成测试代码命令
# 生成工程源码的全部测试代码 如果存在 则追加测试类到已有文件
mvn genetest:gene

# 生成工程源码的全部测试代码 使用 mockito 作为 mock 工具
mvn genetest:gene -Dmock=mockito

# 生成工程源码的全部测试代码 如果存在 则替换原有文件 重新生成
mvn genetest:gene -Dmode=overwrite

# 生成工程源码的全部测试代码 指定生成文件名后缀
mvn genetest:gene -Dsuffix=AutoTest

# 生成工程源码的指定包下面的测试代码 如果存在 则追加测试类到已有文件
mvn genetest:gene -Dincludes=github.plugin.genetest.model

# 生成工程源码的包含指定类的测试代码 如果存在 则追加测试类到已有文件
mvn genetest:gene -Dincludes=model.github.plugin.genetest.ParseModel

# 生成工程源码的指定包内并且排除指定类的测试代码 如果存在 则追加测试类到已有文件
mvn genetest:gene -Dincludes=github.plugin.genetest.model -Dexcludes=github.plugin.genetest.ParseModel2
```
## 优化
* 测试文件存在时 解析测试文件 增量添加测试分支
* 增量添加测试分支时 可指定测试方法名称
* 使用 mockito 时 可以自动生成 mock 语句
