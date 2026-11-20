# Application Form & Data Specification (`FORM_SPEC.md`)

> **Backend Architecture (Supabase PostgreSQL + Cloudflare R2):**
> - **Text & Structured Records:** Persisted in **Supabase PostgreSQL** (6 relational tables with `JSONB` columns for arrays, checklists, pending actions, and vector signatures — see `/supabase/schema.sql`) via Supabase Auth (`/auth/v1/*`) and PostgREST (`/rest/v1/*`) using OkHttp (`SupabaseApiClient`, `SupabaseSurveyRepository`, `SupabaseMappers`, `SupabaseSessionStore`).
> - **Document & NoteCam Images:** Stored in **Cloudflare R2** via the Supabase Edge Function `/functions/v1/r2-presign` (`/supabase/functions/r2-presign/index.ts`), which verifies the user's Supabase JWT + active `executives` row and returns a 5-minute presigned `PUT` URL + `public_url` + `object_key`. Zero Cloudflare R2 credentials exist in the Android APK. Resulting `public_url` and `r2_object_key` values are stored in PostgreSQL (`survey_documents`, `customer_surveys`, `installation_photos`).
> - **Configuration & Offline Demo Fallback:** `SUPABASE_URL` and `SUPABASE_ANON_KEY` are read from `BuildConfig` (configured via `.env` / AI Studio Secrets panel; placeholders declared in `.env.example`). When both keys are configured, full cloud Auth/sync/R2 uploads run; when unconfigured (placeholders), the app automatically falls back to `InMemorySurveyRepository` for offline/unit-test use. Local captured photos and generated PDFs are cached under `context.cacheDir`.

---

## 1. Screen Flow & "Next" Button Checks

The app supports two login roles from `LoginScreen` (`AppScreen.LOGIN`):
- **Role 1 (`LoginRole.SURVEY_EXECUTIVE`)** — Marathi Gharakul Solar Survey workflow
- **Role 2 (`LoginRole.SOLAR_INSTALLATION`)** — English Reflection Media 1 KW ONGRID Installation Certificate workflow

### 1.1 Screen Order

#### A. Survey Executive Flow (`LoginRole.SURVEY_EXECUTIVE`)
1. `LoginScreen` (`AppScreen.LOGIN`)
   - Role selector (`SURVEY_EXECUTIVE` / `SOLAR_INSTALLATION`), email, password, plus connection status caption (`Supabase Auth + PostgreSQL + Cloudflare R2 Connected` vs. `Offline Demo Mode`).
   - **Auto-restore on app launch (Cloud Mode):** Reads `PersistedSupabaseSession` from Jetpack DataStore (`gharkul_supabase_session`), refreshes token via `POST /auth/v1/token?grant_type=refresh_token`, re-verifies `executives.is_active`, syncs all tables, and routes directly to `VILLAGE_SELECT` or `INSTALLATION_DASHBOARD`.
   - **Action button (`"लॉगिन करा"` -> `submitLogin()`):**
     - Checks `loginEmail` is non-blank AND contains `"@"`; otherwise shows `"कृपया वैध ईमेल पत्ता प्रविष्ट करा."`.
     - Checks `loginPassword.length >= 4`; otherwise shows `"कृपया किमान ४ अंकी पासवर्ड टाका."`.
     - In Cloud Mode: authenticates via Supabase Auth (`POST /auth/v1/token?grant_type=password`), fetches `executives` row, requires `is_active == true` and `role in ('SURVEY_EXECUTIVE', 'ADMIN')`, patches `executives.last_login_at`, syncs `custom_villages`, `customer_surveys` + `survey_documents`, and `installation_reports` + `installation_photos`.
     - On pass: navigates to `AppScreen.VILLAGE_SELECT`.
2. `VillageSelectionScreen` (`AppScreen.VILLAGE_SELECT`)
   - District, Taluka, and Village bottom-sheet selectors (`assets/locations_mr.json` merged with synced rows from `public.custom_villages`), plus optional search (Taluka & Village only) and custom village/wadi creation (`POST /rest/v1/custom_villages`).
   - **Action button (`"ग्राहक यादी पहा"` -> `proceedToDashboard()`):**
     - Button is enabled only when `selectedVillage != null`.
     - Navigates to `AppScreen.MAIN_APP` (`MainTab.HOME`).
3. `MAIN_APP` (`AppScreen.MAIN_APP`) — 3 bottom tabs + centered `+ नवीन` button (`GharkulBottomBar`) + top-bar manual cloud sync icon (`survey_sync_button` -> `refreshCloudData()` when `isCloudConfigured`):
   - `MainTab.HOME` (`HomeDashboardScreen`): KPI summary (`stats`), search (`homeSearchQuery`), recent surveys list.
   - `MainTab.CUSTOMERS` (`CustomerListScreen`): search (`listSearchQuery`), status filter (`CustomerStatusFilter`), category filter (`SurveyCategory?`), survey cards with delete action (`DeleteCustomerConfirmationDialog` -> deletes local cache, `DELETE /rest/v1/customer_surveys?id=eq.<id>` cascading `survey_documents`, and batch-deletes R2 keys via `r2-presign`).
   - `CustomerDetailSheet` (ModalBottomSheet opened when tapping a customer card): displays details (including `"सरकारी फॉर्म पूर्ण · इन्स्टॉलेशनकडे वर्ग"` chip if `govFormCompleted == true`), Edit, Delete, and Share dialog (`मेसेज पाठवा / फॉर्म शेअर करा`: option 1 WhatsApp/text message enabled only when `finalCategory != CATEGORY_1`; option 2 Complete Form PDF via `SurveyPdfGenerator` which fetches `https://` R2 URLs on `Dispatchers.IO`).
   - `MainTab.PROFILE` (`ProfileScreen`): executive info, change village button (`AppScreen.VILLAGE_SELECT`), logout button (`POST /auth/v1/logout` + clears DataStore session -> `AppScreen.LOGIN`).
   - `+ नवीन` button (or Edit button on `CustomerDetailSheet`): opens `AddSurveyWizardScreen` (`wizard.isOpen == true`).
4. `AddSurveyWizardScreen` — **3-Step Survey Wizard**:
   - **Step 1 (`WizardStep1BasicAndMeter`) -> Next button (`"पुढे: कागदपत्रे"` -> `nextWizardStep()`):** Runs `validateStep1(w)`:
     1. `w.surveyDate.isBlank()` -> `"कृपया सर्व्हे दिनांक निवडा (* अनिवार्य)."`
     2. `w.beneficiaryName.isBlank()` -> `"कृपया घरकुलावरील नाव (मालकाचे नाव) टाका (* अनिवार्य)."`
     3. `w.mobile.filter { it.isDigit() }.length != 10` -> `"कृपया बरोबर १० अंकी मोबाईल नंबर टाका (* अनिवार्य)."`
     4. `w.emailId.trim().isBlank() || !Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(trimmedEmail)` -> `"कृपया वैध ई-मेल आयडी टाका (उदा. name@gmail.com) (* अनिवार्य)."`
     5. `w.meterInfo.isPoleNearbyForNewConn == null` -> `"कृपया 'नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?' निवडा (* अनिवार्य)."`
     6. `w.meterInfo.interestedInNewConnection == null` -> `"कृपया 'नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?' निवडा (* अनिवार्य)."`
     7. If `w.meterInfo.hasMeter == true`:
        - `w.meterInfo.consumerNumber.trim().let { it.isNotEmpty() && it.length != 12 }` -> `"ग्राहक क्रमांक भरल्यास बरोबर १२ अंकी असणे आवश्यक आहे."`
        - `!w.meterInfo.sameAsBeneficiary && w.meterInfo.meterHolderName.isBlank()` -> `"कृपया मीटर धारकाचे नाव टाका (* अनिवार्य)."`
     8. `w.siteSurveyDetails.roofType == null` -> `"कृपया छताचा प्रकार निवडा (* अनिवार्य)."`
     9. `w.siteSurveyDetails.hasSolarPanelSpace100SqFt == null` -> `"कृपया 'सोलर पॅनेलसाठी जागा उपलब्ध आहे का?' निवडा (* अनिवार्य)."`
     10. `w.siteSurveyDetails.generationMeterLocation.isBlank()` -> `"कृपया जनरेशन मीटर बसवण्याची जागा लिहा (* अनिवार्य)."`
     11. `w.siteSurveyDetails.hasEarthingSpace == null` -> `"कृपया 'अर्थिंगसाठी जागा उपलब्ध आहे का?' निवडा (* अनिवार्य)."`
     - On pass: advances to `currentStep = 2` and runs `recalculateWizardRules()`.
   - **Step 2 (`WizardStep2Documents`) -> Next button (`"पुढे: तपासणी"` -> `nextWizardStep()`):**
     - Performs **no validation**. Advances directly to `currentStep = 3` and runs `recalculateWizardRules()`.
   - **Step 3 (`WizardStep3ReviewAndResult`) -> Actions:**
     - `"संपादित करा"` (`onJumpToStep(1)`): jumps directly back to `currentStep = 1`.
     - `"जतन करा"` (`saveWizardSurvey()`):
       - Requires `selectedDistrict != null`, `selectedTaluka != null`, `selectedVillage != null`.
       - Checks `w.materialAndOfficeDetails.surveyorName.isBlank()` -> `"कृपया सर्व्हे करणाऱ्या व्यक्तीचे नाव लिहा."`
       - In Cloud Mode: uploads any local (`content://`) document photos (`survey-docs/{surveyId}/...`) and NoteCam roof/earthing photos (`survey-notecam/{surveyId}/...`) to Cloudflare R2 via `r2-presign`, then upserts `public.customer_surveys` and `public.survey_documents` via PostgREST, and closes wizard back to `MAIN_APP`.

#### B. Solar Installation Flow (`LoginRole.SOLAR_INSTALLATION`)
1. `LoginScreen` (`AppScreen.LOGIN`, role `SOLAR_INSTALLATION`)
   - **Action button (`"Sign In as Solar Installer"` -> `submitLogin()`):**
     - Checks `loginEmail` non-blank and contains `"@"`; otherwise `"Please enter a valid email address."`.
     - Checks `loginPassword.length >= 4`; otherwise `"Please enter at least a 4-character password."`.
     - In Cloud Mode: authenticates via Supabase Auth, verifies `executives.is_active == true` and `role in ('SOLAR_INSTALLATION', 'ADMIN')`, patches `last_login_at`, and syncs all records.
     - On pass: navigates to `AppScreen.INSTALLATION_DASHBOARD` (`InstallationTab.HOME`).
