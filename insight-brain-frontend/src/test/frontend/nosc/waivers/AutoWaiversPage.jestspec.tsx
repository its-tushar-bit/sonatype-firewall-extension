/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { userEvent } from 'TestRoot/SpecUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';
import AutoWaiversPage from 'MainRoot/nosc/waivers/AutoWaiversPage';
import * as autoWaiversApi from 'MainRoot/nosc/waivers/autoWaiversApi';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

jest.mock('MainRoot/nosc/waivers/autoWaiversApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/autoWaiversApi');
  return {
    ...actual,
    fetchApplicableAutoWaivers: jest.fn(),
    deleteAutoPolicyWaiver: jest.fn(),
    updateAutoPolicyWaiver: jest.fn(),
  };
});

const mockedFetch = autoWaiversApi.fetchApplicableAutoWaivers as jest.MockedFunction<
  typeof autoWaiversApi.fetchApplicableAutoWaivers
>;
const mockedDelete = autoWaiversApi.deleteAutoPolicyWaiver as jest.MockedFunction<
  typeof autoWaiversApi.deleteAutoPolicyWaiver
>;
const mockedUpdate = autoWaiversApi.updateAutoPolicyWaiver as jest.MockedFunction<
  typeof autoWaiversApi.updateAutoPolicyWaiver
>;

const FEATURE_STATE = {
  productFeatures: {
    productFeatures: {
      'auto-waivers': true,
      'developer-dashboard': true,
      'auto-waiver-management': true,
    },
  },
};

describe('AutoWaiversPage', () => {
  let addToastSpy: jest.SpyInstance;

  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedFetch.mockReset();
    mockedDelete.mockReset().mockResolvedValue(undefined);
    mockedUpdate.mockReset().mockResolvedValue({
      autoPolicyWaiverId: 'aw-1',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerType: 'organization',
      threatLevel: 5,
      reachability: true,
      pathForward: false,
      scopesOperatorAny: true,
    });
    addToastSpy = jest.spyOn(toastActions, 'addToast');
  });

  afterEach(() => {
    addToastSpy.mockRestore();
  });

  it('loads owner-scoped applicable configs and opens details', async () => {
    mockedFetch.mockResolvedValue([
      {
        autoPolicyWaiverId: 'aw-1',
        autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverOwnerName: 'Root Organization',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: false,
        threatLevel: 7,
        hasNotReachable: true,
        hasNoPathForward: false,
        createTime: '2026-01-01T00:00:00Z',
      },
    ]);

    const { router } = renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    await screen.findByTestId('nosc-auto-waivers-table');
    expect(mockedFetch).toHaveBeenCalledWith({
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    });
    expect(screen.getByText('Root Organization')).toBeInTheDocument();
    expect(screen.getByText(/Not reachable/)).toBeInTheDocument();

    await userEvent.click(screen.getByTestId('nosc-auto-waiver-open-aw-1'));
    expect(goSpy).toHaveBeenCalledWith('nexusOneAutoWaiverDetail', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
      autoPolicyWaiverId: 'aw-1',
    });
  });

  it('requires confirmation before delete', async () => {
    mockedFetch.mockResolvedValue([
      {
        autoPolicyWaiverId: 'aw-2',
        autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: false,
        threatLevel: 3,
      },
    ]);

    renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });

    await screen.findByTestId('nosc-auto-waiver-delete-aw-2');
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-delete-aw-2'));
    expect(mockedDelete).not.toHaveBeenCalled();
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-delete-confirm'));
    await waitFor(() => {
      expect(mockedDelete).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-2',
      });
    });
    expect(addToastSpy).toHaveBeenCalledWith({ type: 'success', message: 'Auto-waiver deleted' });
  });

  it('keeps the delete dialog open and shows an in-dialog error on failure', async () => {
    mockedFetch.mockResolvedValue([
      {
        autoPolicyWaiverId: 'aw-fail',
        autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: false,
        threatLevel: 3,
      },
    ]);
    mockedDelete.mockRejectedValueOnce(new Error('delete blocked'));

    renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });

    await userEvent.click(await screen.findByTestId('nosc-auto-waiver-delete-aw-fail'));
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-delete-confirm'));
    expect(await screen.findByTestId('nosc-auto-waiver-delete-error')).toHaveTextContent(
      'delete blocked',
    );
    expect(screen.getByTestId('nosc-auto-waiver-delete-dialog')).toBeInTheDocument();
    expect(addToastSpy).toHaveBeenCalledWith({ type: 'error', message: 'delete blocked' });
  });

  it('explains why New Auto-Waiver is disabled at the local limit', async () => {
    mockedFetch.mockResolvedValue(
      Array.from({ length: 3 }, (_, index) => ({
        autoPolicyWaiverId: `aw-local-${index}`,
        autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: false,
        threatLevel: 3,
      })),
    );

    renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });

    await screen.findByTestId('nosc-auto-waivers-table');
    await waitFor(() => {
      expect(screen.getByTestId('nosc-auto-waivers-create')).toBeDisabled();
    });
    expect(screen.getByTestId('nosc-auto-waivers-create')).toHaveAttribute(
      'data-disabled-reason',
      'Maximum of 3 local auto-waivers reached',
    );
  });

  it('opens edit modal and updates a local (non-inherited) config', async () => {
    mockedFetch.mockResolvedValue([
      {
        autoPolicyWaiverId: 'aw-local',
        autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: false,
        threatLevel: 7,
        hasNotReachable: true,
        hasNoPathForward: false,
        scopesOperatorAny: true,
      },
    ]);

    renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });

    await userEvent.click(await screen.findByTestId('nosc-auto-waiver-edit-aw-local'));
    expect(await screen.findByTestId('new-auto-waiver-modal')).toBeInTheDocument();
    await userEvent.clear(screen.getByTestId('new-auto-waiver-threat'));
    await userEvent.type(screen.getByTestId('new-auto-waiver-threat'), '5');
    await userEvent.click(screen.getByTestId('new-auto-waiver-submit'));

    await waitFor(() => {
      expect(mockedUpdate).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-local',
        body: expect.objectContaining({ threatLevel: 5 }),
      });
    });
  });

  it('hides Edit and Delete for inherited configs', async () => {
    mockedFetch.mockResolvedValue([
      {
        autoPolicyWaiverId: 'aw-inherited',
        autoPolicyWaiverOwnerId: 'parent-org',
        autoPolicyWaiverOwnerType: 'organization',
        isInherited: true,
        threatLevel: 4,
        hasNotReachable: true,
        hasNoPathForward: false,
      },
    ]);

    renderNexusOneRoute(<AutoWaiversPage />, 'nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    }, {
      preloadedState: FEATURE_STATE,
    });

    await screen.findByTestId('nosc-auto-waivers-table');
    expect(screen.getByText('Inherited')).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-auto-waiver-edit-aw-inherited')).not.toBeInTheDocument();
    expect(screen.queryByTestId('nosc-auto-waiver-delete-aw-inherited')).not.toBeInTheDocument();
    expect(screen.getByTestId('nosc-auto-waiver-open-aw-inherited')).toBeInTheDocument();
  });
});
