// Mock Sonatype IQ Server. Run with: npm run mock
import express from "express";
import cors from "cors";
import { lookup, evaluatePolicy } from "./data.js";

const app = express();
app.use(cors());
app.use(express.json());

const PORT = 8765;

app.get("/health", (_req, res) => res.json({ ok: true, service: "mock-iq" }));

// /api/v2/components/info?purl=...
app.get("/api/v2/components/info", (req, res) => {
  const purl = String(req.query.purl || "");
  if (!purl) return res.status(400).json({ error: "purl required" });
  const comp = lookup(purl);
  if (!comp) return res.status(404).json({ error: "not found" });
  res.json({ purl, ...comp });
});

// /api/v2/policy/evaluate { purl, orgId }
app.post("/api/v2/policy/evaluate", (req, res) => {
  const { purl } = req.body || {};
  const comp = lookup(purl);
  if (!comp) return res.status(404).json({ error: "not found" });
  res.json({ purl, ...evaluatePolicy(comp) });
});

// Combined verdict — what the extension actually calls
app.post("/api/v2/firewall/verdict", (req, res) => {
  const { purl } = req.body || {};
  const comp = lookup(purl);
  if (!comp) return res.status(404).json({ error: "not found" });
  const policy = evaluatePolicy(comp);
  const reachability =
    comp.cves.length > 0
      ? { reachable: comp.cves.some((c) => c.reachable), appsScanned: 12 }
      : undefined;
  res.json({
    component: { purl, ...comp },
    policy,
    reachability,
    fetchedAt: Date.now(),
    source: "mock",
  });
});

// /api/v2/remediation { purl }
app.post("/api/v2/remediation", (req, res) => {
  const { purl } = req.body || {};
  const comp = lookup(purl);
  if (!comp?.goldenVersion) return res.json({ purl, goldenVersion: null });
  res.json({ purl, goldenVersion: comp.goldenVersion });
});

// /api/v2/waivers { purl, reason }
app.post("/api/v2/waivers", (req, res) => {
  const { purl, reason } = req.body || {};
  if (!purl || !reason) return res.status(400).json({ error: "purl and reason required" });
  const id = `waiver-${Math.random().toString(36).slice(2, 10)}`;
  console.log(`[mock-iq] Waiver requested: ${id} for ${purl} — ${reason}`);
  res.json({ waiverId: id, status: "submitted", expiresInDays: 30 });
});

app.listen(PORT, () => {
  console.log(`Mock Sonatype IQ Server listening on http://localhost:${PORT}`);
  console.log(`Try:  curl 'http://localhost:${PORT}/api/v2/components/info?purl=pkg:npm/lodash@4.17.21'`);
});
