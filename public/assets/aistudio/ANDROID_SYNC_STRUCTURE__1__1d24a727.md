# Gharkul Solar Survey & Installation — Android ↔ Office Dashboard Sync Specification

> **Attach this file to your Android AI Studio project** alongside the update prompt.
> This document is the single source of truth matching the live **घरकुल सोलर सर्व्हे – ऑफिस ॲडमिन** Web Dashboard, **Supabase (`Reflection Media`)** PostgreSQL + Auth + RLS schema, and **Cloudflare R2 (`r2-presign`)** object storage pipeline.

---

## 1. System Overview & Access Control Architecture

All field executive accounts, roles, assigned locations, and mobile access permissions are controlled **exclusively from the Office Admin Web Dashboard**.

- **No self-signup on Android**: Remove or hide any "Register / Sign Up" option on mobile. The Admin creates executive accounts in the Office Dashboard and personally enters each executive's email and password on their phone.
- **Two Mobile Login Modes** (already present in the Android app — wire both to **Supabase Auth** + `public.executives`):
  1. **Executive Login (Survey Executive)** → requires `public.executives.role == 'SURVEY_EXECUTIVE'` **AND** `is_active == true`.
  2. **Installation Login (Solar Installation)** → requires `public.executives.role == 'SOLAR_INSTALLATION'` **AND** `is_active == true`.
- **Admin accounts (`ADMIN`) cannot log into field mobile modes** (and field accounts cannot log into the Office Web Dashboard).
- **Mandatory Live Access Check**:
  - On **Login**, **App Launch (session restore)**, **Foreground Resume (`onResume`)**, and **before every Sync/Upload**, query `public.executives` by `auth_user_id = auth.uid()`.
  - Verify `is_active == true` AND `role` matches the active login mode.
  - If `is_active == false` or `role` changed or row is missing: immediately call `supabase.auth.signOut()`, clear local session state, and show:
    - Marathi message: `"तुमचा मोबाईल प्रवेश ऑफिस अ‍ॅडमिनने बंद केला आहे किंवा तुमची भूमिका बदलली आहे. कृपया ऑफिसशी संपर्क साधा."`
- **Update `last_login_at`**: Immediately after successful mobile login, update `public.executives.last_login_at = now()` (ISO-8601 UTC string) on the logged-in executive's row.

---

## 2. Role-Scoped Visibility Rules (Strict — Enforced in Both Supabase RLS & Android UI)

| Mobile Login Mode | Required `executives.role` | Visible Customers / Records on Mobile | Allowed Writes |
|---|---|---|---|
| **Survey Executive Login** | `SURVEY_EXECUTIVE` (`is_active = true`) | **ONLY** `customer_surveys` where `executive_id = current_executive.id` (and their `survey_documents`). Also reads `custom_villages`. | Create & edit **only own** `customer_surveys` (always setting `executive_id = current_executive.id`), upsert `survey_documents`, upload/replace docs & NoteCam photos via `r2-presign`, add `custom_villages`. |
| **Solar Installation Login** | `SOLAR_INSTALLATION` (`is_active = true`) | **Tab 1 — Pending Queue (`इन्स्टॉलेशन बाकी ग्राहक`)**: **ONLY** `customer_surveys` where `final_category = 'CATEGORY_1'` **AND** `gov_form_completed = true` **AND** no row exists in `installation_reports` with `linked_survey_id = customer_surveys.id`.<br>**Tab 2 — My Submitted Reports (`माझे पूर्ण इन्स्टॉलेशन`)**: **ONLY** `installation_reports` where `installer_executive_id = current_executive.id` (and their `installation_photos`). | Read-only on pending Category 1 `customer_surveys`. Create & edit **only own** `installation_reports` (always setting `installer_executive_id = current_executive.id` and `linked_survey_id = selected_survey.id`), insert/replace `installation_photos`, upload/replace site photos via `r2-presign`. |

