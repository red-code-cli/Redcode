package com.redcode.app.data.remote

import android.util.Base64
import okhttp3.Interceptor
import okhttp3.Response

class BasicAuthInterceptor(
    private val passwordProvider: () -> String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val password = passwordProvider()

        if (password.isBlank()) {
            return chain.proceed(original)
        }

        val credentials = "opencode:$password"
        val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        val authenticatedRequest = original.newBuilder()
            .header("Authorization", basicAuth)
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
