package com.hootoom.forum.governance.service.impl;

import com.hootoom.forum.common.exception.BusinessException;
import com.hootoom.forum.governance.entity.SensitiveWord;
import com.hootoom.forum.governance.mapper.SensitiveWordMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveTextServiceImplTest {
    @Test
    void normalizesUnicodeCaseAndWhitespaceBeforeMatching() {
        SensitiveWord word = new SensitiveWord();
        word.setNormalizedWord("bad word");
        word.setMatchType("CONTAINS");
        word.setAction("REJECT");
        SensitiveWordMapper mapper = mock(SensitiveWordMapper.class);
        when(mapper.findAllActive()).thenReturn(List.of(word));

        SensitiveTextServiceImpl service = new SensitiveTextServiceImpl(mapper);
        assertThatThrownBy(() -> service.rejectUnsafeProfileText("ＢＡＤ　　ＷＯＲＤ", null))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("CONTENT_REJECTED");
    }
}
