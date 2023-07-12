import com.kargo.LawsonPosHubService
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentConfirmResponse
import spock.lang.*
import com.kargo.request.GoodsDetailRequest
import com.kargo.response.GoodsDetailResponse

class BlackItemFilter extends Helper {
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared env, prd, items, blackItems
    @Shared
    LawsonPosHubService goodsClient, barcodeClient, traderefundClient

    def setupSpec() {
        // 初始化 LawsonPosHubService 参数
        env = ['mid': '00062000000', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'http://121.43.156.191:21001', 'store_id': '421196', 'user_id': '00000002', 'pos_id': '01', 'jar_version': '1']
        //env = ['mid': '00062000000', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'http://10.100.70.120:7001', 'store_id': '208888', 'user_id': '00000002', 'pos_id': '01', 'jar_version': '1']
        prd = ['mid': '00062000000', 'sessionKey': 'LAWSONJZ2NJKARGO', 'kargoUrl': 'http://47.97.19.94:21001', 'store_id': '203118', 'user_id': '20311801', 'pos_id': '01', 'jar_version': '1']

        goodsClient = createLawsonPosHubService(env, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(env, '/barcode')
        def client = createLawsonPosHubService(env, '/tradeconfirm')

        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102'] // 6901028075831 黑名单商品
        //items = [] // 6901028075831 黑名单商品

        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"6901028055000\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"56\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"2501856002103\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"44\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")

        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2)]
        //blackItems = []
    }

    def "call uploadgoodsdetail without member_no"() {
        given:
        // 商品明细
        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, blackItems)
        when:
        totalFee = request.getTotal_fee()
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse) {
            pay_code == '038'
            responseCode == '0000'
        }
    }

    def "call barcode with YoRen"() {
        given:
        def request = createBarCodeRequest(memberNo, outTradeNo, totalFee)

        when:
        barcodeYoRenResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeYoRenResponse) {
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == members['13818595461'] //YoRen测试环境会员号
        }
        where:
        memberNo = '391820104291798645'
    }

    def "call uploadgoodsdetail with member_no"() {
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse) {
            pay_code == '038'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    @Unroll
    def "call barcode to payment"() {
        given:
        //def paras = ['currency':'CNY', 'dt':now.format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC')), 'dynamic_id': pan.keySet()[0], 'out_trade_no': outTradeNo, 'trade_no':outTradeNo + '02','total_fee':total_fee, 'fee_type':0] + common
        def request = createBarCodeRequest(pan, outTradeNo, totalFee)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse) {
            responseCode == respCode
            responseMessage == respMessage
            biz_type == bizType // 支付00
            pay_code == payCode
        }
        where:
        pan | payCode | respCode | respMessage | bizType
       //'133072400089601371' | null | '8045' | '商品在黑名单中，请清除该商品' | null
        '133072400089601371' | null | '8044' | 'GoodsList contains blacklist goods' | null
        //getUnionpayPan() | '057' | '0000' | 'Txn completed successfully' | '00' // 支付方式 ['银联支付':'057'] ['建行支付':'027']
    }

    @Ignore
    def "call confirm"() {
        given:
        PaymentConfirmRequest request = createPaymentConfirmRequest([], barcodeYoRenResponse.user_info.code, outTradeNo, totalFee, 0, 0)
        when:
        def paymentConfirmResponse = (PaymentConfirmResponse) client.execute(request)
        then:
        with(paymentConfirmResponse) {
            responseCode == '0000'
            totalPoint > 0
            status == '1000'
        }
    }
}