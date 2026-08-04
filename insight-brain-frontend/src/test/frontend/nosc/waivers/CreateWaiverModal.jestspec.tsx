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
import CreateWaiverModal from 'MainRoot/nosc/waivers/CreateWaiverModal';
import * as mutationApi from 'MainRoot/nosc/waivers/waiversMutationApi';

jest.mock('MainRoot/nosc/waivers/waiversMutationApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/waiversMutationApi');
  return {
    ...actual,
    fetchWaiverScopeTargets: jest.fn(),
    fetchPolicyWaiverReasons: jest.fn(),
    createPolicyWaiver: jest.fn(),
  };
});

const mockedScopes = mutationApi.fetchWaiverScopeTargets as jest.MockedFunction<
  typeof mutationApi.fetchWaiverScopeTargets
>;
const mockedReasons = mutationApi.fetchPolicyWaiverReasons as jest.MockedFunction<
  typeof mutationApi.fetchPolicyWaiverReasons
>;
const mockedCreate = mutationApi.createPolicyWaiver as jest.MockedFunction<
  typeof mutationApi.createPolicyWaiver
>;

describe('CreateWaiverModal', () => {
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedScopes.mockReset().mockResolvedValue([
      { ownerType: 'application', ownerId: 'app-1', ownerName: 'App One' },
    ]);
    mockedReasons.mockReset().mockResolvedValue([]);
    mockedCreate.mockReset().mockResolvedValue(undefined);
  });

  it('requires a comment or reason before create', async () => {
    render(
      <Theme>
        <CreateWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onCreated={jest.fn()}
        />
      </Theme>,
    );

    await screen.findByTestId('create-waiver-scope');
    await userEvent.click(screen.getByTestId('create-waiver-submit'));
    expect(await screen.findByTestId('create-waiver-error')).toHaveTextContent(
      'Add a comment or select a reason',
    );
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('surfaces API create failures', async () => {
    mockedCreate.mockRejectedValueOnce({
      response: { data: { message: 'Permission denied' } },
    });
    render(
      <Theme>
        <CreateWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onCreated={jest.fn()}
        />
      </Theme>,
    );

    await screen.findByTestId('create-waiver-scope');
    await userEvent.type(screen.getByTestId('create-waiver-comment'), 'because');
    await userEvent.click(screen.getByTestId('create-waiver-submit'));
    await waitFor(() => {
      expect(screen.getByTestId('create-waiver-error')).toHaveTextContent('Permission denied');
    });
  });

  it('shows empty-scope copy only when load succeeds with no scopes', async () => {
    mockedScopes.mockResolvedValueOnce([]);
    render(
      <Theme>
        <CreateWaiverModal
          open
          onOpenChange={jest.fn()}
          policyViolationId="v-1"
          applicationPublicId="app-1"
          policyId="p-1"
          onCreated={jest.fn()}
        />
      </Theme>,
    );
    expect(await screen.findByTestId('create-waiver-no-scopes')).toBeInTheDocument();
  });
});
