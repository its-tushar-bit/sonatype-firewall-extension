/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ContinuousMonitoringEditor from 'MainRoot/OrgsAndPolicies/сontinuousMonitoringEditor/ContinuousMonitoringEditor';
import * as policyMonitoringSelectors from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';

import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as stagesSelectors from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';

describe('Continuous Monitoring Component', () => {
  let renderComponent,
    selectMonitoringLoadingSpy,
    selectMonitoringLoadErrorSpy,
    selectIsMonitoringSupportedSpy,
    savePolicyMonitoringSpy,
    setMonitoredStageSpy,
    removePolicyMonitoringSpy,
    saveMaskTimerDoneSpy,
    loadApplicablePolicyMonitoringSpy,
    selectSelectedMonitoredStageSpy;

  beforeEach(() => {
    spyOn(stagesSelectors, 'selectCliStagesWithInheritOrNoMonitorOption').and.returnValue([
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
    selectMonitoringLoadingSpy = spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoading').and.returnValue(
      false
    );
    selectSelectedMonitoredStageSpy = spyOn(policyMonitoringSelectors, 'selectSelectedMonitoredStage').and.returnValue({
      stageName: 'Stage Release',
      stageTypeId: 'stage-release',
    });
    selectMonitoringLoadErrorSpy = spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoadError').and.returnValue(
      null
    );
    selectIsMonitoringSupportedSpy = spyOn(productFeaturesSelectors, 'selectIsMonitoringSupported').and.returnValue(
      true
    );
    savePolicyMonitoringSpy = spyOn(actions, 'savePolicyMonitoring').and.callThrough();
    setMonitoredStageSpy = spyOn(actions, 'setMonitoredStage').and.callThrough();
    removePolicyMonitoringSpy = spyOn(actions, 'removePolicyMonitoring').and.callThrough();
    saveMaskTimerDoneSpy = spyOn(actions, 'saveMaskTimerDone').and.callThrough();
    loadApplicablePolicyMonitoringSpy = spyOn(actions, 'loadApplicablePolicyMonitoring').and.returnValue({
      type: 'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
      payload: {
        policyMonitoringByOwner: [
          {
            ownerName: 'Root Organization',
            policyMonitoring: {
              id: '68d05f2bcbed42cb91b629a4dfa160a6',
              ownerId: 'ROOT_ORGANIZATION_ID',
              stageTypeId: 'operate',
            },
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
    selectIsMonitoringSupportedSpy.and.returnValue(true);
    selectMonitoringLoadingSpy.and.returnValue(true);
    selectMonitoringLoadErrorSpy.and.returnValue(null);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error message', () => {
    selectIsMonitoringSupportedSpy.and.returnValue(true);
    selectMonitoringLoadingSpy.and.returnValue(null);
    selectMonitoringLoadErrorSpy.and.returnValue(true);
    renderComponent();
    expect(screen.getByText('An error occurred loading data.')).toBeVisible();
  });

  it('initial disabled Update button', () => {
    renderComponent();
    const updateButton = screen.getByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClassName('disabled');
    fireEvent.click(updateButton);
    expect(savePolicyMonitoringSpy).not.toHaveBeenCalled();
    expect(removePolicyMonitoringSpy).not.toHaveBeenCalled();
    expect(saveMaskTimerDoneSpy).not.toHaveBeenCalled();
  });

  it('Update button is active', () => {
    renderComponent();
    expect(loadApplicablePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[1];
    fireEvent.click(radio);
    const updateButton = screen.getByRole('button');
    expect(updateButton).not.toHaveClassName('disabled');
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
    expect(updateButton).not.toHaveClassName('disabled');
    expect(savePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
  });

  it('remove stage of monitoring can trigger', () => {
    renderComponent();
    selectSelectedMonitoredStageSpy.and.returnValue({
      stageName: 'Do not monitor',
    });
    const radio = screen.getAllByRole('radio', { Name: 'monitor' })[0];
    fireEvent.click(radio);
    expect(setMonitoredStageSpy).toHaveBeenCalledTimes(1);
    const updateButton = screen.getByRole('button');
    fireEvent.click(updateButton);
    expect(updateButton).not.toHaveClassName('disabled');
    expect(removePolicyMonitoringSpy).toHaveBeenCalledTimes(1);
  });

  it('shows error message on error', () => {
    selectMonitoringLoadErrorSpy.and.returnValue('Error');
    renderComponent();
    const error = screen.getByRole('alert');
    expect(error).toBeVisible();
  });
});
