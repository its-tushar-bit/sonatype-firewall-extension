/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Badge, Card, Flex, Heading, Link, Text } from '@radix-ui/themes';
import { SectionHeading, useComponent } from '@guide/ui-core';
import { tokens, formatDisplayDate } from '@guide/ui-core/utils';
import {
  getGuidePolicyCompliance,
  stageLabel,
  threatAccent,
  waiverScopeLabel,
  ROOT_ORGANIZATION_ID,
  type GuidePolicyViolation,
} from './policyComplianceTypes';
import { PolicyBadgeV2 } from './PolicyBadgeV2';
import { usePolicyContext } from 'GuideRoot/components/navigation/context-picker/PolicyContext';

export function PolicyComplianceCardV2() {
  const component = useComponent();
  const compliance = getGuidePolicyCompliance(component);
  const { activeOwner } = usePolicyContext();

  if (!compliance) {
    return null;
  }

  // Stable server-side "links" indirection (UserInterfaceLinksResource @ /ui/links): a GET to
  // /ui/links/{ownerType}/{ownerId}/management 302-redirects into the Lifecycle management view.
  // Drive it from the picker selection — root when nothing is selected. The link uses the owner's
  // publicId (identical to id for orgs; the human-readable id legacy management links expect for
  // apps); the compliance payload carries no ownerType and only the internal id, so it cannot
  // build this link itself.
  const lifecycleOwnerType = activeOwner?.type === 'app' ? 'application' : 'organization';
  const lifecycleOwnerId = activeOwner?.publicId ?? ROOT_ORGANIZATION_ID;
  const lifecycleUrl = `/ui/links/${lifecycleOwnerType}/${lifecycleOwnerId}/management`;
  const ownerLabel = activeOwner?.name ?? 'Root Organization';

  // e.g. "Root Organization · Release Stage · via Lifecycle"; the stage segment is dropped when the
  // payload omits it (badge-only responses carry no stage).
  const policyContextLabel = [
    ownerLabel,
    compliance.stage ? stageLabel(compliance.stage) : null,
    'via Lifecycle',
  ]
    .filter(Boolean)
    .join(' · ');

  return (
    <Card size={tokens.card.large}>
      <Flex direction="column" gap={tokens.space.item}>
        <Flex justify="between" align="center">
          <SectionHeading>Policy Compliance</SectionHeading>
          <PolicyBadgeV2 complianceLevel={compliance.complianceLevel} />
        </Flex>

        <Text size={tokens.sizes.body.sm} color="gray">
          Policy context:{' '}
          <Link color="blue" href={lifecycleUrl} target="_blank" rel="noopener noreferrer">
            {policyContextLabel}
          </Link>
        </Text>

        {compliance.violations != null && (
          <ViolationsSection violations={compliance.violations} />
        )}
      </Flex>
    </Card>
  );
}

function ViolationsSection({ violations }: { violations: GuidePolicyViolation[] }) {
  if (violations.length === 0) {
    return (
      <Text size={tokens.sizes.body.sm} color="gray">
        No policy violations
      </Text>
    );
  }

  const allWaived = violations.every((violation) => violation.waived);
  const headingText = `Violations (${violations.length}${allWaived ? ' — all waived' : ''})`;

  // Highest threat first. Copy before sorting so the source array (and the count/allWaived
  // derived above) is never mutated; the sort is stable, so equal-threat order is preserved.
  const sortedViolations = [...violations].sort((a, b) => b.threatLevel - a.threatLevel);

  return (
    <Flex direction="column" gap={tokens.space.item}>
      <Heading as="h3" size={tokens.sizes.cardTitle}>
        {headingText}
      </Heading>
      {sortedViolations.map((violation, index) => (
        // policyId alone is not unique: the backend emits one violation per policy alert, so a
        // single policy can appear twice (e.g. an active alert plus a waived alert on the same
        // component). Compose with waived-state + index to keep keys stable and collision-free.
        <ViolationRow
          key={`${violation.policyId}-${violation.waived ? 'w' : 'a'}-${index}`}
          violation={violation}
        />
      ))}
    </Flex>
  );
}

function ViolationRow({ violation }: { violation: GuidePolicyViolation }) {
  const accent = threatAccent(violation.threatLevel);

  return (
    <Card size={tokens.card.small} variant="surface">
      <Flex direction="column" gap={tokens.space.tight}>
        <Flex align="center" gap={tokens.space.inline} wrap="wrap">
          <span
            aria-hidden="true"
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              display: 'inline-block',
              backgroundColor: `var(--${accent}-11)`,
            }}
          />
          <Text size={tokens.sizes.body.sm} weight="bold" color={accent}>
            {violation.threatLevel}
          </Text>
          <Text size={tokens.sizes.body.sm} weight="bold">
            {violation.policyName}
          </Text>
          {violation.waived && (
            <Badge color="amber" variant="soft" size={tokens.badge.medium}>
              Waived
            </Badge>
          )}
        </Flex>

        {(violation.constraintViolations ?? []).map((constraint, constraintIndex) => (
          // constraintId is expected unique within a violation, but compose with the index so a
          // degenerate duplicate id can't collide — mirrors the reasons / ViolationRow key pattern.
          <Flex
            key={`${constraint.constraintId}-${constraintIndex}`}
            direction="column"
            gap={tokens.space.tight}
          >
            <Text size={tokens.sizes.body.sm} color="gray">
              {constraint.constraintName}
            </Text>
            {(constraint.reasons ?? []).map((reason, index) => (
              <Text key={`${constraint.constraintId}-${index}`} size={tokens.sizes.body.sm}>
                · {reason.reason}
              </Text>
            ))}
          </Flex>
        ))}

        {violation.waived && violation.waiver && (
          <Flex align="center" gap={tokens.space.inline} wrap="wrap">
            <Text size={tokens.sizes.body.xs} color="gray">
              {waiverScopeLabel(violation.waiver)}
            </Text>
            {violation.waiver.expiryTime && (
              <Text size={tokens.sizes.body.xs} color="gray">
                Expires: {formatDisplayDate(violation.waiver.expiryTime)}
              </Text>
            )}
          </Flex>
        )}
      </Flex>
    </Card>
  );
}
