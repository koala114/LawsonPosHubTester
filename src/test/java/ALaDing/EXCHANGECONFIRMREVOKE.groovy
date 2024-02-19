package ALaDing

import Core.Helper
import com.kargo.LawsonPosHubService
import com.kargo.request.BarCodeRequest
import com.kargo.request.ExchangeConfirmRequest
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.GoodsDetailResponse
import spock.lang.Ignore
import spock.lang.Shared

class EXCHANGECONFIRMREVOKE extends Helper {
    Date now = new Date()
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared dev, prd, items, blackItems, pan
    @Shared LawsonPosHubService exchangeConfirmClient, goodsClient, barcodeClient, traderefundClient, tradeconfirmClient
    @Shared BarCodeRequest barcodeRequest
    @Shared def coupons = [["code":"899429724020614340309000000000010009", "amt": 10.0]]

    def setupSpec(){
        // 初始化 LawsonPosHubService 参数 https://lawson-poshub.kargotest.com
        dev = ['mid':'00062000000', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        prd = ['mid':'00062000000', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        exchangeConfirmClient = createLawsonPosHubService(dev, '/exchangeconfirm')
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        tradeconfirmClient = createLawsonPosHubService(dev, '/tradeconfirm')
        traderefundClient = createLawsonPosHubService(dev, '/traderefund')

        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102', '6902538008548'] // 6901028075831 黑名单商品
        //items = [] // 6901028075831 黑名单商品

        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"6923127360100\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"2121\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")

        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2)]
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')


        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, [])
        totalFee = request.getTotal_fee()
        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)

        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
        }
    }

    def "call barcode with YoRen"(){
        given:
        barcodeClient = createLawsonPosHubService(dev, '/barcode')

        BarCodeRequest request = createBarCodeRequest(memberNo, outTradeNo, totalFee)
        when:
        barcodeYoRenResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeYoRenResponse){
            responseCode == '0000'
            biz_type == '02'
            pay_code == '038'
            user_info.code == barcodeYoRenResponse.getUser_info().getCode()// YoRen测试环境会员号
        }
        where:
        memberNo = '391003870323196996'
    }

    def "call uploadgoodsdetail with member_no"(){
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

    def "call barcode to payment"(){
        given:
        barcodeRequest = createBarCodeRequest(pan, outTradeNo, totalFee)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(barcodeRequest)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == '01' // 阿拉丁是01
            pay_code == '032'
            total_fee == 10.00 //挡板返回该阿拉订10元券
        }

        where:
        pan << coupons['code']
    }

    def "call exchangeconfirm"() {
        // 兑换业务传券号；撤销业务不传券号
        given:
        ExchangeConfirmRequest exchangeConfirm = createExchangeConfirmRequest(outTradeNo, null, barcodeResponse.total_fee, totalFee)
        when:
        ExchangeConfirmResponse exchangeConfirmResp = (ExchangeConfirmResponse) exchangeConfirmClient.execute(exchangeConfirm)
        then:
        with(exchangeConfirmResp){
            responseCode == '0000'
        }
    }
}