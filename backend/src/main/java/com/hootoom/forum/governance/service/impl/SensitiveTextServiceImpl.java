package com.hootoom.forum.governance.service.impl;

import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.governance.entity.SensitiveWord;
import com.hootoom.forum.governance.mapper.SensitiveWordMapper;
import com.hootoom.forum.governance.service.SensitiveTextService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;

@Service
public class SensitiveTextServiceImpl implements SensitiveTextService {
    private final SensitiveWordMapper mapper;

    public SensitiveTextServiceImpl(SensitiveWordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void rejectUnsafeProfileText(String nickname, String bio) {
        String normalizedNickname = normalize(nickname);
        String normalizedBio = normalize(bio == null ? "" : bio);
        // 个人资料没有人工审核队列，因此 REVIEW 与 REJECT 均阻止保存；REJECT 优先便于未来扩展策略。
        boolean matched = mapper.findAllActive().stream()
                .sorted(Comparator.comparing((SensitiveWord word) -> !"REJECT".equals(word.getAction())))
                .anyMatch(word -> matches(normalizedNickname, word) || matches(normalizedBio, word));
        if (matched) {
            throw new BusinessException("CONTENT_REJECTED", "提交的资料暂时无法保存", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private boolean matches(String text, SensitiveWord word) {
        String target = normalize(word.getNormalizedWord());
        return "EXACT".equals(word.getMatchType()) ? text.equals(target) : text.contains(target);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").strip();
    }
}
