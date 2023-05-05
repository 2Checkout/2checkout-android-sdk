package com.twocheckout.mobile


import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import com.twocheckout.connectors.datapack.CreditCardInputResult
import com.twocheckout.connectors.datapack.FormUICustomizationData
import com.twocheckout.connectors.datapack.PayerCardData
import com.twocheckout.connectors.datapack.PaymentConfigurationData
import com.twocheckout.connectors.payments.card.ThreedsManager
import com.twocheckout.connectors.payments.paypal.PaypalStarter
import com.twocheckout.connectors.screens.TwoCheckoutPaymentForm
import com.twocheckout.connectors.screens.TwoCheckoutPaymentOptions
import com.twocheckout.mobile.SettingsActivity.Companion.getShowCard
import com.twocheckout.mobile.SettingsActivity.Companion.getShowPaypal
import com.twocheckout.mobile.dialogs.ErrorDisplayDialog
import com.twocheckout.mobile.dialogs.currency.CurrencySelectorDialog
import com.twocheckout.mobile.http.CheckOrderStatus
import com.twocheckout.mobile.http.HttpAuthenticationAPI
import com.twocheckout.mobile.http.OrdersCardPaymentAPI
import com.verifone.mobile.Json
import kotlinx.android.synthetic.main.activity_checkout.*

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt


/**
 * Checkout implementation for the app
 */
open class CheckoutActivity : AppCompatActivity() {

    companion object{
        var currencyTV = "EUR"
    }

    private var transactionRefNo =""
    private val selectCurrencyButton by lazy { findViewById<AppCompatButton>(R.id.selectCurrencyBtn) }
    private lateinit var currencyDialog: CurrencySelectorDialog
    private var amount: Double = 0.0
    private val itemPrice = 0.01

    private lateinit var garmentList: JSONArray
    private lateinit var selectedGarment: JSONObject

    private lateinit var settingsButton:ImageView
    @Volatile private lateinit var progressDialog: ProgressDialog

    private lateinit var mPaymentBtn:AppCompatButton
    private lateinit var mCreditCardBtnNoThreeds:AppCompatButton
    private val threedsReceiver = createPaymentsReceiver()
    private val paypalReceiver = createPaypalReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        progressDialog = ProgressDialog(this)
        mPaymentBtn = findViewById(R.id.payment_options_btn)
        currencyDialog = CurrencySelectorDialog(::onCurrencyInput)
        mCreditCardBtnNoThreeds = findViewById(R.id.card_payment_btn_no_threeds)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.customView = View.inflate(this, R.layout.custom_action_bar, null)
        // Set up the mock information for our item in the UI.
        settingsButton = findViewById(R.id.settings_btn)
        setupCurrency()
        selectedGarment = fetchRandomGarment()
        displayGarment(selectedGarment)

        mPaymentBtn.setOnClickListener {
            showPaymentOptions()
        }

        mCreditCardBtnNoThreeds.setOnClickListener {

        }

        selectCurrencyButton.setOnClickListener {

            currencyDialog.show(supportFragmentManager,"currency_select")
        }

