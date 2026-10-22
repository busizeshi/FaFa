package com.fafa.common.api;

import java.util.List;

/** 按稳定标识递增读取的一页数据；没有下一页时游标为 {@code null}。 */
public record CursorPage<T>(List<T> items, String nextCursor) implements java.io.Serializable {
    public CursorPage { items = List.copyOf(items); }
}
