package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.domain.entity.SensitiveWord;
import com.zhihu.platform.domain.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AuditService {

    private final SensitiveWordMapper sensitiveWordMapper;

    public AuditService(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct
    public void ensureDefaultSensitiveWords() {
        List<String> defaults = Arrays.asList(
                "色情",
                "色情网",
                "成人视频",
                "约炮",
                "嫖娼",
                "赌博",
                "博彩",
                "赌场",
                "诈骗",
                "刷单",
                "兼职刷单",
                "毒品",
                "冰毒",
                "大麻",
                "枪支",
                "炸药",
                "代考",
                "代写论文",
                "出售身份证",
                "出售银行卡",
                "porn",
                "gambling",
                "casino",
                "scam",
                "drug",
                "gun",
                "explosive"
        );
        for (String word : defaults) {
            long count = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                    .eq(SensitiveWord::getWord, word));
            if (count == 0) {
                SensitiveWord item = new SensitiveWord();
                item.setWord(word);
                item.setEnabled(1);
                sensitiveWordMapper.insert(item);
            }
        }
    }

    public String hitSensitiveWord(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<SensitiveWord> words = sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getEnabled, 1));
        String normalizedText = normalize(text);
        for (SensitiveWord word : words) {
            if (word.getWord() != null && normalizedText.contains(normalize(word.getWord()))) {
                return word.getWord();
            }
        }
        return null;
    }

    private String normalize(String text) {
        return text == null
                ? ""
                : text.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}]+", "");
    }
}
