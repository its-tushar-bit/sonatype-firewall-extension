/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/dom';
import { render } from 'TestRoot/SpecUtil';
import EnterpriseReportCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportCard';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import userEvent from '@testing-library/user-event';

describe('EnterpriseReportCard', () => {
  let renderComponent, stateGoSpy;

  const initialState = {
    dashboard: {
      dashboardId: 'rolling-recap',
      category: 'dataInsight',
      title: 'Rolling Recap Dashboard',
      description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
      features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
      accessButtonText: 'View Rolling Recap',
      gropuId: 'parent',
      previewImage: '',
      previewImageIcon: 'faBrain',
      priority: 1,
      spotlight: false,
      spotlightText: '',
      spotlightColor: '',
    },
    iqVersion: '1.188.0-SNAPSHOT',
  };

  renderComponent = (props) => render(<EnterpriseReportCard key={1} {...initialState} {...props} />);

  it('renders the card', async () => {
    const { dashboard } = initialState;
    renderComponent();

    expect(await screen.findByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
    const cardHeading = screen.getByRole('heading');

    expect(cardHeading).toHaveTextContent(dashboard.title);
    expect(screen.queryByText(dashboard.description)).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[0])).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[1])).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[2])).toBeInTheDocument();
    // querying by default text 'NEW', as spotlightText is not assigned
    expect(screen.queryByText('NEW')).not.toBeInTheDocument();

    const btn = screen.getByRole('button', { name: dashboard.accessButtonText });
    expect(btn).toBeInTheDocument();
  });

  it('calls stateGo with given dashboard id on button click', async () => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    const user = userEvent.setup();
    renderComponent();

    const button = screen.getByRole('button', { name: 'View Rolling Recap' });
    expect(button).toBeInTheDocument();
    await user.click(button);

    expect(stateGoSpy).toHaveBeenCalledWith('enterpriseReportingDashboard', {
      id: initialState.dashboard.dashboardId,
    });
  });

  it('renders a disabled button when iQVersion is older than dashboard.sinceIQVersion', async () => {
    const dashboard = {
      ...initialState.dashboard,
      sinceIQVersion: '189',
    };
    renderComponent({ dashboard: dashboard });

    const button = screen.getByRole('button', { name: dashboard.accessButtonText });
    expect(button).toBeInTheDocument();
    expect(button).toBeDisabled();
  });

  describe('group card', () => {
    const dashboard = {
      category: 'enterprise',
      groupId: 'parent',
      features: ['feature1', 'feature2', 'feature3'],
      title: 'Parent Card',
      sinceIQVersion: '188',
      previewImageIcon: 'faBrain',
      groupedDashboards: [
        {
          category: 'enterprise',
          groupId: 'parent',
          dashboardId: 'dashboard1',
          sinceIQVersion: '188',
          accessButtonText: 'View Dashboard1',
        },
        {
          category: 'enterprise',
          groupId: 'parent',
          dashboardId: 'dashboard2',
          sinceIQVersion: '190',
          accessButtonText: 'View Dashboard2',
        },
      ],
    };

    it('does not disable the button if at least one child dashboard is not disabled', () => {
      renderComponent({ dashboard });
      const button = screen.getByRole('button', { name: dashboard.groupedDashboards[0].accessButtonText });
      expect(button).not.toBeDisabled();
    });

    it('disables the button if all children are disabled', () => {
      const disabledDashboard = {
        ...dashboard,
        groupedDashboards: [
          {
            ...dashboard.groupedDashboards[0],
            sinceIQVersion: '200',
          },
          dashboard.groupedDashboards[1],
        ],
      };
      renderComponent({ dashboard: disabledDashboard });
      const button = screen.getByRole('button', { name: dashboard.groupedDashboards[0].accessButtonText });
      expect(button).toBeDisabled();
    });

    it('calls stateGo with dashboardId & groupId on button click', async () => {
      stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
      const user = userEvent.setup();
      renderComponent({ dashboard });

      const button = screen.getByRole('button', { name: dashboard.groupedDashboards[0].accessButtonText });
      expect(button).toBeInTheDocument();
      await user.click(button);

      expect(stateGoSpy).toHaveBeenCalledWith('enterpriseReportingDashboardGroup', {
        id: dashboard.groupedDashboards[0].dashboardId,
        groupId: dashboard.groupId,
      });
    });

    it('calls stateGo with dashboardId & groupId on dropdown button click', async () => {
      stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
      const user = userEvent.setup();
      renderComponent({ dashboard });

      const dropdownBtn = screen.getByRole('button', { name: 'more options' });
      await user.click(dropdownBtn);

      const dropdownOption = screen.getByRole('button', { name: dashboard.groupedDashboards[1].accessButtonText });
      expect(dropdownOption).toBeVisible();
      await user.click(dropdownOption);

      expect(stateGoSpy).toHaveBeenCalledWith('enterpriseReportingDashboardGroup', {
        id: dashboard.groupedDashboards[1].dashboardId,
        groupId: dashboard.groupId,
      });
    });
  });

  describe('spotlight', () => {
    it('does not spotlight a given card if spotlightText is not assigned and spotlight is false', () => {
      renderComponent();

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      // querying from the default 'NEW' text as spotlightText is unassigned
      expect(screen.queryByText('NEW')).not.toBeInTheDocument();
    });

    it('spotlights a given card with default text and default color', () => {
      const dashboard = {
        ...initialState.dashboard,
        spotlight: true,
      };
      renderComponent({ dashboard: dashboard });

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      expect(screen.queryByText('NEW')).toBeInTheDocument();
      expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-small-tag--purple');
    });

    it('spotlights a given card with custom text if spotlightText is assigned', () => {
      const dashboard = {
        ...initialState.dashboard,
        spotlight: true,
        spotlightText: 'TEST',
      };
      renderComponent({ dashboard: dashboard });

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      expect(screen.getByText('TEST')).toBeInTheDocument();
    });

    it('spotlights a given card with custom text if spotlightText is assigned and spotlight is false', () => {
      const dashboard = {
        ...initialState.dashboard,
        spotlight: false,
        spotlightText: 'TEST',
      };
      renderComponent({ dashboard: dashboard });

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      expect(screen.getByText('TEST')).toBeInTheDocument();
    });

    it('spotlights a given card with a valid color', () => {
      const dashboard = { ...initialState.dashboard, spotlight: true, spotlightColor: 'pink' };
      renderComponent({ dashboard: dashboard });

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-small-tag--pink');
    });

    it('spotlights a given card with an invalid color rendering default', () => {
      const dashboard = {
        ...initialState.dashboard,
        spotlight: true,
        spotlightColor: 'invalid',
      };
      renderComponent({ dashboard: dashboard });

      expect(screen.getByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
      expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-small-tag--purple');
    });
  });

  describe('telemetry tracking', () => {
    it('includes telemetry ID for herodevs_eol dashboard', () => {
      const dashboard = {
        ...initialState.dashboard,
        dashboardId: 'herodevs_eol',
        accessButtonText: 'View HeroDevs EOL',
      };
      renderComponent({ dashboard });

      const button = screen.getByRole('button', { name: 'View HeroDevs EOL' });
      expect(button).toHaveAttribute('data-analytics-id', 'lc-reporting-herodevs-view-cta');
    });

    it('includes telemetry ID for best_practices dashboard', () => {
      const dashboard = {
        ...initialState.dashboard,
        dashboardId: 'best_practices',
        accessButtonText: 'View Best Practices',
      };
      renderComponent({ dashboard });

      const button = screen.getByRole('button', { name: 'View Best Practices' });
      expect(button).toHaveAttribute('data-analytics-id', 'lc-reporting-best-practices-view-cta');
    });

    it('does not include telemetry ID for other dashboards', () => {
      renderComponent();

      const button = screen.getByRole('button', { name: initialState.dashboard.accessButtonText });
      expect(button).not.toHaveAttribute('data-analytics-id');
    });
  });
});
