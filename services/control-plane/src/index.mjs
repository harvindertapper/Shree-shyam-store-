const MAX_BODY_BYTES = 256 * 1024;
const MAX_BATCH = 100;
const EVENT_TYPES = new Set([
  "categories", "products", "sales", "sale_items", "customers",
  "udhaar_transactions", "stock_adjustments"
]);
const PAYLOAD_FIELDS = Object.freeze({
  categories: new Set(["globalId", "name", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  products: new Set(["globalId", "name", "categoryGlobalId", "mrp", "sellingPrice", "purchasePrice", "moneyScale", "currentStock", "unit", "trackStock", "lowStockAlertQty", "barcode", "barcodeKey", "isActive", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  sales: new Set(["globalId", "billNumber", "totalAmount", "moneyScale", "paymentMode", "paymentState", "receivedAmount", "customerGlobalId", "note", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  sale_items: new Set(["globalId", "saleGlobalId", "productGlobalId", "productNameSnapshot", "quantity", "unit", "unitPrice", "lineTotal", "moneyScale", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  customers: new Set(["globalId", "name", "phone", "creditLimit", "moneyScale", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  udhaar_transactions: new Set(["globalId", "eventId", "customerGlobalId", "saleGlobalId", "type", "amount", "balanceEffect", "moneyScale", "note", "correctsEventId", "correctionReason", "actorUid", "actorName", "actorRole", "actorDeviceId", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]),
  stock_adjustments: new Set(["globalId", "productGlobalId", "oldStock", "newStock", "difference", "reason", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"])
});
const REQUIRED_PAYLOAD_FIELDS = Object.freeze({
  categories: ["globalId", "name", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  products: ["globalId", "name", "mrp", "currentStock", "unit", "trackStock", "lowStockAlertQty", "barcode", "isActive", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  sales: ["globalId", "billNumber", "totalAmount", "paymentMode", "paymentState", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  sale_items: ["globalId", "saleGlobalId", "productGlobalId", "productNameSnapshot", "quantity", "unit", "unitPrice", "lineTotal", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  customers: ["globalId", "name", "creditLimit", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  udhaar_transactions: ["globalId", "eventId", "customerGlobalId", "type", "amount", "balanceEffect", "actorUid", "actorName", "actorRole", "actorDeviceId", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"],
  stock_adjustments: ["globalId", "productGlobalId", "oldStock", "newStock", "difference", "reason", "createdAt", "updatedAt", "isDeleted", "mutationVersion", "mutationDeviceId", "idempotencyKey"]
});
const GLOBAL_REFERENCE_FIELDS = new Set(["categoryGlobalId", "customerGlobalId", "saleGlobalId", "productGlobalId"]);
const STRING_FIELDS = new Set(["globalId", ...GLOBAL_REFERENCE_FIELDS, "name", "unit", "barcode", "barcodeKey", "billNumber", "paymentMode", "paymentState", "note", "productNameSnapshot", "phone", "eventId", "type", "correctsEventId", "correctionReason", "actorUid", "actorName", "actorRole", "actorDeviceId", "reason", "mutationDeviceId", "idempotencyKey"]);
const BOOLEAN_FIELDS = new Set(["isDeleted", "trackStock", "isActive"]);
const INTEGER_FIELDS = new Set(["mrp", "sellingPrice", "purchasePrice", "moneyScale", "totalAmount", "receivedAmount", "unitPrice", "lineTotal", "creditLimit", "amount", "balanceEffect", "createdAt", "updatedAt", "mutationVersion"]);
const NUMBER_FIELDS = new Set(["currentStock", "lowStockAlertQty", "quantity", "oldStock", "newStock", "difference"]);
const WRITE_ROLES = Object.freeze({
  categories: new Set(["OWNER", "MANAGER"]),
  products: new Set(["OWNER", "MANAGER"]),
  sales: new Set(["OWNER", "MANAGER", "CASHIER"]),
  sale_items: new Set(["OWNER", "MANAGER", "CASHIER"]),
  customers: new Set(["OWNER", "MANAGER", "CASHIER"]),
  udhaar_transactions: new Set(["OWNER", "MANAGER", "CASHIER"]),
  stock_adjustments: new Set(["OWNER", "MANAGER"])
});

export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ status: "ok", environment: env.ENVIRONMENT ?? "unknown" });
      }
      const identity = await authenticate(request, env);
      if (!identity) return error(401, "unauthorized");
      if (request.method === "POST" && url.pathname === "/v1/stores") {
        return await createStore(request, env, identity);
      }
      const match = url.pathname.match(/^\/v1\/stores\/([A-Za-z0-9_-]{1,128})\/(events|changes|invites|invites\/redeem|devices\/invites|devices\/enroll|devices\/[A-Za-z0-9_-]{8,128}|members\/[A-Za-z0-9_-]{1,128})$/);
      if (!match) return error(404, "not_found");
      const [, storeId, resource] = match;
      if (resource === "invites/redeem" && request.method === "POST") {
        return await redeemInvite(request, env, storeId, identity);
      }
      const member = await env.DB.prepare(
        "SELECT role FROM memberships WHERE store_id = ? AND firebase_uid = ? AND status = 'ACTIVE'"
      ).bind(storeId, identity.uid).first();
      if (!member) return error(403, "store_access_denied");
      if (resource === "invites" && request.method === "POST") {
        return await createInvite(request, env, storeId, identity, member.role);
      }
      if (resource === "devices/invites" && request.method === "POST") {
        if (member.role !== "OWNER") return error(403, "role_denied");
        return await createDeviceInvite(request, env, storeId, identity);
      }
      if (resource === "devices/enroll" && request.method === "POST") {
        return await redeemDeviceInvite(request, env, storeId, identity, member.role);
      }
      if (resource.startsWith("devices/") && request.method === "DELETE") {
        if (member.role !== "OWNER") return error(403, "role_denied");
        const deviceId = resource.slice("devices/".length);
        const result = await env.DB.prepare(
          "UPDATE enrolled_devices SET status = 'REVOKED' WHERE store_id = ? AND id = ? AND status = 'ACTIVE'"
        ).bind(storeId, deviceId).run();
        return result.meta?.changes ? json({ status: "revoked", deviceId }) : error(404, "device_not_found");
      }
      if (resource.startsWith("members/") && request.method === "DELETE") {
        if (member.role !== "OWNER") return error(403, "role_denied");
        const memberUid = resource.slice("members/".length);
        if (memberUid === identity.uid) return error(400, "owner_cannot_remove_self");
        const target = await env.DB.prepare(
          "SELECT role FROM memberships WHERE store_id = ? AND firebase_uid = ? AND status = 'ACTIVE'"
        ).bind(storeId, memberUid).first();
        if (!target || target.role === "OWNER") return error(404, "member_not_found");
        await env.DB.batch([
          env.DB.prepare("UPDATE memberships SET status = 'REVOKED' WHERE store_id = ? AND firebase_uid = ? AND status = 'ACTIVE'").bind(storeId, memberUid),
          env.DB.prepare("UPDATE enrolled_devices SET status = 'REVOKED' WHERE store_id = ? AND assigned_uid = ? AND status = 'ACTIVE'").bind(storeId, memberUid)
        ]);
        return json({ status: "revoked", memberUid });
      }
      if (resource === "events" && request.method === "POST") {
        if (member.role !== "OWNER" && member.role !== "MANAGER" && member.role !== "CASHIER") return error(403, "role_denied");
        return await appendEvents(request, env, storeId, member.role, identity.uid);
      }
      if (resource === "changes" && request.method === "GET") {
        const deviceId = url.searchParams.get("deviceId") ?? "";
        if (!/^[A-Za-z0-9_-]{8,128}$/.test(deviceId)) return error(400, "invalid_device_id");
        const enrolled = await env.DB.prepare(
          "SELECT id FROM enrolled_devices WHERE id = ? AND store_id = ? AND assigned_uid = ? AND status = 'ACTIVE'"
        ).bind(deviceId, storeId, identity.uid).first();
        if (!enrolled) return error(403, "device_not_enrolled");
        const raw = url.searchParams.get("after") ?? "0";
        if (!/^\d{1,15}$/.test(raw)) return error(400, "invalid_cursor");
        const after = Number(raw);
        const result = await env.DB.prepare(
          "SELECT cursor, event_id AS eventId, device_id AS deviceId, schema_version AS schemaVersion, idempotency_key AS idempotencyKey, event_type AS eventType, payload_json AS payloadJson, created_at AS createdAt FROM events WHERE store_id = ? AND cursor > ? ORDER BY cursor ASC LIMIT ?"
        ).bind(storeId, after, MAX_BATCH).all();
        const events = (result.results ?? []).map(row => ({
          cursor: row.cursor, eventId: row.eventId, deviceId: row.deviceId,
          schemaVersion: row.schemaVersion, idempotencyKey: row.idempotencyKey,
          eventType: row.eventType, payload: JSON.parse(row.payloadJson), createdAt: row.createdAt
        }));
        return json({ events, nextCursor: events.at(-1)?.cursor ?? after, hasMore: events.length === MAX_BATCH });
      }
      return error(405, "method_not_allowed");
    } catch (cause) {
      return error(cause?.status ?? 500, cause?.code ?? "internal_error");
    }
  }
};

async function createStore(request, env, identity) {
  if (!identity.emailVerified) return error(403, "verified_email_required");
  const body = await readJson(request);
  if (body.error) return body.error;
  if (Object.keys(body.value).length !== 0) return error(400, "invalid_store_request");
  const now = Date.now();
  const storeId = crypto.randomUUID().replaceAll("-", "");
  const deviceId = crypto.randomUUID().replaceAll("-", "");
  await env.DB.batch([
    env.DB.prepare("INSERT INTO stores (id,created_at) VALUES (?,?)").bind(storeId, now),
    env.DB.prepare("INSERT INTO memberships (store_id,firebase_uid,role,status,created_at) VALUES (?,?,'OWNER','ACTIVE',?)").bind(storeId, identity.uid, now),
    env.DB.prepare("INSERT INTO enrolled_devices (id,store_id,assigned_uid,enrolled_by_uid,status,created_at,last_seen_at) VALUES (?,?,?,?,'ACTIVE',?,?)").bind(deviceId, storeId, identity.uid, identity.uid, now, now)
  ]);
  return json({ storeId, deviceId, role: "OWNER" }, 201);
}

async function createInvite(request, env, storeId, identity, role) {
  if (role !== "OWNER") return error(403, "role_denied");
  if (!identity.emailVerified) return error(403, "verified_email_required");
  const body = await readJson(request);
  if (body.error) return body.error;
  const { email, memberRole } = body.value;
  const normalizedEmail = typeof email === "string" ? email.trim().toLowerCase() : "";
  if (Object.keys(body.value).length !== 2 || !Object.hasOwn(body.value, "email") || !Object.hasOwn(body.value, "memberRole") ||
      !normalizedEmail || normalizedEmail.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail) || !["MANAGER", "CASHIER"].includes(memberRole)) {
    return error(400, "invalid_invitation");
  }
  const token = randomToken();
  const tokenHash = await sha256(token);
  const createdAt = Date.now();
  const expiresAt = createdAt + INVITATION_TTL_MS;
  try {
    await env.DB.prepare(
      "INSERT INTO invitations (token_hash,store_id,email,role,invited_by_uid,status,created_at,expires_at) VALUES (?,?,?,?,?,'PENDING',?,?)"
    ).bind(tokenHash, storeId, normalizedEmail, memberRole, identity.uid, createdAt, expiresAt).run();
  } catch {
    return error(409, "invitation_conflict");
  }
  return json({ token, email: normalizedEmail, role: memberRole, expiresAt }, 201);
}

async function redeemInvite(request, env, storeId, identity) {
  if (!identity.emailVerified || !identity.email) return error(403, "verified_email_required");
  const body = await readJson(request);
  if (body.error) return body.error;
  if (Object.keys(body.value).length !== 1 || typeof body.value.token !== "string" || body.value.token.length < 32 || body.value.token.length > 256) {
    return error(400, "invalid_invitation");
  }
  const tokenHash = await sha256(body.value.token);
  const invitation = await env.DB.prepare(
    "SELECT role FROM invitations WHERE store_id = ? AND token_hash = ? AND email = ? AND status = 'PENDING' AND expires_at > ?"
  ).bind(storeId, tokenHash, identity.email.toLowerCase(), Date.now()).first();
  if (!invitation) return error(404, "invitation_not_found_or_expired");
  const now = Date.now();
  const deviceId = crypto.randomUUID().replaceAll("-", "");
  const redemptionNonce = crypto.randomUUID();
  try {
    const results = await env.DB.batch([
      env.DB.prepare("UPDATE invitations SET status='REDEEMED',redeemed_by_uid=?,redeemed_at=?,redemption_nonce=? WHERE store_id=? AND token_hash=? AND email=? AND status='PENDING' AND expires_at>? AND NOT EXISTS (SELECT 1 FROM memberships WHERE store_id=? AND firebase_uid=? AND status='ACTIVE')").bind(identity.uid, now, redemptionNonce, storeId, tokenHash, identity.email.toLowerCase(), now, storeId, identity.uid),
      env.DB.prepare("INSERT INTO memberships (store_id,firebase_uid,role,status,created_at) SELECT store_id,?,role,'ACTIVE',? FROM invitations WHERE store_id=? AND token_hash=? AND redeemed_by_uid=? AND redemption_nonce=? AND status='REDEEMED' ON CONFLICT(store_id,firebase_uid) DO UPDATE SET role=excluded.role,status='ACTIVE' WHERE memberships.status='REVOKED'").bind(identity.uid, now, storeId, tokenHash, identity.uid, redemptionNonce),
      env.DB.prepare("INSERT INTO enrolled_devices (id,store_id,assigned_uid,enrolled_by_uid,status,created_at,last_seen_at) SELECT ?,store_id,?,invited_by_uid,'ACTIVE',?,? FROM invitations WHERE store_id=? AND token_hash=? AND redeemed_by_uid=? AND redemption_nonce=? AND status='REDEEMED'").bind(deviceId, identity.uid, now, now, storeId, tokenHash, identity.uid, redemptionNonce)
    ]);
    if (!results[0]?.meta?.changes) return error(409, "invitation_already_used");
    if (!results[1]?.meta?.changes || !results[2]?.meta?.changes) return error(409, "membership_conflict");
  } catch {
    return error(409, "membership_conflict");
  }
  return json({ storeId, deviceId, role: invitation.role }, 201);
}

async function readJson(request) {
  const contentLength = Number(request.headers.get("content-length") ?? 0);
  if (!Number.isFinite(contentLength) || contentLength < 0) return { error: error(400, "invalid_content_length") };
  if (contentLength > MAX_BODY_BYTES) return { error: error(413, "body_too_large") };
  const reader = request.body?.getReader();
  const decoder = new TextDecoder();
  let raw = "";
  let bytes = 0;
  if (reader) {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      bytes += value.byteLength;
      if (bytes > MAX_BODY_BYTES) {
        await reader.cancel();
        return { error: error(413, "body_too_large") };
      }
      raw += decoder.decode(value, { stream: true });
    }
    raw += decoder.decode();
  }
  try {
    const value = JSON.parse(raw);
    return value && typeof value === "object" && !Array.isArray(value)
      ? { value }
      : { error: error(400, "invalid_json") };
  } catch {
    return { error: error(400, "invalid_json") };
  }
}

function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

async function sha256(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, "0")).join("");
}

const INVITATION_TTL_MS = 48 * 60 * 60 * 1000;
const DEVICE_INVITATION_TTL_MS = 15 * 60 * 1000;

async function createDeviceInvite(request, env, storeId, identity) {
  if (!identity.emailVerified) return error(403, "verified_email_required");
  const body = await readJson(request);
  if (body.error) return body.error;
  const memberUid = body.value.memberUid;
  const approverDeviceId = body.value.approverDeviceId;
  if (Object.keys(body.value).length !== 2 || typeof memberUid !== "string" ||
      memberUid.length < 1 || memberUid.length > 128 ||
      typeof approverDeviceId !== "string" || !/^[A-Za-z0-9_-]{8,128}$/.test(approverDeviceId)) {
    return error(400, "invalid_device_invitation");
  }
  const approverDevice = await env.DB.prepare(
    "SELECT id FROM enrolled_devices WHERE id = ? AND store_id = ? AND assigned_uid = ? AND status = 'ACTIVE'"
  ).bind(approverDeviceId, storeId, identity.uid).first();
  if (!approverDevice) return error(403, "device_not_enrolled");
  const target = await env.DB.prepare(
    "SELECT role FROM memberships WHERE store_id = ? AND firebase_uid = ? AND status = 'ACTIVE'"
  ).bind(storeId, memberUid).first();
  if (!target) return error(404, "member_not_found");
  const token = randomToken();
  const now = Date.now();
  await env.DB.prepare(
    "INSERT INTO device_invitations (token_hash,store_id,assigned_uid,invited_by_uid,status,created_at,expires_at) VALUES (?,?,?,?,'PENDING',?,?)"
  ).bind(await sha256(token), storeId, memberUid, identity.uid, now, now + DEVICE_INVITATION_TTL_MS).run();
  return json({ token, memberUid, expiresAt: now + DEVICE_INVITATION_TTL_MS }, 201);
}

async function redeemDeviceInvite(request, env, storeId, identity, role) {
  if (!identity.emailVerified) return error(403, "verified_email_required");
  const body = await readJson(request);
  if (body.error) return body.error;
  if (Object.keys(body.value).length !== 1 || typeof body.value.token !== "string" ||
      body.value.token.length < 32 || body.value.token.length > 256) return error(400, "invalid_device_invitation");
  const tokenHash = await sha256(body.value.token);
  const now = Date.now();
  const deviceId = crypto.randomUUID().replaceAll("-", "");
  const nonce = crypto.randomUUID();
  const results = await env.DB.batch([
    env.DB.prepare("UPDATE device_invitations SET status='REDEEMED',redeemed_at=?,redemption_nonce=? WHERE token_hash=? AND store_id=? AND assigned_uid=? AND status='PENDING' AND expires_at>? AND EXISTS (SELECT 1 FROM memberships WHERE store_id=? AND firebase_uid=? AND status='ACTIVE')")
      .bind(now, nonce, tokenHash, storeId, identity.uid, now, storeId, identity.uid),
    env.DB.prepare("INSERT INTO enrolled_devices (id,store_id,assigned_uid,enrolled_by_uid,status,created_at,last_seen_at) SELECT ?,store_id,assigned_uid,invited_by_uid,'ACTIVE',?,? FROM device_invitations WHERE token_hash=? AND store_id=? AND assigned_uid=? AND status='REDEEMED' AND redemption_nonce=? AND EXISTS (SELECT 1 FROM memberships WHERE store_id=? AND firebase_uid=? AND status='ACTIVE')")
      .bind(deviceId, now, now, tokenHash, storeId, identity.uid, nonce, storeId, identity.uid)
  ]);
  if (!results[0]?.meta?.changes || !results[1]?.meta?.changes) return error(404, "device_invitation_not_found_or_expired");
  return json({ storeId, deviceId, role }, 201);
}

async function appendEvents(request, env, storeId, role, uid) {
  const bodyResult = await readJson(request);
  if (bodyResult.error) return bodyResult.error;
  const body = bodyResult.value;
  if (Object.keys(body).length !== 1) return error(400, "invalid_batch");
  if (!Array.isArray(body.events) || body.events.length === 0 || body.events.length > MAX_BATCH) return error(400, "invalid_batch_size");
  let batchDeviceId;
  for (const event of body.events) {
    if (!event || !EVENT_TYPES.has(event.eventType)) return error(400, "invalid_event");
    if (!WRITE_ROLES[event.eventType]?.has(role)) return error(403, "role_denied");
    if (!isValidEvent(event, role, uid)) return error(400, "invalid_event");
    if (batchDeviceId && batchDeviceId !== event.deviceId) return error(400, "mixed_device_batch");
    batchDeviceId = event.deviceId;
  }
  const device = await env.DB.prepare(
    "SELECT id FROM enrolled_devices WHERE id = ? AND store_id = ? AND assigned_uid = ? AND status = 'ACTIVE'"
  ).bind(batchDeviceId, storeId, uid).first();
  if (!device) return error(403, "device_not_enrolled");
  const accepted = [];
  const statements = [];
  const seenEventIds = new Map();
  const seenKeys = new Map();
  for (const event of body.events) {
    const envelopeHash = await hashEnvelope(event);
    const seen = seenEventIds.get(event.eventId) ?? seenKeys.get(event.idempotencyKey);
    if (seen) {
      if (seen.eventId !== event.eventId || seen.idempotencyKey !== event.idempotencyKey || seen.envelopeHash !== envelopeHash) return error(409, "idempotency_collision");
      accepted.push({ eventId: event.eventId, status: "replayed" });
      continue;
    }
    const existing = await env.DB.prepare(
      "SELECT event_id AS eventId, idempotency_key AS idempotencyKey, envelope_hash AS envelopeHash FROM events WHERE store_id = ? AND (event_id = ? OR idempotency_key = ?) LIMIT 1"
    ).bind(storeId, event.eventId, event.idempotencyKey).first();
    if (existing) {
      if (existing.eventId !== event.eventId || existing.idempotencyKey !== event.idempotencyKey || existing.envelopeHash !== envelopeHash) return error(409, "idempotency_collision");
      seenEventIds.set(event.eventId, existing);
      seenKeys.set(event.idempotencyKey, existing);
      accepted.push({ eventId: event.eventId, status: "replayed" });
      continue;
    }
    const payload = JSON.stringify(event.payload);
    statements.push(env.DB.prepare(
      "INSERT INTO events (store_id,event_id,device_id,schema_version,idempotency_key,event_type,envelope_hash,payload_json,created_at) VALUES (?,?,?,?,?,?,?,?,?)"
    ).bind(storeId, event.eventId, event.deviceId, event.schemaVersion, event.idempotencyKey, event.eventType, envelopeHash, payload, event.createdAt));
    const current = { eventId: event.eventId, idempotencyKey: event.idempotencyKey, envelopeHash };
    seenEventIds.set(event.eventId, current);
    seenKeys.set(event.idempotencyKey, current);
    accepted.push({ eventId: event.eventId, status: "accepted" });
  }
  if (statements.length) {
    try { await env.DB.batch(statements); }
    catch { return error(409, "batch_conflict_retry_safe"); }
  }
  return json({ results: accepted, cursor: await currentCursor(env, storeId) });
}

function isValidEvent(e, role, uid) {
  if (Object.keys(e).length !== 7 ||
      Object.keys(e).some(key => !["eventId", "deviceId", "schemaVersion", "idempotencyKey", "eventType", "payload", "createdAt"].includes(key))) return false;
  if (!(e && typeof e === "object" && typeof e.eventId === "string" && e.eventId.length >= 8 && e.eventId.length <= 128 &&
    typeof e.deviceId === "string" && e.deviceId.length >= 8 && e.deviceId.length <= 128 &&
    Number.isSafeInteger(e.schemaVersion) && e.schemaVersion === 2 &&
    typeof e.idempotencyKey === "string" && e.idempotencyKey.length >= 8 && e.idempotencyKey.length <= 200 &&
    EVENT_TYPES.has(e.eventType) && Number.isSafeInteger(e.createdAt) && e.createdAt > 0 && e.createdAt <= Date.now() + 5 * 60_000 &&
    e.payload && typeof e.payload === "object" && !Array.isArray(e.payload))) return false;
  const allowed = PAYLOAD_FIELDS[e.eventType];
  if (!allowed || Object.keys(e.payload).some(key => !allowed.has(key))) return false;
  if (REQUIRED_PAYLOAD_FIELDS[e.eventType].some(key => !Object.hasOwn(e.payload, key) || e.payload[key] === null)) return false;
  if (e.payload.mutationDeviceId !== e.deviceId || e.payload.idempotencyKey !== e.idempotencyKey ||
      typeof e.payload.globalId !== "string" || e.payload.globalId.length < 1 || e.payload.globalId.length > 128 ||
      !Number.isSafeInteger(e.payload.mutationVersion) || e.payload.mutationVersion < 1 ||
      e.idempotencyKey !== `${e.eventType}/${e.payload.globalId}/${e.payload.mutationVersion}` ||
      !Number.isSafeInteger(e.payload.updatedAt) || e.payload.updatedAt < 1) return false;
  for (const [field, value] of Object.entries(e.payload)) {
    if (value === null) {
      if (REQUIRED_PAYLOAD_FIELDS[e.eventType].includes(field)) return false;
      continue;
    }
    if (STRING_FIELDS.has(field) && (typeof value !== "string" || value.length > 4096)) return false;
    if (GLOBAL_REFERENCE_FIELDS.has(field) && (value.length < 1 || value.length > 128)) return false;
    if (BOOLEAN_FIELDS.has(field) && typeof value !== "boolean") return false;
    if (INTEGER_FIELDS.has(field) && (!Number.isSafeInteger(value) ||
        (["sellingPrice", "purchasePrice", "receivedAmount"].includes(field) && value < 0))) return false;
    if (NUMBER_FIELDS.has(field) && (typeof value !== "number" || !Number.isFinite(value))) return false;
    if (!STRING_FIELDS.has(field) && !BOOLEAN_FIELDS.has(field) && !INTEGER_FIELDS.has(field) && !NUMBER_FIELDS.has(field)) return false;
  }
  if ("moneyScale" in e.payload && e.payload.moneyScale !== 2) return false;
  if (e.eventType === "sales" && (!new Set(["CASH", "UPI", "UDHAAR"]).has(e.payload.paymentMode) ||
      !new Set(["PENDING", "RECEIVED", "FAILED", "NOT_REQUIRED", "PARTIALLY_REFUNDED", "REFUNDED"]).has(e.payload.paymentState))) return false;
  if (e.eventType === "udhaar_transactions" && (!new Set(["CREDIT", "PAYMENT", "REVERSAL", "CORRECTION"]).has(e.payload.type) ||
      e.payload.actorUid !== uid || e.payload.actorRole !== role || e.payload.actorDeviceId !== e.deviceId)) return false;
  return true;
}

async function hashEnvelope(event) {
  const canonical = JSON.stringify({
    schemaVersion: event.schemaVersion,
    deviceId: event.deviceId,
    eventType: event.eventType,
    createdAt: event.createdAt,
    payload: Object.fromEntries(Object.keys(event.payload).sort().map(key => [key, event.payload[key]]))
  });
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(canonical));
  return Array.from(new Uint8Array(digest), value => value.toString(16).padStart(2, "0")).join("");
}

async function currentCursor(env, storeId) {
  const row = await env.DB.prepare("SELECT COALESCE(MAX(cursor),0) AS cursor FROM events WHERE store_id = ?").bind(storeId).first();
  return row?.cursor ?? 0;
}

async function authenticate(request, env) {
  const authorization = request.headers.get("authorization") ?? "";
  const match = authorization.match(/^Bearer ([A-Za-z0-9._-]+)$/);
  if (!match || !env.FIREBASE_PROJECT_ID) return null;
  const [encodedHeader, encodedPayload, signature] = match[1].split(".");
  if (!signature) return null;
  let header, claims;
  try {
    header = decodePart(encodedHeader); claims = decodePart(encodedPayload);
  } catch { return null; }
  const now = Math.floor(Date.now() / 1000);
  if (header.alg !== "RS256" || typeof header.kid !== "string" || claims.iss !== `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}` ||
      claims.aud !== env.FIREBASE_PROJECT_ID || typeof claims.sub !== "string" || claims.sub.length === 0 || claims.sub.length > 128 ||
      !Number.isInteger(claims.exp) || claims.exp <= now || !Number.isInteger(claims.iat) || claims.iat > now + 60) return null;
  const keys = await firebaseKeys();
  const jwk = keys.find(key => key.kid === header.kid && key.use === "sig");
  if (!jwk) return null;
  const key = await crypto.subtle.importKey("jwk", jwk, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const valid = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", key, base64UrlDecode(signature), new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`));
  if (!valid) return null;
  return {
    uid: claims.sub,
    email: typeof claims.email === "string" ? claims.email.trim().toLowerCase() : "",
    emailVerified: claims.email_verified === true
  };
}

let keyCache;
async function firebaseKeys() {
  if (!keyCache || keyCache.until < Date.now()) {
    const response = await fetch("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com");
    if (!response.ok) throw new Error("Firebase signing keys unavailable");
    const cacheControl = response.headers.get("cache-control") ?? "";
    const age = Number(cacheControl.match(/max-age=(\d+)/)?.[1] ?? 300);
    keyCache = { keys: (await response.json()).keys, until: Date.now() + Math.min(age, 3600) * 1000 };
  }
  return keyCache.keys;
}

function decodePart(value) { return JSON.parse(new TextDecoder().decode(base64UrlDecode(value))); }
function base64UrlDecode(value) { return Uint8Array.from(atob(value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=")), c => c.charCodeAt(0)); }
function json(value, status = 200) { return new Response(JSON.stringify(value), { status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" } }); }
function error(status, code) { return json({ error: { code } }, status); }
