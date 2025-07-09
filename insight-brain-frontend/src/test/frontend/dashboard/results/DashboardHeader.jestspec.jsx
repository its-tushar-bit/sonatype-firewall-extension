/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import DashboardHeader from 'MainRoot/dashboard/results/DashboardHeader';

describe('DashboardHeader', () => {
  let renderComponent;

  const minimalProps = {
    dashboard: {
      currentTab: 'violations',
      violations: {
        results: [],
      },
      isDashboardEnabled: true,
      isWaiversTabEnabled: false,
    },
    filterSidebarOpen: false,
    toggleFilterSidebar: jest.fn(),
    appliedFilterName: 'Default',
    filterLoading: false,
    loadFilterError: null,
    exportTitle: 'Export Title',
    exportRequestData: {},
    exportUrl: '/export',
    showDirtyAsterisk: false,
    loadFilter: jest.fn(),
    stateGo: jest.fn(),
    isDashboardEnabled: true,
    isWaiversTabEnabled: false,
    prevStateName: '',
  };

  beforeEach(() => {
    renderComponent = (additionalProps) => {
      render(<DashboardHeader {...minimalProps} {...additionalProps} />);
    };
  });

  it('renders the component with the correct title', () => {
    renderComponent();
    expect(screen.getByText('Results')).toBeInTheDocument();
  });

  it('renders the export button with the correct title', () => {
    renderComponent();
    const exportButton = screen.queryByRole('button', { name: /Export Title/i });
    const formElement = screen.getByRole('form');
    expect(exportButton).toBeInTheDocument();
    expect(formElement).toHaveAttribute('action', '/export');
  });

  it('displays the filter button with the correct label', () => {
    renderComponent();
    const filterButton = screen.queryByRole('button', { name: /Filter/i });
    expect(filterButton).toBeInTheDocument();
    expect(filterButton).toHaveTextContent('Filter: Default');
  });

  it('calls toggleFilterSidebar when the filter button is clicked', () => {
    renderComponent();
    const filterButton = screen.queryByRole('button', { name: /Filter/i });
    fireEvent.click(filterButton);
    expect(minimalProps.toggleFilterSidebar).toHaveBeenCalledWith(true);
  });

  it('displays an alert when the dashboard is disabled and hides export and filter buttons', () => {
    renderComponent({ isDashboardEnabled: false });
    const exportButton = screen.queryByRole('button', { name: /Export Title/i });
    const filterButton = screen.queryByRole('button', { name: /Filter/i });
    const alert = screen.getByText('The Dashboard feature has been disabled by your administrator.');
    expect(exportButton).not.toBeInTheDocument();
    expect(filterButton).not.toBeInTheDocument();
    expect(alert).toBeInTheDocument();
  });
});
