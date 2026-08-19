/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Link, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
// dateUtils.js is outside the TS program — import is untyped under strict .tsx.
import { formatTimeAgo } from 'MainRoot/util/dateUtils';
import { ViolationRow } from 'MainRoot/nosc/violations/violationListTypes';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import {
  threatCategoryLabel,
  violationStateLabel,
} from 'MainRoot/nosc/violations/violationsListApi';
import './ViolationCardGrid.scss';

const STATE_BADGE_COLOR: Readonly<Record<string, 'red' | 'gray'>> = {
  OPEN: 'red',
  WAIVED: 'gray',
};

/** Martha policy chrome: {@code Security-Critical} from threatCategory + severity (not policyName). */
export function policyTypeSeverityLabel(row: ViolationRow): string | undefined {
  const category = row.threatCategory?.trim()
    ? threatCategoryLabel(row.threatCategory.trim())
    : undefined;
  const severityRaw = row.severity?.trim();
  const severity = severityRaw
    ? severityRaw.charAt(0).toUpperCase() + severityRaw.slice(1).toLowerCase()
    : undefined;
  if (category && severity) return `${category}-${severity}`;
  if (category) return category;
  if (severity) return severity;
  return row.policyName?.trim() || undefined;
}

const ABSOLUTE_DATE_TIME = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

/**
 * Absolute date/time plus relative age for triage (e.g. {@code Mar 12, 2026, 3:41 PM · 2 months ago}).
 * Sub-minute relative age reads as {@code just now}.
 */
function formatFirstSeen(ms: number): string {
  const absolute = ABSOLUTE_DATE_TIME.format(new Date(ms));
  const ago = formatTimeAgo(ms);
  const relative = ago === 'seconds ago' ? 'just now' : ago;
  return `${absolute} · ${relative}`;
}

function componentDisplay(row: ViolationRow): string {
  return row.componentName || '(unknown component)';
}

function violationCardAriaLabel(
  v: ViolationRow,
  options: { readonly hideStateBadges?: boolean } = {},
): string {
  const state = v.state ?? 'OPEN';
  const component = componentDisplay(v);
  // Lead with the actual state so the label never contradicts it (e.g. a waived card must not be
  // announced as "Open violation …"). Include the threat level so severity — shown visually by the
  // badge — is also announced. Reads as "Open violation for X on Y, threat level 10" / "Waived
  // violation for X on Y, threat level 3, auto-waived".
  // Carry the application into the accessible name so link-by-link navigation keeps the primary drill
  // context (the visible card still shows org/stage/first-seen for sighted users).
  const target = v.applicationName ? `${component} in ${v.applicationName}` : component;
  // When state badges are hidden (Legal), prefer policyName (LTG / license) and omit OPEN/WAIVED —
  // Wave A policyTypeSeverity chrome is for POLICY_VIOLATION cards only.
  const policy = options.hideStateBadges
    ? v.policyName?.trim() || '(unknown policy)'
    : policyTypeSeverityLabel(v) || '(unknown policy)';
  const lead = options.hideStateBadges
    ? `${policy} on ${target}`
    : `${violationStateLabel(state)} violation for ${policy} on ${target}`;
  const parts = [lead];
  if (v.threatLevel != null) {
    parts.push(`threat level ${v.threatLevel}`);
  }
  // Gate on the waived state (not just the flag) so a stale `waivedWithAutoWaiver` on an OPEN row
  // never announces "auto-waived" while the visual badges — which are gated on `isWaived` — show none.
  if (!options.hideStateBadges && state === 'WAIVED' && v.waivedWithAutoWaiver) {
    parts.push('auto-waived');
  }
  return parts.join(', ');
}

