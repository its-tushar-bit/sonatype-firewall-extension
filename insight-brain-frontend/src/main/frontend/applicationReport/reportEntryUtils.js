/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// atob alone yields Latin-1 code points, so multi-byte UTF-8 (non-ASCII component names,
// internationalized violation messages, etc.) round-trips wrong. Decode via TextDecoder to
// preserve the original UTF-8 bytes.
function base64ToUtf8(base64) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return new TextDecoder('utf-8').decode(bytes);
}

/**
 * HRC's browseReport endpoint (added in CLM-44276) returns the whole ReportEntry
 * object with a base64-encoded `buf` field, instead of the raw JSON bytes like the
 * application endpoint does. Detect that wrapper and unwrap it.
 * Application responses (already-parsed JSON) fall through unchanged.
 */
export function unwrapReportEntry(raw) {
  if (
    raw &&
    typeof raw === 'object' &&
    typeof raw.buf === 'string' &&
    typeof raw.name === 'string' &&
    typeof raw.time === 'number'
  ) {
    try {
      return JSON.parse(base64ToUtf8(raw.buf));
    } catch (e) {
      // eslint-disable-next-line no-console
      console.error('Failed to unwrap ReportEntry response for', raw.name, e);
      return undefined;
    }
  }
  return raw;
}

/**
 * Fallback chain for the display name shown at the top of every report subpage.
 * Application reports have a real application object; HRC reports don't, and rely
 * on the componentDisplayName carried through the route params (CLM-42090), with the
 * hrcId as the last-resort fallback.
 */
export function getReportDisplayName(metadata, routerParams) {
  return metadata?.application?.name || routerParams?.componentDisplayName || routerParams?.hrcId || '';
}

/**
 * Ensure a response object has an `aaData` array. createReportEntries and similar report
 * consumers rely on `.aaData` being an array. HRC responses may lack this field (or come
 * back as `{}`), so we normalize here to prevent crashes.
 */
export function ensureAaData(raw) {
  const unwrapped = unwrapReportEntry(raw);
  if (!unwrapped || typeof unwrapped !== 'object') return undefined;
  return { ...unwrapped, aaData: unwrapped.aaData || [] };
}
