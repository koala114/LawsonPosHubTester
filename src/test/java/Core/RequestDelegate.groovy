package Core

import com.kargo.LawsonPosHubService
import com.kargo.internal.constants.ClientConstant
import com.kargo.request.BarCodeRequest
import com.kargo.request.GoodsDetailRequest
import com.kargo.request.detail.OrderItem
import com.kargo.response.ExchangeConfirmResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.BarcodeResponse
import com.kargo.response.PaymentConfirmResponse
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
        return [response, request.total_fee]
    }

    BarcodeResponse barCodeRequest(String memberNo, String outTradeNo, Double totalFee){
        LawsonPosHubService ls = lawsonPosHubService('/barcode')
        return ls.execute(createBarCodeRequest(memberNo, outTradeNo, totalFee))
    }

    ExchangeConfirmResponse exchangeConfirmRequest(String outTradeNo, def coupons, double payAmt, double totalFee){
        LawsonPosHubService ls = lawsonPosHubService('/exchangeconfirm')
        return ls.execute(createExchangeConfirmRequest(outTradeNo, coupons, payAmt, totalFee))
    }

    PaymentConfirmResponse paymentConfirmRequest(def couponList, String memberNo, String outTradeNo, Double totalFee, Double pointAmount, Double prepaidAmount){
        LawsonPosHubService ls = lawsonPosHubService('/tradeconfirm')
        return ls.execute(createPaymentConfirmRequest(couponList, memberNo, outTradeNo, totalFee, pointAmount, prepaidAmount))
    }

    private LawsonPosHubService lawsonPosHubService(String miyaUrl){
        return new LawsonPosHubService(mid, store_id, pos_id, kargoUrl, sessionKey, miyaUrl, "", "", "pay")
    }

}
