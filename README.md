# 2Checkout ANdroid SDK

**Building the library using Android Studio**

    1) Checkout project using git
    2) Open the project in Android Studio
    3) on the top right side in Android Studio we have the tab called Gradle which brings up a gradle build tasks list
        (if the gradle build tasks list does not show then the option will need to be turned on
        go to: File->Settings->Experimental(tab), 
        in the Experimental tab there is the option: "Do not build Gradle task list during Gradle sync".
        Uncheck this option and then click on: File -> Sync project with Gradle files (this will generate the Gradle task list)
    4) in the gradle build task list we have the module TwoCheckoutAndroidSDK
    Here select the option: TwoCheckoutAndroidSDK->build->assemble (this will build our library debug+release versions)

    The build will be created in outputs directory of the TwoCheckoutAndroidSDK module : "\TwoCheckoutAndroidSDK\build\outputs\aar" 



**Show the available payment options(sample code) :**

    private fun showPaymentOptions() {
        val payOptionsList = ArrayList<String>(2)
        payOptionsList.add(TwoCheckoutPaymentOptions.paymentOptionCard)
        payOptionsList.add(TwoCheckoutPaymentOptions.paymentOptionPayPal)
       
       
        val paymentOptionsSheet = TwoCheckoutPaymentOptions(this,payOptionsList,::onPayMethodSelected)
        paymentOptionsSheet.showPaymentOptionList()
    }
    
    **parameters for TwoCheckoutPaymentOptions class:**
    
    this // activty context
    payOptionList // ArrayList that contains the payment options you want to display to the user
    ::onPayMethodSelected // callback method to receive the payment option selected by the user
    

 **Example callback method for getting the selected payment option:**
 
 
    private fun onPayMethodSelected(payMethod:String) {
    //selected payment method is received as a string so compare with the predefiend strings as in the sample below
    //and start the normal payment flow
        if (payMethod == TwoCheckoutPaymentOptions.paymentOptionPayPal) {
            startPayPalFlow()
        } else if (payMethod == TwoCheckoutPaymentOptions.paymentOptionCard) {
            creditCardPay(true)
        } 
    }    
   

**Card Payments**
**Show the 2Checkout card input screen:**

    val customizationParam = loadFormPreferences()
        customizationParam.userTextFont = parseFont()
        customizationParam.userTextFontRes = parseFontResource()
        val fullPrice = "$itemPrice $currencyTV"
        setLocation(false)
        val formConfigData = PaymentConfigurationData(
            this, //your merchant activity context
            fullPrice, //full price amount + currency as string to display
            merchantCode, //your merchant code
            customizationParam // parameter object to change the card form background , input fields background and text color
            //while this parameter is required and can't be null it can be left empty, so you don't have to specify any 
            //customization values and in this case the default colors will be used.
        )


        val cardPaymentForm = TwoCheckoutPaymentForm(
            formConfigData,//configuration data for payment form as described above
            ::showLoadingSpinner,//callback function that gets called when the blocking api call to retrieve the card payment token  begins
            //typically this will be used to display a loading message or dialog
            ::onCreditCardInput //callback function that gets called when the blocking get card payment token api call returns
        )
       
        //call this function to show the card form after initialization
        cardPaymentForm.displayPaymentForm()
        
**sample callback method for receiving payment card token (::onCreditCardInput) and startin the card payment flow api call**
        
        private fun onCreditCardInput(cardPaymentToken:String) {
            if (cardPaymentToken.isEmpty()){
                progressDialog.dismiss()
                //if card payment token is empty display an error message here
                return
            }
            
            //card payment token received so start normal card payment flow
            //more details about 2co card payment flow can be found here: link!
            
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
    
**sample function for retrieving 2CO card order payment result**
         
    private fun onCardPaymentComplete(result:String) {
        progressDialog.dismiss()
        if(result.isEmpty()){
            //if the result received from the api call is empty display an error message here
            return
        }
        
        //after we get the response string we check to see if 3ds authentication is required
        //the function getThreedsUrl checks the response for an threeds authentication url
        //if a 3ds auth url is present in the response then we need to start 3ds authentication flow like in the code sample below 
        
        .....
        //get the 3ds authentication url to start 3ds flow
        val threedsResult = getThreedsUrl(result)
        if (threedsResult.isNotEmpty()){
            //start the 3ds flow if threeds url is not empty
            startThreedsAuth(threedsResult)
            return
        }
        
        .....
        
        //if 3ds auth url is not present 3ds authentication is not required so we can check
        //the response for the transaction status like in the sample below.
        
        var transactionStatus = ""
        var reference =""
        
        //parse the response string into a json object
        try{
            val resultJson = JSONObject(result)
            transactionStatus = resultJson.getString("Status")
            reference = resultJson.getString("RefNo")}
        catch (e:java.lang.Exception) {
            //if parsing the response failed then transaction is failed so display an error message here
           
            return
        }
        //get the transaction status from the parsed json response and display the status to the payer 
        if (transactionStatus=="AUTHRECEIVED"){
           //payment is succcessfull so display an message to the user
        } else {
            //payment failed so display an error message here
        }
        transactionRefNo = ""
        saveRefNO()
    }
        
**sample 3ds flow for 2CO**

    sample for showing the 3ds auth screen when 3ds url is present in the 2CO card payment response.
    
    private fun startThreedsAuth(threedsUrl:String) {
        if(threedsUrl.isEmpty()){
            progressDialog.dismiss()
        }
        //Create an instance of type ThreedsManager
        val threedsScreen = ThreedsManager(this,threedsUrl)
        //this : Context - merchant activity context 
        //threedsUrl : String - 3ds authentication url received
        
        //display the 2CO threeds authentication screen to the user
        threedsScreen.displayThreedsConfirmation(threedsReceiver)
        //threedsReceiver : ActivityResultLauncher<Intent> used by our 2CO sdk to return 3ds auth result
        //this parameter has to be provided by the merchant like in the sample function below
        
        //code sample for creating the threeds result receiver
        private fun create3DSReceiver(): ActivityResultLauncher<Intent> {
        val actResLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result: ActivityResult ->
                //handling the 3ds result begins here by checking the result and extracting the refno from the intext extra data 
                if (result.resultCode == ThreedsManager.threedsResultCode) {
                    if (result.data!=null) {
                        result.data?.let {
                            //our sdk will return the transaction reference number here after the 3ds is completes
                            //note that this receiver only indicates that the 3ds flow is over and does not indicate if it was succesfull or not
                            //so another api call to get the status of the transaction is required
                            val refNO = it.getStringExtra(ThreedsManager.keyRefNO)?:""
                            if (refNO.isNotEmpty()){
                                launchOrderStatusCheck(refNO)//api call to check transaction status
                            } else {
                                if no refNo is found in the result the transaction failed so display an error message here
                            }
                        }
                    }
                }
            }
        return actResLauncher
    }
    
    }
    
    
