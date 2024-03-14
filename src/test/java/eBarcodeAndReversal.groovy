import com.kargo.LawsonPosHubService
import com.kargo.request.PaymentReverseRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentReverseResponse
import spock.lang.Shared

class eBarcodeAndReversal extends Helper {
    //http://121.43.156.191:21001 http://10.100.70.129:7001'http://47.97.19.94:21001' https://lawson-poshub.kargotest.com
    @Shared String outTradeNo
    @Shared BarcodeResponse barcodeResponse = null
    @Shared LawsonPosHubService barcodeClient, cancelClient
    @Shared def totalFee

    def setupSpec(){
        totalFee = 0.01
        def env = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        barcodeClient = createLawsonPosHubService(env, '/barcode')
        cancelClient = createLawsonPosHubService(env, '/tradecancel')

        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))    }

    def "call barcode to payment"(){
        given:
        def request = createBarCodeRequest(pan, outTradeNo, totalFee)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        then:
        with(barcodeResponse){
            responseCode == '0000'
            ret_code == '00'
            biz_type == '00' // 支付00
            status == '1000'
            pay_code == payCode
            outid != null
        }
        where:
        pan | payCode
        //getUnionpayPan() | '057'
        '013990005507383097321210003745111'|'014'
    }

    def "call tradecancel for payment"(){
        given:
         /* 期望结果 */
        //def Leg1 = ['stan':barcodeResponse.trade_no, 'transaction_type':'RVSAL', 'upc':null, 'result_cd': null, 'execute_method':'TRADECANCEL', 'route_id':null]
        //def Leg3 = ['stan':barcodeResponse.trade_no, 'transaction_type':'RVSAL', 'upc':'0000000000000', 'result_cd': '0000', 'execute_method':'TRADECANCEL', 'route_id':'kargoUH',
                //'pay_method':barcodeResponse.pay_code]
        //def Leg4 = ['stan':barcodeResponse.trade_no, 'transaction_type':'RVSAL', 'upc':'0000000000000', 'result_cd': '0000', 'execute_method':'TRADECANCEL', 'route_id':'kargoUH',
                    //'pay_method':barcodeResponse.pay_code]
        //def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]

        //PaymentReverseRequest request = createPaymentReverseRequest(barcodeResponse.out_trade_no, barcodeResponse.trade_no, barcodeResponse.pay_code)
        PaymentReverseRequest request = createPaymentReverseRequest('208888240314153001', '20888824031415300102', '051')
        when:
        def resp = (PaymentReverseResponse) cancelClient.execute(request)
        //def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(resp){
            responseCode == '0000'
            ret_code == '00'
            biz_type == '00'
            status == '3000'
        }
        //result.size() == 0
    }
}
