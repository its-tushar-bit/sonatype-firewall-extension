/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/continuousMonitoringEditor/ContinuousMonitoringEditor';
import * as policyMonitoringSelectors from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';

import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as stagesSelectors from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('Continuous Monitoring Component', () => {
  let renderComponent,
    selectMonitoringLoadingSpy,
    selectMonitoringLoadErrorSpy,
    selectIsMonitoringSupportedSpy,
    savePolicyMonitoringSpy,
    setMonitoredStageSpy,
    removePolicyMonitoringSpy,
    loadApplicablePolicyMonitoringSpy,
    selectSelectedMonitoredStageSpy;

  beforeEach(() => {
    jest.spyOn(stagesSelectors, 'selectCliStagesWithInheritOrNoMonitorOption').mockReturnValue([
      {
        stageName: 'Do not monitor',
      },
      {
        stageTypeId: 'develop',
        stageName: 'Develop',
      },
      {
        stageTypeId: 'source',
        stageName: 'Source',
      },
      {
        stageTypeId: 'build',
        stageName: 'Build',
      },
      {
        stageTypeId: 'stage-release',
        stageName: 'Stage Release',
      },
      {
        stageTypeId: 'release',
        stageName: 'Release',
      },
      {
        stageTypeId: 'operate',
        stageName: 'Operate',
      },
    ]);
    selectMonitoringLoadingSpy = jest
      .spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoading')
      .mockReturnValue(false);
    selectSelectedMonitoredStageSpy = jest
      .spyOn(policyMonitoringSelectors, 'selectSelectedMonitoredStage')
      .mockReturnValue({
        stageName: 'Stage Release',
        stageTypeId: 'stage-release',
      });
    selectMonitoringLoadErrorSpy = jest
      .spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoadError')
      .mockReturnValue(null);
    selectIsMonitoringSupportedSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsMonitoringSupported')
      .mockReturnValue(true);
    savePolicyMonitoringSpy = jest.spyOn(actions, 'savePolicyMonitoring');
    setMonitoredStageSpy = jest.spyOn(actions, 'setMonitoredStage');
    removePolicyMonitoringSpy = jest.spyOn(actions, 'removePolicyMonitoring');
    loadApplicablePolicyMonitoringSpy = jest.spyOn(actions, 'loadApplicablePolicyMonitoring').mockReturnValue({
      type: 'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
      payload: {
        policyMonitoringByOwner: [
          {
            ownerName: 'Root Organization',
            policyMonitorings: [
              {
                id: '68d05f2bcbed42cb91b629a4dfa160a6',
                ownerId: 'ROOT_ORGANIZATION_ID',
                stageTypeId: 'operate',
              },
              {
                id: '68d05f2bcbed42cb91b629a4dfa160a7',
                ownerId: 'ROOT_ORGANIZATION_ID',
                stageTypeId: 'compliance',
              },
            ],
          },
        ],
      },
    });

    renderComponent = () => render(<ContinuousMonitoringEditor />);
  });

  it('renders tile with the correct page title', () => {
    renderComponent();
    expect(screen.getByText('Continuous Monitoring')).toBeVisible();
  });

  it('renders loading indicator', () => {
    selectIsMonitoringSupportedSpy.mockReturnValue(true);
    selectMonitoringLoadingSpy.mockReturnValue(true);
    selectMonitoringLoadErrorSpy.mockReturnValue(null);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error message', () => {
    selectIsMonitoringSupportedSpy.mockReturnValue(true);
    selectMonitoringLoadingSpy.mockReturnValue(null);
    selectMonitoringLoadErrorSpy.mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('An error occurred loading data.')).toBeVisible();
  });

  it('Update button is active', () => {
    renderComponent();
    expect(loadApplicablePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[1];
    fireEvent.click(radio);
    const updateButton = screen.getByRole('button');
    expect(updateButton).not.toHaveClass('disabled');
  });

  it('select different stages correctly', () => {
    renderComponent();
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[0];
    fireEvent.click(radio);
    expect(setMonitoredStageSpy).toHaveBeenCalledTimes(1);
  });

  it('save stage of monitoring can trigger', () => {
    renderComponent();
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[1];
    fireEvent.click(radio);
    expect(setMonitoredStageSpy).toHaveBeenCalledTimes(1);
    const updateButton = screen.getByRole('button');
    fireEvent.click(updateButton);
    expect(updateButton).not.toHaveClass('disabled');
    expect(savePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
  });

  it('remove stage of monitoring can trigger', () => {
    renderComponent();
    selectSelectedMonitoredStageSpy.mockReturnValue({
      stageName: 'Do not monitor',
    });
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[0];
    fireEvent.click(radio);
    expect(setMonitoredStageSpy).toHaveBeenCalledTimes(1);
    const updateButton = screen.getByRole('button');
    fireEvent.click(updateButton);
    expect(updateButton).not.toHaveClass('disabled');
    expect(removePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
  });

  it('shows error message on error', () => {
    selectMonitoringLoadErrorSpy.mockReturnValue('Error');
    renderComponent();
    const error = screen.getByRole('alert');
    expect(error).toBeVisible();
  });
});
