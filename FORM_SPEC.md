# Application Form & Data Specification (`FORM_SPEC.md`)

> **Important Backend Note:** Per `app/build.gradle.kts`, `SurveyRepository.kt`, and `LocationRepository.kt`, this app has **no remote database, no Supabase/REST API, no Firebase Firestore/Auth/Storage, and no Room/SQLite persistence**. All database/Firebase/network libraries in `app/build.gradle.kts` are commented out. Every record is stored **in-memory** during the app session (`InMemorySurveyRepository` and `LocationRepository`), and captured photos/PDFs are stored locally under `context.cacheDir`. Therefore, **"Database column or API/Supabase field"** lists `None (in-memory only — no DB/API/Supabase backend)` along with the exact Kotlin property path where the value is kept in memory.

---

## 1. Screen Flow & "Next" Button Checks

The app supports two login roles from `LoginScreen` (`AppScreen.LOGIN`):
- **Role 1 (`LoginRole.SURVEY_EXECUTIVE`)** — Marathi Gharakul Solar Survey workflow
- **Role 2 (`LoginRole.SOLAR_INSTALLATION`)** — English Reflection Media 1 KW ONGRID Installation Certificate workflow

### 1.1 Screen Order

#### A. Survey Executive Flow (`LoginRole.SURVEY_EXECUTIVE`)
1. `LoginScreen` (`AppScreen.LOGIN`)
   - Role selector (`SURVEY_EXECUTIVE` / `SOLAR_INSTALLATION`), email, password.
   - **Action button (`"लॉगिन करा"` -> `submitLogin()`):**
     - Checks `loginEmail` is non-blank AND contains `"@"`; otherwise shows `"कृपया वैध ईमेल पत्ता प्रविष्ट करा."`.
     - Checks `loginPassword.length >= 4`; otherwise shows `"कृपया किमान ४ अंकी पासवर्ड टाका."`.
     - On pass: navigates to `AppScreen.VILLAGE_SELECT`.
2. `VillageSelectionScreen` (`AppScreen.VILLAGE_SELECT`)
   - District, Taluka, and Village bottom-sheet selectors (`assets/locations_mr.json`), plus optional search (Taluka & Village only) and custom village/wadi creation.
   - **Action button (`"ग्राहक यादी पहा"` -> `proceedToDashboard()`):**
     - Button is enabled only when `selectedVillage != null`.
     - Checks `selectedVillage != null`; navigates to `AppScreen.MAIN_APP` (`MainTab.HOME`).
3. `MAIN_APP` (`AppScreen.MAIN_APP`) — 3 bottom tabs + centered `+ नवीन` button (`GharkulBottomBar`):
   - `MainTab.HOME` (`HomeDashboardScreen`): KPI summary (`stats`), search (`homeSearchQuery`), recent surveys list.
   - `MainTab.CUSTOMERS` (`CustomerListScreen`): search (`listSearchQuery`), status filter (`CustomerStatusFilter`), category filter (`SurveyCategory?`), survey cards with delete action (`DeleteCustomerConfirmationDialog`).
   - `CustomerDetailSheet` (ModalBottomSheet opened when tapping a customer card): displays details, Edit, Delete, and Share dialog (`मेसेज पाठवा / फॉर्म शेअर करा`: option 1 WhatsApp/text message enabled only when `finalCategory != CATEGORY_1`; option 2 Complete Form PDF via `SurveyPdfGenerator`).
   - `MainTab.PROFILE` (`ProfileScreen`): executive info, change village button (`AppScreen.VILLAGE_SELECT`), logout button (`AppScreen.LOGIN`).
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
       - On pass: saves `CustomerSurvey` to `InMemorySurveyRepository` and closes wizard back to `MAIN_APP`.

#### B. Solar Installation Flow (`LoginRole.SOLAR_INSTALLATION`)
1. `LoginScreen` (`AppScreen.LOGIN`, role `SOLAR_INSTALLATION`)
   - **Action button (`"Sign In as Solar Installer"` -> `submitLogin()`):**
     - Checks `loginEmail` non-blank and contains `"@"`; otherwise `"Please enter a valid email address."`.
     - Checks `loginPassword.length >= 4`; otherwise `"Please enter at least a 4-character password."`.
     - On pass: navigates to `AppScreen.INSTALLATION_DASHBOARD` (`InstallationTab.HOME`).
2. `InstallationDashboardScreen` (`AppScreen.INSTALLATION_DASHBOARD`) — 3-item bottom bar (`InstallationBottomBar`):
   - `InstallationTab.HOME`: KPI cards (`Approved Ready`, `Completed`, `Cat. 1 Total`), `installationSearchQuery`, `installationFilter`, Completed Certificates list (Edit / Share PDF), and Approved Category 1 Pending Customers list (`CATEGORY_1` with `OverallStatus.READY`).
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
   - **Step 3 (`NoteCam`) -> Submit button (`"Submit Certificate"` -> `saveInstallationReport()`):**
     - Re-runs `validateInstallationPart1(form)` (jumps to page 1 on failure) and `validateInstallationPart2(form)` (jumps to page 2 on failure).
     - Step 3 photos (`afterInstallationPhotos`) are optional.
     - On pass: saves `InstallationReport` in `InMemorySurveyRepository`, returns to `INSTALLATION_DASHBOARD`, and opens `ShareInstallationPdfOptionDialog` (`With Images` / `Without Images`).

---

## 2. Screen-by-Screen Field Specifications

### 2.1 `LoginScreen` (`AppScreen.LOGIN`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.loginRole` (`selectedRole`) | None (in-memory only — `SurveyAppUiState.loginRole`) | `1. Executive Login` (`घरकुल सर्व्हे`) / `2. Solar Installation` (`Completion Form`) | radio (role card toggle) | `SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION` | yes | Always one selected | `LoginRole.SURVEY_EXECUTIVE` | Always visible |
| `uiState.loginEmail` (`email`) | None (in-memory only — `SurveyAppUiState.loginEmail`) | Role 1: `ईमेल` / Role 2: `Email Address` | text (`KeyboardType.Email`) | Any string | yes | Non-blank and contains `"@"` (`!email.isBlank() && email.contains("@")`) | `"ramesh.k@gharkulsolar.in"` (switches to `"installer@reflectionmedias.in"` when selecting `SOLAR_INSTALLATION`) | Always visible |
| `uiState.loginPassword` (`password`) | None (in-memory only — `SurveyAppUiState.loginPassword`) | Role 1: `पासवर्ड` / Role 2: `Password` | text (`KeyboardType.Password`, masked unless eye icon toggled) | Any string | yes | `password.length >= 4` | `"123456"` | Always visible |

---

### 2.2 `VillageSelectionScreen` (`AppScreen.VILLAGE_SELECT`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.selectedDistrict` | None (in-memory only — `SurveyAppUiState.selectedDistrict`) | `जिल्हा` (Sheet title: `जिल्हा निवडा`) | dropdown (ModalBottomSheet list) | Districts loaded from `assets/locations_mr.json`: `ahilyanagar` (`अहिल्यानगर`), `sambhajinagar` (`छत्रपती संभाजीनगर`) | yes | Non-null before proceeding | First district in `locations_mr.json` (`ahilyanagar` / `अहिल्यानगर`) | Always visible |
| `uiState.selectedTaluka` | None (in-memory only — `SurveyAppUiState.selectedTaluka`) | `तालुका` (Sheet title: `तालुका निवडा (<district>)`) | dropdown (ModalBottomSheet list) | Talukas under `selectedDistrict.talukas` in `assets/locations_mr.json` (14 in Ahilyanagar, 9 in Sambhajinagar) | yes | Non-null before proceeding | `taluka.id == "nagar"` (`नगर`) else first taluka of `selectedDistrict` | Always visible |
| `uiState.selectedVillage` | None (in-memory only — `SurveyAppUiState.selectedVillage`) | `गाव` (Sheet title: `गाव निवडा (<taluka>)`) | dropdown (ModalBottomSheet list) | Villages under `selectedTaluka.villages` + any custom villages added in session | yes | `selectedVillage != null` required to enable `"ग्राहक यादी पहा"` button | `village.id == "ng_19"` (`हिवरे बाजार`) else first village of `selectedTaluka` | Always visible |
| `searchQuery` (local state) | None (UI filter state only, not saved) | Placeholder: `नाव शोधा (मराठी किंवा English)...` | text | Any string | no | Case-insensitive substring filter on `nameMr` / `nameEn` | `""` | Only when Taluka or Village picker sheet is open (`activeMode == TALUKA` or `VILLAGE`) |
| `customVillageInput` (local state -> `LocationRepository.addCustomVillage`) | None (in-memory only — `LocationRepository.cachedDistricts`) | `यादीत गाव / वाडी नसल्यास जोडा:` (Placeholder: `गावाचे नाव लिहा`, Button: `जोडा`) | text | Non-blank string | no | Button adds only if `customVillageInput.isNotBlank()`; trimmed; ID `"custom_${System.currentTimeMillis()}"` | `""` | Only when Village picker sheet is open (`activeMode == PickerMode.VILLAGE`) |