2. `InstallationDashboardScreen` (`AppScreen.INSTALLATION_DASHBOARD`) — 3-item bottom bar (`InstallationBottomBar`) + top-bar manual sync icon (`installation_sync_button` when `isCloudConfigured`):
   - `InstallationTab.HOME`: KPI cards (`Approved Ready`, `Completed`, `Cat. 1 Total`), `installationSearchQuery`, `installationFilter`, Completed Certificates list (Edit / Share PDF on `Dispatchers.IO`), and Approved Category 1 Pending Customers list (`finalCategory == CATEGORY_1` where `govFormCompleted == true || overallStatus == READY`, sorted with `govFormCompleted` customers first and tagged `"Gov Form Done"` vs `"Category 1 · Ready"`).
   - Centered Red `+` (`Manual`): opens `InstallationFormScreen` with blank manual form (`linkedSurveyId = null`).
   - `InstallationTab.PROFILE`: Installer details, counts, Corporate Office info, Logout button.
   - Tapping `"Fill Installation Certificate"` on a Category 1 survey opens `InstallationFormScreen` pre-filled (`linkedSurveyId = survey.id`).
3. `InstallationFormScreen` (`AppScreen.INSTALLATION_FORM`) — **3-Step Installation Certificate Wizard**:
   - Note: Top step pills (`1. Fill Form`, `2. Serial Nos.`, `3. NoteCam`) call `setInstallationFormPage(page)` directly without validation.
   - **Step 1 (`Fill Form`) -> Next button (`"Next: Serial Nos."` -> `nextInstallationFormPage()`):** Runs `validateInstallationPart1(form)`:
     1. `consumerNo.isBlank()` -> `"Please enter Consumer No. (Light Bill No)."`
     2. `installationDate.isBlank()` -> `"Please enter or select the Installation Date."`
     3. `consumerName.isBlank()` -> `"Please enter the Consumer Name."`
     4. `mobileNo.filter { it.isDigit() }.length != 10` -> `"Please enter a valid 10-digit Mobile No."`
     5. `systemCapacity.isBlank() || systemType.isBlank()` -> `"Please enter System Capacity and System Type."`
     6. `products.firstOrNull { it.isQtyRequired && it.qty.isBlank() }` -> `"Please enter QTY for ${missingRequiredQty.productModel}."`
     7. `installerName.isBlank()` -> `"Please enter the Installer Name."`
     8. `installerSignatureStrokes.isEmpty()` -> `"Please capture the Installer Signature."`
     9. `customerSignatureStrokes.isEmpty()` -> `"Please capture the Customer Sign and Stamp."`
     - On pass: advances to `currentPage = 2`.
   - **Step 2 (`Serial Nos.`) -> Next button (`"Next: NoteCam Photos"` -> `nextInstallationFormPage()`):** Runs `validateInstallationPart2(form)`:
     1. `!inverterSerial.isFilled` -> `"Please enter and re-enter the Invertor Serial Number."`
     2. `!inverterSerial.isMatch` -> `"Invertor Serial Number entries do not match. Please verify both fields."`
     3. `!generationMeterSerial.isFilled` -> `"Please enter and re-enter the Generation Meter Serial Number."`
     4. `!generationMeterSerial.isMatch` -> `"Generation Meter Serial Number entries do not match. Please verify both fields."`
     5. `panelSerials.size < 2` -> `"At least 2 Panel Serial Numbers are required."`
     6. For each panel `i` (1-based): `!panel.serial.isFilled` -> `"Please enter and re-enter Panel ${i} Serial Number."`; `!panel.serial.isMatch` -> `"Panel ${i} Serial Number entries do not match. Please verify both fields."`.
     - On pass: advances to `currentPage = 3`.
   - **Step 3 (`NoteCam`) -> Submit button (`"Submit Certificate"` -> `saveInstallationReport(onSaved)`):**
     - Re-runs `validateInstallationPart1(form)` (jumps to page 1 on failure) and `validateInstallationPart2(form)` (jumps to page 2 on failure).
     - Step 3 photos (`afterInstallationPhotos`) are optional.
     - In Cloud Mode: uploads any local (`content://`) `afterInstallationPhotos` to Cloudflare R2 (`installation-photos/{reportId}/...`), upserts `public.installation_reports`, syncs `public.installation_photos`, returns to `INSTALLATION_DASHBOARD`, and opens `ShareInstallationPdfOptionDialog` (`With Images` / `Without Images`, rendered on `Dispatchers.IO`).

---

## 2. Screen-by-Screen Field Specifications

### 2.1 `LoginScreen` (`AppScreen.LOGIN`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.loginRole` (`selectedRole`) | Verified against `public.executives.role` (`SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION`, or `ADMIN`) | `1. Executive Login` (`घरकुल सर्व्हे`) / `2. Solar Installation` (`Completion Form`) | radio (role card toggle) | `SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION` | yes | Always one selected; in Cloud Mode must match `executives.role` (or `ADMIN`) | `LoginRole.SURVEY_EXECUTIVE` | Always visible |
| `uiState.loginEmail` (`email`) | Supabase Auth `POST /auth/v1/token?grant_type=password` (`email`) + `public.executives.email` | Role 1: `ईमेल` / Role 2: `Email Address` | text (`KeyboardType.Email`) | Any string | yes | Non-blank and contains `"@"` (`!email.isBlank() && email.contains("@")`) | Cloud mode: `""`; Offline demo: `"ramesh.k@gharkulsolar.in"` (or `"installer@reflectionmedias.in"`) | Always visible |
| `uiState.loginPassword` (`password`) | Supabase Auth `POST /auth/v1/token?grant_type=password` (`password`) — never stored in PostgreSQL | Role 1: `पासवर्ड` / Role 2: `Password` | text (`KeyboardType.Password`, masked unless eye icon toggled) | Any string | yes | `password.length >= 4` | Cloud mode: `""`; Offline demo: `"123456"` | Always visible |

---

### 2.2 `VillageSelectionScreen` (`AppScreen.VILLAGE_SELECT`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.selectedDistrict` | Saved on `customer_surveys.district_name_mr`; custom village uses `custom_villages.district_id`, `district_name_mr` | `जिल्हा` (Sheet title: `जिल्हा निवडा`) | dropdown (ModalBottomSheet list) | Districts loaded from `assets/locations_mr.json`: `ahilyanagar` (`अहिल्यानगर`), `sambhajinagar` (`छत्रपती संभाजीनगर`) | yes | Non-null before proceeding | First district in `locations_mr.json` (`ahilyanagar` / `अहिल्यानगर`) | Always visible |
| `uiState.selectedTaluka` | Saved on `customer_surveys.taluka_name_mr`; custom village uses `custom_villages.taluka_id`, `taluka_name_mr` | `तालुका` (Sheet title: `तालुका निवडा (<district>)`) | dropdown (ModalBottomSheet list) | Talukas under `selectedDistrict.talukas` in `assets/locations_mr.json` (14 in Ahilyanagar, 9 in Sambhajinagar) | yes | Non-null before proceeding | `taluka.id == "nagar"` (`नगर`) else first taluka of `selectedDistrict` | Always visible |
| `uiState.selectedVillage` | Saved on `customer_surveys.village_id`, `village_name_mr` | `गाव` (Sheet title: `गाव निवडा (<taluka>)`) | dropdown (ModalBottomSheet list) | Villages under `selectedTaluka.villages` + rows synced from `public.custom_villages` | yes | `selectedVillage != null` required to enable `"ग्राहक यादी पहा"` button | `village.id == "ng_19"` (`हिवरे बाजार`) else first village of `selectedTaluka` | Always visible |
| `searchQuery` (local state) | Client-side UI filter only (not stored in DB) | Placeholder: `नाव शोधा (मराठी किंवा English)...` | text | Any string | no | Case-insensitive substring filter on `nameMr` / `nameEn` | `""` | Only when Taluka or Village picker sheet is open (`activeMode == TALUKA` or `VILLAGE`) |
| `customVillageInput` (`addAndSelectCustomVillage`) | `public.custom_villages` (`id`, `district_id`, `district_name_mr`, `taluka_id`, `taluka_name_mr`, `village_name_mr`, `village_name_en`, `created_by`) | `यादीत गाव / वाडी नसल्यास जोडा:` (Placeholder: `गावाचे नाव लिहा`, Button: `जोडा`) | text | Non-blank string | no | Button adds only if `customVillageInput.isNotBlank()`; trimmed; ID `"custom_${System.currentTimeMillis()}"`; upserted to `custom_villages` in Cloud Mode | `""` | Only when Village picker sheet is open (`activeMode == PickerMode.VILLAGE`) |

---

### 2.3 `HomeDashboardScreen` & `CustomerListScreen` (`AppScreen.MAIN_APP`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.homeSearchQuery` | Client-side UI filter over synced `customer_surveys` | Placeholder: `नाव किंवा मोबाईल शोधा` | text | Any string | no | Filters `beneficiaryName`, `mobile`, `gharkulId` (case-insensitive) | `""` | `activeTab == MainTab.HOME` |
| `uiState.listSearchQuery` | Client-side UI filter over synced `customer_surveys` | Placeholder: `नाव, मोबाईल, घरकुल आयडी किंवा मीटर क्र. शोधा` | text | Any string | no | Filters `beneficiaryName`, `mobile`, `gharkulId`, `meterInfo.consumerNumber` (case-insensitive) | `""` | `activeTab == MainTab.CUSTOMERS` |
| `uiState.selectedStatusFilter` | Client-side filter on `customer_surveys.overall_status` | Chips: `सर्व`, `तयार`, `ग्राहक बाकी` | radio (FilterChip row) | `ALL` (`सर्व`), `READY` (`तयार`), `PENDING_CUSTOMER` (`ग्राहक बाकी`) | no | Single selection | `CustomerStatusFilter.ALL` | `activeTab == MainTab.CUSTOMERS` |
| `uiState.selectedCategoryFilter` | Client-side filter on `customer_surveys.final_category` | Chips: `सर्व श्रेणी`, `श्रेणी 1`, `श्रेणी 2`, `श्रेणी 3`, `श्रेणी 4` | radio (toggleable FilterChip row) | `null` (`सर्व श्रेणी`), `CATEGORY_1`, `CATEGORY_2`, `CATEGORY_3`, `CATEGORY_4` | no | Tapping active chip resets to `null` | `null` | `activeTab == MainTab.CUSTOMERS` |

