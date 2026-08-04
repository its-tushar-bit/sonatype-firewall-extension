/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Button, Dialog, Flex, Text } from '@radix-ui/themes';
import { useNoscToast } from 'MainRoot/nosc/toast/useNoscToast';
import {
  selectHasAutoWaiverManagement,
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import type { ApiAutoPolicyWaiverDTO } from 'MainRoot/nosc/waivers/waiverTypes';
import {
  createAutoWaiverExclusion,
  fetchApplicableAutoWaiverForViolation,
  normalizeAutoWaiverOwnerType,
} from 'MainRoot/nosc/waivers/autoWaiversApi';

export interface ExcludeAutoWaiverButtonProps {
  readonly policyViolationId: string;
  readonly applicationPublicId: string;
  readonly scanId: string | undefined;
  readonly isWaived: boolean;
  readonly onExcluded: () => void;
}

/**
 * Exclude (opt-out) for a violation currently covered by an auto-waiver.
 * Requires scanId + violation id — same contract as Classic report exclude.
 */
export default function ExcludeAutoWaiverButton({
  policyViolationId,
  applicationPublicId,
  scanId,
  isWaived,
  onExcluded,
}: ExcludeAutoWaiverButtonProps): JSX.Element | null {
  const toast = useNoscToast();
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const canManage = useSelector(selectHasAutoWaiverManagement);
  const featureReady = isAutoWaiversEnabled && isDeveloperDashboardEnabled;
  const [autoWaiver, setAutoWaiver] = useState<ApiAutoPolicyWaiverDTO | null>(null);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!featureReady || !isWaived || !canManage || !policyViolationId) {
      setAutoWaiver(null);
      return;
    }
    let cancelled = false;
    void fetchApplicableAutoWaiverForViolation(policyViolationId)
      .then((dto) => {
        if (!cancelled) setAutoWaiver(dto);
      })
      .catch(() => {
        if (!cancelled) setAutoWaiver(null);
      });
    return () => {
      cancelled = true;
    };
  }, [canManage, featureReady, isWaived, policyViolationId]);

  if (!featureReady || !canManage || !isWaived || !autoWaiver?.autoPolicyWaiverId || !scanId) {
    return null;
  }

  const confirm = async (): Promise<void> => {
    setBusy(true);
    setError(null);
    try {
      const ownerType = normalizeAutoWaiverOwnerType(autoWaiver.ownerType);
      await createAutoWaiverExclusion({
        ownerType,
        ownerId: autoWaiver.ownerId,
        body: {
          applicationPublicId,
          ownerId: autoWaiver.ownerId,
          scanId,
          policyViolationId,
          autoPolicyWaiverId: autoWaiver.autoPolicyWaiverId,
          matchStrategy: 'POLICY_VIOLATION',
        },
      });
      setOpen(false);
      toast.success('Excluded from auto-waiver');
      onExcluded();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to exclude from auto-waiver';
      setError(message);
      toast.error(message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <Button
        size="2"
        variant="soft"
        color="orange"
        onClick={() => {
          setError(null);
          setOpen(true);
        }}
        data-testid="nosc-violation-detail-exclude-auto-waiver"
      >
        Exclude from Auto-Waiver
      </Button>
      <Dialog.Root
        open={open}
        onOpenChange={(nextOpen) => {
          setOpen(nextOpen);
          if (nextOpen) setError(null);
        }}
      >
        <Dialog.Content maxWidth="420px" data-testid="nosc-exclude-auto-waiver-dialog">
          <Dialog.Title>Exclude from auto-waiver</Dialog.Title>
          <Dialog.Description size="2" mb="3">
            This creates an exclusion so this violation is no longer covered by the automatic
            waiver rule. The violation can reopen on the next evaluation.
          </Dialog.Description>
          {error && (
            <Text size="2" color="red" mb="3" data-testid="nosc-exclude-auto-waiver-error">
              {error}
            </Text>
          )}
          <Flex gap="3" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray" disabled={busy}>
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              color="orange"
              disabled={busy}
              onClick={() => void confirm()}
              data-testid="nosc-exclude-auto-waiver-confirm"
            >
              Exclude
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>
    </>
  );
}
