/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { createStore } from 'redux';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';

// Mock RouterStateContext BEFORE importing the component under test — the real module
// eagerly constructs a UIRouterReact instance at module load which we don't need here.
jest.mock('MainRoot/react/RouterStateContext', () => ({
  useRouterState: jest.fn(),
}));

jest.mock('MainRoot/hostedRepositoryComponentReport/hostedRepositoryComponentReportNexusOneShell', () => ({
  wrapHostedRepositoryComponentReportRoot: (node) => node,
}));

jest.mock('@uirouter/react', () => ({
  UIView: () => null,
}));

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import HostedRepositoryComponentReportRoot from 'MainRoot/hostedRepositoryComponentReport/HostedRepositoryComponentReportRoot';

const makeStore = () => createStore(() => ({}));

const renderWithParams = (params) => {
  useRouterState.mockReturnValue({ params });
  return render(
    <Provider store={makeStore()}>
      <HostedRepositoryComponentReportRoot />
    </Provider>
  );
};

describe('HostedRepositoryComponentReportRoot', () => {
  let setReportParametersSpy;
  let loadReportIfNeededSpy;

  beforeEach(() => {
    setReportParametersSpy = jest
      .spyOn(applicationReportActions, 'setReportParameters')
      .mockReturnValue({ type: 'SET_REPORT_PARAMETERS' });
    loadReportIfNeededSpy = jest
      .spyOn(applicationReportActions, 'loadReportIfNeeded')
      .mockReturnValue({ type: 'LOAD_REPORT_IF_NEEDED' });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('dispatches setReportParameters with isApplication=false at position 9 for HRC route', () => {
    renderWithParams({ hrcId: 'hrc-123', scanId: 'scan-456' });

    expect(setReportParametersSpy).toHaveBeenCalledTimes(1);
    const args = setReportParametersSpy.mock.calls[0];
    // Positional signature guard — arg 9 must be `false` (isApplication) so the reducer
    // stores ownerType='HOSTED_REPOSITORY_COMPONENT'. Regression guard for the review
    // comment about the arg being at the wrong position.
    expect(args[0]).toBe('hrc-123'); // ownerId
    expect(args[1]).toBe('scan-456'); // scanId
    expect(args[2]).toBe(false); // isUnknownJs
    // args[3..7] pass through embeddable/policyViolationId/componentHash/tabId
    expect(args[8]).toBe(false); // isApplication
  });

  it('dispatches loadReportIfNeeded after setReportParameters', () => {
    renderWithParams({ hrcId: 'hrc-123', scanId: 'scan-456' });

    expect(setReportParametersSpy).toHaveBeenCalled();
    expect(loadReportIfNeededSpy).toHaveBeenCalledTimes(1);
    // Order matters: params must be set before the load fires so the load reads HRC state.
    const setOrder = setReportParametersSpy.mock.invocationCallOrder[0];
    const loadOrder = loadReportIfNeededSpy.mock.invocationCallOrder[0];
    expect(setOrder).toBeLessThan(loadOrder);
  });

  it('does not dispatch when hrcId is missing', () => {
    renderWithParams({ scanId: 'scan-456' });

    expect(setReportParametersSpy).not.toHaveBeenCalled();
    expect(loadReportIfNeededSpy).not.toHaveBeenCalled();
  });

  it('does not dispatch when scanId is missing', () => {
    renderWithParams({ hrcId: 'hrc-123' });

    expect(setReportParametersSpy).not.toHaveBeenCalled();
    expect(loadReportIfNeededSpy).not.toHaveBeenCalled();
  });
});
