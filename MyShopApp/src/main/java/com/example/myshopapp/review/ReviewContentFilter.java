package com.example.myshopapp.review;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 评价敏感词检测：命中后进入人工审核，避免广告、引流内容直接展示。
 */
@Component
public class ReviewContentFilter {

    private final List<String> sensitiveWords;

    public ReviewContentFilter(
            @Value("${myshop.review.sensitive-words:加微信,加v,代购,刷单,返现,兼职,赌博,色情,诈骗,办证}")
            String rawWords) {
        this.sensitiveWords = Arrays.stream(rawWords.split(","))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .toList();
    }

    public boolean containsSensitive(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return sensitiveWords.stream()
                .anyMatch(word -> normalized.contains(word.toLowerCase(Locale.ROOT)));
    }
}