        settingsButton.setOnClickListener {
            val settingsScreen = Intent(this, SettingsActivity::class.java)
            startActivity(settingsScreen)
        }
    }

    private fun onCurrencyInput(newCurrency:String) {
        if (newCurrency.isNotEmpty() && newCurrency.length==3) {
            currencyTV = newCurrency
            displayGarment(selectedGarment)
            saveCurrencyPrefs(currencyTV)
        }
    }

    private fun saveCurrencyPrefs(currencyStr:String) {
        val sharedPref = getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
        sharedPref.edit().putString("selected_currency",currencyStr).apply()
    }

    private fun getCurrencyPrefs():String{
        val sharedPref = getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
        return sharedPref.getString("selected_currency", "")?:""
    }

    private fun setupCurrency() {
        val temp = getCurrencyPrefs()
        currencyTV = "EUR"
        if (temp.isNotEmpty()){
            currencyTV = temp
        }

    }

    private fun fetchRandomGarment() : JSONObject {
        if (!::garmentList.isInitialized) {
            garmentList = Json.readFromResources(this, R.raw.tshirts)
        }

        val randomIndex:Int = (Math.random() * (garmentList.length() - 1)).roundToInt()
        return garmentList.getJSONObject(randomIndex)
    }

    private fun displayGarment(garment: JSONObject) {
        detailTitle.text = garment.getString("title")

        val price = itemPrice
        detailPrice.text = "$price $currencyTV"
        amount = price.toString().toDouble()

        val escapedHtmlText:String = Html.fromHtml(garment.getString("description")).toString()
        detailDescription.text = Html.fromHtml(escapedHtmlText)

        val imageUri = "@drawable/${garment.getString("image")}"
        val imageResource = resources.getIdentifier(imageUri, null, packageName)
        detailImage.setImageResource(imageResource)
    }

    private fun gotoPaymentDoneScreen(reference: String,transactionType:String ,amount: String,customerParam:String,currencyParam:String) {
        val paymentDone = Intent(this, PaymentFlowDone::class.java)
        paymentDone.putExtra(PaymentFlowDone.keyPayerName, customerParam)
        paymentDone.putExtra(PaymentFlowDone.keyTransactionReference, reference)
        paymentDone.putExtra(PaymentFlowDone.keyTransactionAmount, amount)
        paymentDone.putExtra(PaymentFlowDone.keyTransactionCurrency,currencyParam)
        paymentDone.putExtra(PaymentFlowDone.keyTransactionType,transactionType)
        startActivity(paymentDone)
    }

    private fun showPaymentOptions() {
        val payOptionsList = ArrayList<String>(2)
        if (getShowCard(this)) payOptionsList.add(TwoCheckoutPaymentOptions.paymentOptionCard)
        if (getShowPaypal(this)) payOptionsList.add(TwoCheckoutPaymentOptions.paymentOptionPayPal)

        if (payOptionsList.isEmpty()) {
            Toast.makeText(this,getString(R.string.no_payment_options),Toast.LENGTH_LONG).show()
            return
        }

        val paymentOptionsSheet = TwoCheckoutPaymentOptions(this,payOptionsList,::onPayMethodSelected)
        paymentOptionsSheet.showPaymentOptionList()
    }


    private fun loadFormPreferences():FormUICustomizationData {
        val sharedPref = getSharedPreferences("customization", Context.MODE_PRIVATE)
        val temp = FormUICustomizationData()
        temp.paymentFormBackground = sharedPref.getString(
            SettingsActivity.keySaveBackgroundColor,
            ""
        )?:""
        temp.formTextFieldsBackground = sharedPref.getString(
            SettingsActivity.keySaveTextFieldsColor,
            ""
        )?:""
        temp.formInputTextColor = sharedPref.getString(
            SettingsActivity.keySaveInputTextColor,
            ""
        )?:""
        temp.hintTextColor =sharedPref.getString(SettingsActivity.keySaveHintColor, "")?:""
        temp.payButtonColor = sharedPref.getString(SettingsActivity.keySavePayBtnColor, "")?:""
        temp.formTitleTextColor = sharedPref.getString(SettingsActivity.keySaveTitleColor, "")?:""
        return temp
    }

    private fun gotoCardFormPayment(merchantCode:String) {
        val customizationParam = loadFormPreferences()
        customizationParam.userTextFont = parseFont()
        customizationParam.userTextFontRes = parseFontResource()
        val fullPrice = "$itemPrice $currencyTV"
        setLocation(false)
        val formConfigData = PaymentConfigurationData(
            this,
            fullPrice,
            merchantCode,
            customizationParam
        )


        val cardPaymentForm = TwoCheckoutPaymentForm(
            formConfigData,::showLoadingSpinner,
            ::onCreditCardInput
        )
        val settingsUrlParam  = SettingsActivity.getCardPaymentsUrl(this)
        if (settingsUrlParam.isNotEmpty()){
            cardPaymentForm.overrideTokenURL(settingsUrlParam)
        }
        cardPaymentForm.displayPaymentForm()
    }

    private fun onOrderCheckStatus(result:String){
        progressDialog.dismiss()
        try {
            val statusObj = JSONObject(result)
            val statusResult = statusObj.getString("Status")
            if (statusResult == "AUTHRECEIVED"){
                val referenceNO = statusObj.getString("RefNo")
                gotoPaymentDoneScreen(referenceNO,PaymentFlowDone.TransactionType.typeCreditCard.name,""+itemPrice,"John Doe",
                    currencyTV)
            } else {
                ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
            }
        } catch (e:Exception){
            e.printStackTrace()
            ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
        }
        transactionRefNo = ""
        saveRefNO()
    }


    override fun onStop() {
        super.onStop()
        saveRefNO()
    }


    private fun saveRefNO(){
        val sharedPref = getSharedPreferences("main_screen", Context.MODE_PRIVATE)
        sharedPref.edit().putString("key_transaction_ref",transactionRefNo).apply()
    }

    private fun getRefNO(){
        val sharedPref = getSharedPreferences("main_screen", Context.MODE_PRIVATE)
        transactionRefNo = sharedPref.getString("key_transaction_ref","")?:""
        sharedPref.edit().putString("key_transaction_ref","").apply()
    }

    private fun createPaymentsReceiver(): ActivityResultLauncher<Intent> {
        val actResLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result: ActivityResult ->
                if (result.resultCode == ThreedsManager.threedsResultCode) {
                    if (result.data!=null) {
                        result.data?.let {
                            val refNO = it.getStringExtra(ThreedsManager.keyRefNO)?:""
                            if (refNO.isNotEmpty()){
                                launchOrderStatusCheck(refNO)
                            } else {
                                ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
                            }
                        }
                    }
                }
            }
        return actResLauncher
    }

    private fun createPaypalReceiver(): ActivityResultLauncher<Intent> {
        val actResLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result: ActivityResult ->

                if (result.resultCode == PaypalStarter.paypalResultCode) {
                    if (result.data!=null) {
                        result.data?.let {
                            val refNO = it.getStringExtra(PaypalStarter.keyRefNO)?:""
                            if (refNO.isNotEmpty()){
                                launchOrderStatusCheck(refNO)
                            } else {
                                ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
                            }
                        }
                    }
                }
            }
        return actResLauncher
    }


    private fun launchOrderStatusCheck(authRefParam:String){
        val checkOrderStatus = CheckOrderStatus(::onOrderCheckStatus)
        val httpAuthAPI = HttpAuthenticationAPI()
        httpAuthAPI.secretKey = SettingsActivity.getMerchantSecretKey(this)
        httpAuthAPI.merchantCode = SettingsActivity.getMerchantCode(this)
        try {
            val headersTemp = httpAuthAPI.getHeaders()
            checkOrderStatus.authHeaders = headersTemp
            checkOrderStatus.launchAPI(authRefParam)
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun startThreedsAuth(threedsUrl:String) {
        if(threedsUrl.isEmpty()){
            progressDialog.dismiss()
        }
        val threedsScreen = ThreedsManager(this,threedsUrl)
        threedsScreen.displayThreedsConfirmation(threedsReceiver)
    }

    private fun startPaypalScreen(paypalUrl:String){
        if(paypalUrl.isEmpty()){
            progressDialog.dismiss()
        }
        val mPaypalStarter = PaypalStarter(this,paypalUrl)
        mPaypalStarter.displayPaypalScreen(paypalReceiver)
    }

    private fun getThreedsUrl(response: String): String {
        val responseJson = JSONObject(response)
        var token = ""
        try {
            token = responseJson.getJSONObject("PaymentDetails").getJSONObject("PaymentMethod")
                .getJSONObject("Authorize3DS").getJSONObject("Params").getString("avng8apitoken")
            transactionRefNo = responseJson.getString("RefNo")
        } catch (e:Exception) {
            e.printStackTrace()
        }
        if (token.isEmpty()) return ""
        return responseJson.getJSONObject("PaymentDetails").getJSONObject("PaymentMethod")
            .getJSONObject("Authorize3DS").getString("Href") + "?avng8apitoken=$token"
    }

    private fun getPaypalUrl(response: String): String {
        val responseJson = JSONObject(response)
        try {
            transactionRefNo = responseJson.getString("RefNo")
            return responseJson.getJSONObject("PaymentDetails").getJSONObject("PaymentMethod")
                .getString("RedirectURL")

        } catch (e:Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun onCardPaymentComplete(result:String) {
        progressDialog.dismiss()
        if(result.isEmpty()){
            ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
            return
        }
        val threedsResult = getThreedsUrl(result)

        if (threedsResult.isNotEmpty()){
            startThreedsAuth(threedsResult)
            return
        }
        var transactionStatus = ""
        var reference =""
        try{
            val resultJson = JSONObject(result)
            transactionStatus = resultJson.getString("Status")
            reference = resultJson.getString("RefNo")}
        catch (e:java.lang.Exception) {
            ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
            return
        }
        if (transactionStatus=="AUTHRECEIVED"){
            gotoPaymentDoneScreen(reference,PaymentFlowDone.TransactionType.typeCreditCard.name,""+itemPrice,"John Doe",
                currencyTV)
        } else {
            ErrorDisplayDialog.newInstance("Card transaction status",transactionStatus).show(supportFragmentManager,"error")
        }
        transactionRefNo = ""
        saveRefNO()
    }

    private fun onCreditCardInput(cardPaymentToken:String) {
        if (cardPaymentToken.isEmpty()){
            progressDialog.dismiss()
            ErrorDisplayDialog.newInstance("Card transaction failed","Get token failed").show(supportFragmentManager,"error")
            return
        }
        val ordersCardPayment = OrdersCardPaymentAPI(currencyTV,false,::onCardPaymentComplete)
        val httpAuthAPI = HttpAuthenticationAPI()
        httpAuthAPI.secretKey = SettingsActivity.getMerchantSecretKey(this)
        httpAuthAPI.merchantCode = SettingsActivity.getMerchantCode(this)
        try {
            val headersTemp = httpAuthAPI.getHeaders()
            ordersCardPayment.overridePaymentURL(SettingsActivity.getCardPaymentsUrl(this))
            ordersCardPayment.authHeaders = headersTemp
            ordersCardPayment.launchAPI(cardPaymentToken)
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun onPaypalFlowComplete(result:String){
        progressDialog.dismiss()
        if(result.isEmpty()){
            ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
            return
        }
        val redirectURL = getPaypalUrl(result)

        if (redirectURL.isNotEmpty()){
            startPaypalScreen(redirectURL)
            return
        }
        var transactionStatus = ""
        var reference =""
        try{
            val resultJson = JSONObject(result)
            transactionStatus = resultJson.getString("Status")
            reference = resultJson.getString("RefNo")}
        catch (e:java.lang.Exception) {
            ErrorDisplayDialog.newInstance("Card transaction failed","unknown error").show(supportFragmentManager,"error")
            return
        }
        if (transactionStatus=="AUTHRECEIVED"){
            gotoPaymentDoneScreen(reference,PaymentFlowDone.TransactionType.typeCreditCard.name,""+itemPrice,"John Doe",
                currencyTV)
        } else {
            ErrorDisplayDialog.newInstance("Card transaction status",transactionStatus).show(supportFragmentManager,"error")
        }
        transactionRefNo = ""
        saveRefNO()
    }

    private fun startPaypalFlow(){
        val ordersPaypalPayment = OrdersCardPaymentAPI(currencyTV,true,::onPaypalFlowComplete)
        val httpAuthAPI = HttpAuthenticationAPI()
        httpAuthAPI.secretKey = SettingsActivity.getMerchantSecretKey(this)
        httpAuthAPI.merchantCode = SettingsActivity.getMerchantCode(this)
        try {
            val headersTemp = httpAuthAPI.getHeaders()
            ordersPaypalPayment.authHeaders = headersTemp
            ordersPaypalPayment.launchAPI("")
        } catch (e:Exception) {
            progressDialog.dismiss()
            ErrorDisplayDialog.newInstance("Paypal error","Invalid key").show(supportFragmentManager,"error")
            e.printStackTrace()
        }
    }

    private fun parseFont():Typeface? {
        val fontName = SettingsActivity.getStoredFont(this)
        when (fontName) {
            "Oswald"-> return ResourcesCompat.getFont(this, R.font.oswald_demibold)
            "Pacifico"-> return ResourcesCompat.getFont(this, R.font.pacifico)
            "OpenSans"-> return ResourcesCompat.getFont(this, R.font.open_sans_semibold_italic)
            "QuickSand"-> return ResourcesCompat.getFont(this, R.font.quicksand_italic)
            "Roboto"-> return ResourcesCompat.getFont(this, R.font.roboto_condensed_bold)
            else -> {
                return null
            }
        }
    }

    private fun parseFontResource():Int {
        val fontName = SettingsActivity.getStoredFont(this)
        when (fontName) {
            "Oswald"-> return R.font.oswald_demibold
            "Pacifico"-> return R.font.pacifico
            "OpenSans"-> return R.font.open_sans_semibold_italic
            "QuickSand"-> return R.font.quicksand_italic
            "Roboto"-> return R.font.roboto_condensed_bold
            else -> {
                return 0
            }
        }
    }

    private fun testGetTokenFeature() {
        val cardInputObject = CreditCardInputResult()
        cardInputObject.name = "John Smith"
        val cardPayData = PayerCardData()
        cardPayData.expirationDate = "" //card expiry
        cardPayData.creditCard = ""// card nr
        cardPayData.cvv ="" //card cvc cvv
        cardInputObject.cardData = cardPayData
        TwoCheckoutPaymentForm.getCardPaymentToken(SettingsActivity.getMerchantCode(this),cardInputObject,::onCreditCardInput)
    }

    private fun onPayMethodSelected(payMethod:String) {
        if (payMethod == TwoCheckoutPaymentOptions.paymentOptionPayPal) {
            showLoadingSpinner()
            startPaypalFlow()
        } else if (payMethod == TwoCheckoutPaymentOptions.paymentOptionCard) {
            gotoCardFormPayment(SettingsActivity.getMerchantCode(this))
        }
    }
    private fun showLoadingSpinner() {
        runOnUiThread {
            progressDialog.setTitle("Processing transaction...")
            progressDialog.setCancelable(true)
            progressDialog.show()
        }
    }

    private fun setLocation(setEnglish:Boolean) {
        var langSelected = "EN"
        if (!setEnglish){
            try{
                langSelected = SettingsActivity.getStoredLanguage(this).substring(0,2)
            }catch (e:StringIndexOutOfBoundsException){
                langSelected = "EN"
            }
        }

        val locale = Locale(langSelected)
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        resources.updateConfiguration(config, this.resources.displayMetrics)
    }

}