### Special Assigned Area Value (`सर्व गाव`)
- In `public.executives`, `assigned_district`, `assigned_taluka`, and `assigned_village` store Marathi display names (e.g., `अहिल्यानगर`, `अकोले`, `धामणगाव आवारी`).
- When `assigned_village == "सर्व गाव"` (or empty `""`), the Survey Executive can select **any village** in their assigned taluka/district (or all villages if `assigned_taluka` is empty).
- When `assigned_village` is a specific village name, pre-select it by default in the Survey form (still allowing custom village addition if needed).

---

## 3. Shared Supabase PostgreSQL Schema (All 6 Tables)

Every column name below matches the exact snake_case columns queried and written by the Office Admin Web Dashboard.

### Table 1: `public.executives`
```sql
id                UUID PRIMARY KEY DEFAULT gen_random_uuid()
auth_user_id      UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE
role              TEXT NOT NULL CHECK (role IN ('SURVEY_EXECUTIVE', 'SOLAR_INSTALLATION', 'ADMIN'))
name_mr           TEXT NOT NULL
first_name_mr     TEXT NOT NULL DEFAULT ''
email             TEXT UNIQUE NOT NULL
mobile            TEXT NOT NULL DEFAULT ''
role_mr           TEXT NOT NULL DEFAULT ''
assigned_district TEXT NOT NULL DEFAULT ''
assigned_taluka   TEXT NOT NULL DEFAULT ''
assigned_village  TEXT NOT NULL DEFAULT ''   -- Specific village name OR 'सर्व गाव'
is_active         BOOLEAN NOT NULL DEFAULT true
last_login_at     TIMESTAMPTZ
created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
```

### Table 2: `public.custom_villages`
```sql
id                TEXT PRIMARY KEY           -- e.g. 'ahilyanagar_akole_custom_1710000000'
district_id       TEXT NOT NULL              -- e.g. 'ahilyanagar', 'sambhajinagar'
district_name_mr  TEXT NOT NULL              -- e.g. 'अहिल्यानगर'
taluka_id         TEXT NOT NULL              -- e.g. 'akole'
taluka_name_mr    TEXT NOT NULL              -- e.g. 'अकोले'
village_name_mr   TEXT NOT NULL
village_name_en   TEXT NOT NULL DEFAULT ''
created_by        UUID REFERENCES public.executives(id) ON DELETE SET NULL
created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
```

