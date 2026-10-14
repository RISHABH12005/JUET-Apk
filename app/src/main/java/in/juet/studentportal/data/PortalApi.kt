package `in`.juet.studentportal.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * Illustrative API contracts only. These are NOT verified JUET endpoints.
 * Replace them only with API routes documented/approved by JUET administrators.
 */
data class LoginRequest(
    @SerializedName("student_id") val studentId: String,
    val password: String
)
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "Bearer"
)
data class StudentDto(val id: String, val name: String, val semester: String)
data class AttendanceDto(val subject: String, val attended: Int, val total: Int)

interface PortalApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("student/me")
    suspend fun getStudent(@Header("Authorization") bearerToken: String): StudentDto

    @GET("student/attendance")
    suspend fun getAttendance(@Header("Authorization") bearerToken: String): List<AttendanceDto>
}

object ApiClient {
    /** Pass the approved HTTPS base URL. The example URL in BuildConfig is a placeholder. */
    fun create(baseUrl: String): PortalApi {
        require(baseUrl.startsWith("https://")) { "API must use HTTPS." }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PortalApi::class.java)
    }
}
