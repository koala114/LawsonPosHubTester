package Core

import com.kargo.LawsonPosHubService
import com.kargo.internal.constants.ClientConstant
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.PaymentConfirmRequest
import com.kargo.response.CreatePaymentResponse
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentConfirmResponse
import com.kargo.response.PaymentRefundResponse
import com.kargo.response.detail.BillBizInfo
import groovy.sql.Sql
import groovy.transform.TupleConstructor
import com.kargo.request.detail.OrderItem
import org.apache.log4j.Logger

@TupleConstructor
class RequestDelegate extends Helper {
    private static final Logger log = Logger.getLogger(RequestDelegate.class)

    RequestDelegate(mid, sessionKey, kargoUrl, store_id){
        super.mid = mid
        super.sessionKey = sessionKey
        super.kargoUrl = kargoUrl
        super.store_id = store_id
        super.user_id = store_id + '01'
        super.pos_id = '02'
        //super.jar_version = ClientConstant.LS_Jar_Version
    }

    def uploadGoodsRequest(String memberNo, String outTradeNo, def items){
        LawsonPosHubService ls = lawsonPosHubService('/uploadgoodsdetail')
        GoodsDetailRequest request = createGoodsDetailRequest(memberNo, outTradeNo, items)
        def response = ls.execute(request)
        return [response, request]
    }

    def uploadGoodsRequest(String outTradeNo, OrderItem item){
        LawsonPosHubService ls = lawsonPosHubService('/uploadgoodsdetail')
        GoodsDetailRequest request = createGoodsDetailRequest(outTradeNo, item)
        def response = ls.execute(request)
        return response
    }

    BarcodeResponse barCodeRequest(String memberNo, String outTradeNo, Double totalFee){
        LawsonPosHubService ls = lawsonPosHubService('/barcode')
        return ls.execute(createBarCodeRequest(memberNo, outTradeNo, totalFee))
    }

    BarcodeResponse barCodeRequest(String memberNo, String outTradeNo){
        LawsonPosHubService ls = lawsonPosHubService('/barcode') // FFT使用该barcode接口
        def barcodeRequest = createBarCodeRequest(memberNo, outTradeNo, null)
        barcodeRequest.setFee_type("1")
        return ls.execute(barcodeRequest)
    }

    ExchangeConfirmResponse exchangeConfirmRequest(String outTradeNo, def coupons, double payAmt, double totalFee){
        LawsonPosHubService ls = lawsonPosHubService('/exchangeconfirm')
        return ls.execute(createExchangeConfirmRequest(outTradeNo, coupons, payAmt, totalFee))
    }

    PaymentConfirmResponse paymentConfirmRequest(def couponList, String memberNo, String outTradeNo, Double totalFee, Double pointAmount, Double prepaidAmount){
        LawsonPosHubService ls = lawsonPosHubService('/tradeconfirm')

        return ls.execute(createPaymentConfirmRequest(couponList, memberNo, outTradeNo, totalFee, pointAmount, prepaidAmount))
    }

    PaymentConfirmResponse paymentConfirmRequest(def couponList, String memberNo, String outTradeNo, Double totalFee, Double pointAmount, Double prepaidAmount, List<OrderItem> orderItemList){
        LawsonPosHubService ls = lawsonPosHubService('/tradeconfirm')
        PaymentConfirmRequest paymentConfirmRequest = createPaymentConfirmRequest(couponList, memberNo, outTradeNo, totalFee, pointAmount, prepaidAmount)
        paymentConfirmRequest.setOrder_items(orderItemList)

        return ls.execute(paymentConfirmRequest)
    }

    CreatePaymentResponse createPaymentRequest(String dynamicId, String outTradeNo, ArrayList<BillBizInfo> billBizInfo){
        LawsonPosHubService ls = lawsonPosHubService('/createpayment')
        def createPaymentRequest = createPaymentRequestt(dynamicId, outTradeNo, billBizInfo[0].bill_id, billBizInfo[0].bill_amt)
        return ls.execute(createPaymentRequest)
    }

    PaymentRefundResponse paymentRefundRequest(String code, String oldTradeNo, Double totalFee){
        LawsonPosHubService ls = lawsonPosHubService('/traderefund')
        def paymentRefundRequest = createPaymentRefundRequest(code, oldTradeNo, totalFee)
        return ls.execute(paymentRefundRequest)
    }

    PaymentRefundResponse paymentRefundRequest(String code, String oldTradeNo){
        LawsonPosHubService ls = lawsonPosHubService('/traderefund')
        def paymentRefundRequest = createPaymentRefundRequest(code, oldTradeNo)
        return ls.execute(paymentRefundRequest)
    }

    private LawsonPosHubService lawsonPosHubService(String miyaUrl){
        return new LawsonPosHubService(mid, store_id, pos_id, kargoUrl, sessionKey, miyaUrl, "", "", "pay")
    }

    public PosHubDBConnector(){
        Sql sql
        sql = Sql.newInstance("jdbc:mysql://47.101.50.215:13306/lawson_hub?autoReconnect=true&useUnicode=true&characterEncoding=utf8",
                "root", "karGo!23456", "com.mysql.jdbc.Driver")
    }

    public KargoHubDBConnector(){
        Sql sql
        sql = Sql.newInstance("jdbc:mysql://47.101.50.215:13306/ks_transaction?autoReconnect=true&useUnicode=true&characterEncoding=utf8",
                "root", "karGo!23456", "com.mysql.jdbc.Driver")
    }
}
