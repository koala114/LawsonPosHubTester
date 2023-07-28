import com.kargo.LawsonPosHubService
import com.kargo.request.BarCodeRequest
import com.kargo.request.ExchangeConfirmRequest
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.HealthCheckRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.PaymentRefundRequest
import com.kargo.request.QueryCardInfoRequest
import com.kargo.request.UnFreezeRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.HealthCheckResponse
import com.kargo.response.PaymentRefundResponse
import com.kargo.response.QueryCardInfoResponse
import com.kargo.response.UnFreezeResponse
import groovy.json.JsonSlurper
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

class FastBaseline extends Helper {
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared outTradeNo
    @Shared totalFee = 0.01
    @Shared env, items, blackItems=[]
    @Shared LawsonPosHubService goodsClient, barcodeClient, traderefundClient, tradeconfirmClient, exchangeConfirmClient, queryCardClient, unfreezeClient
    @Shared barcodeRespList = []

    def setupSpec(){
        // 初始化 LawsonPosHubService 参数 https://lawson-poshub.kargotest.com, http://121.43.156.191:21001
        env = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:9000', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //env = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://121.43.156.191:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //env = ['mid':'DEFAULT', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1.9-3']

        goodsClient = createLawsonPosHubService(env, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(env, '/barcode')
        tradeconfirmClient = createLawsonPosHubService(env, '/tradeconfirm')
        traderefundClient = createLawsonPosHubService(env, '/traderefund')
        exchangeConfirmClient = createLawsonPosHubService(env, '/exchangeconfirm')
        queryCardClient = createLawsonPosHubService(env, '/querycardinfo')
        unfreezeClient = createLawsonPosHubService(env, '/unfreeze')


        // 全局out_trade_no, 所有交易相同
        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6920459950180', '2501408063102'] // 6901028075831 黑名单商品
        //items = [] // 6901028075831 黑名单商品

        //def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        //def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"6923127360100\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"2121\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        //blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2)]

        def mostItems = jsonSlurper.parseText("[{\"barcode\":\"6972549660493\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利沁葡水\",\"quantity\":1.000,\"row_no\":1,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6901285991219\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.00,\"discount_quantity\":3.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"怡宝  饮用纯净水\",\"quantity\":3,\"row_no\":2,\"sell_price\":2.00,\"total_amount\":6.00,\"total_discount\":1.00},{\"barcode\":\"2501408943107\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.50,\"discount_quantity\":2.000}],\"goods_category\":\"05\",\"kagou_sign\":\"N\",\"name\":\"湘鄂风味道地肠XX\",\"quantity\":2.000,\"row_no\":3,\"sell_price\":5.00,\"total_amount\":10.00,\"total_discount\":2.50},{\"barcode\":\"8809448200094\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"34\",\"kagou_sign\":\"N\",\"name\":\"韩美禾啵乐哈密瓜味戒指糖\",\"quantity\":1.000,\"row_no\":4,\"sell_price\":14.00,\"total_amount\":14.00,\"total_discount\":0},{\"barcode\":\"8410525173950\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"菲尼牌西瓜形夹心泡泡\",\"quantity\":1.000,\"row_no\":5,\"sell_price\":5.00,\"total_amount\":5.00,\"total_discount\":0},{\"barcode\":\"6920459998434\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":3.00,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"康师傅茉莉清茶低糖\",\"quantity\":2,\"row_no\":6,\"sell_price\":4.00,\"total_amount\":8.00,\"total_discount\":3.00},{\"barcode\":\"6972549660905\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.80,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利茉莉乌龙茶饮料无糖\",\"quantity\":2,\"row_no\":8,\"sell_price\":5.50,\"total_amount\":11.00,\"total_discount\":2.80},{\"barcode\":\"8801906160452\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"趣莱福蒜味虾片\",\"quantity\":1.000,\"row_no\":9,\"sell_price\":17.80,\"total_amount\":17.80,\"total_discount\":0},{\"barcode\":\"6901845040968\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"格力高饼干抹茶慕斯\",\"quantity\":1.000,\"row_no\":10,\"sell_price\":10.20,\"total_amount\":10.20,\"total_discount\":0},{\"barcode\":\"6923450611944\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"16\",\"kagou_sign\":\"N\",\"name\":\"m&m's 冰淇淋巧克力味\",\"quantity\":1.000,\"row_no\":11,\"sell_price\":18.90,\"total_amount\":18.90,\"total_discount\":0},{\"barcode\":\"4894375030139\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.90,\"discount_quantity\":1.000}],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"Aji椰子卷\",\"quantity\":1.000,\"row_no\":12,\"sell_price\":12.80,\"total_amount\":12.80,\"total_discount\":2.90},{\"barcode\":\"6958652300068\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"品客 浓香奶酪味\",\"quantity\":1.000,\"row_no\":13,\"sell_price\":12.80,\"total_amount\":12.80,\"total_discount\":0},{\"barcode\":\"6928804014877\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"可口可乐\",\"quantity\":1.000,\"row_no\":14,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6972549660097\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利瓶装乌龙茶无\",\"quantity\":1,\"row_no\":15,\"sell_price\":5.50,\"total_amount\":5.50,\"total_discount\":0},{\"barcode\":\"6901939671603\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"可口可乐可乐味汽水\",\"quantity\":1.000,\"row_no\":16,\"sell_price\":7.50,\"total_amount\":7.50,\"total_discount\":0},{\"barcode\":\"6902538004045\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.45,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"脉动维生素饮料青柠\",\"quantity\":1,\"row_no\":17,\"sell_price\":6.20,\"total_amount\":6.20,\"total_discount\":1.45},{\"barcode\":\"6902538005141\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.45,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"脉动维生素饮料桃子口味\",\"quantity\":1,\"row_no\":18,\"sell_price\":6.20,\"total_amount\":6.20,\"total_discount\":1.45},{\"barcode\":\"6921168558049\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":7.50,\"discount_quantity\":3.000}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"①农夫山泉东方树叶茉莉花茶\",\"quantity\":3.000,\"row_no\":19,\"sell_price\":6.50,\"total_amount\":19.50,\"total_discount\":7.50},{\"barcode\":\"6907868581389\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":11.80,\"discount_quantity\":2.000}],\"goods_category\":\"16\",\"kagou_sign\":\"N\",\"name\":\"八喜 草莓冰淇淋\",\"quantity\":2.000,\"row_no\":20,\"sell_price\":13.80,\"total_amount\":27.60,\"total_discount\":11.80},{\"barcode\":\"6970399920415\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"③元气森林白桃味苏打气泡水\",\"quantity\":1.000,\"row_no\":21,\"sell_price\":6.00,\"total_amount\":6.00,\"total_discount\":0},{\"barcode\":\"6902827110228\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"美年达橙味\",\"quantity\":1.000,\"row_no\":22,\"sell_price\":4.20,\"total_amount\":4.20,\"total_discount\":0},{\"barcode\":\"6928804014952\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"芬达橙味汽水\",\"quantity\":1.000,\"row_no\":23,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6974139940251\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"17\",\"kagou_sign\":\"N\",\"name\":\"可可满分椰子水\",\"quantity\":1.000,\"row_no\":24,\"sell_price\":9.80,\"total_amount\":9.80,\"total_discount\":0},{\"barcode\":\"2501408866109\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":3.20,\"discount_quantity\":2.000}],\"goods_category\":\"05\",\"kagou_sign\":\"N\",\"name\":\"照烧脆骨丸串\",\"quantity\":2.000,\"row_no\":25,\"sell_price\":6.50,\"total_amount\":13.00,\"total_discount\":3.20},{\"barcode\":\"6921168596348\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.50,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"东方树叶青柑普洱茶\",\"quantity\":1,\"row_no\":26,\"sell_price\":6.5,\"total_amount\":6.50,\"total_discount\":2.50},{\"barcode\":\"6921168594993\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"苏打矿泉水白桃风味\",\"quantity\":1,\"row_no\":27,\"sell_price\":4.8,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"4891028705949\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":4.90,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"维他柠檬茶饮料\",\"quantity\":2,\"row_no\":28,\"sell_price\":6.9,\"total_amount\":13.80,\"total_discount\":4.90},{\"barcode\":\"6926265306418\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":7.20,\"discount_quantity\":2.0}],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"上好佳田园泡玉米口味\",\"quantity\":2,\"row_no\":30,\"sell_price\":7.2,\"total_amount\":14.40,\"total_discount\":7.20}]")
        mostItems.each { blackItems.add(new OrderItem(it)) }
        //blackItems = []
    }