---

### 2.4 `AddSurveyWizardScreen` — Step 1 (`WizardStep1BasicAndMeter`)

Note: District, Taluka, Village (`village_id`, `village_name_mr`, `taluka_name_mr`, `district_name_mr`) and `executive_id` are inherited from `VillageSelectionScreen` and active session and saved on `public.customer_surveys`.

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.surveyDate` | `public.customer_surveys.survey_date` (`text`) | `* सर्व्हे दिनांक` | date (`DatePickerDialog`) | Date formatted `"dd / MM / yyyy"` via `Locale.getDefault()` | yes | Must not be blank (`surveyDate.isBlank()` checked) | Today's date (`defaultTodayDateString()`) | Always visible |
| `wizard.casteCategory` | `public.customer_surveys.caste_category` (`text` CHECK `'OPEN','SC','ST'`) | `* प्रवर्ग` | radio (segmented buttons) | `OPEN` (`"OPEN"`), `SC` (`"SC"`), `ST` (`"ST"`) | yes | Always non-null enum | `CasteCategory.OPEN` | Always visible |
| `wizard.capacity` | `public.customer_surveys.capacity_kw` (`integer`) | `* सोलर क्षमता` (Displayed text: `1 kW (घरकुल योजना)`) | read-only card (not user-editable) | `SolarCapacity.KW_1` (`1`) | yes | Fixed to `KW_1`; `saveWizardSurvey()` hardcodes `SolarCapacity.KW_1` | `SolarCapacity.KW_1` | Always visible |
| `wizard.beneficiaryName` | `public.customer_surveys.beneficiary_name` (`text`) | `* घरकुलावरील नाव (मालकाचे नाव)` | text | Any string | yes | Must not be blank (`beneficiaryName.isBlank()`); trimmed on save | `""` | Always visible |
| `wizard.mobile` | `public.customer_surveys.mobile` (`text`) | `* मोबाईल नं. (आधार व बँकेशी लिंक असलेला)` | phone (`KeyboardType.Phone`) | Digits only (`0-9`) | yes | Input strips non-digits & caps at 10 chars; validation requires `mobile.filter { it.isDigit() }.length == 10` | `""` | Always visible |
| `wizard.emailId` | `public.customer_surveys.email_id` (`text`) | `* ई-मेल आयडी (E-mail ID)` | text (`KeyboardType.Email`) | String without spaces (`it.replace(" ", "")`) | yes | Non-blank and matches `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | `""` | Always visible |
| `wizard.addressLandmark` | `public.customer_surveys.address_landmark` (`text`) | `पत्ता / जवळची खूण` | text | Any string | no | None; trimmed on save | `""` | Always visible |
| `wizard.gharkulId` | `public.customer_surveys.gharkul_id` (`text`) | `घरकुल आयडी / मंजुरी क्र.` | text | Any string | no | Non-blocking duplicate warning if matches another survey (`equals(..., ignoreCase = true)`); trimmed on save | `""` | Always visible |
| `wizard.aadhaarLast4` | `public.customer_surveys.aadhaar_last4` (`text`) | `आधार शेवटचे ४ अंक` | number (`KeyboardType.Number`) | Digits only (`0-9`) | no | Input strips non-digits & caps at 4 digits; non-blocking duplicate warning when `length == 4` matches another survey | `""` | Always visible |
| `wizard.gpsLocation` | `public.customer_surveys.gps_location` (`text`, nullable) | `घराचे GPS लोकेशन (पर्यायी)` | button (populates district demo coords) | `"19.8762° N, 75.3433° E"` (if `selectedDistrict.id == "sambhajinagar"`) or `"19.0948° N, 74.7480° E"` (otherwise) | no | None | `null` (Display: `"अद्याप नोंदवलेले नाही"`) | Always visible |
| `wizard.meterInfo.hasMeter` | `public.customer_surveys.has_meter` (`boolean`) | `* लाईट मीटर उपलब्ध आहे का?` | YES/NO (`YesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Non-null boolean; setting `false` forces `sameAsBeneficiary = false` | `true` (`होय`) | Always visible |
| `wizard.meterInfo.isPoleNearbyForNewConn` | `public.customer_surveys.is_pole_nearby_for_new_conn` (`boolean`, nullable) | `* नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` (`== null` blocks Step 1 Next) | `null` | Always visible |
| `wizard.meterInfo.interestedInNewConnection` | `public.customer_surveys.interested_in_new_connection` (`boolean`, nullable) | `* नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` (`== null` blocks Step 1 Next) | `null` | Always visible |
| `wizard.meterInfo.consumerNumber` | `public.customer_surveys.consumer_number` (`text`) | `ग्राहक क्रमांक (Consumer No.)` | number (`KeyboardType.Number`) | Digits only (`0-9`) | no | Input strips non-digits & caps at 12 digits; if non-empty, `trim().length == 12` required; non-blocking duplicate warning if matches another survey | `""` | Only when `meterInfo.hasMeter == true` |
| `wizard.meterInfo.sameAsBeneficiary` | `public.customer_surveys.same_as_beneficiary` (`boolean`) | `* मीटर लाभार्थ्याच्या नावावर आहे का?` | YES/NO (`YesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Setting `true` copies `wizard.beneficiaryName` into `meterHolderName` | `true` (`होय`) | Only when `meterInfo.hasMeter == true` |
| `wizard.meterInfo.meterHolderName` (when `sameAsBeneficiary == true`) | `public.customer_surveys.meter_holder_name` (`text`) | `* लाईट बिलावरील नाव` | text | Any string | no (UI shows `*`, NOT checked in `validateStep1` when `sameAsBeneficiary == true`) | Displayed value falls back to `wizard.beneficiaryName` if empty | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == true` |
| `wizard.meterInfo.meterHolderName` (when `sameAsBeneficiary == false`) | `public.customer_surveys.meter_holder_name` (`text`) | `* लाईट बिलावरील नाव (मीटर धारक)` | text | Any string | yes | Must not be blank (`!sameAsBeneficiary && meterHolderName.isBlank()` blocks Step 1 Next) | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.meterInfo.relationship` | `public.customer_surveys.meter_relationship` (`text`) | `लाभार्थ्याशी नाते` | radio (`FilterChip` row) | `"पती"`, `"पत्नी"`, `"वडील"`, `"आई"`, `"मुलगा"`, `"भाऊ"` | no | None | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.meterInfo.isHolderAlive` | `public.customer_surveys.is_holder_alive` (`boolean`) | `मीटर धारक हयात आहेत का?` | YES/NO (`YesNoPillGroup` custom labels) | `true` (`हयात`), `false` (`मृत`) | no | Non-null boolean; if `false`, requires `DEATH_CERTIFICATE` in Step 2 | `true` (`हयात`) | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.siteSurveyDetails.roofType` | `public.customer_surveys.roof_type` (`text`, nullable) | `* छताचा प्रकार` | radio (`FilterChip` FlowRow) | `RCC`, `TIN_SHEET`, `TILED`, `CEMENT_SHEET`, `UNDER_CONSTRUCTION` | yes | Must not be `null` | `null` | Always visible |
| `wizard.siteSurveyDetails.hasSolarPanelSpace100SqFt` | `public.customer_surveys.has_solar_panel_space_100_sqft` (`boolean`, nullable) | `* सोलर पॅनेलसाठी जागा उपलब्ध आहे का? (किमान १०० चौ.फूट)` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` | `null` | Always visible |
| `wizard.siteSurveyDetails.generationMeterLocation` | `public.customer_surveys.generation_meter_location` (`text`) | `* जनरेशन मीटर बसवण्याची जागा` | text | Any string | yes | Must not be blank (`generationMeterLocation.isBlank()` blocks Step 1 Next) | `""` | Always visible |
| `wizard.siteSurveyDetails.hasEarthingSpace` | `public.customer_surveys.has_earthing_space` (`boolean`, nullable) | `* अर्थिंगसाठी (Earthing) जागा उपलब्ध आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` | `null` | Always visible |
| `wizard.houseType` | `public.customer_surveys.house_type` (`text` CHECK `'SINGLE','DUPLEX'`) | `* छत किंवा घर एकच मजली आहे का?` | radio (button pair) | `SINGLE` (`"1 मजली"`), `DUPLEX` (`"2 मजली"`) | yes | Always non-null enum | `HouseType.SINGLE` | Always visible |

---

### 2.5 `AddSurveyWizardScreen` — Step 2 (`WizardStep2Documents`)

#### A. Document Compliance Checkboxes (`महत्त्वाच्या सूचना व तपासणी`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.documentComplianceChecks.lightBillIsResidential` | `public.customer_surveys.light_bill_is_residential` (`boolean`) | `लाईट बिल घरगुती (Residential) आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.panNameAndDobMatchAadhaar` | `public.customer_surveys.pan_name_dob_match_aadhaar` (`boolean`) | `आधार आणि पॅनवरील नाव व जन्मतारीख सारखीच आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.bankIsNationalizedNotAdcc` | `public.customer_surveys.bank_is_nationalized_not_adcc` (`boolean`) | `राष्ट्रीयकृत बँक खाते आहे (ADCC बँक नाही; खाते क्र. व IFSC स्पष्ट)` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.singleMobileLinkedAllDocs` | `public.customer_surveys.single_mobile_linked_all_docs` (`boolean`) | `आधार, पॅन, लाईट बिल व बँक खात्याला एकच चालू मोबाईल लिंक आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.aadhaarSeededToBank` | `public.customer_surveys.aadhaar_seeded_to_bank` (`boolean`) | `बँक खाते आधारशी लिंक (Aadhaar Seeding) आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |

#### B. Document Capture Cards (`requiredList = SurveyRuleEngine.requiredDocumentsFor(meterInfo, casteCategory)`)

Each `docType` row is persisted in **`public.survey_documents`** (`id = "${surveyId}_${docType.name}"`, unique on `(survey_id, doc_type)`), with its binary JPEG stored in **Cloudflare R2** under `survey-docs/{surveyId}/{doc_type_lower}_{timestamp}.jpg`:

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.documents[docType].printedName` | `public.survey_documents.printed_name` (`text`) | Placeholder: `कागदपत्रावरील नाव लिहा` (on `<docType.labelMr>` card) | text | Any string | no (Step 2 Next has no validation) | Live name-matching via `SurveyRuleEngine.matchName(...)`; sets `survey_documents.match_result` | `""` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].photoUri` (+ `r2ObjectKey`, `photoSizeKb`, `isSizeValid`) | `public.survey_documents.photo_url` (Cloudflare R2 public URL), `r2_object_key`, `photo_size_kb`, `is_size_valid` | Camera button / sheet options: `कॅमेराने फोटो काढा`, `गॅलरीतून निवडा`, `नमुना फोटो जोडा` | photo/file (`TakePicture`, `PickVisualMedia.ImageOnly`, or `"captured://sample_${docType.name}"`) | Local URI uploaded via `r2-presign` -> `https://<R2_PUBLIC_BASE_URL>/survey-docs/...` | no (absence keeps `legibility = MISSING` & adds pending action on Category 1/3) | **No file size limit**; original bytes uploaded as `image/jpeg`; `isSizeValid` always `true`; sets `legibility = DocumentLegibility.CLEAR` on upload | `null` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].legibility` | `public.survey_documents.legibility` (`text` CHECK `'CLEAR','UNCLEAR','MISSING'`) | `बरोबर` / `उपलब्ध नाही` (FilterChips) | toggle (`CLEAR` / `MISSING`) | `CLEAR` (`बरोबर`), `MISSING` (`उपलब्ध नाही`) (`UNCLEAR` exists in enum/DB but hidden in UI) | no | Selecting toggles between `CLEAR` and `MISSING` | `DocumentLegibility.MISSING` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].queryNote` | `public.survey_documents.query_note` (`text`) | Toggle chip: `टीप (Note)` / `टीप (नोंदलेली)`; Field label: `कागदपत्राबाबत शंका / टीप` | text (2–3 lines) | Any string | no | None | `""` | Visible after tapping `"टीप (Note)"` chip or when `queryNote.isNotBlank()` |

