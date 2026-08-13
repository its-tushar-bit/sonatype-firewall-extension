/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { PolicyWaiverDTO, PolicyWaiverDetailDTO } from './waiverTypes';

/**
 * Map Ana / route owner-type tokens onto the lowercase path segments expected by
 * {@code GET /api/v2/policyWaivers/{ownerType}/...}. Ana index-query emits enum-style
 * values ({@code APPLICATION}); Classic list DTOs already use lowercase.
 */
const OWNER_TYPE_ALIASES: Record<string, string> = {
  root_organization: 'organization',
  all_repositories: 'repository_container',
};

export function normalizeWaiverOwnerTypeForApi(raw: string | null | undefined): string | null {
  if (!raw) return null;
  const lower = raw.toLowerCase();
  return OWNER_TYPE_ALIASES[lower] ?? lower;
}

/**
 * `?type` route param for the native Waiver Detail page. Auto-waivers live under a
 * different API (`autoPolicyWaivers`, not `policyWaivers`) — the detail page reads
 * this to fetch the right one. Shared by every waiver table that links into it
 * (`WaiversTable`, `WaiversAnaTable`, `WaiversAnaCardList`) so the contract stays in one place.
 */
export function waiverDetailTypeParam(isAutoWaiver: boolean | undefined): 'autoWaiver' | undefined {
  return isAutoWaiver ? 'autoWaiver' : undefined;
}

/** Calendar-day waiver dates in UTC so the rendered day is timezone-stable. */
export function formatWaiverCalendarDate(value: string | number | undefined | null): string {
  return formatDateUtcYYYYMMDD(value ?? '');
}

export function waiverThreatColor(level: number): ReturnType<typeof threatColorFor> {
  return threatColorFor(level);
}

export function formatWaiverListExpiry(w: PolicyWaiverDTO): string {
  if (w.isAutoWaiver) return 'Auto';
  if (w.expiryTime) return formatWaiverCalendarDate(w.expiryTime);
  if (w.isExpireWhenRemediationAvailable) return 'When remediation available';
  return 'Never';
}

export interface WaiverExpiryPresentation {
  /** Rendered expiry text: a calendar day, or a non-date state such as `Never`. */
  readonly label: string;
  readonly expired: boolean;
  /** `in N days` for a future expiry; null when there is no date to count down to. */
  readonly relative: string | null;
}

const MS_PER_DAY = 24 * 60 * 60 * 1000;

/**
 * Expiry as the detail page presents it: a calendar day plus either an Expired
 * marker or a countdown, falling back to the non-date states (auto-managed,
 * remediation-driven, never).
 */
export function describeWaiverExpiry(
  w: PolicyWaiverDetailDTO,
  now: number = Date.now(),
): WaiverExpiryPresentation {
  if (w.isAutoWaiver) {
    return { label: 'Auto (managed by IQ)', expired: false, relative: null };
  }
  if (w.expiryTime !== null && w.expiryTime !== undefined && w.expiryTime !== '') {
    const expiresAt = new Date(w.expiryTime).getTime();
    if (!Number.isNaN(expiresAt)) {
      const label = formatWaiverCalendarDate(w.expiryTime);
      if (expiresAt <= now) {
        return { label, expired: true, relative: null };
      }
      const days = Math.ceil((expiresAt - now) / MS_PER_DAY);
      return { label, expired: false, relative: days === 1 ? 'in 1 day' : `in ${days} days` };
    }
  }
  if (w.isExpireWhenRemediationAvailable || w.expireWhenRemediationAvailable) {
    return { label: 'When remediation available', expired: false, relative: null };
  }
  return { label: 'Never', expired: false, relative: null };
}

const OWNER_TYPE_LABELS: Record<string, string> = {
  application: 'Application',
  organization: 'Organization',
  repository: 'Repository',
  repository_manager: 'Repository Manager',
  repository_container: 'Repository Container',
};

export function formatWaiverOwnerTypeLabel(ownerType: string | undefined | null): string {
  if (!ownerType) return '';
  const key = ownerType.toLowerCase();
  return (
    OWNER_TYPE_LABELS[key] ??
    key
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
  );
}

/**
 * Scope as `{owner name} ({Owner type})`. The v2 detail payload carries
 * `scopeOwner*` rather than the dashboard list's pre-joined `scope` string, so
 * the label is composed here instead of read off the row.
 *
 * Fallback chain:
 * 1. scopeOwnerName + scopeOwnerType (v2 detail fields)
 * 2. ownerName + ownerType (waiver owner fields)
 * 3. name only (no type available)
 * 4. scope field (legacy/Ana dashboard field, pre-formatted so it outranks a bare type label)
 * 5. type label only (no name and no scope field available)
 * 6. null (no scope available — caller should hide the label)
 *
 * Returns null when no scope information is available, allowing callers to
 * conditionally hide the scope label per CLM-44263 AC#3.
 */
export function formatWaiverScopeLabel(w: PolicyWaiverDetailDTO): string | null {
  // Build from scopeOwner* fields if available (v2 detail endpoint)
  const name = w.scopeOwnerName ?? w.ownerName;
  const typeLabel = formatWaiverOwnerTypeLabel(w.scopeOwnerType ?? w.ownerType);

  // If we have both name and type, format as "Name (Type)"
  if (name && typeLabel) return `${name} (${typeLabel})`;

  // If we have just name, use it
  if (name) return name;

  // Fallback to the pre-formatted scope field from legacy/dashboard endpoints
  if (w.scope) return w.scope;

  // If we have just the type, use its humanized label
  if (typeLabel) return typeLabel;

  // No scope information available — return null so caller can hide the label
  return null;
}

export function formatWaiverComponentLabel(
  w: Pick<PolicyWaiverDTO, 'displayName' | 'componentIdentifier'>,
): string {
  const ci = w.componentIdentifier;
  if (!ci) return 'All Components';
  const c = ci.coordinates ?? {};
  const fmt = ci.format ?? '';
  if (typeof w.displayName === 'string' && w.displayName) return w.displayName;
  // Classic / Firewall often send `{ parts: [{ value }] }` instead of a flat string.
  // ComponentDisplayNameDTO contract: Backend builds display strings by interleaving
  // format-specific separator parts (field=null) between field parts.
  // The canonical renderer is ComponentDisplayName.toString(), which concatenates every part value.
  if (w.displayName && typeof w.displayName === 'object') {
    const fromParts = (w.displayName.parts ?? [])
      .map((part) => part.value)
      .filter((value): value is string => Boolean(value))
      .join('');
    if (fromParts) return fromParts;
  }
  if (c.artifactId && c.version) return `${c.artifactId}:${c.version}`;
  if (c.packageId && c.version) return `${c.packageId}@${c.version}`;
  if (c.name && c.version) return `${c.name}@${c.version}`;
  if (fmt) return `(${fmt})`;
  return 'All Components';
}
