import com.kargo.LawsonPosHubService
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentConfirmResponse
import spock.lang.Shared

class YoRenCouponPoints050 extends Helper {
    Date now = new Date()
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared LawsonPosHubService goodsClient, barcodeClient, traderefundClient
    @Shared DBVerifier dbVerifier
    @Shared point_amount = 2.0 // 积分支付2元
    @Shared dev, prd, items, blackItems

    def DynamicId = ['memberNo':'391715837484026315'] // YoRen会员号 1900267772339
    def YoRenCardNo = '391003840373136998'
    def pan = ['6224434346553149274':'057'] // 支付方式 ['银联支付':'057'] ['建行支付':'027']

    def setupSpec(){
        dbVerifier = new DBVerifier()
        // 初始化 LawsonPosHubService 参数
        dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://121.43.156.191:21001', 'store_id':'201946', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        prd = ['mid':'00062000000', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        //items = ['6920259700053'] // 6901028075831 黑名单商品
        items = []
        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"2501858005102\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem3 = jsonSlurper.parseText("{\"barcode\":\"050733\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":8.00,\"discount_quantity\":2.0}],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"三得利-196℃桃子配制酒\",\"quantity\":2,\"row_no\":3,\"sell_price\":15.9,\"total_amount\":31.80,\"total_discount\":8.00}");
        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2), new OrderItem(blackItem3)]
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')

        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, blackItems)
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
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        def request = createBarCodeRequest(DynamicId['memberNo'], outTradeNo, totalFee)

        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == '1900213189174' //YoRen测试环境会员号
        }
    }

    def "call uploadgoodsdetail with member_no"(){
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(DynamicId['memberNo'], outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)

        /* 期望结果 */
        def Leg4 = ['out_trade_no':goodsDetailResponse.getOut_trade_no(), 'transaction_type':'GOODS', 'pan':'', 'upc':null, 'result_cd': '0000', 'execute_method':'UPLOADGOODSDETAIL', 'route_id':null]
        def expectedValue = ['LEG_4':Leg4]
        def result =  dbVerifier.validateOltp(expectedValue)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
        }
        result.size() == 0
    }

    def "call barcode to payment"(){
        given:
        totalFee = totalFee - barcodeResponse.getCoupons().save_payment - point_amount // e支付金额=支付金额 - 券金额 - 积分
        def request = createBarCodeRequest(pan, outTradeNo, totalFee)
        when:
        def barcodePaymentResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodePaymentResponse){
            responseCode == '0000'
            biz_type == '00' // 支付00
            pay_code == paycode
        }
        where:
        pan|paycode
        //'911167728145959244'|'052' // QQ支付
        //'134280065829410384'|'050' // 微信支付
        //'287837647376519672'|'051' // 支付宝
        //'77FF03120721218131'|'004' // 索迪斯
        getUnionpayPan()|'057'
    }

    def "call confirm"(){
        given:
        def tradeconfirmClient = createLawsonPosHubService(dev, '/tradeconfirm')
        //传member_no调用YoRen的settlementTransactions返回totalPoint。不传member_no调用YoRen的dealDoneNotice没有返回totalPoint
        def code = barcodeResponse.getUser_info().code
        PaymentConfirmRequest request = createPaymentConfirmRequest(barcodeResponse.getCoupons()*.coupon_code, code, outTradeNo, totalFee, point_amount, 1.0)
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