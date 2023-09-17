import cn.hiles.PlagiarismCheckUtil;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.hiles.Main.filePreprocessing;


/**
 * description
 *
 * @author Helios
 * Time：2023-09-17 20:00
 */
public class PlagiarismCheckUtilTest {
    @Test
    public void jieBaSegmentation() {
        String str = "小明硕士毕业于中国科学院计算所后在日本京都大学深造小明硕士毕业于中国科学院计算所后在日本京都大学深造小明硕士毕业于中国科学院计算所后在日本京都大学";
        JiebaSegmenter segment = new JiebaSegmenter();
        List<SegToken> words = segment.process(str, JiebaSegmenter.SegMode.SEARCH);
        Map<String, Integer> frequency = new HashMap<>();
        words.forEach(w -> {
            String token = w.word.getToken();
            if (frequency.containsKey(token)) {
                frequency.put(token, frequency.get(token) + 1);
            } else {
                frequency.put(token, 1);
            }
        });
        System.out.println(frequency);
    }

    @Test
    public void getCosineSimilarity() {
        try {
            String str = filePreprocessing("E:\\test\\orig.txt");
            String str1 = filePreprocessing("E:\\test\\orig_0.8_add.txt");
            double cosineSimilarity = PlagiarismCheckUtil.getCosineSimilarity(str, str1);
            System.out.println(cosineSimilarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
