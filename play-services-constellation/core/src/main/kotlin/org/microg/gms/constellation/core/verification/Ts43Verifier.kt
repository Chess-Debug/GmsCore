@file:RequiresApi(Build.VERSION_CODES.O)
@file:SuppressLint("HardwareIds")

package org.microg.gms.constellation.core.verification

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.microg.gms.constellation.core.proto.ChallengeResponse
import org.microg.gms.constellation.core.proto.ClientChallengeResponse
import org.microg.gms.constellation.core.proto.OdsaOperation
import org.microg.gms.constellation.core.proto.ServerChallengeResponse
import org.microg.gms.constellation.core.proto.ServiceEntitlementRequest
import org.microg.gms.constellation.core.proto.Ts43Challenge
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponse
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseError
import org.microg.gms.constellation.core.proto.Ts43ChallengeResponseStatus
import org.microg.gms.constellation.core.verification.ts43.EapAkaService
import org.microg.gms.constellation.core.verification.ts43.builder
import org.microg.gms.constellation.core.verification.ts43.subscriberIdIfPermitted
import org.microg.gms.constellation.core.verification.ts43.userAgent
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "Ts43Verifier"

fun Ts43Challenge.verify(context: Context, subId: Int): ChallengeResponse {
    val baseTelephonyManager = requireNotNull(context.getSystemService<TelephonyManager>()) {
        "TelephonyManager unavailable"
    }
    val telephonyManager = if (subId >= 0) {
        baseTelephonyManager.createForSubscriptionId(subId)
    } else {
        baseTelephonyManager
    }
    val eapAkaService = EapAkaService(telephonyManager)
    val okHttpClient = newTs43HttpClient()
    val httpHistory = mutableListOf<String>()
    val op: OdsaOperation?
    val requestType: Ts43ChallengeResponseError.RequestType
    val routeName: String
    val responseBuilder: (String, List<String>) -> Ts43ChallengeResponse

    when {
        client_challenge != null -> {
            op = client_challenge.operation
            requestType =
                Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_GET_PHONE_NUMBER_API
            routeName = "Client"
            responseBuilder = { responseBody, history ->
                Ts43ChallengeResponse(
                    ts43_type = ts43_type,
                    client_challenge_response = ClientChallengeResponse(
                        get_phone_number_response = responseBody
                    ),
                    http_history = history
                )
            }
        }

        server_challenge != null -> {
            op = server_challenge.operation
            requestType =
                Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_ACQUIRE_TEMPORARY_TOKEN_API
            routeName = "Server"
            responseBuilder = { responseBody, history ->
                Ts43ChallengeResponse(
                    ts43_type = ts43_type,
                    server_challenge_response = ServerChallengeResponse(
                        acquire_temporary_token_response = responseBody
                    ),
                    http_history = history
                )
            }
        }

        else -> {
            return failureResponse(
                this,
                Ts43ChallengeResponseStatus.Code.TS43_STATUS_CHALLENGE_NOT_SET,
                httpHistory
            )
        }
    }

    return try {
        Log.d(TAG, "TS43: $routeName ODSA challenge route: ${op?.operation}")
        if (op == null) {
            return failureResponse(
                this,
                Ts43ChallengeResponseStatus.Code.TS43_STATUS_CHALLENGE_NOT_SET,
                httpHistory
            )
        }

        val requestPayload = buildOdsaRequestPayload(
            context,
            telephonyManager,
            eapAkaService,
            okHttpClient,
            httpHistory,
            entitlement_url,
            eap_aka_realm,
            service_entitlement_request,
            app_id
        ) ?: return failureResponse(
            this,
            Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR,
            httpHistory
        )

        val responseBody = try {
            performOdsaRequest(
                context,
                telephonyManager,
                okHttpClient,
                httpHistory,
                entitlement_url,
                op,
                requestPayload,
                app_id,
                requestType
            )
        } catch (failure: Ts43ApiException) {
            val originalRequest = service_entitlement_request
            if (!Ts43RetryPolicy.shouldRefreshToken(
                    failure.httpStatus,
                    originalRequest?.authentication_token?.isNotEmpty() == true,
                    originalRequest?.temporary_token?.isNotEmpty() == true
                )
            ) {
                throw failure
            }

            // AOSP treats HTTP 511 as an expired cached token. Force a fresh
            // SIM-backed EAP-AKA flow once; neither token may accompany it.
            httpHistory += "RETRY ODSA after expired token"
            val freshRequest = Ts43RetryPolicy.withoutExpiredTokens(
                originalRequest ?: throw failure
            )
            val refreshedPayload = buildOdsaRequestPayload(
                context,
                telephonyManager,
                eapAkaService,
                okHttpClient,
                httpHistory,
                entitlement_url,
                eap_aka_realm,
                freshRequest,
                app_id
            ) ?: throw failure

            // The retry is intentionally not wrapped in another recovery
            // handler. A second 511 is a real error, not an infinite loop.
            performOdsaRequest(
                context,
                telephonyManager,
                okHttpClient,
                httpHistory,
                entitlement_url,
                op,
                refreshedPayload,
                app_id,
                requestType
            )
        }
        ChallengeResponse(
            ts43_challenge_response = responseBuilder(responseBody, httpHistory.toList())
        )
    } catch (e: Ts43ApiException) {
        Log.w(TAG, "TS43 API failure requestType=${e.requestType} http=${e.httpStatus}", e)
        ChallengeResponse(
            ts43_challenge_response = Ts43ChallengeResponse(
                ts43_type = ts43_type,
                status = Ts43ChallengeResponseStatus(
                    error = Ts43ChallengeResponseError(
                        error_code = e.errorCode,
                        http_status = e.httpStatus,
                        request_type = e.requestType
                    )
                ),
                http_history = httpHistory.toList()
            )
        )
    } catch (e: NullPointerException) {
        Log.e(TAG, "TS43 null failure", e)
        failureResponse(
            this,
            Ts43ChallengeResponseStatus.Code.TS43_STATUS_INTERNAL_ERROR,
            httpHistory
        )
    } catch (e: RuntimeException) {
        Log.e(TAG, "TS43 runtime failure", e)
        failureResponse(
            this,
            Ts43ChallengeResponseStatus.Code.TS43_STATUS_RUNTIME_ERROR,
            httpHistory
        )
    }
}