---

### 2.3 `HomeDashboardScreen` & `CustomerListScreen` (`AppScreen.MAIN_APP`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.homeSearchQuery` | None (in-memory filter — `SurveyAppUiState.homeSearchQuery`) | Placeholder: `नाव किंवा मोबाईल शोधा` | text | Any string | no | Filters `beneficiaryName`, `mobile`, `gharkulId` (case-insensitive) | `""` | `activeTab == MainTab.HOME` |
| `uiState.listSearchQuery` | None (in-memory filter — `SurveyAppUiState.listSearchQuery`) | Placeholder: `नाव, मोबाईल, घरकुल आयडी किंवा मीटर क्र. शोधा` | text | Any string | no | Filters `beneficiaryName`, `mobile`, `gharkulId`, `meterInfo.consumerNumber` (case-insensitive) | `""` | `activeTab == MainTab.CUSTOMERS` |
| `uiState.selectedStatusFilter` | None (in-memory filter — `SurveyAppUiState.selectedStatusFilter`) | Chips: `सर्व`, `तयार`, `ग्राहक बाकी` | radio (FilterChip row) | `ALL` (`सर्व`), `READY` (`तयार`), `PENDING_CUSTOMER` (`ग्राहक बाकी`) | no | Single selection | `CustomerStatusFilter.ALL` | `activeTab == MainTab.CUSTOMERS` |
| `uiState.selectedCategoryFilter` | None (in-memory filter — `SurveyAppUiState.selectedCategoryFilter`) | Chips: `सर्व श्रेणी`, `श्रेणी 1`, `श्रेणी 2`, `श्रेणी 3`, `श्रेणी 4` | radio (toggleable FilterChip row) | `null` (`सर्व श्रेणी`), `CATEGORY_1`, `CATEGORY_2`, `CATEGORY_3`, `CATEGORY_4` | no | Tapping active chip resets to `null` | `null` | `activeTab == MainTab.CUSTOMERS` |

---

### 2.4 `AddSurveyWizardScreen` — Step 1 (`WizardStep1BasicAndMeter`)

