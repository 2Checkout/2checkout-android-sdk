package com.twocheckout.connectors.payments.paypal

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URI
import java.net.URL
import java.net.URLDecoder

class PayPalWebClient(onConfirmationDone: (MutableMap<String, String>) -> Unit): WebViewClient() {
    val sendConfirmationResult = onConfirmationDone
    var failUrl = ""
    var successUrl = ""
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val sourceURL = url.toString()
        if (sourceURL.isNotEmpty()&&sourceURL.contains("refNo")) {
            sendConfirmationResult(parseQueryParams(URL(url)))
            return
        }
        if (sourceURL.isNotEmpty() && successUrl.isNotEmpty() && failUrl.isNotEmpty()){
            if(sourceURL.contains(successUrl)||sourceURL.contains(failUrl)){
                sendConfirmationResult(parseQueryParams(URL(url)))
                return
            }
        }
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if(url!=null && url.contains("status")){
            sendConfirmationResult(parseQueryParams(URL(url)))
            return
        }

        if (successUrl.isNotEmpty()&&failUrl.isNotEmpty()){
            if(url!=null && (url.contains(successUrl)||url.contains(failUrl))){
                sendConfirmationResult(parseQueryParams(URL(url)))
                return
            }
        }
    }

    private fun parseQueryParams(urlParam: URL):MutableMap<String, String> {
        val queryPairs: MutableMap<String, String> = LinkedHashMap()
        if (null !== urlParam?.query) {
            val query: String = urlParam.query
            val pairs = query.split("&").toTypedArray()
            for (pair in pairs) {
                if (pair.contains("=")) {
                    val idx = pair.indexOf("=")
                    queryPairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] =
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                }
            }
        }
        return queryPairs
    }
}