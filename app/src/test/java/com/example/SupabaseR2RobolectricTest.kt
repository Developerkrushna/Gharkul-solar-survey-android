package com.example

import android.app.Application
import android.net.Uri
import android.os.Looper
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.example.data.InMemorySurveyRepository
import com.example.data.LocationRepository
import com.example.data.remote.AccessRevokedException
import com.example.data.remote.CustomVillageRecord
import com.example.data.remote.SupabaseMappers
import com.example.data.remote.SupabaseSettings
import com.example.data.remote.SupabaseSurveyRepository
import com.example.model.AppScreen
import com.example.model.DocumentEntry
import com.example.model.DocumentLegibility
import com.example.model.DocumentType
import com.example.model.ExecutiveUser
import com.example.model.InstallationPhotoItem
import com.example.model.InstallationQueueFilter
import com.example.model.InstallationReport
import com.example.model.LoginRole
import com.example.model.NameMatchResult
import com.example.model.SerialVerificationField
import com.example.model.defaultInstallationProductRows
import com.example.model.defaultPanelSerialEntries
import com.example.viewmodel.SurveyAppViewModel
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseR2RobolectricTest {

  private lateinit var app: Application
  private val testSettings =
    SupabaseSettings(
      url = "https://demo-project.supabase.co",
      anonKey = "test-anon-key-123456",
    )

  private data class CapturedCall(
    val method: String,
    val url: String,
    val bodyText: String,
    val bodyLength: Int,
  )

  private val calls = CopyOnWriteArrayList<CapturedCall>()
  private var mockExecutiveId = "exec-uuid-1"
  private var mockExecutiveRole = "SURVEY_EXECUTIVE"
  private var mockExecutiveRoleMr = "फील्ड एक्झिक्युटिव्ह"
  private var mockExecutiveActive = true
  private var mockAssignedDistrict = "अहिल्यानगर"
  private var mockAssignedTaluka = "संगमनेर"
  private var mockAssignedVillage = "सर्व गाव"
  private val storedSurveys = JSONArray()
  private val storedReports = JSONArray()
  private var uploadCounter = 0
  private var simulateNetworkDown = false

  private val mockOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val url = req.url.toString()
        val method = req.method
        val buf = Buffer()
        req.body?.writeTo(buf)
        val rawBytes = buf.readByteArray()
        val bodyText = if (method == "PUT") "" else rawBytes.toString(Charsets.UTF_8)
        calls.add(CapturedCall(method, url, bodyText, rawBytes.size))

        if (simulateNetworkDown) {
          throw java.io.IOException("Simulated offline network")
        }

        when {
          url.contains("/auth/v1/token") -> {
            val json =
              JSONObject()
                .put("access_token", "jwt-access-token-123")
                .put("refresh_token", "jwt-refresh-token-123")
                .put(
                  "user",
                  JSONObject()
                    .put("id", "auth-uuid-1")
                    .put("email", "ramesh.k@gharkulsolar.in"),
                )
            jsonResponse(req, 200, json.toString())
          }

          url.contains("/auth/v1/logout") -> {
            jsonResponse(req, 204, "")
          }

          url.contains("/rest/v1/executives") && method == "GET" -> {
            val execArr =
              JSONArray().put(
                JSONObject()
                  .put("id", mockExecutiveId)
                  .put("auth_user_id", "auth-uuid-1")
                  .put("email", "ramesh.k@gharkulsolar.in")
                  .put("name_mr", "रमेश कुलकर्णी")
                  .put("first_name_mr", "रमेश")
                  .put("mobile", "9823011223")
                  .put("role", mockExecutiveRole)
                  .put("role_mr", mockExecutiveRoleMr)
                  .put("assigned_district", mockAssignedDistrict)
                  .put("assigned_taluka", mockAssignedTaluka)
                  .put("assigned_village", mockAssignedVillage)
                  .put("is_active", mockExecutiveActive),
              )
            jsonResponse(req, 200, execArr.toString())
          }

          url.contains("/rest/v1/executives") && method == "PATCH" -> {
            jsonResponse(req, 204, "")
          }

          url.contains("/rest/v1/custom_villages") && method == "GET" -> {
            jsonResponse(req, 200, "[]")
          }

          url.contains("/rest/v1/custom_villages") && method == "POST" -> {
            jsonResponse(req, 201, "")
          }

          url.contains("/rest/v1/customer_surveys") && method == "GET" -> {
            jsonResponse(req, 200, storedSurveys.toString())
          }

          url.contains("/rest/v1/customer_surveys") && method == "POST" -> {
            jsonResponse(req, 201, "")
          }

          url.contains("/rest/v1/customer_surveys") && method == "DELETE" -> {
            jsonResponse(req, 204, "")
          }

          url.contains("/rest/v1/survey_documents") && method == "POST" -> {
            jsonResponse(req, 201, "")
          }

          url.contains("/rest/v1/installation_reports") && method == "GET" -> {
            jsonResponse(req, 200, storedReports.toString())
          }

          url.contains("/rest/v1/installation_reports") && method == "POST" -> {
            jsonResponse(req, 201, "")
          }

          url.contains("/rest/v1/installation_photos") && (method == "DELETE" || method == "POST") -> {
            jsonResponse(req, 200, "")
          }

          url.contains("/functions/v1/r2-presign") -> {
            val payload = JSONObject(bodyText)
            val action = payload.optString("action")
            if (action == "delete") {
              val count = payload.optJSONArray("object_keys")?.length() ?: 0
              jsonResponse(req, 200, JSONObject().put("deleted", count).toString())
            } else {
              uploadCounter++
              val folder = payload.getString("folder")
              val recordId = payload.getString("recordId")
              val fileName = payload.getString("fileName")
              val objectKey = "$folder/$recordId/v${uploadCounter}_$fileName"
              val respJson =
                JSONObject()
                  .put("upload_url", "https://r2-upload.example.invalid/$objectKey?X-Amz-Signature=abc")
                  .put("public_url", "https://pub.example.r2.dev/$objectKey")
                  .put("object_key", objectKey)
              jsonResponse(req, 200, respJson.toString())
            }
          }

          url.startsWith("https://r2-upload.example.invalid/") && method == "PUT" -> {
            Response.Builder()
              .request(req)
              .protocol(Protocol.HTTP_1_1)
              .code(200)
              .message("OK")
              .body("".toResponseBody(null))
              .build()
          }

          else -> jsonResponse(req, 404, "{\"error\":\"Unmocked $method $url\"}")
        }
      }
      .build()
  }

  private fun jsonResponse(req: Request, code: Int, body: String): Response =
    Response.Builder()
      .request(req)
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message(if (code in 200..299) "OK" else "Error")
      .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
      .build()

  @Before
  fun setup() {
    app = ApplicationProvider.getApplicationContext()
    calls.clear()
    uploadCounter = 0
    simulateNetworkDown = false
    mockExecutiveId = "exec-uuid-1"
    mockExecutiveRole = "SURVEY_EXECUTIVE"
    mockExecutiveRoleMr = "फील्ड एक्झिक्युटिव्ह"
    mockExecutiveActive = true
    mockAssignedDistrict = "अहिल्यानगर"
    mockAssignedTaluka = "संगमनेर"
    mockAssignedVillage = "सर्व गाव"
    while (storedSurveys.length() > 0) storedSurveys.remove(0)
    while (storedReports.length() > 0) storedReports.remove(0)
  }

  @Test
  fun `mappers round-trip executive live columns survey and installation report with JSONB and R2 keys`() {
    val execJson =
      JSONObject()
        .put("id", "exec-live-1")
        .put("auth_user_id", "auth-live-1")
        .put("email", "exec@gharkulsolar.in")
        .put("name_mr", "सुरेश पाटील")
        .put("first_name_mr", "सुरेश")
        .put("mobile", "9823011223")
        .put("role", "SURVEY_EXECUTIVE")
        .put("role_mr", "फील्ड एक्झिक्युटिव्ह")
        .put("assigned_district", "अहिल्यानगर")
        .put("assigned_taluka", "संगमनेर")
        .put("assigned_village", "सर्व गाव")
        .put("is_active", true)
    val decodedExec = SupabaseMappers.executiveFromJson(execJson)
    assertEquals("exec-live-1", decodedExec.id)
    assertEquals("सुरेश पाटील", decodedExec.nameMr)
    assertEquals("सुरेश", decodedExec.firstNameMr)
    assertEquals("फील्ड एक्झिक्युटिव्ह", decodedExec.roleMr)
    assertEquals("अहिल्यानगर", decodedExec.assignedDistrict)
    assertEquals("संगमनेर", decodedExec.assignedTaluka)
    assertEquals("सर्व गाव", decodedExec.assignedVillage)

    val sampleSurvey = InMemorySurveyRepository().surveys.value.first().copy(
      govFormCompleted = true,
      govFormCompletedAt = "2026-11-19T10:15:00Z",
    )
    val surveyJson = SupabaseMappers.customerSurveyToJson(sampleSurvey)
    val docsJson = SupabaseMappers.surveyDocumentsToJsonArray(sampleSurvey.id, sampleSurvey.documents)
    surveyJson.put("survey_documents", docsJson)

    val decodedSurvey = SupabaseMappers.customerSurveyFromJson(surveyJson)
    assertEquals(sampleSurvey.id, decodedSurvey.id)
    assertEquals(sampleSurvey.beneficiaryName, decodedSurvey.beneficiaryName)
    assertTrue(decodedSurvey.govFormCompleted)
    assertEquals("2026-11-19T10:15:00Z", decodedSurvey.govFormCompletedAt)
    assertEquals(
      sampleSurvey.materialAndOfficeDetails.customerSignatureStrokes.size,
      decodedSurvey.materialAndOfficeDetails.customerSignatureStrokes.size,
    )

    val report =
      InstallationReport(
        id = "inst_roundtrip",
        linkedSurveyId = sampleSurvey.id,
        installerExecutiveId = "exec-uuid-2",
        villageName = "हिवरे बाजार",
        consumerNo = "123456789012",
        installationDate = "19 / 11 / 2026",
        consumerName = sampleSurvey.beneficiaryName,
        mobileNo = "9823011223",
        products = defaultInstallationProductRows(),
        installerName = "Vikram Kulkarni",
        installerSignatureStrokes = listOf(listOf(0.1f to 0.2f, 0.5f to 0.6f)),
        supervisorName = "R. Patil",
        managerName = "A. Deshmukh",
        customerSignatureStrokes = listOf(listOf(0.2f to 0.3f, 0.8f to 0.7f)),
        inverterSerial = SerialVerificationField("INV-100", "INV-100"),
        generationMeterSerial = SerialVerificationField("GM-200", "GM-200"),
        panelSerials = defaultPanelSerialEntries().mapIndexed { idx, p ->
          p.copy(serial = SerialVerificationField("PNL-$idx", "PNL-$idx"))
        },
        afterInstallationPhotos =
          listOf(
            InstallationPhotoItem(
              id = "photo_1",
              caption = "Roof View",
              photoUri = "https://pub.example.r2.dev/installation-photos/inst_roundtrip/p1.jpg",
              r2ObjectKey = "installation-photos/inst_roundtrip/p1.jpg",
              photoSizeKb = 420,
              geoInfo = "19.09, 74.74",
              isCameraGeoStamped = true,
            )
          ),
      )

    val reportJson = SupabaseMappers.installationReportToJson(report)
    assertTrue("accountant_name must be omitted from Supabase payload", !reportJson.has("accountant_name"))
    val photosArr = SupabaseMappers.installationPhotosToJsonArray(report.id, report.afterInstallationPhotos)
    reportJson.put("installation_photos", photosArr)

    val decodedReport = SupabaseMappers.installationReportFromJson(reportJson)
    assertEquals(report.id, decodedReport.id)
    assertEquals("R. Patil", decodedReport.supervisorName)
    assertEquals("A. Deshmukh", decodedReport.managerName)
    assertEquals(23, decodedReport.products.size)
    assertEquals("02", decodedReport.products.first().qty)
    assertEquals("INV-100", decodedReport.inverterSerial.trimmedPrimary)
    assertEquals(2, decodedReport.panelSerials.size)
    assertEquals("https://pub.example.r2.dev/installation-photos/inst_roundtrip/p1.jpg", decodedReport.afterInstallationPhotos.first().photoUri)
    assertEquals("installation-photos/inst_roundtrip/p1.jpg", decodedReport.afterInstallationPhotos.first().r2ObjectKey)
  }

  @Test
  fun `survey executive scopes queries strictly and deletes old R2 object before replacing photo`() = runBlocking {
    val repo = SupabaseSurveyRepository(app, testSettings, mockOkHttpClient)
    val (exec, _) =
      repo.signInAndVerifyExecutive(
        email = "ramesh.k@gharkulsolar.in",
        password = "secret",
        requestedRole = LoginRole.SURVEY_EXECUTIVE,
      )
    assertEquals("exec-uuid-1", exec.id)
    assertTrue(calls.any { it.method == "GET" && it.url.contains("/rest/v1/executives?auth_user_id=eq.auth-uuid-1") })
    assertTrue(calls.any { it.method == "PATCH" && it.url.contains("/rest/v1/executives?id=eq.exec-uuid-1") && it.bodyText.contains("last_login_at") })

    // Verify role-scoped fetch only asks for this executive's surveys
    repo.syncAll()
    assertTrue(
      calls.any {
        it.method == "GET" &&
          it.url.contains("/rest/v1/customer_surveys?executive_id=eq.exec-uuid-1") &&
          it.url.contains("survey_documents(*)")
      }
    )

    val photoDir = File(app.cacheDir, "survey_photos").apply { mkdirs() }
    val testJpg1 = File(photoDir, "aadhaar_v1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
    val contentUri1 = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", testJpg1).toString()

    val baseSurvey = InMemorySurveyRepository().surveys.value.first()
    val surveyToSave =
      baseSurvey.copy(
        id = "cust_r2_test",
        documents =
          baseSurvey.documents.toMutableMap().apply {
            put(
              DocumentType.AADHAAR,
              DocumentEntry(
                type = DocumentType.AADHAAR,
                printedName = baseSurvey.beneficiaryName,
                photoUri = contentUri1,
                photoSizeKb = 1,
                legibility = DocumentLegibility.CLEAR,
                matchResult = NameMatchResult.MATCH,
              ),
            )
          },
      )

    val saved1 = repo.saveSurveyWithUploads(surveyToSave)
    val firstKey = saved1.documents[DocumentType.AADHAAR]?.r2ObjectKey
    assertNotNull(firstKey)
    assertTrue(firstKey!!.startsWith("survey-docs/cust_r2_test/"))
    assertEquals(1, saved1.documents[DocumentType.AADHAAR]?.photoSizeKb)

    // Replace the Aadhaar photo with a second local file -> must DELETE old R2 key BEFORE presign + PUT
    calls.clear()
    val testJpg2 = File(photoDir, "aadhaar_v2.jpg").apply { writeBytes(ByteArray(2048) { 7 }) }
    val contentUri2 = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", testJpg2).toString()
    val surveyReplaced =
      saved1.copy(
        documents =
          saved1.documents.toMutableMap().apply {
            put(
              DocumentType.AADHAAR,
              saved1.documents[DocumentType.AADHAAR]!!.copy(
                photoUri = contentUri2,
                r2ObjectKey = firstKey,
              ),
            )
          },
      )

    val saved2 = repo.saveSurveyWithUploads(surveyReplaced, previousSurvey = saved1)
    val secondKey = saved2.documents[DocumentType.AADHAAR]?.r2ObjectKey
    assertNotNull(secondKey)
    assertTrue(secondKey != firstKey)
    assertEquals(2, saved2.documents[DocumentType.AADHAAR]?.photoSizeKb)

    val r2Calls = calls.filter { it.url.endsWith("/functions/v1/r2-presign") }
    assertTrue(r2Calls.size >= 2)
    val deleteIndex =
      r2Calls.indexOfFirst {
        val j = JSONObject(it.bodyText)
        j.optString("action") == "delete" &&
          j.optJSONArray("object_keys")?.optString(0) == firstKey
      }
    val presignIndex =
      r2Calls.indexOfFirst { JSONObject(it.bodyText).optString("action") == "presign" }
    assertTrue("Old R2 key must be deleted before presigning replacement", deleteIndex in 0 until presignIndex)

    // Verify delete triggers both PostgreSQL DELETE and Cloudflare R2 batch delete
    repo.deleteSurveyWithUploads(saved2)
    assertTrue(calls.any { it.method == "DELETE" && it.url.contains("/rest/v1/customer_surveys?id=eq.cust_r2_test") })
    val finalDeleteCall =
      calls.last {
        it.url.endsWith("/functions/v1/r2-presign") &&
          JSONObject(it.bodyText).optString("action") == "delete"
      }
    val deletedKeys = JSONObject(finalDeleteCall.bodyText).getJSONArray("object_keys")
    assertEquals(secondKey, deletedKeys.getString(0))
  }

  @Test
  fun `solar installation role scopes pending surveys and own reports and replaces R2 photos cleanly`() = runBlocking {
    mockExecutiveId = "exec-inst-2"
    mockExecutiveRole = "SOLAR_INSTALLATION"
    mockExecutiveRoleMr = "सोलर इन्स्टॉलेशन"

    val repo = SupabaseSurveyRepository(app, testSettings, mockOkHttpClient)
    val (exec, _) =
      repo.signInAndVerifyExecutive(
        email = "installer@gharkulsolar.in",
        password = "secret",
        requestedRole = LoginRole.SOLAR_INSTALLATION,
      )
    assertEquals("exec-inst-2", exec.id)

    repo.syncAll()
    assertTrue(
      calls.any {
        it.method == "GET" &&
          it.url.contains("/rest/v1/customer_surveys?final_category=eq.CATEGORY_1&gov_form_completed=eq.true")
      }
    )
    assertTrue(
      calls.any {
        it.method == "GET" &&
          it.url.contains("/rest/v1/installation_reports?installer_executive_id=eq.exec-inst-2")
      }
    )

    val photoDir = File(app.cacheDir, "survey_photos").apply { mkdirs() }
    val instJpg1 = File(photoDir, "install_site_1.jpg").apply { writeBytes(byteArrayOf(9, 8, 7, 6)) }
    val instUri1 = Uri.fromFile(instJpg1).toString()

    val savedReport1 =
      repo.saveInstallationReportWithUploads(
        InstallationReport(
          id = "inst_r2_test",
          linkedSurveyId = "cust_cat1_done",
          villageName = "हिवरे बाजार",
          consumerNo = "182039481201",
          consumerName = "सुनीता शिंदे",
          mobileNo = "9823011223",
          installerName = "Vikram Kulkarni",
          afterInstallationPhotos =
            listOf(
              InstallationPhotoItem(
                id = "site_photo_1",
                caption = "After Installation Roof",
                photoUri = instUri1,
                photoSizeKb = 1,
                geoInfo = "19.04, 74.73",
                isCameraGeoStamped = true,
              )
            ),
        )
      )
    assertEquals("exec-inst-2", savedReport1.installerExecutiveId)
    val firstPhotoKey = savedReport1.afterInstallationPhotos.first().r2ObjectKey
    assertNotNull(firstPhotoKey)
    assertTrue(firstPhotoKey!!.startsWith("installation-photos/inst_r2_test/"))

    // Replace photo on edit -> must delete old key before uploading new one
    calls.clear()
    val instJpg2 = File(photoDir, "install_site_2.jpg").apply { writeBytes(ByteArray(3072) { 3 }) }
    val instUri2 = Uri.fromFile(instJpg2).toString()
    val savedReport2 =
      repo.saveInstallationReportWithUploads(
        report =
          savedReport1.copy(
            afterInstallationPhotos =
              listOf(
                savedReport1.afterInstallationPhotos.first().copy(
                  photoUri = instUri2,
                  r2ObjectKey = firstPhotoKey,
                )
              )
          ),
        previousReport = savedReport1,
      )
    assertEquals(3, savedReport2.afterInstallationPhotos.first().photoSizeKb)
    val r2Calls = calls.filter { it.url.endsWith("/functions/v1/r2-presign") }
    val delIdx =
      r2Calls.indexOfFirst {
        val j = JSONObject(it.bodyText)
        j.optString("action") == "delete" &&
          j.optJSONArray("object_keys")?.optString(0) == firstPhotoKey
      }
    val preIdx = r2Calls.indexOfFirst { JSONObject(it.bodyText).optString("action") == "presign" }
    assertTrue(delIdx in 0 until preIdx)
  }

  @Test
  fun `role gate rejects ADMIN wrong field role and inactive accounts with immediate signOut`() = runBlocking {
    val repo = SupabaseSurveyRepository(app, testSettings, mockOkHttpClient)

    // 1. ADMIN account must be rejected on mobile and signed out immediately
    mockExecutiveRole = "ADMIN"
    var adminErr: Throwable? = null
    try {
      repo.signInAndVerifyExecutive("admin@gharkulsolar.in", "secret", LoginRole.SURVEY_EXECUTIVE)
    } catch (t: Throwable) {
      adminErr = t
    }
    assertNotNull(adminErr)
    assertEquals(SupabaseSurveyRepository.ADMIN_MOBILE_BLOCKED_MESSAGE_MR, adminErr!!.message)
    assertTrue(calls.any { it.url.contains("/auth/v1/logout") })

    // 2. Wrong role (SURVEY_EXECUTIVE trying to sign into SOLAR_INSTALLATION)
    calls.clear()
    mockExecutiveRole = "SURVEY_EXECUTIVE"
    var wrongRoleErr: Throwable? = null
    try {
      repo.signInAndVerifyExecutive("ramesh.k@gharkulsolar.in", "secret", LoginRole.SOLAR_INSTALLATION)
    } catch (t: Throwable) {
      wrongRoleErr = t
    }
    assertNotNull(wrongRoleErr)
    assertTrue(calls.any { it.url.contains("/auth/v1/logout") })

    // 3. Inactive executive must throw AccessRevokedException with exact Marathi message
    calls.clear()
    mockExecutiveActive = false
    var inactiveErr: Throwable? = null
    try {
      repo.signInAndVerifyExecutive("ramesh.k@gharkulsolar.in", "secret", LoginRole.SURVEY_EXECUTIVE)
    } catch (t: Throwable) {
      inactiveErr = t
    }
    assertTrue(inactiveErr is AccessRevokedException)
    assertEquals(SupabaseSurveyRepository.ACCESS_REVOKED_MESSAGE_MR, inactiveErr!!.message)
    assertTrue(calls.any { it.url.contains("/auth/v1/logout") })
  }

  @Test
  fun `cloud ViewModel signs in and immediately revokes session onAppForegroundResume when admin disables account`() {
    val seed = InMemorySurveyRepository().surveys.value.first().copy(
      id = "cust_gov_done",
      govFormCompleted = true,
      govFormCompletedAt = "2026-11-19T11:00:00Z",
    )
    storedSurveys.put(
      SupabaseMappers.customerSurveyToJson(seed).put(
        "survey_documents",
        SupabaseMappers.surveyDocumentsToJsonArray(seed.id, seed.documents),
      )
    )

    val vm = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    Thread.sleep(150)
    shadowOf(Looper.getMainLooper()).idle()

    assertTrue(vm.uiState.value.isCloudConfigured)
    assertEquals(AppScreen.LOGIN, vm.uiState.value.currentScreen)
    assertEquals(InstallationQueueFilter.PENDING, vm.uiState.value.installationFilter)

    vm.updateLoginEmail("ramesh.k@gharkulsolar.in")
    vm.updateLoginPassword("123456")
    vm.submitLogin()

    val deadline = System.currentTimeMillis() + 3000
    while (vm.uiState.value.currentScreen == AppScreen.LOGIN && System.currentTimeMillis() < deadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }

    assertEquals(AppScreen.VILLAGE_SELECT, vm.uiState.value.currentScreen)
    assertNull(vm.uiState.value.loginError)
    assertEquals(1, vm.uiState.value.approvedCategory1Surveys.size)
    assertTrue(vm.uiState.value.approvedCategory1Surveys.first().govFormCompleted)

    // Simulate Office Admin turning off is_active -> foreground resume must sign out & clear state
    calls.clear()
    mockExecutiveActive = false
    vm.onAppForegroundResume()

    val revokeDeadline = System.currentTimeMillis() + 3000
    while (vm.uiState.value.currentScreen != AppScreen.LOGIN && System.currentTimeMillis() < revokeDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }

    assertEquals(AppScreen.LOGIN, vm.uiState.value.currentScreen)
    assertEquals(SupabaseSurveyRepository.ACCESS_REVOKED_MESSAGE_MR, vm.uiState.value.loginError)
    assertTrue(vm.uiState.value.allSurveys.isEmpty())
    assertTrue(vm.uiState.value.installationReports.isEmpty())
    assertTrue(calls.any { it.url.contains("/auth/v1/logout") })
  }

  @Test
  fun `LocationRepository dynamically merges new district taluka village and resolves assigned area`() {
    val locationRepo = LocationRepository(app)
    locationRepo.mergeCustomVillages(
      listOf(
        CustomVillageRecord(
          id = "dist_nashik_tal_sinnar_custom_1700000000000",
          districtId = "dist_nashik",
          districtNameMr = "नाशिक",
          talukaId = "tal_sinnar",
          talukaNameMr = "सिन्नर",
          villageNameMr = "मुसळगाव",
          villageNameEn = "Musalgaon",
        ),
        CustomVillageRecord(
          id = "dist_nashik_tal_sinnar_custom_1700000000001",
          districtId = "dist_nashik",
          districtNameMr = "नाशिक",
          talukaId = "tal_sinnar",
          talukaNameMr = "सिन्नर",
          villageNameMr = "नांदूर शिंगोटे",
          villageNameEn = "Nandur Shingote",
        ),
      )
    )
    val mergedDistricts = locationRepo.getDistricts()

    val specificExec =
      ExecutiveUser(
        id = "e1",
        nameMr = "रमेश कुलकर्णी",
        firstNameMr = "रमेश",
        email = "r@g.in",
        mobile = "9823011223",
        role = "SURVEY_EXECUTIVE",
        roleMr = "फील्ड एक्झिक्युटिव्ह",
        assignedDistrict = "नाशिक",
        assignedTaluka = "सिन्नर",
        assignedVillage = "मुसळगाव",
      )
    val resolvedSpecific =
      locationRepo.resolveSelectionForExecutive(
        districts = mergedDistricts,
        executive = specificExec,
        currentDistrictId = null,
        currentTalukaId = null,
        currentVillageId = null,
        preferAssignedArea = true,
      )
    assertEquals("नाशिक", resolvedSpecific.district?.nameMr)
    assertEquals("सिन्नर", resolvedSpecific.taluka?.nameMr)
    assertEquals("मुसळगाव", resolvedSpecific.village?.nameMr)

    val allVillagesExec = specificExec.copy(assignedVillage = "सर्व गाव")
    val resolvedAll =
      locationRepo.resolveSelectionForExecutive(
        districts = mergedDistricts,
        executive = allVillagesExec,
        currentDistrictId = null,
        currentTalukaId = null,
        currentVillageId = null,
        preferAssignedArea = true,
      )
    assertEquals("नाशिक", resolvedAll.district?.nameMr)
    assertEquals("सिन्नर", resolvedAll.taluka?.nameMr)
    assertEquals(2, resolvedAll.taluka?.villages?.size)
  }

  @Test
  fun `user stays logged in across app restarts both online and offline until explicit logout`() {
    // 1. First launch -> log in once and enter Dashboard for selected village
    val vm1 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(AppScreen.LOGIN, vm1.uiState.value.currentScreen)

    vm1.updateLoginEmail("ramesh.k@gharkulsolar.in")
    vm1.updateLoginPassword("123456")
    vm1.submitLogin()
    val loginDeadline = System.currentTimeMillis() + 3000
    while (vm1.uiState.value.currentScreen == AppScreen.LOGIN && System.currentTimeMillis() < loginDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }
    assertEquals(AppScreen.VILLAGE_SELECT, vm1.uiState.value.currentScreen)
    vm1.proceedToDashboard()
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(AppScreen.MAIN_APP, vm1.uiState.value.currentScreen)

    // 2. Reopen app online -> restores directly into MAIN_APP without showing LOGIN
    val vm2 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    assertTrue(vm2.uiState.value.isRestoringSession)
    val restoreDeadline = System.currentTimeMillis() + 3000
    while (vm2.uiState.value.isRestoringSession && System.currentTimeMillis() < restoreDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }
    assertEquals(AppScreen.MAIN_APP, vm2.uiState.value.currentScreen)
    assertNull(vm2.uiState.value.syncErrorMessage)

    // 3. Reopen app while offline -> STILL stays logged in on MAIN_APP with offline banner
    simulateNetworkDown = true
    val vm3 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    val offlineDeadline = System.currentTimeMillis() + 3000
    while (vm3.uiState.value.isRestoringSession && System.currentTimeMillis() < offlineDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }
    assertEquals(AppScreen.MAIN_APP, vm3.uiState.value.currentScreen)
    assertNotNull(vm3.uiState.value.syncErrorMessage)

    // 4. Explicit logout clears session -> next launch asks for LOGIN
    simulateNetworkDown = false
    vm3.logout()
    Thread.sleep(150)
    shadowOf(Looper.getMainLooper()).idle()
    val vm4 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(AppScreen.LOGIN, vm4.uiState.value.currentScreen)
  }

  @Test
  fun `offline survey form and photo save durably on device survive restart and sync on refresh button`() {
    val vm1 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()

    vm1.updateLoginEmail("ramesh.k@gharkulsolar.in")
    vm1.updateLoginPassword("123456")
    vm1.submitLogin()
    val loginDeadline = System.currentTimeMillis() + 3000
    while (vm1.uiState.value.currentScreen == AppScreen.LOGIN && System.currentTimeMillis() < loginDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }
    vm1.proceedToDashboard()
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()

    // Go OFFLINE and submit a new survey with a local photo
    simulateNetworkDown = true
    calls.clear()
    val photoDir = File(app.cacheDir, "survey_photos").apply { mkdirs() }
    val localPhoto = File(photoDir, "offline_aadhaar.jpg").apply { writeBytes(ByteArray(2048) { 5 }) }
    val localPhotoUri = Uri.fromFile(localPhoto).toString()

    vm1.openNewSurveyWizard()
    vm1.updateWizardBasic(
      beneficiaryName = "विठ्ठल जाधव",
      mobile = "9876543210",
    )
    vm1.updateDocumentEntry(
      type = DocumentType.AADHAAR,
      printedName = "विठ्ठल जाधव",
      photoUri = localPhotoUri,
      photoSizeKb = 2,
      legibility = DocumentLegibility.CLEAR,
    )
    vm1.updateMaterialAndOffice { it.copy(surveyorName = "रमेश कुलकर्णी") }
    vm1.saveWizardSurvey()

    val saveDeadline = System.currentTimeMillis() + 3000
    while ((vm1.uiState.value.isSavingSurvey || vm1.uiState.value.wizard.isOpen) && System.currentTimeMillis() < saveDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }

    assertEquals(1, vm1.uiState.value.pendingSyncCount)
    val offlineSavedSurvey = vm1.uiState.value.allSurveys.firstOrNull { it.beneficiaryName == "विठ्ठल जाधव" }
    assertNotNull(offlineSavedSurvey)
    val durablePhotoUri = offlineSavedSurvey!!.documents[DocumentType.AADHAAR]?.photoUri
    assertNotNull(durablePhotoUri)
    assertTrue("Photo must be copied into durable filesDir/offline_photos", durablePhotoUri!!.contains("offline_photos"))

    // Delete original cacheDir file to prove durable offline_photos copy survives cache eviction & app restart
    localPhoto.delete()
    val vm2 = SurveyAppViewModel(app, testSettings, mockOkHttpClient)
    val reopenDeadline = System.currentTimeMillis() + 3000
    while (vm2.uiState.value.isRestoringSession && System.currentTimeMillis() < reopenDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }
    assertEquals(AppScreen.MAIN_APP, vm2.uiState.value.currentScreen)
    assertEquals(1, vm2.uiState.value.pendingSyncCount)
    assertTrue(vm2.uiState.value.allSurveys.any { it.beneficiaryName == "विठ्ठल जाधव" })

    // Come back ONLINE and press the circular arrow button (refreshCloudData)
    simulateNetworkDown = false
    calls.clear()
    vm2.refreshCloudData()
    val syncDeadline = System.currentTimeMillis() + 3000
    while (vm2.uiState.value.isSyncing && System.currentTimeMillis() < syncDeadline) {
      Thread.sleep(25)
      shadowOf(Looper.getMainLooper()).idle()
    }

    assertEquals(0, vm2.uiState.value.pendingSyncCount)
    assertTrue(calls.any { it.method == "PUT" && it.url.startsWith("https://r2-upload.example.invalid/survey-docs/") && it.bodyLength == 2048 })
    assertTrue(calls.any { it.method == "POST" && it.url.endsWith("/rest/v1/customer_surveys") && it.bodyText.contains("विठ्ठल जाधव") })

    // Clean up session for subsequent tests
    vm2.logout()
    Thread.sleep(100)
    shadowOf(Looper.getMainLooper()).idle()
  }
}
