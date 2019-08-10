package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.PayConfig;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.github.wxpay.sdk.WXPayConstants.FAIL;

@Slf4j
@Component
public class PayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    public String createWXPayUrl(Long orderId, Long totalPay, String desc) {
        try {
            Map<String, String> data = new HashMap<>();
            // 商品描述
            data.put("body", desc);
            // 订单号
            data.put("out_trade_no", orderId.toString());
            // 金额，单位是分
            data.put("total_fee", totalPay.toString());
            // 调用微信支付的终端ip
            data.put("spbill_create_ip", payConfig.getSpbillCreateIp());
            // 回调地址
            data.put("notify_url", payConfig.getNotifyUrl());
            // 交易类型为扫码支付
            data.put("trade_type", payConfig.getTradeType());
            // 利用wxPay工具，完成下单
            Map<String, String> result = wxPay.unifiedOrder(data);

            // 判断通信和业务是否成功
            isSuccess(result);

            // 校验签名
            checkSignature(result);

            // 下单成功，获取支付链接
            String url = result.get("code_url");

            return url;

        } catch (Exception e) {
            log.error("[微信下单] 创建预交易订单失败!", e);
            return null;
        }
    }

    /**
     * 校验签名
     *
     * @param result
     */
    public void checkSignature(Map<String, String> result) {
        try {
            boolean boo1 = WXPayUtil.isSignatureValid(result, payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            boolean boo2 = WXPayUtil.isSignatureValid(result, payConfig.getKey(), WXPayConstants.SignType.MD5);
            if (!boo1 && !boo2) {
                throw new LyException(ExceptionEnum.WX_PAY_SIGNATURE_INVALID);
            }
        } catch (Exception e) {
            log.error("[微信支付] 校验签名失败");
            throw new LyException(ExceptionEnum.WX_PAY_CHECK_SIGNATURE_ERROR);
        }
    }

    /**
     * 判断通信和业务是否成功
     *
     * @param result
     * @return
     */
    public void isSuccess(Map<String, String> result) {
        // 判断通信标识
        String return_code = result.get("return_code");
        if (FAIL.equals(return_code)) {
            log.error("[微信下单] 微信下单通信失败,失败原因:{}", result.get("return_msg"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
        // 判断业务是否成功
        String result_code = result.get("result_code");
        if (FAIL.equals(result_code)) {
            log.error("[微信下单] 微信下单业务失败,错误码:{},错误原因:{}", result.get("err_code"), result.get("err_code_des"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
    }

    public PayState queryPayState(Long orderId) {
        try {
            // 封装查询参数
            Map<String, String> data = new HashMap<>();
            // 订单号
            data.put("out_trade_no", orderId.toString());
            Map<String, String> result = wxPay.orderQuery(data);

            // 判断通信和业务是否成功
            isSuccess(result);
            // 校验签名
            checkSignature(result);

            // 校验订单号和金额是否存在
            String totalFeeStr = result.get("total_fee");
            String orderIdStr = result.get("out_trade_no");
            if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(orderIdStr)){
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }
            // 获取结果中的金额
            Long totalFee = Long.valueOf(totalFeeStr);
            // 查询订单
            Order order = orderMapper.selectByPrimaryKey(orderId);
            // 校验金额
            if (totalFee != 1L/*测试order.getActualPay()*/){
                // 金额不符
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }

            /**
             * SUCCESS—支付成功
             * REFUND—转入退款
             * NOTPAY—未支付
             * CLOSED—已关闭
             * REVOKED—已撤销（付款码支付）
             * USERPAYING--用户支付中（付款码支付）
             * PAYERROR--支付失败(其他原因，如银行返回失败)
             */

            // 获取支付状态
            String trade_state = result.get("trade_state");

            if ("SUCCESS".equals(trade_state)){
                // 支付成功
                // 修改订单状态
                OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
                orderStatus.setStatus(OrderStatusEnum.PAID.value());
                orderStatus.setPaymentTime(new Date());
                int count = statusMapper.updateByPrimaryKeySelective(orderStatus);
                if (count != 1){
                    throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
                }
                // 返回成功
                return PayState.SUCCESS;
            }

            if ("NOTPAY".equals(trade_state) || "USERPAYING".equals(trade_state)){
                return PayState.NOT_PAY;
            }
            return PayState.FAIL;

        } catch (Exception e) {
            return PayState.NOT_PAY;
        }
    }
}
