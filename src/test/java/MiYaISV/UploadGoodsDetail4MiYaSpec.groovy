package MiYaISV

import Core.Helper
import com.kargo.LawsonPosHubService
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.request.PaymentRefundRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.PaymentConfirmResponse
import com.kargo.response.PaymentRefundResponse
import spock.lang.Ignore
import spock.lang.Shared

class UploadGoodsDetail4MiYaSpec extends Helper {
    @Shared GoodsDetailResponse goodsDetailResponse
    @Shared BarcodeResponse barcodeResponse, barcodeYoRenResponse
    @Shared totalFee = 0.00
    @Shared dev, prd, items, blackItems
    @Shared LawsonPosHubService goodsClient, barcodeClient, traderefundClient, tradeconfirmClient
    @Shared outTradeNo

    def setupSpec(){
        // 初始化 LawsonPosHubService 参数 https://lawson-poshub.kargotest.com;http://121.43.156.191:21001
        dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        prd = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://121.43.156.191:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        tradeconfirmClient = createLawsonPosHubService(dev, '/tradeconfirm')
        traderefundClient = createLawsonPosHubService(dev, '/traderefund')

        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))

        items = ['6923127360100', '2501408063102', '6920259700053'] // 6901028075831 黑名单商品
        //items = [] // 6901028075831 黑名单商品

        def blackItem1 = jsonSlurper.parseText("{\"barcode\":\"1345597486671291400\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"58\",\"kagou_sign\":\"N\",\"name\":\"黑名单1\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem2 = jsonSlurper.parseText("{\"barcode\":\"2501858005102\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"黑名单2\",\"quantity\":1,\"row_no\":1,\"sell_price\":0.33,\"total_amount\":0.33,\"total_discount\":0}")
        def blackItem3 = jsonSlurper.parseText("{\"barcode\":\"78013011111111\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":8.00,\"discount_quantity\":2.0}],\"goods_category\":\"51\",\"kagou_sign\":\"N\",\"name\":\"三得利-196℃桃子配制酒\",\"quantity\":2,\"row_no\":3,\"sell_price\":15.9,\"total_amount\":31.80,\"total_discount\":8.00}");
        blackItems = [new OrderItem(blackItem1), new OrderItem(blackItem2), new OrderItem(blackItem3)]
        //blackItems = []

        def items = jsonSlurper.parseText("[{\"barcode\":\"6972549660493\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利沁葡水\",\"quantity\":1.000,\"row_no\":1,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6901285991219\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.00,\"discount_quantity\":3.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"怡宝  饮用纯净水\",\"quantity\":3,\"row_no\":2,\"sell_price\":2.00,\"total_amount\":6.00,\"total_discount\":1.00},{\"barcode\":\"2501408943107\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.50,\"discount_quantity\":2.000}],\"goods_category\":\"05\",\"kagou_sign\":\"N\",\"name\":\"湘鄂风味道地肠XX\",\"quantity\":2.000,\"row_no\":3,\"sell_price\":5.00,\"total_amount\":10.00,\"total_discount\":2.50},{\"barcode\":\"8809448200094\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"34\",\"kagou_sign\":\"N\",\"name\":\"韩美禾啵乐哈密瓜味戒指糖\",\"quantity\":1.000,\"row_no\":4,\"sell_price\":14.00,\"total_amount\":14.00,\"total_discount\":0},{\"barcode\":\"8410525173950\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"菲尼牌西瓜形夹心泡泡\",\"quantity\":1.000,\"row_no\":5,\"sell_price\":5.00,\"total_amount\":5.00,\"total_discount\":0},{\"barcode\":\"6920459998434\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":3.00,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"康师傅茉莉清茶低糖\",\"quantity\":2,\"row_no\":6,\"sell_price\":4.00,\"total_amount\":8.00,\"total_discount\":3.00},{\"barcode\":\"6972549660905\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.80,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利茉莉乌龙茶饮料无糖\",\"quantity\":2,\"row_no\":8,\"sell_price\":5.50,\"total_amount\":11.00,\"total_discount\":2.80},{\"barcode\":\"8801906160452\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"趣莱福蒜味虾片\",\"quantity\":1.000,\"row_no\":9,\"sell_price\":17.80,\"total_amount\":17.80,\"total_discount\":0},{\"barcode\":\"6901845040968\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"29\",\"kagou_sign\":\"N\",\"name\":\"格力高饼干抹茶慕斯\",\"quantity\":1.000,\"row_no\":10,\"sell_price\":10.20,\"total_amount\":10.20,\"total_discount\":0},{\"barcode\":\"6923450611944\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"16\",\"kagou_sign\":\"N\",\"name\":\"m&m's 冰淇淋巧克力味\",\"quantity\":1.000,\"row_no\":11,\"sell_price\":18.90,\"total_amount\":18.90,\"total_discount\":0},{\"barcode\":\"4894375030139\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.90,\"discount_quantity\":1.000}],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"Aji椰子卷\",\"quantity\":1.000,\"row_no\":12,\"sell_price\":12.80,\"total_amount\":12.80,\"total_discount\":2.90},{\"barcode\":\"6958652300068\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"品客 浓香奶酪味\",\"quantity\":1.000,\"row_no\":13,\"sell_price\":12.80,\"total_amount\":12.80,\"total_discount\":0},{\"barcode\":\"6928804014877\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"可口可乐\",\"quantity\":1.000,\"row_no\":14,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6972549660097\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"三得利瓶装乌龙茶无\",\"quantity\":1,\"row_no\":15,\"sell_price\":5.50,\"total_amount\":5.50,\"total_discount\":0},{\"barcode\":\"6901939671603\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"可口可乐可乐味汽水\",\"quantity\":1.000,\"row_no\":16,\"sell_price\":7.50,\"total_amount\":7.50,\"total_discount\":0},{\"barcode\":\"6902538004045\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.45,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"脉动维生素饮料青柠\",\"quantity\":1,\"row_no\":17,\"sell_price\":6.20,\"total_amount\":6.20,\"total_discount\":1.45},{\"barcode\":\"6902538005141\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":1.45,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"脉动维生素饮料桃子口味\",\"quantity\":1,\"row_no\":18,\"sell_price\":6.20,\"total_amount\":6.20,\"total_discount\":1.45},{\"barcode\":\"6921168558049\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":7.50,\"discount_quantity\":3.000}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"①农夫山泉东方树叶茉莉花茶\",\"quantity\":3.000,\"row_no\":19,\"sell_price\":6.50,\"total_amount\":19.50,\"total_discount\":7.50},{\"barcode\":\"6907868581389\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":11.80,\"discount_quantity\":2.000}],\"goods_category\":\"16\",\"kagou_sign\":\"N\",\"name\":\"八喜 草莓冰淇淋\",\"quantity\":2.000,\"row_no\":20,\"sell_price\":13.80,\"total_amount\":27.60,\"total_discount\":11.80},{\"barcode\":\"6970399920415\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"③元气森林白桃味苏打气泡水\",\"quantity\":1.000,\"row_no\":21,\"sell_price\":6.00,\"total_amount\":6.00,\"total_discount\":0},{\"barcode\":\"6902827110228\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"美年达橙味\",\"quantity\":1.000,\"row_no\":22,\"sell_price\":4.20,\"total_amount\":4.20,\"total_discount\":0},{\"barcode\":\"6928804014952\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"芬达橙味汽水\",\"quantity\":1.000,\"row_no\":23,\"sell_price\":4.80,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"6974139940251\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"17\",\"kagou_sign\":\"N\",\"name\":\"可可满分椰子水\",\"quantity\":1.000,\"row_no\":24,\"sell_price\":9.80,\"total_amount\":9.80,\"total_discount\":0},{\"barcode\":\"2501408866109\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":3.20,\"discount_quantity\":2.000}],\"goods_category\":\"05\",\"kagou_sign\":\"N\",\"name\":\"照烧脆骨丸串\",\"quantity\":2.000,\"row_no\":25,\"sell_price\":6.50,\"total_amount\":13.00,\"total_discount\":3.20},{\"barcode\":\"6921168596348\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":2.50,\"discount_quantity\":1.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"东方树叶青柑普洱茶\",\"quantity\":1,\"row_no\":26,\"sell_price\":6.5,\"total_amount\":6.50,\"total_discount\":2.50},{\"barcode\":\"6921168594993\",\"commission_sale\":\"0\",\"discount_info_list\":[],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"苏打矿泉水白桃风味\",\"quantity\":1,\"row_no\":27,\"sell_price\":4.8,\"total_amount\":4.80,\"total_discount\":0},{\"barcode\":\"4891028705949\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":4.90,\"discount_quantity\":2.0}],\"goods_category\":\"18\",\"kagou_sign\":\"N\",\"name\":\"维他柠檬茶饮料\",\"quantity\":2,\"row_no\":28,\"sell_price\":6.9,\"total_amount\":13.80,\"total_discount\":4.90},{\"barcode\":\"6926265306418\",\"commission_sale\":\"0\",\"discount_info_list\":[{\"discount_amount\":7.20,\"discount_quantity\":2.0}],\"goods_category\":\"30\",\"kagou_sign\":\"N\",\"name\":\"上好佳田园泡玉米口味\",\"quantity\":2,\"row_no\":30,\"sell_price\":7.2,\"total_amount\":14.40,\"total_discount\":7.20}]")
        //items.each { blackItems.add(new OrderItem(it)) }
    }

    def "call uploadgoodsdetail without member_no"(){
        given:
        // 商品明细
        GoodsDetailRequest request = createGoodsDetailRequest(null, outTradeNo, items, blackItems)
        when:
        totalFee = request.getTotal_fee()
        //totalFee = 0.01
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            ret_code == '00'
            responseCode == '0000'
            responseMessage == '交易成功完成'
        }
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
            responseMessage == '交易成功完成'
        }
        where:
        memberNo = '391109216737792338'
    }

    def "call uploadgoodsdetail with member_no"(){
        given:
        GoodsDetailRequest request = createGoodsDetailRequest(barcodeYoRenResponse.user_info.code, outTradeNo, items, blackItems)
        totalFee = request.getTotal_fee()
        //totalFee = 0.01

        when:
        goodsDetailResponse = (GoodsDetailResponse) goodsClient.execute(request)
        then:
        with(goodsDetailResponse){
            pay_code == '038'
            ret_code == '00'
            responseCode == '0000'
            responseMessage == '交易成功完成'
            extraInfo.contains('memberAmountFree')
        }
    }

    def "call barcode to payment"(){
        given:
        //def paras = ['currency':'CNY', 'dt':now.format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone('UTC')), 'dynamic_id': pan.keySet()[0], 'out_trade_no': outTradeNo, 'trade_no':outTradeNo + '02','total_fee':total_fee, 'fee_type':0] + common
        def request = createBarCodeRequest(pan, outTradeNo, 100)
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '0000'
            responseMessage == '请求成功[PAYSUCCESS]'
            ret_code == '00'
            biz_type == '00' // 支付00
            pay_code == paycode
            pay_name == payname
            status == paystatus
        }
        where:
        pan|paycode|payname|paystatus
        '6240105666367315102'|'027'|'建行支付'|'1000'
        //'132692326141378115'|'050'|'微信支付'|'1000'

        /* '651680771898912912'|'026'|'驾续多'|'10000'
         '77FF03120721218131'|'004'|'索迪斯'|'1000'
         '0100505719322301014'|'007'|'数字人民币'|'1000'
         '132692326141378115'|'050'|'微信支付'|'1000'
         '810086744359766447'|'031'|'移动和包'|'1000'
         '283712123251107120'|'051'|'支付宝'|'1000'
         '6226994810298330706'|'057'|'银联支付'|'1000'
         */

        //'LS210032839440110269000'|'100'
        //'0100803882792891721'|'007' // 微信支付
        //'77FF03120721218131'|'004' // 索迪斯
        //'https://www.apple.com.cn'|'023'//中百抖音
        //'6220204222068652830'|'057'//中百抖音
        //'810086722461596869'|'031'|'移动和包'|'1000'
    }

    def "call confirm"(){
        given:
        PaymentConfirmRequest request = createPaymentConfirmRequest([], barcodeYoRenResponse.getUser_info().code, outTradeNo, totalFee, 0, 0)
        when:
        def paymentConfirmResponse = (PaymentConfirmResponse) tradeconfirmClient.execute(request)
        then:
        with(paymentConfirmResponse){
            responseCode == '0000'
            responseMessage == '交易成功完成'
            ret_code == '00'
            //totalPoint > 0
            status == '1000'
        }
    }

    def "call traderefund for YoRen"(){// YoRen退积分old_trade_no用out_trade_no
        given:
        PaymentRefundRequest request = createPaymentRefundRequest(barcodeYoRenResponse.getUser_info().getCode(), barcodeYoRenResponse.out_trade_no);
        when:
        def paymentRefundResponse = (PaymentRefundResponse) traderefundClient.execute(request)
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            responseMessage == '交易成功完成'
            biz_type == '01' // YoRen 退款01
            ret_code == '00'
            status == '2000'
        }
    }

    def "call traderefund for payment"(){ // e支付退款的old_trade_no用e支付barcode的tradeNo
        given:
        PaymentRefundRequest request = createPaymentRefundRequest(barcodeResponse.getPay_code(), barcodeResponse.trade_no, barcodeResponse.getTotal_fee());
        //PaymentRefundRequest request = createPaymentRefundRequest('027', '208888051025248451', 4.00);
        when:
        def paymentRefundResponse = (PaymentRefundResponse) traderefundClient.execute(request)
        then:
        with(paymentRefundResponse){
            responseCode == '0000'
            responseMessage == '退款成功'
            biz_type == '00'
            ret_code == '00'
            status == '2000'
        }
    }
}