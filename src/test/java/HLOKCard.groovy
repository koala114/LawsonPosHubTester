import com.kargo.LawsonPosHubService
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.HlOkCardPayRequest
import com.kargo.request.HlOkCardRefundRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.HlOkCardPayResponse
import com.kargo.response.HlOkCardRefundResponse
import com.kargo.response.PaymentConfirmResponse
import spock.lang.Ignore
import spock.lang.Shared

class HLOKCard extends Helper {
    @Shared BarcodeResponse barcodeYoRenResponse
    @Shared HlOkCardPayResponse hlOkCardPayResponse
    @Shared outTradeNo
    @Shared totalFee = 17.01
    @Shared env, prd, items, blackItems
    @Shared LawsonPosHubService barcodeClient, goodsClient, hlokcardpayClient, hlokcardrefundClient, tradeconfirmClient

    def setupSpec() {
        // 初始化 LawsonPosHubService 参数
        env = ['mid': '00062000000', 'sessionKey': '9Y3SGFCLR2BH4T51', 'kargoUrl': 'https://lawson-poshub.kargotest.com', 'store_id': '208888', 'user_id': '00000002', 'pos_id': '01', 'jar_version': '1']
        prd = ['mid': '00062000000', 'sessionKey': 'LAWSONJZ2NJKARGO', 'kargoUrl': 'http://47.97.19.94:21001', 'store_id': '203118', 'user_id': '20311801', 'pos_id': '01', 'jar_version': '1']

        barcodeClient = createLawsonPosHubService(env, '/barcode')
        goodsClient = createLawsonPosHubService(env, '/uploadgoodsdetail')
        tradeconfirmClient = createLawsonPosHubService(env, '/tradeconfirm')
        hlokcardrefundClient = createLawsonPosHubService(env, '/hlokcardrefund')
        hlokcardpayClient = createLawsonPosHubService(env, '/hlokcardpay')


        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102'] // 6901028075831 黑名单商品； 56、58大类黑名单
        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"567\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"6923127360100\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"58\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")

        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2)]
    }

    def "call barcode with YoRen"(){
        given:
        def request = createBarCodeRequest(memberNo, outTradeNo, totalFee)

        when:
        barcodeYoRenResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeYoRenResponse){
            responseCode == '0000'
            biz_type == '02'
            ret_code == '00'
            pay_code == '038'
            ['1900267772339', '1900213189174'].contains(user_info.code) //YoRen测试环境会员号
        }
        where:
        memberNo = '391020184231758647'
    }

    def "call uploadgoodsdetail with member_no"(){
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        when:
        def goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            ret_code == '00'
            responseCode == '0000'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call HLOKCard pay"() {
        given:
        HlOkCardPayRequest hlOkCardPayRequest = createHlOkCardPayRequest(pan, outTradeNo, totalFee)
        when:
        hlOkCardPayResponse = hlokcardpayClient.execute(hlOkCardPayRequest)
        then:
        with(hlOkCardPayResponse){
            responseCode == '0000'
            ret_code == '00'
            biz_type == '03'
            status == '1000'
            pay_code == '009'
            amountProcessed == String.valueOf(totalFee)
        }
        where:
        pan = '554916216814712'
    }

    def "call confirm"(){
        given:
        PaymentConfirmRequest request = createPaymentConfirmRequest([], memberNo, outTradeNo, totalFee, 0, 0)

        when:
        def paymentConfirmResponse = (PaymentConfirmResponse) tradeconfirmClient.execute(request)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            ret_code == '00'
            //totalPoint > 0
        }
        where:
        memberNo = barcodeYoRenResponse.hasProperty('user_info')?barcodeYoRenResponse.user_info.code:''
    }

    def "call HLOKCard refund"(){
        given:
        HlOkCardRefundRequest hlOkCardRefundRequest = createHlOkCardRefundRequest(hlOkCardPayResponse.trade_no, hlOkCardPayResponse.amountProcessed)
        //HlOkCardRefundRequest hlOkCardRefundRequest = createHlOkCardRefundRequest("4214670112301017", "0.01")
        //hlOkCardRefundRequest.setPay_code("009")
        when:
        HlOkCardRefundResponse hlOkCardRefundResponse = hlokcardrefundClient.execute(hlOkCardRefundRequest)
        then:
        with(hlOkCardRefundResponse){
            responseCode == '0000'
            ret_code == '00'
            //totalPoint > 0
            biz_type == '03'
            pay_code == '009'
            status == '2000'
        }
    }
}
