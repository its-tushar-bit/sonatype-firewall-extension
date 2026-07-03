/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import ComponentDetailsBackButton from 'MainRoot/componentDetails/ComponentDetailsBackButton';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as RouterSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';

describe('ComponentDetailsBackButton', () => {
  let renderComponent,
    minimalProps,
    routerContextMock,
    selectIsPrioritiesPageContainerSpy,
    selectIsContainerImagesEvaluationEnabledAndProxyStageSpy,
    selectReportStageIdSpy;

  beforeEach(() => {
    renderComponent = (additionalProps, preloadedState) =>
      render(<ComponentDetailsBackButton {...minimalProps} {...additionalProps} />, { preloadedState });

    routerContextMock = {
      href: jest.fn('href').mockImplementation((stateName) => stateName),
      get: jest.fn('get').mockImplementation((stateName) => {
        if (stateName === 'applicationReport.policy') {
          return { data: { title: 'Application Report' } };
        }
        return stateName;
      }),
      includes: jest.fn(() => false),
    };

    jest.spyOn(routerContext, 'useRouterState').mockReturnValue(routerContextMock);

    selectIsPrioritiesPageContainerSpy = jest
      .spyOn(RouterSelectors, 'selectIsPrioritiesPageContainer')
      .mockReturnValue(false);

    selectIsContainerImagesEvaluationEnabledAndProxyStageSpy = jest
      .spyOn(applicationReportSelectors, 'selectIsContainerImagesEvaluationEnabledAndProxyStage')
      .mockReturnValue(false);

    selectReportStageIdSpy = jest.spyOn(applicationReportSelectors, 'selectReportStageId').mockReturnValue('proxy');
  });

  describe('Back to Dependency Tree button', () => {
    it('renders if scanId and publicId props are provided', () => {
      renderComponent({ scanId: 'scanId', publicId: 'testId', fromDependencyTree: true });

      const backBtn = screen.getByRole('link', { name: 'Back To Dependency Tree' });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'applicationReport.dependencyTree');
    });

    it('if navigated from priorities page renders if scanId and publicId props are provided when navigated from Priorities Page -> Dependencies -> Click on Dependency', () => {
      selectIsPrioritiesPageContainerSpy.mockReturnValue(true);
      selectIsContainerImagesEvaluationEnabledAndProxyStageSpy.mockReturnValue(false);

      const routerPreloadedState = {
        router: {
          currentState: {
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree',
          },
          currentParams: {
            publicId: 'testPublicAppId',
            scanId: 'testScanId',
          },
        },
      };
      renderComponent({ scanId: 'scanId', publicId: 'testId', fromDependencyTree: true }, routerPreloadedState);

      const backBtn = screen.getByRole('link', { name: 'Back To Dependency Tree' });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute(
        'href',
        'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.dependencyTree'
      );
    });
  });

  describe('Back To Container Report button', () => {
    it('renders if scanId and publicId props are provided', () => {
      selectReportStageIdSpy.mockReturnValue('proxy');
      selectIsContainerImagesEvaluationEnabledAndProxyStageSpy.mockReturnValue(true);

      renderComponent({ scanId: 'scanId', publicId: 'testId' });
      const backBtn = screen.getByRole('link', { name: 'Back To Container Report' });
      expect(backBtn).toBeInTheDocument();
      expect(backBtn).toHaveAttribute('href', 'firewall.containerReport');
      expect(routerContextMock.href).toHaveBeenCalledWith('firewall.containerReport', {
        scanId: 'scanId',
        publicId: 'testId',
        origin: FIREWALL_CONTAINER_REPOSITORY_RESULTS,
      });
    });
  });

  describe('if rendered within prioritiesPageContainer', () => {
    beforeEach(() => {
      selectIsPrioritiesPageContainerSpy.mockReturnValue(true);
    });

    it('renders "Back to Priorities" if navigated Developer Dashboard -> Priorities Page -> Component Details', () => {
      const routerPreloadedState = {
        router: {
          currentState: {
            name: 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard.componentDetails.overview',
          },
          currentParams: {
            publicId: 'testPublicAppId',
            scanId: 'testScanId',
          },
        },
      };
      renderComponent(null, routerPreloadedState);

      const backBtn = screen.getByRole('link', { name: 'Back to Priorities' });

      expect(backBtn).toBeInTheDocument();
    });
  });

  it('renders "Back to Application Report" by default', () => {
    renderComponent();

    expect(screen.getByRole('link', { name: 'Back to Application Report' })).toBeInTheDocument();
  });

  describe('hosted repo origin', () => {
    it('renders "Back to Repository Component Report" when currentParams.origin=hostedRepoComponents (refresh-safe)', () => {
      const routerPreloadedState = {
        router: {
          currentParams: {
            publicId: 'maven-releases_common_common_1_common-1.jar',
            scanId: 'bc50a9b4678c4442974aff5e68750034',
            origin: 'hostedRepoComponents',
            repositoryManagerId: 'rm-1',
            repositoryId: 'repo-1',
            repositoryPublicId: 'maven-releases',
            componentDisplayName: 'common 1 (.jar)',
          },
        },
      };

      renderComponent(null, routerPreloadedState);

      expect(screen.getByRole('link', { name: 'Back to Repository Component Report' })).toBeInTheDocument();
      // CLM-42090: componentDisplayName MUST be forwarded so the destination report page
      // renders the friendly component name instead of the synthetic application public id.
      expect(routerContextMock.href).toHaveBeenCalledWith('applicationReport.policy', {
        publicId: 'maven-releases_common_common_1_common-1.jar',
        scanId: 'bc50a9b4678c4442974aff5e68750034',
        origin: 'hostedRepoComponents',
        repositoryManagerId: 'rm-1',
        repositoryId: 'repo-1',
        repositoryPublicId: 'maven-releases',
        componentDisplayName: 'common 1 (.jar)',
      });
    });

    it('forwards componentDisplayName from prevParams when the current page did not receive it directly (CLM-42090)', () => {
      const routerPreloadedState = {
        router: {
          currentParams: {
            publicId: 'maven-releases_common_common_1_common-1.jar',
            scanId: 'bc50a9b4678c4442974aff5e68750034',
          },
          prevParams: {
            origin: 'hostedRepoComponents',
            repositoryManagerId: 'rm-1',
            repositoryId: 'repo-1',
            repositoryPublicId: 'maven-releases',
            componentDisplayName: 'common 1 (.jar)',
          },
        },
      };

      renderComponent(null, routerPreloadedState);

      expect(screen.getByRole('link', { name: 'Back to Repository Component Report' })).toBeInTheDocument();
      expect(routerContextMock.href).toHaveBeenCalledWith(
        'applicationReport.policy',
        expect.objectContaining({ componentDisplayName: 'common 1 (.jar)' })
      );
    });
  });
});
