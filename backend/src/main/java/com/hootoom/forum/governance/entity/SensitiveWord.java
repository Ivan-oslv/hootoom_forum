package com.hootoom.forum.governance.entity;

import lombok.Data;

@Data
public class SensitiveWord {
    private Long id;
    private String normalizedWord;
    private String matchType;
    private String action;
}
