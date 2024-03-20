package Core

import com.kargo.InitService
import com.kargo.LawsonPosHubService
import com.kargo.request.*
import com.kargo.request.coupon.*
import com.kargo.request.detail.ExchangeConfirmCoupons
import com.kargo.request.detail.ExchangeConfirmReceive
import com.kargo.request.detail.OrderItem
import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.apache.log4j.Logger
import spock.lang.Shared

import java.util.regex.Matcher
import java.util.regex.Pattern

import static java.util.TimeZone.getTimeZone

class Helper {
    private static final Logger log = Logger.getLogger(Helper.class);
    @Shared members = ['13818595461':'1900267772339']
    String mid, store_id, pos_id, kargoUrl, sessionKey, user_id, jar_version
    static String tradeNoPostfix = 0
    @Shared jsonSlurper = new JsonSlurper()

    protected LawsonPosHubService createLawsonPosHubService(def env, String miyaUrl){
        mid = env.mid
        user_id = env.user_id
        store_id = env.store_id
        pos_id = env.pos_id
        kargoUrl = env.kargoUrl
        sessionKey = env.sessionKey
        InitService.init()
        return new LawsonPosHubService(mid, store_id, pos_id, kargoUrl, sessionKey, miyaUrl, "", "", "pay")
    }

    protected GoodsDetailRequest createGoodsDetailRequestString(outTradeNo, OrderItem item){
        def paras = ['currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'extraInfo':'{\"memberAmount\":0.0}', 'modify_flag':0, 'out_trade_no':store_id + outTradeNo,
                     'pos_id':pos_id, 'pos_version':'1', 'store_id': store_id, 'total_fee':item.total_amount, 'user_id':user_id, 'order_items':[item]]
        GoodsDetailRequest request = new GoodsDetailRequest(*:paras)
        return request
    }

    protected GoodsDetailRequest createGoodsDetailRequest(String memberNo, String outTradeNo, def items){
        def totalFee = 0.0
        def discount = []

        def its = createItems(items)
        its.each {totalFee =it.quantity*it.sell_price + totalFee; return totalFee}
        log.info("totalFee is " + totalFee)
        its.each {discount = it.discount_info_list.discount_amount + discount}
        log.info("discount is " + discount.sum())
        totalFee = totalFee - (discount.sum()?:0.0)
        log.info("totalFee - discount = " + totalFee)

        def paras = ['currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'extraInfo':'{\"memberAmount\":0.0}', 'modify_flag':0, 'out_trade_no':store_id + outTradeNo,
                     'pos_id':pos_id, 'pos_version':jar_version, 'store_id': this.store_id, 'total_fee':totalFee.round(2), 'user_id':user_id, 'order_items':its]
        if(memberNo)
            paras << [ 'member_no': memberNo, 'modify_flag': 1]
        GoodsDetailRequest request = new GoodsDetailRequest(*:paras)
        return request
    }

    protected BarCodeRequest createBarCodeRequest(String dynamicId, String outTradeNo, Double totalFee){
        def paras = ['currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'dynamic_id': dynamicId, 'out_trade_no': store_id + outTradeNo, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix),
                     'pos_id':pos_id, 'store_id':store_id, 'total_fee':totalFee, 'user_id':user_id, 'fee_type':0]
        BarCodeRequest request = new BarCodeRequest(paras);
        return request
    }

    protected CreatePaymentRequest createPaymentRequestt(String dynamicId, String outTradeNo, String billId, BigDecimal billAmt){
        def paras = ['bill_id':billId, 'bill_amt':billAmt, 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'barcode': dynamicId, 'out_trade_no': store_id + outTradeNo, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix),
                     'pos_id':pos_id, 'store_id':store_id, 'user_id':user_id]

        CreatePaymentRequest request = new CreatePaymentRequest(paras);
        return request
    }

    protected PaymentConfirmRequest createPaymentConfirmRequest(def couponList, String memberNo, String outTradeNo, Double totalFee, Double pointAmount, Double prepaidAmount){
        def paras = ['coupon_code':couponList, 'currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'extraInfo':'{\"memberPromotion\":\"0\",\"memberPromotionRec\":\"\",\"memberAmount\":0.0}', 'out_trade_no': store_id + outTradeNo, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix),
                     'pos_id':pos_id, 'store_id':store_id, 'total_fee':totalFee, 'user_id':user_id, 'offline_flag':'0', 'point_amount':0, 'prepaid_amount':prepaidAmount?prepaidAmount:0, 'point_amount': pointAmount?pointAmount:0]
        if (memberNo)
            paras = paras + ['member_no': memberNo]

        PaymentConfirmRequest request = new PaymentConfirmRequest(paras);
        return request
    }

    protected QueryCardInfoRequest createQueryCardInfoRequest(String trackInfo){
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'pos_id':pos_id, 'store_id':store_id, 'user_id':user_id, 'track_info':trackInfo]

        QueryCardInfoRequest request = new QueryCardInfoRequest(paras);
        return request
    }

    protected PaymentRefundRequest createPaymentRefundRequest(String memberNo, String oldTradeNo){
        def refundOutTradeNo = (new Date()).format("ddHHmmssSSS", getTimeZone('Asia/Shanghai'))
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'member_no':memberNo, 'pos_id':pos_id, 'store_id':store_id, 'user_id':user_id, 'old_trade_no':oldTradeNo, 'order_items':[],
                     'out_trade_no': refundOutTradeNo, 'trade_no':refundOutTradeNo + tradeNoPostfix]

        PaymentRefundRequest request = new PaymentRefundRequest(paras);
        return request
    }

