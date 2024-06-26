package MiYaISV

import Core.RequestDelegate
import com.kargo.request.GoodsDetailRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import spock.lang.Shared
import spock.lang.Specification

/*
Step1. UploadGoodsDetail仅上传商品信息不需要用户未登录YoRen
Step2. BarCode微信支付成功
Step3. TradeConfirm时PosHub调用YoRen的dealDoneNotice接口，检查是否传paymentMerchantID和paymentUserID值给YoRen系统
 */

class YoRenDealDoneNoticeSpec extends Specification {
    @Shared BarcodeResponse barcodeResponse
    @Shared GoodsDetailRequest goodsDetailRequest
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Shared totalFee = 0.00
    @Shared items, blackItems
    @Delegate RequestDelegate requestDelegate

    def setup(){
        requestDelegate = new RequestDelegate('00062000000', '9Y3SGFCLR2BH4T51', 'http://127.0.0.1:21001', '350909')
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        items = ['6970399927247', '6938888889896'] // 6901028075831 黑名单商品

        when:
        def (GoodsDetailResponse goodsDetailResponse, goodsDetailReqObj) = uploadGoodsRequest(null, outTradeNo, items)
        goodsDetailRequest = goodsDetailReqObj
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
        }
    }

    def "call barcode to payment"(){
        when:
        barcodeResponse = barCodeRequest(pan, outTradeNo, goodsDetailRequest.total_fee / 2)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            ret_code == '00'
            biz_type == '00' // 支付00
            pay_code == paycode
            pay_name == payname
            status == paystatus
        }
        where:
        pan|paycode|payname|paystatus
        '132692326141378115'|'050'|'微信支付'|'1000'
        '283712123251107120'|'051'|'支付宝'|'1000'
    }

    def "call confirm"(){
        given:
        def amount = goodsDetailRequest.total_fee // 总金额
        when: "Call YoRen's DealDoneNotice"
        def paymentConfirmResponse = paymentConfirmRequest([], null, outTradeNo, totalFee, 0, 0)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            status == '1000'
        }
    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        when:
        def paymentRefundResponse = paymentRefundRequest(barcodeResponse.getPay_code(), barcodeResponse.trade_no, barcodeResponse.getTotal_fee())
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            biz_type == '00'
            ret_code == '00'
            status == '2000'
        }
    }
}
