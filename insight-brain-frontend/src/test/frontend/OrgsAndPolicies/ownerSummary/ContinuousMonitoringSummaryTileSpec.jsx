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
import * as policyMonitoringSelectors from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('ContinuousMonitoringSummaryTile', () => {
  let renderComponent, selectIsLoadingSpy, selectMonitoredStageFromActionStagesSpy, routerContextMock;

  beforeEach(() => {
    spyOn(stageSelectors, 'selectActionStagesIsLoading').and.returnValue(false);
    spyOn(stageSelectors, 'selectActionStagesLoadError').and.returnValue('');
    spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLinkParams').and.returnValue({
      to: 'management.edit.application.monitor-policy',
      params: {
        applicationPublicId: 'multiModule',
      },
    });
    selectMonitoredStageFromActionStagesSpy = spyOn(
      policyMonitoringSelectors,
      'selectMonitoredStageFromActionStages'
    ).and.returnValue('');
    selectIsLoadingSpy = spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoading').and.returnValue(false);
    spyOn(policyMonitoringSelectors, 'selectPolicyMonitoringLoadError').and.returnValue('');

    routerContextMock = {
      href: jasmine.createSpy('href').and.returnValue('#/management/edit/application/multiModule/monitoring'),
      get: jasmine.createSpy('get').and.returnValue('mockGetValue'),
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    renderComponent = () => render(<ContinuousMonitoringSummaryTile />);
  });

  describe('Continuous Monitoring Feature is enabled', () => {
    beforeEach(() => {
      spyOn(productFeatureSelectors, 'selectIsMonitoringSupported').and.returnValue(true);
    });

    it('renders a correct title', () => {
      renderComponent();

      let headerTitle = screen.getByRole('heading', { level: 2 });
      expect(headerTitle).toBeVisible();
      expect(headerTitle).toHaveTextContent('Continuous monitoring');
    });

    it('renders loading while fetching information', () => {
      selectIsLoadingSpy.and.returnValue(true);
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders Develop stage', () => {
      selectMonitoredStageFromActionStagesSpy.and.returnValue({ stageName: 'Develop' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Develop');
    });

    it('renders Stage Release', () => {
      selectMonitoredStageFromActionStagesSpy.and.returnValue({ stageName: 'Stage Release' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Stage Release');
    });

    it('text is a link to edit continuous monitoring page', () => {
      selectMonitoredStageFromActionStagesSpy.and.returnValue({ stageName: 'Stage Release' });
      renderComponent();
      const monitoringStage = screen.getByRole('listitem');
      expect(monitoringStage).toBeVisible();
      expect(monitoringStage).toHaveTextContent('Stage Release');

      const linkToEdit = screen.getByText('Stage Release').closest('a');
      expect(linkToEdit).toHaveAttribute('href', '#/management/edit/application/multiModule/monitoring');
    });
  });

  describe('Continuous Monitoring Feature is Disabled', () => {
    beforeEach(() => {
      spyOn(productFeatureSelectors, 'selectIsMonitoringSupported').and.returnValue(false);
    });

    it('does not render with a Firewall only license', async () => {
      renderComponent();

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Continuous monitoring');
      expect(title).toBeNull();
    });
  });
});
