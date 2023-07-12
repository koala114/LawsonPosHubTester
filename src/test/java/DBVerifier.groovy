import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.apache.log4j.Logger

import java.sql.SQLException

class DBVerifier {
    private static final Logger log = Logger.getLogger(DBVerifier.class);
    Sql sql
    def failedResults = []

    public DBVerifier(){
        sql = Sql.newInstance("jdbc:mysql://kargo-obp-dev-public.mysql.rds.aliyuncs.com/lawson_hub?autoReconnect=true&useUnicode=true&characterEncoding=utf8",
                "lawson_hub_dev", "lawson_hub_Dev_123", "com.mysql.jdbc.Driver")
    }

    public validateOltp(Map params) {
        log.info("验证数据-----------------------------------------------------------------")
        params.each {k,v -> validate(k,v) }
        return failedResults
    }

    public validate(leg, expectValue) {
        def oltp
        def slurper = new JsonSlurper()
        def expectHttpBody
        def actHttpBody

        String tmp = ''
        log.info("开始检查用例 ------------------------------${leg} ------------------------------ ")
        expectValue.each{k, v -> tmp += k + ','}
        tmp = tmp.substring(0, tmp.lastIndexOf(','))
        String statement = "SELECT ${tmp} FROM oltp_txn_log_digital WHERE leg='${leg}' AND (stan='${expectValue.stan}' or out_trade_no='${expectValue.out_trade_no}') AND transaction_type='${expectValue.transaction_type}'"
        log.info(statement)
        log.info("期望 ${leg}: " + this.sort(expectValue).toString())
        try{
            oltp = sql.rows(statement)
            log.info("实际 ${leg}: " +this.sort(oltp?oltp[0]:[]).toString())
        }catch (SQLException e){log.info(e.printStackTrace())}

        def Leg4HTTPBodyTemp = slurper.parseText('{"responseCode":"0000","responseMessage":"Txn completed successfully","storeId":"208888","tradeNo":"208888011357284441","outTradeNo":"20888801135728444","biz_content":{"ret_code":"00","ret_msg":"请求成功","biz_type":"02","out_trade_no":"20888801135728444","trade_no":"208888011357284441","pay_id":"038","pay_code":"038","pay_name":"罗森点点","user_info":{"level":"01","total_point":-111862,"total_point_amount":0.0,"total_prepaid_amount":0,"mobile":"13818595461","name":"微信用户","status":0,"code":"1900267772339","kbn":"1"},"extraInfo":"{\\"memberAmount\\":0,\\"memberAmountFixed\\":0,\\"memberAmountFlg\\":0,\\"memberAmountFree\\":0,\\"memberAmountLimited\\":0,\\"memberAmountLocked\\":0,\\"memberPromotion\\":\\"0\\",\\"memberPromotionRec\\":\\"\\"}"}}')

        // 比较http_body
        if(expectValue['http_body']){
            expectHttpBody = slurper.parseText(expectValue['http_body']).sort()
            if("${leg}" == 'LEG_4'){
                String tradeNo = expectHttpBody.trade_no
                Leg4HTTPBodyTemp.tradeNo = tradeNo
                Leg4HTTPBodyTemp.outTradeNo = expectHttpBody.out_trade_no
                Leg4HTTPBodyTemp.biz_content.trade_no = tradeNo
                Leg4HTTPBodyTemp.biz_content.out_trade_no = expectHttpBody.out_trade_no
                Leg4HTTPBodyTemp.biz_content.biz_type = expectHttpBody.biz_type
                Leg4HTTPBodyTemp.biz_content.user_info = expectHttpBody.user_info
                Leg4HTTPBodyTemp.storeId = tradeNo.substring(0, 6)
                expectHttpBody = Leg4HTTPBodyTemp.sort()
            }
            else
                expectHttpBody.total_fee = String.valueOf(expectHttpBody.total_fee) // total_fee 转换成字符串后比较

            actHttpBody = slurper.parseText(oltp[0]['http_body']).sort()
            expectValue.remove('http_body')
            oltp[0].remove('http_body')
        }

        if (expectHttpBody != actHttpBody || oltp==null || expectValue != oltp[0]) {
            failedResults << leg
            log.info("结束检查用例 ------------------------------${leg} 失败 ------------------------------ ")
            return false
        }
        else {
            log.info("结束检查用例 ------------------------------${leg} 成功 ------------------------------ ")
            return true
        }
    }

    public Map sort(obj){
        if(obj.size() == 0){
            return null;
        }
        return obj.sort()
        //return obj.sort(){a, b -> b.value <=> a.value}
    }
}