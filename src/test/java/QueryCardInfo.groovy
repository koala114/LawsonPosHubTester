import com.kargo.InitService
import com.kargo.LawsonPosHubService
import com.kargo.request.BarCodeRequest
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.QueryCardInfoRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentConfirmResponse
import com.kargo.response.QueryCardInfoResponse
import groovy.json.JsonSlurper
import org.apache.log4j.Logger
import spock.lang.Ignore
import spock.lang.Shared

class QueryCardInfo  extends Helper {
    private static final Logger log = Logger.getLogger(QueryCardInfo.class);
    Date now = new Date()
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared env, items
    @Shared LawsonPosHubService goodsClient, barcodeClient, queryCardClient, tradeconfirmClient
    @Shared BarcodeResponse barcodeResponse = null, barcodeYoRenResponse
    @Shared DBVerifier dbVerifier

    def setupSpec(){
        dbVerifier = new DBVerifier()
        // 初始化 LawsonPosHubService 参数
        env = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://121.43.156.191:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        goodsClient = createLawsonPosHubService(env, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(env, '/barcode')
        queryCardClient = createLawsonPosHubService(env, '/querycardinfo')
        tradeconfirmClient = createLawsonPosHubService(env, '/tradeconfirm')

        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
        // 黑名单商品 6901028075831
        items = ['6920459950180', '2501408063102', '6901028075831'] // 6901028075831 黑名单商品
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, [])
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)

        /* 期望结果 */
        def Leg4 = ['out_trade_no':goodsDetailResponse.getOut_trade_no(), 'transaction_type':'GOODS', 'pan':'', 'upc':null, 'result_cd': '0000', 'execute_method':'UPLOADGOODSDETAIL', 'route_id':null]
        def expectedValue = ['LEG_4':Leg4] // 没有会员号的uploadgoodsdetail只有LEG_4
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
        }
        result.size() == 0
    }

    def "call barcode with YoRen"(){
        given:
        BarCodeRequest request = createBarCodeRequest(memberNo, outTradeNo, totalFee)
        when:
        barcodeYoRenResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':barcodeYoRenResponse.getTrade_no(), 'transaction_type':'REDMP', 'pan':memberNo, 'upc':null, 'result_cd': null, 'execute_method':'BARCODE', 'route_id':null, 'http_body': request.toString()]
        def Leg3 = Leg1 + ['result_cd': '100', 'route_id':'yoren', 'pay_method':'038', 'rrn':barcodeYoRenResponse.getOutid()] - ['http_body': request.toString()]
        def Leg4 = Leg3 +  ['result_cd': '0000', 'http_body': barcodeYoRenResponse.toString()]
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(barcodeYoRenResponse){
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == barcodeYoRenResponse.getUser_info().getCode()// YoRen测试环境会员号
        }
        result.size() == 0
        where:
        memberNo = '391003870323196996'
    }

    def "call uploadgoodsdetail with member_no"(){ // 新增一个OrderItem, "barcode":"050733" 会返回
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items, [])
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call querycardinfo"(){
        given:
        QueryCardInfoRequest request = createQueryCardInfoRequest(trackInfo)
        when:
        QueryCardInfoResponse queryCardInfoResponse = (QueryCardInfoResponse) queryCardClient.execute(request)
        /* 期望结果 */
        //def Leg1 = ['out_trade_no':queryCardInfoResponse, 'transaction_type':'CHK_CARD_STATUS', 'pan':trackInfo, 'upc':null, 'result_cd': null, 'execute_method':'QUERYCARDINFO', 'route_id':null]
        //def Leg2 = ['out_trade_no':paras.out_trade_no, 'transaction_type':'CHK_CARD_STATUS', 'pan':trackInfo, 'upc':'6955814701848', 'result_cd': null, 'execute_method':'QUERYCARDINFO', 'route_id':'kargoKH']
        //def Leg3 = Leg2 + ['result_cd': '0000', 'transaction_type':'BALIQ', 'pay_method':'045', 'rrn':queryCardInfoResponse.getOutid()]
        //def Leg4 = Leg3 + ['transaction_type':'CHK_CARD_STATUS']
        //def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        //def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(queryCardInfoResponse){
            responseCode == '0000'
            card_no == '69558147018480006016844010007349'
        }
        //result.size() == 0
        where:
        //trackInfo = '996016844010007349^6955814701848;                                           '
        trackInfo = '996016844010007340^6955814701848;                                           '

    }

    def "call barcode payment"(){
        given:
        BarCodeRequest request = createBarCodeRequest(pan, outTradeNo, 2.00)
        when:
        def barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':barcodeResponse.getTrade_no(), 'transaction_type':'REDMP', 'pan':pan, 'upc':null, 'result_cd': null, 'execute_method':'BARCODE', 'route_id':null]
        def Leg3 = Leg1 + ['upc':upc, 'result_cd': '0000', 'route_id':'kargoKH', 'pay_method':payCode, 'rrn':barcodeResponse.getOutid()]
        def Leg4 = Leg3
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(barcodeResponse){
            responseCode == respCode
            biz_type == bizType // 卡支付03
            pay_code == payCode
            balance >= 0.0
        }
        result.size() == 0
        where:
        pan | payCode | respCode | respMessage | bizType |upc
        //'133072400089601371' | null | '8044' | 'GoodsList contains blacklist goods' | null
        //getUnionpayPan()|'057'|'0000'|''|'00' |'0000000000000'
        '996016844010007341^6955814701848;                                           ' | '045' | '0000' | 'Txn completed successfully' | '03' |'6955814701848'
    }

    def "call barcode ePay"(){
        given:
        BarCodeRequest request = createBarCodeRequest(pan, outTradeNo, totalFee - 2.00)
        when:
        def barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        /* 期望结果 */
        def Leg1 = ['stan':barcodeResponse.getTrade_no(), 'transaction_type':'REDMP', 'pan':pan, 'upc':null, 'result_cd': null, 'execute_method':'BARCODE', 'route_id':null]
        def Leg3 = Leg1 + ['upc':'0000000000000', 'result_cd': '0000', 'route_id':'kargoUH', 'pay_method':payCode, 'rrn':barcodeResponse.getOutid(), 'rps_id':barcodeResponse.getSys_trade_no()]
        def Leg4 = Leg3
        def expectedValue = ['LEG_1':Leg1, 'LEG_3':Leg3, 'LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(barcodeResponse){
            responseCode == respCode
            biz_type == '00' // e支付00
            pay_code == payCode
        }
        result.size() == 0
        where:
        pan | payCode | respCode
        getUnionpayPan()|'057'|'0000'
    }

    def "call confirm"(){
        given:
        //传member_no调用YoRen的settlementTransactions返回totalPoint。不传member_no调用YoRen的dealDoneNotice没有返回totalPoint
        PaymentConfirmRequest request = createPaymentConfirmRequest([], barcodeYoRenResponse.getUser_info().getCode(), outTradeNo, totalFee, totalFee, 0.0)
        when:
        def paymentConfirmResponse = (PaymentConfirmResponse) tradeconfirmClient.execute(request)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            totalPoint > 0
            status == '1000'
        }
    }

    def cleanup() {
        dbVerifier.failedResults = []
    }
}
