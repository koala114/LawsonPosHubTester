package KargoISV

import Core.RequestDelegate
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentRefundRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentRefundResponse
import groovy.sql.Sql
import spock.lang.Shared
import spock.lang.Specification

class UploadGoodsDetail4KargoSpec extends Specification {
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared Sql sql
    @Shared items
    @Shared whiteProduct = '6921168509256'
    @Shared GoodsDetailRequest goodsDetailRequest
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Delegate RequestDelegate requestDelegate

    def setup() {
        def dev = ['mid': '00062000000', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'http://127.0.0.1:21001', 'store_id': '208888']
        //def dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://47.101.50.215:21001', 'store_id':'208888', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']
        requestDelegate = new RequestDelegate('00062000000', '9Y3SGFCLR2BH4T51', 'http://127.0.0.1:21001', '208888')
    }

    def "call uploadgoodsdetail without member_no"() {
        given:
        items = ['6920459950180', '2501408063102', '6902538008548'] // 6901028075831 黑名单商品

        when:
        def (GoodsDetailResponse goodsDetailResponse, goodsDetailReqObj) = uploadGoodsRequest(null, outTradeNo, items)
        goodsDetailRequest = goodsDetailReqObj
        then:
        with(goodsDetailResponse) {
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
        items = items << whiteProduct // 加个白名单商品
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
        sql = KargoHubDBConnector();
        def amount = goodsDetailRequest.total_fee // 总金额
        when:
        barcodeResponse = barCodeRequest(pan, outTradeNo, amount)
        def goods = sql.rows("select request_goods_info from txn_goods_info_new where stan=?", [barcodeResponse.trade_no])
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '00' // 电子支付00
            pay_code == payCode
            total_fee == amount
            status == '1000'
        }
        and: "Check if white product is send to kargo isv for wechat"
        goods[0]['request_goods_info'].contains(whiteProduct)
        where:
        pan|payCode
        '132692326141378115'|'050'
        /* '651680771898912912'|'026'|'驾续多'|'10000'
         '77FF03120721218131'|'004'|'索迪斯'|'1000'
         '0100505719322301014'|'007'|'数字人民币'|'1000'
         '132692326141378115'|'050'|'微信支付'|'1000'
         '810086744359766447'|'031'|'移动和包'|'1000'
         '283712123251107120'|'051'|'支付宝'|'1000'
         '6226994810298330706'|'057'|'银联支付'|'1000'
         */
        //'6240105666367315102'|'027'|'建行支付'|'1000'
        //'LS210032839440110269000'|'100'
        //'0100803882792891721'|'007' // 微信支付
        //'77FF03120721218131'|'004' // 索迪斯
        //'https://www.apple.com.cn'|'023'//中百抖音
        //'6220204222068652830'|'057'//中百抖音
        //'810086722461596869'|'031'|'移动和包'|'1000'
    }

    def "call confirm"(){
        given:
        def amount = goodsDetailRequest.total_fee // 总金额
        and:
        def orderItemList = goodsDetailRequest.getOrder_items()
        when:
        def paymentConfirmResponse = paymentConfirmRequest([], barcodeYoRenResponse.getUser_info().code, outTradeNo, amount, 0, 0, orderItemList)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            status == '1000'
        }
    }

    def "call traderefund for YoRen"(){// YoRen退积分old_trade_no用out_trade_no
        when:
        def paymentRefundResponse = paymentRefundRequest(barcodeYoRenResponse.getUser_info().getCode(), barcodeYoRenResponse.out_trade_no)
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            responseMessage == '交易成功完成'
            biz_type == '01' // YoRen 退款01
            ret_code == '00'
            status == '2000'
        }
    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        when:
        def paymentRefundResponse = paymentRefundRequest(barcodeResponse.pay_code, barcodeResponse.trade_no, barcodeResponse.total_fee)
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            responseMessage == '交易成功完成'
            biz_type == '00'
            ret_code == '00'
            status == '2000'
        }
    }

    def cleanupSpec() {
        sql.close()
    }
}