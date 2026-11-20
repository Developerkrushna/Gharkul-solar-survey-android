// Supabase Edge Function: r2-presign
// Issues 5-minute presigned PUT URLs for Cloudflare R2 after verifying Supabase JWT + active executive.
// Required Edge Function Secrets (`supabase secrets set ...`):
//   R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET_NAME, R2_PUBLIC_BASE_URL

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { S3Client, PutObjectCommand, DeleteObjectCommand } from "npm:@aws-sdk/client-s3@3";
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner@3";

const ALLOWED_FOLDERS = new Set(["survey-docs", "survey-notecam", "installation-photos"]);

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return json({ error: "Missing Authorization header" }, 401);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: userData, error: userErr } = await supabase.auth.getUser();
    if (userErr || !userData?.user) {
      return json({ error: "Unauthorized user" }, 401);
    }

    const { data: execRow } = await supabase
      .from("executives")
      .select("id, is_active")
      .eq("auth_user_id", userData.user.id)
      .maybeSingle();

    if (!execRow || !execRow.is_active) {
      return json({ error: "Inactive or unregistered executive account" }, 403);
    }

    const body = await req.json();
    const action = (body.action ?? "presign").toString();

    const accountId = Deno.env.get("R2_ACCOUNT_ID") ?? "";
    const accessKeyId = Deno.env.get("R2_ACCESS_KEY_ID") ?? "";
    const secretAccessKey = Deno.env.get("R2_SECRET_ACCESS_KEY") ?? "";
    const bucketName = Deno.env.get("R2_BUCKET_NAME") ?? "";
    const publicBaseUrl = (Deno.env.get("R2_PUBLIC_BASE_URL") ?? "").replace(/\/+$/, "");

    if (!accountId || !accessKeyId || !secretAccessKey || !bucketName || !publicBaseUrl) {
      return json({ error: "Cloudflare R2 secrets are not configured on Edge Function" }, 500);
    }

    const s3 = new S3Client({
      region: "auto",
      endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
      credentials: { accessKeyId, secretAccessKey },
    });

    if (action === "delete") {
      const objectKeys: string[] = Array.isArray(body.object_keys) ? body.object_keys : [];
      for (const key of objectKeys) {
        if (typeof key === "string" && key.includes("/")) {
          await s3.send(new DeleteObjectCommand({ Bucket: bucketName, Key: key }));
        }
      }
      return json({ deleted: objectKeys.length }, 200);
    }

    const folder = (body.folder ?? "").toString();
    const recordId = (body.recordId ?? "").toString().replace(/[^a-zA-Z0-9_-]/g, "");
    const fileName = (body.fileName ?? "").toString().replace(/[^a-zA-Z0-9._-]/g, "");
    const contentType = (body.contentType ?? "image/jpeg").toString();

    if (!ALLOWED_FOLDERS.has(folder) || !recordId || !fileName) {
      return json({ error: "Invalid folder, recordId, or fileName" }, 400);
    }

    const objectKey = `${folder}/${recordId}/${fileName}`;
    const command = new PutObjectCommand({
      Bucket: bucketName,
      Key: objectKey,
      ContentType: contentType,
    });

    const uploadUrl = await getSignedUrl(s3, command, { expiresIn: 300 });
    const publicUrl = `${publicBaseUrl}/${objectKey}`;

    return json({ upload_url: uploadUrl, public_url: publicUrl, object_key: objectKey }, 200);
  } catch (err) {
    return json({ error: err instanceof Error ? err.message : "Unknown error" }, 500);
  }
});

function json(payload: unknown, status: number): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
