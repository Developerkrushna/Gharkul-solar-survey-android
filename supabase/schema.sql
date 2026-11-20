-- Gharkul Solar Survey & Installation — Shared Supabase PostgreSQL Schema & Production RLS
-- Matches ANDROID_SYNC_STRUCTURE.md verbatim. Pairs with Cloudflare R2 via supabase/functions/r2-presign.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. public.executives
CREATE TABLE IF NOT EXISTS public.executives (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  auth_user_id      UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  role              TEXT NOT NULL CHECK (role IN ('SURVEY_EXECUTIVE', 'SOLAR_INSTALLATION', 'ADMIN')),
  name_mr           TEXT NOT NULL,
  first_name_mr     TEXT NOT NULL DEFAULT '',
  email             TEXT UNIQUE NOT NULL,
  mobile            TEXT NOT NULL DEFAULT '',
  role_mr           TEXT NOT NULL DEFAULT '',
  assigned_district TEXT NOT NULL DEFAULT '',
  assigned_taluka   TEXT NOT NULL DEFAULT '',
  assigned_village  TEXT NOT NULL DEFAULT '',
  is_active         BOOLEAN NOT NULL DEFAULT true,
  last_login_at     TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_executives_auth_user_id ON public.executives(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_executives_role_active ON public.executives(role, is_active);

-- 2. public.custom_villages
CREATE TABLE IF NOT EXISTS public.custom_villages (
  id                TEXT PRIMARY KEY,
  district_id       TEXT NOT NULL,
  district_name_mr  TEXT NOT NULL,
  taluka_id         TEXT NOT NULL,
  taluka_name_mr    TEXT NOT NULL,
  village_name_mr   TEXT NOT NULL,
  village_name_en   TEXT NOT NULL DEFAULT '',
  created_by        UUID REFERENCES public.executives(id) ON DELETE SET NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_custom_villages_lookup ON public.custom_villages(district_id, taluka_id);

-- 3. public.customer_surveys
CREATE TABLE IF NOT EXISTS public.customer_surveys (
  id                                TEXT PRIMARY KEY,
  village_id                        TEXT NOT NULL DEFAULT '',
  village_name_mr                   TEXT NOT NULL DEFAULT '',
  taluka_name_mr                    TEXT NOT NULL DEFAULT '',
  district_name_mr                  TEXT NOT NULL DEFAULT '',
  survey_date                       TEXT NOT NULL,
  survey_date_label                 TEXT NOT NULL DEFAULT '',
  is_surveyed_today                 BOOLEAN NOT NULL DEFAULT true,

  -- Customer & Gharkul details
  beneficiary_name                  TEXT NOT NULL,
  mobile                            TEXT NOT NULL,
  email_id                          TEXT NOT NULL DEFAULT '',
  address_landmark                  TEXT NOT NULL DEFAULT '',
  gharkul_id                        TEXT NOT NULL DEFAULT '',
  aadhaar_last4                     TEXT NOT NULL DEFAULT '',
  caste_category                    TEXT NOT NULL DEFAULT 'OPEN' CHECK (caste_category IN ('OPEN', 'SC', 'ST')),
  house_type                        TEXT NOT NULL DEFAULT 'SINGLE' CHECK (house_type IN ('SINGLE', 'DUPLEX')),
  capacity_kw                       NUMERIC NOT NULL DEFAULT 1,
  gps_location                      TEXT,

  -- Electricity Meter info
  has_meter                         BOOLEAN NOT NULL DEFAULT true,
  consumer_number                   TEXT NOT NULL DEFAULT '',
  meter_holder_name                 TEXT NOT NULL DEFAULT '',
  same_as_beneficiary               BOOLEAN NOT NULL DEFAULT true,
  meter_relationship                TEXT NOT NULL DEFAULT '',
  is_holder_alive                   BOOLEAN NOT NULL DEFAULT true,
  is_pole_nearby_for_new_conn       BOOLEAN,
  interested_in_new_connection      BOOLEAN,

  -- Site Survey & NoteCam Photos (Cloudflare R2)
  roof_type                         TEXT CHECK (roof_type IS NULL OR roof_type IN ('RCC', 'TIN_SHEET', 'TILED', 'CEMENT_SHEET', 'UNDER_CONSTRUCTION')),
  has_solar_panel_space_100_sqft    BOOLEAN,
  generation_meter_location         TEXT NOT NULL DEFAULT '',
  has_earthing_space                BOOLEAN,
  notecam_roof_done                 BOOLEAN NOT NULL DEFAULT false,
  notecam_roof_photo_url            TEXT,
  notecam_roof_r2_key               TEXT,
  notecam_roof_photo_kb             INTEGER,
  notecam_roof_geo_info             TEXT NOT NULL DEFAULT '',
  notecam_earthing_done             BOOLEAN NOT NULL DEFAULT false,
  notecam_earthing_photo_url        TEXT,
  notecam_earthing_r2_key           TEXT,
  notecam_earthing_photo_kb         INTEGER,
  notecam_earthing_geo_info         TEXT NOT NULL DEFAULT '',

  -- 5 Compliance Checks
  light_bill_is_residential         BOOLEAN NOT NULL DEFAULT false,
  pan_name_dob_match_aadhaar        BOOLEAN NOT NULL DEFAULT false,
  bank_is_nationalized_not_adcc     BOOLEAN NOT NULL DEFAULT false,
  single_mobile_linked_all_docs     BOOLEAN NOT NULL DEFAULT false,
  aadhaar_seeded_to_bank            BOOLEAN NOT NULL DEFAULT false,

  -- Material Estimate & Vector Signatures
  documents_complete_status         BOOLEAN NOT NULL DEFAULT true,
  earth_wire_meters                 TEXT NOT NULL DEFAULT '20 मीटर',
  ac_cable_meters                   TEXT NOT NULL DEFAULT '10 मीटर',
  pipe_meters                       TEXT NOT NULL DEFAULT '5 पाईप',
  surveyor_name                     TEXT NOT NULL DEFAULT '',
  signatures                        JSONB NOT NULL DEFAULT '{"customer": [], "surveyor": []}'::jsonb,

  -- Checklist, Category Engine & Status
  checklist_answers                 JSONB NOT NULL DEFAULT '{}'::jsonb,
  matched_categories                JSONB NOT NULL DEFAULT '["CATEGORY_2"]'::jsonb,
  suggested_category                TEXT NOT NULL DEFAULT 'CATEGORY_2' CHECK (suggested_category IN ('CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3', 'CATEGORY_4')),
  final_category                    TEXT NOT NULL DEFAULT 'CATEGORY_2' CHECK (final_category IN ('CATEGORY_1', 'CATEGORY_2', 'CATEGORY_3', 'CATEGORY_4')),
  override_reason                   TEXT NOT NULL DEFAULT '',
  overall_status                    TEXT NOT NULL DEFAULT 'PENDING_CUSTOMER' CHECK (overall_status IN ('READY', 'PENDING_CUSTOMER', 'PENDING_COMPANY')),
  pending_actions                   JSONB NOT NULL DEFAULT '[]'::jsonb,
  executive_remark                  TEXT NOT NULL DEFAULT '',

  -- Ownership & Handoff to Installation
  executive_id                      UUID REFERENCES public.executives(id) ON DELETE SET NULL,
  gov_form_completed                BOOLEAN NOT NULL DEFAULT false,
  gov_form_completed_at             TIMESTAMPTZ,
  created_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customer_surveys_village ON public.customer_surveys(village_id);
CREATE INDEX IF NOT EXISTS idx_customer_surveys_cat_status ON public.customer_surveys(final_category, overall_status, gov_form_completed);
CREATE INDEX IF NOT EXISTS idx_customer_surveys_executive ON public.customer_surveys(executive_id);

-- 4. public.survey_documents
CREATE TABLE IF NOT EXISTS public.survey_documents (
  id              TEXT PRIMARY KEY,
  survey_id       TEXT NOT NULL REFERENCES public.customer_surveys(id) ON DELETE CASCADE,
  doc_type        TEXT NOT NULL CHECK (doc_type IN (
                    'LIGHT_BILL', 'AADHAAR', 'PAN', 'BANK_PASSBOOK',
                    'OWNERSHIP_PROOF', 'PASSPORT_PHOTO', 'CASTE_CERTIFICATE',
                    'RATION_CARD', 'DEATH_CERTIFICATE'
                  )),
  printed_name    TEXT NOT NULL DEFAULT '',
  photo_url       TEXT,
  r2_object_key   TEXT,
  photo_size_kb   INTEGER,
  is_size_valid   BOOLEAN NOT NULL DEFAULT true,
  legibility      TEXT NOT NULL DEFAULT 'CLEAR' CHECK (legibility IN ('CLEAR', 'UNCLEAR', 'MISSING')),
  match_result    TEXT NOT NULL DEFAULT 'NOT_ENTERED' CHECK (match_result IN ('MATCH', 'CLOSE_MATCH', 'MISMATCH', 'NOT_ENTERED')),
  query_note      TEXT NOT NULL DEFAULT '',
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (survey_id, doc_type)
);

CREATE INDEX IF NOT EXISTS idx_survey_documents_survey_id ON public.survey_documents(survey_id);

-- 5. public.installation_reports
CREATE TABLE IF NOT EXISTS public.installation_reports (
  id                                TEXT PRIMARY KEY,
  linked_survey_id                  TEXT REFERENCES public.customer_surveys(id) ON DELETE SET NULL,
  village_name                      TEXT NOT NULL DEFAULT '',
  consumer_no                       TEXT NOT NULL DEFAULT '',
  installation_date                 TEXT NOT NULL,
  consumer_name                     TEXT NOT NULL,
  mobile_no                         TEXT NOT NULL,
  system_capacity                   TEXT NOT NULL DEFAULT '1 KW',
  system_type                       TEXT NOT NULL DEFAULT 'ONGRID',
  products                          JSONB NOT NULL DEFAULT '[]'::jsonb,
  installer_name                    TEXT NOT NULL DEFAULT '',
  accountant_name                   TEXT NOT NULL DEFAULT '',
  manager_name                      TEXT NOT NULL DEFAULT '',
  supervisor_name                   TEXT NOT NULL DEFAULT '',
  signatures                        JSONB NOT NULL DEFAULT '{"installer": [], "customer": []}'::jsonb,
  inverter_serial_primary           TEXT NOT NULL DEFAULT '',
  inverter_serial_confirm           TEXT NOT NULL DEFAULT '',
  generation_meter_serial_primary   TEXT NOT NULL DEFAULT '',
  generation_meter_serial_confirm   TEXT NOT NULL DEFAULT '',
  panel_serial_numbers              JSONB NOT NULL DEFAULT '[]'::jsonb,
  updated_at_millis                 BIGINT NOT NULL,
  installer_executive_id            UUID REFERENCES public.executives(id) ON DELETE SET NULL,
  created_at                        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_installation_reports_linked_survey ON public.installation_reports(linked_survey_id);
CREATE INDEX IF NOT EXISTS idx_installation_reports_installer ON public.installation_reports(installer_executive_id);
CREATE INDEX IF NOT EXISTS idx_installation_reports_updated ON public.installation_reports(updated_at_millis DESC);

-- 6. public.installation_photos
CREATE TABLE IF NOT EXISTS public.installation_photos (
  id                    TEXT PRIMARY KEY,
  report_id             TEXT NOT NULL REFERENCES public.installation_reports(id) ON DELETE CASCADE,
  slot_index            INTEGER NOT NULL DEFAULT 0,
  caption               TEXT NOT NULL DEFAULT '',
  photo_url             TEXT,
  r2_object_key         TEXT,
  photo_size_kb         INTEGER,
  geo_info              TEXT NOT NULL DEFAULT '',
  is_camera_geo_stamped BOOLEAN NOT NULL DEFAULT false,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_installation_photos_report_id ON public.installation_photos(report_id, slot_index);

-- Auto-update updated_at trigger
CREATE OR REPLACE FUNCTION public.touch_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_customer_surveys_updated ON public.customer_surveys;
CREATE TRIGGER trg_customer_surveys_updated
BEFORE UPDATE ON public.customer_surveys
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

DROP TRIGGER IF EXISTS trg_survey_documents_updated ON public.survey_documents;
CREATE TRIGGER trg_survey_documents_updated
BEFORE UPDATE ON public.survey_documents
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

DROP TRIGGER IF EXISTS trg_installation_reports_updated ON public.installation_reports;
CREATE TRIGGER trg_installation_reports_updated
BEFORE UPDATE ON public.installation_reports
FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();

-- =============================================================================
-- Production Row Level Security (RLS) Policies (Section 4 of ANDROID_SYNC_STRUCTURE.md)
-- =============================================================================

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

ALTER TABLE public.executives ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.custom_villages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer_surveys ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.survey_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.installation_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.installation_photos ENABLE ROW LEVEL SECURITY;

-- 1. public.executives
DROP POLICY IF EXISTS "exec_select_policy" ON public.executives;
DROP POLICY IF EXISTS "exec_admin_all" ON public.executives;
DROP POLICY IF EXISTS "exec_self_select" ON public.executives;
DROP POLICY IF EXISTS "exec_self_update_login" ON public.executives;

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
