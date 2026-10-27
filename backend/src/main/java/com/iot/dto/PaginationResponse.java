// backend/src/main/java/com/iot/dto/PaginationResponse.java
package com.iot.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaginationResponse<T> {
    private List<T> data;
    private PaginationInfo pagination;

    @Data
    public static class PaginationInfo {
        private int page;
        private int limit;
        private long total;
        private int totalPages;
    }
}