### Table 3: `public.customer_surveys`
```sql
id                                TEXT PRIMARY KEY   -- e.g. 'survey_<epochMs>_<rand>'
village_id                        TEXT NOT NULL DEFAULT ''
village_name_mr                   TEXT NOT NULL DEFAULT ''
taluka_name_mr                    TEXT NOT NULL DEFAULT ''
district_name_mr                  TEXT NOT NULL DEFAULT ''
survey_date                       TEXT NOT NULL      -- Format: 'DD/MM/YYYY' (e.g. '19/11/2026')
survey_date_label                 TEXT NOT NULL DEFAULT ''
is_surveyed_today                 BOOLEAN NOT NULL DEFAULT true

-- Customer & Gharkul details
beneficiary_name                  TEXT NOT NULL
mobile                            TEXT NOT NULL      -- 10 digits only
email_id                          TEXT NOT NULL DEFAULT ''
address_landmark                  TEXT NOT NULL DEFAULT ''
gharkul_id                        TEXT NOT NULL DEFAULT ''
aadhaar_last4                     TEXT NOT NULL DEFAULT '' -- 4 digits
caste_category                    TEXT NOT NULL DEFAULT 'OPEN' CHECK (caste_category IN ('OPEN', 'SC', 'ST'))
house_type                        TEXT NOT NULL DEFAULT 'SINGLE' CHECK (house_type IN ('SINGLE', 'DUPLEX'))
capacity_kw                       NUMERIC NOT NULL DEFAULT 1
gps_location                      TEXT               -- e.g. '19.5342, 74.0018'

-- Electricity Meter info
has_meter                         BOOLEAN NOT NULL DEFAULT true
consumer_number                   TEXT NOT NULL DEFAULT ''
meter_holder_name                 TEXT NOT NULL DEFAULT ''
same_as_beneficiary               BOOLEAN NOT NULL DEFAULT true
meter_relationship                TEXT NOT NULL DEFAULT ''
is_holder_alive                   BOOLEAN NOT NULL DEFAULT true
is_pole_nearby_for_new_conn       BOOLEAN            -- nullable
interested_in_new_connection      BOOLEAN            -- nullable

-- Site Survey & NoteCam Photos (Cloudflare R2)
roof_type                         TEXT CHECK (roof_type IN ('RCC', 'TIN_SHEET', 'TILED', 'CEMENT_SHEET', 'UNDER_CONSTRUCTION'))
has_solar_panel_space_100_sqft    BOOLEAN            -- nullable
generation_meter_location         TEXT NOT NULL DEFAULT ''
has_earthing_space                BOOLEAN            -- nullable
notecam_roof_done                 BOOLEAN NOT NULL DEFAULT false
notecam_roof_photo_url            TEXT
notecam_roof_r2_key               TEXT
notecam_roof_photo_kb             INTEGER
notecam_roof_geo_info             TEXT NOT NULL DEFAULT ''
notecam_earthing_done             BOOLEAN NOT NULL DEFAULT false
notecam_earthing_photo_url        TEXT
notecam_earthing_r2_key           TEXT
notecam_earthing_photo_kb         INTEGER
notecam_earthing_geo_info         TEXT NOT NULL DEFAULT ''

-- 5 Compliance Checks
light_bill_is_residential         BOOLEAN NOT NULL DEFAULT false
pan_name_dob_match_aadhaar        BOOLEAN NOT NULL DEFAULT false
bank_is_nationalized_not_adcc     BOOLEAN NOT NULL DEFAULT false
single_mobile_linked_all_docs     BOOLEAN NOT NULL DEFAULT false
aadhaar_seeded_to_bank            BOOLEAN NOT NULL DEFAULT false

-- Material Estimate & Vector Signatures
documents_complete_status         BOOLEAN NOT NULL DEFAULT true
earth_wire_meters                 TEXT NOT NULL DEFAULT '20 मीटर'
ac_cable_meters                   TEXT NOT NULL DEFAULT '10 मीटर'
pipe_meters                       TEXT NOT NULL DEFAULT '5 पाईप'
surveyor_name                     TEXT NOT NULL DEFAULT ''
signatures                        JSONB NOT NULL DEFAULT '{"customer": [], "surveyor": []}'::jsonb

-- Checklist, Category Engine & Status
checklist_answers                 JSONB NOT NULL DEFAULT '{}'::jsonb
matched_categories                JSONB NOT NULL DEFAULT '["CATEGORY_2"]'::jsonb
suggested_category                TEXT NOT NULL DEFAULT 'CATEGORY_2' CHECK (suggested_category IN ('CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3', 'CATEGORY_4'))
final_category                    TEXT NOT NULL DEFAULT 'CATEGORY_2' CHECK (final_category IN ('CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3', 'CATEGORY_4'))
override_reason                   TEXT NOT NULL DEFAULT ''
overall_status                    TEXT NOT NULL DEFAULT 'PENDING_CUSTOMER' CHECK (overall_status IN ('READY', 'PENDING_CUSTOMER', 'PENDING_COMPANY'))
pending_actions                   JSONB NOT NULL DEFAULT '[]'::jsonb
executive_remark                  TEXT NOT NULL DEFAULT ''

-- Ownership & Handoff to Installation
executive_id                      UUID REFERENCES public.executives(id) ON DELETE SET NULL
gov_form_completed                BOOLEAN NOT NULL DEFAULT false   -- Set to true ONLY by Office Admin ("फॉर्म पूर्ण")
gov_form_completed_at             TIMESTAMPTZ
created_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
```

