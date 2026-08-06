import { RuntimeMessage, RuntimeResponse, FirewallVerdict } from "../types";
import { getSettings, setSettings } from "../lib/settings";
import { fetchVerdict, requestWaiver } from "../lib/iq-client";
import { getCached, setCached } from "../lib/cache";

const tabVerdicts = new Map<number, FirewallVerdict>();

chrome.runtime.onMessage.addListener(
  (msg: RuntimeMessage, sender, sendResponse: (r: RuntimeResponse) => void) => {
    handle(msg, sender.tab?.id)
      .then(sendResponse)
      .catch((e: Error) => sendResponse({ ok: false, error: e.message }));
    return true; // async
  },
);

chrome.tabs.onRemoved.addListener((tabId) => {
  tabVerdicts.delete(tabId);
});

async function handle(msg: RuntimeMessage, tabId?: number): Promise<RuntimeResponse> {
  switch (msg.type) {
    case "GET_VERDICT": {
      const cached = getCached(msg.purl);
      if (cached) {
        if (tabId !== undefined) {
          tabVerdicts.set(tabId, cached);
          await updateBadge(cached, tabId);
        }
        return { ok: true, verdict: cached };
      }
      const settings = await getSettings();
      const verdict = await fetchVerdict(msg.purl, settings);
      setCached(msg.purl, verdict);
      if (tabId !== undefined) {
        tabVerdicts.set(tabId, verdict);
        await updateBadge(verdict, tabId);
      }
      return { ok: true, verdict };
    }
    case "GET_SETTINGS":
      return { ok: true, settings: await getSettings() };
    case "SET_SETTINGS":
      await setSettings(msg.settings);
      return { ok: true, settings: msg.settings };
    case "REQUEST_WAIVER": {
      const settings = await getSettings();
      const id = await requestWaiver(msg.purl, msg.reason, settings);
      return { ok: true, waiverId: id };
    }
    case "GET_LAST_VIEWED": {
      const [activeTab] = await chrome.tabs.query({
        active: true,
        lastFocusedWindow: true,
      });
      const activeId = activeTab?.id;
      const lastViewed = activeId !== undefined ? tabVerdicts.get(activeId) ?? null : null;
      return { ok: true, lastViewed };
    }
  }
}

async function updateBadge(v: FirewallVerdict, tabId: number) {
  const v2 = v.policy.verdict;
  const text =
    v2 === "block" ? "!" : v2 === "quarantine" ? "Q" : v2 === "warn" ? "W" : "✓";
  const color =
    v2 === "block"
      ? "#D92D20"
      : v2 === "quarantine"
        ? "#F79009"
        : v2 === "warn"
          ? "#F79009"
          : "#12B76A";
  await chrome.action.setBadgeText({ text, tabId });
  await chrome.action.setBadgeBackgroundColor({ color, tabId });
}
