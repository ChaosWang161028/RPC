package com.chaos.channelHandler.handler;

import com.chaos.compress.CompressFactory;
import com.chaos.compress.Compressor;
import com.chaos.enumeration.RequestType;
import com.chaos.serialize.Serializer;
import com.chaos.serialize.SerializerFactory;
import com.chaos.transport.message.ChaosrpcRequest;
import com.chaos.transport.message.MessageFormatConstant;
import com.chaos.transport.message.RequestPlayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

/**
 * 基于长度字段的帧解码器
 */
@Slf4j
public class ChaosrpcRequestDecoder extends LengthFieldBasedFrameDecoder {
    public ChaosrpcRequestDecoder() {
        super(
                // 找到当前报文的总长度，截取报文，截取出来的报文我们可以去进行解析
                // 最大帧的长度，超过这个 maxFrameLength 值会直接丢弃
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度的字段的偏移量
                MessageFormatConstant.MAGIC.length
                        + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // todo 负载的适配长度
                -(MessageFormatConstant.MAGIC.length
                        + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH
                        + MessageFormatConstant.FULL_FIELD_LENGTH),
                0
        );

    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

        Thread.sleep(new Random().nextInt(50));
        Object decode = super.decode(ctx, in);
        if(decode instanceof ByteBuf byteBuf) {
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        // 1.解析魔数
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        // 检测魔数是否匹配
        for (int i = 0; i < magic.length; i++) {
            if(magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("获得的请求类型不合法。");
            }
        }

        // 2.解析版本号
        byte version = byteBuf.readByte();
        if(version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("获得的请求版本不被支持。");
        }

        // 3.解析头部的长度
        short headLength = byteBuf.readShort();

        // 4.解析总长度
        int fullLength = byteBuf.readInt();

        // 5.序列化类型
        byte serializeType = byteBuf.readByte();

        // 6.压缩类型
        byte compressType = byteBuf.readByte();

        // 7.请求类型 判断是不是心跳检测
        byte requestType = byteBuf.readByte();

        // 8.请求id
        long requestId = byteBuf.readLong();

        // 9.时间戳
        long timestamp = byteBuf.readLong();

        // 我们需要封装
        ChaosrpcRequest chaosrpcRequest = new ChaosrpcRequest();
        chaosrpcRequest.setRequestId(requestId);
        chaosrpcRequest.setCompressType(compressType);
        chaosrpcRequest.setSerializeType(serializeType);
        chaosrpcRequest.setRequestType(requestType);
        chaosrpcRequest.setTimeStamp(timestamp);

        // 心跳请求没有负载，此处可以判断并直接返回
        if (requestType == RequestType.HEARTBEAT.getId()) {
            return chaosrpcRequest;
        }

        int playloadLength = fullLength - headLength;
        byte[] playload = new byte[playloadLength];
        byteBuf.readBytes(playload);

        // 有了字节数组之后就可以解压缩，反序列化
        // 解压
        if(playload.length != 0) {
            Compressor compressor = CompressFactory.getCompress(compressType).getImpl();
            playload = compressor.decompress(playload);
            // 反序列化
            // 1--->jdk
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getImpl();
            RequestPlayload requestPlayload = serializer.disSerialize(playload, RequestPlayload.class);
            chaosrpcRequest.setRequestPlayload(requestPlayload);
        }
        if(log.isDebugEnabled()) {
            log.debug("请求{}已经在服务端完成解码工作.", chaosrpcRequest.getRequestId());
        }
        return chaosrpcRequest;
    }
}
