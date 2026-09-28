PRAGMA foreign_keys = ON;

CREATE TABLE stores (
  id TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL
);

CREATE TABLE memberships (
  store_id TEXT NOT NULL REFERENCES stores(id),
  firebase_uid TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('OWNER','MANAGER','CASHIER')),
  status TEXT NOT NULL CHECK (status IN ('ACTIVE','REVOKED')),
  created_at INTEGER NOT NULL,
  PRIMARY KEY (store_id, firebase_uid)
);

CREATE TABLE enrolled_devices (
  id TEXT PRIMARY KEY,
  store_id TEXT NOT NULL REFERENCES stores(id),
  assigned_uid TEXT NOT NULL,
  enrolled_by_uid TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('ACTIVE','REVOKED')),
  created_at INTEGER NOT NULL,
  last_seen_at INTEGER NOT NULL
);

CREATE TABLE invitations (
  token_hash TEXT PRIMARY KEY,
  store_id TEXT NOT NULL REFERENCES stores(id),
  email TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('MANAGER','CASHIER')),
  invited_by_uid TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('PENDING','REDEEMED','REVOKED')),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  redeemed_by_uid TEXT,
  redeemed_at INTEGER
);

CREATE INDEX invitations_store_status ON invitations(store_id,status,expires_at);

CREATE TABLE events (
  cursor INTEGER PRIMARY KEY AUTOINCREMENT,
  store_id TEXT NOT NULL REFERENCES stores(id),
  event_id TEXT NOT NULL,
  device_id TEXT NOT NULL REFERENCES enrolled_devices(id),
  schema_version INTEGER NOT NULL,
  idempotency_key TEXT NOT NULL,
  event_type TEXT NOT NULL,
  envelope_hash TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  UNIQUE (store_id, event_id),
  UNIQUE (store_id, idempotency_key)
);

CREATE INDEX events_store_cursor ON events(store_id, cursor);
