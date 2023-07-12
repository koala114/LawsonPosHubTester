import com.kargo.InitService
import com.kargo.LawsonPosHubService
import com.kargo.request.PaymentReverseRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentReverseResponse
import spock.lang.Shared

class eBarcodeReversal extends Helper {
    Date now = new Date()
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo
    @Shared dev, prd, items
    @Shared LawsonPosHubService goodsClient, barcodeClient, traderefundClient
    @Shared DBVerifier dbVerifier
    @Shared totalFee = 1.34

    def setupSpec(){
        InitService.init()
        dbVerifier = new DBVerifier()
        dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'https://lawson-poshub.kargotest.com', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    }

    def "call barcode to payment"(){
        given:
        def barcodeClient = createLawsonPosHubService(dev, '/barcode')
        def request = createBarCodeRequest(getUnionpayPan(), outTradeNo, totalFee)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':barcodeResponse.getTrade_no(), 'transaction_type':'REDMP', 'upc':null, 'result_cd': null, 'execute_method':'BARCODE', 'route_id':null]
        def Leg3 = Leg1 + ['upc':'0000000000000', 'result_cd': '0000', 'route_id':'kargoUH', 'rrn':barcodeResponse.getOutid(), 'rps_id':barcodeResponse.getSys_trade_no()]
        def Leg4 = Leg3
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)

        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '00' // 支付00
            status == '1000'
            pay_code == '057'
            outid != null
        }
        result.size() == 0
    }

    def "call tradecancel for payment"(){
        given:
        /* 期望结果 */
        def stan = barcodeResponse.getTrade_no()
        def Leg1 = ['stan':stan, 'transaction_type':'RVSAL', 'upc':null, 'result_cd': null, 'execute_method':'TRADECANCEL', 'route_id':null]
        def Leg3 = ['stan':stan, 'transaction_type':'RVSAL', 'upc':'0000000000000', 'result_cd': '0000', 'execute_method':'TRADECANCEL', 'route_id':'kargoUH',
                    'pay_method':barcodeResponse.pay_code]
        def Leg4 = ['stan':stan, 'transaction_type':'RVSAL', 'upc':'0000000000000', 'result_cd': '0000', 'execute_method':'TRADECANCEL', 'route_id':'kargoUH',
                    'pay_method':barcodeResponse.pay_code]
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]

        def tradecancelClient = createLawsonPosHubService(dev, '/tradecancel')
        PaymentReverseRequest request = createPaymentReverseRequest(outTradeNo, barcodeResponse.getTrade_no(), barcodeResponse.getPay_code())
        when:
        PaymentReverseResponse paymentReverseResponse = (PaymentReverseResponse) tradecancelClient.execute(request)
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(paymentReverseResponse){
            responseCode == '0000'
            biz_type == '00'
            status == '3000'
        }
        result.size() == 0
    }
}