### Table 4: `public.survey_documents`
```sql
id              TEXT PRIMARY KEY             -- Format: '<survey_id>_<doc_type>'
survey_id       TEXT NOT NULL REFERENCES public.customer_surveys(id) ON DELETE CASCADE
doc_type        TEXT NOT NULL CHECK (doc_type IN (
                  'LIGHT_BILL', 'AADHAAR', 'PAN', 'BANK_PASSBOOK',
                  'OWNERSHIP_PROOF', 'PASSPORT_PHOTO', 'CASTE_CERTIFICATE',
                  'RATION_CARD', 'DEATH_CERTIFICATE'
                ))
printed_name    TEXT NOT NULL DEFAULT ''
photo_url       TEXT                         -- Cloudflare R2 public_url
r2_object_key   TEXT                         -- e.g. 'survey-docs/<survey_id>/light_bill_1795095839892.jpg'
photo_size_kb   INTEGER
is_size_valid   BOOLEAN NOT NULL DEFAULT true -- Always true (NO 500 KB cap)
legibility      TEXT NOT NULL DEFAULT 'CLEAR' CHECK (legibility IN ('CLEAR', 'UNCLEAR', 'MISSING'))
match_result    TEXT NOT NULL DEFAULT 'NOT_ENTERED' CHECK (match_result IN ('MATCH', 'CLOSE_MATCH', 'MISMATCH', 'NOT_ENTERED'))
query_note      TEXT NOT NULL DEFAULT ''
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
UNIQUE (survey_id, doc_type)
```

> **Note on Legibility UI**: In edit/upload modals, present **`बरोबर`** (stored as `'CLEAR'`) and **`उपलब्ध नाही`** (stored as `'MISSING'`). Keep `'CLEAR'` / `'MISSING'` as the stored database values.

### Table 5: `public.installation_reports`
```sql
id                                TEXT PRIMARY KEY  -- e.g. 'inst_<epochMs>_<rand>'
linked_survey_id                  TEXT REFERENCES public.customer_surveys(id) ON DELETE SET NULL
village_name                      TEXT NOT NULL DEFAULT ''
consumer_no                       TEXT NOT NULL DEFAULT ''
installation_date                 TEXT NOT NULL     -- Format: 'DD / MM / YYYY'
consumer_name                     TEXT NOT NULL
mobile_no                         TEXT NOT NULL     -- 10 digits only
system_capacity                   TEXT NOT NULL DEFAULT '1 KW'
system_type                       TEXT NOT NULL DEFAULT 'ONGRID'
products                          JSONB NOT NULL DEFAULT '[]'::jsonb  -- 23-row array (see Section 5)
installer_name                    TEXT NOT NULL DEFAULT ''
accountant_name                   TEXT NOT NULL DEFAULT ''
manager_name                      TEXT NOT NULL DEFAULT ''
supervisor_name                   TEXT NOT NULL DEFAULT ''
signatures                        JSONB NOT NULL DEFAULT '{"installer": [], "customer": []}'::jsonb
inverter_serial_primary           TEXT NOT NULL DEFAULT ''
inverter_serial_confirm           TEXT NOT NULL DEFAULT ''
generation_meter_serial_primary   TEXT NOT NULL DEFAULT ''
generation_meter_serial_confirm   TEXT NOT NULL DEFAULT ''
panel_serial_numbers              JSONB NOT NULL DEFAULT '[]'::jsonb  -- Array of { id, primaryValue, confirmValue }
updated_at_millis                 BIGINT NOT NULL
installer_executive_id            UUID REFERENCES public.executives(id) ON DELETE SET NULL
created_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
```

### Table 6: `public.installation_photos`
```sql
id                    TEXT PRIMARY KEY
report_id             TEXT NOT NULL REFERENCES public.installation_reports(id) ON DELETE CASCADE
slot_index            INTEGER NOT NULL DEFAULT 0
caption               TEXT NOT NULL DEFAULT ''
photo_url             TEXT
r2_object_key         TEXT               -- e.g. 'installation-photos/<report_id>/photo_1_0_1795095839892.jpg'
photo_size_kb         INTEGER
geo_info              TEXT NOT NULL DEFAULT ''
is_camera_geo_stamped BOOLEAN NOT NULL DEFAULT false
created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
```

