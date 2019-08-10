package com.leyou.order.service.impl;

import com.leyou.auth.pojo.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.poji.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.service.OrderService;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    @Override
    @Transactional
    public Long createOrder(OrderDTO orderDTO) {

        Order order = new Order();
        // 1. 新增订单
        // 1.1 订单编号，基本信息
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());
        // 1.2 用户信息
        UserInfo user = UserInterceptor.getLoginUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);
        // 1.3 收货人信息
        AddressDTO addressDTO = AddressClient.findById(1L);
        order.setReceiver(addressDTO.getName());
        order.setReceiverAddress(addressDTO.getAddress());
        order.setReceiverState(addressDTO.getState());
        order.setReceiverCity(addressDTO.getCity());
        order.setReceiverDistrict(addressDTO.getDistrict());
        order.setReceiverZip(addressDTO.getZipCode());
        order.setReceiverMobile(addressDTO.getPhone());
        // 1.4 金额
        List<CartDTO> carts = orderDTO.getCarts();
        Map<Long, Integer> cartsNum = carts.stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));
        Set<Long> ids = cartsNum.keySet();

        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(ids));
        Long total = 0L;
        // 订单详情集合
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (Sku sku : skus) {
            // 计算商品总价
            total += sku.getPrice() * cartsNum.get(sku.getId());
            // 封装orderDetail对象，并存入集合中
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(sku.getId());
            orderDetail.setImage(StringUtils.substringBefore(sku.getImages(),","));
            orderDetail.setNum(cartsNum.get(sku.getId()));
            orderDetail.setOrderId(orderId);
            orderDetail.setOwnSpec(sku.getOwnSpec());
            orderDetail.setTitle(sku.getTitle());
            orderDetail.setPrice(sku.getPrice());
            orderDetails.add(orderDetail);
        }
        order.setTotalPay(total);
        // 实付金额 = 总金额 + 邮费 - 优惠金额
        order.setActualPay(total + order.getPostFee() - 0);
        int count = orderMapper.insertSelective(order);
        if (count != 1){
            log.error("[订单服务] 新增订单失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }
        // 2. 新增订单详情
        count = detailMapper.insertList(orderDetails);
        if (count != orderDetails.size()){
            log.error("[订单服务] 新增订单详情信息失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_DETAIL_ERROR);
        }
        // 3. 新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1){
            log.error("[订单服务] 新增订单状态信息失败,orderId:{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_STATUS_ERROR);
        }
        // 4. 减库存
        goodsClient.decreaseStock(carts);

        return orderId;
    }

    /**
     * 根据订单id查询订单信息
     * @param orderId
     * @return
     */
    @Override
    public Order queryOrderById(Long orderId) {

        // 查询订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order == null ){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }
        // 查询订单详情
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> details = detailMapper.select(orderDetail);
        if (CollectionUtils.isEmpty(details)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);
        // 查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        if (orderStatus == null){
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);
        return order;
    }

    /**
     * 生成微信支付二维码url
     * @param orderId
     * @return
     */
    @Override
    public String createWXPayUrl(Long orderId) {

        // 查询订单
        Order order = queryOrderById(orderId);

        // 判断订单支付状态
        OrderStatus orderStatus = order.getOrderStatus();
        if (OrderStatusEnum.UN_PAY.value() != orderStatus.getStatus()){
            // 订单状态异常
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        // 获取实付金额
        Long actualPay = order.getActualPay();
        // 商品描述
        String desc = order.getOrderDetails().get(0).getTitle();
        // 生成url
        String url = payHelper.createWXPayUrl(orderId, 1L/*测试actualPay*/, desc);

        return url;
    }

    /**
     * 处理微信
     * @param result
     */
    @Override
    @Transactional
    public void handleNotify(Map<String, String> result) {
        // 校验数据
        payHelper.isSuccess(result);
        // 校验签名
        payHelper.checkSignature(result);
        // 校验订单号和金额是否存在
        String totalFeeStr = result.get("total_fee");
        String orderIdStr = result.get("out_trade_no");
        if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(orderIdStr)){
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        // 获取结果中的金额
        Long totalFee = Long.valueOf(totalFeeStr);
        // 查询订单
        Long orderId = Long.valueOf(orderIdStr);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        // 校验金额
        if (totalFee != 1L/*测试order.getActualPay()*/){
            // 金额不符
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }

        // 修改订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        orderStatus.setStatus(OrderStatusEnum.PAID.value());
        orderStatus.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
        if (count != 1){
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }
        log.info("[订单回调] 订单支付成功,订单编号:{}",orderId);
    }

    /**
     * 根据订单id查询订单支付状态
     * @param orderId
     * @return
     */
    @Override
    public PayState queryOrderStatusByOrderId(Long orderId) {

        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        // 判断是否支付
        if (orderStatus.getStatus() == OrderStatusEnum.PAID.value()){
            // 如果是已支付，那就一定是已支付
            return PayState.SUCCESS;
        }

        // 如果是未支付，不一定是未支付，需要向微信查询支付状态
        return payHelper.queryPayState(orderId);
    }
}
