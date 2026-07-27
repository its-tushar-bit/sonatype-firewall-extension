/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo, type ReactElement, type ReactNode } from 'react';
import { Badge, Box, Button, Card, Flex, Heading, Link, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { EntityDetailRow } from 'MainRoot/nosc/entityDetail/EntityDetailRow';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useWaiverDetail } from './useWaivers';
import type { PolicyWaiverDetailDTO } from './waiverTypes';
import {
  describeWaiverExpiry,
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverScopeLabel,
} from './waiverDisplayUtils';

/**
 * Native Nexus One Waiver Detail page (CLM-40007 / CLM-42773 / CLM-43289).
 *
 * Mounted at `/waivers/{ownerType}/{ownerId}/{waiverId}`, reading a single
 * `GET /api/v2/policyWaivers/...`. The Overview shows threat + policy header,
 * constraint blurb, a Scope/Component/Expires meta strip, and one Waiver Details
 * card. Everything renders from that one payload; no estate fan-out.
 *
 * Mutations (extend, delete, approve/reject) and additional entity tabs stay
 * deferred — see CLM-42708 / CLM-43365.
 */

interface ParsedRoute {
  ownerType: string | null;
  ownerId: string | null;
  waiverId: string | null;
}

const OWNER_TYPE_MAP: Record<string, string> = {
  root_organization: 'organization',
  all_repositories: 'repository_container',
};

/**
 * Owner type as the v2 path expects it. Lowercased first because list links can
 * carry the enum casing (`APPLICATION`) while the API path matches lowercase.
 */
function normalizeOwnerType(rawOwnerType: string | undefined): string | null {
  if (!rawOwnerType) return null;
  const ownerType = rawOwnerType.toLowerCase();
  return OWNER_TYPE_MAP[ownerType] ?? ownerType;
}

interface BackLinkTarget {
  href: string;
  label: string;
}

function computeBackLink(from: string | null, hrefFor: (state: string) => string): BackLinkTarget {
  if (from === 'dashboard') {
    return { href: hrefFor('nexusOneDashboard.waivers'), label: 'Dashboard Waivers' };
  }
  return { href: hrefFor('nexusOneWaivers'), label: 'Waivers' };
}

function classicHref(path: string): string {
  return bundleIndexUrl('classic', path);
}

function classicWaiverDetailHref(route: ParsedRoute): string {
  if (!route.ownerType || !route.ownerId || !route.waiverId) {
    return classicHref('/dashboard/waiverRequests');
  }
  return classicHref(
    `/waiver/details/${encodeURIComponent(route.ownerType)}/${encodeURIComponent(route.ownerId)}/${encodeURIComponent(route.waiverId)}/waiver`,
  );
}

const WAIVER_TABS = [
  {
    value: 'overview',
    label: 'Overview',
    testId: 'preview-waiver-detail-tab-overview',
  },
] as const;

/** Meta strip entry: a dimmed label followed by its value chip. */
function MetaItem({
  label,
  children,
  testId,
}: {
  readonly label: string;
  readonly children: ReactNode;
  readonly testId: string;
}): ReactElement {
  return (
    <Flex align="center" gap="2" data-testid={testId}>
      <Text size="1" color="gray">
        {label}
      </Text>
      {children}
    </Flex>
  );
}

function ExpiryValue({
  waiver,
  expiredBadgeTestId,
}: {
  readonly waiver: PolicyWaiverDetailDTO;
  /** Unique per placement — ExpiryValue appears in both the meta strip and the card. */
  readonly expiredBadgeTestId: string;
}): ReactElement {
  const expiry = describeWaiverExpiry(waiver);
  return (
    <Flex align="center" gap="2" wrap="wrap">
      <Text size="2">{expiry.label}</Text>
      {expiry.expired && (
        <Badge color="red" variant="solid" data-testid={expiredBadgeTestId}>
          Expired
        </Badge>
      )}
      {expiry.relative && (
        <Text size="1" color="gray">
          {expiry.relative}
        </Text>
      )}
    </Flex>
  );
}

