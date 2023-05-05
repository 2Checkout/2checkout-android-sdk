package com.twocheckout.mobile.dialogs.currency

class CurrencyAdapterItem {
    private var currencyName: String = ""


    constructor(cName:String) {
        currencyName = cName

    }

    fun getCurrencyName(): String {
        return currencyName
    }
}