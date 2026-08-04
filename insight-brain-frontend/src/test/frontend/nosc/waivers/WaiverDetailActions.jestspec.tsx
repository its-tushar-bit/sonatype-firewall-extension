/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { userEvent } from 'TestRoot/SpecUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import WaiverDetailActions from 'MainRoot/nosc/waivers/WaiverDetailActions';
import * as mutationApi from 'MainRoot/nosc/waivers/waiversMutationApi';

jest.mock('MainRoot/nosc/waivers/waiversMutationApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/waiversMutationApi');
  return {
    ...actual,
    deletePolicyWaiver: jest.fn(),
    updatePolicyWaiver: jest.fn(),
    reviewPolicyWaiverRequest: jest.fn(),
    withdrawPolicyWaiverRequest: jest.fn(),
  };
});

const mockedDelete = mutationApi.deletePolicyWaiver as jest.MockedFunction<
  typeof mutationApi.deletePolicyWaiver
>;
const mockedUpdate = mutationApi.updatePolicyWaiver as jest.MockedFunction<
  typeof mutationApi.updatePolicyWaiver
>;
const mockedReview = mutationApi.reviewPolicyWaiverRequest as jest.MockedFunction<
  typeof mutationApi.reviewPolicyWaiverRequest
>;
const mockedWithdraw = mutationApi.withdrawPolicyWaiverRequest as jest.MockedFunction<
  typeof mutationApi.withdrawPolicyWaiverRequest
>;

function renderActions(
  props: Partial<React.ComponentProps<typeof WaiverDetailActions>> = {},
) {
  const onChanged = jest.fn();
  const onDeletedOrWithdrawn = jest.fn();
  const result = render(
    <Theme>
      <WaiverDetailActions
        ownerType="application"
        ownerId="app-1"
        waiverId="w-1"
        isRequested={false}
        isAutoWaiver={false}
        waiver={{
          id: 'w-1',
          ownerId: 'app-1',
          ownerType: 'application',
          scope: 'application',
          threatLevel: 8,
          comment: 'ok',
          matcherStrategy: 'EXACT_COMPONENT',
        } as any}
        request={null}
        onChanged={onChanged}
        onDeletedOrWithdrawn={onDeletedOrWithdrawn}
        {...props}
      />
    </Theme>,
  );
  return { ...result, onChanged, onDeletedOrWithdrawn };
}

const pendingRequest = {
  policyWaiverRequestId: 'w-1',
  status: 'REQUESTED',
  comment: 'please',
  matcherStrategy: 'EXACT_COMPONENT',
  expiryTime: null,
  expireWhenRemediationAvailable: true,
  policyWaiverReasonId: 'reason-1',
  canReview: true,
} as any;

