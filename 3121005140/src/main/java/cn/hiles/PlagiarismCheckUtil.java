package cn.hiles;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * description
 * 文本查重工具类
 *
 * @author Helios
 * Time：2023-09-17 19:27
 */
public class PlagiarismCheckUtil {
    /**
     * 求相似度
     *
     * @param originStr      源字符串
     * @param plagiarizedStr 抄袭字符串
     * @param mode           算法模式
     */
    public static double getSimilarity(String originStr, String plagiarizedStr, modeEnum mode) throws Exception {
        switch (mode) {
            case COSINE_SIMILARITY:
                return getCosineSimilarity(originStr, plagiarizedStr);
            case SIM_HASH_AND_JACCARD:
                return getSimHashAndJaccardSimilarity(originStr, plagiarizedStr);
            default:
                return 0;
        }
    }


    /**
     * 通过余弦相似度算法求相似度
     *
     * @param originStr      源字符串
     * @param plagiarizedStr 抄袭字符串
     */
    public static double getCosineSimilarity(String originStr, String plagiarizedStr) throws Exception {
        //分词并计算词频 双线程执行
        AtomicReference<Map<String, Integer>> originFrequency = new AtomicReference<>();
        AtomicReference<Map<String, Integer>> plagiarizedFrequency = new AtomicReference<>();
        Thread originThread = new Thread(() -> originFrequency.set(jieBaSegmentation(originStr)));
        Thread plagiarizedThread = new Thread(() -> plagiarizedFrequency.set(jieBaSegmentation(plagiarizedStr)));
        originThread.start();
        plagiarizedThread.start();
        try {
            originThread.join();
            plagiarizedThread.join();
        } catch (InterruptedException e) {
            throw new Exception("Error: thread interrupted!");
        }
        //去重
        Set<String> words = new HashSet<>();
        words.addAll(originFrequency.get().keySet());
        words.addAll(plagiarizedFrequency.get().keySet());

        //构造向量
        Vector<Integer> originVector = new Vector<>();
        Vector<Integer> plagiarizedVector = new Vector<>();

        for (String word : words) {
            originVector.add(originFrequency.get().getOrDefault(word, 0));
            plagiarizedVector.add(plagiarizedFrequency.get().getOrDefault(word, 0));
        }
        //计算相似度
        int sumA = 0;
        int sumB = 0;
        int sumAB = 0;
        for (int i = 0; i < words.size(); i++) {
            int a = originVector.get(i);
            int b = plagiarizedVector.get(i);
            sumAB += a * b;
            sumA += a * a;
            sumB += b * b;
        }
        double a = Math.sqrt(sumA);
        double b = Math.sqrt(sumB);
        return BigDecimal.valueOf(sumAB).divide(BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)), 3, RoundingMode.HALF_UP).doubleValue();
    }


    /**
     * 通过sim-hash和Jaccard算法求相似度
     *
     * @param originStr      源字符串
     * @param plagiarizedStr 抄袭字符串
     */
    public static double getSimHashAndJaccardSimilarity(String originStr, String plagiarizedStr) throws Exception {
        //分词并计算词频 双线程执行
        AtomicReference<Map<String, Integer>> originFrequency = new AtomicReference<>();
        AtomicReference<Map<String, Integer>> plagiarizedFrequency = new AtomicReference<>();
        Thread originThread = new Thread(() -> originFrequency.set(jieBaSegmentation(originStr)));
        Thread plagiarizedThread = new Thread(() -> plagiarizedFrequency.set(jieBaSegmentation(plagiarizedStr)));
        originThread.start();
        plagiarizedThread.start();
        try {
            originThread.join();
            plagiarizedThread.join();
        } catch (InterruptedException e) {
            throw new Exception("Error: thread interrupted!");
        }
        //计算sim-hash
        String originSimHash = getSimHash(originFrequency.get());
        String plagiarizedSimHash = getSimHash(plagiarizedFrequency.get());
        //计算海明距离
        int hammingDistance = 0;
        if (originSimHash.length() != plagiarizedSimHash.length()) {
            throw new Exception("Error: sim-hash length not equal!");
        }
        for (int i = 0; i < originSimHash.length(); i++) {
            if (originSimHash.charAt(i) != plagiarizedSimHash.charAt(i)) {
                hammingDistance++;
            }
        }
        //计算jaccard相似度
        //交集
        int intersection = originSimHash.length() - hammingDistance;
        //并集
        int union = originSimHash.length() + hammingDistance;
        return BigDecimal.valueOf(intersection).divide(BigDecimal.valueOf(union), 3, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * jieba分词并计算权重
     *
     * @param str 字符串
     * @return Map<String, Integer> 词频
     */
    public static Map<String, Integer> jieBaSegmentation(String str) {
        JiebaSegmenter segment = new JiebaSegmenter();
        List<SegToken> words = segment.process(str, JiebaSegmenter.SegMode.SEARCH);
        Map<String, Integer> frequency = new HashMap<>();
        //统计词频
        words.forEach(w -> {
            String token = w.word.getToken();
            if (frequency.containsKey(token)) {
                frequency.put(token, frequency.get(token) + 1);
            } else {
                frequency.put(token, 1);
            }
        });
        //根据词频计算权重
        frequency.replaceAll((w, v) -> frequency.get(w) * w.length());
        return frequency;
    }


    /**
     * 求sim-hash
     *
     * @param frequency 词频
     * @return String sim-hash
     */
    public static String getSimHash(Map<String, Integer> frequency) throws NoSuchAlgorithmException {
        int[] vector = new int[128];
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        for (String word : frequency.keySet()) {
            //1. 获取自定义的哈希值
            StringBuilder hash = new StringBuilder(new BigInteger(1, messageDigest.digest(word.getBytes(StandardCharsets.UTF_8))).toString(2));
            //如果位数不够那就补充位数，为了不影响实际情况，补0，1有特殊的意义
            if (hash.length() < 128) {
                int bit = 128 - hash.length();
                for (int i = 0; i < bit; i++) {
                    hash.append("0");
                }
            }
            //2 and 3：这里既是加权，也是合并，因为只对vector这一个向量进行操作
            for (int j = 0; j < vector.length; j++) {
                //只有在1的地方进行加权
                if (hash.charAt(j) == '1') {
                    vector[j] += frequency.get(word);
                } else {
                    vector[j] -= frequency.get(word);
                }
            }
        }
        //4：降维
        StringBuilder simHash = new StringBuilder();
        for (int i : vector) {
            if (i > 0) {
                simHash.append("1");
            } else {
                simHash.append("0");
            }
        }
        return simHash.toString();
    }


}