private fun performOdsaRequest(
    context: Context,
    telephonyManager: TelephonyManager,
    okHttpClient: OkHttpClient,
    httpHistory: MutableList<String>,
    entitlementUrl: String,
    op: OdsaOperation,
    req: ServiceEntitlementRequest?,
    challengeAppId: String?,
    requestType: Ts43ChallengeResponseError.RequestType
): String {
    val appIds = listOfNotNull(challengeAppId?.takeIf { it.isNotBlank() })
        .ifEmpty { listOf("ap2014") }
    val builder = op.builder(telephonyManager, req, appIds)
    val url = builder.buildBaseUrl(entitlementUrl)
    val userAgent = req?.userAgent(context) ?: "PRD-TS43 OS-Android/${Build.VERSION.RELEASE}"
    val accept = req?.accept_content_type?.takeIf { it.isNotEmpty() } ?: "application/json"
    val acceptLanguage = Locale.getDefault().toLanguageTag().ifEmpty { "en-US" }
    // The request URL can contain the IMSI, ICCID, EAP identity, and bearer tokens.
    // Keep the operation in diagnostics without returning those values to callers.
    httpHistory += "GET ODSA entitlement"

    val request = Request.Builder()
        .url(url)
        .header("Accept", accept)
        .header("User-Agent", userAgent)
        .header("Accept-Language", acceptLanguage)
        .build()

    return try {
        val response = okHttpClient.newCall(request).execute()
        httpHistory += "RESP ODSA ${response.code}"
        if (response.isSuccessful) {
            response.body?.string()
                ?: throw Ts43ApiException(
                    errorCode = errorCode(32),
                    httpStatus = response.code,
                    requestType = requestType
                )
        } else {
            Log.w(TAG, "ODSA request failed: ${response.code} for ${op.operation}")
            throw Ts43ApiException(
                errorCode = errorCode(31),
                httpStatus = response.code,
                requestType = requestType
            )
        }
    } catch (e: IOException) {
        Log.e(TAG, " Network error in ODSA request", e)
        throw Ts43ApiException(
            errorCode = errorCode(30),
            httpStatus = -1,
            requestType = requestType,
            cause = e
        )
    }
}

