import test from "node:test";
import assert from "node:assert/strict";
import { generateKeyPairSync, sign } from "node:crypto";
import worker from "../src/index.mjs";

const { privateKey, publicKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const jwk = { ...publicKey.export({ format: "jwk" }), kid: "test-key", use: "sig", alg: "RS256" };
const originalFetch = globalThis.fetch;
globalThis.fetch = async () => new Response(JSON.stringify({ keys: [jwk] }), {
  headers: { "cache-control": "public, max-age=300", "content-type": "application/json" }
});

test.after(() => { globalThis.fetch = originalFetch; });

test("health is available without a Firebase token", async () => {
  const response = await worker.fetch(new Request("https://worker.test/health"), { ENVIRONMENT: "test" });
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: "ok", environment: "test" });
});

test("cloud routes reject missing and invalid authentication", async () => {
  for (const headers of [{}, { authorization: "Bearer invalid.token.signature" }]) {
    const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0", { headers }), env());
    assert.equal(response.status, 401);
  }
});

test("expired and wrong-project Firebase tokens are rejected", async () => {
  for (const token of [await idToken({ expiresOffset: -1 }), await idToken({ audience: "another-project" })]) {
    const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=device-12345678", {
      headers: authHeaders(token)
    }), env());
    assert.equal(response.status, 401);
  }
});

test("a signed member can append once, replay safely, and pull by cursor", async () => {
  const db = new MemoryDb();
  const token = await idToken();
  const event = categoryEvent();
  const headers = authHeaders(token);
  const posted = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [event] })
  }), env(db));
  assert.equal(posted.status, 200);
  assert.deepEqual(await posted.json(), {
    results: [{ eventId: event.eventId, status: "accepted" }], cursor: 1
  });
  assert.equal(db.events.length, 1);

  const replay = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [event] })
  }), env(db));
  assert.deepEqual((await replay.json()).results, [{ eventId: event.eventId, status: "replayed" }]);
  assert.equal(db.events.length, 1);

  const collision = structuredClone(event);
  collision.payload.name = "Different content under the same key";
  const collisionResponse = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [collision] })
  }), env(db));
  assert.equal(collisionResponse.status, 409);
  assert.deepEqual(await collisionResponse.json(), { error: { code: "idempotency_collision" } });
  assert.equal(db.events.length, 1);

  const pulled = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=device-12345678", { headers }), env(db));
  const page = await pulled.json();
  assert.equal(page.nextCursor, 1);
  assert.equal(page.events[0].payload.globalId, "category-global-1");
});

test("identical events in one batch are accepted once and replayed in order", async () => {
  const db = new MemoryDb();
  const event = categoryEvent();
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ events: [event, structuredClone(event)] })
  }), env(db));

  assert.equal(response.status, 200);
  assert.deepEqual((await response.json()).results, [
    { eventId: event.eventId, status: "accepted" },
    { eventId: event.eventId, status: "replayed" }
  ]);
  assert.equal(db.events.length, 1);
});

test("one batch cannot reuse an idempotency key for a different event", async () => {
  const db = new MemoryDb();
  const first = categoryEvent();
  const second = { ...structuredClone(first), eventId: "event-87654321" };
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify({ events: [first, second] })
  }), env(db));

  assert.equal(response.status, 409);
  assert.equal(db.events.length, 0);
});

test("event and batch reject unrecognized envelope fields", async () => {
  const event = categoryEvent();
  event.role = "OWNER";
  const headers = authHeaders(await idToken());
  const first = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [event] })
  }), env());
  assert.equal(first.status, 400);

  delete event.role;
  const second = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [event], ownerOverride: true })
  }), env());
  assert.equal(second.status, 400);
});

test("large streamed body is rejected without buffering the whole request", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()), body: "x".repeat(300_000)
  }), env());
  assert.equal(response.status, 413);
  assert.deepEqual(await response.json(), { error: { code: "body_too_large" } });
});