**Document Card Visibility (`requiredDocumentsFor`):**
- Always visible (`isRequiredCore = true`, UI shows `*`):
  1. `LIGHT_BILL` (`* लाईट बिल`)
  2. `AADHAAR` (`* आधार कार्ड`)
  3. `PAN` (`* पॅन कार्ड`)
  4. `BANK_PASSBOOK` (`* बँक पासबुक किंवा कॅन्सल चेक`)
  5. `OWNERSHIP_PROOF` (`* मालकी हक्काचा पुरावा`)
  6. `PASSPORT_PHOTO` (`* पासपोर्ट साईज फोटो`)
- Conditional (`UI shows *` whenever card is visible):
  7. `CASTE_CERTIFICATE` (`* जातीचा दाखला (SC / ST)`): visible only when `casteCategory == SC || casteCategory == ST`.
  8. `RATION_CARD` (`* रेशन कार्ड`): visible only when `meterInfo.hasMeter == true && !meterInfo.sameAsBeneficiary`.
  9. `DEATH_CERTIFICATE` (`* मृत्यू दाखला`): visible only when `meterInfo.hasMeter == true && !meterInfo.sameAsBeneficiary && !meterInfo.isHolderAlive`.

#### C. Note Cam Section (`Note Cam फोटो (साईट Geo-Location सह)`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.siteSurveyDetails.noteCamRoofBeforePhotoUri` (+ `Done`, `R2Key`, `Kb`, `GeoInfo`) | `public.customer_surveys.notecam_roof_done`, `notecam_roof_photo_url` (R2 URL), `notecam_roof_r2_key`, `notecam_roof_photo_kb`, `notecam_roof_geo_info` | `सोलर बसवण्याच्या आधीच्या छताचा फोटो` (Buttons: `कॅमेरा (Geo)`, `गॅलरी`) | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Local URI uploaded via `r2-presign` -> R2 `survey-notecam/{surveyId}/notecam_roof_<ts>.jpg` | no | Camera path requires `CAMERA` + (`ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`) and live device GPS enabled. Gallery path copies raw image without overlay or GPS check. | `null` (`Done = false`, `GeoInfo = ""`) | Always visible on Step 2 |
| `wizard.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri` (+ `Done`, `R2Key`, `Kb`, `GeoInfo`) | `public.customer_surveys.notecam_earthing_done`, `notecam_earthing_photo_url` (R2 URL), `notecam_earthing_r2_key`, `notecam_earthing_photo_kb`, `notecam_earthing_geo_info` | `Earthing Point चा फोटो (मालकासोबत)` (Buttons: `कॅमेरा (Geo)`, `गॅलरी`) | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Local URI uploaded via `r2-presign` -> R2 `survey-notecam/{surveyId}/notecam_earthing_<ts>.jpg` | no | Same as `noteCamRoofBeforePhotoUri` above. | `null` (`Done = false`, `GeoInfo = ""`) | Always visible on Step 2 |

---

### 2.6 `AddSurveyWizardScreen` — Step 3 (`WizardStep3ReviewAndResult`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.checklist.gharkulAndMeterSamePerson` | `public.customer_surveys.checklist_answers` (`jsonb` -> `gharkulAndMeterSamePerson`) | `१. घरकुल व मीटर एकाच व्यक्तीच्या नावावर आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Derived automatically from Step 1/2 until manually edited | `true` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.allDocsHaveSameName` | `public.customer_surveys.checklist_answers` (`jsonb` -> `allDocsHaveSameName`) | `१. बँक पासबुक, आधार कार्ड, पॅन कार्ड, लाईट बिल या सर्वांवर नाव समान आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Derived automatically from Step 1/2 until manually edited | `true` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.aadhaarNameChangeNeeded` | `public.customer_surveys.checklist_answers` (`jsonb` -> `aadhaarNameChangeNeeded`) | `२. आधार कार्डवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.checklist.lightBillNameChangeNeeded` | `public.customer_surveys.checklist_answers` (`jsonb` -> `lightBillNameChangeNeeded`) | `२. लाईट बिलावरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.checklist.panNameChangeNeeded` | `public.customer_surveys.checklist_answers` (`jsonb` -> `panNameChangeNeeded`) | `२. पॅन कार्डवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.checklist.passbookNameChangeNeeded` | `public.customer_surveys.checklist_answers` (`jsonb` -> `passbookNameChangeNeeded`) | `२. बँक पासबुकवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.checklist.meterOnFamilyMemberName` | `public.customer_surveys.checklist_answers` (`jsonb` -> `meterOnFamilyMemberName`) | `३. घरकुल नावावर आहे पण लाईट मीटर कनेक्शन घरातील दुसऱ्या व्यक्तीच्या नावे आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.checklist.noElectricityConnection` | `public.customer_surveys.checklist_answers` (`jsonb` -> `noElectricityConnection`) | `४. घरकुल नावावर आहे पण लाईट कनेक्शन नाही का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.documentsCompleteStatus` | `public.customer_surveys.documents_complete_status` (`boolean`) | `* कागदपत्रांची स्थिती` | radio (segmented buttons) | `true` (`पूर्ण`), `false` (`अपूर्ण`) | yes | Always non-null boolean; also auto-synced to `pending.isEmpty()` inside `recalculateWizardRules()` | `true` (`पूर्ण`) | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.earthWireMeters` | `public.customer_surveys.earth_wire_meters` (`text`) | `१६ Sq.mm. Aluminium (Earth Wire)` | number (digits via custom drawer -> `"<n> मीटर"`) | Positive integer `1..9999` or default `"20 मीटर"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled when `toIntOrNull() > 0` | `"20 मीटर"` | Value always visible; custom input when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.acCableMeters` | `public.customer_surveys.ac_cable_meters` (`text`) | `AC 1.5 sq.mm 3 Core केबल` | number (digits -> `"<n> मीटर"`) | Positive integer `1..9999` or default `"10 मीटर"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled when `toIntOrNull() > 0` | `"10 मीटर"` | Value always visible; custom input when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.pipeMeters` | `public.customer_surveys.pipe_meters` (`text`) | `पाईप (१ पाईप = ३० मीटर)` | number (digits -> `"<n> पाईप"`, displayed as `"<n> पाईप (<n*30> मीटर)"`) | Positive integer `1..9999` or default `"5 पाईप"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled when `toIntOrNull() > 0` | `"5 पाईप"` | Value always visible; custom input when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.customerSignatureStrokes` | `public.customer_surveys.signatures` (`jsonb` -> `customer`: `[[[x,y],...],...]`) | `ग्राहक / अर्जदाराची सही` | signature canvas (`List<List<Pair<Float, Float>>>`) | Finger-drawn strokes or empty list | no (not checked on Save) | None | `emptyList()` | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.surveyorName` | `public.customer_surveys.surveyor_name` (`text`) | `* सर्व्हे करणाऱ्या व्यक्तीचे नाव` | text | Any string | yes | Must not be blank (`surveyorName.isBlank()` blocks `"जतन करा"`) | `executive.nameMr` | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.surveyorSignatureStrokes` | `public.customer_surveys.signatures` (`jsonb` -> `surveyor`: `[[[x,y],...],...]`) | `सर्व्हे करणाऱ्या व्यक्तीची सही` | signature canvas (`List<List<Pair<Float, Float>>>`) | Finger-drawn strokes or empty list | no (not checked on Save) | None | `emptyList()` | Always visible on Step 3 |

*(Also persisted on `public.customer_surveys` from computed/web workflow fields: `matched_categories` (`jsonb`), `suggested_category`, `final_category`, `override_reason`, `overall_status`, `pending_actions` (`jsonb`), `executive_remark`, `gov_form_completed`, `gov_form_completed_at`, `survey_date_label`, `is_surveyed_today`.)*

---

