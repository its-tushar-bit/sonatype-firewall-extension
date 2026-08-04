/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactElement,
  type ReactNode,
} from 'react';
import { Badge, Box, Button, Card, Flex, Heading, Link, Text } from '@radix-ui/themes';
import { PageHeading } from '@sonatype/nexus-one-components';
import { extractAxiosMessage } from 'MainRoot/nosc/util/extractAxiosMessage';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { EntityDetailLayout } from 'MainRoot/nosc/entityDetail/EntityDetailLayout';
import { EntityDetailRow } from 'MainRoot/nosc/entityDetail/EntityDetailRow';
import { resolveEntityDetailContext } from 'MainRoot/nosc/entityDetail/resolveEntityDetailContext';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { useWaiverDetail } from './useWaivers';
import { WaiverSecurityDetailsTab } from './WaiverSecurityDetailsTab';
import WaiverDetailActions from './WaiverDetailActions';
import type { PolicyWaiverDetailDTO } from './waiverTypes';
import {
  describeWaiverExpiry,
  formatWaiverCalendarDate,
  formatWaiverComponentLabel,
  formatWaiverScopeLabel,
  normalizeWaiverOwnerTypeForApi,
} from './waiverDisplayUtils';
import {
  fetchPolicyWaiverRequest,
  type PolicyWaiverRequestDTO,
  type WaiverOwnerType,
} from './waiversMutationApi';

/**
 * Native Nexus One Waiver Detail page (CLM-40007 / CLM-42773 / CLM-43289).
 *
 * Mounted at `/waivers/{ownerType}/{ownerId}/{waiverId}`, reading a single
 * `GET /api/v2/policyWaivers/...` for manual waivers, or
 * `GET /api/v2/autoPolicyWaivers/...` when `?type=autoWaiver` is set (CLM-43502).
 * The Overview shows threat + policy header, constraint blurb, a
 * Scope/Component/Expires meta strip, and one Waiver Details card. Everything
 * renders from that one payload; no estate fan-out.
 *
 * A Security Details tab (CLM-43365) appears when the waiver names a
 * vulnerability, reading the existing `GET /api/v2/vulnerabilities/{refId}`.
 *
 * Mutations (extend, delete, approve/reject/withdraw) are wired via v2 APIs
 * (CLM-43963). Estate-expansion tabs stay deferred — see Kitchen Sink / CLM-42708.
 */

