package com.chaos.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务调用方发起的请求内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChaosrpcRequest {

    // 请求id
    private long requestId;

    // 请求的类型，压缩的类型，序列化的方式
    private byte requestType;
    private byte compressType;
    private byte serializeType;

    private long timeStamp;

    // 具体的消息体
    private RequestPlayload requestPlayload;
}
