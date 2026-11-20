package com.example.data

import android.content.Context
import com.example.data.remote.CustomVillageRecord
import com.example.model.District
import com.example.model.ExecutiveUser
import com.example.model.Taluka
import com.example.model.Village
import org.json.JSONObject

data class ResolvedLocationSelection(
  val district: District?,
  val taluka: Taluka?,
  val village: Village?,
)

class LocationRepository(private val context: Context) {

  companion object {
    const val ALL_VILLAGES_LABEL_MR = "सर्व गाव"
  }

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
        id = "${districtId}_${talukaId}_custom_${System.currentTimeMillis()}",
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

  /**
   * Merges `public.custom_villages` synced from Supabase into the location tree.
   * Dynamically creates missing Districts and Talukas added from the Office Dashboard.
   */
  fun mergeCustomVillages(records: List<CustomVillageRecord>) {
    if (records.isEmpty()) return
    val mutableDistricts = getDistricts().toMutableList()

    for (rec in records) {
      val cleanDistrictId = rec.districtId.trim()
      val cleanTalukaId = rec.talukaId.trim()
      val cleanVillageMr = rec.villageNameMr.trim()
      if (cleanDistrictId.isBlank() || cleanTalukaId.isBlank() || cleanVillageMr.isBlank()) continue

      val newVillage =
        Village(
          id = rec.id,
          nameMr = cleanVillageMr,
          nameEn = rec.villageNameEn.ifBlank { cleanVillageMr },
        )

      val dIdx =
        mutableDistricts.indexOfFirst {
          it.id.equals(cleanDistrictId, ignoreCase = true) ||
            (rec.districtNameMr.isNotBlank() && it.nameMr.trim() == rec.districtNameMr.trim())
        }

      if (dIdx < 0) {
        val districtLabel = rec.districtNameMr.ifBlank { cleanDistrictId }
        val talukaLabel = rec.talukaNameMr.ifBlank { cleanTalukaId }
        mutableDistricts.add(
          District(
            id = cleanDistrictId,
            nameMr = districtLabel,
            nameEn = districtLabel,
            talukas =
              listOf(
                Taluka(
                  id = cleanTalukaId,
                  nameMr = talukaLabel,
                  nameEn = talukaLabel,
                  villages = listOf(newVillage),
                )
              ),
          )
        )
      } else {
        val district = mutableDistricts[dIdx]
        val mutableTalukas = district.talukas.toMutableList()
        val tIdx =
          mutableTalukas.indexOfFirst {
            it.id.equals(cleanTalukaId, ignoreCase = true) ||
              (rec.talukaNameMr.isNotBlank() && it.nameMr.trim() == rec.talukaNameMr.trim())
          }

        if (tIdx < 0) {
          val talukaLabel = rec.talukaNameMr.ifBlank { cleanTalukaId }
          mutableTalukas.add(
            Taluka(
              id = cleanTalukaId,
              nameMr = talukaLabel,
              nameEn = talukaLabel,
              villages = listOf(newVillage),
            )
          )
        } else {
          val taluka = mutableTalukas[tIdx]
          val exists =
            taluka.villages.any {
              it.id == newVillage.id || it.nameMr.trim() == cleanVillageMr
            }
          if (!exists) {
            mutableTalukas[tIdx] = taluka.copy(villages = listOf(newVillage) + taluka.villages)
          }
        }
        mutableDistricts[dIdx] = district.copy(talukas = mutableTalukas)
      }
    }

    cachedDistricts = mutableDistricts
  }

  /**
   * Resolves default district/taluka/village for an executive based on `assigned_district`,
   * `assigned_taluka`, and `assigned_village` (supporting `"सर्व गाव"` or blank -> any village in
   * assigned taluka/district).
   */
  fun resolveSelectionForExecutive(
    districts: List<District>,
    executive: ExecutiveUser,
    currentDistrictId: String?,
    currentTalukaId: String?,
    currentVillageId: String?,
    preferAssignedArea: Boolean,
  ): ResolvedLocationSelection {
    if (!preferAssignedArea) {
      val preservedDistrict = districts.find { it.id == currentDistrictId }
      val preservedTaluka = preservedDistrict?.talukas?.find { it.id == currentTalukaId }
      val preservedVillage = preservedTaluka?.villages?.find { it.id == currentVillageId }
      if (preservedDistrict != null && preservedTaluka != null && preservedVillage != null) {
        return ResolvedLocationSelection(preservedDistrict, preservedTaluka, preservedVillage)
      }
    }

    val assignedDistrictMr = executive.assignedDistrict.trim()
    val assignedTalukaMr = executive.assignedTaluka.trim()
    val assignedVillageMr = executive.assignedVillage.trim()
    val isAllVillages = assignedVillageMr.isBlank() || assignedVillageMr == ALL_VILLAGES_LABEL_MR

    val matchedDistrict =
      if (assignedDistrictMr.isNotBlank()) {
        districts.find {
          it.nameMr.trim() == assignedDistrictMr ||
            it.nameEn.equals(assignedDistrictMr, ignoreCase = true) ||
            it.id.equals(assignedDistrictMr, ignoreCase = true)
        }
      } else {
        null
      }
        ?: districts.find { it.id == currentDistrictId }
        ?: districts.firstOrNull()

    val matchedTaluka =
      if (assignedTalukaMr.isNotBlank() && matchedDistrict != null) {
        matchedDistrict.talukas.find {
          it.nameMr.trim() == assignedTalukaMr ||
            it.nameEn.equals(assignedTalukaMr, ignoreCase = true) ||
            it.id.equals(assignedTalukaMr, ignoreCase = true)
        }
      } else {
        null
      }
        ?: matchedDistrict?.talukas?.find { it.id == currentTalukaId }
        ?: matchedDistrict?.talukas?.find { it.id == "nagar" }
        ?: matchedDistrict?.talukas?.firstOrNull()

    val matchedVillage =
      if (!isAllVillages && matchedTaluka != null) {
        matchedTaluka.villages.find {
          it.nameMr.trim() == assignedVillageMr ||
            it.nameEn.equals(assignedVillageMr, ignoreCase = true) ||
            it.id.equals(assignedVillageMr, ignoreCase = true)
        }
      } else {
        null
      }
        ?: matchedTaluka?.villages?.find { it.id == currentVillageId }
        ?: matchedTaluka?.villages?.firstOrNull()

    return ResolvedLocationSelection(
      district = matchedDistrict,
      taluka = matchedTaluka,
      village = matchedVillage,
    )
  }
}