Note: District, Taluka, and Village (`districtNameMr`, `talukaNameMr`, `villageNameMr`, `villageId`) are inherited from `VillageSelectionScreen` and saved on `CustomerSurvey`.

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.surveyDate` | None (in-memory only — `CustomerSurvey.surveyDate`) | `* सर्व्हे दिनांक` | date (`DatePickerDialog`) | Date formatted `"dd / MM / yyyy"` via `Locale.getDefault()` | yes | Must not be blank (`surveyDate.isBlank()` checked) | Today's date (`defaultTodayDateString()`) | Always visible |
| `wizard.casteCategory` | None (in-memory only — `CustomerSurvey.casteCategory`) | `* प्रवर्ग` | radio (segmented buttons) | `OPEN` (`"OPEN"`), `SC` (`"SC"`), `ST` (`"ST"`) | yes | Always non-null enum | `CasteCategory.OPEN` | Always visible |
| `wizard.capacity` | None (in-memory only — `CustomerSurvey.capacity`) | `* सोलर क्षमता` (Displayed text: `1 kW (घरकुल योजना)`) | read-only card (not user-editable) | `SolarCapacity.KW_1` (`1 kW`) | yes | Fixed to `KW_1`; `saveWizardSurvey()` hardcodes `SolarCapacity.KW_1` | `SolarCapacity.KW_1` | Always visible |
| `wizard.beneficiaryName` | None (in-memory only — `CustomerSurvey.beneficiaryName`) | `* घरकुलावरील नाव (मालकाचे नाव)` | text | Any string | yes | Must not be blank (`beneficiaryName.isBlank()`); trimmed on save | `""` | Always visible |
| `wizard.mobile` | None (in-memory only — `CustomerSurvey.mobile`) | `* मोबाईल नं. (आधार व बँकेशी लिंक असलेला)` | phone (`KeyboardType.Phone`) | Digits only (`0-9`) | yes | Input strips non-digits & caps at 10 chars; validation requires `mobile.filter { it.isDigit() }.length == 10` | `""` | Always visible |
| `wizard.emailId` | None (in-memory only — `CustomerSurvey.emailId`) | `* ई-मेल आयडी (E-mail ID)` | text (`KeyboardType.Email`) | String without spaces (`it.replace(" ", "")`) | yes | Non-blank and matches `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$` | `""` | Always visible |
| `wizard.addressLandmark` | None (in-memory only — `CustomerSurvey.addressLandmark`) | `पत्ता / जवळची खूण` | text | Any string | no | None; trimmed on save | `""` | Always visible |
| `wizard.gharkulId` | None (in-memory only — `CustomerSurvey.gharkulId`) | `घरकुल आयडी / मंजुरी क्र.` | text | Any string | no | Non-blocking duplicate warning if matches another survey (`equals(..., ignoreCase = true)`); trimmed on save | `""` | Always visible |
| `wizard.aadhaarLast4` | None (in-memory only — `CustomerSurvey.aadhaarLast4`) | `आधार शेवटचे ४ अंक` | number (`KeyboardType.Number`) | Digits only (`0-9`) | no | Input strips non-digits & caps at 4 digits; non-blocking duplicate warning when `length == 4` matches another survey | `""` | Always visible |
| `wizard.gpsLocation` | None (in-memory only — `CustomerSurvey.gpsLocation`) | `घराचे GPS लोकेशन (पर्यायी)` | button (populates district demo coords) | `"19.8762° N, 75.3433° E"` (if `selectedDistrict.id == "sambhajinagar"`) or `"19.0948° N, 74.7480° E"` (otherwise) | no | None | `null` (Display: `"अद्याप नोंदवलेले नाही"`) | Always visible |
| `wizard.meterInfo.hasMeter` | None (in-memory only — `CustomerSurvey.meterInfo.hasMeter`) | `* लाईट मीटर उपलब्ध आहे का?` | YES/NO (`YesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Non-null boolean; setting `false` forces `sameAsBeneficiary = false` | `true` (`होय`) | Always visible |
| `wizard.meterInfo.isPoleNearbyForNewConn` | None (in-memory only — `CustomerSurvey.meterInfo.isPoleNearbyForNewConn`) | `* नवीन कनेक्शन घेण्यासाठी पोल जवळ आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` (`== null` blocks Step 1 Next) | `null` | Always visible |
| `wizard.meterInfo.interestedInNewConnection` | None (in-memory only — `CustomerSurvey.meterInfo.interestedInNewConnection`) | `* नवीन कनेक्शन घेण्यासाठी इच्छुक आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` (`== null` blocks Step 1 Next) | `null` | Always visible |
| `wizard.meterInfo.consumerNumber` | None (in-memory only — `CustomerSurvey.meterInfo.consumerNumber`) | `ग्राहक क्रमांक (Consumer No.)` | number (`KeyboardType.Number`) | Digits only (`0-9`) | no | Input strips non-digits & caps at 12 digits; if non-empty, `trim().length == 12` required; non-blocking duplicate warning if matches another survey | `""` | Only when `meterInfo.hasMeter == true` |
| `wizard.meterInfo.sameAsBeneficiary` | None (in-memory only — `CustomerSurvey.meterInfo.sameAsBeneficiary`) | `* मीटर लाभार्थ्याच्या नावावर आहे का?` | YES/NO (`YesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Setting `true` copies `wizard.beneficiaryName` into `meterHolderName` | `true` (`होय`) | Only when `meterInfo.hasMeter == true` |
| `wizard.meterInfo.meterHolderName` (when `sameAsBeneficiary == true`) | None (in-memory only — `CustomerSurvey.meterInfo.meterHolderName`) | `* लाईट बिलावरील नाव` | text | Any string | no (UI shows `*`, NOT checked in `validateStep1` when `sameAsBeneficiary == true`) | Displayed value falls back to `wizard.beneficiaryName` if empty | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == true` |
| `wizard.meterInfo.meterHolderName` (when `sameAsBeneficiary == false`) | None (in-memory only — `CustomerSurvey.meterInfo.meterHolderName`) | `* लाईट बिलावरील नाव (मीटर धारक)` | text | Any string | yes | Must not be blank (`!sameAsBeneficiary && meterHolderName.isBlank()` blocks Step 1 Next) | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.meterInfo.relationship` | None (in-memory only — `CustomerSurvey.meterInfo.relationship`) | `लाभार्थ्याशी नाते` | radio (`FilterChip` row) | `"पती"`, `"पत्नी"`, `"वडील"`, `"आई"`, `"मुलगा"`, `"भाऊ"` | no | None | `""` | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.meterInfo.isHolderAlive` | None (in-memory only — `CustomerSurvey.meterInfo.isHolderAlive`) | `मीटर धारक हयात आहेत का?` | YES/NO (`YesNoPillGroup` custom labels) | `true` (`हयात`), `false` (`मृत`) | no | Non-null boolean; if `false`, requires `DEATH_CERTIFICATE` in Step 2 | `true` (`हयात`) | Only when `meterInfo.hasMeter == true && meterInfo.sameAsBeneficiary == false` |
| `wizard.siteSurveyDetails.roofType` | None (in-memory only — `CustomerSurvey.siteSurveyDetails.roofType`) | `* छताचा प्रकार` | radio (`FilterChip` FlowRow) | `RCC` (`"RCC (सिमेंट धाबा)"`), `TIN_SHEET` (`"पत्रा"`), `TILED` (`"कौल"`), `CEMENT_SHEET` (`"सिमेंट पत्रा"`), `UNDER_CONSTRUCTION` (`"बांधकाम सुरू आहे"`) | yes | Must not be `null` | `null` | Always visible |
| `wizard.siteSurveyDetails.hasSolarPanelSpace100SqFt` | None (in-memory only — `CustomerSurvey.siteSurveyDetails.hasSolarPanelSpace100SqFt`) | `* सोलर पॅनेलसाठी जागा उपलब्ध आहे का? (किमान १०० चौ.फूट)` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` | `null` | Always visible |
| `wizard.siteSurveyDetails.generationMeterLocation` | None (in-memory only — `CustomerSurvey.siteSurveyDetails.generationMeterLocation`) | `* जनरेशन मीटर बसवण्याची जागा` | text | Any string | yes | Must not be blank (`generationMeterLocation.isBlank()` blocks Step 1 Next) | `""` | Always visible |
| `wizard.siteSurveyDetails.hasEarthingSpace` | None (in-memory only — `CustomerSurvey.siteSurveyDetails.hasEarthingSpace`) | `* अर्थिंगसाठी (Earthing) जागा उपलब्ध आहे का?` | YES/NO (`NullableYesNoPillGroup`) | `true` (`होय`), `false` (`नाही`) | yes | Must not be `null` | `null` | Always visible |
| `wizard.houseType` | None (in-memory only — `CustomerSurvey.houseType`) | `* छत किंवा घर एकच मजली आहे का?` | radio (button pair) | `SINGLE` (`"1 मजली"`), `DUPLEX` (`"2 मजली"`) | yes | Always non-null enum | `HouseType.SINGLE` | Always visible |

---

### 2.5 `AddSurveyWizardScreen` — Step 2 (`WizardStep2Documents`)

#### A. Document Compliance Checkboxes (`महत्त्वाच्या सूचना व तपासणी`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.documentComplianceChecks.lightBillIsResidential` | None (in-memory only — `CustomerSurvey.documentComplianceChecks.lightBillIsResidential`) | `लाईट बिल घरगुती (Residential) आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.panNameAndDobMatchAadhaar` | None (in-memory only — `CustomerSurvey.documentComplianceChecks.panNameAndDobMatchAadhaar`) | `आधार आणि पॅनवरील नाव व जन्मतारीख सारखीच आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.bankIsNationalizedNotAdcc` | None (in-memory only — `CustomerSurvey.documentComplianceChecks.bankIsNationalizedNotAdcc`) | `राष्ट्रीयकृत बँक खाते आहे (ADCC बँक नाही; खाते क्र. व IFSC स्पष्ट)` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.singleMobileLinkedAllDocs` | None (in-memory only — `CustomerSurvey.documentComplianceChecks.singleMobileLinkedAllDocs`) | `आधार, पॅन, लाईट बिल व बँक खात्याला एकच चालू मोबाईल लिंक आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |
| `wizard.documentComplianceChecks.aadhaarSeededToBank` | None (in-memory only — `CustomerSurvey.documentComplianceChecks.aadhaarSeededToBank`) | `बँक खाते आधारशी लिंक (Aadhaar Seeding) आहे` | checkbox (boolean) | `true`, `false` | no | None | `false` | Always visible on Step 2 |

#### B. Document Capture Cards (`requiredList = SurveyRuleEngine.requiredDocumentsFor(meterInfo, casteCategory)`)

For each visible `docType` in `requiredList` (up to 9 types — see Section 3 for visibility rules), the card renders 4 inputs:

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.documents[docType].printedName` | None (in-memory only — `CustomerSurvey.documents[docType].printedName`) | Placeholder: `कागदपत्रावरील नाव लिहा` (on `<docType.labelMr>` card) | text | Any string | no (Step 2 Next has no validation) | Live name-matching via `SurveyRuleEngine.matchName(...)`; sets `matchResult` | `""` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].photoUri` (plus `photoSizeKb`, `isSizeValid`) | None (in-memory only — `CustomerSurvey.documents[docType].photoUri`) | Camera button / sheet options: `कॅमेराने फोटो काढा`, `गॅलरीतून निवडा`, `नमुना फोटो जोडा` | photo/file (`TakePicture`, `PickVisualMedia.ImageOnly`, or sample string `"captured://sample_${docType.name}"`) | Image URI or `"captured://..."` | no (absence keeps `legibility = MISSING` & adds pending action on Category 1/3) | **No file size limit**; original bytes saved as `.jpg`; `isSizeValid` always `true`; sets `legibility = DocumentLegibility.CLEAR` on upload | `null` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].legibility` (toggle button) | None (in-memory only — `CustomerSurvey.documents[docType].legibility`) | `उपलब्ध नाही` (FilterChip) | toggle (`MISSING` / `CLEAR`) | `CLEAR` (`स्पष्ट`), `MISSING` (`उपलब्ध नाही`) (`UNCLEAR` exists in enum but cannot be selected in UI) | no | Selecting toggles between `MISSING` and `CLEAR` | `DocumentLegibility.MISSING` | Card visible per `requiredDocumentsFor()` |
| `wizard.documents[docType].queryNote` | None (in-memory only — `CustomerSurvey.documents[docType].queryNote`) | Toggle chip: `टीप (Note)` / `टीप (नोंदलेली)`; Field label: `कागदपत्राबाबत शंका / टीप` | text (2–3 lines) | Any string | no | None | `""` | Visible after tapping `"टीप (Note)"` chip or when `queryNote.isNotBlank()` |

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
| `wizard.siteSurveyDetails.noteCamRoofBeforePhotoUri` (+ `Done`, `Kb`, `GeoInfo`) | None (in-memory only — `CustomerSurvey.siteSurveyDetails.noteCamRoofBeforePhotoUri`) | `सोलर बसवण्याच्या आधीच्या छताचा फोटो` (Buttons: `कॅमेरा (Geo)`, `गॅलरी`) | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Image URI (`content://...`) | no | Camera path requires `CAMERA` + (`ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`) permissions and live device GPS enabled (otherwise opens `"Live GPS / लोकेशन आवश्यक आहे"` Settings dialog). Gallery path copies raw image without overlay or GPS check. | `null` (`Done = false`, `GeoInfo = ""`) | Always visible on Step 2 |
| `wizard.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri` (+ `Done`, `Kb`, `GeoInfo`) | None (in-memory only — `CustomerSurvey.siteSurveyDetails.noteCamEarthingWithOwnerPhotoUri`) | `Earthing Point चा फोटो (मालकासोबत)` (Buttons: `कॅमेरा (Geo)`, `गॅलरी`) | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Image URI (`content://...`) | no | Same as `noteCamRoofBeforePhotoUri` above. | `null` (`Done = false`, `GeoInfo = ""`) | Always visible on Step 2 |

