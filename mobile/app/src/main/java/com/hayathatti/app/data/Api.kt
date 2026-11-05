package com.hayathatti.app.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

interface ApiService {
    @GET("api/health")
    suspend fun health(): HealthDto

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @GET("api/signals")
    suspend fun signals(@Query("limit") limit: Int, @Query("offset") offset: Int, @Query("type") type: String?): List<SignalDto>

    @GET("api/signals/mongo")
    suspend fun signalsMongo(@Query("limit") limit: Int, @Query("offset") offset: Int): List<SignalDto>

    @GET("api/ble-data")
    suspend fun bleData(@Query("limit") limit: Int, @Query("offset") offset: Int): List<BleDto>

    @POST("api/signals")
    suspend fun createSignal(@Body body: SignalCreate): SignalDto

    @POST("api/ble-data")
    suspend fun createBle(@Body body: BleCreate): JsonObject

    @GET("api/disasters")
    suspend fun disasters(@Query("source") source: String): List<DisasterDto>
}

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    // Backend emergency alanını zorunlu ister; varsayılan değerli alanlar da gönderilmeli.
    encodeDefaults = true
}

object ApiFactory {
    /** Adres ve token her istekte güncel değerden okunur; Sunucu ayarı değişince yeniden kurulum gerekmez. */
    fun create(baseUrl: () -> String, token: () -> String?): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                baseUrl().toHttpUrlOrNull()?.let { target ->
                    builder.url(
                        original.url.newBuilder().scheme(target.scheme).host(target.host).port(target.port).build(),
                    )
                }
                token()?.let { builder.header("Authorization", "Bearer $it") }
                chain.proceed(builder.build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

enum class ErrKind { NETWORK, UNAUTHORIZED, REJECTED, SERVER }

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String, val kind: ErrKind) : ApiResult<Nothing>
}

object ApiErrors {
    const val NETWORK = "Sunucuya ulaşılamıyor. İnternetinizi veya sunucu adresini kontrol edin."

    fun fromStatus(code: Int, overrides: Map<Int, String> = emptyMap()): ApiResult.Err {
        overrides[code]?.let {
            return ApiResult.Err(it, if (code == 401) ErrKind.UNAUTHORIZED else ErrKind.REJECTED)
        }
        return when (code) {
            401 -> ApiResult.Err("Oturumunuzun süresi doldu. Yeniden giriş yapın.", ErrKind.UNAUTHORIZED)
            in 400..499 -> ApiResult.Err("Gönderilen bilgiler kabul edilmedi. Alanları kontrol edin.", ErrKind.REJECTED)
            else -> ApiResult.Err("Sunucuda bir sorun oluştu. Biraz sonra yeniden deneyin.", ErrKind.SERVER)
        }
    }
}

suspend fun <T> apiCall(overrides: Map<Int, String> = emptyMap(), block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Ok(block())
    } catch (e: HttpException) {
        ApiErrors.fromStatus(e.code(), overrides)
    } catch (e: IOException) {
        ApiResult.Err(ApiErrors.NETWORK, ErrKind.NETWORK)
    } catch (e: SerializationException) {
        ApiResult.Err("Sunucudan beklenmeyen bir yanıt geldi.", ErrKind.SERVER)
    }
