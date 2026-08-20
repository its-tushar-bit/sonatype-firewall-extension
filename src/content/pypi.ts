import { getSettings, injectBadge, injectUnsupportedBadge, requestVerdict } from "./shared";
import { RuntimeMessage } from "../types";

async function runScan() {
  // /project/<name>/   or   /project/<name>/<version>/
  const m = location.pathname.match(/^\/project\/([^/]+)(?:\/([^/]+))?/);
  if (!m) return;
  const host =
    document.querySelector<HTMLElement>(".package-header") ||
    document.querySelector<HTMLElement>("main") ||
    document.body;

  const settings = await getSettings();
  if (settings?.mode === "real") {
    injectUnsupportedBadge(host, "Maven only in this build");
    return;
  }

  const name = m[1];
  const version = m[2] || readVersionFromDom() || "latest";
  const purl = `pkg:pypi/${name}@${version}`;

  const result = await requestVerdict(purl);
  if (result.kind === "error") {
    injectUnsupportedBadge(host, `scan failed — ${result.error}`);
    return;
  }
  injectBadge(host, result.verdict);
}

chrome.runtime.onMessage.addListener((msg: RuntimeMessage) => {
  if (msg.type === "RESCAN") void runScan();
});

void runScan();

function readVersionFromDom(): string | null {
  const el = document.querySelector(".package-header__name");
  const txt = el?.textContent || "";
  const m = txt.match(/\s+([\d][\w.\-+]*)\s*$/);
  return m ? m[1] : null;
}
