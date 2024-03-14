import com.kargo.LawsonPosHubService
import com.kargo.internal.GenerateQRCode
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.QRCodeRequest
import com.kargo.request.UnFreezeRequest
import com.kargo.request.coupon.CouponCaluRequest
import com.kargo.request.coupon.CouponCancelRequest
import com.kargo.request.coupon.CouponConfirmRequest
import com.kargo.request.coupon.CouponRefundReqeust
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.QRCodeRequestResponse
import com.kargo.response.UnFreezeResponse
import com.kargo.response.coupon.CouponCaluResponse
import com.kargo.response.coupon.CouponCancelResponse
import com.kargo.response.coupon.CouponConfirmResponse
import com.kargo.response.coupon.CouponRefundResponse
import spock.lang.Ignore
import spock.lang.Shared

class DDCouponRefund extends Helper {
    @Shared
    GoodsDetailResponse goodsDetailResponse
    @Shared
    CouponCaluRequest couponCaluRequest
    @Shared
            BarcodeResponse, barcodeYoRenResponse
    @Shared
    CouponConfirmResponse couponConfirmResponse
    @Shared
    CouponCaluResponse couponCaluResponse
    @Shared
    CouponRefundResponse couponRefundResponse
    @Shared
            outTradeNo, tradeNo
    @Shared
            totalFee = 0.00
    @Shared
            items, blackItems
    @Shared
    LawsonPosHubService goodsClient, barcodeClient, couponBarCodeClient, traderefundClient, tradeconfirmClient, unfreezeClient, couponConfirmeClient, couponRefundClient

    def setupSpec() {
        // 初始化 LawsonPosHubService 参数 https://lawson-poshub.kargotest.com;http://121.43.156.191:21001
        //dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://10.100.70.120:7001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        def env = ['mid': 'DEFAULT', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'http://47.101.50.215:21001', 'store_id': '208888', 'user_id': '00000002', 'pos_id': '01', 'jar_version': '1']
        // def env = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://10.100.70.129:7001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //prd = ['mid':'DEFAULT', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        goodsClient = createLawsonPosHubService(env, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(env, '/barcode')
        couponBarCodeClient = createLawsonPosHubService(env, '/couponBarCode')
        couponConfirmeClient = createLawsonPosHubService(env, '/couponConfirm')
        couponRefundClient = createLawsonPosHubService(env, '/couponRefund')
        tradeconfirmClient = createLawsonPosHubService(env, '/tradeconfirm')
        traderefundClient = createLawsonPosHubService(env, '/traderefund')
        unfreezeClient = createLawsonPosHubService(env, '/unfreeze')

        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102'] // 6901028075831 黑名单商品
        //items = [] // 6901028075831 黑名单商品

        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"2501858005102\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem3 = jsonSlurper.parseText("{\"barcode\":\"4901777374461\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":8.00,\"discount_quantity\":2.0}],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"三得利-196℃桃子配制酒\",\"quantity\":2,\"row_no\":3,\"sell_price\":15.9,\"total_amount\":31.80,\"total_discount\":8.00}");
        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2), new OrderItem(blackItem3)]
        //blackItems = []
    }

    def "call uploadgoodsdetail without member_no"() {
        given:
        // 商品明细
        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, blackItems)
        when:
        totalFee = request.getTotal_fee()
        //totalFee = 0.01
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse) {
            pay_code == '038'
            ret_code == '00'
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
            ret_code == '00'
            pay_code == '038'
            ['1900267772339', '1900213189174'].contains(user_info.code) //YoRen测试环境会员号
        }
        where:
        memberNo = '391003870323196996'
    }

    def "call uploadgoodsdetail with member_no"() {
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        //totalFee = 0.01

        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse) {
            pay_code == '038'
            ret_code == '00'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call couponBarCode"() { // -----> /dmall/coupon/hangupCalculate
        // 存入 kargo:admin:cache:orderInfo:208888:01:outTradeNo pan:D10768668413318337
        given:
        couponCaluRequest = createCouponCaluRequest(DDMemberNo, outTradeNo, '01', items, blackItems)
        when:
        couponCaluResponse = (CouponCaluResponse) couponBarCodeClient.execute(couponCaluRequest)
        then:
        with(couponCaluResponse) {
            responseCode == '0000'
            couonPayCode == cPayCode
        }
        where:
        DDMemberNo|cPayCode
        //'D10768668413318337'|'025'
        // 'L10768666610018888'|'024'
    }

    @Ignore
    def "call couponConfirm"() { // -----> /dmall/coupon/useCoupon
        given:
        CouponConfirmRequest request = createCouponConfirm(outTradeNo, '01', couponCaluResponse.getyList())
        when:
        couponConfirmResponse = (CouponConfirmResponse) couponConfirmeClient.execute(request)
        then:
        with(couponConfirmResponse) {
            responseCode == '0000'
        }
    }

    @Ignore
    def "call couponRefund"() { // -----> /dmall/coupon/recoverCoupon
        given:
        //CouponRefundReqeust request = createCouponRefundReqeust(couponCaluRequest.trade_no, outTradeNo, couponCaluRequest.venderCode, couponCaluResponse.getyList())
        def yList = [[couponCode:"SJMJ100662308000000001133"]]
        CouponRefundReqeust request = createCouponRefundReqeust('20888831100721', '20888802131411629', '01', yList)
        when:
        couponRefundResponse = (CouponRefundResponse) couponRefundClient.execute(request)
        then:
        with(couponRefundResponse) {
            responseCode == '0000'
        }
    }
}