interface ParsedRoute {
  ownerType: string | null;
  ownerId: string | null;
  waiverId: string | null;
  /** True when the `?type=autoWaiver` query param says this id is an auto-waiver. */
  isAutoWaiver: boolean;
  /** True when the id is a POLICY_WAIVER_REQUEST (`?requested=true`). */
  isRequested: boolean;
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

const OVERVIEW_TAB = {
  value: 'overview',
  label: 'Overview',
  testId: 'preview-waiver-detail-tab-overview',
} as const;

const SECURITY_TAB = {
  value: 'security-details',
  label: 'Security Details',
  testId: 'preview-waiver-detail-tab-security',
} as const;

type WaiverTabId = typeof OVERVIEW_TAB.value | typeof SECURITY_TAB.value;

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
      ownerType: normalizeWaiverOwnerTypeForApi(
        typeof params.ownerType === 'string' ? params.ownerType : undefined,
      ),
      ownerId: typeof params.ownerId === 'string' ? params.ownerId : null,
      waiverId: typeof params.waiverId === 'string' ? params.waiverId : null,
      isAutoWaiver: params.type === 'autoWaiver',
      isRequested: params.requested === 'true' || params.requested === true,
    }),
    [params.ownerType, params.ownerId, params.waiverId, params.type, params.requested],
  );
  const [request, setRequest] = useState<PolicyWaiverRequestDTO | null>(null);
  const [requestError, setRequestError] = useState<string | null>(null);
  const [requestLoading, setRequestLoading] = useState(false);
  const requestLoadGenerationRef = useRef(0);
  const backLink = useMemo<BackLinkTarget>(
    () =>
      computeBackLink(typeof params.from === 'string' ? params.from : null, (s) =>
        stateService.href(s),
      ),
    [params.from, stateService],
  );
  const waiverDetail = useWaiverDetail(
    route.isRequested ? null : route.ownerType,
    route.isRequested ? null : route.ownerId,
    route.isRequested ? null : route.waiverId,
    route.isAutoWaiver,
  );

  const requestRouteKey = route.isRequested
    ? `${route.ownerType}/${route.ownerId}/${route.waiverId}`
    : null;

  const reloadRequest = useCallback(() => {
    if (!route.isRequested || !route.ownerType || !route.ownerId || !route.waiverId) {
      setRequest(null);
      setRequestError(null);
      setRequestLoading(false);
      return;
    }
    // Bump generation so in-flight loads from a prior route cannot win the race.
    requestLoadGenerationRef.current += 1;
    const generation = requestLoadGenerationRef.current;
    const ownerType = route.ownerType as WaiverOwnerType;
    const ownerId = route.ownerId;
    const policyWaiverRequestId = route.waiverId;
    setRequestLoading(true);
    setRequestError(null);
    void fetchPolicyWaiverRequest({
      ownerType,
      ownerId,
      policyWaiverRequestId,
    })
      .then((data) => {
        if (generation !== requestLoadGenerationRef.current) return;
        setRequest(data);
      })
      .catch((err: unknown) => {
        if (generation !== requestLoadGenerationRef.current) return;
        setRequest(null);
        setRequestError(extractAxiosMessage(err) || 'Failed to load waiver request');
      })
      .finally(() => {
        if (generation !== requestLoadGenerationRef.current) return;
        setRequestLoading(false);
      });
  }, [route.isRequested, route.ownerType, route.ownerId, route.waiverId]);

  useEffect(() => {
    reloadRequest();
  }, [reloadRequest, requestRouteKey]);

  const loading = route.isRequested ? requestLoading : waiverDetail.loading;
  const error = route.isRequested ? requestError : waiverDetail.error;
  const waiver = route.isRequested ? null : waiverDetail.waiver;
  const refetch = route.isRequested ? reloadRequest : waiverDetail.refetch;

  // Stabilize the empty fallback so the header memo does not recompute every render.
  const constraints = useMemo(() => waiver?.constraintFacts ?? [], [waiver?.constraintFacts]);

  // Security Details only exists when the waiver names a vulnerability.
  const hasSecurityTab = Boolean(waiver?.vulnerabilityId);
  const tabs = useMemo(
    () => (hasSecurityTab ? [OVERVIEW_TAB, SECURITY_TAB] : [OVERVIEW_TAB]),
    [hasSecurityTab],
  );
  const [activeTab, setActiveTab] = useState<WaiverTabId>(OVERVIEW_TAB.value);
  // Keep Security Details mounted after the first visit so tab switches do not
  // re-fetch `GET /api/v2/vulnerabilities/{refId}`.
  const [securityTabVisited, setSecurityTabVisited] = useState(false);

  // Only reset after a settled load proves this waiver has no vulnerability —
  // during a waiverId swap `waiver` is briefly null and must not bounce tabs.
  useEffect(() => {
    if (loading || !waiver) return;
    if (!waiver.vulnerabilityId) {
      setActiveTab(OVERVIEW_TAB.value);
      setSecurityTabVisited(false);
    }
  }, [loading, waiver]);

  useEffect(() => {
    if (activeTab === SECURITY_TAB.value) setSecurityTabVisited(true);
  }, [activeTab]);

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
            Failed to load {route.isRequested ? 'waiver request' : 'waiver'}
            {route.waiverId ? <> <code>{route.waiverId}</code></> : null}.
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
    if (route.isRequested) {
      if (loading || !request) {
        return <LoadingSkeleton height={96} data-testid="preview-waiver-detail-header-loading" />;
      }
      return (
        <Flex direction="column" gap="3" data-testid="preview-waiver-detail-header">
          <Flex align="center" gap="3" wrap="wrap">
            <ViolationThreatBadge threat={request.threatLevel ?? 0} size="2" />
            <PageHeading mb="0">{request.policyName ?? 'Waiver request'}</PageHeading>
            <Badge
              color={
                (request.status ?? '').toUpperCase() === 'APPROVED'
                  ? 'green'
                  : (request.status ?? '').toUpperCase() === 'REJECTED'
                    ? 'red'
                    : 'orange'
              }
              variant="soft"
            >
              {(request.status ?? 'REQUESTED').toUpperCase()}
            </Badge>
          </Flex>
          {route.ownerType && route.ownerId && route.waiverId && (
            <WaiverDetailActions
              key={`request-${route.ownerType}-${route.ownerId}-${route.waiverId}`}
              ownerType={route.ownerType as WaiverOwnerType}
              ownerId={route.ownerId}
              waiverId={route.waiverId}
              isRequested
              isAutoWaiver={false}
              waiver={null}
              request={request}
              onChanged={reloadRequest}
              onDeletedOrWithdrawn={() => stateService.go('nexusOneWaivers')}
            />
          )}
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
        {route.ownerType && route.ownerId && route.waiverId && (
          <WaiverDetailActions
            key={`waiver-${route.ownerType}-${route.ownerId}-${route.waiverId}`}
            ownerType={route.ownerType as WaiverOwnerType}
            ownerId={route.ownerId}
            waiverId={route.waiverId}
            isRequested={false}
            isAutoWaiver={route.isAutoWaiver}
            waiver={waiver}
            request={null}
            onChanged={refetch}
            onDeletedOrWithdrawn={() => stateService.go('nexusOneWaivers')}
          />
        )}

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
  }, [
    constraints,
    error,
    loading,
    refetch,
    reloadRequest,
    request,
    route.isAutoWaiver,
    route.isRequested,
    route.ownerId,
    route.ownerType,
    route.waiverId,
    stateService,
    waiver,
  ]);

  return (
    <EntityDetailLayout
      breadcrumb={breadcrumb}
      header={header}
      context={context}
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={(next) => {
        if (next === OVERVIEW_TAB.value || next === SECURITY_TAB.value) setActiveTab(next);
      }}
      mainTestId="preview-waiver-detail-page"
      testIdPrefix="preview-waiver-detail"
    >
      {waiver && securityTabVisited && waiver.vulnerabilityId && (
        <Box
          style={{ display: activeTab === SECURITY_TAB.value ? undefined : 'none' }}
          aria-hidden={activeTab !== SECURITY_TAB.value}
        >
          <WaiverSecurityDetailsTab
            waiver={waiver}
            vulnerabilityId={waiver.vulnerabilityId}
            ownerType={route.ownerType}
            ownerId={route.ownerId}
          />
        </Box>
      )}

      {route.isRequested && request && activeTab === OVERVIEW_TAB.value && (
        <Box mt="4" data-testid="preview-waiver-detail-body">
          <Card style={{ maxWidth: 880 }}>
            <Flex direction="column" p="4">
              <Heading size="4" mb="3">
                Request Details
              </Heading>
              <EntityDetailRow label="Requester" testId="preview-waiver-detail-requester">
                <Text size="2">{request.requesterName ?? '—'}</Text>
              </EntityDetailRow>
              <EntityDetailRow label="Note to reviewer" testId="preview-waiver-detail-note">
                <Text size="2">{request.noteToReviewer ?? '—'}</Text>
              </EntityDetailRow>
              <EntityDetailRow label="Comments" testId="preview-waiver-detail-comments">
                <Text size="2" color="gray">
                  {request.comment || 'No additional comments'}
                </Text>
              </EntityDetailRow>
              {request.rejectionReason && (
                <EntityDetailRow label="Rejection reason" testId="preview-waiver-detail-rejection">
                  <Text size="2" color="red">
                    {request.rejectionReason}
                  </Text>
                </EntityDetailRow>
              )}
            </Flex>
          </Card>
        </Box>
      )}

      {/* Load/error chrome lives in the header slot (Application detail pattern). */}
      {waiver && activeTab === OVERVIEW_TAB.value && (
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

            </Flex>
          </Card>
        </Box>
      )}
    </EntityDetailLayout>
  );
}