### 2.7 `InstallationDashboardScreen` (`AppScreen.INSTALLATION_DASHBOARD`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.installationSearchQuery` | Client-side UI filter over synced `customer_surveys` (Cat. 1) & `installation_reports` | Placeholder: `Search consumer name, consumer no., or mobile` | text | Any string | no | Filters `consumerName`/`beneficiaryName`, `consumerNo`/`consumerNumber`, `mobile`, `villageName` | `""` | `activeInstallationTab == HOME` |
| `uiState.installationFilter` | Client-side queue tab | Tabs: `इन्स्टॉलेशन बाकी ग्राहक`, `माझे पूर्ण इन्स्टॉलेशन` | tab selector (`PENDING` / `COMPLETED`) | `PENDING` (`इन्स्टॉलेशन बाकी ग्राहक`), `COMPLETED` (`माझे पूर्ण इन्स्टॉलेशन`) | no | Single selection | `InstallationQueueFilter.PENDING` | `activeInstallationTab == HOME` |

---

### 2.8 `InstallationFormScreen` — Step 1 (`Fill Form`)

*(All labels in this workflow are in **English**.)*

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `installationForm.consumerNo` | `public.installation_reports.consumer_no` (`text`) | `Consumer No. *` | number (`KeyboardType.Number`) | Any non-blank string | yes | Must not be blank (`consumerNo.isBlank()` checked; no length restriction enforced) | `""` (pre-filled from `survey.meterInfo.consumerNumber` if started from Category 1 customer) | Step 1, Section A expanded (`consumerExpanded = true`) |
| `installationForm.installationDate` | `public.installation_reports.installation_date` (`text`) | `Installation Date *` | date (text + `DatePickerDialog`) | Date string (picker formats `"dd / MM / yyyy"` using `Locale.ENGLISH`) | yes | Must not be blank | `defaultInstallationDate()` (today) | Step 1, Section A expanded |
| `installationForm.consumerName` | `public.installation_reports.consumer_name` (`text`) | `Consumer Name *` | text (`KeyboardCapitalization.Words`) | Any non-blank string | yes | Must not be blank; trimmed on save | `""` (or `survey.beneficiaryName`) | Step 1, Section A expanded |
| `installationForm.mobileNo` | `public.installation_reports.mobile_no` (`text`) | `Mobile No. *` (full-width row) | phone (`KeyboardType.Phone`) | Digits | yes | `mobileNo.filter { it.isDigit() }.length == 10`; stores digits-only on save | `""` (or `survey.mobile`) | Step 1, Section A expanded |
| `installationForm.systemCapacity` | `public.installation_reports.system_capacity` (`text`) | `System Capacity *` | text | Any non-blank string | yes | Must not be blank | `"1 KW"` | Step 1, Section A expanded |
| `installationForm.systemType` | `public.installation_reports.system_type` (`text`) | `System Type *` | text | Any non-blank string | yes | Must not be blank | `"ONGRID"` | Step 1, Section A expanded |
| `installationForm.products[0..22].company` / `.size` / `.capacity` | `public.installation_reports.products` (`jsonb` array of 23 objects: `{rowId, srNoDisplay, productModel, isSubRow, isQtyRequired, company, size, capacity, qty}`) | `Company`, `Size`, `Capacity` for each of 23 rows (2-column stacked grid: Row 1 `Company` \| `Size`, Row 2 `Capacity` \| `QTY`/`QTY *`) | text | Any string | no | Editable freely; not validated on Next/Save | `""` (all empty by default) | Step 1, Section B expanded (`materialsExpanded = true`) |
| `installationForm.products[0..22].qty` | `public.installation_reports.products[i].qty` (`jsonb`) | `QTY *` (12 rows where `isQtyRequired == true`, with `* Required` badge) / `QTY` (11 optional rows) | text (2nd cell of Row 2 in 2x2 item grid) | Any string | yes for 12 rows (`isQtyRequired == true`); no for other 11 rows | When `isQtyRequired == true`, `qty.isBlank()` blocks Step 1 Next (`"Please enter QTY for <productModel>."`) | Pre-filled `defaultInstallationProductRows()` (see list below; unmentioned rows default to `""`) | Step 1, Section B expanded |
| `installationForm.installerName` | `public.installation_reports.installer_name` (`text`) | `Installer Name *` | text (`KeyboardCapitalization.Words`) | Any non-blank string | yes | Must not be blank | `installationExecutive.nameMr` | Step 1, Section C expanded (`signOffExpanded = true`) |
| `installationForm.installerSignatureStrokes` | `public.installation_reports.signatures` (`jsonb` -> `installer`: `[[[x,y],...],...]`) | `Installer Signature *` | signature canvas | Non-empty stroke list | yes | `installerSignatureStrokes.isEmpty()` blocks Step 1 Next | `emptyList()` | Step 1, Section C expanded |
| `installationForm.supervisorName` | `public.installation_reports.supervisor_name` (`text`) | `1) Supervisor` (under `Office Verification (Optional)`) | text (`KeyboardCapitalization.Words`) | Any string | no | None | `""` | Step 1, Section C expanded |
| `installationForm.managerName` | `public.installation_reports.manager_name` (`text`) | `2) Manager` | text (`KeyboardCapitalization.Words`) | Any string | no | None | `""` | Step 1, Section C expanded |
| `installationForm.customerSignatureStrokes` | `public.installation_reports.signatures` (`jsonb` -> `customer`: `[[[x,y],...],...]`) | `Customer Sign and Stamp *` | signature canvas | Non-empty stroke list | yes | `customerSignatureStrokes.isEmpty()` blocks Step 1 Next | `emptyList()` | Step 1, Section C expanded |

**Default 23 `InstallationProductRow` rows (`defaultInstallationProductRows()` — all `company`, `size`, `capacity` default to `""`):**
- `1.` `PANEL'S`: QTY `"02"`, `isQtyRequired = true`
- `2.` `INVERTER`: QTY `"01"`, `isQtyRequired = true`
- Sub-row `2A` `A) LA Cable 16 sq mm`: QTY `"20 mtr"`, `isQtyRequired = true`
- Sub-row `2B` `B) AC Cable 1.5 sq mm`: QTY `"10 mtr"`, `isQtyRequired = true`
- Sub-row `2C` `C) DC Cable`: QTY `""`, `isQtyRequired = false`
- Sub-row `2D` `D) Earthing Cable`: QTY `""`, `isQtyRequired = false`
- `3.` `ACDB`: QTY `"01"`, `isQtyRequired = true`
- `4.` `EARTHING RODE`: QTY `"02"`, `isQtyRequired = true`
- `5.` `L.A.`: QTY `"01"`, `isQtyRequired = true`
- `6.` `CHEMICAL BAG`: QTY `"01"`, `isQtyRequired = true`
- `7.` Sub-row `7A` `A) PVC PIPE`: QTY `"08"`, `isQtyRequired = true`
- Sub-row `7B` `B) PVC CONDUIT`: QTY `""`, `isQtyRequired = false`
- Sub-row `7C` `C) PVC ELBOW`: QTY `""`, `isQtyRequired = false`
- Sub-row `7D` `D) PVC T`: QTY `""`, `isQtyRequired = false`
- Sub-row `7E` `E) PVC CLAMP`: QTY `""`, `isQtyRequired = false`
- Sub-row `7F` `F) PVC LONG BEND`: QTY `""`, `isQtyRequired = false`
- `8.` `MC 4 CONNECTOR`: QTY `""`, `isQtyRequired = false`
- `9.` `LUG FOR STRUCTURE`: QTY `""`, `isQtyRequired = false`
- `10.` `LUG FOR ACDB`: QTY `""`, `isQtyRequired = false`
- `11.` `LUG FOR L.A.`: QTY `"02"`, `isQtyRequired = true`
- `12.` `EARTH COVER PIT`: QTY `"02"`, `isQtyRequired = true`
- `13.` `GENERATION METER`: QTY `"01"`, `isQtyRequired = true`
- `14.` `EXTRA WIRING`: QTY `""`, `isQtyRequired = false`

---

### 2.9 `InstallationFormScreen` — Step 2 (`Serial Nos.`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `installationForm.inverterSerial.primaryValue` & `.confirmValue` | `public.installation_reports.inverter_serial_primary`, `inverter_serial_confirm` (`text`) | `Invertor Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes | Both trimmed values non-empty and exact match (`trimmedPrimary == trimmedConfirm`) | `""` / `""` | Always visible on Step 2 |
| `installationForm.generationMeterSerial.primaryValue` & `.confirmValue` | `public.installation_reports.generation_meter_serial_primary`, `generation_meter_serial_confirm` (`text`) | `Generation Meter Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes | Both trimmed values non-empty and exact match | `""` / `""` | Always visible on Step 2 |
| `installationForm.panelSerials[i].serial.primaryValue` & `.confirmValue` | `public.installation_reports.panel_serial_numbers` (`jsonb` array of `{id, primaryValue, confirmValue}`) | `Panel ${i+1} Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes (minimum 2 panels; dynamic `+ Add Panel Serial Number` button adds more; delete button visible for index `>= 2` when `size > 2`) | `panelSerials.size >= 2`; every panel must have both trimmed fields non-empty and matching | 2 items (`panel_1`, `panel_2`) with `""` / `""` | Always visible on Step 2 |

---

### 2.10 `InstallationFormScreen` — Step 3 (`NoteCam`)

Each photo slot is persisted in **`public.installation_photos`** (`report_id` FK -> `installation_reports.id`, ordered by `slot_index`), with binary JPEG stored in **Cloudflare R2** under `installation-photos/{reportId}/...`:

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `installationForm.afterInstallationPhotos[i].caption` | `public.installation_photos.caption` (`text`) | `Photo Description / Label` (Dynamic cards + `Add More Photo` button; delete slot icon visible when `size > 1 && idx >= 1`) | text | Any string | no | None | Slot 1: `"Solar Site Photo (After Installation)"`; added slots: `"After Installation Photo ${nextIdx}"` | Always visible on Step 3 |
| `installationForm.afterInstallationPhotos[i].photoUri` (+ `r2ObjectKey`, `photoSizeKb`, `geoInfo`, `isCameraGeoStamped`) | `public.installation_photos.photo_url` (R2 URL), `r2_object_key`, `photo_size_kb`, `geo_info`, `is_camera_geo_stamped`, `slot_index` | `NoteCam (Geo)` / `Gallery` | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Local URI uploaded via `r2-presign` -> R2 `installation-photos/{reportId}/...` | no (photos optional on submit) | `NoteCam (Geo)` requires `CAMERA` + (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) and device GPS enabled (otherwise opens `"Enable GPS Location"` dialog). Gallery copies original bytes without overlay. | `null` | Always visible on Step 3 |

