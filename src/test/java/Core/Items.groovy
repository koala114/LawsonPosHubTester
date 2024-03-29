package Core

import groovy.json.JsonSlurper
import com.kargo.request.detail.OrderItem

class Items {
    public static createItems(def barcodes){
        JsonSlurper jsonSlurper = new JsonSlurper()
        def i = []
        def items = ['6921168509256': jsonSlurper.parseText('{"barcode":"6921168509256","commission_sale":"0","discount_info_list":[],"goods_category":"02","kagou_sign":"N","name":"火腿鸡蛋三明治 1便","quantity":1,"row_no":1,"sell_price":7.5,"total_amount":7.50,"total_discount":0}')]
        items << ['6923127360100': jsonSlurper.parseText('{"barcode":"6923127360100","commission_sale":"0", "discount_info_list":[],"goods_category":"07","kagou_sign":"N","name":"香辣粉丝包","quantity":1.000,"row_no":1,"sell_price":2.50,"total_amount":2.50,"total_discount":0}')]
        items << ['6920459950180': jsonSlurper.parseText('{"barcode":"6920459950180","commission_sale":"0","discount_info_list":[{"discount_amount":6.00,"discount_quantity":2.0}],"goods_category":"18","kagou_sign":"N","name":"贝纳颂咖啡拿铁","quantity":2,"row_no":1,"sell_price":7,"total_amount":14.00,"total_discount":6.00}')]
        items << ['6902538008548': jsonSlurper.parseText('{"barcode":"6902538008548","commission_sale":"0","discount_info_list":[{"discount_amount":5.90,"discount_quantity":1.0}],"goods_category":"17","kagou_sign":"N","name":"达能优白动植蛋白乳饮拿铁味","quantity":1,"row_no":2,"sell_price":9.9,"total_amount":9.90,"total_discount":5.90}')]
        items << ['2501408063102': jsonSlurper.parseText('{"barcode":"2501408063102","commission_sale":"0","discount_info_list":[],"goods_category":"01","kagou_sign":"N","name":"臻享饭团(纯牛肉汉堡)1便","quantity":1,"row_no":1,"sell_price":8.9,"total_amount":8.90,"total_discount":0}')]
        items << ['6920259700053': jsonSlurper.parseText('{"barcode":"6920259700053","commission_sale":"0","discount_info_list":[],"goods_category":"42","kagou_sign":"N","name":"罗森可充气打火机","quantity":1,"row_no":1,"sell_price":3,"total_amount":3.00,"total_discount":0}')]
        items << ['6970399927247': jsonSlurper.parseText('{"barcode":"6970399927247","commission_sale":"0","discount_info_list":[],"goods_category":"18","kagou_sign":"N","name":"★外星人电解质水白桃口味5","quantity":1,"row_no":1,"sell_price":6.5,"total_amount":6.5,"total_discount":0}')]
        items << ['6938888889896': jsonSlurper.parseText('{"barcode":"6938888889896","commission_sale":"0","discount_info_list":[],"goods_category":"17","kagou_sign":"N","name":"香飘飘密谷果汁茶樱桃莓莓","quantity":1,"row_no":2,"sell_price":7,"total_amount":7.0,"total_discount":0}')]
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
        return i
    }
}
