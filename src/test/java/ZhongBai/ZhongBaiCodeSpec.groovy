package ZhongBai

import Core.RequestDelegate
import com.kargo.LawsonPosHubService
import com.kargo.request.GoodsDetailRequest
import com.kargo.response.BarcodeResponse
import com.sun.scenario.effect.impl.sw.java.JSWBlend_SRC_OUTPeer
import groovy.sql.Sql
import spock.lang.*
import com.kargo.response.GoodsDetailResponse

/*
测试中百券
1、Barcode 会请求中百api 101，其中参数useFlag=0，即试算
2、TradeConfirm 会请求中百api 101，其中参数useFlag=0，即核销
 */
class ZhongBaiCodeSpec extends Specification {
    @Shared totalFee = 0.00
    @Shared Sql sql
    @Shared items, blackItems
    @Shared GoodsDetailRequest goodsDetailRequest
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Delegate RequestDelegate requestDelegate

    def setup() {
        def dev = ['mid': '00062000000', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'http://127.0.0.1:21001', 'store_id': '360320', 'user_id': '36032001', 'pos_id': '01', 'jar_version': '1']
        //def dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://47.101.50.215:21001', 'store_id':'208888', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']
        requestDelegate = new RequestDelegate(dev)
    }

    def "call uploadgoodsdetail without member_no"() {
        given:
        items = ['6920459950180', '2501408063102', '6902538008548'] // 6901028075831 黑名单商品
        blackItems = []

        when:
        def (GoodsDetailResponse goodsDetailResponse, goodsDetailReqObj) = uploadGoodsRequest(null, outTradeNo, items, blackItems)
        goodsDetailRequest = goodsDetailReqObj
        then:
        with(goodsDetailResponse) {
            pay_code == '038'
            responseCode == '0000'
        }
    }

    def "call barcode to payment"() {
        given:
        //totalFee = goodsDetailRequest.total_fee
        totalFee = 0.01
        when: "Request ZhongBai api service 101 with useFlag=0"
        def barcodeResponse = barCodeRequest('666613297933851', outTradeNo, totalFee)
        then:
        with(barcodeResponse) {
            responseCode == '0000'
            responseMessage == '交易成功完成'
            ret_code == '00'
            biz_type == '01' // 中百券01
            pay_code == paycode
            pay_name == payname
            status == '1000'
        }
        where:
        pan               | paycode | payname
        '666613297933851' | '006'   | '中百券'
    }

    def "call confirm"() {
        given:
        sql = DBConnector();
        def amount = goodsDetailRequest.total_fee // 总金额
        when: "Request ZhongBai api service 101 with useFlag=1"
        def paymentConfirmResponse = paymentConfirmRequest([], null, outTradeNo, totalFee, 0, 0)
        def payCode = sql.rows('select route_id, pay_method from oltp_txn_log_digital where out_trade_no=:otn and leg=:leg and execute_method=:method', otn:paymentConfirmResponse.out_trade_no, leg:'LEG_3', method:'TRADECONFIRM')
        then:
        with(paymentConfirmResponse) {
            responseCode == '0000'
            status == '1000'
        }
        and: "Check if tradeconfirm is send to ZHONGBAI"
        payCode[0]['pay_method'] == '006'
        payCode[0]['route_id'] == 'zhongbaii'
    }

    def cleanupSpec() {
        sql.close()
    }
}