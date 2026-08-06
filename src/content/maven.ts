import { requestVerdict, injectBadge } from "./shared";

(async function run() {
  // central.sonatype.com/artifact/<group>/<artifact>/<version>
  // search.maven.org/artifact/<group>/<artifact>/<version>/<packaging>
  const m = location.pathname.match(
    /^\/artifact\/([^/]+)\/([^/]+)(?:\/([^/]+))?/,
  );
  if (!m) return;
  const [, group, artifact, version] = m;
  const purl = `pkg:maven/${group}/${artifact}@${version || "latest"}`;

  const verdict = await requestVerdict(purl);
  if (!verdict) return;

  const host =
    document.querySelector<HTMLElement>("h1")?.parentElement ||
    document.querySelector<HTMLElement>("main") ||
    document.body;
  injectBadge(host, verdict);
})();
