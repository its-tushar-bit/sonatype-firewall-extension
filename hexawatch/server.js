/* eslint-disable no-console */
const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");

const PORT = Number(process.env.PORT || 9090);
const pool = new Pool({
  host: process.env.PGHOST || "localhost",
  port: Number(process.env.PGPORT || 5455),
  database: process.env.PGDATABASE || "hexawatch",
  user: process.env.PGUSER || "hexawatch",
  password: process.env.PGPASSWORD || "hexawatch",
});

const app = express();
app.use(cors()); // dev-only: allow chrome-extension:// origins
app.use(express.json({ limit: "128kb" }));

app.get("/health", async (_req, res) => {
  try {
    await pool.query("SELECT 1");
    res.json({ ok: true, service: "hexawatch" });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

function requireUserCode(req, res) {
  const userCode = String(req.query.userCode || req.body?.userCode || "").trim();
  if (!userCode) {
    res.status(400).json({ ok: false, error: "userCode is required" });
    return null;
  }
  return userCode;
}

function toApiRow(row) {
  return {
    userCode: row.user_code,
    passCode: row.pass_code || "",
    iqServerUrl: row.iq_server_url || "",
    vrmId: row.vrm_id || "",
    vrmName: row.vrm_name || "",
    selectedRepoIds: Array.isArray(row.selected_repo_ids) ? row.selected_repo_ids : [],
    selectedRepos: Array.isArray(row.selected_repos) ? row.selected_repos : [],
    updatedAt: row.updated_at,
  };
}

app.get("/extension/config", async (req, res) => {
  const userCode = requireUserCode(req, res);
  if (!userCode) return;
  try {
    const { rows } = await pool.query(
      `SELECT user_code, pass_code, iq_server_url, vrm_id, vrm_name, selected_repo_ids, selected_repos, updated_at
       FROM extension_config
       WHERE user_code = $1`,
      [userCode],
    );
    if (rows.length === 0) {
      res.status(404).json({ ok: false, error: "No config for that userCode" });
      return;
    }
    res.json({ ok: true, config: toApiRow(rows[0]) });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.put("/extension/config", async (req, res) => {
  const userCode = requireUserCode(req, res);
  if (!userCode) return;
  const {
    passCode = "",
    iqServerUrl = "",
    vrmId = "",
    vrmName = "",
    selectedRepoIds = [],
    selectedRepos = [],
  } = req.body || {};
  if (!Array.isArray(selectedRepoIds)) {
    res.status(400).json({ ok: false, error: "selectedRepoIds must be an array" });
    return;
  }
  if (!Array.isArray(selectedRepos)) {
    res.status(400).json({ ok: false, error: "selectedRepos must be an array" });
    return;
  }
  try {
    const { rows } = await pool.query(
      `INSERT INTO extension_config (user_code, pass_code, iq_server_url, vrm_id, vrm_name, selected_repo_ids, selected_repos)
       VALUES ($1, $2, $3, $4, $5, $6::jsonb, $7::jsonb)
       ON CONFLICT (user_code) DO UPDATE
         SET pass_code = EXCLUDED.pass_code,
             iq_server_url = EXCLUDED.iq_server_url,
             vrm_id = EXCLUDED.vrm_id,
             vrm_name = EXCLUDED.vrm_name,
             selected_repo_ids = EXCLUDED.selected_repo_ids,
             selected_repos = EXCLUDED.selected_repos,
             updated_at = now()
       RETURNING user_code, pass_code, iq_server_url, vrm_id, vrm_name, selected_repo_ids, selected_repos, updated_at`,
      [
        userCode,
        passCode,
        iqServerUrl,
        vrmId,
        vrmName,
        JSON.stringify(selectedRepoIds),
        JSON.stringify(selectedRepos),
      ],
    );
    res.json({ ok: true, config: toApiRow(rows[0]) });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.listen(PORT, () => {
  console.log(`[hexawatch] listening on http://localhost:${PORT}`);
});