describe('WaiverDetailActions', () => {
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedDelete.mockReset().mockResolvedValue(undefined);
    mockedUpdate.mockReset().mockResolvedValue(undefined);
    mockedReview.mockReset().mockResolvedValue(undefined);
    mockedWithdraw.mockReset().mockResolvedValue(undefined);
  });

  it('renders nothing for auto-waivers', () => {
    renderActions({ isAutoWaiver: true });
    expect(screen.queryByTestId('waiver-detail-actions')).not.toBeInTheDocument();
  });

  it('requires confirmation before delete and then notifies parent', async () => {
    const { onDeletedOrWithdrawn } = renderActions();

    await userEvent.click(screen.getByTestId('waiver-detail-delete'));
    expect(mockedDelete).not.toHaveBeenCalled();

    await userEvent.click(screen.getByTestId('waiver-detail-delete-confirm'));
    await waitFor(() => {
      expect(mockedDelete).toHaveBeenCalledWith({
        ownerType: 'application',
        ownerId: 'app-1',
        policyWaiverId: 'w-1',
      });
    });
    expect(onDeletedOrWithdrawn).toHaveBeenCalled();
  });

  it('extends a waiver preserving reason and remediation expiry', async () => {
    const { onChanged } = renderActions({
      waiver: {
        id: 'w-1',
        ownerId: 'app-1',
        ownerType: 'application',
        scope: 'application',
        threatLevel: 8,
        comment: 'ok',
        matcherStrategy: 'EXACT_COMPONENT',
        expireWhenRemediationAvailable: true,
        policyWaiverReasonId: 'reason-keep',
        expiryTime: '2030-06-01T12:00:00Z',
      } as any,
    });

    await userEvent.click(screen.getByTestId('waiver-detail-extend'));
    const dateInput = screen.getByTestId('waiver-detail-extend-date');
    expect(dateInput).toHaveAttribute('min');
    expect((dateInput as HTMLInputElement).value).toBe('2030-06-01');
    await userEvent.clear(dateInput);
    await userEvent.type(dateInput, '2030-01-15');
    await userEvent.click(screen.getByTestId('waiver-detail-extend-submit'));

    await waitFor(() => {
      expect(mockedUpdate).toHaveBeenCalledWith(
        expect.objectContaining({
          ownerType: 'application',
          ownerId: 'app-1',
          policyWaiverId: 'w-1',
          options: expect.objectContaining({
            expiryTime: expect.stringMatching(/^2030-01-15T23:59:59/),
            expireWhenRemediationAvailable: true,
            waiverReasonId: 'reason-keep',
          }),
        }),
      );
    });
    expect(onChanged).toHaveBeenCalled();
  });

  it('approves a pending request with the requester reason id', async () => {
    const { onChanged } = renderActions({
      isRequested: true,
      waiver: null,
      request: pendingRequest,
    });

    await userEvent.click(screen.getByTestId('waiver-detail-approve'));
    await waitFor(() => expect(mockedReview).toHaveBeenCalled());
    expect(mockedReview.mock.calls[0][0].review.status).toBe('APPROVED');
    expect(mockedReview.mock.calls[0][0].review.expireWhenRemediationAvailable).toBe(true);
    expect(mockedReview.mock.calls[0][0].review.waiverReasonId).toBe('reason-1');
    expect(onChanged).toHaveBeenCalled();
    expect(screen.queryByTestId('waiver-detail-approve')).not.toBeInTheDocument();
  });

  it('rejects a pending request', async () => {
    const { onChanged } = renderActions({
      isRequested: true,
      waiver: null,
      request: pendingRequest,
    });

    await userEvent.click(screen.getByTestId('waiver-detail-reject'));
    await userEvent.click(screen.getByTestId('waiver-detail-reject-submit'));
    await waitFor(() => expect(mockedReview).toHaveBeenCalled());
    expect(mockedReview.mock.calls[0][0].review.status).toBe('REJECTED');
    expect(onChanged).toHaveBeenCalled();
  });

  it('hides Approve/Reject when canReview is absent (fail closed)', () => {
    renderActions({
      isRequested: true,
      waiver: null,
      request: { ...pendingRequest, canReview: undefined },
    });
    expect(screen.queryByTestId('waiver-detail-approve')).not.toBeInTheDocument();
    expect(screen.queryByTestId('waiver-detail-reject')).not.toBeInTheDocument();
    expect(screen.getByTestId('waiver-detail-withdraw')).toBeInTheDocument();
  });

  it('withdraws a pending request when canReview is false', async () => {
    const { onDeletedOrWithdrawn } = renderActions({
      isRequested: true,
      waiver: null,
      request: { ...pendingRequest, canReview: false },
    });

    expect(screen.queryByTestId('waiver-detail-approve')).not.toBeInTheDocument();
    await userEvent.click(screen.getByTestId('waiver-detail-withdraw'));
    await userEvent.click(screen.getByTestId('waiver-detail-withdraw-confirm'));
    await waitFor(() => {
      expect(mockedWithdraw).toHaveBeenCalledWith({
        ownerType: 'application',
        ownerId: 'app-1',
        policyWaiverRequestId: 'w-1',
      });
    });
    expect(onDeletedOrWithdrawn).toHaveBeenCalled();
  });

  it('surfaces API body message from axios-shaped errors', async () => {
    mockedDelete.mockRejectedValueOnce({
      message: 'Request failed with status code 400',
      response: { data: { message: 'This policy waiver already exists.' } },
    });
    renderActions();

    await userEvent.click(screen.getByTestId('waiver-detail-delete'));
    await userEvent.click(screen.getByTestId('waiver-detail-delete-confirm'));

    expect(await screen.findByTestId('waiver-detail-action-error')).toHaveTextContent(
      'This policy waiver already exists.',
    );
  });
});