test("Udhaar audit actor must match authenticated membership and enrolled device", async () => {
  const db = new MemoryDb({ role: "CASHIER" });
  const event = udhaarEvent();
  const headers = authHeaders(await idToken());
  for (const change of [
    { actorUid: "owner-elsewhere" },
    { actorRole: "OWNER" },
    { actorDeviceId: "other-device-123" }
  ]) {
    const spoofed = structuredClone(event);
    Object.assign(spoofed.payload, change);
    const denied = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
      method: "POST", headers, body: JSON.stringify({ events: [spoofed] })
    }), env(db));
    assert.equal(denied.status, 400);
  }
  assert.equal(db.events.length, 0);

  const accepted = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [event] })
  }), env(db));
  assert.equal(accepted.status, 200);
  assert.equal(db.events.length, 1);
});

test("members cannot read or write a different store", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/other-store/changes?after=0", {
    headers: authHeaders(await idToken())
  }), env());
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: { code: "store_access_denied" } });
});

test("revoked devices cannot download store changes", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=device-12345678", {
    headers: authHeaders(await idToken())
  }), env(new MemoryDb({ deviceStore: "revoked" })));
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: { code: "device_not_enrolled" } });
});

test("revoked devices cannot upload and revoked membership blocks an active device", async () => {
  const revokedDevice = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ events: [categoryEvent()] })
  }), env(new MemoryDb({ deviceStore: "revoked" })));
  assert.equal(revokedDevice.status, 403);
  assert.deepEqual(await revokedDevice.json(), { error: { code: "device_not_enrolled" } });

  const db = new MemoryDb();
  db.memberships.get("store-a/owner-uid").status = "REVOKED";
  const revokedMember = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=device-12345678", {
    headers: authHeaders(await idToken())
  }), env(db));
  assert.equal(revokedMember.status, 403);
  assert.deepEqual(await revokedMember.json(), { error: { code: "store_access_denied" } });
});

test("a verified owner can create a store and first enrolled device", async () => {
  const db = new MemoryDb({ memberStore: "" });
  const response = await worker.fetch(new Request("https://worker.test/v1/stores", {
    method: "POST", headers: authHeaders(await idToken()), body: "{}"
  }), env(db));
  assert.equal(response.status, 201);
  const result = await response.json();
  assert.match(result.storeId, /^[a-f0-9]{32}$/);
  assert.match(result.deviceId, /^[a-f0-9]{32}$/);
  assert.equal(result.role, "OWNER");
  assert.equal(db.stores.length, 1);
  assert.equal(db.memberships.get(`${result.storeId}/owner-uid`).role, "OWNER");
  assert.equal(db.devices.get(`${result.storeId}/${result.deviceId}`).assignedUid, "owner-uid");
});

test("a verified owner can invite a member once and bind that member's device", async () => {
  const db = new MemoryDb();
  const ownerHeaders = authHeaders(await idToken());
  const inviteResponse = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites", {
    method: "POST", headers: ownerHeaders,
    body: JSON.stringify({ email: "cashier@example.com", memberRole: "CASHIER" })
  }), env(db));
  assert.equal(inviteResponse.status, 201);
  const invite = await inviteResponse.json();
  assert.ok(!db.invitations.has(invite.token));

  const redemption = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites/redeem", {
    method: "POST", headers: authHeaders(await idToken({ uid: "cashier-uid", email: "Cashier@example.com" })),
    body: JSON.stringify({ token: invite.token })
  }), env(db));
  assert.equal(redemption.status, 201);
  const membership = await redemption.json();
  assert.equal(membership.role, "CASHIER");
  assert.equal(db.memberships.get("store-a/cashier-uid").role, "CASHIER");
  assert.equal(db.devices.get(`store-a/${membership.deviceId}`).assignedUid, "cashier-uid");

  const replay = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites/redeem", {
    method: "POST", headers: authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" })),
    body: JSON.stringify({ token: invite.token })
  }), env(db));
  assert.equal(replay.status, 404);
});

test("a new invitation reactivates a revoked cashier without changing an active owner", async () => {
  const db = new MemoryDb();
  db.memberships.set("store-a/cashier-uid", { role: "CASHIER", status: "REVOKED" });
  const headers = authHeaders(await idToken());
  const invite = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites", {
    method: "POST", headers,
    body: JSON.stringify({ email: "cashier@example.com", memberRole: "MANAGER" })
  }), env(db));
  const token = (await invite.json()).token;
  const redeemed = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites/redeem", {
    method: "POST", headers: authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" })),
    body: JSON.stringify({ token })
  }), env(db));
  assert.equal(redeemed.status, 201);
  assert.deepEqual(db.memberships.get("store-a/cashier-uid"), { role: "MANAGER", status: "ACTIVE" });
  assert.deepEqual(db.memberships.get("store-a/owner-uid"), { role: "OWNER", status: "ACTIVE" });
});

