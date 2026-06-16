/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import {
  Badge,
  Box,
  Card,
  Flex,
  Heading,
  Link,
  Separator,
  Text,
} from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { AsyncPageState } from 'MainRoot/nosc/components/AsyncPageState';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { bundleIndexUrl } from 'MainRoot/util/urlUtil';
import { useWaiverDetail } from './useWaivers';
import type { PolicyWaiverDetailDTO } from './waiverTypes';
import {
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverDetailExpiry,
  waiverThreatColor,
} from './waiverDisplayUtils';

/**
 * Native Nexus One Waiver Detail page (CLM-39545 / CLM-39709).
 *
 * Mounted at `/waivers/{ownerType}/{ownerId}/{waiverId}`. Reads live from
 * `GET /api/v2/policyWaivers/{ownerType}/{ownerId}/{waiverId}`.
 *
 * The Classic page is `WaiverDetails.jsx`; this page renders the same field set
 * in Radix idiom (header + constraint / scope / component / reason / created
 * cards + a "Continue in Classic" escape hatch).
 *
 * Route params come from UI-Router (`useCurrentStateAndParams`). ownerType is
 * normalized via the same map Classic uses internally (root_organization →
 * organization, all_repositories → repository_container) so the backend call
 * shape matches Classic's.
 *
 * TODO(CLM-39709): edit / delete / bulk waiver actions and the vulnerability
 * detail modal still live in Classic; deferred to avoid wiring half a workflow.
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

/** Normalize the raw ownerType segment to the value Classic's backend expects. */
function normalizeOwnerType(rawOwnerType: string | undefined): string | null {
  if (!rawOwnerType) return null;
  return OWNER_TYPE_MAP[rawOwnerType] ?? rawOwnerType;
}

interface BackLinkTarget {
  href: string;
  label: string;
}

/**
 * `?from=<value>` is an opaque marker set by the caller (WaiversTable via its
 * `linkFrom` prop) so the back-link routes the user to wherever they came from:
 *   - 'dashboard' → Dashboard's Waivers tab
 *   - anything else / unset → standalone Waivers page
 */
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

export default function WaiverDetailPage(): JSX.Element {
  const offsets = usePreviewShellOffsets();
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
    route.waiverId
  );

  const constraint = waiver?.constraintFacts?.[0];
  const conditions = constraint?.conditionFacts ?? [];
  const scopeOwnerName = waiver?.scopeOwnerName ?? waiver?.ownerName ?? waiver?.ownerId ?? '';
  const scopeOwnerType = waiver?.scopeOwnerType ?? waiver?.ownerType ?? '';

  return (
    // Radix Theme is provided once by NexusOneShellLayout; render content into a
    // fixed, scrollable <main> region below the shell chrome.
    <Box
      asChild
      p="6"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      <main data-testid="preview-waiver-detail-page">
        {/* Back link */}
        <Box mb="4">
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
        </Box>

        <AsyncPageState
          loading={loading}
          error={error}
          onRetry={refetch}
          loadingHeight={320}
          loadingTestId="preview-waiver-detail-loading"
          errorTestId="preview-waiver-detail-error"
          errorTitle="Failed to load waiver"
        >
          {waiver && (
            <Flex direction="column" gap="5">
              {/* Header card */}
              <Card data-testid="preview-waiver-detail-header">
                <Flex direction="column" gap="3" p="4">
                  <Flex align="center" gap="3" wrap="wrap">
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
                  <Flex gap="4" wrap="wrap" align="center">
                    <Text size="2" color="gray">
                      Scope: <strong>{waiver.scope}</strong>
                    </Text>
                    {waiver.id && (
                      <Text size="2" color="gray" style={{ fontFamily: 'var(--code-font-family)' }}>
                        {waiver.id}
                      </Text>
                    )}
                  </Flex>
                </Flex>
              </Card>

              <Flex gap="5" wrap="wrap" align="stretch">
                {/* LEFT column: constraint + components + reason + comments */}
                <Flex direction="column" gap="5" style={{ flex: '2 1 520px', minWidth: 0 }}>
                  {/* Constraint */}
                  <Card data-testid="preview-waiver-detail-constraint">
                    <Flex direction="column" gap="3" p="4">
                      <Heading size="4">Policy constraint</Heading>
                      <Text size="2" color="gray">
                        Violations matching <strong>{constraint?.constraintName ?? '—'}</strong>{' '}
                        will not be reported.
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
  
                  {/* Component */}
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
  
                  {/* Reason + comment */}
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
  
                {/* RIGHT column: scope/dates/creator/actions */}
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
        </AsyncPageState>
      </main>
    </Box>
  );
}
