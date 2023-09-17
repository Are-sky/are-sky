package cn.hiles;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * description
 * 算法枚举
 *
 * @author Helios
 * Time：2023-09-17 19:37
 */
@Getter
@AllArgsConstructor
public enum modeEnum {
    COSINE_SIMILARITY(1),
    SIM_HASH_AND_JACCARD(2);
    private final int code;
}
