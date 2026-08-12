/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useState, type ReactElement } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { Badge, Box, Button, Card, Flex, Grid, Heading, Link as RadixLink, Text, Tooltip } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { estateComponentDetailHref } from 'MainRoot/nosc/components/detail/estateComponentDetailHref';
import { vulnerabilityDetailHref } from 'MainRoot/nosc/vulnerabilities/detail/vulnerabilityDetailHref';
import {
  selectViolationDetailIdentityState,
  selectViolationDetailWaiversState,
  selectViolationHasPermissionForAppWaivers,
  selectViolationWaiverPermissionError,
} from 'MainRoot/nosc/violations/detail/violationDetailSelectors';
import {
  selectHasWaiverRequestWorkflow,
  selectIsWaiverRequestWorkflowEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  fetchViolationIdentity,
  fetchViolationWaiverPermission,
  fetchViolationWaivers,
} from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import {
  componentDisplayNameLabel,
  getMostRecentScanId,
  getMostRecentStageEntry,
  getSecurityVulnerabilityRefId,
} from 'MainRoot/nosc/violations/detail/violationDetailUtils';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { ConstraintsSection } from 'MainRoot/nosc/violations/detail/ConstraintsSection';
import type { ViolationDetailsDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';
import CreateWaiverModal from 'MainRoot/nosc/waivers/CreateWaiverModal';
import RequestWaiverModal from 'MainRoot/nosc/waivers/RequestWaiverModal';
import ExcludeAutoWaiverButton from 'MainRoot/nosc/waivers/ExcludeAutoWaiverButton';
import {
  messageForWaiverActionDisableReason,
  readPendingWaiverRequestSessionFlag,
  resolveCreateWaiverDisableReason,
  resolveRequestWaiverDisableReason,
  writePendingWaiverRequestSessionFlag,
} from 'MainRoot/nosc/waivers/waiverActionEligibility';
// dateUtils.js is outside the TS program — import is untyped under strict .tsx.
import { formatTimeAgo } from 'MainRoot/util/dateUtils';

const FIRST_SEEN_DATE_TIME = new Intl.DateTimeFormat(undefined, {
  month: 'short',
  day: 'numeric',
  year: 'numeric',
  hour: 'numeric',
  minute: '2-digit',
});

function componentHref(details: ViolationDetailsDTO): string | undefined {
  if (!details.hash) {
    return undefined;
  }
  // Hash-only: violation DTOs expose applicationPublicId, not Path internal org/app ids.
  return estateComponentDetailHref(details.hash);
}

function formatFirstSeen(openTime: string): string | undefined {
  const ms = Date.parse(openTime);
  if (!Number.isFinite(ms)) {
    return undefined;
  }
  const absolute = FIRST_SEEN_DATE_TIME.format(new Date(ms));
  // Sub-minute ages map to "just now" from elapsed ms so we do not couple to
  // formatTimeAgo's exact sub-minute wording.
  const relative = Date.now() - ms < 60_000 ? 'just now' : formatTimeAgo(ms);
  return `${absolute} · ${relative}`;
}

function ActionButtonWithReason(props: {
  readonly label: string;
  readonly testId: string;
  readonly variant?: 'solid' | 'soft';
  readonly disabledReason: string | undefined;
  readonly onClick: () => void;
  readonly showLock?: boolean;
}): ReactElement {
  const { label, testId, variant = 'solid', disabledReason, onClick, showLock } = props;
  const disabled = Boolean(disabledReason);
  const ariaLabel = disabledReason ? `${label} (${disabledReason})` : label;
  const button = (
    <Button
      size="2"
      variant={variant}
      disabled={disabled}
      onClick={onClick}
      aria-label={ariaLabel}
      data-testid={testId}
      data-disabled-reason={disabledReason ?? undefined}
    >
      {label}
      {showLock ? (
        <>
          {' '}
          <ActionIcons.Lock size={14} aria-hidden />
        </>
      ) : null}
    </Button>
  );

  // Always wrap in a stable <span> so enable/disable transitions do not replace the
  // button node (keeps test refs / focus stable). Tooltip only when disabled — disabled
  // controls do not receive pointer events on their own.
  if (!disabledReason) {
    return <span>{button}</span>;
  }

  return (
    <Tooltip content={disabledReason}>
      <span>{button}</span>
    </Tooltip>
  );
}

export function OverviewTab(): ReactElement {
  const dispatch = useDispatch<any>();
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();
  const identityState = useSelector(selectViolationDetailIdentityState);
  const waiversState = useSelector(selectViolationDetailWaiversState);
  const hasPermissionForAppWaivers = useSelector(selectViolationHasPermissionForAppWaivers);
  const waiverPermissionError = useSelector(selectViolationWaiverPermissionError);
  const isWaiverRequestWorkflowEnabled = useSelector(selectIsWaiverRequestWorkflowEnabled);
  const hasWaiverRequestWorkflow = useSelector(selectHasWaiverRequestWorkflow);

  const details = identityState.data;
  const violationId = typeof params.id === 'string' ? params.id : details?.policyViolationId;
  const [createOpen, setCreateOpen] = useState(false);
  const [requestOpen, setRequestOpen] = useState(false);
  const [hasPendingRequest, setHasPendingRequest] = useState(false);

  // Restore same-tab pending gate without scanning the unbounded request list API.
  useEffect(() => {
    if (!violationId) {
      setHasPendingRequest(false);
      return;
    }
    setHasPendingRequest(readPendingWaiverRequestSessionFlag(violationId));
  }, [violationId]);

  const retryWaiverPermission = useCallback(() => {
    if (!violationId || !details?.applicationPublicId) return;
    void dispatch(
      fetchViolationWaiverPermission({
        violationId,
        applicationPublicId: details.applicationPublicId,
      }),
    );
  }, [details?.applicationPublicId, dispatch, violationId]);

  const handleWaiverCreated = useCallback(() => {
    // Diff against prior IDs — create returns 204 with no waiver id, and
    // activeWaivers has no createTime / sort guarantee. Prefer staying on the
    // violation when the created row cannot be identified uniquely.
    if (!violationId) return;
    const priorIds = new Set(
      waiversState.active.map((waiver) => waiver.policyWaiverId),
    );
    void dispatch(fetchViolationIdentity({ violationId }));
    void dispatch(fetchViolationWaivers({ violationId }))
      .unwrap()
      .then((waivers) => {
        const created = waivers.activeWaivers.find(
          (waiver) => !priorIds.has(waiver.policyWaiverId),
        );
        if (!created?.scopeOwnerType || !created.scopeOwnerId || !created.policyWaiverId) {
          return;
        }
        stateService.go('nexusOneWaiverDetail', {
          ownerType: String(created.scopeOwnerType).toLowerCase(),
          ownerId: created.scopeOwnerId,
          waiverId: created.policyWaiverId,
          from: 'waivers-list',
        });
      })
      .catch(() => undefined);
  }, [dispatch, stateService, violationId, waiversState.active]);

  if (identityState.status === 'loading' || identityState.status === 'idle') {
    return <LoadingSkeleton height={240} data-testid="nosc-violation-detail-overview-loading" />;
  }

  if (identityState.status === 'error' || !details || !violationId) {
    return (
      <Card mt="4" data-testid="nosc-violation-detail-overview-error">
        <Text size="2" color="red">
          Failed to load overview details.
        </Text>
      </Card>
    );
  }

  // HRC-owned (hosted-repository-component) violations intentionally have null
  // applicationPublicId / applicationName. All app-scoped UI (waiver actions,
  // waiver modals, Application row link, security-vuln href) is hidden for those.
  const applicationPublicId = details.applicationPublicId;
  const applicationName = details.applicationName;
  const isAppOwned = applicationPublicId != null && applicationName != null;

  // Prefer server aggregate; only consult active waivers once that fetch is ready
  // so a parallel waivers load cannot flash "Open" → "Waived".
  const isWaived = Boolean(
    details.waived || (waiversState.status === 'ready' && waiversState.active.length > 0),
  );
  const showRequestWaiver = Boolean(isWaiverRequestWorkflowEnabled);
  const isRequestWaiverGated = !hasWaiverRequestWorkflow;
  const canUseNativeWaiverModals = Boolean(details.policyId);
  const resolvedComponentHref = componentHref(details);
  const componentLabel = componentDisplayNameLabel(details.displayName, details.hash || 'Component');
  const latestStage = getMostRecentStageEntry(details.stageData);
  const firstSeen = formatFirstSeen(details.openTime);
  const securityRefId = getSecurityVulnerabilityRefId(details);
  const organizationLabel = details.organizationName?.trim() || undefined;
  const cveHref = securityRefId && applicationPublicId
    ? vulnerabilityDetailHref({
        vulnId: securityRefId,
        applicationPublicId,
        componentHash: details.hash,
        violationId,
        scanId: getMostRecentScanId(details.stageData),
      })
    : undefined;

  const createDisableReason = resolveCreateWaiverDisableReason({
    hasWaivePermission: hasPermissionForAppWaivers,
    isWaived,
  });
  const requestDisableReason = resolveRequestWaiverDisableReason({
    isWaived,
    hasPendingRequest,
    isEnterpriseGated: isRequestWaiverGated,
  });
  const createDisabledMessage = messageForWaiverActionDisableReason(createDisableReason);
  const requestDisabledMessage = messageForWaiverActionDisableReason(requestDisableReason);
  const canAddWaiver = createDisableReason === null;
  const canRequestWaiver = showRequestWaiver && requestDisableReason === null;

  const goToAddWaiver = (): void => {
    if (!canAddWaiver) return;
    if (canUseNativeWaiverModals) {
      setCreateOpen(true);
      return;
    }
    // Fallback when policyId is absent from the detail payload (should be rare).
    stateService.go('addWaiver', { violationId });
  };

  const goToRequestWaiver = (): void => {
    if (!canRequestWaiver) return;
    if (canUseNativeWaiverModals) {
      setRequestOpen(true);
      return;
    }
    stateService.go('requestWaiver', { violationId });
  };

  return (
    <Flex direction="column" gap="4" mt="4" data-testid="nosc-violation-detail-overview-tab">
      <Card>
        <Flex direction="column" gap="4">
          <Flex justify="between" align="start" gap="4" wrap="wrap">
            <Flex direction="column" gap="2">
              <Flex align="center" gap="2" wrap="wrap">
                <ViolationThreatBadge threat={details.threatLevel} size="2" />
                <Heading as="h2" size="4">
                  {details.policyName}
                </Heading>
                <Badge color={isWaived ? 'green' : 'orange'} variant="soft" radius="full">
                  {isWaived ? 'Waived' : 'Open'}
                </Badge>
              </Flex>
              <Text size="2" color="gray">
                {organizationLabel
                  ? `${details.policyThreatCategory} policy in ${organizationLabel}`
                  : `${details.policyThreatCategory} policy`}
              </Text>
            </Flex>

            {/* `isAppOwned` narrowing does not flow through JSX predicates, so the `applicationPublicId`
                check is re-stated here to satisfy TS narrowing for the `string` prop below. */}
            {isAppOwned && applicationPublicId && (
              <Flex gap="2" wrap="wrap">
                <ActionButtonWithReason
                  label="Create Waiver"
                  testId="nosc-violation-detail-add-waiver"
                  variant={canAddWaiver ? 'solid' : 'soft'}
                  disabledReason={createDisabledMessage}
                  onClick={goToAddWaiver}
                />
                <ExcludeAutoWaiverButton
                  policyViolationId={violationId}
                  applicationPublicId={applicationPublicId}
                  scanId={getMostRecentScanId(details.stageData)}
                  isWaived={isWaived}
                  onExcluded={() => {
                    void dispatch(fetchViolationIdentity({ violationId }));
                    void dispatch(fetchViolationWaivers({ violationId }));
                  }}
                />
                {hasPermissionForAppWaivers === null && waiverPermissionError && (
                  <Button
                    size="2"
                    variant="soft"
                    onClick={retryWaiverPermission}
                    data-testid="nosc-violation-detail-retry-waiver-permission"
                  >
                    Retry Add Waiver check
                  </Button>
                )}
                {showRequestWaiver && (
                  <ActionButtonWithReason
                    label="Request Waiver"
                    testId="nosc-violation-detail-request-waiver"
                    variant={canAddWaiver ? 'soft' : 'solid'}
                    disabledReason={requestDisabledMessage}
                    onClick={goToRequestWaiver}
                    showLock={isRequestWaiverGated}
                  />
                )}
              </Flex>
            )}
          </Flex>

          <Grid columns={{ initial: '1', sm: '2' }} gap="3">
            {applicationPublicId && applicationName ? (
              <Box>
                <Text as="p" size="1" color="gray" weight="medium">
                  Application
                </Text>
                <RadixLink href={`#/applications/${encodeURIComponent(applicationPublicId)}`}>
                  {applicationName}
                </RadixLink>
              </Box>
            ) : details.hrcId ? (
              <Box data-testid="nosc-violation-detail-hrc-source">
                <Text as="p" size="1" color="gray" weight="medium">
                  Source
                </Text>
                <Text size="2">Hosted repository component</Text>
              </Box>
            ) : null}
            {organizationLabel && (
              <Box>
                <Text as="p" size="1" color="gray" weight="medium">
                  Organization
                </Text>
                <Text size="2">{organizationLabel}</Text>
              </Box>
            )}
            <Box>
              <Text as="p" size="1" color="gray" weight="medium">
                Component
              </Text>
              {resolvedComponentHref ? (
                <RadixLink href={resolvedComponentHref}>{componentLabel}</RadixLink>
              ) : (
                <Text size="2" color="gray">
                  {componentLabel}
                </Text>
              )}
            </Box>
            {latestStage && (
              <Box>
                <Text as="p" size="1" color="gray" weight="medium">
                  Stage
                </Text>
                <Text size="2">{latestStage.stageId}</Text>
              </Box>
            )}
            {firstSeen && (
              <Box data-testid="nosc-violation-detail-first-seen">
                <Text as="p" size="1" color="gray" weight="medium">
                  First seen
                </Text>
                <Text size="2">{firstSeen}</Text>
              </Box>
            )}
            {securityRefId && cveHref && (
              <Box data-testid="nosc-violation-detail-cve">
                <Text as="p" size="1" color="gray" weight="medium">
                  CVE
                </Text>
                <RadixLink href={cveHref}>{securityRefId}</RadixLink>
              </Box>
            )}
            {details.reachabilityStatus && (
              <Box>
                <Text as="p" size="1" color="gray" weight="medium">
                  Reachability
                </Text>
                <Badge color="gray" variant="soft">
                  {details.reachabilityStatus}
                </Badge>
              </Box>
            )}
          </Grid>
        </Flex>
      </Card>

      <ConstraintsSection constraintViolations={details.constraintViolations} />

      {/* `applicationPublicId` re-stated for TS narrowing on the modal's `string` prop; see comment above. */}
      {details.policyId && isAppOwned && applicationPublicId && (
        <>
          <CreateWaiverModal
            open={createOpen}
            onOpenChange={setCreateOpen}
            policyViolationId={violationId}
            applicationPublicId={applicationPublicId}
            policyId={details.policyId}
            onCreated={handleWaiverCreated}
          />
          <RequestWaiverModal
            open={requestOpen}
            onOpenChange={setRequestOpen}
            policyViolationId={violationId}
            applicationPublicId={applicationPublicId}
            policyId={details.policyId}
            onRequested={(result) => {
              // Browser-session gate only — list API has no policyViolationId filter, so we
              // do not scan the full app request list on mount (enterprise-scale).
              writePendingWaiverRequestSessionFlag(violationId);
              setHasPendingRequest(true);
              void dispatch(fetchViolationIdentity({ violationId }));
              void dispatch(fetchViolationWaivers({ violationId }));
              if (!result) return;
              stateService.go('nexusOneWaiverDetail', {
                ownerType: result.ownerType,
                ownerId: result.ownerId,
                waiverId: result.requestId,
                from: 'waivers-list',
                requested: 'true',
              });
            }}
          />
        </>
      )}
    </Flex>
  );
}
