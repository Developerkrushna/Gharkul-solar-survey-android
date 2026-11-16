package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocationRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("घरकुल सर्व्हे", appName)
  }

  @Test
  fun `load Ahilyanagar and Sambhajinagar districts with all talukas`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocationRepository(context)
    val districts = repo.getDistricts()
    assertEquals(2, districts.size)

    val ahilyanagar = districts.first { it.id == "ahilyanagar" }
    assertEquals("अहिल्यानगर", ahilyanagar.nameMr)
    assertEquals(14, ahilyanagar.talukas.size)
    assertTrue(ahilyanagar.talukas.all { it.villages.isNotEmpty() })

    val sambhajinagar = districts.first { it.id == "sambhajinagar" }
    assertEquals("छत्रपती संभाजीनगर", sambhajinagar.nameMr)
    assertEquals(9, sambhajinagar.talukas.size)
    assertTrue(sambhajinagar.talukas.all { it.villages.isNotEmpty() })
  }
}