function ViolationCard({
  violation: v,
  href,
  hideStateBadges = false,
}: {
  readonly violation: ViolationRow;
  readonly href: string;
  readonly hideStateBadges?: boolean;
}): JSX.Element {
  // Severity band → color token; mapped to border color in ViolationCardGrid.scss (keep in sync).
  const threatColor = threatColorFor(v.threatLevel ?? 0);
  const state = v.state ?? 'OPEN';
  const component = componentDisplay(v);
  const isWaived = state === 'WAIVED';
  // Page SQL enrich maps PolicyViolation.openTime → firstOccurredTime; omit the line when absent.
  const firstSeen = v.firstOccurredTime != null ? formatFirstSeen(v.firstOccurredTime) : '';
  // Legal (hideStateBadges) shows LTG / license via policyName; Violations use Wave A chrome.
  const policyLabel = hideStateBadges
    ? v.policyName?.trim() || undefined
    : policyTypeSeverityLabel(v);

  return (
    <Card asChild>
      <Link
        href={href}
        underline="none"
        className="violation-card-link"
        data-threat-color={threatColor}
        data-testid="violation-card-link"
        aria-label={violationCardAriaLabel(v, { hideStateBadges })}
      >
        <Flex direction="column" gap="2">
          <Flex align="center" justify="between" gap="4" wrap="wrap">
            <Flex align="center" gap="3" style={{ minWidth: 0, flex: 1 }}>
              <ViolationThreatBadge threat={v.threatLevel} size="2" />
              <Text size="3" weight="bold" style={{ wordBreak: 'break-word' }}>
                {component}
              </Text>
            </Flex>
            {!hideStateBadges && (
              <Flex align="center" gap="2" wrap="wrap">
                <Badge color={STATE_BADGE_COLOR[state] ?? 'gray'} variant="soft" size="1">
                  {violationStateLabel(state)}
                </Badge>
                {/* One waiver pill per waived row, and the two kinds are mutually exclusive so the auto
                    tag reads as distinct rather than layered on top of the manual indicator (CLM-42261):
                    a manual waiver keeps the standard "Waiver Applied" (blue outline); an auto-waiver
                    shows a distinct "Auto-waived" pill (orange soft + bolt glyph). Soft, not solid, so it
                    reads as informational alongside the soft state badge rather than as a warning. Both
                    gate on isWaived so a stale waivedWithAutoWaiver flag can't tag an OPEN row. */}
                {isWaived &&
                  (v.waivedWithAutoWaiver ? (
                    <Badge
                      color="orange"
                      variant="soft"
                      size="1"
                      data-testid="violation-card-auto-waiver"
                    >
                      <DomainIcons.AutoWaiver size={11} aria-hidden />
                      Auto-waived
                    </Badge>
                  ) : (
                    <Badge color="blue" variant="outline" size="1" data-testid="violation-card-waiver">
                      Waiver Applied
                    </Badge>
                  ))}
              </Flex>
            )}
          </Flex>

          <Flex gap="4" wrap="wrap" align="center">
            {policyLabel && (
              <Text size="1" color="gray" weight="medium" data-testid="violation-card-policy">
                {policyLabel}
              </Text>
            )}
            {(v.organizationName || v.applicationName) && (
              <Flex align="center" gap="1">
                {v.organizationName && (
                  <>
                    <DomainIcons.Organizations size={12} color="var(--gray-9)" aria-hidden />
                    <Text size="1" color="gray">
                      {v.organizationName}
                    </Text>
                    {v.applicationName && (
                      <Text size="1" color="gray" aria-hidden="true">
                        /
                      </Text>
                    )}
                  </>
                )}
                {v.applicationName && (
                  <>
                    <DomainIcons.Applications size={12} color="var(--gray-9)" aria-hidden />
                    <Text size="1" color="gray">
                      {v.applicationName}
                    </Text>
                  </>
                )}
              </Flex>
            )}
            {v.stage && (
              <Flex align="center" gap="1">
                <DomainIcons.Stage size={12} color="var(--gray-9)" aria-hidden />
                <Text size="1" color="gray">
                  {v.stage}
                </Text>
              </Flex>
            )}
            {firstSeen && (
              <Flex align="center" gap="1" data-testid="violation-card-first-seen">
                <DomainIcons.Clock size={12} color="var(--gray-9)" aria-hidden />
                <Text size="1" color="gray">
                  first seen {firstSeen}
                </Text>
              </Flex>
            )}
          </Flex>
        </Flex>
      </Link>
    </Card>
  );
}

export interface ViolationCardGridProps {
  readonly violations: ReadonlyArray<ViolationRow>;
  /** Override card href (Legal findings drill to Classic report). */
  readonly getCardHref?: (violation: ViolationRow) => string;
  /** Hide OPEN/WAIVED + waiver pills (Legal findings have no waiver state). */
  readonly hideStateBadges?: boolean;
}

/** Card list for Martha V1 Violations — one clickable card per violation row. */
export default function ViolationCardGrid({
  violations,
  getCardHref,
  hideStateBadges = false,
}: ViolationCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="violation-card-grid">
      {violations.map((violation) => (
        <Box key={violation.policyViolationId} data-testid="violation-card">
          <ViolationCard
            violation={violation}
            href={getCardHref?.(violation) ?? violationDetailHref(violation.policyViolationId)}
            hideStateBadges={hideStateBadges}
          />
        </Box>
      ))}
    </Flex>
  );
}
