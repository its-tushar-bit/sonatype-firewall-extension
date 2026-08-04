/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { Button, Dialog, Flex, Text } from '@radix-ui/themes';
import { extractAxiosMessage } from 'MainRoot/nosc/util/extractAxiosMessage';
import WaiverMutationFormFields, {
  DEFAULT_WAIVER_MUTATION_FORM,
  parseScopeKey,
  scopeKeyFor,
  type WaiverMutationFormState,
} from 'MainRoot/nosc/waivers/WaiverMutationFormFields';
import {
  createPolicyWaiverRequest,
  expiryDateToIsoEndOfDay,
  fetchPolicyWaiverReasons,
  fetchWaiverScopeTargets,
  type PolicyWaiverReason,
  type WaiverOwnerType,
  type WaiverScopeTarget,
} from 'MainRoot/nosc/waivers/waiversMutationApi';

export interface RequestWaiverModalProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly policyViolationId: string;
  readonly applicationPublicId: string;
  readonly policyId: string;
  readonly onRequested: (result: {
    readonly requestId: string;
    readonly ownerType: WaiverOwnerType;
    readonly ownerId: string;
  } | null) => void;
}

function buildOptions(form: WaiverMutationFormState) {
  return {
    comment: form.comment.trim() || null,
    matcherStrategy: form.matcherStrategy,
    expiryTime:
      form.expiryMode === 'date' && form.expiryDate
        ? expiryDateToIsoEndOfDay(form.expiryDate)
        : null,
    waiverReasonId: form.waiverReasonId || null,
    expireWhenRemediationAvailable: form.expiryMode === 'remediation',
    noteToReviewer: form.noteToReviewer.trim() || null,
  };
}

export default function RequestWaiverModal({
  open,
  onOpenChange,
  policyViolationId,
  applicationPublicId,
  policyId,
  onRequested,
}: RequestWaiverModalProps): JSX.Element {
  const [form, setForm] = useState<WaiverMutationFormState>(DEFAULT_WAIVER_MUTATION_FORM);
  const [scopes, setScopes] = useState<ReadonlyArray<WaiverScopeTarget>>([]);
  const [reasons, setReasons] = useState<ReadonlyArray<PolicyWaiverReason>>([]);
  const [loadingMeta, setLoadingMeta] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    setLoadingMeta(true);
    setError(null);
    setForm(DEFAULT_WAIVER_MUTATION_FORM);
    Promise.all([
      fetchWaiverScopeTargets({
        ownerType: 'application',
        ownerId: applicationPublicId,
        policyId,
      }),
      fetchPolicyWaiverReasons().catch(() => []),
    ])
      .then(([nextScopes, nextReasons]) => {
        if (cancelled) return;
        setScopes(nextScopes);
        setReasons(nextReasons);
        const preferred =
          nextScopes.find((scope) => scope.ownerType === 'application') ?? nextScopes[0];
        setForm({
          ...DEFAULT_WAIVER_MUTATION_FORM,
          scopeKey: preferred ? scopeKeyFor(preferred) : '',
        });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(extractAxiosMessage(err) || 'Failed to load request options');
      })
      .finally(() => {
        if (!cancelled) setLoadingMeta(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open, applicationPublicId, policyId]);

  const submit = async (): Promise<void> => {
    const scope = parseScopeKey(form.scopeKey);
    if (!scope) {
      setError('Select a waiver scope');
      return;
    }
    if (form.expiryMode === 'date' && !form.expiryDate) {
      setError('Pick an expiration date');
      return;
    }
    if (!form.comment.trim() && !form.waiverReasonId) {
      setError('Add a comment or select a reason');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const ownerType = scope.ownerType as WaiverOwnerType;
      const created = await createPolicyWaiverRequest({
        ownerType,
        ownerId: scope.ownerId,
        policyViolationId,
        options: buildOptions(form),
      });
      const requestId = created.policyWaiverRequestId ?? created.id ?? null;
      onOpenChange(false);
      onRequested(
        requestId
          ? { requestId, ownerType, ownerId: scope.ownerId }
          : null,
      );
    } catch (err: unknown) {
      setError(extractAxiosMessage(err) || 'Failed to request waiver');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content maxWidth="480px" data-testid="request-waiver-modal">
        <Dialog.Title>Request waiver</Dialog.Title>
        <Dialog.Description size="2" mb="3">
          Submit a waiver request for reviewer approval.
        </Dialog.Description>
        {loadingMeta ? (
          <Text size="2" color="gray">
            Loading scope options…
          </Text>
        ) : !error && scopes.length === 0 ? (
          <Text size="2" color="gray" data-testid="request-waiver-no-scopes">
            No waiver scopes are available for this violation.
          </Text>
        ) : scopes.length === 0 ? null : (
          <WaiverMutationFormFields
            form={form}
            onChange={setForm}
            scopes={scopes}
            reasons={reasons}
            showNoteToReviewer
            disabled={submitting}
            testIdPrefix="request-waiver"
          />
        )}
        {error && (
          <Text size="2" color="red" mt="3" data-testid="request-waiver-error">
            {error}
          </Text>
        )}
        <Flex gap="3" mt="4" justify="end">
          <Dialog.Close>
            <Button variant="soft" color="gray" disabled={submitting}>
              Cancel
            </Button>
          </Dialog.Close>
          <Button
            onClick={() => void submit()}
            disabled={loadingMeta || submitting || !form.scopeKey || scopes.length === 0}
            data-testid="request-waiver-submit"
          >
            {submitting ? 'Submitting…' : 'Submit request'}
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
