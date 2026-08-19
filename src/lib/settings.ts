import { DEFAULT_SETTINGS, ExtensionSettings } from "../types";

export async function getSettings(): Promise<ExtensionSettings> {
  const stored = await chrome.storage.local.get("settings");
  return { ...DEFAULT_SETTINGS, ...(stored.settings || {}) };
}

export async function setSettings(settings: ExtensionSettings): Promise<void> {
  await chrome.storage.local.set({ settings });
}
