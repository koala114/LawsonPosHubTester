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

    def setupSpec(){
        dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'203118', 'user_id':'20311802',  'pos_id':'01', 'jar_version':'1']
        //dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'https://lawson-poshub.kargotest.com', 'store_id':'208886', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
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
        BarCodeRequest request = createBarCodeRequest('132898863728591235', outTradeNo, 0.01)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '00' // e支付 00
            pay_code == payCode
        }
        where:
        pan|payCode|routeID
        //getUnionpayPan()|'057'|'kargoKH'
        '134883487554134018'|'050'|'kargoKH'
        //'284296728570790602'|'051'|'kargoKH'
        //'69558147050060006018409060007706'|'045'|'kargoKH'
    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        given:
        def client = createLawsonPosHubService(dev, '/traderefund')
        //PaymentRefundRequest request = createPaymentRefundRequest(barcodeResponse.pay_code, barcodeResponse.getTrade_no(), 0.01 )
        PaymentRefundRequest request = createPaymentRefundRequest("050", "203118081438595582", 20.90 )
        when:
        resp = (PaymentRefundResponse) client.execute(request)
        then:
        with(resp){
            status == '2000'
            responseCode == '0000'
            biz_type == '00' // e支付 退款00
        }
    }
}