---

## 4. Production Row Level Security (RLS) SQL — Run Once in Supabase SQL Editor

This SQL enforces all Office Admin + Survey Executive + Solar Installation Executive access rules at the PostgreSQL database layer:

```sql
-- Helper functions (SECURITY DEFINER avoids recursive RLS lookups on public.executives)
CREATE OR REPLACE FUNCTION public.current_active_executive_id()
RETURNS UUID
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT id FROM public.executives
  WHERE auth_user_id = auth.uid() AND is_active = true
  LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.current_active_executive_role()
RETURNS TEXT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT role FROM public.executives
  WHERE auth_user_id = auth.uid() AND is_active = true
  LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.survey_has_installation(target_survey_id TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.installation_reports
    WHERE linked_survey_id = target_survey_id
  );
$$;

-- Enable RLS on all 6 tables
ALTER TABLE public.executives ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_villages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_surveys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.survey_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.installation_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.installation_photos ENABLE ROW LEVEL SECURITY;

-- Drop old permissive policies if present
DROP POLICY IF EXISTS "exec_select_policy" ON public.executives;
DROP POLICY IF EXISTS "exec_admin_all" ON public.executives;
DROP POLICY IF EXISTS "exec_self_select" ON public.executives;
DROP POLICY IF EXISTS "exec_self_update_login" ON public.executives;

-- 1. public.executives
CREATE POLICY "exec_admin_all" ON public.executives
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

CREATE POLICY "exec_self_select" ON public.executives
  FOR SELECT USING (auth_user_id = auth.uid());

CREATE POLICY "exec_self_update_login" ON public.executives
  FOR UPDATE USING (auth_user_id = auth.uid() AND is_active = true)
  WITH CHECK (
    auth_user_id = auth.uid()
    AND is_active = true
    AND role = public.current_active_executive_role()
  );

-- 2. public.custom_villages
DROP POLICY IF EXISTS "villages_active_select" ON public.custom_villages;
DROP POLICY IF EXISTS "villages_active_insert" ON public.custom_villages;
DROP POLICY IF EXISTS "villages_admin_all" ON public.custom_villages;

CREATE POLICY "villages_admin_all" ON public.custom_villages
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

CREATE POLICY "villages_active_select" ON public.custom_villages
  FOR SELECT USING (public.current_active_executive_id() IS NOT NULL);

CREATE POLICY "villages_active_insert" ON public.custom_villages
  FOR INSERT WITH CHECK (public.current_active_executive_id() IS NOT NULL);

-- 3. public.customer_surveys
DROP POLICY IF EXISTS "surveys_admin_all" ON public.customer_surveys;
DROP POLICY IF EXISTS "surveys_exec_own_select" ON public.customer_surveys;
DROP POLICY IF EXISTS "surveys_exec_own_insert" ON public.customer_surveys;
DROP POLICY IF EXISTS "surveys_exec_own_update" ON public.customer_surveys;
DROP POLICY IF EXISTS "surveys_installer_pending_select" ON public.customer_surveys;

CREATE POLICY "surveys_admin_all" ON public.customer_surveys
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

-- Survey Executive: ONLY own created surveys
CREATE POLICY "surveys_exec_own_select" ON public.customer_surveys
  FOR SELECT USING (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND executive_id = public.current_active_executive_id()
  );

CREATE POLICY "surveys_exec_own_insert" ON public.customer_surveys
  FOR INSERT WITH CHECK (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND executive_id = public.current_active_executive_id()
    AND gov_form_completed = false
  );

CREATE POLICY "surveys_exec_own_update" ON public.customer_surveys
  FOR UPDATE USING (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND executive_id = public.current_active_executive_id()
  )
  WITH CHECK (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND executive_id = public.current_active_executive_id()
  );

-- Solar Installation Executive: ONLY pending Category 1 + gov_form_completed = true + not yet installed
CREATE POLICY "surveys_installer_pending_select" ON public.customer_surveys
  FOR SELECT USING (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND final_category = 'CATEGORY_1'
    AND gov_form_completed = true
    AND NOT public.survey_has_installation(id)
  );

-- 4. public.survey_documents
DROP POLICY IF EXISTS "docs_admin_all" ON public.survey_documents;
DROP POLICY IF EXISTS "docs_exec_own_all" ON public.survey_documents;

CREATE POLICY "docs_admin_all" ON public.survey_documents
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

CREATE POLICY "docs_exec_own_all" ON public.survey_documents
  FOR ALL USING (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND EXISTS (
      SELECT 1 FROM public.customer_surveys s
      WHERE s.id = survey_documents.survey_id
        AND s.executive_id = public.current_active_executive_id()
    )
  )
  WITH CHECK (
    public.current_active_executive_role() = 'SURVEY_EXECUTIVE'
    AND EXISTS (
      SELECT 1 FROM public.customer_surveys s
      WHERE s.id = survey_documents.survey_id
        AND s.executive_id = public.current_active_executive_id()
    )
  );

-- 5. public.installation_reports
DROP POLICY IF EXISTS "install_admin_all" ON public.installation_reports;
DROP POLICY IF EXISTS "install_exec_own_select" ON public.installation_reports;
DROP POLICY IF EXISTS "install_exec_own_insert" ON public.installation_reports;
DROP POLICY IF EXISTS "install_exec_own_update" ON public.installation_reports;

CREATE POLICY "install_admin_all" ON public.installation_reports
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

-- Solar Installation Executive: ONLY own submitted reports
CREATE POLICY "install_exec_own_select" ON public.installation_reports
  FOR SELECT USING (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND installer_executive_id = public.current_active_executive_id()
  );

CREATE POLICY "install_exec_own_insert" ON public.installation_reports
  FOR INSERT WITH CHECK (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND installer_executive_id = public.current_active_executive_id()
  );

CREATE POLICY "install_exec_own_update" ON public.installation_reports
  FOR UPDATE USING (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND installer_executive_id = public.current_active_executive_id()
  )
  WITH CHECK (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND installer_executive_id = public.current_active_executive_id()
  );

-- 6. public.installation_photos
DROP POLICY IF EXISTS "install_photos_admin_all" ON public.installation_photos;
DROP POLICY IF EXISTS "install_photos_exec_own_all" ON public.installation_photos;

CREATE POLICY "install_photos_admin_all" ON public.installation_photos
  FOR ALL USING (public.current_active_executive_role() = 'ADMIN')
  WITH CHECK (public.current_active_executive_role() = 'ADMIN');

CREATE POLICY "install_photos_exec_own_all" ON public.installation_photos
  FOR ALL USING (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND EXISTS (
      SELECT 1 FROM public.installation_reports r
      WHERE r.id = installation_photos.report_id
        AND r.installer_executive_id = public.current_active_executive_id()
    )
  )
  WITH CHECK (
    public.current_active_executive_role() = 'SOLAR_INSTALLATION'
    AND EXISTS (
      SELECT 1 FROM public.installation_reports r
      WHERE r.id = installation_photos.report_id
        AND r.installer_executive_id = public.current_active_executive_id()
    )
  );
```