    protected PaymentRefundRequest createPaymentRefundRequest(String code, String oldTradeNo, Double totalFee){
        def refundOutTradeNo = (new Date()).format("ddHHmmssSSS", getTimeZone('Asia/Shanghai'))
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'pos_id':pos_id, 'store_id':store_id, 'user_id':user_id, 'old_trade_no': oldTradeNo, 'order_items':[],
                     'out_trade_no': store_id.concat(refundOutTradeNo), 'trade_no':store_id.concat(refundOutTradeNo) + tradeNoPostfix]
        if(totalFee){
            paras = paras + ['pay_code': code, 'refund_fee': totalFee]
        }
        else
            paras = paras + ['member_no': code]

        PaymentRefundRequest request = new PaymentRefundRequest(paras);
        return request
    }

    protected PaymentReverseRequest createPaymentReverseRequest(String outTradeNo, String tradeNo, String payCode){
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'out_trade_no':  outTradeNo, 'pos_id':pos_id, 'store_id':store_id, 'trade_no':tradeNo, 'user_id':user_id]
        if(payCode)
            paras = paras + ['pay_code':payCode]
        PaymentReverseRequest request = new PaymentReverseRequest(paras)
        return request
    }

    protected PaymentQueryRequest createTradeQueryRequest(String outTradeNo, String tradeNo, String payCode){
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'out_trade_no':  outTradeNo, 'pos_id':pos_id, 'store_id':store_id, 'trade_no':tradeNo, 'user_id':user_id]
        if(payCode)
            paras = paras + ['pay_code':payCode]
        PaymentQueryRequest request = new PaymentQueryRequest(paras)
        return request
    }

    protected ExchangeConfirmRequest createExchangeConfirmRequest(String outTradeNo, def coupons, double payAmt, double totalFee){
        def receiveList = []
        def couponsList = []
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')),'total_fee': totalFee, 'pay_amt':payAmt,'out_trade_no':  store_id + outTradeNo, 'pos_id':pos_id, 'store_id':store_id, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix), 'user_id':user_id]
        //def paras = ['dt':'2023-07-10 19:05:38', 'out_trade_no':  20188201148821, 'pay_amt':6.90, 'pos_id':'01', 'store_id':'201882', 'total_fee':6.90, 'trade_no':'2018820114882103', 'user_id':'20188201']

        if(coupons){
            def amt = 0.0
            // 有券号时（code）即兑换业务，否则撤销业务
            coupons.each{
                ExchangeConfirmCoupons es = new ExchangeConfirmCoupons(['amt':it.amt, 'code': it.code])
                ExchangeConfirmReceive er = new ExchangeConfirmReceive(['amt':it.amt, 'code': it.code])
                amt = amt + it.amt
                paras['pay_amt'] = amt

                paras = paras + ['receive':receiveList<<er, 'coupons':couponsList<<es]
            }
        }
        ExchangeConfirmRequest request = new ExchangeConfirmRequest(paras)
        return request
    }

    protected UnFreezeRequest createUnFreezeRequest(String outTradeNo, String memberNo){
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'out_trade_no':  store_id + outTradeNo, 'pos_id':pos_id, 'store_id':store_id,
                     'user_id':user_id, 'member_no': memberNo]
        UnFreezeRequest request = new UnFreezeRequest(paras)
        return request
    }

    protected HlOkCardPayRequest createHlOkCardPayRequest(String dynamicId, String outTradeNo, Double totalFee){
        def paras = ['currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'dynamic_id':dynamicId, 'out_trade_no': store_id + outTradeNo , 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix), 'pos_id':pos_id, 'store_id':store_id, 'total_fee':totalFee, 'user_id':user_id, 'accessCode':'MDAxBNMNyYuKEA7KUs48Tfnhvc7HJdNaTuf+cYot6vubfaqFwByhaClED3Sh/j9/cLF/ulVRBVSrJhuufhcqSxe90aDQwUfOMz21y0t/q+cLj6kcIy4a50fzK+HD0tnpMZvXTIthRpRdi71nXBdVYLvd+q/SD9kzYeITkxl/d7RfIybCTzEb']
        HlOkCardPayRequest request = new HlOkCardPayRequest(paras)
        return request
    }

    protected HlOkCardCancelRequest createHlOkCardCancelRequest(String outTradeNo, String tradeNo, Double totalFee){
        def paras = ['currency':'CNY', 'dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'pay_code':'009', 'out_trade_no': outTradeNo, 'trade_no':tradeNo, 'pos_id':pos_id, 'store_id':store_id, 'total_fee':totalFee, 'user_id':user_id, 'accessCode':'MDAxBNMNyYuKEA7KUs48Tfnhvc7HJdNaTuf+cYot6vubfaqFwByhaClED3Sh/j9/cLF/ulVRBVSrJhuufhcqSxe90aDQwUfOMz21y0t/q+cLj6kcIy4a50fzK+HD0tnpMZvXTIthRpRdi71nXBdVYLvd+q/SD9kzYeITkxl/d7RfIybCTzEb']
        HlOkCardCancelRequest request = new HlOkCardCancelRequest(paras)
        return request
    }

    protected HlOkCardRefundRequest createHlOkCardRefundRequest(String oldTradeNo, String totalFee){
        def refundOutTradeNo = (new Date()).format("ddHHmmssSSS", getTimeZone('Asia/Shanghai'))
        def paras = ['dt':(new Date()).format("yyyy-MM-dd HH:mm:ss", getTimeZone('Asia/Shanghai')), 'pos_id':pos_id, 'store_id':store_id, 'user_id':user_id, 'old_trade_no':oldTradeNo, 'order_items':[],
                     'out_trade_no': refundOutTradeNo, 'trade_no':refundOutTradeNo + tradeNoPostfix, 'refund_fee': Double.valueOf(totalFee)]

        HlOkCardRefundRequest request = new HlOkCardRefundRequest(paras);
        return request
    }

    protected CouponRefundReqeust createCouponRefundReqeust(String oldTradeNo, String outTradeNo, String vanderCode, def yList){
        def refundTradeNo= (new Date()).format("ddHHmmssSSS", getTimeZone('Asia/Shanghai'))
        def couponCodeList = []
        yList.each{
            couponCodeList << ['couponCode': it.couponCode, 'orderId':store_id.concat(outTradeNo)]
        }
        def paras = ['old_trade_no':oldTradeNo, 'storeCode':store_id, 'venderCode':vanderCode, 'orderUseCoupsReqs':couponCodeList,
                     'orderAction':0, 'orderAction':0, 'out_trade_no':store_id.concat(refundTradeNo), 'trade_no':store_id.concat(refundTradeNo).concat(++tradeNoPostfix),
        'currCalcUuid':store_id.concat(refundTradeNo).concat(++tradeNoPostfix)]

        CouponRefundReqeust request = new CouponRefundReqeust(paras)
        return request
    }

    protected CouponCancelRequest createCouponCancelRequest(String outTradeNo, String tradeNo, String vanderCode, def yList){
        def couponCodeList = []
        yList.each{
            couponCodeList << it.couponCode
        }
        def paras = ['venderCode':vanderCode, 'couponCodeList':couponCodeList,
                     'out_trade_no': store_id + outTradeNo, 'trade_no':tradeNo]

        CouponCancelRequest request = new CouponCancelRequest(paras)
        return request
    }

    protected CouponConfirmRequest createCouponConfirm(String outTradeNo, String vanderCode, def yList){
        def couponDtoList = []
        yList.each{
            CouponDto c = new CouponDto()
            c.couponCode = it.couponCode
            c.actualValue = it.actualValue
            couponDtoList << c
        }
        def paras = ['storeCode':store_id, 'venderCode':vanderCode, 'couponDtoList':couponDtoList,
                     'out_trade_no': store_id + outTradeNo, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix), 'orderId': store_id + outTradeNo]

        CouponConfirmRequest request = new CouponConfirmRequest(paras)
        return request
    }

    protected CouponCaluRequest createCouponCaluRequest(String optCode, String outTradeNo, String vanderCode, def items, def blackItems ){
        def totalFee = 0.0
        def discount = []
        def wareReqDtoArrayList = []

        def its = createItems(items, blackItems)
        its.each {totalFee =it.quantity*it.sell_price + totalFee; return totalFee}
        log.info("totalFee is " + totalFee)
        its.each {discount = it.discount_info_list.discount_amount + discount}
        log.info("discount is " + discount.sum())
        totalFee = totalFee - (discount.sum()?:0.0)
        log.info("totalFee - discount = " + totalFee)
        its.each {
            // totalSharedPrice以分为单位
            def paras = ['gift':false, 'matnr': it.barcode, 'name': it.name, 'num':it.quantity,'totalOriginPrice':it.total_amount*100, 'totalSharedPrice':it.total_amount*100, 'totalWarePrice':it.total_amount*100, 'wareType':1, 'uuid':it.row_no.toString()]
            wareReqDtoArrayList << new WareReqDto(paras)
        }
        def paras = ['otpCode':optCode, 'needHangUp':true, 'storeCode':store_id, 'venderCode':vanderCode, 'wareReqVOList':wareReqDtoArrayList,
                     'out_trade_no': store_id + outTradeNo, 'trade_no':store_id.concat(outTradeNo).concat(++tradeNoPostfix), 'couponCodeList': []]

        CouponCaluRequest request = new CouponCaluRequest(paras);
        return request
    }

    private createItems(def barcodes){
        def i = []
        def items = ['6921168509256': jsonSlurper.parseText('{"barcode":"6921168509256","commission_sale":"0","discount_info_list":[],"goods_category":"02","kagou_sign":"N","name":"火腿鸡蛋三明治 1便","quantity":1,"row_no":1,"sell_price":7.5,"total_amount":7.50,"total_discount":0}')]
        items << ['6923127360100': jsonSlurper.parseText('{"barcode":"6923127360100","commission_sale":"0", "discount_info_list":[],"goods_category":"07","kagou_sign":"N","name":"香辣粉丝包","quantity":1.000,"row_no":1,"sell_price":2.50,"total_amount":2.50,"total_discount":0}')]
        items << ['6920459950180': jsonSlurper.parseText('{"barcode":"6920459950180","commission_sale":"0","discount_info_list":[{"discount_amount":6.00,"discount_quantity":2.0}],"goods_category":"18","kagou_sign":"N","name":"贝纳颂咖啡拿铁","quantity":2,"row_no":1,"sell_price":7,"total_amount":14.00,"total_discount":6.00}')]
        items << ['6902538008548': jsonSlurper.parseText('{"barcode":"6902538008548","commission_sale":"0","discount_info_list":[{"discount_amount":5.90,"discount_quantity":1.0}],"goods_category":"17","kagou_sign":"N","name":"达能优白动植蛋白乳饮拿铁味","quantity":1,"row_no":2,"sell_price":9.9,"total_amount":9.90,"total_discount":5.90}')]
        items << ['2501408063102': jsonSlurper.parseText('{"barcode":"2501408063102","commission_sale":"0","discount_info_list":[],"goods_category":"01","kagou_sign":"N","name":"臻享饭团(纯牛肉汉堡)1便","quantity":1,"row_no":1,"sell_price":8.9,"total_amount":8.90,"total_discount":0}')]
        items << ['6920259700053': jsonSlurper.parseText('{"barcode":"6920259700053","commission_sale":"0","discount_info_list":[],"goods_category":"42","kagou_sign":"N","name":"罗森可充气打火机","quantity":1,"row_no":1,"sell_price":3,"total_amount":3.00,"total_discount":0}')]
        // 黑名单商品
        items << ['6901028075831': jsonSlurper.parseText('{"barcode":"6901028075831","commission_sale":"0","discount_info_list":[],"goods_category":"56","kagou_sign":"N","name":"红双喜(硬8mg)","quantity":1,"row_no":1,"sell_price":11,"total_amount":11.00,"total_discount":0}')]

        // YoRen 积分商品 总会包含2个积分商品发送给YoRen测试环境
//        def tmp = jsonSlurper.parseText('{"barcode":"044041","commission_sale":"0","discount_info_list":[{"discount_amount":5.90,"discount_quantity":1.0}],"goods_category":"17","kagou_sign":"N","name":"农夫山泉","quantity":2,"row_no":2,"sell_price":3.8,"total_amount":7.60,"total_discount":0}')
//        i << new OrderItem(*:tmp)
//        tmp = jsonSlurper.parseText('{"barcode":"401022","commission_sale":"0","discount_info_list":[],"goods_category":"01","kagou_sign":"N","name":"臻享饭团(纯牛肉汉堡)1便","quantity":1,"row_no":1,"sell_price":8.9,"total_amount":8.90,"total_discount":0}')
//        i << new OrderItem(*:tmp)

        items.each {k, v -> if(k in barcodes) i << new OrderItem(*:v)}

        if(!(barcodes instanceof ArrayList) )
            i << new OrderItem(*:barcodes)
        return i +  new OrderItem(items['6901028075831']) //额外加一个黑名单商品
    }

    protected getUnionpayPan() {
        def get = new URL("https://open.unionpay.com/tjweb/ij/tool/qrcodeFormPage/coverSweepReceiverApp").openConnection()
        def responsePUID = ''
        if (get.getResponseCode().equals(200)) {
            responsePUID = get.getInputStream().getText()
        }

        Pattern pUID = Pattern.compile("childPageDataLoad\\((\\d*),(.*)\\)", Pattern.CASE_INSENSITIVE);
        Matcher pUidmatcher = pUID.matcher(responsePUID);
        pUidmatcher.find();
        String pUid = Integer.parseInt(pUidmatcher.group(1));

        StringBuilder stringBuilder = new StringBuilder("https://open.unionpay.com/ajweb/help/qrcodeFormPage/sendOk?")
        stringBuilder.append("puid=")
        stringBuilder.append(URLEncoder.encode(pUid, 'UTF-8'))
        stringBuilder.append("&requestType=")
        stringBuilder.append(URLEncoder.encode('coverSweepReceiverApp', "UTF-8"))
        stringBuilder.append("&sendtype=")
        stringBuilder.append(URLEncoder.encode('C2B码申请', "UTF-8"))
        stringBuilder.append("&sendData=")
        stringBuilder.append(URLEncoder.encode("[{\"fid\":523,\"keyword\":\"issCode\",\"value\":\"90880019\"},{\"fid\":525,\"keyword\":\"backUrl\",\"value\":\"http://101.231.204.84:8091/sim/notify_url2.jsp\"},{\"fid\":526,\"keyword\":\"qrType\",\"value\":\"35\"},{\"fid\":527,\"keyword\":\"reqAddnData\",\"value\":\"\"},{\"fid\":646,\"keyword\":\"emvCodeIn\",\"value\":\"\"},{\"fid\":1941,\"keyword\":\"addnCond\",\"value\":\"\"},{\"fid\":2691,\"keyword\":\"resvData\",\"value\":\"\"},{\"fid\":3421,\"keyword\":\"userId\",\"value\":\"usertest\"},{\"fid\":528,\"keyword\":\"accNo\",\"value\":\"6216261000000002485\"},{\"fid\":529,\"keyword\":\"name_payerinfo\",\"value\":\"宋小\"},{\"fid\":530,\"keyword\":\"cardAttr\",\"value\":\"01\"},{\"fid\":531,\"keyword\":\"acctClass\",\"value\":\"1\"},{\"fid\":881,\"keyword\":\"mobile\",\"value\":\"\"},{\"fid\":1781,\"keyword\":\"issCode_payerinfo\",\"value\":\"\"}]", "UTF-8"))


        get = new URL(stringBuilder.toString()).openConnection()

        def resStr = get.getInputStream().getText()
        Pattern pattern = Pattern.compile("qrNo=(\\d*)", Pattern.CASE_INSENSITIVE)
        Matcher matcher = pattern.matcher(resStr)
        matcher.find()
        return matcher.group(1)
    }
}