test("owner approves a second phone for an active member and token is one use", async () => {
  const db = new MemoryDb();
  db.memberships.set("store-a/cashier-uid", { role: "CASHIER", status: "ACTIVE" });
  const inviteResponse = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/invites", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ memberUid: "cashier-uid", approverDeviceId: "device-12345678" })
  }), env(db));
  assert.equal(inviteResponse.status, 201);
  const invitation = await inviteResponse.json();
  assert.ok(!db.deviceInvitations.has(invitation.token));

  const wrongAccount = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ token: invitation.token })
  }), env(db));
  assert.equal(wrongAccount.status, 404);

  const cashierHeaders = authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" }));
  const enrolled = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers: cashierHeaders, body: JSON.stringify({ token: invitation.token })
  }), env(db));
  assert.equal(enrolled.status, 201);
  const device = await enrolled.json();
  assert.equal(device.role, "CASHIER");
  assert.equal(db.devices.get(`store-a/${device.deviceId}`).assignedUid, "cashier-uid");
  const replay = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers: cashierHeaders, body: JSON.stringify({ token: invitation.token })
  }), env(db));
  assert.equal(replay.status, 404);
  assert.equal([...db.devices.values()].filter(row => row.assignedUid === "cashier-uid").length, 1);
});

test("cashier cannot approve devices and revoked members cannot enroll", async () => {
  const db = new MemoryDb({ role: "CASHIER" });
  const denied = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/invites", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify({ memberUid: "owner-uid", approverDeviceId: "device-12345678" })
  }), env(db));
  assert.equal(denied.status, 403);

  db.memberships.get("store-a/owner-uid").role = "OWNER";
  db.memberships.set("store-a/cashier-uid", { role: "CASHIER", status: "ACTIVE" });
  const invited = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/invites", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify({ memberUid: "cashier-uid", approverDeviceId: "device-12345678" })
  }), env(db));
  const token = (await invited.json()).token;
  db.memberships.get("store-a/cashier-uid").status = "REVOKED";
  const revoked = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers: authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" })),
    body: JSON.stringify({ token })
  }), env(db));
  assert.equal(revoked.status, 403);
});

test("owner must approve from an already enrolled device", async () => {
  const db = new MemoryDb();
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/invites", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ memberUid: "owner-uid", approverDeviceId: "other-device-123" })
  }), env(db));
  assert.equal(response.status, 403);
  assert.equal(db.deviceInvitations.size, 0);
});

test("owner may enroll a second phone, while expired or other-store tokens fail", async () => {
  const db = new MemoryDb();
  const headers = authHeaders(await idToken());
  const created = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/invites", {
    method: "POST", headers, body: JSON.stringify({ memberUid: "owner-uid", approverDeviceId: "device-12345678" })
  }), env(db));
  assert.equal(created.status, 201);
  const token = (await created.json()).token;
  const otherStore = await worker.fetch(new Request("https://worker.test/v1/stores/other-store/devices/enroll", {
    method: "POST", headers, body: JSON.stringify({ token })
  }), env(db));
  assert.equal(otherStore.status, 403);

  const hash = [...db.deviceInvitations.keys()][0];
  db.deviceInvitations.get(hash).expiresAt = Date.now() - 1;
  const expired = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers, body: JSON.stringify({ token })
  }), env(db));
  assert.equal(expired.status, 404);
  db.deviceInvitations.get(hash).expiresAt = Date.now() + 60_000;
  const enrolled = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/devices/enroll", {
    method: "POST", headers, body: JSON.stringify({ token })
  }), env(db));
  assert.equal(enrolled.status, 201);
  assert.equal((await enrolled.json()).role, "OWNER");
});