---

### 2.6 `AddSurveyWizardScreen` — Step 3 (`WizardStep3ReviewAndResult`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `wizard.checklist.gharkulAndMeterSamePerson` | None (in-memory only — `CustomerSurvey.checklistAnswers.gharkulAndMeterSamePerson`) | `१. घरकुल व मीटर एकाच व्यक्तीच्या नावावर आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Derived automatically from Step 1/2 until manually edited | `true` (auto-derived via `deriveChecklist`) | Always visible on Step 3 |
| `wizard.checklist.allDocsHaveSameName` | None (in-memory only — `CustomerSurvey.checklistAnswers.allDocsHaveSameName`) | `१. बँक पासबुक, आधार कार्ड, पॅन कार्ड, लाईट बिल या सर्वांवर नाव समान आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Derived automatically from Step 1/2 until manually edited | `true` (auto-derived via `deriveChecklist`) | Always visible on Step 3 |
| `wizard.checklist.aadhaarNameChangeNeeded` | None (in-memory only — `CustomerSurvey.checklistAnswers.aadhaarNameChangeNeeded`) | `२. आधार कार्डवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.lightBillNameChangeNeeded` | None (in-memory only — `CustomerSurvey.checklistAnswers.lightBillNameChangeNeeded`) | `२. लाईट बिलावरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.panNameChangeNeeded` | None (in-memory only — `CustomerSurvey.checklistAnswers.panNameChangeNeeded`) | `२. पॅन कार्डवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.passbookNameChangeNeeded` | None (in-memory only — `CustomerSurvey.checklistAnswers.passbookNameChangeNeeded`) | `२. बँक पासबुकवरील नावामध्ये बदल आवश्यक आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.meterOnFamilyMemberName` | None (in-memory only — `CustomerSurvey.checklistAnswers.meterOnFamilyMemberName`) | `३. घरकुल नावावर आहे पण लाईट मीटर कनेक्शन घरातील दुसऱ्या व्यक्तीच्या नावे आहे का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.checklist.noElectricityConnection` | None (in-memory only — `CustomerSurvey.checklistAnswers.noElectricityConnection`) | `४. घरकुल नावावर आहे पण लाईट कनेक्शन नाही का?` | YES/NO | `true` (`होय`), `false` (`नाही`) | yes | Auto-derived or manually toggled | `false` (auto-derived) | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.documentsCompleteStatus` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.documentsCompleteStatus`) | `* कागदपत्रांची स्थिती` | radio (segmented buttons) | `true` (`पूर्ण`), `false` (`अपूर्ण`) | yes | Always non-null boolean; also auto-synced to `pending.isEmpty()` inside `recalculateWizardRules()` | `true` (`पूर्ण`) | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.earthWireMeters` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.earthWireMeters`) | `१६ Sq.mm. Aluminium (Earth Wire)` (Custom input label: `एकूण मीटर संख्या`, button: `+ कस्टम` / `जोडा`) | number (digits via custom drawer -> saved as `"<n> मीटर"`) | Positive integer > 0 (`1..9999`) or default `"20 मीटर"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled only when `toIntOrNull() > 0` | `"20 मीटर"` | Value always visible; custom input visible only when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.acCableMeters` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.acCableMeters`) | `AC 1.5 sq.mm 3 Core केबल` (Custom input label: `एकूण मीटर संख्या`, button: `+ कस्टम` / `जोडा`) | number (digits -> saved as `"<n> मीटर"`) | Positive integer > 0 (`1..9999`) or default `"10 मीटर"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled only when `toIntOrNull() > 0` | `"10 मीटर"` | Value always visible; custom input visible only when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.pipeMeters` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.pipeMeters`) | `पाईप (१ पाईप = ३० मीटर)` (Custom input label: `एकूण पाईप संख्या`, button: `+ कस्टम` / `जोडा`) | number (digits -> saved as `"<n> पाईप"`, displayed as `"<n> पाईप (<n*30> मीटर)"`) | Positive integer > 0 (`1..9999`) or default `"5 पाईप"` | yes | Digits-only, max 4 chars; `"जोडा"` enabled only when `toIntOrNull() > 0` | `"5 पाईप"` | Value always visible; custom input visible only when `+ कस्टम` toggled |
| `wizard.materialAndOfficeDetails.customerSignatureStrokes` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.customerSignatureStrokes`) | `ग्राहक / अर्जदाराची सही` | signature canvas (`List<List<Pair<Float, Float>>>`) | Finger-drawn strokes or empty list | no (not checked on Save) | None | `emptyList()` | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.surveyorName` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.surveyorName`) | `* सर्व्हे करणाऱ्या व्यक्तीचे नाव` | text | Any string | yes | Must not be blank (`surveyorName.isBlank()` blocks `"जतन करा"`) | `executive.nameMr` (`"रमेश कुलकर्णी"`) | Always visible on Step 3 |
| `wizard.materialAndOfficeDetails.surveyorSignatureStrokes` | None (in-memory only — `CustomerSurvey.materialAndOfficeDetails.surveyorSignatureStrokes`) | `सर्व्हे करणाऱ्या व्यक्तीची सही` | signature canvas (`List<List<Pair<Float, Float>>>`) | Finger-drawn strokes or empty list | no (not checked on Save) | None | `emptyList()` | Always visible on Step 3 |

*(Note: `wizard.overrideCategory`, `wizard.overrideReason`, and `wizard.executiveRemark` exist in `AddEditWizardState` and `CustomerSurvey` but have **no UI fields rendered** on Step 3.)*

---

### 2.7 `InstallationDashboardScreen` (`AppScreen.INSTALLATION_DASHBOARD`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `uiState.installationSearchQuery` | None (in-memory filter — `SurveyAppUiState.installationSearchQuery`) | Placeholder: `Search consumer name, consumer no., or mobile` | text | Any string | no | Filters `consumerName`/`beneficiaryName`, `consumerNo`/`consumerNumber`, `mobile`, `villageName` | `""` | `activeInstallationTab == HOME` |
| `uiState.installationFilter` | None (in-memory filter — `SurveyAppUiState.installationFilter`) | Chips: `All`, `Pending`, `Completed` | radio (FilterChip row) | `ALL` (`All`), `PENDING` (`Pending`), `COMPLETED` (`Completed`) | no | Single selection | `InstallationQueueFilter.ALL` | `activeInstallationTab == HOME` |

---

### 2.8 `InstallationFormScreen` — Step 1 (`Fill Form`)

