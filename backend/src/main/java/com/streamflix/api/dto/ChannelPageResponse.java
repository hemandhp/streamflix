package com.streamflix.api.dto;

import com.streamflix.api.model.Channel;

import java.util.List;

public record ChannelPageResponse(
        List<Channel> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages
) {
    public static ChannelPageResponse of(List<Channel> pageItems, int page, int pageSize, long totalItems) {
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        return new ChannelPageResponse(pageItems, page, pageSize, totalItems, totalPages);
    }
}
