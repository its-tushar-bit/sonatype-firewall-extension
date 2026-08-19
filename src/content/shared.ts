import { FirewallVerdict, RuntimeMessage, RuntimeResponse } from "../types";

export async function requestVerdict(purl: string): Promise<FirewallVerdict | null> {
  const msg: RuntimeMessage = { type: "GET_VERDICT", purl };
  console.log("[sonatype-firewall] requesting verdict for", purl);
  const res = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>(msg);
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

export async function getSettings() {
  const res = await chrome.runtime.sendMessage<RuntimeMessage, RuntimeResponse>({
    type: "GET_SETTINGS",
  });
  if (res.ok && "settings" in res) return res.settings;
  return null;
}

export function badgeStyles(verdict: FirewallVerdict["policy"]["verdict"]) {
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

export function injectUnsupportedBadge(host: HTMLElement, message: string): HTMLElement {
  const existing = host.querySelector<HTMLElement>("#sonatype-firewall-badge");
  if (existing) existing.remove();
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
    font-weight: 500;
    background: #6b7280;
    color: #ffffff;
    box-shadow: 0 1px 3px rgba(0,0,0,0.12);
  `;
  const dot = document.createElement("span");
  dot.style.cssText = `width: 8px; height: 8px; border-radius: 50%; background: #ffffff; opacity: 0.85;`;
  wrap.appendChild(dot);
  const label = document.createElement("span");
  label.textContent = `Sonatype Firewall: ${message}`;
  wrap.appendChild(label);
  host.prepend(wrap);
  return wrap;
}

export function injectBadge(host: HTMLElement, verdict: FirewallVerdict): HTMLElement {
  const existing = host.querySelector<HTMLElement>("#sonatype-firewall-badge");
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
    policy.textContent = `· ${verdict.policy.policyName} @ ${verdict.policy.stage}`;
    wrap.appendChild(policy);
  }

  if (verdict.component.goldenVersion) {
    const golden = document.createElement("span");
    golden.style.cssText = `
      margin-left: 8px; padding: 3px 8px;
      background: rgba(255,255,255,0.2); border-radius: 4px;
      font-weight: 500;
    `;
    golden.textContent = `★ Golden Version: ${verdict.component.goldenVersion.version}`;
    wrap.appendChild(golden);
  }

  host.prepend(wrap);
  return wrap;
}

export function rewriteInstallSnippets(
  rootSelector: string,
  proxyUrl: string,
  ecosystem: "npm" | "pypi" | "maven",
) {
  const blocks = document.querySelectorAll<HTMLElement>(rootSelector);
  blocks.forEach((el) => {
    const text = el.textContent || "";
    let updated = text;
    if (ecosystem === "npm" && /^\s*npm\s+i(nstall)?\s+/.test(text)) {
      updated = text.replace(
        /^(\s*npm\s+i(nstall)?\s+)/,
        `$1--registry=${proxyUrl}/npm-proxy/ `,
      );
    } else if (ecosystem === "pypi" && /^\s*pip\s+install\s+/.test(text)) {
      updated = text.replace(
        /^(\s*pip\s+install\s+)/,
        `$1-i ${proxyUrl}/pypi-proxy/simple `,
      );
    }
    if (updated !== text) {
      el.textContent = updated;
      el.title = "Rewritten by Sonatype Firewall extension to use your Nexus proxy";
      el.style.outline = "1px dashed #1F65BF";
    }
  });
}
