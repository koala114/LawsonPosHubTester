import com.kargo.LawsonPosHubService
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentRefundRequest
import com.kargo.request.UnFreezeRequest
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentRefundResponse
import com.kargo.response.PaymentReverseResponse
import com.kargo.response.UnFreezeResponse
import spock.lang.Ignore
import spock.lang.Shared

class YoRenCard90 extends Helper {
    Date now = new Date()
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared dev, prd, items, blackItems, memberNo
    @Shared LawsonPosHubService goodsClient, barcodeClient, traderefundClient

    def setupSpec(){
        // 初始化 LawsonPosHubService 参数
        dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'https://lawson-poshub.kargotest.com', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://10.100.70.129:7001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        prd = ['mid':'00062000000', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102', '6901028075831'] //  黑名单商品
        blackItems = []
    }

    def "call barcode with YoRen"(){
        given:
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        def request = createBarCodeRequest(memberNo, outTradeNo, totalFee)

        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == code
        }
        where:
        memberNo|code
        '391203543595434664'|'1900267772339' //YoRen测试环境会员号
    }

    def "call uploadgoodsdetail with member_no"(){ // 新增一个OrderItem, "barcode":"050733" 会返回
        given:
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call unfreeze"(){
        given:
        def unfreezeClient = createLawsonPosHubService(dev, '/unfreeze')
        UnFreezeRequest request = createUnFreezeRequest(outTradeNo, barcodeResponse.user_info.code)
        when:
        UnFreezeResponse unfreezeResponse = (UnFreezeResponse) unfreezeClient.execute(request)
        then:
        with(unfreezeResponse){
            ret_code == '00'
        }
    }

    def "recall uploadgoodsdetail with member_no"(){ // 新增一个OrderItem, "barcode":"050733" 会返回
        given:
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call barcode with YoRen Card"(){
        given:
        def request = createBarCodeRequest(yorenCardNo, outTradeNo, totalFee)

        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == bizType
            pay_code == payCode
            status == '1000'
            ['1900267772339'].contains(user_info.code)
        }
        where:
        yorenCardNo|payCode|bizType
        '381412007190869322'|'090'|'00'
    }

    @Ignore
    def "call tradeCancel with YoRen Card"(){
        given:
        def tradeCancelClient = createLawsonPosHubService(dev, '/tradecancel')
        def request = createPaymentReverseRequest(outTradeNo, barcodeResponse.trade_no, barcodeResponse.pay_code)

        when:
        def tradeCancelResponse = (PaymentReverseResponse) tradeCancelClient.execute(request)
        then:
        with(tradeCancelResponse){
            responseCode == status
        }
        where:
        status = '3000'
    }

    def "call tradeRefund with YoRen Card"(){
        given:
        def traderefundClient = createLawsonPosHubService(dev, '/traderefund')
        PaymentRefundRequest request = createPaymentRefundRequest(barcodeResponse.getPay_code(), barcodeResponse.trade_no, barcodeResponse.getTotal_fee());
        when:
        def paymentRefundResponse = (PaymentRefundResponse) traderefundClient.execute(request)
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            biz_type == '00' // e支付 退款00
            ret_code == '00'
            status == '2000'
        }
    }
}
