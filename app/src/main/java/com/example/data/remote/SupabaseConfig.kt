package com.example.data.remote

import com.example.BuildConfig

data class SupabaseSettings(
  val url: String,
  val anonKey: String,
) {
  val normalizedUrl: String
    get() = url.trim().trimEnd('/')

  val isConfigured: Boolean
    get() =
      normalizedUrl.startsWith("https://") &&
        !normalizedUrl.contains("YOUR_PROJECT_REF") &&
        anonKey.isNotBlank() &&
        !anonKey.contains("YOUR_SUPABASE_ANON_KEY")
}

object SupabaseConfig {
  fun fromBuildConfig(): SupabaseSettings =
    SupabaseSettings(
      url = BuildConfig.SUPABASE_URL,
      anonKey = BuildConfig.SUPABASE_ANON_KEY,
    )
}
