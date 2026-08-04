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
import AutoWaiverDetailPage from 'MainRoot/nosc/waivers/AutoWaiverDetailPage';
import * as autoWaiversApi from 'MainRoot/nosc/waivers/autoWaiversApi';

jest.mock('MainRoot/nosc/waivers/autoWaiversApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/autoWaiversApi');
  return {
    ...actual,
    fetchAutoPolicyWaiver: jest.fn(),
    fetchAutoWaiverExclusions: jest.fn(),
    deleteAutoWaiverExclusion: jest.fn(),
    deleteAutoPolicyWaiver: jest.fn(),
  };
});

const mockedFetchConfig = autoWaiversApi.fetchAutoPolicyWaiver as jest.MockedFunction<
  typeof autoWaiversApi.fetchAutoPolicyWaiver
>;
const mockedFetchExclusions = autoWaiversApi.fetchAutoWaiverExclusions as jest.MockedFunction<
  typeof autoWaiversApi.fetchAutoWaiverExclusions
>;
const mockedDeleteExclusion = autoWaiversApi.deleteAutoWaiverExclusion as jest.MockedFunction<
  typeof autoWaiversApi.deleteAutoWaiverExclusion
>;
const mockedDeleteConfig = autoWaiversApi.deleteAutoPolicyWaiver as jest.MockedFunction<
  typeof autoWaiversApi.deleteAutoPolicyWaiver
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

describe('AutoWaiverDetailPage', () => {
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedFetchConfig.mockReset().mockResolvedValue({
      autoPolicyWaiverId: 'aw-1',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerType: 'organization',
      ownerName: 'Root Organization',
      threatLevel: 7,
      reachability: true,
      pathForward: false,
      scopesOperatorAny: true,
      createTime: '2026-01-01T00:00:00Z',
    });
    mockedFetchExclusions.mockReset().mockResolvedValue([
      {
        autoPolicyWaiverExclusionId: 'ex-1',
        autoPolicyWaiverId: 'aw-1',
        componentDisplayName: 'demo-component',
        policyName: 'Security-High',
        threatLevel: 8,
        createTime: '2026-02-01T00:00:00Z',
      },
    ]);
    mockedDeleteExclusion.mockReset().mockResolvedValue(undefined);
    mockedDeleteConfig.mockReset().mockResolvedValue(undefined);
  });

  it('shows config overview and includes (removes) an exclusion', async () => {
    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );

    await screen.findByTestId('nosc-auto-waiver-detail-overview');
    expect(screen.getByText(/Threat ≤ 7/)).toBeInTheDocument();
    expect(screen.getByText('demo-component')).toBeInTheDocument();

    await userEvent.click(screen.getByTestId('nosc-auto-waiver-include-ex-1'));
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-include-confirm'));

    await waitFor(() => {
      expect(mockedDeleteExclusion).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
        autoPolicyWaiverExclusionId: 'ex-1',
      });
    });
  });

  it('requires confirmation before deleting the auto-waiver config', async () => {
    const { router } = renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    await screen.findByTestId('nosc-auto-waiver-detail-delete');
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-detail-delete'));
    expect(mockedDeleteConfig).not.toHaveBeenCalled();
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-detail-delete-confirm'));

    await waitFor(() => {
      expect(mockedDeleteConfig).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      });
    });
    expect(goSpy).toHaveBeenCalledWith('nexusOneAutoWaivers', {
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    });
  });

  it('keeps the detail delete dialog open with an in-dialog error on failure', async () => {
    mockedDeleteConfig.mockRejectedValueOnce(new Error('delete blocked'));

    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );

    await userEvent.click(await screen.findByTestId('nosc-auto-waiver-detail-delete'));
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-detail-delete-confirm'));
    expect(await screen.findByTestId('nosc-auto-waiver-detail-delete-error')).toHaveTextContent(
      'delete blocked',
    );
    expect(screen.getByTestId('nosc-auto-waiver-detail-delete-dialog')).toBeInTheDocument();
  });

  it('keeps the Include dialog open with an in-dialog error on failure', async () => {
    mockedDeleteExclusion.mockRejectedValueOnce(new Error('include blocked'));

    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );

    await userEvent.click(await screen.findByTestId('nosc-auto-waiver-include-ex-1'));
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-include-confirm'));
    expect(await screen.findByTestId('nosc-auto-waiver-include-error')).toHaveTextContent(
      'include blocked',
    );
    expect(screen.getByTestId('nosc-auto-waiver-include-dialog')).toBeInTheDocument();
  });

  it('shows empty exclusions state and defaults omitted threat to 0', async () => {
    mockedFetchConfig.mockResolvedValue({
      autoPolicyWaiverId: 'aw-1',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerType: 'organization',
      ownerName: 'Root Organization',
      // threatLevel omitted (backend NON_EMPTY for 0)
      reachability: true,
      pathForward: false,
      scopesOperatorAny: true,
    });
    mockedFetchExclusions.mockResolvedValue([]);

    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );

    await screen.findByTestId('nosc-auto-waiver-detail-overview');
    expect(screen.getByText(/Threat ≤ 0/)).toBeInTheDocument();
    expect(screen.getByText('No exclusions for this config.')).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-auto-waiver-exclusions-pagination')).not.toBeInTheDocument();
  });

  it('locks the page when auto-waivers feature is disabled', async () => {
    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      {
        preloadedState: {
          productFeatures: {
            productFeatures: {
              'auto-waiver-management': true,
            },
          },
        },
      },
    );

    expect(await screen.findByTestId('nosc-auto-waiver-detail-locked')).toBeInTheDocument();
    expect(mockedFetchConfig).not.toHaveBeenCalled();
  });

  it('steps back from page N when the last exclusion on that page is included', async () => {
    // API is asked for pageSize+1; 26 rows ⇒ hasNext, UI keeps 25.
    const pageOne = Array.from({ length: 26 }, (_, index) => ({
      autoPolicyWaiverExclusionId: `ex-page1-${index}`,
      autoPolicyWaiverId: 'aw-1',
      componentDisplayName: `component-${index}`,
      policyName: 'Security-High',
      threatLevel: 5,
      createTime: '2026-02-01T00:00:00Z',
    }));
    mockedFetchExclusions
      .mockResolvedValueOnce(pageOne)
      .mockResolvedValueOnce([
        {
          autoPolicyWaiverExclusionId: 'ex-page2-only',
          autoPolicyWaiverId: 'aw-1',
          componentDisplayName: 'last-on-page-2',
          policyName: 'Security-High',
          threatLevel: 6,
          createTime: '2026-03-01T00:00:00Z',
        },
      ])
      .mockResolvedValueOnce(pageOne);

    renderNexusOneRoute(
      <AutoWaiverDetailPage />,
      'nexusOneAutoWaiverDetail',
      {
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        autoPolicyWaiverId: 'aw-1',
      },
      { preloadedState: FEATURE_STATE },
    );

    await screen.findByTestId('nosc-auto-waiver-exclusions-pagination');
    await userEvent.click(screen.getByRole('button', { name: 'Next page' }));

    await screen.findByTestId('nosc-auto-waiver-include-ex-page2-only');
    expect(mockedFetchExclusions).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 2, pageSize: 26 }),
    );

    await userEvent.click(screen.getByTestId('nosc-auto-waiver-include-ex-page2-only'));
    await userEvent.click(screen.getByTestId('nosc-auto-waiver-include-confirm'));

    await waitFor(() => {
      expect(mockedFetchExclusions).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 1, pageSize: 26 }),
      );
    });
    expect(await screen.findByText('component-0')).toBeInTheDocument();
  });
});
