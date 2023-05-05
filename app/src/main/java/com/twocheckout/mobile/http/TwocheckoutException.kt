package com.twocheckout.mobile.http

class TwoCheckoutException(error:String,errorNr:Int,e:Exception): Throwable() {
    val exceptionObj = e
    val errorMessage = error
    val nr = errorNr
}