export default function WaiverDetailPage(): ReactElement {
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();

  const route = useMemo<ParsedRoute>(
    () => ({
      ownerType: normalizeOwnerType(
        typeof params.ownerType === 'string' ? params.ownerType : undefined,
      ),
      ownerId: typeof params.ownerId === 'string' ? params.ownerId : null,
      waiverId: typeof params.waiverId === 'string' ? params.waiverId : null,
    }),
    [params.ownerType, params.ownerId, params.waiverId],
  );
  const backLink = useMemo<BackLinkTarget>(
    () =>
      computeBackLink(typeof params.from === 'string' ? params.from : null, (s) =>
        stateService.href(s),
      ),
    [params.from, stateService],
  );
  const { loading, error, waiver, refetch } = useWaiverDetail(
    route.ownerType,
    route.ownerId,
    route.waiverId,
  );

  // Stabilize the empty fallback so the header memo does not recompute every render.
  const constraints = useMemo(() => waiver?.constraintFacts ?? [], [waiver?.constraintFacts]);

  const context = useMemo(() => {
    if (!waiver?.vulnerabilityId) return null;
    return resolveEntityDetailContext({
      current: 'vulnerability',
      vulnId: waiver.vulnerabilityId,
    });
  }, [waiver?.vulnerabilityId]);

  const breadcrumb = useMemo(
    () => (
      <Flex align="center" gap="2" data-testid="preview-waiver-detail-breadcrumb">
        <Link
          href={backLink.href}
          size="2"
          color="gray"
          data-testid="preview-waiver-detail-back-link"
        >
          <Flex align="center" gap="1">
            <ActionIcons.ChevronLeft size={14} />
            <span>{backLink.label}</span>
          </Flex>
        </Link>
        <Text size="2" color="gray">
          /
        </Text>
        <Text size="2" weight="medium">
          Waiver Details
        </Text>
      </Flex>
    ),
    [backLink.href, backLink.label],
  );

  const header = useMemo(() => {
    if (error) {
      return (
        <Flex
          direction="column"
          gap="3"
          align="start"
          p="4"
          data-testid="preview-waiver-detail-header-error"
          style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-3)' }}
        >
          <Text size="2" color="red">
            Failed to load waiver{route.waiverId ? <> <code>{route.waiverId}</code></> : null}.
          </Text>
          <Button
            size="2"
            variant="soft"
            onClick={refetch}
            data-testid="preview-waiver-detail-header-retry"
          >
            Retry
          </Button>
        </Flex>
      );
    }
    // useWaiverDetail reports loading until the active key matches; keep a
    // skeleton if we ever see ready/idle with no waiver so the slot is not blank.
    if (loading || !waiver) {
      return <LoadingSkeleton height={96} data-testid="preview-waiver-detail-header-loading" />;
    }
    return (
      <Flex direction="column" gap="3" data-testid="preview-waiver-detail-header">
        <Flex align="center" gap="3" wrap="wrap">
          <ViolationThreatBadge threat={waiver.threatLevel} size="2" />
          {/* mb="0" drops the heading token's bottom margin so it centers against the badge. */}
          <PageHeading mb="0">{waiver.policyName ?? 'Policy waiver'}</PageHeading>
          {waiver.isAutoWaiver && (
            <Badge color="green" variant="soft">
              Auto-waiver
            </Badge>
          )}
          {waiver.forContainerImage && (
            <Badge color="blue" variant="soft">
              Container image
            </Badge>
          )}
        </Flex>

        {constraints.length > 0 && (
          <Box data-testid="preview-waiver-detail-constraint">
            {constraints.map((constraint, constraintIndex) => (
              <Box key={constraintIndex} mt={constraintIndex === 0 ? '0' : '2'}>
                <Text as="p" size="2">
                  <strong>{constraint.constraintName ?? 'This constraint'}</strong> is in violation
                  for:
                </Text>
                <ul
                  data-testid="preview-waiver-detail-conditions"
                  style={{
                    margin: 'var(--space-1) 0 0 0',
                    paddingLeft: 'var(--space-5)',
                  }}
                >
                  {(constraint.conditionFacts ?? []).map((condition, conditionIndex) => (
                    <li key={conditionIndex}>
                      <Text size="2" color="gray">
                        {condition.reason ?? '—'}
                      </Text>
                    </li>
                  ))}
                </ul>
              </Box>
            ))}
          </Box>
        )}

        <Flex align="center" gap="4" wrap="wrap" data-testid="preview-waiver-detail-meta">
          <MetaItem label="Scope:" testId="preview-waiver-detail-meta-scope">
            <Badge color="gray" variant="soft">
              {formatWaiverScopeLabel(waiver)}
            </Badge>
          </MetaItem>
          {/* Container-image waivers use the Container image badge; Classic hid the component card. */}
          {!waiver.forContainerImage && (
            <MetaItem label="Component:" testId="preview-waiver-detail-meta-component">
              <Badge
                color="gray"
                variant="soft"
                style={{ fontFamily: 'var(--code-font-family)' }}
                title={
                  waiver.matcherStrategy ? `Matcher: ${waiver.matcherStrategy}` : undefined
                }
              >
                {formatWaiverComponentLabel(waiver)}
              </Badge>
              {waiver.componentUpgradeAvailable && (
                <Badge color="green" variant="soft">
                  Upgrade available
                </Badge>
              )}
            </MetaItem>
          )}
          <MetaItem label="Expires:" testId="preview-waiver-detail-meta-expires">
            <ExpiryValue
              waiver={waiver}
              expiredBadgeTestId="preview-waiver-detail-meta-expired-badge"
            />
          </MetaItem>
        </Flex>
      </Flex>
    );
  }, [constraints, error, loading, refetch, route.waiverId, waiver]);

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={context}
      tabs={WAIVER_TABS}
      activeTab="overview"
      // Single Overview tab today; wire when a second tab is added.
      onTabChange={() => undefined}
      mainTestId="preview-waiver-detail-page"
      testIdPrefix="preview-waiver-detail"
    >
      {/* Load/error chrome lives in the header slot (Application detail pattern). */}
      {waiver && (
        <Box mt="4" data-testid="preview-waiver-detail-body">
          <Card style={{ maxWidth: 880 }}>
            <Flex direction="column" p="4">
              <Heading size="4" mb="3">
                Waiver Details
              </Heading>

              <EntityDetailRow label="Created" testId="preview-waiver-detail-created">
                <Text size="2">
                  {formatWaiverCalendarDate(waiver.createTime)}
                  {waiver.creatorName ? ` by ${waiver.creatorName}` : ''}
                </Text>
              </EntityDetailRow>

              <EntityDetailRow label="Expires" testId="preview-waiver-detail-expires">
                <ExpiryValue
                  waiver={waiver}
                  expiredBadgeTestId="preview-waiver-detail-expired-badge"
                />
              </EntityDetailRow>

              <EntityDetailRow label="Reason" testId="preview-waiver-detail-reason">
                <Text size="2">{waiver.reasonText ?? '—'}</Text>
              </EntityDetailRow>

              {waiver.vulnerabilityId && (
                <EntityDetailRow label="Vulnerability" testId="preview-waiver-detail-vulnerability">
                  <Link
                    size="2"
                    href={vulnerabilityDetailHref({ vulnId: waiver.vulnerabilityId })}
                    data-testid="preview-waiver-detail-vuln-link"
                  >
                    {waiver.vulnerabilityId}
                  </Link>
                </EntityDetailRow>
              )}

              <EntityDetailRow label="Comments" testId="preview-waiver-detail-comments">
                <Box
                  pl="3"
                  style={{
                    borderLeft: '2px solid var(--gray-5)',
                    whiteSpace: 'pre-wrap',
                  }}
                >
                  <Text size="2" color="gray">
                    {waiver.comment || 'No additional comments'}
                  </Text>
                </Box>
              </EntityDetailRow>

              <Flex
                justify="end"
                gap="4"
                pt="3"
                style={{ borderTop: '1px solid var(--gray-4)' }}
                data-testid="preview-waiver-detail-actions"
              >
                <Link
                  size="2"
                  href={classicHref('/dashboard/waivers')}
                  data-testid="preview-waiver-detail-create-classic"
                >
                  View Waivers in Classic →
                </Link>
                <Link
                  size="2"
                  href={classicWaiverDetailHref(route)}
                  data-testid="preview-waiver-detail-classic-link"
                >
                  Manage in Classic →
                </Link>
              </Flex>
            </Flex>
          </Card>
        </Box>
      )}
    </EntityDetailLayout>
  );
}
