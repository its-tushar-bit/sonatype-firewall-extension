import { requestVerdict, getSettings, injectBadge, rewriteInstallSnippets } from "./shared";

(async function run() {
  // /project/<name>/   or   /project/<name>/<version>/
  const m = location.pathname.match(/^\/project\/([^/]+)(?:\/([^/]+))?/);
  if (!m) return;
  const name = m[1];
  const version = m[2] || readVersionFromDom() || "latest";
  const purl = `pkg:pypi/${name}@${version}`;

  const settings = await getSettings();
  const verdict = await requestVerdict(purl);
  if (!verdict) return;

  const host =
    document.querySelector<HTMLElement>(".package-header") ||
    document.querySelector<HTMLElement>("main") ||
    document.body;
  injectBadge(host, verdict);

  if (settings?.rewriteInstallCommands) {
    rewriteInstallSnippets("code, pre", settings.nexusProxyUrl, "pypi");
  }
})();

function readVersionFromDom(): string | null {
  const el = document.querySelector(".package-header__name");
  const txt = el?.textContent || "";
  const m = txt.match(/\s+([\d][\w.\-+]*)\s*$/);
  return m ? m[1] : null;
}
