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
import ExcludeAutoWaiverButton from 'MainRoot/nosc/waivers/ExcludeAutoWaiverButton';
import * as autoWaiversApi from 'MainRoot/nosc/waivers/autoWaiversApi';

jest.mock('MainRoot/nosc/waivers/autoWaiversApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/autoWaiversApi');
  return {
    ...actual,
    fetchApplicableAutoWaiverForViolation: jest.fn(),
    createAutoWaiverExclusion: jest.fn(),
  };
});

const mockedFetchApplicable = autoWaiversApi.fetchApplicableAutoWaiverForViolation as jest.MockedFunction<
  typeof autoWaiversApi.fetchApplicableAutoWaiverForViolation
>;
const mockedCreateExclusion = autoWaiversApi.createAutoWaiverExclusion as jest.MockedFunction<
  typeof autoWaiversApi.createAutoWaiverExclusion
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

describe('ExcludeAutoWaiverButton', () => {
  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedFetchApplicable.mockReset().mockResolvedValue({
      autoPolicyWaiverId: 'aw-1',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerType: 'organization',
      threatLevel: 7,
    });
    mockedCreateExclusion.mockReset();
  });

  it('excludes a waived violation and calls onExcluded', async () => {
    const onExcluded = jest.fn();
    mockedCreateExclusion.mockResolvedValueOnce({
      autoPolicyWaiverExclusionId: 'ex-1',
      autoPolicyWaiverId: 'aw-1',
    });

    renderNexusOneRoute(
      <ExcludeAutoWaiverButton
        policyViolationId="viol-1"
        applicationPublicId="app-1"
        scanId="scan-1"
        isWaived
        onExcluded={onExcluded}
      />,
      'nexusOneWaivers',
      {},
      { preloadedState: FEATURE_STATE },
    );

    await userEvent.click(await screen.findByTestId('nosc-violation-detail-exclude-auto-waiver'));
    await userEvent.click(screen.getByTestId('nosc-exclude-auto-waiver-confirm'));

    await waitFor(() => {
      expect(mockedCreateExclusion).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        body: expect.objectContaining({
          applicationPublicId: 'app-1',
          scanId: 'scan-1',
          policyViolationId: 'viol-1',
          autoPolicyWaiverId: 'aw-1',
          matchStrategy: 'POLICY_VIOLATION',
        }),
      });
    });
    expect(onExcluded).toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.queryByTestId('nosc-exclude-auto-waiver-dialog')).not.toBeInTheDocument();
    });
  });

  it('clears a stale exclusion error when the dialog is reopened', async () => {
    mockedCreateExclusion.mockRejectedValueOnce(new Error('exclude failed'));

    renderNexusOneRoute(
      <ExcludeAutoWaiverButton
        policyViolationId="viol-1"
        applicationPublicId="app-1"
        scanId="scan-1"
        isWaived
        onExcluded={jest.fn()}
      />,
      'nexusOneWaivers',
      {},
      { preloadedState: FEATURE_STATE },
    );

    await screen.findByTestId('nosc-violation-detail-exclude-auto-waiver');
    await userEvent.click(screen.getByTestId('nosc-violation-detail-exclude-auto-waiver'));
    await userEvent.click(screen.getByTestId('nosc-exclude-auto-waiver-confirm'));
    expect(await screen.findByTestId('nosc-exclude-auto-waiver-error')).toHaveTextContent(
      'exclude failed',
    );

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => {
      expect(screen.queryByTestId('nosc-exclude-auto-waiver-dialog')).not.toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('nosc-violation-detail-exclude-auto-waiver'));
    expect(await screen.findByTestId('nosc-exclude-auto-waiver-dialog')).toBeInTheDocument();
    expect(screen.queryByTestId('nosc-exclude-auto-waiver-error')).not.toBeInTheDocument();
  });
});
