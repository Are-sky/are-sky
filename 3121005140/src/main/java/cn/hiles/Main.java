package cn.hiles;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Helios
 * 文本查重主类
 * 采用cosine similarity  or simhash and Jaccard Similarity
 * @version 1.0
 */
public class Main {
    public static void main(String[] args) {
        //获取参数
        if (args.length < 3) {
            System.out.println("Error：missing arguments! please input 3 arguments:<originPath> <plagiarizedPath> <answerPath>");
            return;
        }
        //参数转换
        String originPath = args[0];
        String plagiarizedPath = args[1];
        String answerPath = args[2];
        try {
            AtomicReference<String> originText = new AtomicReference<>();
            AtomicReference<String> plagiarizedText = new AtomicReference<>();
            //开始查重
            System.out.println("Start plagiarism check...");
            //多线程读取两个文件并进行预处理
            //创建线程
            //读取源文件
            Thread originThread = new Thread(() -> {
                try {
                    originText.set(filePreprocessing(originPath));
                } catch (IOException e) {
                    System.out.println("Error：file not found!");
                    //终止程序
                    System.exit(0);
                }
            });
            //读取抄袭文件
            Thread plagiarizedThread = new Thread(() -> {
                try {
                    plagiarizedText.set(filePreprocessing(plagiarizedPath));
                } catch (IOException e) {
                    System.out.println("Error：file not found!");
                    //终止程序
                    System.exit(0);
                }
            });
            //启动线程
            originThread.start();
            plagiarizedThread.start();
            //等待线程结束
            originThread.join();
            plagiarizedThread.join();
            //创建答案文件
            File answerFile = FileUtil.file(answerPath);
            //计算相似度 根据文件大小选择算法
            double similarity;
            if (originText.get().length() > 1024 * 1024 * 30 || plagiarizedText.get().length() > 1024 * 1024 * 30) {
                //采用sim-hash and Jaccard Similarity
                System.out.println("Use sim-hash and Jaccard Similarity");
                similarity = PlagiarismCheckUtil.getSimilarity(originText.get(), plagiarizedText.get(), modeEnum.SIM_HASH_AND_JACCARD);
            } else {
                //采用cosine similarity
                System.out.println("Use cosine similarity");
                similarity = PlagiarismCheckUtil.getSimilarity(originText.get(), plagiarizedText.get(), modeEnum.COSINE_SIMILARITY);
            }
            //输出相似度
            System.out.println("Similarity：" + similarity);
            //写入答案文件
            FileUtil.writeString(String.valueOf(similarity), answerFile, StandardCharsets.UTF_8);
            System.out.println("Plagiarism check finished!");
        } catch (Exception e) {
            System.out.println("Error：unknown error!");
        }
    }

    /**
     * 文件预处理
     * 去除标点符号、空格、换行符
     *
     * @param filePath 文件路径
     * @return String 处理后的字符串
     */
    public static String filePreprocessing(String filePath) throws IOException {
        //缓冲区
        StringBuilder buffer = new StringBuilder();
        //读取文件
        File file = FileUtil.file(filePath);
        //判空
        if (file == null) {
            throw new FileNotFoundException();
        }
        //缓冲读取文件并过滤
        FileInputStream fileInputStream = new FileInputStream(file);
        //读取文件
        String fileText = IoUtil.read(fileInputStream, Charset.defaultCharset());
        //过滤
        for (int i = 0; i < fileText.length(); i++) {
            char c = fileText.charAt(i);
            //过滤标点符号
            if (c == '，' || c == '。' || c == '、' || c == '；' || c == '：' || c == '？' || c == '！' || c == '“'
                    || c == '”' || c == '‘' || c == '’' || c == '（' || c == '）' || c == '《' || c == '》'
                    || c == '【' || c == '】' || c == '—' || c == '…' || c == '￥' || c == '·'
                    || c == '｛' || c == '｝' || c == '［' || c == '］' || c == '＜' || c == '＞' || c == '＆') {
                continue;
            }
            //过滤空格
            if (c == ' ') {
                continue;
            }
            //过滤换行符
            if (c == '\n') {
                continue;
            }
            //过滤回车符
            if (c == '\r') {
                continue;
            }
            //过滤制表符
            if (c == '\t') {
                continue;
            }
            //添加到缓冲区
            buffer.append(c);
        }
        //关闭流
        IoUtil.close(fileInputStream);
        return buffer.toString();
    }
}