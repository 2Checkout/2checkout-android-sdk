package com.twocheckout.connectors.datapack

import android.content.Context

class PaymentConfigurationData(
    ctx: Context,
    price: String = "",
    merchantCode: String,
    displayUICustomization: FormUICustomizationData
) {

    var activityContext:Context = ctx
    var displayPrice:String= price
    var payButtonText:String = ""
    var merchantCodeParam:String = merchantCode
    var hideCard = false
    var displayCustomization: FormUICustomizationData = displayUICustomization

}