*(All labels in this workflow are in **English**.)*

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `installationForm.consumerNo` | None (in-memory only — `InstallationReport.consumerNo`) | `Consumer No. *` | number (`KeyboardType.Number`) | Any non-blank string | yes | Must not be blank (`consumerNo.isBlank()` checked; no length restriction enforced) | `""` (pre-filled from `survey.meterInfo.consumerNumber` if started from Category 1 customer) | Step 1, Section A expanded (`consumerExpanded = true` by default) |
| `installationForm.installationDate` | None (in-memory only — `InstallationReport.installationDate`) | `Installation Date *` | date (text + `DatePickerDialog`) | Date string (picker formats `"dd / MM / yyyy"` using `Locale.ENGLISH`) | yes | Must not be blank | `defaultInstallationDate()` (today) | Step 1, Section A expanded |
| `installationForm.consumerName` | None (in-memory only — `InstallationReport.consumerName`) | `Consumer Name *` | text (`KeyboardCapitalization.Words`) | Any non-blank string | yes | Must not be blank; trimmed on save | `""` (or `survey.beneficiaryName`) | Step 1, Section A expanded |
| `installationForm.mobileNo` | None (in-memory only — `InstallationReport.mobileNo`) | `Mobile No. *` (full-width row) | phone (`KeyboardType.Phone`) | Digits | yes | `mobileNo.filter { it.isDigit() }.length == 10`; stores digits-only on save | `""` (or `survey.mobile`) | Step 1, Section A expanded |
| `installationForm.systemCapacity` | None (in-memory only — `InstallationReport.systemCapacity`) | `System Capacity *` | text | Any non-blank string | yes | Must not be blank (`validateInstallationPart1` checks both capacity and type) | `"1 KW"` | Step 1, Section A expanded |
| `installationForm.systemType` | None (in-memory only — `InstallationReport.systemType`) | `System Type *` | text | Any non-blank string | yes | Must not be blank | `"ONGRID"` | Step 1, Section A expanded |
| `installationForm.products[0..22].company` / `.size` / `.capacity` | None (in-memory only — `InstallationReport.products`) | `Company`, `Size`, `Capacity` for each of 23 rows (arranged in 2-column stacked grid: Row 1 `Company` \| `Size`, Row 2 `Capacity` \| `QTY`/`QTY *`) | text | Any string | no | Editable freely; not validated on Next/Save | `""` (all empty by default) | Step 1, Section B expanded (`materialsExpanded = true` by default) |
| `installationForm.products[0..22].qty` | None (in-memory only — `InstallationReport.products`) | `QTY *` (12 rows where `isQtyRequired == true`, with `* Required` badge) / `QTY` (11 optional rows) | text (2nd cell of Row 2 in 2x2 item grid) | Any string | yes for 12 rows (`isQtyRequired == true`); no for other 11 rows | When `isQtyRequired == true`, `qty.isBlank()` blocks Step 1 Next (`"Please enter QTY for <productModel>."`) | Pre-filled `defaultInstallationProductRows()` (see list below; unmentioned rows default to `""`) | Step 1, Section B expanded (`materialsExpanded = true` by default) |
| `installationForm.installerName` | None (in-memory only — `InstallationReport.installerName`) | `Installer Name *` | text (`KeyboardCapitalization.Words`) | Any non-blank string | yes | Must not be blank | `installationExecutive.nameMr` (`"Vikram Kulkarni"`) | Step 1, Section C expanded (`signOffExpanded = true` by default) |
| `installationForm.installerSignatureStrokes` | None (in-memory only — `InstallationReport.installerSignatureStrokes`) | `Installer Signature *` | signature canvas | Non-empty stroke list | yes | `installerSignatureStrokes.isEmpty()` blocks Step 1 Next | `emptyList()` | Step 1, Section C expanded |
| `installationForm.accountantName` | None (in-memory only — `InstallationReport.accountantName`) | `1) Accountant` (under `Office Verification (Optional)`) | text (`KeyboardCapitalization.Words`) | Any string | no | None | `""` | Step 1, Section C expanded |
| `installationForm.managerName` | None (in-memory only — `InstallationReport.managerName`) | `2) Manager` | text (`KeyboardCapitalization.Words`) | Any string | no | None | `""` | Step 1, Section C expanded |
| `installationForm.supervisorName` | None (in-memory only — `InstallationReport.supervisorName`) | `3) Supervisor` | text (`KeyboardCapitalization.Words`) | Any string | no | None | `""` | Step 1, Section C expanded |
| `installationForm.customerSignatureStrokes` | None (in-memory only — `InstallationReport.customerSignatureStrokes`) | `Customer Sign and Stamp *` | signature canvas | Non-empty stroke list | yes | `customerSignatureStrokes.isEmpty()` blocks Step 1 Next | `emptyList()` | Step 1, Section C expanded |

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
| `installationForm.inverterSerial.primaryValue` & `.confirmValue` | None (in-memory only — `InstallationReport.inverterSerial`) | `Invertor Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes | Both trimmed values non-empty and exact match (`trimmedPrimary == trimmedConfirm`) | `""` / `""` | Always visible on Step 2 |
| `installationForm.generationMeterSerial.primaryValue` & `.confirmValue` | None (in-memory only — `InstallationReport.generationMeterSerial`) | `Generation Meter Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes | Both trimmed values non-empty and exact match | `""` / `""` | Always visible on Step 2 |
| `installationForm.panelSerials[i].serial.primaryValue` & `.confirmValue` | None (in-memory only — `InstallationReport.panelSerials[i]`) | `Panel ${i+1} Serial Number *` (`1st Entry`, `2nd Entry (Confirm)`) | text (`KeyboardCapitalization.Characters`) | Any non-blank string | yes (minimum 2 panels; dynamic `+ Add Panel Serial Number` button adds more; delete button visible for index `>= 2` when `size > 2`) | `panelSerials.size >= 2`; every panel must have both trimmed fields non-empty and matching | 2 items (`panel_1`, `panel_2`) with `""` / `""` | Always visible on Step 2 |

---

### 2.10 `InstallationFormScreen` — Step 3 (`NoteCam`)

| Field name in code | Database column or API/Supabase field | Label shown on screen | Input type | Allowed values | Required? | Validation rules | Default value | Visible only when... |
|---|---|---|---|---|---|---|---|---|
| `installationForm.afterInstallationPhotos[i].caption` | None (in-memory only — `InstallationReport.afterInstallationPhotos[i].caption`) | `Photo Description / Label` (Dynamic cards + `Add More Photo` button; delete slot icon visible when `size > 1 && idx >= 1`) | text | Any string | no | None | Slot 1: `"Solar Site Photo (After Installation)"`; added slots: `"After Installation Photo ${nextIdx}"` | Always visible on Step 3 |
| `installationForm.afterInstallationPhotos[i].photoUri` (+ `photoSizeKb`, `geoInfo`, `isCameraGeoStamped`) | None (in-memory only — `InstallationReport.afterInstallationPhotos[i].photoUri`) | `NoteCam (Geo)` / `Gallery` | photo/file (`TakePicture` with live GPS overlay, or `PickVisualMedia.ImageOnly` unmodified) | Image URI (`content://...`) | no (photos optional on submit) | `NoteCam (Geo)` requires `CAMERA` + (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) and device GPS enabled (otherwise opens `"Enable GPS Location"` dialog). Gallery copies original bytes without overlay. | `null` | Always visible on Step 3 |

---

## 3. Document Uploads & Local Cache Storage

All uploads are stored **on-device in app cache only** (no remote storage bucket).

- **Directory:** `<context.cacheDir>/survey_photos/` (defined in `res/xml/file_paths.xml` as `<cache-path name="survey_photos" path="survey_photos/" />`)
- **Exposed URI Authority:** `content://${context.packageName}.fileprovider/survey_photos/<filename>` (`com.aistudio.gharkulsurvey.mhsxqr.fileprovider`)
- **Generated PDFs Directory:** `<context.cacheDir>/survey_pdfs/` (`<cache-path name="survey_pdfs" path="survey_pdfs/" />`):
  - Survey PDF: `gharkul_${safeName}_${timestamp}.pdf` (always embeds brand logo & all document + NoteCam photos at 2 cards/page)
  - Installation PDF: `installation_${modeTag}_${safeName}_${timestamp}.pdf` (`modeTag` = `"with_images"` or `"certificate"`)