test("an active membership cannot redeem another invitation to change its role", async () => {
  const db = new MemoryDb();
  db.memberships.set("store-a/cashier-uid", { role: "CASHIER", status: "ACTIVE" });
  const invite = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites", {
    method: "POST", headers: authHeaders(await idToken()),
    body: JSON.stringify({ email: "cashier@example.com", memberRole: "MANAGER" })
  }), env(db));
  const token = (await invite.json()).token;
  const redeemed = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites/redeem", {
    method: "POST", headers: authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" })),
    body: JSON.stringify({ token })
  }), env(db));
  assert.equal(redeemed.status, 409);
  assert.deepEqual(db.memberships.get("store-a/cashier-uid"), { role: "CASHIER", status: "ACTIVE" });
  assert.equal(db.invitations.size, 1);
  assert.equal([...db.invitations.values()][0].status, "PENDING");
});

test("unverified email cannot bootstrap a store or redeem a staff invitation", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores", {
    method: "POST", headers: authHeaders(await idToken({ emailVerified: false })), body: "{}"
  }), env(new MemoryDb({ memberStore: "" })));
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: { code: "verified_email_required" } });
});

test("owner revocation removes member and assigned-device access", async () => {
  const db = new MemoryDb();
  db.memberships.set("store-a/cashier-uid", { role: "CASHIER", status: "ACTIVE" });
  db.devices.set("store-a/cashier-device-123", { assignedUid: "cashier-uid", status: "ACTIVE" });
  const ownerRevoke = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/members/cashier-uid", {
    method: "DELETE", headers: authHeaders(await idToken())
  }), env(db));
  assert.equal(ownerRevoke.status, 200);
  assert.equal(db.memberships.get("store-a/cashier-uid").status, "REVOKED");
  assert.equal(db.devices.get("store-a/cashier-device-123").status, "REVOKED");

  const revokedMember = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=cashier-device-123", {
    headers: authHeaders(await idToken({ uid: "cashier-uid", email: "cashier@example.com" }))
  }), env(db));
  assert.equal(revokedMember.status, 403);
});

test("only the owner can invite staff or revoke members", async () => {
  const db = new MemoryDb({ role: "CASHIER" });
  const headers = authHeaders(await idToken());
  const invite = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/invites", {
    method: "POST", headers, body: JSON.stringify({ email: "staff@example.com", memberRole: "CASHIER" })
  }), env(db));
  assert.equal(invite.status, 403);

  const revoke = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/members/manager-uid", {
    method: "DELETE", headers
  }), env(db));
  assert.equal(revoke.status, 403);
});

test("owner cannot revoke its own final membership", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/members/owner-uid", {
    method: "DELETE", headers: authHeaders(await idToken())
  }), env());
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: { code: "owner_cannot_remove_self" } });
});

test("cashier cannot submit a stock adjustment", async () => {
  const body = { events: [categoryEvent("stock_adjustments")] };
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify(body)
  }), env(new MemoryDb({ role: "CASHIER" })));
  assert.equal(response.status, 403);
  assert.deepEqual(await response.json(), { error: { code: "role_denied" } });
});

test("payload secrets and unknown fields are rejected", async () => {
  const event = categoryEvent();
  event.payload.passwordHash = "must-not-sync";
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify({ events: [event] })
  }), env());
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: { code: "invalid_event" } });
});

test("schema v2 rejects local row IDs and requires stable relationship references", async () => {
  const headers = authHeaders(await idToken());
  for (const [type, invalidField] of [
    ["products", "categoryId"], ["sales", "customerId"],
    ["sale_items", "saleId"], ["udhaar_transactions", "customerId"],
    ["stock_adjustments", "productId"]
  ]) {
    const event = relatedEvent(type);
    event.payload[invalidField] = 42;
    const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
      method: "POST", headers, body: JSON.stringify({ events: [event] })
    }), env());
    assert.equal(response.status, 400, type);
  }
  const item = relatedEvent("sale_items");
  delete item.payload.saleGlobalId;
  const missing = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [item] })
  }), env());
  assert.equal(missing.status, 400);
  const legacy = categoryEvent();
  legacy.schemaVersion = 1;
  const legacyResponse = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers, body: JSON.stringify({ events: [legacy] })
  }), env());
  assert.equal(legacyResponse.status, 400);
});

