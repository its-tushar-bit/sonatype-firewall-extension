/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render } from 'TestRoot/SpecUtil';
import EnterpriseReportCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportCard';
import { screen } from '@testing-library/dom';

describe('EnterpriseReportCard', () => {
  let renderComponent;

  let dashboard = {
    dashboardId: 'rolling-recap',
    title: 'Rolling Recap Dashboard: Past 365 Days',
    description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
    features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
    accessButtonText: 'View Rolling Recap',
    previewImage: '',
    priority: 1,
    spotlight: false,
  };

  beforeEach(() => {
    renderComponent = () => render(<EnterpriseReportCard dashboard={dashboard} key={1} />);
  });

  it('renders the card', async () => {
    renderComponent();

    expect(await screen.findByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: dashboard.title })).toBeInTheDocument();
    expect(screen.queryByText(dashboard.description)).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[0])).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[1])).toBeInTheDocument();
    expect(screen.queryByText(dashboard.features[2])).toBeInTheDocument();
    expect(screen.queryByText('NEW')).not.toBeInTheDocument();

    const btn = screen.getByRole('button', { name: dashboard.accessButtonText });
    expect(btn).toBeInTheDocument();
    expect(btn).toHaveClass('dashboard-id-btn-rolling-recap');
  });

  it('spotligths a given card with default color', async () => {
    dashboard = { ...dashboard, spotlight: true };

    renderComponent();

    expect(await screen.findByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
    expect(screen.queryByText('NEW')).toBeInTheDocument();
    expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-selectable-color--turquoise');
    expect(screen.getByRole('button', { name: dashboard.accessButtonText })).toBeInTheDocument();
  });

  it('spotligths a given card with a valid color', async () => {
    dashboard = { ...dashboard, spotlight: true, spotlightColor: 'kiwi' };

    renderComponent();

    expect(await screen.findByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
    expect(screen.queryByText('NEW')).toBeInTheDocument();
    expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-selectable-color--kiwi');
    expect(screen.getByRole('button', { name: dashboard.accessButtonText })).toBeInTheDocument();
  });

  it('spotligths a given card with an invalid color rendering default', async () => {
    dashboard = { ...dashboard, spotlight: true, spotlightColor: 'invalid' };

    renderComponent();

    expect(await screen.findByRole('enterprise-reporting-dashboard-card')).toBeInTheDocument();
    expect(screen.queryByText('NEW')).toBeInTheDocument();
    expect(screen.queryByText('NEW').parentElement).toHaveClass('nx-selectable-color--turquoise');
    expect(screen.getByRole('button', { name: dashboard.accessButtonText })).toBeInTheDocument();
  });
});
