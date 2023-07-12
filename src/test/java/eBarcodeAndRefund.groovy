import com.kargo.InitService
import com.kargo.LawsonPosHubService
import com.kargo.request.BarCodeRequest
import com.kargo.request.HealthCheckRequest
import com.kargo.request.PaymentRefundRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentRefundResponse
import org.apache.log4j.Logger
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

class eBarcodeAndRefund extends Helper {
    Date now = new Date()
    private static final Logger log = Logger.getLogger(eBarcodeAndRefund.class);
    def resp
    static String outTradeNo
    static String storeCode
    @Shared
    BarcodeResponse barcodeResponse = null
    @Shared LawsonPosHubService barcodeClient

    def pan = ['6235555383837928945':'057'] // 支付方式 ['银联支付':'057'] ['建行支付':'027']  ['游人预付费':'090']
    static def total_fee
    static def dev
    static DBVerifier dbVerifier

    def setupSpec(){
        dbVerifier = new DBVerifier()
        //dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://10.100.70.181:7001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://121.43.156.191:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //dev = ['mid':'98621000008', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        //dev = ['mid':'98621000008', 'storeCode':'203118', 'user_id':'20311801',  'pos_id':'01', 'sessionKey':'LAWSONJZ2NJKARGO']
        InitService.init()
        total_fee = 2.33
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    }

    def "call barcode to payment"(){
        given:
        Date now = new Date()
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        BarCodeRequest request = createBarCodeRequest(pan, outTradeNo, 0.01)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':barcodeResponse.getTrade_no(), 'transaction_type':'REDMP', 'pan':pan, 'upc':null, 'result_cd': null, 'execute_method':'BARCODE', 'route_id':null]
        def Leg3 = Leg1 + ['upc':'0000000000000', 'result_cd': '0000', 'route_id':routeID, 'pay_method':payCode, 'rrn':barcodeResponse.getOutid()]
        def Leg4 = Leg3
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '00' // e支付 00
            pay_code == payCode
        }
        result.size() == 0
        where:
        pan|payCode|routeID
        //getUnionpayPan()|'057'|'kargoKH'
        '132883487554134018'|'050'|'kargoKH'

    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        given:
        def client = createLawsonPosHubService(dev, '/traderefund')
        //PaymentRefundRequest request = createPaymentRefundRequest(barcodeResponse.pay_code, barcodeResponse.getOut_trade_no() + "99", 0.33 )
        PaymentRefundRequest request = createPaymentRefundRequest("050", "4214670112301016", 0.01 )
        when:
        resp = (PaymentRefundResponse) client.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':resp.getTrade_no(), 'transaction_type':'REFUND', 'pan':pan.keySet()[0], 'upc':null, 'result_cd': null, 'execute_method':'TRADEREFUND', 'route_id':null]
        def Leg3 = Leg1 + ['upc':'0000000000000', 'result_cd': '0000', 'route_id':'kargoUH', 'pay_method':pan.values()[0], 'rrn':resp.getOutid()]
        def Leg4 = Leg3
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(resp){
            responseCode == '0000'
            biz_type == '00' // e支付 退款00
            status == '2000'
        }
        result.size() == 0
    }
}
