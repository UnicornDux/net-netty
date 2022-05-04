package com.edu.file;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * jdk1.7 中引入了 Path 类与 Paths工具类
 * -----------------------------------------------------
 * Path 类用来表示文件或者文件夹的路径
 * Paths 类是用来获取Path对象的工具类
 *
 */
public class TestFilePath {

    public static void main(String[] args) {

        Path path = Paths.get("1.txt"); // 相对路径, 使用的是 user.dir 来进行文件的定位
        System.out.println(path);
        System.out.println(path.toAbsolutePath()); // 获取绝对路径
        System.out.println(Files.exists(path)); // 判断文件是否存在
        System.out.println(path.getFileSystem());

        Path source = Paths.get("d://logs/"); // 绝对路径, 使用的是系统的绝对路径
        System.out.println(source);

        Path target = Paths.get("d:/logs");  // 绝对路径, 使用的是系统的绝对路径
        System.out.println(target);

        Path join = Paths.get("d:/logs/", "console.log");  // 多级路径拼接
        System.out.println(join);

    }

    public static void testPathOperation() throws IOException {
        Path path = Paths.get("d:/logs/", "console.log");  // 多级路径拼接
        System.out.println(path);
        System.out.println(path.getFileSystem());
        System.out.println(Files.exists(path)); // 判断文件是否存在

        // 创建一级目录，
        // 多级目录则会报错 NoSuchFileException,
        // 如果目录已经存在，则会报错 FileAlreadyExistsException
        Files.createDirectory(path);
        // 创建多级目录
        Files.createDirectories(path);
    }


    public static void copyFile(){
        Path source = Paths.get("d:/logs/", "console.log");  // 多级路径拼接
        Path target = Paths.get("d:/logs/", "stdout.log");  // 多级路径拼接
        try {
            // 直接拷贝, 如果文件 target 已经存在会报错 FileAlreadyExistsException，
            // 需要添加一个额外参数 StandardCopyOption.REPLACE_EXISTING
            // 这个方法拷贝文件使用的是操作系统的底层实现，效率与 transferTo 方法相差不多
            // 这个方法是用于拷贝文件的，不能用于拷贝目录
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(){
        Path path = Paths.get("d:/logs/", "console.log");  // 多级路径拼接
        try {
            // 删除文件. 文件不存在会报错 NoSuchFileException
            // 如果是目录，如果目录中有内容，则会报错 DirectoryNotEmptyException
            Files.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
