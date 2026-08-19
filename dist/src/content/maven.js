"use strict";
(() => {
  // src/content/shared.ts
  async function requestVerdict(purl) {
    const msg = { type: "GET_VERDICT", purl };
    console.log("[sonatype-firewall] requesting verdict for", purl);
    const res = await chrome.runtime.sendMessage(msg);
    if (!res) {
      console.warn("[sonatype-firewall] no response from background service worker");
      return null;
    }
    if (!res.ok) {
      console.error("[sonatype-firewall] verdict fetch failed:", res.error);
      return null;
    }
    if (!("verdict" in res)) {
      console.warn("[sonatype-firewall] response had no verdict field:", res);
      return null;
    }
    console.log("[sonatype-firewall] got verdict:", res.verdict);
    return res.verdict;
  }
  function badgeStyles(verdict) {
    switch (verdict) {
      case "block":
        return { bg: "#D92D20", fg: "#fff", label: "BLOCKED BY FIREWALL" };
      case "quarantine":
        return { bg: "#F79009", fg: "#1a1a1a", label: "QUARANTINED" };
      case "warn":
        return { bg: "#F79009", fg: "#1a1a1a", label: "WARN" };
      case "allow":
        return { bg: "#6b7280", fg: "#fff", label: "NO VULNERABILITIES" };
    }
  }
  function injectBadge(host, verdict) {
    const existing = host.querySelector("#sonatype-firewall-badge");
    if (existing) existing.remove();
    const styles = badgeStyles(verdict.policy.verdict);
    const wrap = document.createElement("div");
    wrap.id = "sonatype-firewall-badge";
    wrap.style.cssText = `
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    margin: 12px 0;
    border-radius: 6px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    font-size: 13px;
    font-weight: 600;
    background: ${styles.bg};
    color: ${styles.fg};
    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
  `;
    const dot = document.createElement("span");
    dot.style.cssText = `
    width: 8px; height: 8px; border-radius: 50%;
    background: ${styles.fg}; opacity: 0.85;
  `;
    wrap.appendChild(dot);
    const label = document.createElement("span");
    label.textContent = `Sonatype Firewall: ${styles.label}`;
    wrap.appendChild(label);
    if (verdict.policy.policyName) {
      const policy = document.createElement("span");
      policy.style.cssText = `opacity: 0.85; font-weight: 400;`;
      policy.textContent = `\xB7 ${verdict.policy.policyName} @ ${verdict.policy.stage}`;
      wrap.appendChild(policy);
    }
    if (verdict.component.goldenVersion) {
      const golden = document.createElement("span");
      golden.style.cssText = `
      margin-left: 8px; padding: 3px 8px;
      background: rgba(255,255,255,0.2); border-radius: 4px;
      font-weight: 500;
    `;
      golden.textContent = `\u2605 Golden Version: ${verdict.component.goldenVersion.version}`;
      wrap.appendChild(golden);
    }
    host.prepend(wrap);
    return wrap;
  }

  // src/content/maven.ts
  async function runScan() {
    console.log("[sonatype-firewall] maven scan running on", location.href);
    const m = location.pathname.match(
      /^\/artifact\/([^/]+)\/([^/]+)(?:\/([^/]+))?(?:\/([^/]+))?/
    );
    if (!m) {
      console.log("[sonatype-firewall] path did not match /artifact/{group}/{artifact}/{version}");
      return;
    }
    const [, group, artifact, version, packaging] = m;
    const type = packaging || "jar";
    const purl = `pkg:maven/${group}/${artifact}@${version || "latest"}?type=${type}`;
    console.log("[sonatype-firewall] parsed purl:", purl);
    const verdict = await requestVerdict(purl);
    if (!verdict) return;
    const host = document.querySelector("h1")?.parentElement || document.querySelector("main") || document.body;
    injectBadge(host, verdict);
  }
  chrome.runtime.onMessage.addListener((msg) => {
    if (msg.type === "RESCAN") void runScan();
  });
  void runScan();
})();