test("schema v2 accepts global references for every related event type", async () => {
  const db = new MemoryDb();
  const events = ["products", "sales", "sale_items", "udhaar_transactions", "stock_adjustments"].map(relatedEvent);
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/events", {
    method: "POST", headers: authHeaders(await idToken()), body: JSON.stringify({ events })
  }), env(db));
  assert.equal(response.status, 200, JSON.stringify({ result: await response.clone().json(), events }));
  assert.equal(db.events.length, events.length);
  const pulled = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=0&deviceId=device-12345678", {
    headers: authHeaders(await idToken())
  }), env(db));
  const changes = (await pulled.json()).events;
  assert.equal(changes.length, events.length);
  for (const change of changes) {
    assert.deepEqual(change.payload, events.find(event => event.eventId === change.eventId).payload);
  }
});

test("invalid cursor is rejected after authentication", async () => {
  const response = await worker.fetch(new Request("https://worker.test/v1/stores/store-a/changes?after=-1&deviceId=device-12345678", {
    headers: authHeaders(await idToken())
  }), env());
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: { code: "invalid_cursor" } });
});

function env(db = new MemoryDb()) {
  return { DB: db, ENVIRONMENT: "test", FIREBASE_PROJECT_ID: "zenmart-test" };
}

function authHeaders(token) {
  return { authorization: `Bearer ${token}`, "content-type": "application/json" };
}

async function idToken({ audience = "zenmart-test", expiresOffset = 3600, uid = "owner-uid", email = "owner@example.com", emailVerified = true } = {}) {
  const now = Math.floor(Date.now() / 1000);
  const header = b64({ alg: "RS256", typ: "JWT", kid: "test-key" });
  const payload = b64({
    aud: audience, iss: "https://securetoken.google.com/zenmart-test", sub: uid, email, email_verified: emailVerified,
    iat: now, exp: now + expiresOffset, auth_time: now
  });
  const message = `${header}.${payload}`;
  return `${message}.${sign("RSA-SHA256", Buffer.from(message), privateKey).toString("base64url")}`;
}

function b64(value) { return Buffer.from(JSON.stringify(value)).toString("base64url"); }

function categoryEvent(type = "categories") {
  const payload = type === "stock_adjustments" ? {
    globalId: "category-global-1", mutationVersion: 1, mutationDeviceId: "device-12345678",
    idempotencyKey: "stock_adjustments/category-global-1/1", updatedAt: 1000,
    productGlobalId: "product-global-1", oldStock: 1, newStock: 0, difference: -1, reason: "sale"
  } : {
    globalId: "category-global-1", mutationVersion: 1, mutationDeviceId: "device-12345678",
    idempotencyKey: "categories/category-global-1/1", updatedAt: 1000, createdAt: 1000,
    name: "Grocery", isDeleted: false
  };
  return {
    eventId: "event-12345678", deviceId: "device-12345678", schemaVersion: 2,
    idempotencyKey: payload.idempotencyKey, eventType: type, payload, createdAt: 1000
  };
}

function udhaarEvent() {
  const event = categoryEvent();
  event.eventType = "udhaar_transactions";
  event.idempotencyKey = "udhaar_transactions/ledger-global-1/1";
  event.payload = {
    globalId: "ledger-global-1", eventId: "ledger-event-1", customerGlobalId: "customer-global-1",
    type: "CREDIT", amount: 100, balanceEffect: 100,
    actorUid: "owner-uid", actorName: "Cashier", actorRole: "CASHIER", actorDeviceId: event.deviceId,
    createdAt: 1000, updatedAt: 1000, isDeleted: false, mutationVersion: 1,
    mutationDeviceId: event.deviceId, idempotencyKey: event.idempotencyKey
  };
  return event;
}

