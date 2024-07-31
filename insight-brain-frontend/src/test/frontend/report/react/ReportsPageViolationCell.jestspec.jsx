/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import ReportsPageViolationCell from 'MainRoot/report/react/ReportsPageViolationCell';

describe('ReportsPageViolationCell', () => {
  let renderComponent, hrefUiRouterStateSpy, props;

  beforeEach(() => {
    hrefUiRouterStateSpy = jest.fn('hrefUiRouterState').mockImplementation((state) => state);
    props = {
      stage: 'build',
      app: {
        id: '137a638c1a474963b464a81e50be9fe4',
        publicId: 'CDPAPPGO',
        name: 'Test app',
        organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
        organizationName: 'CDP Go',
        policyEvaluations: {
          build: {
            id: '1c09f8e97e9247cfb37778462efbc205',
            applicationId: '137a638c1a474963b464a81e50be9fe4',
            stageTypeId: 'build',
            scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
            time: 1636403882656,
            commitHash: null,
            initiator: 'admin',
            scanTriggerType: 'WEB_UI',
            forObsoleteScan: false,
            reevaluation: false,
            forMonitoring: false,
          },
        },
        policyEvaluationsResults: {
          build: {
            alerts: [],
            affectedComponentCount: 664,
            criticalComponentCount: 649,
            severeComponentCount: 13,
            moderateComponentCount: 2,
            criticalPolicyViolationCount: 709,
            severePolicyViolationCount: 152,
            moderatePolicyViolationCount: 7,
            legacyViolationCount: 0,
            totalComponentCount: 1157,
          },
        },
        contact: {
          internalName: null,
          displayName: null,
          email: null,
          realm: null,
          error: null,
        },
        hasPendingSourceControlPolicyEvaluation: false,
      },
      hrefUiRouterState: hrefUiRouterStateSpy,
    };
    renderComponent = (props) => render(<ReportsPageViolationCell {...props} />);
  });

  describe('when there are no violations', () => {
    beforeEach(() => {
      const noViolationResults = {
        build: {
          alerts: [],
          affectedComponentCount: 664,
          criticalComponentCount: 0,
          severeComponentCount: 0,
          moderateComponentCount: 0,
          criticalPolicyViolationCount: 709,
          severePolicyViolationCount: 152,
          moderatePolicyViolationCount: 7,
          legacyViolationCount: 0,
          totalComponentCount: 1157,
        },
      };
      renderComponent({ ...props, app: { ...props.app, policyEvaluationsResults: noViolationResults } });
    });

    it('Renders component without violations and "No violations" message', () => {
      expect(screen.queryByText('No violations')).toBeVisible();
    });
  });

  describe('when there are pending results', () => {
    beforeEach(() => {
      const pendingResultsProps = {
        stage: 'source',
        app: {
          id: '137a638c1a474963b464a81e50be9fe4',
          publicId: 'CDPAPPGO',
          name: 'Test app',
          organizationId: 'fce7dfaa30b047d585190ac632a07bfd',
          organizationName: 'CDP Go',
          policyEvaluations: {
            source: {
              id: '1c09f8e97e9247cfb37778462efbc205',
              applicationId: '137a638c1a474963b464a81e50be9fe4',
              stageTypeId: 'source',
              scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
              time: 1636403882656,
              commitHash: null,
              initiator: 'admin',
              scanTriggerType: 'WEB_UI',
              forObsoleteScan: false,
              reevaluation: false,
              forMonitoring: false,
            },
          },
          policyEvaluationsResults: {},
          contact: {
            internalName: null,
            displayName: null,
            email: null,
            realm: null,
            error: null,
          },
          hasPendingSourceControlPolicyEvaluation: true,
        },
        hrefUiRouterState: () => {},
      };

      renderComponent({ ...pendingResultsProps });
    });

    it('Renders the component with the "pending" message', () => {
      expect(screen.getByText('pending')).toBeVisible();
    });
  });

  it('Renders the component', () => {
    renderComponent(props);

    const criticalPolicyViolationCount = screen.getByText('709');
    const severePolicyViolationCount = screen.getByText('152');
    const moderatePolicyViolationCount = screen.getByText('7');

    expect(criticalPolicyViolationCount).toBeVisible();
    expect(severePolicyViolationCount).toBeVisible();
    expect(moderatePolicyViolationCount).toBeVisible();

    const viewReport = screen.getByRole('link', { name: /view report/i });
    expect(viewReport).toBeVisible();
    fireEvent.click(viewReport);
    expect(hrefUiRouterStateSpy).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'CDPAPPGO',
      scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
    });

    expect(screen.queryByRole('link', { name: /priorities/i })).not.toBeInTheDocument();
  });

  it('it renders a "View Report" link that opens in the same tab when isDeveloperDashboardEnabled is false and isDeveloper is false', () => {
    const newProps = {
      ...props,
      isDeveloper: false,
      isDeveloperDashboardEnabled: false,
    };
    renderComponent(newProps);

    const prioritiesLink = screen.queryByRole('link', { name: /view report/i });
    expect(prioritiesLink).toBeVisible();
    expect(prioritiesLink).toHaveAttribute('target', '');
  });

  it('it renders a "View Priorities" link that opens in the same tab when isDeveloperDashboardEnabled is true and isDeveloper is true', () => {
    const newProps = {
      ...props,
      isDeveloper: true,
      isDeveloperDashboardEnabled: true,
    };
    renderComponent(newProps);

    const prioritiesLink = screen.queryByRole('link', { name: /priorities/i });
    expect(prioritiesLink).toBeVisible();
    expect(prioritiesLink).toHaveAttribute('target', '');
  });

  it('it renders "Report" link that opens in the same tab and "Priorities" link that opens in a new tab when isDeveloperDashboardEnabled is true and isDeveloper is false', () => {
    const newProps = {
      ...props,
      isDeveloper: false,
      isDeveloperDashboardEnabled: true,
    };
    renderComponent(newProps);

    const viewReport = screen.getByRole('link', { name: /report/i });
    expect(viewReport).toBeVisible();
    expect(viewReport).toHaveAttribute('target', '');

    const prioritiesLink = screen.queryByRole('link', { name: /priorities/i });
    expect(prioritiesLink).toBeVisible();
    expect(prioritiesLink).toHaveAttribute('target', '_blank');
  });

  it('Renders the component with priorities link if development dashboard is enabled', () => {
    renderComponent({ ...props, isDeveloperDashboardEnabled: true });

    const criticalPolicyViolationCount = screen.getByText('709');
    const severePolicyViolationCount = screen.getByText('152');
    const moderatePolicyViolationCount = screen.getByText('7');

    expect(criticalPolicyViolationCount).toBeVisible();
    expect(severePolicyViolationCount).toBeVisible();
    expect(moderatePolicyViolationCount).toBeVisible();

    const viewReport = screen.getByRole('link', { name: /report/i });
    expect(viewReport).toBeVisible();
    fireEvent.click(viewReport);
    expect(hrefUiRouterStateSpy).toHaveBeenCalledWith('applicationReport.policy', {
      publicId: 'CDPAPPGO',
      scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
    });

    const viewPriorities = screen.getByRole('link', { name: /priorities/i });
    expect(viewPriorities).toBeVisible();
    fireEvent.click(viewPriorities);
    expect(hrefUiRouterStateSpy).toHaveBeenCalledWith('prioritiesPageFromReports', {
      publicAppId: 'CDPAPPGO',
      scanId: '4d8ad4f41b7d46a79d95e1359d40b861',
    });
  });
});
