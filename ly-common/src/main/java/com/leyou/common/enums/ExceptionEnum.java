package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ExceptionEnum {
    BRAND_NOT_FOUND(404,"品牌不存在"),
    CATEGORY_NOT_FOUND(404,"商品分类没查到"),
    GOODS_NOT_FOUND(404,"商品不存在"),
    GOODS_DETAIL_NOT_FOUND(404,"商品详情信息不存在"),
    GOODS_SKU_NOT_FOUND(404,"未找到该商品信息"),
    GOODS_STOCK_NOT_FOUND(404,"商品库存不存在"),
    SPEC_GROUP_NOT_FOUND(404,"商品规格组不存在"),
    SPEC_PARAM_NOT_FOUND(404,"商品规格参数不存在"),
    BRAND_SAVE_ERROR(500,"新增品牌失败"),
    GOODS_SAVE_ERROR(500,"新增商品失败"),
    FILE_UPLOAD_ERROR(500,"文件上传失败"),
    INVALID_FILE_TYPE(400,"无效的文件类型"),
    GOODS_UPDATE_ERROR(500,"商品更新失败"),
    GOODS_ID_CANNOT_BE_NULL(400,"商品id不能为空"),
    INVALID_USER_DATA_TYPE(400,"用户数据类型无效"),
    INVALID_VERIFY_CODE(400,"无效的验证码"),
    USER_REGISTER_ERROR(500,"用户注册失败"),
    INVALID_USERNAME_PASSWORD(400,"用户名或密码错误"),
    UNAUTHORIZED(403,"未授权"),
    CART_NOT_FOUND(404,"购物车为空"),
    STOCK_IS_NOT_ENOUGH(500,"库存不足"),
    CREATE_ORDER_ERROR(500,"新增订单失败"),
    CREATE_ORDER_DETAIL_ERROR(500,"新增订单详情信息失败"),
    CREATE_ORDER_STATUS_ERROR(500,"新增订单状态信息失败"),
    ORDER_NOT_FOUND(404,"订单不存在"),
    ORDER_DETAIL_NOT_FOUND(404,"订单详情信息不存在"),
    ORDER_STATUS_NOT_FOUND(404,"订单状态信息不存在"),
    WX_PAY_ORDER_FAIL(500,"微信下单失败"),
    WX_PAY_SIGNATURE_INVALID(400,"微信支付签名无效"),
    WX_PAY_CHECK_SIGNATURE_ERROR(500,"校验签名失败"),
    ORDER_STATUS_ERROR(400,"订单状态异常"),
    INVALID_ORDER_PARAM(400,"无效的订单参数"),
    UPDATE_ORDER_STATUS_ERROR(500,"更新订单状态信息失败"),
    ;
    private int code;
    private String msg;
}
