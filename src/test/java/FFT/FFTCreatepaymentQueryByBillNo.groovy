package FFT

import Core.RequestDelegate
import com.kargo.response.BarcodeResponse
import com.kargo.response.CreatePaymentResponse
import com.kargo.response.GoodsDetailResponse
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

/*
1. BARCODE ----> get "searchType":"0" from FFT
                call bill.conf.get.service ---->  request "billNo":"0128353227" extract from "3924020128353227000901803".substring(bill_no_start -1, bill_no_start -1 + bill_no_len)
                                           call query.by.billno.service
2. UPLOADGOODSDETAIL
3. CREATEPAYMENT
4. TRADECONFIRM
 */

class FFTCreatepaymentQueryByBillNo extends Specification {
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    @Shared String pan = '3924020128353227000901803' // {"searchType":"0","searchTypeName":"用户代码","validationExp":"^\\d{10}$","remark":null,"needPwd":"N"}
    @Delegate RequestDelegate requestDelegate

    def setup(){
        def env = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']
        requestDelegate = new RequestDelegate(env)
    }

    def "call barcode to payment"(){
        when:"Since searchType is 0 PosHub will send request of query.by.billno.service via com.shfft.oap.client.response.QueryByBillNoResponse"
        barcodeResponse = barCodeRequest(pan, outTradeNo)
        then:
        with(barcodeResponse){
            responseCode == '1005'
            responseMessage == '不需要查询，请直接条码销账'
            ret_code == '1005'
            billBizInfos[0].bill_amt == 706.3
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