import com.kargo.internal.GenerateQRCode
import com.kargo.request.QRCodeRequest
import com.kargo.response.QRCodeRequestResponse

class QRCodeImageGen extends Helper {

    def "call QRCodeRequest"() {
        given:
        QRCodeRequest qrCodeRequest = new QRCodeRequest()
        qrCodeRequest.setOrder_no("2030400122177501")
        qrCodeRequest.setPay_amt(2.50)
        qrCodeRequest.setTimestamp("1688711768156")
        QRCodeRequestResponse qrCodeRequestResponse = GenerateQRCode.generate(qrCodeRequest, "LawsonPoshub@2018");
    }
}