---

## 5. Exact JSONB Structures & Business Logic

### 5.1 `checklist_answers` JSONB Keys (8 Booleans)
```json
{
  "gharkulAndMeterSamePerson": true,
  "allDocsHaveSameName": true,
  "aadhaarNameChangeNeeded": false,
  "lightBillNameChangeNeeded": false,
  "panNameChangeNeeded": false,
  "passbookNameChangeNeeded": false,
  "meterOnFamilyMemberName": false,
  "noElectricityConnection": false
}
```

### 5.2 Category Priority Rules (`suggested_category` / `final_category`)
- **CATEGORY_4**: `has_meter == false` OR `noElectricityConnection == true`
- **CATEGORY_3**: `has_meter == true` AND (`same_as_beneficiary == false` OR `meterOnFamilyMemberName == true`)
- **CATEGORY_2**: Any of `aadhaarNameChangeNeeded`, `lightBillNameChangeNeeded`, `panNameChangeNeeded`, `passbookNameChangeNeeded` is `true`, OR `allDocsHaveSameName == false`
- **CATEGORY_1**: All documents clear and names match beneficiary + meter on beneficiary's name (`overall_status = 'READY'`)
- Priority order when multiple match: **`CATEGORY_4 > CATEGORY_3 > CATEGORY_2 > CATEGORY_1`**.

