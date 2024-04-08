package FFT

import Core.RequestDelegate
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.CreatePaymentResponse
import com.kargo.response.GoodsDetailResponse
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

/*
1. BARCODE
2. UPLOADGOODSDETAIL
3. CREATEPAYMENT
4. TRADECONFIRM
 */

class FFTCreatepaymentNoQuery extends Specification {
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Shared String pan = '0310119064002619240201000001879047' // 该码在fft_code表中匹配到payment_type=1时直接用createpayment接口进行核销，不用去fft查询账单
    @Delegate RequestDelegate requestDelegate

    def setup(){
        requestDelegate = new RequestDelegate('00062000000', '9Y3SGFCLR2BH4T51', 'http://127.0.0.1:21001', 'store_id':'203118')
    }

    def "call barcode to payment"(){
        when:"PosHub will not send request of query.by.billno.service"
        barcodeResponse = barCodeRequest(pan, outTradeNo)
        then:
        with(barcodeResponse){
            responseCode == '1005'
            responseMessage == '不需要查询，请直接条码销账'
            ret_code == '1005'
            billBizInfos[0].bill_amt == 187.9
            biz_type == '04' // 付费通账单查询
        }
    }

    def "call uploadgoodsdetail"(){
        given:
        def jsonSlurper = new JsonSlurper()
        def item = jsonSlurper.parseText("{\"barcode\":\"2501311138102\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"60\",\"kagou_sign\":\"N\",\"name\":\"嘉定水\",\"quantity\":1,\"row_no\":1,\"sell_price\":20.7,\"total_amount\":20.7,\"total_discount\":0}");
        OrderItem orderItem = new OrderItem(*:item)

        when:
        GoodsDetailResponse goodsDetailResponse =  uploadGoodsRequest(outTradeNo,  orderItem)
        then:
        with(goodsDetailResponse){
            responseCode == '0000'
            responseMessage == '交易成功完成'
            ret_code == '00'
            pay_code == '038'
        }
    }

    def "call createPaymentRequest"(){
        given:
        def jsonSlurper = new JsonSlurper()
        def item = jsonSlurper.parseText("{\"barcode\":\"2501311138102\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"60\",\"kagou_sign\":\"N\",\"name\":\"嘉定水\",\"quantity\":1,\"row_no\":1,\"sell_price\":20.7,\"total_amount\":20.7,\"total_discount\":0}");

        when:
        CreatePaymentResponse createPaymenResponse = createPaymentRequest(pan, outTradeNo, barcodeResponse.billBizInfos)
        then:
        with(createPaymenResponse){
            status == '01' // 01成功、02失败
            ret_code == '00'
            responseCode == '0000'
        }
    }

    def "call confirm"(){
        when:
        def paymentConfirmResponse = paymentConfirmRequest([], null, outTradeNo, barcodeResponse.billBizInfos[0].bill_amt, 0, 0)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            status == '1000'
        }
    }
}
