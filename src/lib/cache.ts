import { FirewallVerdict } from "../types";

const TTL_MS = 5 * 60 * 1000;
const memCache = new Map<string, FirewallVerdict>();

export function getCached(purl: string): FirewallVerdict | null {
  const entry = memCache.get(purl);
  if (!entry) return null;
  if (Date.now() - entry.fetchedAt > TTL_MS) {
    memCache.delete(purl);
    return null;
  }
  return entry;
}

export function setCached(purl: string, verdict: FirewallVerdict): void {
  memCache.set(purl, verdict);
}

export function clearCache(): void {
  memCache.clear();
}
