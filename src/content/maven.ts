import { requestVerdict, injectBadge, injectUnsupportedBadge } from "./shared";
import { RuntimeMessage } from "../types";

function parseMavenPurl(pathname: string): string | null {
  // Sonatype Central UI: /artifact/{group}/{artifact}/{version}[/{packaging}]
  const central = pathname.match(
    /^\/artifact\/([^/]+)\/([^/]+)(?:\/([^/]+))?(?:\/([^/]+))?/,
  );
  if (central) {
    const [, group, artifact, version, packaging] = central;
    const type = packaging || "jar";
    return `pkg:maven/${group}/${artifact}@${version || "latest"}?type=${type}`;
  }

  // Raw Maven repo layout (repo1.maven.org, Nexus browse, etc.):
  //   /maven2/{group-path}/{artifact}/{version}/[{file}]
  //   /{group-path}/{artifact}/{version}/  (some proxies omit /maven2)
  // groupPath uses slashes; convert to dots. Require a version-shaped segment
  // (starts with a digit) so we don't fire on the /group/artifact/ listing.
  const parts = pathname.split("/").filter(Boolean);
  const start = parts[0] === "maven2" ? 1 : 0;
  const segs = parts.slice(start);
  if (segs.length < 3) return null;

  // Locate the version segment (starts with a digit). Prefer the last such
  // segment before any file segment so /group/artifact/1.2.3/artifact-1.2.3.jar
  // still resolves cleanly.
  let versionIdx = -1;
  for (let i = segs.length - 1; i >= 2; i--) {
    if (/^\d/.test(segs[i]) && !segs[i].includes(".jar") && !segs[i].includes(".pom")) {
      versionIdx = i;
      break;
    }
  }
  if (versionIdx < 2) return null;
  const version = segs[versionIdx];
  const artifact = segs[versionIdx - 1];
  const group = segs.slice(0, versionIdx - 1).join(".");
  if (!group || !artifact) return null;
  return `pkg:maven/${group}/${artifact}@${version}?type=jar`;
}

async function runScan() {
  console.log("[sonatype-firewall] maven scan running on", location.href);
  const purl = parseMavenPurl(location.pathname);
  if (!purl) {
    console.log("[sonatype-firewall] path did not match a supported Maven URL layout");
    return;
  }
  console.log("[sonatype-firewall] parsed purl:", purl);

  const host =
    document.querySelector<HTMLElement>("h1")?.parentElement ||
    document.querySelector<HTMLElement>("main") ||
    document.body;

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