---

## 3. Document Uploads, Cloudflare R2 & Local Cache Storage

### 3.1 Cloudflare R2 Remote Object Storage (Cloud Mode)
- **Presign & Delete Gateway:** Supabase Edge Function `POST ${SUPABASE_URL}/functions/v1/r2-presign` (`/supabase/functions/r2-presign/index.ts`).
  - **Authentication:** Requires `Authorization: Bearer <supabase_access_token>` and `apikey: <SUPABASE_ANON_KEY>`. The Edge Function calls `supabase.auth.getUser()` and verifies `public.executives.is_active == true`.
  - **Presign Request JSON:** `{ "action": "presign", "folder": "survey-docs" | "survey-notecam" | "installation-photos", "recordId": "<survey_or_report_id>", "fileName": "<file.jpg>", "contentType": "image/jpeg" }`.
  - **Presign Response JSON:** `{ "upload_url": "<5-min S3 SigV4 PUT URL>", "public_url": "<R2_PUBLIC_BASE_URL>/<object_key>", "object_key": "<folder>/<recordId>/<fileName>" }`.
  - **Upload Flow (`CloudflareR2Uploader`):** Local `content://` photos are read via `ContentResolver` and `PUT` directly to `upload_url` with `Content-Type: image/jpeg`. Already-uploaded `http(s)://` URLs and demo `"captured://"` markers are skipped without re-uploading.
  - **Batch Delete Request JSON:** `{ "action": "delete", "object_keys": ["survey-docs/...", ...] }` (invoked when deleting a customer survey in `SupabaseSurveyRepository.deleteSurveyWithUploads`).

### 3.2 Local On-Device Staging Cache & PDF Export
- **Photo Staging Directory:** `<context.cacheDir>/survey_photos/` (`res/xml/file_paths.xml` -> `<cache-path name="survey_photos" path="survey_photos/" />`)
- **Exposed URI Authority:** `content://${context.packageName}.fileprovider/survey_photos/<filename>` (`com.aistudio.gharkulsurvey.mhsxqr.fileprovider`)
- **Generated PDFs Directory:** `<context.cacheDir>/survey_pdfs/`:
  - Survey PDF: `gharkul_${safeName}_${timestamp}.pdf` (embeds brand logo & all document + NoteCam photos — supports both local `content://` URIs and remote `https://` R2 URLs via `HttpURLConnection` on `Dispatchers.IO`)
  - Installation PDF: `installation_${modeTag}_${safeName}_${timestamp}.pdf` (`modeTag` = `"with_images"` or `"certificate"`, supports local `content://` and remote `https://` R2 URLs on `Dispatchers.IO`)

| Slot / `DocumentType` | Allowed Source / File Type | Size Limit | Cloudflare R2 Object Key Path | Local Staging File Name (`survey_photos/`) | Pixel Modification / Overlay |
|---|---|---|---|---|---|
| `LIGHT_BILL`, `AADHAAR`, `PAN`, `BANK_PASSBOOK`, `OWNERSHIP_PROOF`, `PASSPORT_PHOTO`, `CASTE_CERTIFICATE`, `RATION_CARD`, `DEATH_CERTIFICATE` | Camera (`TakePicture`) / Gallery (`PickVisualMedia.ImageOnly`) / Sample (`captured://sample_<TYPE>`) | **None** (`isSizeValid` always `true`) | `survey-docs/{surveyId}/{doc_type_lower}_{epoch_ms}.jpg` | `<doc_type_lower>_<epoch_ms>.jpg` | Original bytes copied & uploaded unmodified |
| Survey NoteCam: `ROOF` (`NoteCamSlot.ROOF`) | Camera (`TakePicture` -> raw + stamped copy) OR Gallery (`ImageOnly`) | **None** | `survey-notecam/{surveyId}/notecam_roof_{epoch_ms}.jpg` | Raw: `notecam_roof_raw_<epoch_ms>.jpg`; Saved: `notecam_roof_<epoch_ms>.jpg` | Camera: scaled to max `1920px`, JPEG quality `90`, bottom-right translucent badge (`GPS: <lat/lon>`, live geocoded address or `locationBreadcrumb`, `dd/MM/yyyy hh:mm a`). Gallery: unmodified. |
| Survey NoteCam: `EARTHING` (`NoteCamSlot.EARTHING`) | Camera OR Gallery (`ImageOnly`) | **None** | `survey-notecam/{surveyId}/notecam_earthing_{epoch_ms}.jpg` | Raw: `notecam_earthing_raw_<epoch_ms>.jpg`; Saved: `notecam_earthing_<epoch_ms>.jpg` | Same as Survey NoteCam `ROOF` |
| Installation Step 3 (`afterInstallationPhotos[i]`) | Camera (`NoteCam (Geo)`) OR Gallery (`ImageOnly`) | **None** | `installation-photos/{reportId}/{safePhotoId}_{idx}_{epoch_ms}.jpg` | Raw: `install_notecam_raw_<epoch_ms>.jpg`; Camera stamped: `install_notecam_<epoch_ms>.jpg`; Gallery: `install_site_<epoch_ms>.jpg` | Camera: scaled to max `1920px`, JPEG quality `90`, bottom-right translucent badge (`GPS: <lat/lon>`, live geocoded address or `villageName` / `"Solar Installation Site"`, timestamp). Gallery: unmodified. |

---

## 4. Category Decision Logic (`SurveyRuleEngine`)

Categories (`SurveyCategory`) are determined in two stages:

### Stage A: Auto-Derivation of `ChecklistAnswers` (`SurveyRuleEngine.deriveChecklist`)
Runs automatically whenever Step 1 or Step 2 inputs change, **until** the user manually toggles any Yes/No switch on Step 3 (`isChecklistManuallyEdited = true`). Modifying any document entry resets `isChecklistManuallyEdited = false`.

- Reference name for Aadhaar = `beneficiaryName`.
- Reference name for PAN, Light Bill, Passbook = `aadhaarName = documents[AADHAAR]?.printedName?.ifBlank { beneficiaryName } ?: beneficiaryName`.
- `matchName(ref, docName)` returns `MATCH` only after stripping honorifics (`श्री`, `श्रीमती`, `सौ`, `कु`, `mr`, `mrs`, `smt`), collapsing whitespace, and lowercasing for exact equality.
- Derived flags:
  - `gharkulAndMeterSamePerson` (`Q1a`) = `meterInfo.hasMeter && meterInfo.sameAsBeneficiary`
  - `allDocsHaveSameName` (`Q1b`) = `samePerson && gharkulMatchesAadhaar && panMatches && billMatches && passbookMatches`
  - `aadhaarNameChangeNeeded` (`Q2a`) = `!gharkulMatchesAadhaar && AADHAAR.printedName.isNotBlank()`
  - `lightBillNameChangeNeeded` (`Q2b`) = `meterInfo.hasMeter && !billMatches && LIGHT_BILL.printedName.isNotBlank()`
  - `panNameChangeNeeded` (`Q2c`) = `!panMatches && PAN.printedName.isNotBlank()`
  - `passbookNameChangeNeeded` (`Q2d`) = `!passbookMatches && BANK_PASSBOOK.printedName.isNotBlank()`
  - `meterOnFamilyMemberName` (`Q3`) = `meterInfo.hasMeter && !meterInfo.sameAsBeneficiary`
  - `noElectricityConnection` (`Q4`) = `!meterInfo.hasMeter`

### Stage B: Matching & Priority Rule `4 > 3 > 2 > 1` (`evaluateMatchedCategories` & `suggestCategoryWithPriority`)
Using the 8 Yes/No answers from `ChecklistAnswers`:

1. **Conditions evaluated:**
   - `cat2Triggered` = `aadhaarNameChangeNeeded || lightBillNameChangeNeeded || panNameChangeNeeded || passbookNameChangeNeeded || !allDocsHaveSameName`
   - `cat3Triggered` = `meterOnFamilyMemberName || !gharkulAndMeterSamePerson`
   - `cat4Triggered` = `noElectricityConnection`
2. **Matched list (`matchedCategories`):**
   - `CATEGORY_1` added **iff**: `!cat2Triggered && !cat3Triggered && !cat4Triggered && gharkulAndMeterSamePerson && allDocsHaveSameName`
   - `CATEGORY_2` added **if**: `cat2Triggered`
   - `CATEGORY_3` added **if**: `(cat3Triggered && !cat4Triggered) || meterOnFamilyMemberName`
   - `CATEGORY_4` added **if**: `cat4Triggered`
   - Fallback if `matched` is empty: adds `CATEGORY_2`
3. **Final Suggested Category (`suggestedCategory`):**
   - Highest category number in `matchedCategories` (`matched.maxByOrNull { it.number } ?: CATEGORY_2`) — enforcing priority **`4 > 3 > 2 > 1`**.

### Stage C: Overall Status (`computeOverallStatus`) & Web -> Installation Hand-Off
- **`READY`:** `finalCategory == CATEGORY_1 && pendingActions.isEmpty()` (all documents in `requiredDocumentsFor()` have `legibility != MISSING`).
- **`PENDING_CUSTOMER`:** `pendingActions.any { !it.isCompanyAction } || pendingActions.isEmpty()` (when not Category 1 ready).
- **`PENDING_COMPANY`:** `pendingActions.isNotEmpty()` and all `pendingActions` have `isCompanyAction == true`.
- **Web Form-Completed Hand-Off (`gov_form_completed`):** When Office Admin clicks **फॉर्म पूर्ण (Form Completed)** on a Category 1 customer in the Web App, PostgreSQL updates `gov_form_completed = true` and `gov_form_completed_at = now()`. On Android, `approvedCategory1Surveys` prioritizes `govFormCompleted == true` customers at the top of the Solar Installation Portal with a green `"Gov Form Done"` chip.

