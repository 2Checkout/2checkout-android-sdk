package com.twocheckout.connectors.screens
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.twocheckout.connectors.datapack.CreditCardInputResult
import com.twocheckout.connectors.datapack.PaymentConfigurationData
import com.twocheckout.connectors.payments.card.CardPaymentInit


class TwoCheckoutPaymentForm(displayData: PaymentConfigurationData,onShowLoading:() -> Unit,onPaymentTokenReceived: (token:String) -> Unit) {
    companion object {
        val keyTitleTextColor = "titleTextColorValue"
        val keyFormBackground = "formBackgroundColor"
        val keyTextFieldBackground= "textFieldBackgroundColor"
        val keyInputTextColor = "inputTextColor"
        val keyHintColor = "hintTextColor"
        val keyPayButtonColor = "payButtonColor"
        val keyTextFontID = "textFontResourceID"
        val keyDisplayPrice = "displayPrice"

        fun getCardPaymentToken(merchantCode:String,cardInputObject: CreditCardInputResult,onTokenReady: (token:String) -> Unit) {
            val cardPayments = CardPaymentInit(onTokenReady)
            cardPayments.inputCVV(cardInputObject.cardData.cvv)
            cardPayments.inputScope("null")
            cardPayments.inputCardExpiry(cardInputObject.cardData.expirationDate)
            cardPayments.inputCardNumber(cardInputObject.cardData.creditCard)
            cardPayments.inputPayerName(cardInputObject.name)
            cardPayments.inputMerchantCode(merchantCode)
            cardPayments.launchAPI()
        }
    }

    private var overrideFormURL = ""
    private val showLoading = onShowLoading
    private var mCtx:Context
    private val onPayToken = onPaymentTokenReceived
    private var merchantCode = ""
    private var mFragmentManager:FragmentManager
    private var cardInputScreen:CardFormScreen
    init {
        val ctx = displayData.activityContext
        ctx as AppCompatActivity
        mCtx = displayData.activityContext
        merchantCode = displayData.merchantCodeParam
        cardInputScreen = CardFormScreen(ctx,displayData.displayCustomization,displayData.hideCard,displayData.displayPrice,displayData.payButtonText,::onCardInputComplete)
        mFragmentManager = ctx.supportFragmentManager
    }

    private fun onCardInputComplete(cardInputResult: CreditCardInputResult) {
        val cardPayments = CardPaymentInit(::onPaymentTokenReady)
        cardPayments.inputCVV(cardInputResult.cardData.cvv)
        cardPayments.inputScope("null")
        cardPayments.inputCardExpiry(cardInputResult.cardData.expirationDate)
        cardPayments.inputCardNumber(cardInputResult.cardData.creditCard)
        cardPayments.inputPayerName(cardInputResult.name)
        cardPayments.inputMerchantCode(merchantCode)
        if (overrideFormURL.isNotEmpty()){
            cardPayments.overrideTokenURL(overrideFormURL)
        }
        cardPayments.launchAPI()
        showLoading()
    }

    fun overrideTokenURL(urlParam:String){
        overrideFormURL = urlParam
    }

    private fun onPaymentTokenReady(payToken:String) {
        onPayToken(payToken)
    }

    fun displayPaymentForm() {
        cardInputScreen.show(mFragmentManager,"card_form")
    }
}
