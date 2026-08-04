/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { Button, Dialog, Flex, Text, TextArea, TextField } from '@radix-ui/themes';
import { extractAxiosMessage } from 'MainRoot/nosc/util/extractAxiosMessage';
import type { PolicyWaiverDetailDTO } from 'MainRoot/nosc/waivers/waiverTypes';
import {
  deletePolicyWaiver,
  expiryDateToIsoEndOfDay,
  reviewPolicyWaiverRequest,
  updatePolicyWaiver,
  withdrawPolicyWaiverRequest,
  type PolicyWaiverRequestDTO,
  type WaiverMatcherStrategy,
  type WaiverOwnerType,
} from 'MainRoot/nosc/waivers/waiversMutationApi';

export interface WaiverDetailActionsProps {
  readonly ownerType: WaiverOwnerType;
  readonly ownerId: string;
  readonly waiverId: string;
  readonly isRequested: boolean;
  readonly isAutoWaiver: boolean;
  readonly waiver: PolicyWaiverDetailDTO | null;
  readonly request: PolicyWaiverRequestDTO | null;
  readonly onChanged: () => void;
  readonly onDeletedOrWithdrawn: () => void;
}

function toDateInputValue(value: string | number | null | undefined): string {
  if (value == null || value === '') return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function todayDateInputValue(): string {
  return toDateInputValue(Date.now());
}

export default function WaiverDetailActions({
  ownerType,
  ownerId,
  waiverId,
  isRequested,
  isAutoWaiver,
  waiver,
  request,
  onChanged,
  onDeletedOrWithdrawn,
}: WaiverDetailActionsProps): JSX.Element | null {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [extendOpen, setExtendOpen] = useState(false);
  const [extendDate, setExtendDate] = useState('');
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  // Hide decide actions as soon as approve/reject succeeds so a second click cannot
  // race the parent reloadRequest() round trip.
  const [reviewComplete, setReviewComplete] = useState(false);

  if (isAutoWaiver) return null;

  const requestStatus = (request?.status ?? '').toUpperCase();
  // Approve/Reject need WAIVE; fail closed when canReview is absent.
  const canDecideRequest =
    isRequested
    && requestStatus === 'REQUESTED'
    && request?.canReview === true
    && !reviewComplete;
  // Withdraw is requester-only server-side; do not gate on canReview (WAIVE).
  const canWithdrawRequest =
    isRequested
    && requestStatus === 'REQUESTED'
    && !reviewComplete;
  const canMutateCommitted = !isRequested && !!waiver;

  const run = async (action: () => Promise<void>, after?: () => void): Promise<void> => {
    setBusy(true);
    setError(null);
    try {
      await action();
      after?.();
    } catch (err: unknown) {
      setError(extractAxiosMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const matcherStrategy = (waiver?.matcherStrategy
    || request?.matcherStrategy
    || 'EXACT_COMPONENT') as WaiverMatcherStrategy;
  const requestWaiverReasonId = request?.policyWaiverReasonId ?? null;
  const committedWaiverReasonId = waiver?.policyWaiverReasonId ?? null;

  return (
    <Flex direction="column" gap="2" data-testid="waiver-detail-actions">
      <Flex gap="2" wrap="wrap">
        {canMutateCommitted && (
          <>
            <Button
              size="2"
              variant="soft"
              disabled={busy}
              onClick={() => {
                setExtendDate(toDateInputValue(waiver?.expiryTime));
                setExtendOpen(true);
              }}
              data-testid="waiver-detail-extend"
            >
              Extend
            </Button>
            <Button
              size="2"
              color="red"
              variant="soft"
              disabled={busy}
              onClick={() => setDeleteOpen(true)}
              data-testid="waiver-detail-delete"
            >
              Delete
            </Button>
          </>
        )}
        {canDecideRequest && (
          <>
            <Button
              size="2"
              disabled={busy}
              onClick={() =>
                void run(
                  async () => {
                    await reviewPolicyWaiverRequest({
                      ownerType,
                      ownerId,
                      policyWaiverRequestId: waiverId,
                      review: {
                        status: 'APPROVED',
                        matcherStrategy,
                        comment: request?.comment ?? null,
                        expiryTime: request?.expiryTime ?? null,
                        waiverReasonId: requestWaiverReasonId,
                        expireWhenRemediationAvailable:
                          request?.expireWhenRemediationAvailable ?? false,
                      },
                    });
                  },
                  () => {
                    setReviewComplete(true);
                    onChanged();
                  },
                )
              }
              data-testid="waiver-detail-approve"
            >
              Approve
            </Button>
            <Button
              size="2"
              color="red"
              variant="soft"
              disabled={busy}
              onClick={() => setRejectOpen(true)}
              data-testid="waiver-detail-reject"
            >
              Reject
            </Button>
          </>
        )}
        {canWithdrawRequest && (
          <Button
            size="2"
            variant="outline"
            color="gray"
            disabled={busy}
            onClick={() => setWithdrawOpen(true)}
            data-testid="waiver-detail-withdraw"
          >
            Withdraw
          </Button>
        )}
      </Flex>
      {error && (
        <Text size="2" color="red" data-testid="waiver-detail-action-error">
          {error}
        </Text>
      )}

      <Dialog.Root
        open={extendOpen}
        onOpenChange={(open) => {
          setExtendOpen(open);
          if (!open) setExtendDate('');
        }}
      >
        <Dialog.Content maxWidth="400px" data-testid="waiver-detail-extend-dialog">
          <Dialog.Title>Extend waiver</Dialog.Title>
          <Dialog.Description size="2" mb="3">
            Set a new expiration date for this waiver.
          </Dialog.Description>
          <Flex direction="column" gap="1">
            <Text as="label" size="2" weight="medium" htmlFor="waiver-detail-extend-date">
              New expiration date
            </Text>
            <TextField.Root
              id="waiver-detail-extend-date"
              type="date"
              min={todayDateInputValue()}
              value={extendDate}
              onChange={(event) => setExtendDate(event.target.value)}
              data-testid="waiver-detail-extend-date"
            />
          </Flex>
          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray">
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              disabled={!extendDate || busy}
              onClick={() => {
                if (extendDate < todayDateInputValue()) {
                  setError('Pick today or a future expiration date');
                  return;
                }
                void run(
                  async () => {
                    await updatePolicyWaiver({
                      ownerType,
                      ownerId,
                      policyWaiverId: waiverId,
                      options: {
                        matcherStrategy,
                        comment: waiver?.comment ?? null,
                        expiryTime: expiryDateToIsoEndOfDay(extendDate),
                        waiverReasonId: committedWaiverReasonId,
                        // Preserve remediation-driven expiry; Extend only changes the date.
                        expireWhenRemediationAvailable:
                          waiver?.expireWhenRemediationAvailable
                          ?? waiver?.isExpireWhenRemediationAvailable
                          ?? false,
                      },
                    });
                    setExtendOpen(false);
                    setExtendDate('');
                  },
                  onChanged,
                );
              }}
              data-testid="waiver-detail-extend-submit"
            >
              Save
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      <Dialog.Root
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
      >
        <Dialog.Content maxWidth="400px" data-testid="waiver-detail-delete-dialog">
          <Dialog.Title>Delete waiver</Dialog.Title>
          <Dialog.Description size="2" mb="3">
            This permanently removes the waiver. Policy violations covered by it will reopen on the
            next evaluation.
          </Dialog.Description>
          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray">
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              color="red"
              disabled={busy}
              onClick={() =>
                void run(
                  async () => {
                    await deletePolicyWaiver({ ownerType, ownerId, policyWaiverId: waiverId });
                    setDeleteOpen(false);
                  },
                  onDeletedOrWithdrawn,
                )
              }
              data-testid="waiver-detail-delete-confirm"
            >
              Delete
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      <Dialog.Root
        open={rejectOpen}
        onOpenChange={(open) => {
          setRejectOpen(open);
          if (!open) setRejectionReason('');
        }}
      >
        <Dialog.Content maxWidth="400px" data-testid="waiver-detail-reject-dialog">
          <Dialog.Title>Reject request</Dialog.Title>
          <Dialog.Description size="2" mb="3">
            Optionally explain why this waiver request is rejected.
          </Dialog.Description>
          <Flex direction="column" gap="1">
            <Text as="label" size="2" weight="medium" htmlFor="waiver-detail-reject-reason">
              Rejection reason
            </Text>
            <TextArea
              id="waiver-detail-reject-reason"
              value={rejectionReason}
              onChange={(event) => setRejectionReason(event.target.value)}
              data-testid="waiver-detail-reject-reason"
            />
          </Flex>
          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray">
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              color="red"
              disabled={busy}
              onClick={() =>
                void run(
                  async () => {
                    await reviewPolicyWaiverRequest({
                      ownerType,
                      ownerId,
                      policyWaiverRequestId: waiverId,
                      review: {
                        status: 'REJECTED',
                        matcherStrategy,
                        rejectionReason: rejectionReason.trim() || null,
                        comment: request?.comment ?? null,
                        expiryTime: request?.expiryTime ?? null,
                        expireWhenRemediationAvailable:
                          request?.expireWhenRemediationAvailable ?? false,
                      },
                    });
                    setRejectOpen(false);
                    setRejectionReason('');
                  },
                  () => {
                    setReviewComplete(true);
                    onChanged();
                  },
                )
              }
              data-testid="waiver-detail-reject-submit"
            >
              Reject
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      <Dialog.Root open={withdrawOpen} onOpenChange={setWithdrawOpen}>
        <Dialog.Content maxWidth="400px" data-testid="waiver-detail-withdraw-dialog">
          <Dialog.Title>Withdraw request</Dialog.Title>
          <Dialog.Description size="2" mb="3">
            This withdraws the pending waiver request. Reviewers will no longer see it.
          </Dialog.Description>
          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <Button variant="soft" color="gray">
                Cancel
              </Button>
            </Dialog.Close>
            <Button
              color="red"
              disabled={busy}
              onClick={() =>
                void run(
                  async () => {
                    await withdrawPolicyWaiverRequest({
                      ownerType,
                      ownerId,
                      policyWaiverRequestId: waiverId,
                    });
                    setWithdrawOpen(false);
                  },
                  () => {
                    setReviewComplete(true);
                    onDeletedOrWithdrawn();
                  },
                )
              }
              data-testid="waiver-detail-withdraw-confirm"
            >
              Withdraw
            </Button>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>
    </Flex>
  );
}
