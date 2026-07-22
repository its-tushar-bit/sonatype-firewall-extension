/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import { render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import reducer from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import { useViolationDetailData } from 'MainRoot/nosc/violations/detail/useViolationDetailData';
import type { ApplicableWaiversDTO, ViolationDetailsDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

function violation(id: string): ViolationDetailsDTO {
  return {
    policyViolationId: id,
    policyName: 'License Policy',
    policyThreatCategory: 'license',
    policyOwner: {
      ownerName: 'Demo Org',
      ownerType: 'organization',
      ownerId: 'org-1',
    },
    threatLevel: 4,
    openTime: '2026-07-18T10:00:00Z',
    stageData: {
      build: {
        mostRecentEvaluationTime: '2026-07-18T10:00:00Z',
        mostRecentScanId: 'scan-1',
      },
    },
    applicationPublicId: `app-${id}`,
    organizationName: 'Demo Org',
    applicationName: 'Demo App',
  };
}

const emptyWaivers: ApplicableWaiversDTO = {
  activeWaivers: [],
  expiredWaivers: [],
};

function HookConsumer({ violationId }: { readonly violationId?: string }) {
  const detail = useViolationDetailData({ violationId });
  return (
    <div>
      <span data-testid="identity-status">{detail.identityStatus}</span>
      <span data-testid="loaded-id">{detail.identity?.policyViolationId ?? 'none'}</span>
      <span data-testid="permission">{String(detail.hasPermissionForAppWaivers)}</span>
    </div>
  );
}

function renderHookConsumer(violationId?: string) {
  const store = configureStore({
    reducer: {
      violationDetail: reducer,
    },
  });

  const rendered = render(
    <Provider store={store}>
      <HookConsumer violationId={violationId} />
    </Provider>
  );

  return { ...rendered, store };
}

describe('useViolationDetailData', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('loads detail data when the violation id changes', async () => {
    jest.spyOn(axios, 'get').mockImplementation((url: string) => {
      if (url === getViolationDetailsUrl('violation-1')) {
        return Promise.resolve({ data: violation('violation-1') });
      }
      if (url === getViolationDetailsUrl('violation-2')) {
        return Promise.resolve({ data: violation('violation-2') });
      }
      if (url === getApplicableWaiversUrl('violation-1') || url === getApplicableWaiversUrl('violation-2')) {
        return Promise.resolve({ data: emptyWaivers });
      }
      if (url === getApplicationSummaryUrl('app-violation-1')) {
        return Promise.resolve({ data: { id: 'internal-1' } });
      }
      if (url === getApplicationSummaryUrl('app-violation-2')) {
        return Promise.resolve({ data: { id: 'internal-2' } });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    jest.spyOn(axios, 'put').mockImplementation((url: string) => {
      if (url === getPermissionContextTestUrl('application', 'internal-1')) {
        return Promise.resolve({ data: ['WAIVE_POLICY_VIOLATIONS'] });
      }
      if (url === getPermissionContextTestUrl('application', 'internal-2')) {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Unexpected PUT ${url}`));
    });

    const { rerender, store } = renderHookConsumer('violation-1');

    await waitFor(() => expect(screen.getByTestId('loaded-id')).toHaveTextContent('violation-1'));
    expect(screen.getByTestId('identity-status')).toHaveTextContent('ready');
    expect(screen.getByTestId('permission')).toHaveTextContent('true');

    rerender(
      <Provider store={store}>
        <HookConsumer violationId="violation-2" />
      </Provider>
    );

    await waitFor(() => expect(screen.getByTestId('loaded-id')).toHaveTextContent('violation-2'));
    expect(screen.getByTestId('identity-status')).toHaveTextContent('ready');
    expect(screen.getByTestId('permission')).toHaveTextContent('false');
  });

  it('does not load when no violation id is available', () => {
    jest.spyOn(axios, 'get');
    renderHookConsumer(undefined);

    expect(screen.getByTestId('identity-status')).toHaveTextContent('idle');
    expect(screen.getByTestId('loaded-id')).toHaveTextContent('none');
    expect(axios.get).not.toHaveBeenCalled();
  });
});