### 5.3 Required Documents (`requiredDocumentsFor`)
- Always required: `AADHAAR`, `PAN`, `BANK_PASSBOOK`, `OWNERSHIP_PROOF`, `PASSPORT_PHOTO`
- If `has_meter == true`: add `LIGHT_BILL`
- If `caste_category IN ('SC', 'ST')`: add `CASTE_CERTIFICATE`
- If `has_meter == true && same_as_beneficiary == false`: add `RATION_CARD`; and if `is_holder_alive == false`: add `DEATH_CERTIFICATE`

### 5.4 `pending_actions` JSONB Array Item Format
```json
[
  {
    "titleMr": "लाईट बिलावरील नाव दुरुस्त करणे",
    "responsibleMr": "कंपनी",
    "isCompanyAction": true
  }
]
```

### 5.5 `signatures` JSONB Vector Stroke Format
Normalized `0.0..1.0` coordinate pairs (`[x, y]`) grouped into strokes (`List<List<Pair<Float, Float>>>`):
- On `customer_surveys`:
```json
{
  "customer": [[[0.12, 0.45], [0.18, 0.48]]],
  "surveyor": [[[0.20, 0.50], [0.25, 0.52]]]
}
```
- On `installation_reports`:
```json
{
  "installer": [[[0.12, 0.45], [0.18, 0.48]]],
  "customer": [[[0.20, 0.50], [0.25, 0.52]]]
}
```

### 5.6 `products` JSONB Array (Exact 23 Default Rows for `installation_reports`)
Each row object:
```json
{
  "rowId": "1",
  "srNoDisplay": "1.",
  "productModel": "PANEL'S",
  "isSubRow": false,
  "isQtyRequired": true,
  "company": "",
  "size": "",
  "capacity": "",
  "qty": "02"
}
```
Exact 23 `rowId` / `srNoDisplay` / `productModel` / default `qty` entries:
1. `1` (`1.`): `PANEL'S` — default qty `02` (required)
2. `2` (`2.`): `INVERTER` — default qty `01` (required)
3. `2A` (``): `A) LA Cable 16 sq mm` — subRow, default qty `20 mtr` (required)
4. `2B` (``): `B) AC Cable 1.5 sq mm` — subRow, default qty `10 mtr` (required)
5. `2C` (``): `C) DC Cable` — subRow, qty `""`
6. `2D` (``): `D) Earthing Cable` — subRow, qty `""`
7. `3` (`3.`): `ACDB` — default qty `01` (required)
8. `4` (`4.`): `EARTHING RODE` — default qty `02` (required)
9. `5` (`5.`): `L.A.` — default qty `01` (required)
10. `6` (`6.`): `CHEMICAL BAG` — default qty `01` (required)
11. `7A` (`7.`): `A) PVC PIPE` — subRow, default qty `08` (required)
12. `7B` (``): `B) PVC CONDUIT` — subRow, qty `""`
13. `7C` (``): `C) PVC ELBOW` — subRow, qty `""`
14. `7D` (``): `D) PVC T` — subRow, qty `""`
15. `7E` (``): `E) PVC CLAMP` — subRow, qty `""`
16. `7F` (``): `F) PVC LONG BEND` — subRow, qty `""`
17. `8` (`8.`): `MC 4 CONNECTOR` — qty `""`
18. `9` (`9.`): `LUG FOR STRUCTURE` — qty `""`
19. `10` (`10.`): `LUG FOR ACDB` — qty `""`
20. `11` (`11.`): `LUG FOR L.A.` — default qty `02` (required)
21. `12` (`12.`): `EARTH COVER PIT` — default qty `02` (required)
22. `13` (`13.`): `GENERATION METER` — default qty `01` (required)
23. `14` (`14.`): `EXTRA WIRING` — qty `""`

