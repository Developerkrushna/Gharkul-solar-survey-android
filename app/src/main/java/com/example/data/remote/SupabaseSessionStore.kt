package com.example.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ExecutiveUser
import com.example.model.LoginRole
import kotlinx.coroutines.flow.first

private val Context.supabaseDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "gharkul_supabase_session")

data class PersistedSupabaseSession(
  val accessToken: String,
  val refreshToken: String,
  val loginRole: LoginRole,
  val executive: ExecutiveUser,
  val isOfflineDemo: Boolean = false,
  val lastDistrictId: String = "",
  val lastTalukaId: String = "",
  val lastVillageId: String = "",
)

class SupabaseSessionStore(private val context: Context) {

  private companion object {
    const val FAST_PREFS_NAME = "gharkul_session_fast_flag"
    const val KEY_HAS_SESSION = "has_saved_session"
  }

  private val fastPrefs by lazy {
    context.getSharedPreferences(FAST_PREFS_NAME, Context.MODE_PRIVATE)
  }

  private object Keys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val LOGIN_ROLE = stringPreferencesKey("login_role")
    val IS_OFFLINE_DEMO = booleanPreferencesKey("is_offline_demo")
    val LAST_DISTRICT_ID = stringPreferencesKey("last_district_id")
    val LAST_TALUKA_ID = stringPreferencesKey("last_taluka_id")
    val LAST_VILLAGE_ID = stringPreferencesKey("last_village_id")
    val EXEC_ID = stringPreferencesKey("exec_id")
    val EXEC_AUTH_USER_ID = stringPreferencesKey("exec_auth_user_id")
    val EXEC_EMAIL = stringPreferencesKey("exec_email")
    val EXEC_FULL_NAME = stringPreferencesKey("exec_full_name")
    val EXEC_FIRST_NAME = stringPreferencesKey("exec_first_name")
    val EXEC_MOBILE = stringPreferencesKey("exec_mobile")
    val EXEC_ROLE = stringPreferencesKey("exec_role")
    val EXEC_ROLE_MR = stringPreferencesKey("exec_role_mr")
    val ASSIGNED_DISTRICT = stringPreferencesKey("assigned_district")
    val ASSIGNED_TALUKA = stringPreferencesKey("assigned_taluka")
    val ASSIGNED_VILLAGE = stringPreferencesKey("assigned_village")
    val EXEC_IS_ACTIVE = booleanPreferencesKey("exec_is_active")
  }

  /**
   * Synchronous fast check used during ViewModel init so we never flash LoginScreen when a
   * returning user already has a saved session, and never delay LoginScreen on first install.
   */
  fun hasSavedSessionFast(): Boolean = fastPrefs.getBoolean(KEY_HAS_SESSION, false)

  suspend fun saveSession(session: PersistedSupabaseSession) {
    context.supabaseDataStore.edit { prefs ->
      prefs[Keys.ACCESS_TOKEN] = session.accessToken
      prefs[Keys.REFRESH_TOKEN] = session.refreshToken
      prefs[Keys.LOGIN_ROLE] = session.loginRole.name
      prefs[Keys.IS_OFFLINE_DEMO] = session.isOfflineDemo
      if (session.lastDistrictId.isNotBlank()) prefs[Keys.LAST_DISTRICT_ID] = session.lastDistrictId
      if (session.lastTalukaId.isNotBlank()) prefs[Keys.LAST_TALUKA_ID] = session.lastTalukaId
      if (session.lastVillageId.isNotBlank()) prefs[Keys.LAST_VILLAGE_ID] = session.lastVillageId
      prefs[Keys.EXEC_ID] = session.executive.id
      prefs[Keys.EXEC_AUTH_USER_ID] = session.executive.authUserId
      prefs[Keys.EXEC_EMAIL] = session.executive.email
      prefs[Keys.EXEC_FULL_NAME] = session.executive.nameMr
      prefs[Keys.EXEC_FIRST_NAME] = session.executive.firstNameMr
      prefs[Keys.EXEC_MOBILE] = session.executive.mobile
      prefs[Keys.EXEC_ROLE] = session.executive.role
      prefs[Keys.EXEC_ROLE_MR] = session.executive.roleMr
      prefs[Keys.ASSIGNED_DISTRICT] = session.executive.assignedDistrict
      prefs[Keys.ASSIGNED_TALUKA] = session.executive.assignedTaluka
      prefs[Keys.ASSIGNED_VILLAGE] = session.executive.assignedVillage
      prefs[Keys.EXEC_IS_ACTIVE] = session.executive.isActive
    }
    fastPrefs.edit().putBoolean(KEY_HAS_SESSION, true).commit()
  }

  suspend fun saveLastSelectedLocation(
    districtId: String,
    talukaId: String,
    villageId: String,
  ) {
    context.supabaseDataStore.edit { prefs ->
      prefs[Keys.LAST_DISTRICT_ID] = districtId
      prefs[Keys.LAST_TALUKA_ID] = talukaId
      prefs[Keys.LAST_VILLAGE_ID] = villageId
    }
  }

  suspend fun loadSession(): PersistedSupabaseSession? {
    val prefs = context.supabaseDataStore.data.first()
    val isOfflineDemo = prefs[Keys.IS_OFFLINE_DEMO] ?: false
    val token = prefs[Keys.ACCESS_TOKEN].orEmpty()
    val email = prefs[Keys.EXEC_EMAIL].orEmpty()
    if ((!isOfflineDemo && token.isBlank()) || email.isBlank()) return null

    val role =
      runCatching { LoginRole.valueOf(prefs[Keys.LOGIN_ROLE].orEmpty()) }
        .getOrDefault(LoginRole.SURVEY_EXECUTIVE)

    return PersistedSupabaseSession(
      accessToken = token,
      refreshToken = prefs[Keys.REFRESH_TOKEN].orEmpty(),
      loginRole = role,
      isOfflineDemo = isOfflineDemo,
      lastDistrictId = prefs[Keys.LAST_DISTRICT_ID].orEmpty(),
      lastTalukaId = prefs[Keys.LAST_TALUKA_ID].orEmpty(),
      lastVillageId = prefs[Keys.LAST_VILLAGE_ID].orEmpty(),
      executive =
        ExecutiveUser(
          id = prefs[Keys.EXEC_ID].orEmpty(),
          authUserId = prefs[Keys.EXEC_AUTH_USER_ID].orEmpty(),
          email = email,
          nameMr = prefs[Keys.EXEC_FULL_NAME].orEmpty(),
          firstNameMr = prefs[Keys.EXEC_FIRST_NAME].orEmpty(),
          mobile = prefs[Keys.EXEC_MOBILE].orEmpty(),
          role = prefs[Keys.EXEC_ROLE] ?: role.name,
          roleMr = prefs[Keys.EXEC_ROLE_MR].orEmpty(),
          assignedDistrict = prefs[Keys.ASSIGNED_DISTRICT].orEmpty(),
          assignedTaluka = prefs[Keys.ASSIGNED_TALUKA].orEmpty(),
          assignedVillage = prefs[Keys.ASSIGNED_VILLAGE].orEmpty(),
          isActive = prefs[Keys.EXEC_IS_ACTIVE] ?: true,
        ),
    )
  }

  suspend fun clearSession() {
    fastPrefs.edit().putBoolean(KEY_HAS_SESSION, false).commit()
    context.supabaseDataStore.edit { it.clear() }
  }
}
