/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Badge, Flex, Link, Text } from '@radix-ui/themes';
import { dashboardViolationsHref } from 'MainRoot/nosc/dashboard/dashboardBundleUrls';
import { LegalObligationsTrendArrow } from './LegalObligationsTrendArrow';
import type {
  LegalObligationsAlpGroup,
  LegalObligationsTopViolation,
} from './legalObligationsTypes';

import './legalObligationsTile.css';

function violationsTabHref(): string {
  // Violations-tab drill-down filters (?ltg= / ?policy=) deferred until the slice
  // grows policyIds / licenseThreatGroups facets (CLM-40018 follow-up).
  return dashboardViolationsHref();
}

function unreviewedCountColor(count: number): 'red' | 'orange' | 'yellow' | 'gray' {
  if (count >= 25) return 'red';
  if (count >= 10) return 'orange';
  if (count >= 1) return 'yellow';
  return 'gray';
}

function LegalObligationRow({
  rowKey,
  children,
}: {
  rowKey: string;
  children: React.ReactNode;
}) {
  return (
    <Flex
      key={rowKey}
      align="center"
      justify="between"
      gap="3"
      data-testid="legal-obligation-row"
      className="legal-obligation-tile__row"
    >
      {children}
    </Flex>
  );
}

function LegalObligationRowLink({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <Link
      size="2"
      weight="medium"
      href={violationsTabHref()}
      data-testid="legal-obligation-row-link"
      className={className}
    >
      {children}
    </Link>
  );
}

export function LegalObligationsAlpRow({ group }: { group: LegalObligationsAlpGroup }) {
  return (
    <LegalObligationRow rowKey={group.id}>
      <Flex direction="column" gap="1" className="legal-obligation-tile__row-primary">
        <LegalObligationRowLink>{group.name}</LegalObligationRowLink>
        <LegalObligationsTrendArrow trendPct={group.trendPct} />
      </Flex>
      <Badge
        color={unreviewedCountColor(group.reviewCount)}
        radius="full"
        size="2"
        aria-label={`${group.reviewCount} unreviewed components`}
      >
        {group.reviewCount}
      </Badge>
    </LegalObligationRow>
  );
}

export function LegalObligationsTopViolationRow({
  violation,
}: {
  violation: LegalObligationsTopViolation;
}) {
  return (
    <LegalObligationRow rowKey={violation.policyId}>
      <LegalObligationRowLink className="legal-obligation-tile__row-link">
        {violation.policyName}
      </LegalObligationRowLink>
      <Text
        size="3"
        weight="bold"
        className="legal-obligation-tile__open-violation-count"
        aria-label={`${violation.openViolationCount} open violations`}
      >
        {violation.openViolationCount}
      </Text>
    </LegalObligationRow>
  );
}
