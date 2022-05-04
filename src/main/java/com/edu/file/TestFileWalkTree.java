package com.edu.file;


import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFileWalkTree {

    /**
     * ---------------------------------------------------
     * 测试目录的遍历
     * ---------------------------------------------------
     */
    public static void main(String[] args) throws IOException {
        // 测试文件与目录的读取
        TestWalkTreeCount();
        // 遍历目录找到某些个后缀结尾的文件
        TestWalkTreeFindFile();

        // 遍历批量删除文件
        TestWalkTreeDeleteFile();

    }
    public static void TestWalkTreeDeleteFile() throws IOException {
        // Files.delete();  这个 API 只能删除空文件夹

        Files.walkFileTree(Paths.get("d:/logs/"),new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.printf("----------进入: %s-------------\n", dir);
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                System.out.printf("----------退出: %s-------------\n", dir);
                return super.postVisitDirectory(dir, exc);
            }
        });
    }

    public static void TestWalkTreeFindFile() throws IOException {
        AtomicInteger count = new AtomicInteger();
        Files.walkFileTree(Paths.get("target"), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(file.toString().endsWith(".jar")) {
                    count.incrementAndGet();
                    System.out.println(file);
                }
                return super.visitFile(file, attrs);
            }
        });
        System.out.println(count);
    }
    

    public static void TestWalkTreeCount() throws IOException {

        // 匿名内部类中想要访问外部类中变量，这个外部的变量只能是 final 修饰的
        // 因此这里不能使用普通的整形来计数
        AtomicInteger dirCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        // ---------------------------------------------------------
        // 这里遍历目录的时候使用了访问者模式, 来对访问到的文件做具体的操作
        // ---------------------------------------------------------
        Files.walkFileTree(Paths.get("src"), new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println("|==> :: " + dir);
                dirCount.incrementAndGet();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                fileCount.incrementAndGet();
                return super.visitFile(file, attrs);
            }
        });
        System.out.println(dirCount.get());
        System.out.println(fileCount.get());
    }


    public static void TestCopyDictionary() throws IOException {
        String source = "d:/logs/";
        String target = "d:/temp/";

        // 使用 Walk 接收一个文件路径，然后返回一个文件路径的流
        // 就可以使用流式处理对应目录中的文件
        Files.walk(Paths.get(source)).forEach(path -> {
            try {
                String replace = path.toString().replace(source, target);
                Path rPath = Paths.get(replace);
                // 如果是目录，则创建目录
                if (Files.isDirectory(path)) {
                    Files.createDirectory(rPath);
                // 如果是文件，则复制文件
                } else if (Files.isRegularFile(path)) {
                    Files.copy(path, rPath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
