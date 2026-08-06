import { requestVerdict, getSettings, injectBadge, rewriteInstallSnippets } from "./shared";

(async function run() {
  // URL pattern: https://www.npmjs.com/package/<name>  or  /package/<name>/v/<version>
  const m = location.pathname.match(/^\/package\/((?:@[^/]+\/)?[^/]+)(?:\/v\/([^/]+))?/);
  if (!m) return;
  const name = m[1];
  const version = m[2] || (await readVersionFromDom()) || "latest";
  const purl = `pkg:npm/${encodeURIComponent(name).replace(/%2F/g, "/")}@${version}`;

  const settings = await getSettings();
  const verdict = await requestVerdict(purl);
  if (!verdict) return;

  const host =
    document.querySelector<HTMLElement>('[class*="package-tab"] h2')?.parentElement ||
    document.querySelector<HTMLElement>("#top main") ||
    document.querySelector<HTMLElement>("main") ||
    document.body;
  injectBadge(host, verdict);

  if (settings?.rewriteInstallCommands) {
    rewriteInstallSnippets("code, pre", settings.nexusProxyUrl, "npm");
  }
})();

async function readVersionFromDom(): Promise<string | null> {
  // npm renders "Version" near the install snippet; fall back to "latest"
  const el = document.querySelector('[class*="package-version"]');
  return el?.textContent?.trim() || null;
}
