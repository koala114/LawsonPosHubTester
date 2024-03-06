import com.alibaba.fastjson.JSON
import com.kargo.internal.GenerateQRCode
import com.kargo.request.QRCodeRequest
import com.kargo.response.QRCodeRequestResponse

class QRCodeImageGen extends Helper {

    def "call QRCodeRequest"() {
        given:
        QRCodeRequest qrCodeRequest = new QRCodeRequest()
        qrCodeRequest.setOrder_no("2030400122177504")
        qrCodeRequest.setPay_amt(2.50)
        qrCodeRequest.setTimestamp("1688711768157")
        when:
        QRCodeRequestResponse qrCodeRequestResponse = GenerateQRCode.generate(qrCodeRequest, "LawsonPoshub@2018");
        def result = JSON.toJSON(qrCodeRequestResponse)
        println result
        then:
        result.ret_code == '0000'
    }
}
