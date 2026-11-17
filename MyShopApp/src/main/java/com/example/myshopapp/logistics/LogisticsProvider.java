package com.example.myshopapp.logistics;

import com.example.myshopapp.order.dto.SubOrderLogisticsVO;
import com.example.myshopapp.order.entity.SubOrder;

/**
 * 物流轨迹提供方：默认使用本地模拟实现，
 * 接入真实快递服务时新增实现并注册为 Spring Bean 即可，不影响订单业务。
 */
public interface LogisticsProvider {

    /** 提供方名称，便于日志与排障 */
    String name();

    /** 查询子订单物流轨迹 */
    SubOrderLogisticsVO query(SubOrder subOrder);
}
