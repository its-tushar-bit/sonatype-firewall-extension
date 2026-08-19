/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import FirewallEnterpriseReportCard from 'MainRoot/firewall/enterpriseReporting/card/FirewallEnterpriseReportCard';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('FirewallEnterpriseReportCard', () => {
  let stateGoSpy;

  const mockDashboard = {
    dashboardId: 'malware-insights',
    title: 'Malware Insights',
    category: 'firewall',
    spotlight: true,
    spotlightColor: 'teal',
    spotlightText: 'NEW',
    previewImageIcon: 'faShieldVirus',
    description: 'Comprehensive malware detection and analysis',
    features: ['Real-time monitoring', 'Threat detection', 'Automated quarantine'],
    accessButtonText: 'View Dashboard',
    sinceIQVersion: '170',
  };

  const defaultProps = {
    dashboard: mockDashboard,
    iqVersion: '1.170.0',
  };

  beforeEach(() => {
    stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = (props = {}) => {
    return render(<FirewallEnterpriseReportCard {...defaultProps} {...props} />);
  };

  it('renders the card with no intermediate div wrapper', () => {
    const { container } = renderComponent();

    // NxCard is a direct child of container — no wrapping div
    const card = container.firstElementChild;
    expect(card).toHaveAttribute('id', 'fw-enterprise-reporting-dashboard-malware-insights');
  });

  it('should render dashboard card with title and description', () => {
    renderComponent();

    expect(screen.getByText('Malware Insights')).toBeInTheDocument();
    expect(screen.getByText('Comprehensive malware detection and analysis')).toBeInTheDocument();
  });

  it('should render spotlight text when provided', () => {
    renderComponent();

    expect(screen.getByText('NEW')).toBeInTheDocument();
  });

  it('should render all features as list items', () => {
    renderComponent();

    expect(screen.getByText('Real-time monitoring')).toBeInTheDocument();
    expect(screen.getByText('Threat detection')).toBeInTheDocument();
    expect(screen.getByText('Automated quarantine')).toBeInTheDocument();
  });

  it('should render access button with correct text', () => {
    renderComponent();

    expect(screen.getByRole('button', { name: 'View Dashboard' })).toBeInTheDocument();
  });

  it('should navigate to dashboard detail page when button is clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    const button = screen.getByRole('button', { name: 'View Dashboard' });
    await user.click(button);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.enterpriseReportingDashboard', {
      id: 'malware-insights',
    });
  });

  it('should disable button when IQ version is lower than required', () => {
    renderComponent({ iqVersion: '1.169.0' });

    const button = screen.getByRole('button', { name: 'View Dashboard' });
    expect(button).toBeDisabled();
  });

  it('should enable button when IQ version meets requirement', () => {
    renderComponent({ iqVersion: '1.170.0' });

    const button = screen.getByRole('button', { name: 'View Dashboard' });
    expect(button).not.toBeDisabled();
  });

  it('should wrap disabled button with tooltip', () => {
    renderComponent({ iqVersion: '1.169.0' });

    const button = screen.getByRole('button', { name: 'View Dashboard' });
    expect(button).toBeDisabled();
    // Button is wrapped in tooltip span
    expect(button.parentElement.tagName).toBe('SPAN');
  });

  it('should render the view CTA button with the correct analytics ID', () => {
    renderComponent();

    const button = screen.getByRole('button', { name: 'View Dashboard' });
    expect(button).toHaveAttribute('data-analytics-id', 'fw-reporting-malware-insights-view-cta');
  });

  it('should render card with correct ID', () => {
    renderComponent();

    expect(document.getElementById('fw-enterprise-reporting-dashboard-malware-insights')).toBeInTheDocument();
  });

  it('should handle dashboards without spotlight text', () => {
    const dashboardWithoutSpotlight = {
      ...mockDashboard,
      spotlightText: null,
    };
    renderComponent({ dashboard: dashboardWithoutSpotlight });

    expect(screen.getByText('Malware Insights')).toBeInTheDocument();
  });

  it('should apply firewall class to icon when category is firewall', () => {
    renderComponent();

    const list = screen.getByRole('list');
    const checkIcons = within(list).getAllByRole('img', { hidden: true });
    checkIcons.forEach((icon) => expect(icon).toHaveClass('firewall'));
  });

  it('should not apply firewall class to icon when category is not firewall', () => {
    const nonFirewallDashboard = { ...mockDashboard, category: 'enterprise' };
    renderComponent({ dashboard: nonFirewallDashboard });

    const list = screen.getByRole('list');
    const checkIcons = within(list).getAllByRole('img', { hidden: true });
    checkIcons.forEach((icon) => expect(icon).not.toHaveClass('firewall'));
  });

  it('should apply firewall class to feature checkmark icons when category is firewall', () => {
    renderComponent();

    const list = screen.getByRole('list');
    const checkIcons = within(list).getAllByRole('img', { hidden: true });
    expect(checkIcons.length).toBeGreaterThan(0);
    checkIcons.forEach((icon) => expect(icon).toHaveClass('firewall'));
  });

  it('should render spotlight with teal color for firewall category', () => {
    renderComponent();

    expect(screen.getByText('NEW').closest('.nx-small-tag')).toHaveClass('nx-small-tag--teal');
  });

  it('should use dashboard spotlightColor when it is a valid small tag color', () => {
    const dashboardWithBlueSpotlight = { ...mockDashboard, spotlightColor: 'blue' };
    renderComponent({ dashboard: dashboardWithBlueSpotlight });

    expect(screen.getByText('NEW').closest('.nx-small-tag')).toHaveClass('nx-small-tag--blue');
  });

  it('should fallback to teal when spotlightColor is not a valid small tag color', () => {
    const dashboardWithInvalidColor = { ...mockDashboard, spotlightColor: 'yellow' };
    renderComponent({ dashboard: dashboardWithInvalidColor });

    expect(screen.getByText('NEW').closest('.nx-small-tag')).toHaveClass('nx-small-tag--teal');
  });
});
