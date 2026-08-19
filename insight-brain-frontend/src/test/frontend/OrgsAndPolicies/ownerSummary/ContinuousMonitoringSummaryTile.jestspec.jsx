/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';

import ContinuousMonitoringSummaryTile from 'MainRoot/OrgsAndPolicies/ownerSummary/ContinuousMonitoringSummaryTile';
import * as productFeatureSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as stageSelectors from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as policyMonitoringSelectors from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';
import router from 'MainRoot/router/routerInstance';

describe('ContinuousMonitoringSummaryTile', () => {
  let renderComponent,
    selectIsLoadingSpy,
    selectMonitoredStageFromActionStagesSpy,
    selectMonitoredStageFromSbomStagesSpy;

  beforeEach(() => {
    jest.spyOn(stageSelectors, 'selectActionStagesIsLoading').mockReturnValue(false);
    jest.spyOn(stageSelectors, 'selectActionStagesLoadError').mockReturnValue('');
    jest.spyOn(stageSelectors, 'selectSbomStagesIsLoading').mockReturnValue(false);
    jest.spyOn(stageSelectors, 'selectSbomStagesLoadError').mockReturnValue('');
    jest.spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLinkParams').mockReturnValue({
      to: 'management.edit.application.monitor-policy',
      params: {
        applicationPublicId: 'multiModule',
      },
    });
    selectMonitoredStageFromActionStagesSpy = jest
      .spyOn(policyMonitoringSelectors, 'selectMonitoredStageFromActionStages')
      .mockReturnValue('');
    selectMonitoredStageFromSbomStagesSpy = jest
      .spyOn(policyMonitoringSelectors, 'selectMonitoredStageFromSbomStages')
      .mockReturnValue('');
    selectIsLoadingSpy = jest.spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoading').mockReturnValue(false);
    jest.spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoadError').mockReturnValue('');

    jest.spyOn(router.stateService, 'href').mockReturnValue('#/management/edit/application/multiModule/monitoring');
    jest.spyOn(router.stateService, 'get').mockReturnValue('mockGetValue');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);

    renderComponent = () => render(<ContinuousMonitoringSummaryTile />);
  });

  describe('Continuous Monitoring Feature is enabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeatureSelectors, 'selectIsMonitoringSupported').mockReturnValue(true);
    });

    it('renders a correct title', () => {
      renderComponent();

      let headerTitle = screen.getByRole('heading', { level: 2 });
      expect(headerTitle).toBeVisible();
      expect(headerTitle).toHaveTextContent('Continuous monitoring');
    });

    it('renders loading while fetching information', () => {
      selectIsLoadingSpy.mockReturnValue(true);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders Develop stage', () => {
      selectMonitoredStageFromActionStagesSpy.mockReturnValue({ stageName: 'Develop' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Develop');
    });

    it('renders Stage Release', () => {
      selectMonitoredStageFromActionStagesSpy.mockReturnValue({ stageName: 'Stage Release' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Stage Release');
    });

    it('renders Stage text for SBOM Manager when Compliance enabled', () => {
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
      selectMonitoredStageFromSbomStagesSpy.mockReturnValue({ stageName: 'Compliance', stageTypeId: 'compliance' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Notifications and Alerts are enabled for Compliance stage');
    });

    it('renders Stage text for SBOM Manager when Compliance disabled', () => {
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
      selectMonitoredStageFromSbomStagesSpy.mockReturnValue(undefined);
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Notifications and Alerts are disabled for Compliance stage');
    });

    it('text is a link to edit continuous monitoring page', () => {
      selectMonitoredStageFromActionStagesSpy.mockReturnValue({ stageName: 'Stage Release' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Stage Release');

      const linkToEdit = screen.getByText('Stage Release').closest('a');
      expect(linkToEdit).toHaveAttribute('href', '#/management/edit/application/multiModule/monitoring');
    });

    it('text is a link to edit continuous monitoring page for SBOM Manager when Compliance enabled', () => {
      jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
      selectMonitoredStageFromSbomStagesSpy.mockReturnValue({ stageName: 'Compliance' });
      renderComponent(true);
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Notifications and Alerts are enabled for Compliance stage');

      const linkToEdit = screen.getByText('Notifications and Alerts are enabled for Compliance stage').closest('a');
      expect(linkToEdit).toHaveAttribute('href', '#/management/edit/application/multiModule/monitoring');
    });
  });

  describe('Continuous Monitoring Feature is Disabled', () => {
    beforeEach(() => {
      jest.spyOn(productFeatureSelectors, 'selectIsMonitoringSupported').mockReturnValue(false);
    });

    it('does not render with a Firewall only license', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Continuous monitoring');
      expect(title).toBeNull();
    });
  });
});
