CREATE TABLE device_invitations (
  token_hash TEXT PRIMARY KEY,
  store_id TEXT NOT NULL REFERENCES stores(id),
  assigned_uid TEXT NOT NULL,
  invited_by_uid TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('PENDING','REDEEMED','REVOKED')),
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  redeemed_at INTEGER,
  redemption_nonce TEXT
);

CREATE INDEX device_invitations_store_status
  ON device_invitations(store_id,status,expires_at);
