/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { PolicyWaiverDTO, PolicyWaiverDetailDTO } from './waiverTypes';

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
 */
export function formatWaiverScopeLabel(w: PolicyWaiverDetailDTO): string {
  const name = w.scopeOwnerName ?? w.ownerName ?? w.ownerId ?? '';
  const typeLabel = formatWaiverOwnerTypeLabel(w.scopeOwnerType ?? w.ownerType);
  if (name && typeLabel) return `${name} (${typeLabel})`;
  return name || typeLabel || '—';
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
  if (w.displayName && typeof w.displayName === 'object') {
    const fromParts = (w.displayName.parts ?? [])
      .map((part) => part.value)
      .filter((value): value is string => Boolean(value))
      .join(':');
    if (fromParts) return fromParts;
  }
  if (c.artifactId && c.version) return `${c.artifactId}:${c.version}`;
  if (c.packageId && c.version) return `${c.packageId}@${c.version}`;
  if (c.name && c.version) return `${c.name}@${c.version}`;
  if (fmt) return `(${fmt})`;
  return 'All Components';
}
