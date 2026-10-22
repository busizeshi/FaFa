package com.fafa.biz.media.domain;

import java.time.Instant;
import java.util.Map;

/** 对象存储边界；不能由客户端指定任意 bucket 或对象键。 */
public interface ObjectStorage {
    record Policy(String url, Map<String,String> fields) { }
    record ObjectInfo(long bytes,String etag) { }
    record Fragment(String key,String etag) { }
    ObjectInfo inspect(String key);
    void copy(String source,String target);
    void compose(java.util.List<Fragment> parts,String target);
    void fetch(String key,long expectedBytes,java.nio.file.Path target);
    void putFile(String key,java.nio.file.Path file,String mime);
    Policy policy(String key, String mime, long bytes, Instant expiresAt);
    byte[] read(String key, long expectedBytes);
    void put(String key, byte[] bytes, String mime);
    void delete(String key);
    String downloadAttachment(String key,int seconds);
    String download(String key, int seconds);
}
