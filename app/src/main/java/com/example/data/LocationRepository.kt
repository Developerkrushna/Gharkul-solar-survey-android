package com.example.data

import android.content.Context
import com.example.model.District
import com.example.model.Taluka
import com.example.model.Village
import org.json.JSONObject

class LocationRepository(private val context: Context) {

  private var cachedDistricts: List<District>? = null

  fun getDistricts(): List<District> {
    cachedDistricts?.let { return it }
    val jsonText = context.assets.open("locations_mr.json").bufferedReader().use { it.readText() }
    val root = JSONObject(jsonText)
    val districtArray = root.getJSONArray("districts")
    val districts = mutableListOf<District>()
    for (i in 0 until districtArray.length()) {
      val dObj = districtArray.getJSONObject(i)
      val talukaArray = dObj.getJSONArray("talukas")
      val talukas = mutableListOf<Taluka>()
      for (j in 0 until talukaArray.length()) {
        val tObj = talukaArray.getJSONObject(j)
        val villageArray = tObj.getJSONArray("villages")
        val villages = mutableListOf<Village>()
        for (k in 0 until villageArray.length()) {
          val vObj = villageArray.getJSONObject(k)
          villages.add(
            Village(
              id = vObj.getString("id"),
              nameMr = vObj.getString("nameMr"),
              nameEn = vObj.getString("nameEn"),
            )
          )
        }
        talukas.add(
          Taluka(
            id = tObj.getString("id"),
            nameMr = tObj.getString("nameMr"),
            nameEn = tObj.getString("nameEn"),
            villages = villages,
          )
        )
      }
      districts.add(
        District(
          id = dObj.getString("id"),
          nameMr = dObj.getString("nameMr"),
          nameEn = dObj.getString("nameEn"),
          talukas = talukas,
        )
      )
    }
    cachedDistricts = districts
    return districts
  }

  fun addCustomVillage(districtId: String, talukaId: String, villageNameMr: String): Village {
    val cleanName = villageNameMr.trim()
    val newVillage =
      Village(
        id = "custom_${System.currentTimeMillis()}",
        nameMr = cleanName,
        nameEn = cleanName,
      )
    val current = getDistricts()
    cachedDistricts =
      current.map { d ->
        if (d.id != districtId) d
        else {
          d.copy(
            talukas =
              d.talukas.map { t ->
                if (t.id != talukaId) t else t.copy(villages = listOf(newVillage) + t.villages)
              }
          )
        }
      }
    return newVillage
  }
}