### 5.7 `panel_serial_numbers` JSONB Format
```json
[
  { "id": "panel_1", "primaryValue": "SN123456", "confirmValue": "SN123456" },
  { "id": "panel_2", "primaryValue": "SN123457", "confirmValue": "SN123457" }
]
```

---

## 6. Cloudflare R2 Upload & Delete Contract (`r2-presign` Edge Function)

All images (KYC documents, NoteCam roof/earthing photos, and after-installation site photos) are uploaded to **Cloudflare R2** using the deployed Supabase Edge Function:

- **Endpoint**: `POST {SUPABASE_URL}/functions/v1/r2-presign`
- **Required Headers**:
  - `Authorization: Bearer <current_supabase_access_token>`
  - `apikey: <SUPABASE_ANON_KEY>`
  - `Content-Type: application/json`
- **Valid `folder` values**:
  - `"survey-docs"` (KYC documents)
  - `"survey-notecam"` (Roof & Earthing NoteCam photos)
  - `"installation-photos"` (After-installation site photos)
- **File Constraints**: Images only (`image/jpeg`, `image/png`, `image/webp`). **No 500 KB file size cap** (`is_size_valid = true`).

### Step A — Replacing an Old Photo (Delete Old First!)
If replacing an existing document or photo that already has an `r2_object_key`:
```json
POST /functions/v1/r2-presign
{
  "action": "delete",
  "object_keys": ["survey-docs/<survey_id>/light_bill_old.jpg"]
}
```

### Step B — Request Presigned Upload URL
```json
POST /functions/v1/r2-presign
{
  "action": "presign",
  "folder": "survey-docs",
  "recordId": "<survey_id_or_report_id>",
  "fileName": "light_bill_1795095839892.jpg",
  "contentType": "image/jpeg"
}
```
Response (`200 OK`):
```json
{
  "upload_url": "https://<account>.r2.cloudflarestorage.com/<bucket>/<object_key>?X-Amz-...",
  "public_url": "https://pub-xxxx.r2.dev/<object_key>",
  "object_key": "survey-docs/<survey_id>/light_bill_1795095839892.jpg"
}
```

### Step C — Upload Raw Image Bytes Directly to Cloudflare R2
```
PUT <upload_url>
Header: Content-Type: <same contentType>
Body: <raw image bytes>
```
On HTTP `200`, persist `photo_url = public_url`, `r2_object_key = object_key`, and `photo_size_kb = max(1, round(bytes / 1024))` to Supabase!

> **Important Note on Indian Mobile Networks (`*.r2.dev`)**:
> Store the canonical `public_url` (`https://pub-xxxx.r2.dev/...`) in Supabase so the Office Web Dashboard (which proxies `*.r2.dev` via `/api/r2-image`) displays it reliably. On Android, cache newly captured local images for immediate preview and gracefully show a retry button if an ISP blocks `*.r2.dev` previews.

---

## 7. Base Location IDs (Shared Across Android & Office Dashboard)

- Districts:
  - `ahilyanagar` → `अहिल्यानगर`
  - `sambhajinagar` → `छत्रपती संभाजीनगर`
- Additional districts/villages added in the Office Dashboard are stored in `public.custom_villages` and should be merged into the Android village dropdown on sync.