    @Unroll
    def "call uploadgoodsdetail #memberNo"(){
        given:
        // 商品明细
        GoodsDetailRequest request = createGoodsDetailRequest(memberNo, outTradeNo, items, blackItems)
        when:
        totalFee = request.getTotal_fee()
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            responseCode == '0000'
            isExtraInfo == (extraInfo != null)
        }
        where:
        memberNo|isExtraInfo
        //null|false
        '1900267772339'|true
    }

    def "call unfreeze"(){  // -----> /pos/v1/pos/couponCorrection
        given:
        UnFreezeRequest unFreezeRequest = createUnFreezeRequest("20888814142409173", "1900267772339")
        when:
        UnFreezeResponse unFreezeResponse = (UnFreezeResponse) unfreezeClient.execute(unFreezeRequest)
        then:
        with(unFreezeResponse){
            responseCode == '0000'
        }
        where:
        memberNo = '1900267772339'
    }

    def "call barcode "(){
        given:
        BarCodeRequest request = createBarCodeRequest(pan, outTradeNo, totalFee)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        barcodeRespList.add(barcodeResponse)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            biz_type == bizType // e支付 00
            pay_code == payCode
        }
        where:
        pan|bizType|payCode
        '6901209322501'|'02'|'038' // 游仁会员
        //'132720359431780134'|'00'|'050' // 微信
        //'280891674175350197'|'00'|'051' // 支付宝
        getUnionpayPan()|'00'|'057' // 银联
        //'899154330517171601690000000000100108'|'01'|'032' // 阿拉丁走MiYa
        // '77FF03120721218131'|'00'|'004' // 索迪斯走MiYa
        //'996016844010007349^6955814701848;                                           '|'03' | '045' // 卡购卡

    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        given:
        //PaymentRefundRequest request = createPaymentRefundRequest(code, oldTradeNo, amt)
        PaymentRefundRequest request = createPaymentRefundRequest(code, oldTradeNo, amt)
        when:
        def resp = (PaymentRefundResponse) traderefundClient.execute(request)
        then:
        with(resp){
            responseCode == '0000'
            biz_type == bizType // e支付 退款00
            status == '2000'
            pay_code == payCode
        }
       /* where:
        code|oldTradeNo|amt|bizType|payCode
        barcodeRespList[0].getUser_info().code|barcodeRespList[0].getOut_trade_no()|null|'01'|'038'
        barcodeRespList[1].pay_code|barcodeRespList[1].trade_no|barcodeRespList[1].total_fee|'00'|'050'
        barcodeRespList[2].pay_code|barcodeRespList[2].trade_no|barcodeRespList[2].total_fee|'00'|'051'
        barcodeRespList[3].pay_code|barcodeRespList[3].trade_no|barcodeRespList[3].total_fee|'00'|'057' */
    }

    def "call querycardinfo"(){
        given:
        QueryCardInfoRequest request = createQueryCardInfoRequest(trackInfo)
        when:
        QueryCardInfoResponse queryCardInfoResponse = (QueryCardInfoResponse) queryCardClient.execute(request)
        then:
        with(queryCardInfoResponse){
            responseCode == '0000'
            card_no == cardNo
        }
        where:
        trackInfo|bizType|cardNo
        '996016844010007349^6955814701848;                                           '|'03'|'69558147018480006016844010007349'
    }

    def "call exchangeconfirm"() {
        given:
        ExchangeConfirmRequest exchangeConfirm = createExchangeConfirmRequest(null, '01296174', 143.6, 143.6, '899306323051819063079064001000000003')
        /* def slurper = new JsonSlurper()
        def test = slurper.parseText('{"dt":"2023-05-24 08:09:17","jar_version":"1.9-3","store_id":"203118","receive":[{"code":"899709123052408087779094001000000007","amt":9.9}],"out_trade_no":"20311801298004","pay_amt":9.9,"coupons":[{"code":"899709123052408087779094001000000007","amt":9.9}],"signature":"b785eb8c6e317b3b918f567b024f791c2a52ad6fe796db0c9ad71f69d98cd286","user_id":"20311801","pos_id":"01","total_fee":9.90,"trade_no":"2031180129800402"}')
        ExchangeConfirmRequest exchangeConfirm = new ExchangeConfirmRequest(test)
        */
        when:
        ExchangeConfirmResponse exchangeConfirmResp = (ExchangeConfirmResponse) exchangeConfirmClient.execute(exchangeConfirm)
        then:
        with(exchangeConfirmResp){
            responseCode == '0000'
        }
    }

    def "call health"(){
        given:
        def healthClient = createLawsonPosHubService(env, '/health')
        HealthCheckRequest healthCheckRequest = new HealthCheckRequest()
        when:
        HealthCheckResponse healthCheckResponse = (HealthCheckResponse) healthClient.execute(healthCheckRequest)
        then:
        with(healthCheckResponse){
            responseCode == '0000'
        }

    }
}
