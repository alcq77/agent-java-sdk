package io.github.alcq77.cqagent.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应（page 从 1 开始）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;

    private long total;

    private int page;

    private int size;

    private int totalPages;

    private boolean hasNext;

    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(Collections.emptyList())
                .total(0)
                .page(page)
                .size(size)
                .totalPages(0)
                .hasNext(false)
                .build();
    }
}
