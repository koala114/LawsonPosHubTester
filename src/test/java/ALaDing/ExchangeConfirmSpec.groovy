package ALaDing

import Core.RequestDelegate
import com.kargo.request.GoodsDetailRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.GoodsDetailResponse
import spock.lang.Specification
import spock.lang.Ignore
import spock.lang.Shared

class ExchangeConfirmSpec extends Specification {
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared GoodsDetailRequest goodsDetailRequest
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Shared totalFee = 0.00
    @Shared items, blackItems
    @Delegate RequestDelegate requestDelegate

    def setup(){
        requestDelegate = new RequestDelegate('00062000000', '9Y3SGFCLR2BH4T51', 'http://127.0.0.1:21001', '203118')
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        items = ['6920459950180', '2501408063102', '6902538008548'] // 6901028075831 黑名单商品
        blackItems = []

        when:
        def (GoodsDetailResponse goodsDetailResponse, goodsDetailReqObj) = uploadGoodsRequest(null, outTradeNo, items)
        goodsDetailRequest = goodsDetailReqObj
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
        }
    }

    def "call barcode with YoRen"(){
        given:
        def amount = goodsDetailRequest.total_fee // 总金额
        when:
        barcodeYoRenResponse = barCodeRequest(memberNo, outTradeNo, amount)
        then:
        with(barcodeYoRenResponse){
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == barcodeYoRenResponse.getUser_info().getCode()// YoRen测试环境会员号
        }
        where:
        memberNo = '391003870323196996'
    }

    def "call uploadgoodsdetail with member_no"(){
        given:
        when:
        def (GoodsDetailResponse goodsDetailResponse, goodsDetailReqObj) = uploadGoodsRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items)
        goodsDetailRequest = goodsDetailReqObj
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call barcode to payment"(){
        given:
        def amount = goodsDetailRequest.total_fee // 总金额
        when:
        barcodeResponse = barCodeRequest(pan, outTradeNo, amount)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '01' // 阿拉丁是01
            pay_code == payCode
            total_fee == 10.00 //挡板返回该阿拉订10元券
        }

        where:
        // 支付方式 ['银联支付':'057'] ['建行支付':'027'] ['阿拉丁':'032']
        pan|payCode||fee
        '899663724030717450049000000000010005'|'032'||totalFee //第一次用券订单总金额20.9
        //'899320324020614340269000000000010006'|'032'||totalFee - 10.0 // 第二次用券是订单总金额 - 阿拉订返回的券金额10元 = 10.9
    }

    def "call exchangeconfirm"() {
        given:
        // 兑换业务传券号；撤销业务不传券号也需要传券金额payAmt
        def coupons = [["code":"899663724030717450049000000000010005", "amt": 10.0]]
        def amount = goodsDetailRequest.total_fee // 总金额
        when:
        ExchangeConfirmResponse exchangeConfirmResp = exchangeConfirmRequest(outTradeNo, coupons, 10.0, amount)
        then:
        with(exchangeConfirmResp){
            responseCode == '0000'
        }
    }

    def "call confirm"(){
        given:
        def amount = goodsDetailRequest.total_fee // 总金额
        when:
        def paymentConfirmResponse = paymentConfirmRequest([], barcodeYoRenResponse.getUser_info().code, outTradeNo, totalFee, 0, 0)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            totalPoint > 0
            status == '1000'
        }
    }
}
