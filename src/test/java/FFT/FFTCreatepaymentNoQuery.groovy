package FFT

import Core.RequestDelegate
import com.kargo.response.BarcodeResponse
import com.kargo.response.CreatePaymentResponse
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
        def env = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']
        requestDelegate = new RequestDelegate(env)
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