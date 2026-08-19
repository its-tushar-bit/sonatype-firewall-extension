import { requestVerdict, injectBadge } from "./shared";
import { RuntimeMessage } from "../types";

async function runScan() {
  console.log("[sonatype-firewall] maven scan running on", location.href);
  const m = location.pathname.match(
    /^\/artifact\/([^/]+)\/([^/]+)(?:\/([^/]+))?(?:\/([^/]+))?/,
  );
  if (!m) {
    console.log("[sonatype-firewall] path did not match /artifact/{group}/{artifact}/{version}");
    return;
  }
  const [, group, artifact, version, packaging] = m;
  // IQ rejects Maven purls without a ?type= qualifier; default to jar.
  const type = packaging || "jar";
  const purl = `pkg:maven/${group}/${artifact}@${version || "latest"}?type=${type}`;
  console.log("[sonatype-firewall] parsed purl:", purl);

  const verdict = await requestVerdict(purl);
  if (!verdict) return;

  const host =
    document.querySelector<HTMLElement>("h1")?.parentElement ||
    document.querySelector<HTMLElement>("main") ||
    document.body;
  injectBadge(host, verdict);
}

chrome.runtime.onMessage.addListener((msg: RuntimeMessage) => {
  if (msg.type === "RESCAN") void runScan();
});

void runScan();