| Slot / `DocumentType` | Allowed Source / File Type | Size Limit | Local File Path (`cacheDir`) | File Name Format | Pixel Modification / Overlay |
|---|---|---|---|---|---|
| `LIGHT_BILL` (`लाईट बिल`) | Camera (`TakePicture`) / Gallery (`PickVisualMedia.ImageOnly`) / Sample (`captured://sample_LIGHT_BILL`) | **None** (`isSizeValid` always `true`) | `survey_photos/` | `light_bill_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `AADHAAR` (`आधार कार्ड`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `aadhaar_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `PAN` (`पॅन कार्ड`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `pan_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `BANK_PASSBOOK` (`बँक पासबुक किंवा कॅन्सल चेक`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `bank_passbook_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `OWNERSHIP_PROOF` (`मालकी हक्काचा पुरावा`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `ownership_proof_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `PASSPORT_PHOTO` (`पासपोर्ट साईज फोटो`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `passport_photo_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `CASTE_CERTIFICATE` (`जातीचा दाखला (SC / ST)`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `caste_certificate_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `RATION_CARD` (`रेशन कार्ड`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `ration_card_<epoch_ms>.jpg` | Original bytes copied unmodified |
| `DEATH_CERTIFICATE` (`मृत्यू दाखला`) | Camera / Gallery / Sample | **None** | `survey_photos/` | `death_certificate_<epoch_ms>.jpg` | Original bytes copied unmodified |
| Survey NoteCam: `ROOF` (`NoteCamSlot.ROOF`) | Camera (`TakePicture` -> raw file + stamped copy) OR Gallery (`ImageOnly`) | **None** | `survey_photos/` | Raw: `notecam_roof_raw_<epoch_ms>.jpg`; Saved: `notecam_roof_<epoch_ms>.jpg` | Camera: scaled to max `1920px`, JPEG quality `90`, bottom-right translucent badge (`GPS: <lat/lon>`, live geocoded address or `locationBreadcrumb`, `dd/MM/yyyy hh:mm a`). Gallery: copied unmodified, no overlay. |
| Survey NoteCam: `EARTHING` (`NoteCamSlot.EARTHING`) | Camera OR Gallery (`ImageOnly`) | **None** | `survey_photos/` | Raw: `notecam_earthing_raw_<epoch_ms>.jpg`; Saved: `notecam_earthing_<epoch_ms>.jpg` | Same as Survey NoteCam `ROOF` |
| Installation Step 3 (`afterInstallationPhotos[i]`) | Camera (`NoteCam (Geo)`) OR Gallery (`ImageOnly`) | **None** | `survey_photos/` | Raw: `install_notecam_raw_<epoch_ms>.jpg`; Camera stamped: `install_notecam_<epoch_ms>.jpg`; Gallery: `install_site_<epoch_ms>.jpg` | Camera: scaled to max `1920px`, JPEG quality `90`, bottom-right translucent badge (`GPS: <lat/lon>`, live geocoded address or `villageName` / `"Solar Installation Site"`, timestamp). Gallery: copied unmodified, no overlay. |

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

### Stage C: Overall Status (`computeOverallStatus`)
- **`READY`:** `finalCategory == CATEGORY_1 && pendingActions.isEmpty()` (i.e., all documents in `requiredDocumentsFor()` have `legibility != MISSING`).
- **`PENDING_CUSTOMER`:** `pendingActions.any { !it.isCompanyAction } || pendingActions.isEmpty()` (when not Category 1 ready).
- **`PENDING_COMPANY`:** `pendingActions.isNotEmpty()` and all `pendingActions` have `isCompanyAction == true`.

*(Note: `saveWizardSurvey()` sets `finalCategory = wizard.suggestedCategory`, ignoring `wizard.overrideCategory` — see Section 7.)*

---

## 5. Data Models (Exact Property Names & Types)

### 5.1 Survey Models (`com.example.model.SurveyModels.kt`)

- **`CustomerSurvey`**:
  `id: String`, `villageId: String`, `villageNameMr: String`, `talukaNameMr: String`, `districtNameMr: String`, `surveyDate: String`, `beneficiaryName: String`, `mobile: String`, `emailId: String`, `addressLandmark: String`, `gharkulId: String`, `aadhaarLast4: String`, `casteCategory: CasteCategory`, `houseType: HouseType`, `capacity: SolarCapacity`, `gpsLocation: String?`, `meterInfo: MeterInfo`, `siteSurveyDetails: SiteSurveyDetails`, `documentComplianceChecks: DocumentComplianceChecks`, `materialAndOfficeDetails: MaterialAndOfficeDetails`, `documents: Map<DocumentType, DocumentEntry>`, `checklistAnswers: ChecklistAnswers`, `matchedCategories: List<SurveyCategory>`, `suggestedCategory: SurveyCategory`, `finalCategory: SurveyCategory`, `overrideReason: String = ""`, `overallStatus: OverallStatus`, `pendingActions: List<PendingActionItem>`, `executiveRemark: String = ""`, `surveyDateLabel: String`, `isSurveyedToday: Boolean = true`

- **`MeterInfo`**:
  `hasMeter: Boolean = true`, `consumerNumber: String = ""`, `meterHolderName: String = ""`, `sameAsBeneficiary: Boolean = true`, `relationship: String = ""`, `isHolderAlive: Boolean = true`, `isPoleNearbyForNewConn: Boolean? = null`, `interestedInNewConnection: Boolean? = null`

- **`SiteSurveyDetails`**:
  `roofType: RoofType? = null`, `hasSolarPanelSpace100SqFt: Boolean? = null`, `generationMeterLocation: String = ""`, `hasEarthingSpace: Boolean? = null`, `noteCamRoofBeforePhotoDone: Boolean = false`, `noteCamRoofBeforePhotoUri: String? = null`, `noteCamRoofBeforePhotoKb: Int? = null`, `noteCamRoofGeoInfo: String = ""`, `noteCamEarthingWithOwnerDone: Boolean = false`, `noteCamEarthingWithOwnerPhotoUri: String? = null`, `noteCamEarthingWithOwnerPhotoKb: Int? = null`, `noteCamEarthingGeoInfo: String = ""`

- **`DocumentComplianceChecks`**:
  `lightBillIsResidential: Boolean = false`, `panNameAndDobMatchAadhaar: Boolean = false`, `bankIsNationalizedNotAdcc: Boolean = false`, `singleMobileLinkedAllDocs: Boolean = false`, `aadhaarSeededToBank: Boolean = false`

- **`MaterialAndOfficeDetails`**:
  `documentsCompleteStatus: Boolean = true`, `earthWireMeters: String = "20 मीटर"`, `acCableMeters: String = "10 मीटर"`, `pipeMeters: String = "5 पाईप"`, `customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `surveyorName: String = "रमेश कुलकर्णी"`, `surveyorSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`

- **`DocumentEntry`**:
  `type: DocumentType`, `printedName: String = ""`, `photoUri: String? = null`, `photoSizeKb: Int? = null`, `isSizeValid: Boolean = true`, `legibility: DocumentLegibility = DocumentLegibility.MISSING`, `matchResult: NameMatchResult = NameMatchResult.NOT_ENTERED`, `queryNote: String = ""`

- **`ChecklistAnswers`**:
  `gharkulAndMeterSamePerson: Boolean = true`, `allDocsHaveSameName: Boolean = true`, `aadhaarNameChangeNeeded: Boolean = false`, `lightBillNameChangeNeeded: Boolean = false`, `panNameChangeNeeded: Boolean = false`, `passbookNameChangeNeeded: Boolean = false`, `meterOnFamilyMemberName: Boolean = false`, `noElectricityConnection: Boolean = false`

- **`PendingActionItem`**:
  `titleMr: String`, `responsibleMr: String`, `isCompanyAction: Boolean`

- **`District` / `Taluka` / `Village`**:
  `District(id: String, nameMr: String, nameEn: String, talukas: List<Taluka>)`
  `Taluka(id: String, nameMr: String, nameEn: String, villages: List<Village>)`
  `Village(id: String, nameMr: String, nameEn: String)`

- **`ExecutiveUser`**:
  `nameMr: String`, `firstNameMr: String`, `email: String`, `mobile: String`, `roleMr: String`

- **Dashboard Summary Models**:
  `VillageStats(surveyedToday: Int, totalCount: Int, readyCount: Int, pendingCount: Int)`
  `ReportBRow(srNo: String, labelMr: String, total: Int, openCount: Int, scCount: Int, stCount: Int)` *(computed in ViewModel, UNCLEAR/unused in UI)*

### 5.2 Installation Models (`com.example.model.InstallationModels.kt`)

- **`InstallationReport`**:
  `id: String`, `linkedSurveyId: String? = null`, `villageName: String = ""`, `consumerNo: String = ""`, `installationDate: String = defaultInstallationDate()`, `consumerName: String = ""`, `mobileNo: String = ""`, `systemCapacity: String = "1 KW"`, `systemType: String = "ONGRID"`, `products: List<InstallationProductRow> = defaultInstallationProductRows()`, `installerName: String = ""`, `installerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `accountantName: String = ""`, `managerName: String = ""`, `supervisorName: String = ""`, `customerSignatureStrokes: List<List<Pair<Float, Float>>> = emptyList()`, `inverterSerial: SerialVerificationField = SerialVerificationField()`, `generationMeterSerial: SerialVerificationField = SerialVerificationField()`, `panelSerials: List<PanelSerialEntry> = defaultPanelSerialEntries()`, `afterInstallationPhotos: List<InstallationPhotoItem> = defaultInstallationPhotos()`, `updatedAtMillis: Long = System.currentTimeMillis()`

- **`InstallationProductRow`**:
  `rowId: String`, `srNoDisplay: String`, `productModel: String`, `isSubRow: Boolean = false`, `isQtyRequired: Boolean = false`, `company: String = ""`, `size: String = ""`, `capacity: String = ""`, `qty: String = ""`

- **`SerialVerificationField`**:
  `primaryValue: String = ""`, `confirmValue: String = ""` (computed: `trimmedPrimary`, `trimmedConfirm`, `isFilled`, `isMatch`, `hasMismatch`)

- **`PanelSerialEntry`**:
  `id: String`, `serial: SerialVerificationField = SerialVerificationField()`

- **`InstallationPhotoItem`**:
  `id: String`, `caption: String = "Solar Site Photo (After Installation)"`, `photoUri: String? = null`, `photoSizeKb: Int? = null`, `geoInfo: String = ""`, `isCameraGeoStamped: Boolean = false`

### 5.3 ViewModel & Util State Models

- **`AddEditWizardState`** (`SurveyAppViewModel.kt`): `isOpen`, `editingSurveyId`, `currentStep`, `surveyDate`, `beneficiaryName`, `mobile`, `emailId`, `addressLandmark`, `gharkulId`, `aadhaarLast4`, `casteCategory`, `houseType`, `capacity`, `gpsLocation`, `meterInfo`, `siteSurveyDetails`, `documentComplianceChecks`, `materialAndOfficeDetails`, `documents`, `checklist`, `isChecklistManuallyEdited`, `matchedCategories`, `suggestedCategory`, `overrideCategory`, `overrideReason`, `executiveRemark`, `pendingActions`, `overallStatus`, `duplicateWarnings`, `validationError`.
- **`InstallationFormState`** (`SurveyAppViewModel.kt`): `editingReportId`, `linkedSurveyId`, `villageName`, `currentPage`, `consumerNo`, `installationDate`, `consumerName`, `mobileNo`, `systemCapacity`, `systemType`, `products`, `installerName`, `installerSignatureStrokes`, `accountantName`, `managerName`, `supervisorName`, `customerSignatureStrokes`, `inverterSerial`, `generationMeterSerial`, `panelSerials`, `afterInstallationPhotos`, `validationError`.
- **`CompressedImageResult`** (`ImageCompressionUtil.kt`): `uriString: String`, `sizeKb: Int`, `isWithinTargetRange: Boolean = true`.
- **`NoteCamPhotoResult`** (`ImageCompressionUtil.kt`): `uriString: String`, `sizeKb: Int`, `geoSummary: String`.

---

## 6. Backend Calls, Tables & Storage Buckets

**None.** No backend database, API client, Supabase SDK, Firebase SDK, or Room DAO is used anywhere in the codebase.

All data operations are local in-memory or local file cache:
- **Static Asset Read:** `LocationRepository` reads `assets/locations_mr.json` (read-only on app init; custom villages added to in-memory `cachedDistricts`).
- **In-Memory Data Stores (`InMemorySurveyRepository`):**
  - `surveys` (`MutableStateFlow<List<CustomerSurvey>>`): seeded with 5 sample records (`cust_1`..`cust_5`); operations: read `StateFlow`, `saveSurvey` (insert/update), `deleteSurvey` (delete by `id`).
  - `installationReports` (`MutableStateFlow<List<InstallationReport>>`): starts empty; operations: read `StateFlow`, `saveInstallationReport` (insert/update), `deleteInstallationReport` (defined in repository, never called).
  - `getExecutive()` / `getInstallationExecutive()`: returns hardcoded `ExecutiveUser` objects.
- **Local Device File Cache:**
  - `cacheDir/survey_photos/`: writes `.jpg` photos on capture/gallery pick; deletes matching files on `confirmDeleteCustomer()` via `ImageCompressionUtil.deleteCachedPhotos()`.
  - `cacheDir/survey_pdfs/`: writes generated A4 `.pdf` files for Android `ACTION_SEND` sharing via `FileProvider`.
- **Android System Location / Geocoder:** `ImageCompressionUtil.fetchLiveLocationOutcome()` queries `LocationManager` (`GPS_PROVIDER`, `NETWORK_PROVIDER`, `PASSIVE_PROVIDER` fallback) and `Geocoder(context, Locale("mr", "IN"))`.

---

## 7. Status Values & Enums

| Enum / Value List | Code Symbols & Display Labels |
|---|---|
| `LoginRole` | `SURVEY_EXECUTIVE`, `SOLAR_INSTALLATION` |
| `AppScreen` | `LOGIN`, `VILLAGE_SELECT`, `MAIN_APP`, `INSTALLATION_DASHBOARD`, `INSTALLATION_FORM` |
| `MainTab` | `HOME` (`"होम"`), `CUSTOMERS` (`"ग्राहक"`), `PROFILE` (`"प्रोफाइल"`) |
| `InstallationTab` | `HOME` (`"Home"`), `PROFILE` (`"Profile"`) |
| `CasteCategory` | `OPEN` (`"OPEN"`), `SC` (`"SC"`), `ST` (`"ST"`) |
| `HouseType` | `SINGLE` (`"1 मजली"`), `DUPLEX` (`"2 मजली"`) |
| `RoofType` | `RCC` (`"RCC (सिमेंट धाबा)"`), `TIN_SHEET` (`"पत्रा"`), `TILED` (`"कौल"`), `CEMENT_SHEET` (`"सिमेंट पत्रा"`), `UNDER_CONSTRUCTION` (`"बांधकाम सुरू आहे"`) |
| `SolarCapacity` | `KW_1` (`kw = 1`, `"1 kW"`) |
| `DocumentType` | `LIGHT_BILL` (`"लाईट बिल"`), `AADHAAR` (`"आधार कार्ड"`), `PAN` (`"पॅन कार्ड"`), `BANK_PASSBOOK` (`"बँक पासबुक किंवा कॅन्सल चेक"`), `OWNERSHIP_PROOF` (`"मालकी हक्काचा पुरावा"`), `PASSPORT_PHOTO` (`"पासपोर्ट साईज फोटो"`), `CASTE_CERTIFICATE` (`"जातीचा दाखला (SC / ST)"`), `RATION_CARD` (`"रेशन कार्ड"`), `DEATH_CERTIFICATE` (`"मृत्यू दाखला"`) |
| `ResponsibleParty` | `CUSTOMER` (`"ग्राहकाने बदल करून देणे"`), `COMPANY` (`"आम्ही बदल करून देऊ"`) |
| `DocumentLegibility` | `CLEAR` (`"स्पष्ट"`), `UNCLEAR` (`"अस्पष्ट"` — unused in UI), `MISSING` (`"उपलब्ध नाही"`) |
| `NameMatchResult` | `MATCH` (`"नाव जुळले"`), `CLOSE_MATCH` (`"जवळपास जुळते (तपासा)"`), `MISMATCH` (`"नाव जुळत नाही"`), `NOT_ENTERED` (`"नाव प्रविष्ट नाही"`) |
| `SurveyCategory` | `CATEGORY_1` (`1`, `"श्रेणी 1 · सर्व नाव समान"`), `CATEGORY_2` (`2`, `"श्रेणी 2 · नावामध्ये बदल"`), `CATEGORY_3` (`3`, `"श्रेणी 3 · मीटर दुसऱ्याच्या नावे"`), `CATEGORY_4` (`4`, `"श्रेणी 4 · लाईट कनेक्शन नाही"`) |
| `OverallStatus` | `READY` (`labelMr = "तयार"`, `chipLabelMr = "तयार"`), `PENDING_CUSTOMER` (`labelMr = "प्रलंबित · ग्राहक कार्यवाही"`, `chipLabelMr = "अपूर्ण"`), `PENDING_COMPANY` (`labelMr = "प्रलंबित · कंपनी कार्यवाही"`, `chipLabelMr = "अपूर्ण"`) |
| `CustomerStatusFilter` | `ALL` (`"सर्व"`), `READY` (`"तयार"`), `PENDING_CUSTOMER` (`"ग्राहक बाकी"`) *(note: `PENDING_COMPANY` has no dedicated filter chip)* |
| `InstallationQueueFilter` | `ALL` (`"All"`), `PENDING` (`"Pending"`), `COMPLETED` (`"Completed"`) |
| `meterInfo.relationship` (hardcoded list in `WizardStep1BasicAndMeter.kt`) | `"पती"`, `"पत्नी"`, `"वडील"`, `"आई"`, `"मुलगा"`, `"भाऊ"` |

---

## 8. Inconsistencies Found in Code

1. **`overrideCategory` / `overrideReason` / `executiveRemark` exist in State & ViewModel but are absent in UI and partially ignored on Save:**
   - `AddEditWizardState` and `SurveyAppViewModel` define `overrideCategory`, `overrideReason`, `executiveRemark`, `setOverrideCategory()`, `updateOverrideReason()`, and `updateExecutiveRemark()`, and `recalculateWizardRules()` computes `val effective = w.overrideCategory ?: suggested`.
   - `WizardStep3ReviewAndResult.kt` never renders any UI for overriding the category, entering an override reason, or editing `executiveRemark`.
   - Furthermore, `saveWizardSurvey()` sets `val finalCategory = w.suggestedCategory` (ignoring `w.overrideCategory`), while computing `pending` using `finalCategory`.
2. **Mandatory (`*`) Labels vs. Enforced Validation:**
   - `* लाईट बिलावरील नाव` (`meterHolderName` when `sameAsBeneficiary == true`) displays `*` in Step 1, but `validateStep1()` only validates `meterHolderName.isBlank()` when `!sameAsBeneficiary`.
   - All visible document cards in Step 2 display `*` (even `CASTE_CERTIFICATE`, `RATION_CARD`, `DEATH_CERTIFICATE`, which have `isRequiredCore = false` in `DocumentType`), yet Step 2 `"पुढे: तपासणी"` performs zero validation and allows proceeding/saving with all documents `MISSING`.
   - Step 3 digital signatures (`customerSignatureStrokes`, `surveyorSignatureStrokes`) have no validation check on Save in the Survey Wizard, whereas the Installation Certificate requires both signatures.
3. **Consumer Number Validation Difference Between Workflows:**
   - Survey Wizard (`SurveyAppViewModel.validateStep1`) allows `consumerNumber` to be blank, and if non-empty enforces **exactly 12 digits**.
   - Installation Certificate (`SurveyAppViewModel.validateInstallationPart1`) requires `consumerNo` to be **non-blank**, but accepts **any length/characters**.
4. **GPS Source Mismatch (Demo vs. Live):**
   - Step 1 `"घराचे GPS लोकेशन (पर्यायी)"` (`gpsLocation`) uses hardcoded district demo coordinates in `captureSampleGpsForSelectedDistrict()` (`"19.8762° N, 75.3433° E"` for `sambhajinagar`, `"19.0948° N, 74.7480° E"` for everything else).
   - Step 2 NoteCam and Installation Step 3 NoteCam use real device GPS via `LocationManager` + `Geocoder` and prompt Location Settings if unavailable.
5. **Step 2 `isSizeValid` & `DocumentLegibility.UNCLEAR` Unused:**
   - `DocumentEntry.isSizeValid` and `CompressedImageResult.isWithinTargetRange` exist in models, and `DocumentLegibility.UNCLEAR` exists in enum, but no size validation/compression is applied (`saveDocumentPhoto` copies bytes as-is and sets `isSizeValid = true`) and UI only toggles `CLEAR` / `MISSING`.
6. **`documentsCompleteStatus` vs. `OverallStatus`:**
   - `recalculateWizardRules()` automatically overwrites `materialAndOfficeDetails.documentsCompleteStatus = pending.isEmpty()`, but toggling `"पूर्ण"` / `"अपूर्ण"` in Step 3 (`onUpdateMaterialAndOffice`) does not run `recalculateWizardRules()` and does not affect `overallStatus`.
7. **Name Matching Reference Asymmetry:**
   - In `updateDocumentEntry()` and `recalculateWizardRules()`, `AADHAAR` is compared against `beneficiaryName`, while all other documents are compared against `documents[AADHAAR]?.printedName` (falling back to `beneficiaryName` if Aadhaar `printedName` is blank).
   - In `SurveyRuleEngine.computePendingActions()`, `CATEGORY_1` considers a document pending only if `legibility == DocumentLegibility.MISSING`, whereas `CATEGORY_3` treats a document as pending if `legibility != DocumentLegibility.CLEAR` (though in current UI `UNCLEAR` cannot be set).
8. **Installation Step Tab Bypass & Mobile Format:**
   - Tapping `"1. Fill Form"` / `"2. Serial Nos."` / `"3. NoteCam"` pills in `InstallationFormScreen` jumps directly via `setInstallationFormPage()` without running `validateInstallationPart1`/`Part2` (validation is still enforced on Next and Submit).
   - `installationExecutive.mobile` in `InMemorySurveyRepository` is 11 digits (`"91300028122"`), whereas all mobile input validations require 10 digits.
9. **Cached File Cleanup:**
   - Raw temp camera files (`*_raw_<ts>.jpg`) created before NoteCam stamping are left in `cacheDir/survey_photos/`, and removing a photo in Step 2/3 or editing/clearing photos does not delete the cached JPEG; `deleteCachedPhotos()` is only invoked by `confirmDeleteCustomer()`.
10. **Dead Code / Unused Structures:**
   - `SurveyAppUiState.reportBRows` (`buildReportBRows`) is computed in `recomputeDerivedCollections()` but not displayed on any screen.
   - `LoginScreen` role 1 selector uses mixed English/Marathi (`"1. Executive Login"` + `"घरकुल सर्व्हे"`).
   - `InstallationModels.defaultInstallationProductRows()` marks row `"7A"` (`A) PVC PIPE`) with `srNoDisplay = "7."`, while sub-rows `"7B"`..`"7F"` have `srNoDisplay = ""`.
