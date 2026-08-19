/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import {
  Badge,
  Box,
  Button,
  Dialog,
  Flex,
  Heading,
  Link as RadixLink,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { ActionIcons, DomainIcons } from 'MainRoot/nosc/icons';
import { LoadingSkeleton } from 'MainRoot/nosc/components/LoadingSkeleton';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import { useNoscToast } from 'MainRoot/nosc/toast/useNoscToast';
import { formatDateUtcYYYYMMDD } from 'MainRoot/util/dateUtils';
import {
  selectHasAutoWaiverManagement,
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import NewAutoWaiverModal, {
  type NewAutoWaiverModalValues,
} from 'MainRoot/nosc/waivers/NewAutoWaiverModal';
import {
  DEFAULT_AUTO_WAIVER_OWNER_ID,
  DEFAULT_AUTO_WAIVER_OWNER_TYPE,
  DEFAULT_AUTO_WAIVER_THREAT_LEVEL,
  MAX_LOCAL_AUTO_WAIVERS,
  deleteAutoPolicyWaiver,
  fetchApplicableAutoWaivers,
  formatAutoWaiverConditions,
  normalizeAutoWaiverOwnerType,
  type ApiAutoPolicyWaiverStatusDTO,
  type AutoWaiverOwnerType,
} from 'MainRoot/nosc/waivers/autoWaiversApi';

/**
 * Owner-scoped Auto-Waivers config list (CLM-43964).
 *
 * Scalability: one {@code applicableAutoWaivers} call per selected owner — never loop orgs/apps
 * and never decorate rows with per-config violation impact counts.
 */
export default function AutoWaiversPage(): JSX.Element {
  const toast = useNoscToast();
  const offsets = usePreviewShellOffsets();
  const { params } = useCurrentStateAndParams();
  const { stateService } = useRouter();
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);

  const ownerType = normalizeAutoWaiverOwnerType(
    typeof params.ownerType === 'string' ? params.ownerType : DEFAULT_AUTO_WAIVER_OWNER_TYPE,
  );
  const ownerId =
    typeof params.ownerId === 'string' && params.ownerId.trim()
      ? params.ownerId
      : DEFAULT_AUTO_WAIVER_OWNER_ID;

  const [rows, setRows] = useState<ReadonlyArray<ApiAutoPolicyWaiverStatusDTO>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editValues, setEditValues] = useState<NewAutoWaiverModalValues | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ApiAutoPolicyWaiverStatusDTO | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const featureReady = isAutoWaiversEnabled && isDeveloperDashboardEnabled;
  const canManage = Boolean(hasAutoWaiverManagement);

  const load = useCallback(async () => {
    if (!featureReady) {
      setLoading(false);
      setRows([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await fetchApplicableAutoWaivers({ ownerType, ownerId });
      setRows(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load auto-waivers');
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [featureReady, ownerId, ownerType]);

  useEffect(() => {
    void load();
  }, [load]);

  const localCount = useMemo(
    () => rows.filter((row) => row.isInherited !== true).length,
    [rows],
  );
  const createDisabledReason = !canManage
    ? 'Auto-Waiver management entitlement required'
    : localCount >= MAX_LOCAL_AUTO_WAIVERS
      ? `Maximum of ${MAX_LOCAL_AUTO_WAIVERS} local auto-waivers reached`
      : undefined;
  const createDisabled = Boolean(createDisabledReason);

  const openCreate = (): void => {
    if (createDisabled) return;
    setEditValues(null);
    setModalOpen(true);
  };

  const openEdit = (row: ApiAutoPolicyWaiverStatusDTO): void => {
    if (row.isInherited) return;
    setEditValues({
      autoPolicyWaiverId: row.autoPolicyWaiverId,
      threatLevel: row.threatLevel ?? DEFAULT_AUTO_WAIVER_THREAT_LEVEL,
      reachability: Boolean(row.hasNotReachable),
      pathForward: Boolean(row.hasNoPathForward),
      scopesOperatorAny: row.scopesOperatorAny !== false,
    });
    setModalOpen(true);
  };

  const confirmDelete = async (): Promise<void> => {
    if (!deleteTarget || deleteTarget.isInherited) return;
    setBusy(true);
    setDeleteError(null);
    try {
      await deleteAutoPolicyWaiver({
        ownerType: normalizeAutoWaiverOwnerType(deleteTarget.autoPolicyWaiverOwnerType),
        ownerId: deleteTarget.autoPolicyWaiverOwnerId,
        autoPolicyWaiverId: deleteTarget.autoPolicyWaiverId,
      });
      setDeleteTarget(null);
      toast.success('Auto-waiver deleted');
      await load();
    } catch (err: unknown) {
      // Keep the dialog open and show the failure in-dialog — page banner is behind the overlay.
      const message = err instanceof Error ? err.message : 'Failed to delete auto-waiver';
      setDeleteError(message);
      toast.error(message);
    } finally {
      setBusy(false);
    }
  };

  const goToDetail = (row: ApiAutoPolicyWaiverStatusDTO): void => {
    const detailOwnerType = normalizeAutoWaiverOwnerType(row.autoPolicyWaiverOwnerType);
    stateService.go('nexusOneAutoWaiverDetail', {
      ownerType: detailOwnerType,
      ownerId: row.autoPolicyWaiverOwnerId,
      autoPolicyWaiverId: row.autoPolicyWaiverId,
    });
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
      <main data-testid="nosc-auto-waivers-page">
        <Flex direction="column" gap="4">
          <Flex align="center" justify="between" gap="3" wrap="wrap">
            <Flex direction="column" gap="2">
              <RadixLink
                href={stateService.href('nexusOneWaivers')}
                size="2"
                data-testid="nosc-auto-waivers-back"
              >
                ← Waivers
              </RadixLink>
              <Flex align="center" gap="3">
                <DomainIcons.AutoWaiver size={28} color="var(--accent-9)" />
                <Heading size="6">Auto-Waivers</Heading>
                {!canManage && (
                  <Badge color="amber" variant="soft">
                    Enterprise Feature
                  </Badge>
                )}
              </Flex>
              <Text size="2" color="gray">
                Manage automatic waiver rules for{' '}
                <Text weight="medium" as="span">
                  {ownerType}/{ownerId}
                </Text>
                . One owner-scoped load; no estate-wide impact counts.
              </Text>
            </Flex>
            {featureReady && (
              createDisabledReason ? (
                <Tooltip content={createDisabledReason}>
                  <span>
                    <Button
                      size="2"
                      disabled
                      aria-label={`New Auto-Waiver (${createDisabledReason})`}
                      data-testid="nosc-auto-waivers-create"
                      data-disabled-reason={createDisabledReason}
                    >
                      <ActionIcons.Add size={14} aria-hidden /> New Auto-Waiver
                    </Button>
                  </span>
                </Tooltip>
              ) : (
                <Button
                  size="2"
                  onClick={openCreate}
                  data-testid="nosc-auto-waivers-create"
                >
                  <ActionIcons.Add size={14} aria-hidden /> New Auto-Waiver
                </Button>
              )
            )}
          </Flex>

          {!featureReady && (
            <Text size="2" color="gray" data-testid="nosc-auto-waivers-locked">
              Auto-Waivers require the feature flag and Developer Dashboard entitlement.
            </Text>
          )}

          {featureReady && loading && (
            <LoadingSkeleton height={200} data-testid="nosc-auto-waivers-loading" />
          )}

          {featureReady && error && (
            <Text size="2" color="red" data-testid="nosc-auto-waivers-error">
              {error}{' '}
              <Button size="1" variant="soft" onClick={() => void load()}>
                Retry
              </Button>
            </Text>
          )}

          {featureReady && !loading && !error && (
            <Table.Root variant="surface" data-testid="nosc-auto-waivers-table">
              <Table.Header>
                <Table.Row>
                  <Table.ColumnHeaderCell>Created</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Owner</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Max threat</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Conditions</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Source</Table.ColumnHeaderCell>
                  <Table.ColumnHeaderCell>Actions</Table.ColumnHeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                {rows.length === 0 ? (
                  <Table.Row>
                    <Table.Cell colSpan={6}>
                      <Text size="2" color="gray">
                        No auto-waiver configs apply to this owner.
                      </Text>
                    </Table.Cell>
                  </Table.Row>
                ) : (
                  rows.map((row) => (
                    <Table.Row key={row.autoPolicyWaiverId}>
                      <Table.Cell>
                        <Text size="2">
                          {row.createTime ? formatDateUtcYYYYMMDD(row.createTime) : '—'}
                        </Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{row.autoPolicyWaiverOwnerName ?? row.autoPolicyWaiverOwnerId}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        <Badge color="orange" variant="soft">
                          ≤ {row.threatLevel ?? '—'}
                        </Badge>
                      </Table.Cell>
                      <Table.Cell>
                        <Text size="2">{formatAutoWaiverConditions(row)}</Text>
                      </Table.Cell>
                      <Table.Cell>
                        {row.isInherited ? (
                          <Badge color="gray" variant="soft">
                            Inherited
                          </Badge>
                        ) : (
                          <Badge color="green" variant="soft">
                            Local
                          </Badge>
                        )}
                      </Table.Cell>
                      <Table.Cell>
                        <Flex gap="2" wrap="wrap">
                          <Button
                            size="1"
                            variant="soft"
                            onClick={() => goToDetail(row)}
                            data-testid={`nosc-auto-waiver-open-${row.autoPolicyWaiverId}`}
                          >
                            Details
                          </Button>
                          {!row.isInherited && canManage && (
                            <>
                              <Button
                                size="1"
                                variant="soft"
                                onClick={() => openEdit(row)}
                                data-testid={`nosc-auto-waiver-edit-${row.autoPolicyWaiverId}`}
                              >
                                Edit
                              </Button>
                              <Button
                                size="1"
                                color="red"
                                variant="soft"
                                onClick={() => {
                                  setDeleteError(null);
                                  setDeleteTarget(row);
                                }}
                                data-testid={`nosc-auto-waiver-delete-${row.autoPolicyWaiverId}`}
                              >
                                Delete
                              </Button>
                            </>
                          )}
                        </Flex>
                      </Table.Cell>
                    </Table.Row>
                  ))
                )}
              </Table.Body>
            </Table.Root>
          )}
        </Flex>

        <NewAutoWaiverModal
          open={modalOpen}
          onOpenChange={setModalOpen}
          ownerType={ownerType}
          ownerId={ownerId}
          canManage={canManage}
          initial={editValues}
          onSaved={() => void load()}
        />

        <Dialog.Root
          open={Boolean(deleteTarget)}
          onOpenChange={(open) => {
            if (!open) {
              setDeleteTarget(null);
              setDeleteError(null);
            }
          }}
        >
          <Dialog.Content maxWidth="400px" data-testid="nosc-auto-waiver-delete-dialog">
            <Dialog.Title>Delete auto-waiver</Dialog.Title>
            <Dialog.Description size="2" mb="3">
              This removes the automatic waiver rule. IQ has no disable flag — delete is how a
              config is turned off.
            </Dialog.Description>
            {deleteError && (
              <Text size="2" color="red" mb="3" data-testid="nosc-auto-waiver-delete-error">
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
                data-testid="nosc-auto-waiver-delete-confirm"
              >
                Delete
              </Button>
            </Flex>
          </Dialog.Content>
        </Dialog.Root>
      </main>
    </Box>
  );
}

export function autoWaiversListHref(
  stateService: { href: (state: string, params?: Record<string, string>) => string },
  ownerType: AutoWaiverOwnerType = DEFAULT_AUTO_WAIVER_OWNER_TYPE,
  ownerId: string = DEFAULT_AUTO_WAIVER_OWNER_ID,
): string {
  return stateService.href('nexusOneAutoWaivers', { ownerType, ownerId });
}