**explained sample code for extracting the 3ds auth url from carp payment api response:** 
    
    private fun getThreedsUrl(response: String): String {
        val responseJson = JSONObject(response)
        var token = ""
        try {
            //get avng8apitoken token from json object
            token = responseJson.getJSONObject("PaymentDetails").getJSONObject("PaymentMethod")
                .getJSONObject("Authorize3DS").getJSONObject("Params").getString("avng8apitoken")
            //get transaction reference number
            transactionRefNo = responseJson.getString("RefNo")
        } catch (e:Exception) {
            e.printStackTrace()
        }
        if (token.isEmpty()) return ""
        //get Href string from response json
        val Href = responseJson.getJSONObject("PaymentDetails").getJSONObject("PaymentMethod")
            .getJSONObject("Authorize3DS").getString("Href")
        //the 3ds auth url will be constructed by adding up the Href string and avng8apitoken like in the sample below
        return  Href + "?avng8apitoken=$token"
    }
    
    
**Card tokenization API**

The TwoCheckoutPaymentForm class contains a method that can be used to generate a payment token based on provided card data 

fun getCardPaymentToken(merchantCode:String,cardInputObject: CreditCardInputResult,onTokenReady: (token:String) -> Unit)
merchantCode //your merchant code 
cardInputObject //data object that contains all the required card data for tokenization(card nr, cvv, expiryData, name)
onTokenReady //simple callback funtion that will be called by 2CO sdk with the token parameter once the process is complete

Please note that this is a blocking function that needs to run on background thread.

The code sample below explains how this function is used:

 val cardInputObject = CreditCardInputResult()
        cardInputObject.name = "John Smith"
        val cardPayData = PayerCardData()
        cardPayData.expirationDate = "" //card expiration date
        cardPayData.creditCard = ""// card nr
        cardPayData.cvv ="" //card cvc cvv
        cardInputObject.cardData = cardPayData
        TwoCheckoutPaymentForm.getCardPaymentToken(SettingsActivity.getMerchantCode(this),cardInputObject,::onCreditCardInput)
    
    
**Paypal payment flow** 

    //Starting paypal transaction shown in the code sample below
    
        //Start paypal initial api call (initialize the transaction and get the paypal authorization link)
        //more details about this http api call can be found here: 
        
        "https://app.swaggerhub.com/apis-docs/2Checkout-API/api-rest_documentation/6.0#/Order/put_paymentmethods_PAYPAL_EXPRESS_redirecturl_"
        
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
    
    //After api call completes with success we can retrieve the paypal authorization link. 
    With this authorization link if present we can launch paypal authentication flow from 2CO sdk like in the sample code below:
    
        if(paypalUrl.isEmpty()){
            progressDialog.dismiss()
        }
        
        //Initialize the PaypalStarter class:
        //PaypalStarter(ctx: Context, paypalURL: String)
        ctx : Merchant activity context
        paypalURL : paypal authorization url received in the initial api call
        
        val mPaypalStarter = PaypalStarter(this,paypalUrl)
        
        //display the paypal authorization screen from 2CO sdk
        mPaypalStarter.displayPaypalScreen(paypalReceiver)
        
        displayPaypalScreen(resultObject: ActivityResultLauncher<Intent>)
        //resultObject - ActivityResultLauncher<Intent> used to retrieve the result from 2CO sdk
        
        
        //Sample code for creating the object to receive the result
        
        //since this is an lifecycle owner it needs to be created and registered before merchant activity or fragment is created like in the example below
        private val paypalResultObject = createPaypalReceiver()
        
        //sample function for creating the paypal object to receive the result
        //sample function below return the object as a ActivityResultLauncher<Intent>
        private fun createPaypalReceiver(): ActivityResultLauncher<Intent> {
            val actResLauncher: ActivityResultLauncher<Intent> =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                        result: ActivityResult ->
                    
                    if (result.resultCode == PaypalStarter.paypalResultCode) {
                        if (result.data!=null) {
                            result.data?.let {
                                val refNO = it.getStringExtra(PaypalStarter.keyRefNO)?:""
                                if (refNO.isNotEmpty()){
                                    //if reNO value is present start the check for transaction status here with an api call
                                } else {
                                    //if refNO value is not present transaction has failed so display an error messsage
                                }
                            }
                        }
                    }
                }
            return actResLauncher
        }
        
        
    