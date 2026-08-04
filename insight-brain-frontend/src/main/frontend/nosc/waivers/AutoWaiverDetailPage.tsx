/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import {
  Badge,
  Box,
  Button,
  Card,
  Dialog,
  Flex,
  Heading,
  Link as RadixLink,
  Table,
  Text,
} from '@radix-ui/themes';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { Pagination } from 'MainRoot/nosc/components/Pagination';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useNoscToast } from 'MainRoot/nosc/toast/useNoscToast';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import {
  selectHasAutoWaiverManagement,
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import type { ApiAutoPolicyWaiverDTO } from 'MainRoot/nosc/waivers/waiverTypes';
import {
  deleteAutoPolicyWaiver,
  deleteAutoWaiverExclusion,
  fetchAutoPolicyWaiver,
  fetchAutoWaiverExclusions,
  formatAutoWaiverConditions,
  normalizeAutoWaiverOwnerType,
  type ApiAutoPolicyWaiverExclusionDTO,
  type AutoWaiverOwnerType,
} from 'MainRoot/nosc/waivers/autoWaiversApi';

const EXCLUSIONS_PAGE_SIZE = 25;

/**
 * Auto-waiver config detail + paginated exclusion log (Include = delete exclusion).
 * No estate impact aggregates (Kitchen Sink).
 */
export default function AutoWaiverDetailPage(): JSX.Element {
  const toast = useNoscToast();
  const offsets = usePreviewShellOffsets();
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);

  const featureReady = isAutoWaiversEnabled && isDeveloperDashboardEnabled;
  const canManage = Boolean(hasAutoWaiverManagement);

  const ownerType = normalizeAutoWaiverOwnerType(
    typeof params.ownerType === 'string' ? params.ownerType : null,
  );
  const ownerId = typeof params.ownerId === 'string' ? params.ownerId : '';
  const autoPolicyWaiverId =
    typeof params.autoPolicyWaiverId === 'string' ? params.autoPolicyWaiverId : '';

  const [config, setConfig] = useState<ApiAutoPolicyWaiverDTO | null>(null);
  const [exclusions, setExclusions] = useState<ReadonlyArray<ApiAutoPolicyWaiverExclusionDTO>>([]);
  const [exclusionsPage, setExclusionsPage] = useState(1);
  const [exclusionsHasNext, setExclusionsHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [includeTarget, setIncludeTarget] = useState<ApiAutoPolicyWaiverExclusionDTO | null>(null);
  const [includeError, setIncludeError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const listHref = stateService.href('nexusOneAutoWaivers', { ownerType, ownerId });
  const threatDisplay = config?.threatLevel ?? 0;

  const load = useCallback(async () => {
    if (!featureReady) {
      setLoading(false);
      setConfig(null);
      setExclusions([]);
      return;
    }
    if (!ownerId || !autoPolicyWaiverId) {
      setError('Missing auto-waiver route params');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [cfg, excl] = await Promise.all([
        fetchAutoPolicyWaiver({ ownerType, ownerId, autoPolicyWaiverId }),
        fetchAutoWaiverExclusions({
          ownerType,
          ownerId,
          autoPolicyWaiverId,
          page: exclusionsPage,
          // Fetch one extra row so hasNext is exact (avoids empty last page when
          // total === EXCLUSIONS_PAGE_SIZE). Still O(1) round trips per page.
          pageSize: EXCLUSIONS_PAGE_SIZE + 1,
        }),
      ]);
      setConfig(cfg);
      const hasNext = excl.length > EXCLUSIONS_PAGE_SIZE;
      setExclusions(hasNext ? excl.slice(0, EXCLUSIONS_PAGE_SIZE) : excl);
      setExclusionsHasNext(hasNext);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load auto-waiver');
      setConfig(null);
      setExclusions([]);
      setExclusionsHasNext(false);
    } finally {
      setLoading(false);
    }
  }, [autoPolicyWaiverId, exclusionsPage, featureReady, ownerId, ownerType]);

  useEffect(() => {
    void load();
  }, [load]);

  const confirmDelete = async (): Promise<void> => {
    setBusy(true);
    setDeleteError(null);
    try {
      await deleteAutoPolicyWaiver({ ownerType, ownerId, autoPolicyWaiverId });
      setDeleteOpen(false);
      toast.success('Auto-waiver deleted');
      stateService.go('nexusOneAutoWaivers', { ownerType, ownerId });
    } catch (err: unknown) {
      // Keep dialog open — page banner sits behind the overlay.
      const message = err instanceof Error ? err.message : 'Failed to delete auto-waiver';
      setDeleteError(message);
      toast.error(message);
    } finally {
      setBusy(false);
    }
  };

  const confirmInclude = async (): Promise<void> => {
    if (!includeTarget) return;
    setBusy(true);
    setIncludeError(null);
    try {
      await deleteAutoWaiverExclusion({
        ownerType,
        ownerId,
        autoPolicyWaiverId,
        autoPolicyWaiverExclusionId: includeTarget.autoPolicyWaiverExclusionId,
      });
      setIncludeTarget(null);
      toast.success('Exclusion removed (included again)');
      // Avoid an empty page N after removing the last row — step back and let
      // the exclusionsPage dependency reload; otherwise refresh the current page.
      try {
        if (exclusions.length === 1 && exclusionsPage > 1) {
          setExclusionsPage((page) => Math.max(1, page - 1));
        } else {
          await load();
        }
      } catch (refreshErr: unknown) {
        setError(
          refreshErr instanceof Error
            ? `Exclusion removed, but refresh failed: ${refreshErr.message}`
            : 'Exclusion removed, but the list could not be refreshed',
        );
      }
    } catch (err: unknown) {
      // Keep dialog open — page banner sits behind the overlay.
      const message = err instanceof Error ? err.message : 'Failed to include (remove exclusion)';
      setIncludeError(message);
      toast.error(message);
    } finally {
      setBusy(false);
    }
  };

  return (
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
      <main data-testid="nosc-auto-waiver-detail-page">
        <Flex direction="column" gap="4">
          <RadixLink href={listHref} size="2" data-testid="nosc-auto-waiver-detail-back">
            ← Auto-Waivers
          </RadixLink>

          {!featureReady && (
            <Text size="2" color="gray" data-testid="nosc-auto-waiver-detail-locked">
              Auto-Waivers require the feature flag and Developer Dashboard entitlement.
            </Text>
          )}

          {featureReady && loading && (
            <LoadingSkeleton height={220} data-testid="nosc-auto-waiver-detail-loading" />
          )}

          {featureReady && error && (
            <Text size="2" color="red" data-testid="nosc-auto-waiver-detail-error">
              {error}
            </Text>
          )}

          {featureReady && !loading && config && (
            <>
              <Flex align="start" justify="between" gap="3" wrap="wrap">
                <Flex direction="column" gap="2">
                  <Heading size="6">Auto-waiver config</Heading>
                  <Text size="2" color="gray">
                    {config.ownerName ?? ownerId} · max threat ≤ {threatDisplay}
                  </Text>
                </Flex>
                {canManage && (
                  <Button
                    size="2"
                    color="red"
                    variant="soft"
                    onClick={() => {
                      setDeleteError(null);
                      setDeleteOpen(true);
                    }}
                    data-testid="nosc-auto-waiver-detail-delete"
                  >
                    Delete
                  </Button>
                )}
              </Flex>

              <Card>
                <Flex direction="column" gap="3" data-testid="nosc-auto-waiver-detail-overview">
                  <Flex gap="2" wrap="wrap">
                    <Badge color="orange" variant="soft">
                      Threat ≤ {threatDisplay}
                    </Badge>
                    <Badge color="gray" variant="soft">
                      {config.scopesOperatorAny ? 'Any condition' : 'All conditions'}
                    </Badge>
                  </Flex>
                  <Text size="2">
                    Conditions:{' '}
                    {formatAutoWaiverConditions({
                      reachability: config.reachability,
                      pathForward: config.pathForward,
                    })}
                  </Text>
                  <Text size="2" color="gray">
                    Created{' '}
                    {config.createTime ? formatDateUtcYYYYMMDD(config.createTime) : '—'}
                    {config.creatorName ? ` by ${config.creatorName}` : ''}
                  </Text>
                  <Text size="1" color="gray">
                    Exclude from a waived violation&apos;s Overview (needs scan + violation context).
                    Include restores coverage by removing an exclusion below.
                  </Text>
                </Flex>
              </Card>

              <Flex direction="column" gap="2">
                <Heading size="4">Exclusions</Heading>
                <Table.Root variant="surface" data-testid="nosc-auto-waiver-exclusions-table">
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell>Created</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {exclusions.length === 0 ? (
                      <Table.Row>
                        <Table.Cell colSpan={5}>
                          <Text size="2" color="gray">
                            No exclusions for this config.
                          </Text>
                        </Table.Cell>
                      </Table.Row>
                    ) : (
                      exclusions.map((exclusion) => (
                        <Table.Row key={exclusion.autoPolicyWaiverExclusionId}>
                          <Table.Cell>
                            <Text size="2">
                              {exclusion.createTime
                                ? formatDateUtcYYYYMMDD(exclusion.createTime)
                                : '—'}
                            </Text>
                          </Table.Cell>
                          <Table.Cell>
                            <Text size="2">{exclusion.componentDisplayName ?? '—'}</Text>
                          </Table.Cell>
                          <Table.Cell>
                            <Text size="2">{exclusion.policyName ?? '—'}</Text>
                          </Table.Cell>
                          <Table.Cell>
                            <Text size="2">{exclusion.threatLevel ?? '—'}</Text>
                          </Table.Cell>
                          <Table.Cell>
                            {canManage && (
                              <Button
                                size="1"
                                variant="soft"
                                onClick={() => setIncludeTarget(exclusion)}
                                data-testid={`nosc-auto-waiver-include-${exclusion.autoPolicyWaiverExclusionId}`}
                              >
                                Include
                              </Button>
                            )}
                          </Table.Cell>
                        </Table.Row>
                      ))
                    )}
                  </Table.Body>
                </Table.Root>
                {(exclusions.length > 0 || exclusionsPage > 1) && (
                  <Pagination
                    page={exclusionsPage}
                    pageSize={EXCLUSIONS_PAGE_SIZE}
                    hasNextPage={exclusionsHasNext}
                    onPageChange={setExclusionsPage}
                    data-testid="nosc-auto-waiver-exclusions-pagination"
                  />
                )}
              </Flex>
            </>
          )}
        </Flex>

        <Dialog.Root
          open={deleteOpen}
          onOpenChange={(open) => {
            setDeleteOpen(open);
            if (!open) setDeleteError(null);
          }}
        >
          <Dialog.Content maxWidth="400px" data-testid="nosc-auto-waiver-detail-delete-dialog">
            <Dialog.Title>Delete auto-waiver</Dialog.Title>
            <Dialog.Description size="2" mb="3">
              Permanently remove this automatic waiver rule.
            </Dialog.Description>
            {deleteError && (
              <Text size="2" color="red" mb="3" data-testid="nosc-auto-waiver-detail-delete-error">
                {deleteError}
              </Text>
            )}
            <Flex gap="3" justify="end">
              <Dialog.Close>
                <Button variant="soft" color="gray" disabled={busy}>
                  Cancel
                </Button>
              </Dialog.Close>
              <Button
                color="red"
                disabled={busy}
                onClick={() => void confirmDelete()}
                data-testid="nosc-auto-waiver-detail-delete-confirm"
              >
                Delete
              </Button>
            </Flex>
          </Dialog.Content>
        </Dialog.Root>

        <Dialog.Root
          open={Boolean(includeTarget)}
          onOpenChange={(open) => {
            if (!open) {
              setIncludeTarget(null);
              setIncludeError(null);
            }
          }}
        >
          <Dialog.Content maxWidth="400px" data-testid="nosc-auto-waiver-include-dialog">
            <Dialog.Title>Include (remove exclusion)</Dialog.Title>
            <Dialog.Description size="2" mb="3">
              This deletes the exclusion so the auto-waiver can cover the violation again.
            </Dialog.Description>
            {includeError && (
              <Text size="2" color="red" mb="3" data-testid="nosc-auto-waiver-include-error">
                {includeError}
              </Text>
            )}
            <Flex gap="3" justify="end">
              <Dialog.Close>
                <Button variant="soft" color="gray" disabled={busy}>
                  Cancel
                </Button>
              </Dialog.Close>
              <Button
                disabled={busy}
                onClick={() => void confirmInclude()}
                data-testid="nosc-auto-waiver-include-confirm"
              >
                Include
              </Button>
            </Flex>
          </Dialog.Content>
        </Dialog.Root>
      </main>
    </Box>
  );
}

export type { AutoWaiverOwnerType };
