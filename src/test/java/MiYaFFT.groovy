import com.kargo.LawsonPosHubService
import com.kargo.request.detail.OrderItem
import com.kargo.response.BarcodeResponse
import com.kargo.response.CreatePaymentResponse
import com.kargo.response.GoodsDetailResponse
import com.kargo.response.detail.BillBizInfo
import spock.lang.Shared

class MiYaFFT extends Helper {
    @Shared CreatePaymentResponse createPaymentResponse
    @Shared BarcodeResponse barcodeResponse
    @Shared outTradeNo
    @Shared totalFee = 0.00
    @Shared dev, prd, items, blackItems
    @Shared LawsonPosHubService goodsClient, barcodeClient, createPayment, tradeconfirmClient

    def setupSpec(){
        // 初始化 LawsonPosHubService 参数 https://lawson-poshub.kargotest.com;http://121.43.156.191:21001
        dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'http://127.0.0.1:21001', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //dev = ['mid':'DEFAULT', 'sessionKey':'9Y3SGFCLR2BH4T51', 'kargoUrl':'https://lawson-poshub.kargotest.com', 'store_id':'208888', 'user_id':'00000002',  'pos_id':'01', 'jar_version':'1']
        //prd = ['mid':'DEFAULT', 'sessionKey':'LAWSONJZ2NJKARGO', 'kargoUrl':'http://47.97.19.94:21001', 'store_id':'203118', 'user_id':'20311801',  'pos_id':'01', 'jar_version':'1']

        // 全局out_trade_no, 所有交易相同
        goodsClient = createLawsonPosHubService(dev, '/uploadgoodsdetail')
        barcodeClient = createLawsonPosHubService(dev, '/barcode')
        tradeconfirmClient = createLawsonPosHubService(dev, '/tradeconfirm')
        createPayment = createLawsonPosHubService(dev, '/createpayment')

        outTradeNo = (new Date()).format("ddHHmmssSSS", TimeZone.getTimeZone('Asia/Shanghai'))
    }

    def "call barcode to payment"(){
        given:
        def request = createBarCodeRequest(pan, outTradeNo, totalFee)
        request.setFee_type("1")
        when:
        barcodeResponse = (BarcodeResponse) barcodeClient.execute(request)
        then:
        with(barcodeResponse){
            responseCode == '00'
            ret_code == '00'
            biz_type == '04' // 付费通
        }
        where:
        pan|bizType
        //'573050063357885003819194'|'04'
        '0310061384322705240201000006729026'|'04' // 不需要去fft查询，直接用条码createpayment
        //'034220240220236400027005'|'04' // 码的正则规则在fft_code表里,获取相应的org_id和product_id
        //'0A04102221002402100000004100'|'04'
        //'564020000431234000203605'|'04'
    }

    def "call createpayment"(){
        given:
        BillBizInfo billBizInfo = barcodeResponse.billBizInfos[0]
        def request = createPaymentRequestt(billBizInfo.barcode, outTradeNo, billBizInfo.bill_id, billBizInfo.bill_amt)
        when:
        createPaymentResponse = (CreatePaymentResponse) createPayment.execute(request)
        then:
        with(createPaymentResponse){
            responseCode == '0000'
            ret_code == '00'
        }
        where:
        pan = '573050063357885003819194'
    }
}