---

## 5. Data Models (Exact Property Names & Types)

### 5.1 Survey Models (`com.example.model.SurveyModels.kt`)

- **`CustomerSurvey`**:
  `id: String`, `villageId: String`, `villageNameMr: String`, `talukaNameMr: String`, `districtNameMr: String`, `surveyDate: String`, `beneficiaryName: String`, `mobile: String`, `emailId: String`, `addressLandmark: String`, `gharkulId: String`, `aadhaarLast4: String`, `casteCategory: CasteCategory`, `houseType: HouseType`, `capacity: SolarCapacity`, `gpsLocation: String?`, `meterInfo: MeterInfo`, `siteSurveyDetails: SiteSurveyDetails`, `documentComplianceChecks: DocumentComplianceChecks`, `materialAndOfficeDetails: MaterialAndOfficeDetails`, `documents: Map<DocumentType, DocumentEntry>`, `checklistAnswers: ChecklistAnswers`, `matchedCategories: List<SurveyCategory>`, `suggestedCategory: SurveyCategory`, `finalCategory: SurveyCategory`, `overrideReason: String = ""`, `overallStatus: OverallStatus`, `pendingActions: List<PendingActionItem>`, `executiveRemark: String = ""`, `surveyDateLabel: String`, `isSurveyedToday: Boolean = true`, `executiveId: String? = null`, `govFormCompleted: Boolean = false`, `govFormCompletedAt: String? = null`

- **`MeterInfo`**:
  `hasMeter: Boolean = true`, `consumerNumber: String = ""`, `meterHolderName: String = ""`, `sameAsBeneficiary: Boolean = true`, `relationship: String = ""`, `isHolderAlive: Boolean = true`, `isPoleNearbyForNewConn: Boolean? = null`, `interestedInNewConnection: Boolean? = null`

- **`SiteSurveyDetails`**:
  `roofType: RoofType? = null`, `hasSolarPanelSpace100SqFt: Boolean? = null`, `generationMeterLocation: String = ""`, `hasEarthingSpace: Boolean? = null`, `noteCamRoofBeforePhotoDone: Boolean = false`, `noteCamRoofBeforePhotoUri: String? = null`, `noteCamRoofBeforePhotoKb: Int? = null`, `noteCamRoofGeoInfo: String = ""`, `noteCamEarthingWithOwnerDone: Boolean = false`, `noteCamEarthingWithOwnerPhotoUri: String? = null`, `noteCamEarthingWithOwnerPhotoKb: Int? = null`, `noteCamEarthingGeoInfo: String = ""`, `noteCamRoofR2Key: String? = null`, `noteCamEarthingR2Key: String? = null`

- **`DocumentComplianceChecks`**:
  `lightBillIsResidential: Boolean = false`, `panNameAndDobMatchAadhaar: Boolean = false`, `bankIsNationalizedNotAdcc: Boolean = false`, `singleMobileLinkedAllDocs: Boolean = false`, `aadhaarSeededToBank: Boolean = false`

