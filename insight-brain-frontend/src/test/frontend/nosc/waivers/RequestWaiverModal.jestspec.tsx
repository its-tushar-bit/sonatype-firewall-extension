/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { render, userEvent } from 'TestRoot/SpecUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import RequestWaiverModal from 'MainRoot/nosc/waivers/RequestWaiverModal';
import * as mutationApi from 'MainRoot/nosc/waivers/waiversMutationApi';

jest.mock('MainRoot/nosc/waivers/waiversMutationApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/waiversMutationApi');
  return {
    ...actual,
    fetchWaiverScopeTargets: jest.fn(),
    fetchPolicyWaiverReasons: jest.fn(),
    createPolicyWaiverRequest: jest.fn(),
  };
});

const mockedScopes = mutationApi.fetchWaiverScopeTargets as jest.MockedFunction<
  typeof mutationApi.fetchWaiverScopeTargets
>;
const mockedReasons = mutationApi.fetchPolicyWaiverReasons as jest.MockedFunction<
  typeof mutationApi.fetchPolicyWaiverReasons
>;
const mockedCreate = mutationApi.createPolicyWaiverRequest as jest.MockedFunction<
  typeof mutationApi.createPolicyWaiverRequest
>;

describe('RequestWaiverModal', () => {
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedScopes.mockReset().mockResolvedValue([
      { ownerType: 'application', ownerId: 'app-1', ownerName: 'App One' },
    ]);
    mockedReasons.mockReset().mockResolvedValue([]);
    mockedCreate.mockReset().mockResolvedValue({
      policyWaiverRequestId: 'req-1',
      status: 'REQUESTED',
    });
  });

  it('requires a comment or reason before submit', async () => {
    render(
      <Theme>
        <RequestWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onRequested={jest.fn()}
        />
      </Theme>,
    );

    await screen.findByTestId('request-waiver-scope');
    await userEvent.click(screen.getByTestId('request-waiver-submit'));
    expect(await screen.findByTestId('request-waiver-error')).toHaveTextContent(
      'Add a comment or select a reason',
    );
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('surfaces API request failures', async () => {
    mockedCreate.mockRejectedValueOnce(new Error('Permission denied'));
    render(
      <Theme>
        <RequestWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onRequested={jest.fn()}
        />
      </Theme>,
    );

    await screen.findByTestId('request-waiver-scope');
    await userEvent.type(screen.getByTestId('request-waiver-comment'), 'because');
    await userEvent.click(screen.getByTestId('request-waiver-submit'));
    await waitFor(() => {
      expect(screen.getByTestId('request-waiver-error')).toHaveTextContent('Permission denied');
    });
  });

  it('calls onRequested with requestId falling back to id', async () => {
    const onRequested = jest.fn();
    mockedCreate.mockResolvedValueOnce({
      id: 'legacy-req-id',
      status: 'REQUESTED',
    } as mutationApi.PolicyWaiverRequestDTO);
    render(
      <Theme>
        <RequestWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onRequested={onRequested}
        />
      </Theme>,
    );

    await screen.findByTestId('request-waiver-scope');
    await userEvent.type(screen.getByTestId('request-waiver-comment'), 'please');
    await userEvent.click(screen.getByTestId('request-waiver-submit'));
    await waitFor(() => {
      expect(onRequested).toHaveBeenCalledWith({
        requestId: 'legacy-req-id',
        ownerType: 'application',
        ownerId: 'app-1',
      });
    });
  });

  it('shows empty-scope copy only when load succeeds with no scopes', async () => {
    mockedScopes.mockResolvedValueOnce([]);
    render(
      <Theme>
        <RequestWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onRequested={jest.fn()}
        />
      </Theme>,
    );
    expect(await screen.findByTestId('request-waiver-no-scopes')).toBeInTheDocument();
  });
});
