package com.twocheckout.mobile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.textfield.TextInputEditText
import com.twocheckout.connectors.payments.card.ThreedsManager
import com.twocheckout.mobile.dialogs.ErrorDisplayDialog

class SettingsActivity : AppCompatActivity() {


    lateinit var showCardCheck: CheckBox
    lateinit var showPaypalCheck: CheckBox
    lateinit var saveButton:AppCompatButton
    lateinit var cancelButton:AppCompatButton

    lateinit var backgroundColorInput: TextInputEditText
    lateinit var backgroundColorSample: AppCompatTextView

    lateinit var textFieldBackgroundInput: TextInputEditText
    lateinit var textFieldBackgroundSample: AppCompatTextView

    lateinit var textInputColor: TextInputEditText
    lateinit var textInputColorSample: AppCompatTextView

    lateinit var textHintColor: TextInputEditText
    lateinit var textHintColorSample: AppCompatTextView

    lateinit var payButtonColorCode: TextInputEditText
    lateinit var payButtonColorSample: AppCompatTextView

    lateinit var cardTitleColorCode: TextInputEditText
    lateinit var cardTitleColorSample: AppCompatTextView

    val merchantCodeInput by lazy { findViewById<TextInputEditText>(R.id.merchant_code_edit) }
    val merchantSecretInput by lazy {findViewById<TextInputEditText>(R.id.merchant_secret_key_edit)}
    val cardPaymentsUrlInput by lazy {findViewById<TextInputEditText>(R.id.card_payments_url_edit)}
    val selectLanguageBtn by lazy { findViewById<AppCompatButton>(R.id.change_lang_btn) }
    val selectedLangDisplay by lazy { findViewById<AppCompatTextView>(R.id.setup_language_display) }
    val langScreenReceiver = createLanguageSelectReceiver()

    companion object{
        const val keyMerchantSecret = "merchant_secret_key"
        const val keyMerchantCode = "merchant_code"
        const val keySaveBackgroundColor="pref_background_color"
        const val keySaveInputTextColor="pref_input_text_color"
        const val keySaveTextFieldsColor="pref_text_fields_color"
        const val keySaveHintColor="pref_hint_color"
        const val keySavePayBtnColor="pref_pay_btn_color"
        const val keySaveTitleColor="pref_title_color"
        const val keyCardPaymentsUrl="card_payments_url"

        fun saveShowCard(ctx:Context,showCard:Boolean) {
            val sp = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            sp.edit().putBoolean("key_store_show_card",showCard).apply()
        }

        fun getShowCard(ctx:Context):Boolean {
            val sharedPref = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            return sharedPref.getBoolean("key_store_show_card",false)
        }

        fun saveShowPaypal(ctx:Context,showPaypal:Boolean) {
            val sp = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            sp.edit().putBoolean("key_store_show_paypal",showPaypal).apply()
        }

       fun getShowPaypal(ctx:Context):Boolean {
            val sharedPref = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            return sharedPref.getBoolean("key_store_show_paypal",false)
        }



        fun getCardPaymentsUrl(ctx:Context):String {
            val sharedPref = ctx.getSharedPreferences("payment_settings_data", Context.MODE_PRIVATE)
            return sharedPref.getString(keyCardPaymentsUrl,"")?:""
        }

        fun getMerchantSecretKey(ctx:Context):String {
            val sharedPref = ctx.getSharedPreferences("payment_settings_data", Context.MODE_PRIVATE)
            return sharedPref.getString(keyMerchantSecret,"")?:""
        }

        fun getMerchantCode(ctx:Context):String {
            val sharedPref = ctx.getSharedPreferences("payment_settings_data", Context.MODE_PRIVATE)
            return sharedPref.getString(keyMerchantCode,"")?:""
        }
        fun getStoredFont(ctx:Context):String {
            val sharedPref = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            return sharedPref.getString("key_store_font","")?:""
        }
        fun getStoredLanguage(ctx:Context):String {
            val sharedPref = ctx.getSharedPreferences("checkout_data", Context.MODE_PRIVATE)
            return sharedPref.getString("key_store_lang","")?:""
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_screen_layout)
        showCardCheck = findViewById(R.id.checkbox_option_card)
        showPaypalCheck = findViewById(R.id.checkbox_option_paypal)
        saveButton = findViewById(R.id.settings_save_btn)
        cancelButton = findViewById(R.id.settings_cancel_btn)
        showCardCheck.isChecked = getShowCard(this)
        showPaypalCheck.isChecked = getShowPaypal(this)

        backgroundColorInput = findViewById(R.id.form_background_input)
        backgroundColorSample = findViewById(R.id.form_background_sample)

        textFieldBackgroundInput = findViewById(R.id.text_field_background_input)
        textFieldBackgroundSample = findViewById(R.id.text_field_background_sample)

        textInputColor = findViewById(R.id.text_input_color_code)
        textInputColorSample = findViewById(R.id.text_input_color_sample)

        textHintColor = findViewById(R.id.text_hint_color_code)
        textHintColorSample = findViewById(R.id.text_hint_color_sample)

        payButtonColorCode = findViewById(R.id.pay_button_color_code)
        payButtonColorSample = findViewById(R.id.pay_button_color_sample)

        cardTitleColorCode = findViewById(R.id.card_title_color_code)
        cardTitleColorSample = findViewById(R.id.card_title_color_sample)

        saveButton = findViewById(R.id.settings_save_btn)
        cancelButton = findViewById(R.id.settings_cancel_btn)
        selectedLangDisplay.text = getStoredLanguage(this)
        selectLanguageBtn.setOnClickListener {
           gotoLangSelectionScreen()
        }

        loadFormDetails()
        loadPaymentDetails()
        saveButton.setOnClickListener {
            saveShowCard(this,showCardCheck.isChecked)
            saveShowPaypal(this,showPaypalCheck.isChecked)
            saveFormDetails()
            savePaymentDetails()
            finish()
        }
        cancelButton.setOnClickListener {
            finish()
        }
        backgroundColorInput.addTextChangedListener(textWatchBackground)
        textFieldBackgroundInput.addTextChangedListener(textWatchTextFields)
        textInputColor.addTextChangedListener(textWatchInputTextColor)
        textHintColor.addTextChangedListener(textWatchHintColor)
        cardTitleColorCode.addTextChangedListener(textWatchTitleCardColor)
        payButtonColorCode.addTextChangedListener(textWatchPayButtonColor)
        setSampleColor(backgroundColorInput.text.toString(),backgroundColorSample)
        setSampleColor(textFieldBackgroundInput.text.toString(),textFieldBackgroundSample)
        setSampleColor(textInputColor.text.toString(),textInputColorSample)
        setSampleColor(textHintColor.text.toString(),textHintColorSample)
        setSampleColor(payButtonColorCode.text.toString(),payButtonColorSample)
        setSampleColor(cardTitleColorCode.text.toString(),cardTitleColorSample)
    }