private fun buildOdsaRequestPayload(
    context: Context,
    telephonyManager: TelephonyManager,
    eapAkaService: EapAkaService,
    okHttpClient: OkHttpClient,
    httpHistory: MutableList<String>,
    entitlementUrl: String,
    eapAkaRealm: String?,
    req: ServiceEntitlementRequest?,
    challengeAppId: String?
): ServiceEntitlementRequest? {
    if (req == null) return null
    if (req.authentication_token.isNotEmpty() || req.temporary_token.isNotEmpty()) return req

    val mccMnc = telephonyManager.simOperator ?: ""
    // A regular microG install cannot assume Android 10+ privileged identifier access.
    // Without a legitimate IMSI, EAP-AKA cannot proceed; never fabricate one.
    val imsi = telephonyManager.subscriberIdIfPermitted().orEmpty()
    if (mccMnc.length < 5 || imsi.isEmpty()) return null

    val eapId = eapAkaService.buildEapId(mccMnc, imsi, eapAkaRealm)
    val appIds = listOfNotNull(challengeAppId?.takeIf { it.isNotBlank() })
        .ifEmpty { listOf("ap2014") }
    val builder = req.builder(telephonyManager, eapId, appIds)

    val initialUrl = builder.buildBaseUrl(entitlementUrl)
    val postUrl = entitlementUrl
    val userAgent = req.userAgent(context)
    val acceptLanguage = Locale.getDefault().toLanguageTag().ifEmpty { "en-US" }

    var currentRequest = Request.Builder()
        .url(initialUrl)
        .header("Accept", "application/vnd.gsma.eap-relay.v1.0+json")
        .header("User-Agent", userAgent)
        .header("Accept-Language", acceptLanguage)
        .build()
    httpHistory += "GET EAP entitlement"

    for (round in 1..3) {
        val response = try {
            okHttpClient.newCall(currentRequest).execute()
        } catch (e: IOException) {
            Log.e(TAG, "Network error in EAP round $round", e)
            throw Ts43ApiException(
                errorCode = errorCode(30),
                httpStatus = -1,
                requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API,
                cause = e
            )
        }

        val body = response.body?.string() ?: return null
        httpHistory += "RESP EAP round $round ${response.code}"
        if (!response.isSuccessful) {
            Log.w(TAG, "EAP round $round failed with code ${response.code}")
            throw Ts43ApiException(
                errorCode = errorCode(31),
                httpStatus = response.code,
                requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API
            )
        }

        val token = extractAuthToken(body)
        if (token != null) {
            return req.copy(authentication_token = token)
        }

        val eapRelayPacket = extractEapRelayPacket(body)
            ?: throw Ts43ApiException(
                errorCode = errorCode(32),
                httpStatus = response.code,
                requestType = Ts43ChallengeResponseError.RequestType.TS43_REQUEST_TYPE_AUTH_API
            )

        val akaResponse = eapAkaService.performSimAkaAuth(eapRelayPacket, eapId)
            ?: return null

        val postBody = JSONObject().put("eap-relay-packet", akaResponse).toString()
        currentRequest = Request.Builder()
            .url(postUrl)
            .header(
                "Accept",
                "application/vnd.gsma.eap-relay.v1.0+json, text/vnd.wap.connectivity-xml"
            )
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/vnd.gsma.eap-relay.v1.0+json")
            .header("Accept-Language", acceptLanguage)
            .post(
                postBody.toByteArray().toRequestBody(
                    "application/vnd.gsma.eap-relay.v1.0+json".toMediaType()
                )
            )
            .build()
        httpHistory += "POST EAP entitlement"
    }

    return null
}

private fun extractEapRelayPacket(body: String): String? {
    return runCatching { JSONObject(body).optString("eap-relay-packet") }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() }
}

private fun extractAuthToken(body: String): String? {
    runCatching {
        JSONObject(body).optJSONObject("Token")?.optString("token")
    }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }

    return Ts43XmlToken.extract(body)
}

private fun failureResponse(
    challenge: Ts43Challenge,
    statusCode: Ts43ChallengeResponseStatus.Code,
    httpHistory: List<String>
): ChallengeResponse {
    return ChallengeResponse(
        ts43_challenge_response = Ts43ChallengeResponse(
            ts43_type = challenge.ts43_type,
            status = Ts43ChallengeResponseStatus(status_code = statusCode),
            http_history = httpHistory.toList()
        )
    )
}

private class Ts43ApiException(
    val errorCode: Ts43ChallengeResponseError.Code,
    val httpStatus: Int,
    val requestType: Ts43ChallengeResponseError.RequestType,
    cause: Throwable? = null
) : Exception(cause)

private fun errorCode(rawCode: Int): Ts43ChallengeResponseError.Code =
    Ts43ChallengeResponseError.Code.fromValue(rawCode)
        ?: Ts43ChallengeResponseError.Code.TS43_ERROR_CODE_UNSPECIFIED

private fun newTs43HttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val existing = cookieStore.getOrPut(url.host) { mutableListOf() }
                for (cookie in cookies) {
                    existing.removeAll {
                        it.name == cookie.name &&
                                it.domain == cookie.domain &&
                                it.path == cookie.path
                    }
                    existing += cookie
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host]
                    ?.filter { cookie -> cookie.matches(url) }
                    .orEmpty()
            }
        })
        .build()
}
