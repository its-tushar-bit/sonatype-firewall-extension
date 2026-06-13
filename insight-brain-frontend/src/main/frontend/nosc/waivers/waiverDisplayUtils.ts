/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
import type { PolicyWaiverDTO, PolicyWaiverDetailDTO } from './waiverTypes';

/** Calendar-day waiver dates in UTC so the rendered day is timezone-stable. */
export function formatWaiverCalendarDate(value: string | undefined | null): string {
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

export function formatWaiverDetailExpiry(w: PolicyWaiverDetailDTO): string {
  if (w.isAutoWaiver) return 'Auto (managed by IQ)';
  if (w.expiryTime) return formatWaiverCalendarDate(w.expiryTime);
  if (w.isExpireWhenRemediationAvailable) return 'When remediation available';
  return 'Does not expire';
}

export function formatWaiverComponentLabel(
  w: Pick<PolicyWaiverDTO, 'displayName' | 'componentIdentifier'>,
): string {
  const ci = w.componentIdentifier;
  if (!ci) return 'All Components';
  const c = ci.coordinates ?? {};
  const fmt = ci.format ?? '';
  if (typeof w.displayName === 'string' && w.displayName) return w.displayName;
  if (c.artifactId && c.version) return `${c.artifactId}:${c.version}`;
  if (c.packageId && c.version) return `${c.packageId}@${c.version}`;
  if (c.name && c.version) return `${c.name}@${c.version}`;
  if (fmt) return `(${fmt})`;
  return 'All Components';
}