    fun setSampleColor(code:String, sampleView:AppCompatTextView){
        if (code.length<6) return
        val colorCode = parseColor(code.toString())
        if (colorCode!=0) sampleView.setBackgroundColor(colorCode)
    }

    private val textWatchBackground = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {
            setSampleColor(s.toString(),backgroundColorSample)
        }
    }

    private val textWatchTextFields = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {

            setSampleColor(s.toString(),textFieldBackgroundSample)
        }
    }

    private val textWatchInputTextColor = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {
            setSampleColor(s.toString(),textInputColorSample)
        }
    }

    private val textWatchHintColor = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {
            setSampleColor(s.toString(),textHintColorSample)
        }
    }

    private val textWatchPayButtonColor = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {
            setSampleColor(s.toString(),payButtonColorSample)
        }
    }

    private val textWatchTitleCardColor = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, aft: Int) {}
        override fun afterTextChanged(s: Editable) {
            setSampleColor(s.toString(),cardTitleColorSample)
        }
    }

    fun parseColor(colorCodeParam:String):Int {
        return try {
            val colorCode: Int = Color.parseColor(colorCodeParam)
            colorCode
        } catch (e: Exception) {
            0
        }
    }

    private fun saveFormDetails() {
        val sharedPref = getSharedPreferences("customization", Context.MODE_PRIVATE)
        sharedPref.edit().putString(keySaveBackgroundColor,backgroundColorInput.text.toString()).apply()
        sharedPref.edit().putString(keySaveInputTextColor,textInputColor.text.toString()).apply()
        sharedPref.edit().putString(keySaveTextFieldsColor,textFieldBackgroundInput.text.toString()).apply()
        sharedPref.edit().putString(keySaveHintColor,textHintColor.text.toString()).apply()
        sharedPref.edit().putString(keySavePayBtnColor,payButtonColorCode.text.toString()).apply()
        sharedPref.edit().putString(keySaveTitleColor,cardTitleColorCode.text.toString()).apply()
    }

    private fun savePaymentDetails(){
        val sharedPref = getSharedPreferences("payment_settings_data", Context.MODE_PRIVATE)
        sharedPref.edit().putString(keyMerchantCode,merchantCodeInput.text.toString()).apply()
        sharedPref.edit().putString(keyMerchantSecret,merchantSecretInput.text.toString()).apply()
        sharedPref.edit().putString(keyCardPaymentsUrl,cardPaymentsUrlInput.text.toString()).apply()
    }

    private fun loadFormDetails() {
        val sharedPref = getSharedPreferences("customization", Context.MODE_PRIVATE)
        backgroundColorInput.setText(sharedPref.getString(keySaveBackgroundColor,""))
        textInputColor.setText(sharedPref.getString(keySaveInputTextColor,""))
        textFieldBackgroundInput.setText(sharedPref.getString(keySaveTextFieldsColor,""))
        textHintColor.setText(sharedPref.getString(keySaveHintColor,""))
        payButtonColorCode.setText(sharedPref.getString(keySavePayBtnColor,""))
        cardTitleColorCode.setText(sharedPref.getString(keySaveTitleColor,""))
    }

    private fun loadPaymentDetails(){
        val sharedPref = getSharedPreferences("payment_settings_data", Context.MODE_PRIVATE)
        merchantCodeInput.setText(sharedPref.getString(keyMerchantCode,""))
        merchantSecretInput.setText(sharedPref.getString(keyMerchantSecret,""))
        cardPaymentsUrlInput.setText(sharedPref.getString(keyCardPaymentsUrl,""))
    }

    fun gotoLangSelectionScreen() {
        val langScreen = Intent(this,LanguageSelection::class.java)
        langScreenReceiver.launch(langScreen)
    }

    private fun saveLanguage(lang:String) {
        if (lang.isEmpty()) return
        val sp = getSharedPreferences("checkout_data",Context.MODE_PRIVATE)
        sp.edit()
            .putString("key_store_lang",lang)
            .apply()
    }

    private fun createLanguageSelectReceiver(): ActivityResultLauncher<Intent> {
        val actResLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result: ActivityResult ->
                if (result.resultCode == LanguageSelection.resultCode) {
                    if (result.data!=null) {
                        result.data?.let {
                            val selectedLang = it.getStringExtra(LanguageSelection.keySelectedLand)?:""
                            if (selectedLang.isNotEmpty()) {
                                selectedLangDisplay.text = selectedLang
                                saveLanguage(selectedLang)
                            }
                        }
                    }
                }
            }
        return actResLauncher
    }
}