function relatedEvent(type) {
  if (type === "udhaar_transactions") {
    const event = udhaarEvent();
    event.payload.actorRole = "OWNER";
    return event;
  }
  const event = categoryEvent();
  event.eventType = type;
  event.eventId = `event-${type}-12345678`;
  const globalId = `${type}-global-1`;
  event.idempotencyKey = `${type}/${globalId}/1`;
  event.payload = {
    globalId, mutationVersion: 1, mutationDeviceId: event.deviceId,
    idempotencyKey: event.idempotencyKey, updatedAt: 1000, isDeleted: false
  };
  if (type === "products") Object.assign(event.payload, {
    name: "Tea", categoryGlobalId: "category-global-1", mrp: 100,
    currentStock: 2, unit: "PCS", trackStock: true, lowStockAlertQty: 1,
    barcode: "tea-1", isActive: true, createdAt: 1000
  });
  if (type === "sales") Object.assign(event.payload, {
    billNumber: "B-1", totalAmount: 100, paymentMode: "CASH", paymentState: "RECEIVED",
    customerGlobalId: "customer-global-1", createdAt: 1000
  });
  if (type === "sale_items") Object.assign(event.payload, {
    saleGlobalId: "sales-global-1", productGlobalId: "products-global-1",
    productNameSnapshot: "Tea", quantity: 1, unit: "PCS", unitPrice: 100, lineTotal: 100
  });
  if (type === "stock_adjustments") Object.assign(event.payload, {
    productGlobalId: "products-global-1", oldStock: 2, newStock: 1,
    difference: -1, reason: "count", createdAt: 1000
  });
  return event;
}

