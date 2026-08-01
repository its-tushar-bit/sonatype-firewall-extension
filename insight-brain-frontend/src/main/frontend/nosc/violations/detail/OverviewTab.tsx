/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, type ReactElement } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import { Badge, Box, Button, Card, Flex, Grid, Heading, Link as RadixLink, Text, Tooltip } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { ViolationThreatBadge } from 'MainRoot/nosc/violations/ViolationThreatBadge';
import { componentDetailHref } from 'MainRoot/nosc/components/detail/componentDetailHref';
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
import { fetchViolationWaiverPermission } from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import {
  componentDisplayNameLabel,
  getMostRecentScanId,
} from 'MainRoot/nosc/violations/detail/violationDetailUtils';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { ConstraintsSection } from 'MainRoot/nosc/violations/detail/ConstraintsSection';
import type { ViolationDetailsDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

function componentHref(details: ViolationDetailsDTO): string | undefined {
  if (!details.hash) {
    return undefined;
  }

  const scanId = getMostRecentScanId(details.stageData);
  return componentDetailHref(details.applicationPublicId, details.hash, scanId);
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

  const retryWaiverPermission = useCallback(() => {
    if (!violationId || !details?.applicationPublicId) return;
    void dispatch(
      fetchViolationWaiverPermission({
        violationId,
        applicationPublicId: details.applicationPublicId,
      }),
    );
  }, [details?.applicationPublicId, dispatch, violationId]);

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

  // Prefer server aggregate; only consult active waivers once that fetch is ready
  // so a parallel waivers load cannot flash "Open" → "Waived".
  const isWaived = Boolean(
    details.waived || (waiversState.status === 'ready' && waiversState.active.length > 0),
  );
  const canAddWaiver = hasPermissionForAppWaivers === true;
  const showRequestWaiver = Boolean(isWaiverRequestWorkflowEnabled);
  const isRequestWaiverGated = !hasWaiverRequestWorkflow;
  const canRequestWaiver = showRequestWaiver && !isRequestWaiverGated;
  const resolvedComponentHref = componentHref(details);
  const componentLabel = componentDisplayNameLabel(details.displayName, details.hash || 'Component');

  const goToAddWaiver = (): void => {
    if (canAddWaiver) {
      stateService.go('addWaiver', { violationId });
    }
  };

  const goToRequestWaiver = (): void => {
    if (canRequestWaiver) {
      stateService.go('requestWaiver', { violationId });
    }
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
                {details.policyThreatCategory} policy in {details.organizationName}
              </Text>
            </Flex>

            <Flex gap="2" wrap="wrap">
              {canAddWaiver && (
                <Button size="2" onClick={goToAddWaiver} data-testid="nosc-violation-detail-add-waiver">
                  Add Waiver
                </Button>
              )}
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
              {showRequestWaiver &&
                (isRequestWaiverGated ? (
                  <Tooltip content="Enterprise Feature">
                    <span>
                      <Button
                        size="2"
                        variant={canAddWaiver ? 'soft' : 'solid'}
                        disabled
                        aria-label="Request Waiver (Enterprise Feature)"
                        data-testid="nosc-violation-detail-request-waiver"
                      >
                            Request Waiver <ActionIcons.Lock size={14} aria-hidden />
                      </Button>
                    </span>
                  </Tooltip>
                ) : (
                  <Button
                    size="2"
                    variant={canAddWaiver ? 'soft' : 'solid'}
                    onClick={goToRequestWaiver}
                    data-testid="nosc-violation-detail-request-waiver"
                  >
                    Request Waiver
                  </Button>
                ))}
            </Flex>
          </Flex>

          <Grid columns={{ initial: '1', sm: '2' }} gap="3">
            <Box>
              <Text as="p" size="1" color="gray" weight="medium">
                Application
              </Text>
              <RadixLink href={`#/applications/${encodeURIComponent(details.applicationPublicId)}`}>
                {details.applicationName}
              </RadixLink>
            </Box>
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
    </Flex>
  );
}
