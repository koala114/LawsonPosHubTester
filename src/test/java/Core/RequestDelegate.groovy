package Core

import com.kargo.LawsonPosHubService
import com.kargo.internal.constants.ClientConstant
import com.kargo.request.BarCodeRequest
import com.kargo.request.CreatePaymentRequest
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.CreatePaymentResponse
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentConfirmResponse
import com.kargo.response.detail.BillBizInfo
import groovy.transform.TupleConstructor
import com.kargo.request.detail.OrderItem
import groovy.json.JsonSlurper
import org.apache.log4j.Logger

@TupleConstructor
class RequestDelegate extends Helper {
    private static final Logger log = Logger.getLogger(RequestDelegate.class);

    RequestDelegate(){
        super.mid = mid
        super.sessionKey = sessionKey
        super.kargoUrl = kargoUrl
        super.store_id = store_id
        super.user_id = user_id
        super.pos_id = pos_id
        super.jar_version = ClientConstant.JAR_VERSION

    }

    def uploadGoodsRequest(String memberNo, String outTradeNo, def items, def blackItems){
        LawsonPosHubService ls = lawsonPosHubService('/uploadgoodsdetail')
        GoodsDetailRequest request = createGoodsDetailRequest(memberNo, outTradeNo, items, blackItems)
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

    CreatePaymentResponse createPaymentRequest(String dynamicId, String outTradeNo, ArrayList<BillBizInfo> billBizInfo){
        LawsonPosHubService ls = lawsonPosHubService('/createpayment')
        def createPaymentRequest = createPaymentRequestt(dynamicId, outTradeNo, billBizInfo[0].bill_id, billBizInfo[0].bill_amt)
        return ls.execute(createPaymentRequest)
    }

    private LawsonPosHubService lawsonPosHubService(String miyaUrl){
        return new LawsonPosHubService(mid, store_id, pos_id, kargoUrl, sessionKey, miyaUrl, "", "", "pay")
    }

}
