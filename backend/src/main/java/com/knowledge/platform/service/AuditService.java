package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.entity.SensitiveWord;
import com.knowledge.platform.domain.mapper.SensitiveWordMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
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
                "\u8272\u60c5",
                "\u6210\u4eba\u7f51\u7ad9",
                "\u6210\u4eba\u89c6\u9891",
                "\u7ea6\u70ae",
                "\u5ad6\u5a3c",
                "\u8d4c\u535a",
                "\u535a\u5f69",
                "\u8d4c\u573a",
                "\u8bc8\u9a97",
                "\u5237\u5355",
                "\u517c\u804c\u5237\u5355",
                "\u6bd2\u54c1",
                "\u51b0\u6bd2",
                "\u5927\u9ebb",
                "\u67aa\u652f",
                "\u70b8\u836f",
                "\u4ee3\u8003",
                "\u4ee3\u5199\u8bba\u6587",
                "\u51fa\u552e\u8eab\u4efd\u8bc1",
                "\u51fa\u552e\u94f6\u884c\u5361",
                "\u94f6\u884c\u5361\u51fa\u552e",
                "\u4e70\u5356\u94f6\u884c\u5361",
                "\u6536\u94f6\u884c\u5361",
                "\u51fa\u552e\u5bf9\u516c\u8d26\u6237",
                "\u56db\u4ef6\u5957",
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
            if (word.getWord() == null || word.getWord().isBlank()) {
                continue;
            }
            String normalizedWord = normalize(word.getWord());
            if (!normalizedWord.isEmpty() && normalizedText.contains(normalizedWord)) {
                return word.getWord();
            }
        }
        return null;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\p{Punct}\\p{IsPunctuation}\\p{S}]+", "");
    }

    // ---- Sensitive word CRUD ----

    public List<SensitiveWord> listWords() {
        return sensitiveWordMapper.selectList(new LambdaQueryWrapper<SensitiveWord>()
                .orderByAsc(SensitiveWord::getId));
    }

    public SensitiveWord addWord(String word) {
        if (word == null || word.isBlank()) {
            throw new AppException("敏感词不能为空");
        }
        long exists = sensitiveWordMapper.selectCount(new LambdaQueryWrapper<SensitiveWord>()
                .eq(SensitiveWord::getWord, word.trim()));
        if (exists > 0) {
            throw new AppException("敏感词已存在");
        }
        SensitiveWord item = new SensitiveWord();
        item.setWord(word.trim());
        item.setEnabled(1);
        sensitiveWordMapper.insert(item);
        return item;
    }

    public void updateWord(Long id, String word, Integer enabled) {
        SensitiveWord item = sensitiveWordMapper.selectById(id);
        if (item == null) {
            throw new AppException("敏感词不存在");
        }
        if (word != null && !word.isBlank()) {
            item.setWord(word.trim());
        }
        if (enabled != null) {
            item.setEnabled(enabled);
        }
        sensitiveWordMapper.updateById(item);
    }

    public void deleteWord(Long id) {
        sensitiveWordMapper.deleteById(id);
    }
}