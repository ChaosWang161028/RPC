package com.chaos.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 负载均衡器的接口
 */
public interface LoadBalancer {
    // 应该具备的能力，根据服务列表找到一个可用的服务

    /**
     * 根据服务名找到一个可用的服务
     * @param serviceName 服务名称
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(String serviceName, String group);

    /**
     * 当感知节点发生了动态上/下线，我们需要重新进行负载均衡
     *
     * @param serviceName 服务名称
     * @param addresses 地址
     */
    void reLoadBalance(String serviceName, List<InetSocketAddress> addresses);
}