class MemoryDb {
  constructor({ role = "OWNER", memberStore = "store-a", deviceStore = "store-a" } = {}) {
    this.role = role;
    this.memberStore = memberStore;
    this.deviceStore = deviceStore;
    this.events = [];
    this.stores = [];
    this.memberships = new Map();
    this.devices = new Map();
    this.invitations = new Map();
    this.deviceInvitations = new Map();
    if (memberStore) this.memberships.set(`${memberStore}/owner-uid`, { role, status: "ACTIVE" });
    if (deviceStore) this.devices.set(`${deviceStore}/device-12345678`, { assignedUid: "owner-uid", status: "ACTIVE" });
  }
  prepare(query) {
    const statement = { query, params: [], bind: (...params) => { statement.params = params; return statement; } };
    statement.first = async () => {
      const p = statement.params;
      if (query.includes("FROM memberships")) {
        const member = this.memberships.get(`${p[0]}/${p[1]}`);
        return member?.status === "ACTIVE" ? member : null;
      }
      if (query.includes("FROM enrolled_devices")) {
        const device = this.devices.get(`${p[1]}/${p[0]}`);
        return device?.assignedUid === p[2] && device.status === "ACTIVE" ? { id: p[0] } : null;
      }
      if (query.includes("FROM invitations")) {
        const invite = this.invitations.get(p[1]);
        return invite?.storeId === p[0] && invite.email === p[2] && invite.status === "PENDING" && invite.expiresAt > p[3] ? { role: invite.role } : null;
      }
      if (query.includes("SELECT event_id AS eventId")) return this.events.find(row => row.storeId === p[0] && (row.eventId === p[1] || row.idempotencyKey === p[2])) ?? null;
      if (query.includes("MAX(cursor)")) return { cursor: this.events.reduce((max, row) => Math.max(max, row.cursor), 0) };
      return null;
    };
    statement.all = async () => {
      const [storeId, after, limit] = statement.params;
      const rows = this.events.filter(row => row.storeId === storeId && row.cursor > after).slice(0, limit);
      return { results: rows.map(({ cursor, eventId, deviceId, schemaVersion, idempotencyKey, eventType, payloadJson, createdAt }) => ({ cursor, eventId, deviceId, schemaVersion, idempotencyKey, eventType, payloadJson, createdAt })) };
    };
    statement.run = async () => {
      const p = statement.params;
      if (query.includes("INSERT INTO invitations")) {
        const [tokenHash, storeId, email, role, invitedByUid, createdAt, expiresAt] = p;
        this.invitations.set(tokenHash, { storeId, email, role, invitedByUid, createdAt, expiresAt, status: "PENDING" });
        return { meta: { changes: 1 } };
      }
      if (query.includes("INSERT INTO device_invitations")) {
        const [tokenHash, storeId, assignedUid, invitedByUid, createdAt, expiresAt] = p;
        this.deviceInvitations.set(tokenHash, { storeId, assignedUid, invitedByUid, createdAt, expiresAt, status: "PENDING" });
        return { meta: { changes: 1 } };
      }
      if (query.includes("UPDATE enrolled_devices")) {
        const device = this.devices.get(`${p[0]}/${p[1]}`);
        if (!device || device.status !== "ACTIVE") return { meta: { changes: 0 } };
        device.status = "REVOKED";
        return { meta: { changes: 1 } };
      }
      return { meta: { changes: 1 } };
    };
    return statement;
  }
  async batch(statements) {
    if (statements[0]?.query.includes("UPDATE device_invitations")) {
      const [redeemedAt, nonce, tokenHash, storeId, assignedUid, now] = statements[0].params;
      const invite = this.deviceInvitations.get(tokenHash);
      const member = this.memberships.get(`${storeId}/${assignedUid}`);
      if (!invite || invite.storeId !== storeId || invite.assignedUid !== assignedUid ||
          invite.status !== "PENDING" || invite.expiresAt <= now || member?.status !== "ACTIVE") {
        return [{ meta: { changes: 0 } }, { meta: { changes: 0 } }];
      }
      invite.status = "REDEEMED"; invite.redeemedAt = redeemedAt; invite.redemptionNonce = nonce;
      const deviceId = statements[1].params[0];
      this.devices.set(`${storeId}/${deviceId}`, { assignedUid, status: "ACTIVE" });
      return [{ meta: { changes: 1 } }, { meta: { changes: 1 } }];
    }
    if (statements[0]?.query.includes("UPDATE invitations")) {
      const [uid, redeemedAt, nonce, storeId, tokenHash, email, now] = statements[0].params;
      const invitation = this.invitations.get(tokenHash);
      if (!invitation || invitation.storeId !== storeId || invitation.email !== email || invitation.status !== "PENDING" || invitation.expiresAt <= now) {
        return [{ meta: { changes: 0 } }, { meta: { changes: 0 } }, { meta: { changes: 0 } }];
      }
      const existing = this.memberships.get(`${storeId}/${uid}`);
      if (existing?.status === "ACTIVE") {
        return [{ meta: { changes: 0 } }, { meta: { changes: 0 } }, { meta: { changes: 0 } }];
      }
      invitation.status = "REDEEMED"; invitation.redeemedByUid = uid; invitation.redeemedAt = redeemedAt; invitation.redemptionNonce = nonce;
      const memberRole = invitation.role;
      this.memberships.set(`${storeId}/${uid}`, { role: memberRole, status: "ACTIVE" });
      const deviceId = statements[2].params[0];
      this.devices.set(`${storeId}/${deviceId}`, { assignedUid: uid, status: "ACTIVE" });
      return [{ meta: { changes: 1 } }, { meta: { changes: 1 } }, { meta: { changes: 1 } }];
    }
    if (statements[0]?.query.includes("INSERT INTO stores")) {
      const [storeId] = statements[0].params;
      const [, uid] = statements[1].params;
      const [deviceId, , assignedUid] = statements[2].params;
      this.stores.push(storeId);
      this.memberships.set(`${storeId}/${uid}`, { role: "OWNER", status: "ACTIVE" });
      this.devices.set(`${storeId}/${deviceId}`, { assignedUid, status: "ACTIVE" });
      return statements.map(() => ({ meta: { changes: 1 } }));
    }
    if (statements[0]?.query.includes("UPDATE memberships")) {
      const [storeId, uid] = statements[0].params;
      const member = this.memberships.get(`${storeId}/${uid}`);
      if (member) member.status = "REVOKED";
      for (const [key, device] of this.devices) {
        if (key.startsWith(`${storeId}/`) && device.assignedUid === uid) device.status = "REVOKED";
      }
      return statements.map(() => ({ meta: { changes: 1 } }));
    }
    for (const statement of statements) {
      const [storeId, eventId, deviceId, schemaVersion, idempotencyKey, eventType, envelopeHash, payloadJson, createdAt] = statement.params;
      this.events.push({ storeId, eventId, deviceId, schemaVersion, idempotencyKey, eventType, envelopeHash, payloadJson, createdAt, cursor: this.events.length + 1 });
    }
    return [];
  }
}
