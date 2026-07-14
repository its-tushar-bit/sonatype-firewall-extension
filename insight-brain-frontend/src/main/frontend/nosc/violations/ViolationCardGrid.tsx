/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Box, Card, Flex, Link, Text } from '@radix-ui/themes';
import { DomainIcons } from 'MainRoot/nosc/icons';
import { threatColorFor } from 'MainRoot/nosc/applications/applicationDetailUtils';
import { ViolationRow } from 'MainRoot/nosc/violations/violationListTypes';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { violationDetailHref } from 'MainRoot/nosc/violations/violationDetailHref';
import { violationStateLabel } from 'MainRoot/nosc/violations/violationsListApi';

const STATE_BADGE_COLOR: Readonly<Record<string, 'red' | 'gray'>> = {
  OPEN: 'red',
  WAIVED: 'gray',
};

/** Component name with an appended ` : version` when the identifier carries one. */
function componentDisplay(row: ViolationRow): string {
  const version = row.componentIdentifier?.coordinates?.version;
  const name = row.componentName || '(unknown component)';
  return version ? `${name} : ${version}` : name;
}

function violationCardAriaLabel(v: ViolationRow): string {
  const state = v.state ?? 'OPEN';
  const component = componentDisplay(v);
  const policy = v.policyName || '(unknown policy)';
  // Lead with the actual state so the label never contradicts it (e.g. a waived card must not be
  // announced as "Open violation …"). Include the threat level so severity — shown visually by the
  // badge — is also announced. Reads as "Open violation for X on Y, threat level 10" / "Waived
  // violation for X on Y, threat level 3, auto-waived".
  const parts = [`${violationStateLabel(state)} violation for ${policy} on ${component}`];
  if (v.threatLevel != null) {
    parts.push(`threat level ${v.threatLevel}`);
  }
  if (v.waivedWithAutoWaiver) {
    parts.push('auto-waived');
  }
  return parts.join(', ');
}

function ViolationCard({ violation: v }: { readonly violation: ViolationRow }): JSX.Element {
  const borderColor = `var(--${threatColorFor(v.threatLevel ?? 0)}-9)`;
  const state = v.state ?? 'OPEN';
  const component = componentDisplay(v);

  return (
    <Card
      asChild
      style={{ borderLeft: `4px solid ${borderColor}` }}
    >
      <Link
        href={violationDetailHref(v.policyViolationId)}
        underline="none"
        data-testid="violation-card-link"
        aria-label={violationCardAriaLabel(v)}
        style={{ color: 'inherit', display: 'block' }}
      >
        <Flex direction="column" gap="2">
          <Flex align="center" justify="between" gap="4" wrap="wrap">
            <Flex align="center" gap="3" style={{ minWidth: 0, flex: 1 }}>
              <ViolationThreatBadge threat={v.threatLevel} size="1" />
              <Text size="3" weight="bold" style={{ wordBreak: 'break-word' }}>
                {component}
              </Text>
            </Flex>
            <Flex align="center" gap="2">
              <Badge color={STATE_BADGE_COLOR[state] ?? 'gray'} variant="soft" size="1">
                {violationStateLabel(state)}
              </Badge>
              {v.waivedWithAutoWaiver && (
                <Badge color="green" variant="solid" size="1" data-testid="violation-card-auto-waiver">
                  Auto-waiver
                </Badge>
              )}
            </Flex>
          </Flex>

          <Flex gap="4" wrap="wrap" align="center">
            {v.policyName && (
              <Text size="1" color="gray" weight="medium">
                {v.policyName}
              </Text>
            )}
            {v.organizationName && (
              <Flex align="center" gap="1">
                <DomainIcons.Organizations size={12} color="var(--gray-9)" aria-hidden />
                <Text size="1" color="gray">
                  {v.organizationName}
                </Text>
              </Flex>
            )}
            {v.applicationName && (
              <Flex align="center" gap="1">
                <DomainIcons.Applications size={12} color="var(--gray-9)" aria-hidden />
                <Text size="1" color="gray">
                  {v.applicationName}
                </Text>
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
          </Flex>
        </Flex>
      </Link>
    </Card>
  );
}

export interface ViolationCardGridProps {
  readonly violations: ReadonlyArray<ViolationRow>;
}

/** Card list for Martha V1 Violations (CLM-42257). One clickable card per violation row. */
export default function ViolationCardGrid({ violations }: ViolationCardGridProps): JSX.Element {
  return (
    <Flex direction="column" gap="3" data-testid="violation-card-grid">
      {violations.map((violation) => (
        <Box key={violation.policyViolationId} data-testid="violation-card">
          <ViolationCard violation={violation} />
        </Box>
      ))}
    </Flex>
  );
}
