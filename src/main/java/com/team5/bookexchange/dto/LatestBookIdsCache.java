package com.team5.bookexchange.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LatestBookIdsCache {

    // 상세 캐시로 관리할 최신 게시글 ID 최대 30개
    private List<Long> bookIds;

    public LatestBookIdsCache(List<Long> bookIds) {
        this.bookIds = bookIds;
    }
}