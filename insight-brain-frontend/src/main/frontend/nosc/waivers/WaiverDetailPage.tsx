/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo, type ReactElement } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Flex,
  Heading,
  Link,
  Separator,
  Text,
} from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useWaiverDetail } from './useWaivers';
import {
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverDetailExpiry,
  waiverThreatColor,
} from './waiverDisplayUtils';

/**
 * Native Nexus One Waiver Detail page (CLM-40007 / CLM-42773).
 *
 * Mounted at `/waivers/{ownerType}/{ownerId}/{waiverId}`. Uses EntityDetailLayout
 * for shared detail chrome; content remains read-only with Classic manage escape.
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

function normalizeOwnerType(rawOwnerType: string | undefined): string | null {
  if (!rawOwnerType) return null;
  return OWNER_TYPE_MAP[rawOwnerType] ?? rawOwnerType;
}

interface BackLinkTarget {
  href: string;
  label: string;
}

function computeBackLink(from: string | null, hrefFor: (state: string) => string): BackLinkTarget {
  if (from === 'dashboard') {
    return { href: hrefFor('nexusOneDashboard.waivers'), label: 'Back to Dashboard Waivers' };
  }
  return { href: hrefFor('nexusOneWaivers'), label: 'All Waivers' };
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

  const constraint = waiver?.constraintFacts?.[0];
  const conditions = constraint?.conditionFacts ?? [];
  const scopeOwnerName = waiver?.scopeOwnerName ?? waiver?.ownerName ?? waiver?.ownerId ?? '';
  const scopeOwnerType = waiver?.scopeOwnerType ?? waiver?.ownerType ?? '';

  const context = useMemo(() => {
    if (!waiver?.vulnerabilityId) return null;
    return resolveEntityDetailContext({
      current: 'vulnerability',
      vulnId: waiver.vulnerabilityId,
    });
  }, [waiver?.vulnerabilityId]);

  const breadcrumb = useMemo(
    () => (
      <div data-testid="preview-waiver-detail-breadcrumb">
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
      </div>
    ),
    [backLink.href, backLink.label],
  );

  const header = useMemo(() => {
    if (loading) {
      return <LoadingSkeleton height={96} data-testid="preview-waiver-detail-header-loading" />;
    }
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
          <Button size="2" variant="soft" onClick={refetch} data-testid="preview-waiver-detail-header-retry">
            Retry
          </Button>
        </Flex>
      );
    }
    // useWaiverDetail usually reports loading until the active key matches; keep a
    // skeleton if we ever see ready/idle with no waiver so the header slot is not blank.
    if (!waiver) {
      return <LoadingSkeleton height={96} data-testid="preview-waiver-detail-header-loading" />;
    }
    return (
      <Flex align="center" gap="3" wrap="wrap" data-testid="preview-waiver-detail-header">
        <DomainIcons.Waivers size={28} color="var(--accent-9)" />
        <Heading size="6">{waiver.policyName ?? 'Policy waiver'}</Heading>
        <Badge color={waiverThreatColor(waiver.threatLevel)} variant="solid">
          Threat {waiver.threatLevel}
        </Badge>
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
    );
  }, [error, loading, refetch, route.waiverId, waiver]);

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
        <Flex direction="column" gap="5" data-testid="preview-waiver-detail-body">
          <Flex gap="4" wrap="wrap" align="center">
            <Text size="2" color="gray">
              Scope: <strong>{waiver.scope}</strong>
            </Text>
            {waiver.id && (
              <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                {waiver.id}
              </Text>
            )}
            <Link
              size="2"
              href={classicHref('/dashboard/waivers')}
              data-testid="preview-waiver-detail-create-classic"
            >
              View Waivers in Classic →
            </Link>
          </Flex>

          <Flex gap="5" wrap="wrap" align="stretch">
            <Flex direction="column" gap="5" style={{ flex: '2 1 520px', minWidth: 0 }}>
              <Card data-testid="preview-waiver-detail-constraint">
                <Flex direction="column" gap="3" p="4">
                  <Heading size="4">Policy constraint</Heading>
                  <Text size="2" color="gray">
                    Violations matching <strong>{constraint?.constraintName ?? '—'}</strong> will not
                    be reported.
                  </Text>
                  {conditions.length > 0 && (
                    <>
                      <Separator size="4" />
                      <Box>
                        <Text size="2" weight="medium">
                          Triggered by
                        </Text>
                        <ul
                          data-testid="preview-waiver-detail-conditions"
                          style={{ margin: '8px 0 0 16px', paddingLeft: 16 }}
                        >
                          {conditions.map((c, i) => (
                            <li key={i}>
                              <Text size="2">{c.reason ?? '—'}</Text>
                            </li>
                          ))}
                        </ul>
                      </Box>
                    </>
                  )}
                </Flex>
              </Card>

              {!waiver.forContainerImage && (
                <Card data-testid="preview-waiver-detail-component">
                  <Flex direction="column" gap="2" p="4">
                    <Heading size="4">Component</Heading>
                    <Text size="2" style={{ fontFamily: 'var(--code-font-family)' }}>
                      {formatWaiverComponentLabel(waiver)}
                    </Text>
                    {waiver.matcherStrategy && (
                      <Text size="2" color="gray">
                        Matcher: {waiver.matcherStrategy}
                      </Text>
                    )}
                    {waiver.componentUpgradeAvailable && (
                      <Badge color="green" variant="soft">
                        Upgrade available
                      </Badge>
                    )}
                  </Flex>
                </Card>
              )}

              <Card data-testid="preview-waiver-detail-reason">
                <Flex direction="column" gap="3" p="4">
                  <Heading size="4">Reason</Heading>
                  <Text size="2">{waiver.reasonText ?? '—'}</Text>
                  {waiver.comment && (
                    <>
                      <Separator size="4" />
                      <Box>
                        <Text size="2" weight="medium">
                          Comment
                        </Text>
                        <Text
                          as="p"
                          size="2"
                          color="gray"
                          style={{ marginTop: 4, whiteSpace: 'pre-wrap' }}
                        >
                          {waiver.comment}
                        </Text>
                      </Box>
                    </>
                  )}
                </Flex>
              </Card>
            </Flex>

            <Flex direction="column" gap="5" style={{ flex: '1 1 280px', minWidth: 0 }}>
              <Card data-testid="preview-waiver-detail-scope">
                <Flex direction="column" gap="3" p="4">
                  <Heading size="4">Scope</Heading>
                  <Box>
                    <Text size="2" color="gray">
                      {scopeOwnerType.charAt(0).toUpperCase() + scopeOwnerType.slice(1)}
                    </Text>
                    <Text as="p" size="3">
                      {scopeOwnerName || '—'}
                    </Text>
                  </Box>
                </Flex>
              </Card>

              <Card data-testid="preview-waiver-detail-lifecycle">
                <Flex direction="column" gap="3" p="4">
                  <Heading size="4">Lifecycle</Heading>
                  <Box>
                    <Text size="2" color="gray">
                      Created
                    </Text>
                    <Text as="p" size="2">
                      {formatWaiverCalendarDate(waiver.createTime)}
                    </Text>
                  </Box>
                  <Box>
                    <Text size="2" color="gray">
                      Expires
                    </Text>
                    <Text as="p" size="2">
                      {formatWaiverDetailExpiry(waiver)}
                    </Text>
                  </Box>
                  <Box>
                    <Text size="2" color="gray">
                      Created by
                    </Text>
                    <Text as="p" size="2">
                      {waiver.creatorName ?? '—'}
                    </Text>
                  </Box>
                </Flex>
              </Card>

              {waiver.vulnerabilityId && (
                <Card data-testid="preview-waiver-detail-vulnerability">
                  <Flex direction="column" gap="2" p="4">
                    <Heading size="4">Vulnerability</Heading>
                    <Link
                      size="2"
                      href={classicHref(`/vulnerabilities/${encodeURIComponent(waiver.vulnerabilityId)}`)}
                      data-testid="preview-waiver-detail-vuln-link"
                    >
                      {waiver.vulnerabilityId}
                    </Link>
                  </Flex>
                </Card>
              )}

              <Card data-testid="preview-waiver-detail-actions">
                <Flex direction="column" gap="2" p="4">
                  <Heading size="4">Manage</Heading>
                  <Text size="2" color="gray">
                    Editing, deleting, and bulk waiver actions live in Classic for Phase 1.
                  </Text>
                  <Link
                    size="2"
                    href={classicWaiverDetailHref(route)}
                    data-testid="preview-waiver-detail-classic-link"
                  >
                    Continue in Classic →
                  </Link>
                </Flex>
              </Card>
            </Flex>
          </Flex>
        </Flex>
      )}
    </EntityDetailLayout>
  );
}