- **`MaterialAndOfficeDetails`**:
  `documentsCompleteStatus: Boolean = true`, `earthWireMeters: String = "20 मीटर"`, `acCableMeters: String = "10 मीटर"`, `pipeMeters: String = "5 पाईप"`, `customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `surveyorName: String = "रमेश कुलकर्णी"`, `surveyorSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`

- **`DocumentEntry`**:
  `type: DocumentType`, `printedName: String = ""`, `photoUri: String? = null`, `photoSizeKb: Int? = null`, `isSizeValid: Boolean = true`, `legibility: DocumentLegibility = DocumentLegibility.MISSING`, `matchResult: NameMatchResult = NameMatchResult.NOT_ENTERED`, `queryNote: String = ""`, `r2ObjectKey: String? = null`

- **`ChecklistAnswers`**:
  `gharkulAndMeterSamePerson: Boolean = true`, `allDocsHaveSameName: Boolean = true`, `aadhaarNameChangeNeeded: Boolean = false`, `lightBillNameChangeNeeded: Boolean = false`, `panNameChangeNeeded: Boolean = false`, `passbookNameChangeNeeded: Boolean = false`, `meterOnFamilyMemberName: Boolean = false`, `noElectricityConnection: Boolean = false`

- **`PendingActionItem`**:
  `titleMr: String`, `responsibleMr: String`, `isCompanyAction: Boolean`

- **`District` / `Taluka` / `Village`**:
  `District(id: String, nameMr: String, nameEn: String, talukas: List<Taluka>)`
  `Taluka(id: String, nameMr: String, nameEn: String, villages: List<Village>)`
  `Village(id: String, nameMr: String, nameEn: String)`

- **`ExecutiveUser`**:
  `nameMr: String`, `firstNameMr: String`, `email: String`, `mobile: String`, `roleMr: String`, `id: String = ""`, `authUserId: String = ""`, `role: String = "SURVEY_EXECUTIVE"`, `assignedDistrict: String = ""`, `assignedTaluka: String = ""`, `assignedVillage: String = ""`, `isActive: Boolean = true`

### 5.2 Installation Models (`com.example.model.InstallationModels.kt`)

- **`InstallationReport`**:
  `id: String`, `linkedSurveyId: String? = null`, `villageName: String = ""`, `consumerNo: String = ""`, `installationDate: String = defaultInstallationDate()`, `consumerName: String = ""`, `mobileNo: String = ""`, `systemCapacity: String = "1 KW"`, `systemType: String = "ONGRID"`, `products: List<InstallationProductRow> = defaultInstallationProductRows()`, `installerName: String = ""`, `installerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `supervisorName: String = ""`, `managerName: String = ""`, `customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `inverterSerial: SerialVerificationField = SerialVerificationField()`, `generationMeterSerial: SerialVerificationField = SerialVerificationField()`, `panelSerials: List<PanelSerialEntry> = defaultPanelSerialEntries()`, `afterInstallationPhotos: List<InstallationPhotoItem> = defaultInstallationPhotos()`, `updatedAtMillis: Long = System.currentTimeMillis()`, `installerExecutiveId: String? = null`

- **`InstallationProductRow`**:
  `rowId: String`, `srNoDisplay: String`, `productModel: String`, `isSubRow: Boolean = false`, `isQtyRequired: Boolean = false`, `company: String = ""`, `size: String = ""`, `capacity: String = ""`, `qty: String = ""`

- **`SerialVerificationField`**:
  `primaryValue: String = ""`, `confirmValue: String = ""` (computed: `trimmedPrimary`, `trimmedConfirm`, `isFilled`, `isMatch`, `hasMismatch`)

- **`PanelSerialEntry`**:
  `id: String`, `serial: SerialVerificationField = SerialVerificationField()`

- **`InstallationPhotoItem`**:
  `id: String`, `caption: String = "Solar Site Photo (After Installation)"`, `photoUri: String? = null`, `photoSizeKb: Int? = null`, `geoInfo: String = ""`, `isCameraGeoStamped: Boolean = false`, `r2ObjectKey: String? = null`

### 5.3 Remote & State Models

- **`SupabaseSettings`** (`SupabaseConfig.kt`): `url: String`, `anonKey: String` (`isConfigured`, `normalizedUrl`)
- **`PersistedSupabaseSession`** (`SupabaseSessionStore.kt`): `accessToken: String`, `refreshToken: String`, `loginRole: LoginRole`, `executive: ExecutiveUser`
- **`CustomVillageRecord`** (`SupabaseMappers.kt`): `id`, `districtId`, `districtNameMr`, `talukaId`, `talukaNameMr`, `villageNameMr`, `villageNameEn`
- **`R2UploadResult`** (`CloudflareR2Uploader.kt`): `publicUrl: String`, `objectKey: String`
- **`CloudSyncSnapshot`** (`SupabaseSurveyRepository.kt`): `customVillages`, `surveys`, `installationReports`
- **`AddEditWizardState`**, **`InstallationFormState`**, **`SurveyAppUiState`** (`SurveyAppViewModel.kt`): include cloud progress flags `isCloudConfigured`, `isAuthenticating`, `isSyncing`, `isSavingSurvey`, `isSavingInstallation`.

---

## 6. Backend Calls, Tables & Storage Buckets

### 6.1 Supabase PostgreSQL Tables (`/supabase/schema.sql`)

| Table | Primary Key & Foreign Keys | Purpose | JSONB Columns |
|---|---|---|---|
| `public.executives` | `id uuid PK`, `auth_user_id uuid UNIQUE FK -> auth.users(id)` | Role-gated profiles (`SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION`, `ADMIN`), assigned location, `is_active`, `last_login_at` | None |
| `public.custom_villages` | `id text PK`, `created_by uuid FK -> executives(id)` | Custom villages/wadis added by field executives or web admin | None |
| `public.customer_surveys` | `id text PK`, `executive_id uuid FK -> executives(id)` | Full Marathi survey record, meter info, site survey, NoteCam R2 URLs/keys, compliance checks, material defaults, categorization, and Web `gov_form_completed` / `gov_form_completed_at` workflow flags | `checklist_answers`, `matched_categories`, `pending_actions`, `signatures` (`{"customer": [...], "surveyor": [...]}`) |
| `public.survey_documents` | `id text PK`, `survey_id text FK -> customer_surveys(id) ON DELETE CASCADE`, `UNIQUE (survey_id, doc_type)` | Up to 9 document entries per survey, with `printed_name`, `legibility`, `match_result`, `query_note`, Cloudflare R2 `photo_url` & `r2_object_key` | None |
| `public.installation_reports` | `id text PK`, `linked_survey_id text FK -> customer_surveys(id) ON DELETE SET NULL`, `installer_executive_id uuid FK -> executives(id)` | English 1 KW ONGRID Installation Completion Certificate header, office sign-off names, inverter & generation meter serial verification pairs | `products` (23 material rows), `signatures` (`{"installer": [...], "customer": [...]}`), `panel_serial_numbers` (`[{id, primaryValue, confirmValue}]`) |
| `public.installation_photos` | `id text PK`, `report_id text FK -> installation_reports(id) ON DELETE CASCADE` | Dynamic after-installation NoteCam geo photos ordered by `slot_index`, with Cloudflare R2 `photo_url` & `r2_object_key`, `geo_info`, `is_camera_geo_stamped` | None |

### 6.2 Row-Level Security (RLS) Summary
- Helper functions: `public.is_active_executive()` and `public.is_office_admin()` (`SECURITY DEFINER`).
- `executives`: Authenticated users can `SELECT`/`UPDATE` their own row (`auth_user_id = auth.uid()`) or all rows if `ADMIN`; only `ADMIN` can `INSERT`.
- `custom_villages`, `customer_surveys`, `survey_documents`, `installation_reports`, `installation_photos`: `FOR ALL TO authenticated` gated by `public.is_active_executive()`.

### 6.3 Exact HTTP Endpoints Used by Android (`SupabaseApiClient` & `CloudflareR2Uploader`)
1. **Auth Sign-In:** `POST ${SUPABASE_URL}/auth/v1/token?grant_type=password`
2. **Auth Refresh:** `POST ${SUPABASE_URL}/auth/v1/token?grant_type=refresh_token`
3. **Auth Sign-Out:** `POST ${SUPABASE_URL}/auth/v1/logout`
4. **Executive Profile Lookup:** `GET ${SUPABASE_URL}/rest/v1/executives?auth_user_id=eq.<uid>&select=*`
5. **Update Last Login:** `PATCH ${SUPABASE_URL}/rest/v1/executives?id=eq.<exec_id>` (`{ "last_login_at": "<ISO-8601 UTC>" }`)
6. **Custom Villages Sync & Upsert:** `GET /rest/v1/custom_villages?select=*&order=created_at.asc`, `POST /rest/v1/custom_villages` (`Prefer: resolution=merge-duplicates,return=minimal`)
7. **Surveys Sync, Upsert & Delete:**
   - `GET /rest/v1/customer_surveys?select=*,survey_documents(*)&order=created_at.desc`
   - `POST /rest/v1/customer_surveys` (`Prefer: resolution=merge-duplicates,return=minimal`)
   - `POST /rest/v1/survey_documents?on_conflict=survey_id,doc_type` (`Prefer: resolution=merge-duplicates,return=minimal`)
   - `DELETE /rest/v1/customer_surveys?id=eq.<survey_id>` (cascades `survey_documents`)
8. **Installation Reports Sync & Upsert:**
   - `GET /rest/v1/installation_reports?select=*,installation_photos(*)&order=updated_at_millis.desc`
   - `POST /rest/v1/installation_reports` (`Prefer: resolution=merge-duplicates,return=minimal`)
   - `DELETE /rest/v1/installation_photos?report_id=eq.<report_id>` followed by `POST /rest/v1/installation_photos`
9. **Cloudflare R2 Presign & Delete:**
   - `POST ${SUPABASE_URL}/functions/v1/r2-presign` (`action: "presign"` -> returns `upload_url`, `public_url`, `object_key`)
   - `PUT <upload_url>` (direct binary `image/jpeg` upload to Cloudflare R2 bucket)
   - `POST ${SUPABASE_URL}/functions/v1/r2-presign` (`action: "delete"`, `object_keys: [...]`)

### 6.4 Local Storage & System Services
- **Session Storage:** Jetpack DataStore (`gharkul_supabase_session`) stores `access_token`, `refresh_token`, `login_role`, and `executive_json`.
- **Static Asset Read:** `LocationRepository` loads base districts/talukas/villages from `assets/locations_mr.json`, merged with `custom_villages`.
- **Offline Fallback (`InMemorySurveyRepository`):** Active automatically when `SUPABASE_URL` / `SUPABASE_ANON_KEY` are placeholders (seeds 5 demo surveys `cust_1`..`cust_5`).
- **Android System Location / Geocoder:** `ImageCompressionUtil.fetchLiveLocationOutcome()` queries `LocationManager` (`GPS_PROVIDER`, `NETWORK_PROVIDER`, `PASSIVE_PROVIDER`) and `Geocoder(context, Locale("mr", "IN"))`.

---

## 7. Status Values & Enums

| Enum / Value List | Code Symbols & Display Labels |
|---|---|
| `LoginRole` | `SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION` |
| `executives.role` (DB) | `SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION`, `ADMIN` |
| `AppScreen` | `LOGIN`, `VILLAGE_SELECT`, `MAIN_APP`, `INSTALLATION_DASHBOARD`, `INSTALLATION_FORM` |
| `MainTab` | `HOME` (`"होम"`), `CUSTOMERS` (`"ग्राहक"`), `PROFILE` (`"प्रोफाइल"`) |
| `InstallationTab` | `HOME` (`"Home"`), `PROFILE` (`"Profile"`) |
| `CasteCategory` | `OPEN` (`"OPEN"`), `SC` (`"SC"`), `ST` (`"ST"`) |
| `HouseType` | `SINGLE` (`"1 मजली"`), `DUPLEX` (`"2 मजली"`) |
| `RoofType` | `RCC` (`"RCC (सिमेंट धाबा)"`), `TIN_SHEET` (`"पत्रा"`), `TILED` (`"कौल"`), `CEMENT_SHEET` (`"सिमेंट पत्रा"`), `UNDER_CONSTRUCTION` (`"बांधकाम सुरू आहे"`) |
| `SolarCapacity` | `KW_1` (`kw = 1`, `"1 kW"`) |
| `DocumentType` | `LIGHT_BILL` (`"लाईट बिल"`), `AADHAAR` (`"आधार कार्ड"`), `PAN` (`"पॅन कार्ड"`), `BANK_PASSBOOK` (`"बँक पासबुक किंवा कॅन्सल चेक"`), `OWNERSHIP_PROOF` (`"मालकी हक्काचा पुरावा"`), `PASSPORT_PHOTO` (`"पासपोर्ट साईज फोटो"`), `CASTE_CERTIFICATE` (`"जातीचा दाखला (SC / ST)"`), `RATION_CARD` (`"रेशन कार्ड"`), `DEATH_CERTIFICATE` (`"मृत्यू दाखला"`) |
| `ResponsibleParty` | `CUSTOMER` (`"ग्राहकाने बदल करून देणे"`), `COMPANY` (`"आम्ही बदल करून देऊ"`) |
| `DocumentLegibility` | `CLEAR` (`"बरोबर"`), `UNCLEAR` (`"अस्पष्ट"` — hidden in UI), `MISSING` (`"उपलब्ध नाही"`) |
| `NameMatchResult` | `MATCH` (`"नाव जुळले"`), `CLOSE_MATCH` (`"जवळपास जुळते (तपासा)"`), `MISMATCH` (`"नाव जुळत नाही"`), `NOT_ENTERED` (`"नाव प्रविष्ट नाही"`) |
| `SurveyCategory` | `CATEGORY_1` (`1`, `"श्रेणी 1 · सर्व नाव समान"`), `CATEGORY_2` (`2`, `"श्रेणी 2 · नावामध्ये बदल"`), `CATEGORY_3` (`3`, `"श्रेणी 3 · मीटर दुसऱ्याच्या नावे"`), `CATEGORY_4` (`4`, `"श्रेणी 4 · लाईट कनेक्शन नाही"`) |
| `OverallStatus` | `READY` (`labelMr = "तयार"`, `chipLabelMr = "तयार"`), `PENDING_CUSTOMER` (`labelMr = "प्रलंबित · ग्राहक कार्यवाही"`, `chipLabelMr = "अपूर्ण"`), `PENDING_COMPANY` (`labelMr = "प्रलंबित · कंपनी कार्यवाही"`, `chipLabelMr = "अपूर्ण"`) |
| `CustomerStatusFilter` | `ALL` (`"सर्व"`), `READY` (`"तयार"`), `PENDING_CUSTOMER` (`"ग्राहक बाकी"`) *(note: `PENDING_COMPANY` has no dedicated filter chip)* |
| `InstallationQueueFilter` | `PENDING` (`"इन्स्टॉलेशन बाकी ग्राहक"`), `COMPLETED` (`"माझे पूर्ण इन्स्टॉलेशन"`) |
| `meterInfo.relationship` | `"पती"`, `"पत्नी"`, `"वडील"`, `"आई"`, `"मुलगा"`, `"भाऊ"` |

---

## 8. Inconsistencies & Implementation Notes

1. **`overrideCategory` / `overrideReason` / `executiveRemark` in Survey Wizard:**
   - Defined in `AddEditWizardState`, `CustomerSurvey`, and PostgreSQL (`override_reason`, `executive_remark`), but Step 3 UI does not render inputs for them; `saveWizardSurvey()` sets `finalCategory = w.suggestedCategory`.
2. **Mandatory (`*`) Labels vs. Enforced Validation:**
   - `* लाईट बिलावरील नाव` (`meterHolderName` when `sameAsBeneficiary == true`) shows `*` in Step 1, but `validateStep1()` only requires it when `!sameAsBeneficiary`.
   - Step 2 document cards display `*` visually, yet Step 2 `"पुढे: तपासणी"` performs no blocking validation so incomplete surveys can still be saved as pending.
   - Step 3 survey signatures are optional on save, whereas the Installation Certificate requires both Installer and Customer signatures.
3. **Consumer Number Validation Difference:**
   - Survey Wizard allows blank `consumerNumber`, and if entered enforces exactly 12 digits.
   - Installation Certificate requires `consumerNo` to be non-blank with any length/characters.
4. **GPS Source Difference:**
   - Step 1 optional house GPS (`gpsLocation`) populates district coordinates via `captureSampleGpsForSelectedDistrict()`.
   - Step 2 NoteCam and Installation Step 3 NoteCam use real device GPS via `LocationManager` + `Geocoder`.
5. **Removed Photo R2 Cleanup on Edit:**
   - Deleting a whole survey (`confirmDeleteCustomer()`) deletes both local cache files and Cloudflare R2 objects via `r2-presign` (`action: "delete"`). Replacing/clearing a single photo during Edit does not delete the previous R2 object automatically.
6. **Unused / Legacy Elements:**
   - `DocumentLegibility.UNCLEAR` and `SurveyAppUiState.reportBRows` exist in code/schema but are not rendered on Android screens.
   - `InMemorySurveyRepository.installationExecutive.mobile` uses an 11-digit demo number (`"91300028122"`).
