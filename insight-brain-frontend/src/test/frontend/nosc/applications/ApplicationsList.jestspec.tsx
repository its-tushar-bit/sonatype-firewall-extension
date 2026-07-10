/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ApplicationsList from 'MainRoot/nosc/applications/ApplicationsList';
import ApplicationsPage from 'MainRoot/nosc/applications/ApplicationsPage';
import {
  MOCK_APPLICATION_RISK_SCORES,
  MOCK_APPLICATIONS_FILTER_FACETS,
} from 'MainRoot/nosc/applications/mockApplicationsListData';
import { _setBaseUrlForTesting } from 'MainRoot/util/urlUtil';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

describe('ApplicationsList (CLM-42223)', () => {
  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  const renderList = () => renderNexusOneRoute(<ApplicationsList />, 'nexusOneApplications');

  it('renders the two-column page shell with filter rail and content area', () => {
    renderList();
    expect(screen.getByTestId('preview-applications-page')).toBeInTheDocument();
    expect(screen.getByTestId('applications-page-layout')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-rail')).toBeInTheDocument();
    expect(screen.getByTestId('applications-page-content')).toBeInTheDocument();
  });

  it('renders filter rail sections with stub facet counts', () => {
    renderList();
    expect(screen.getByTestId('applications-filter-threat-level')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-stages')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-organizations')).toBeInTheDocument();
    expect(screen.getByTestId('applications-filter-applications')).toBeInTheDocument();

    const filterRail = screen.getByTestId('applications-filter-rail');
    expect(filterRail).toHaveTextContent('Develop');
    expect(filterRail).toHaveTextContent('Java-team');
    expect(filterRail).toHaveTextContent('Apple - Java');
  });

  it('renders toolbar placeholders and total count from mock facets', () => {
    renderList();
    expect(screen.getByTestId('applications-toolbar')).toBeInTheDocument();
    expect(screen.getByTestId('applications-toolbar-search')).toBeInTheDocument();
    expect(screen.getByTestId('applications-toolbar-sort')).toHaveTextContent('Sort: Latest Evaluation');
    expect(screen.getByTestId('applications-toolbar-csv')).toBeInTheDocument();
    expect(screen.getByTestId('applications-toolbar-count')).toHaveTextContent(
      `${MOCK_APPLICATIONS_FILTER_FACETS.totalApplications} applications`,
    );
  });

  it('renders mocked evaluation cards instead of a data table', () => {
    renderList();
    expect(screen.queryByTestId('applications-list-table')).not.toBeInTheDocument();
    expect(screen.getByTestId('evaluation-card-grid')).toBeInTheDocument();
    expect(screen.getAllByTestId('evaluation-card')).toHaveLength(MOCK_APPLICATION_RISK_SCORES.length);
    expect(screen.getAllByTestId('nosc-dashboard-app-link')).toHaveLength(MOCK_APPLICATION_RISK_SCORES.length);
    expect(screen.getAllByTestId('evaluation-card-stage-tile').length).toBeGreaterThan(0);
    expect(screen.getByRole('link', { name: /apple - java/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /banana - java/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /cherry - platform/i })).toBeInTheDocument();
  });

  it('card app name links to the Preview Application Detail page', () => {
    renderList();
    const nameLink = screen.getByRole('link', { name: /apple - java/i });
    expect(nameLink).toHaveAttribute('href', expect.stringContaining('/applications/apple-java'));
  });

  it('page wrapper offsets reflow when LeftNav collapses', async () => {
    window.localStorage.removeItem('nosc.leftnav.collapsed');
    renderList();
    const pageMain = screen.getByTestId('preview-applications-page') as HTMLElement;
    expect(pageMain.style.left).toBe('256px');

    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: true } }),
      );
    });
    expect(pageMain.style.left).toBe('64px');

    await act(async () => {
      window.dispatchEvent(
        new CustomEvent('nosc.leftnav.collapsed.change', { detail: { collapsed: false } }),
      );
    });
    expect(pageMain.style.left).toBe('256px');
  });
});

describe('ApplicationsPage async states', () => {
  it('renders loading skeleton when loading', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        loading
      />,
      'nexusOneApplications',
    );
    expect(screen.getByTestId('applications-list-loading')).toBeInTheDocument();
  });

  it('renders error banner with retry when error is set', async () => {
    const onRetry = jest.fn();
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
        error="Backend unavailable"
        onRetry={onRetry}
      />,
      'nexusOneApplications',
    );
    expect(screen.getByTestId('applications-list-error')).toBeInTheDocument();
    const retryButton = await screen.findByRole('button', { name: /retry/i });
    await userEvent.click(retryButton);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders empty state when there are no applications', () => {
    renderNexusOneRoute(
      <ApplicationsPage
        applications={[]}
        facets={{ ...MOCK_APPLICATIONS_FILTER_FACETS, totalApplications: 0 }}
      />,
      'nexusOneApplications',
    );
    expect(screen.getByTestId('applications-list-empty')).toBeInTheDocument();
  });
});
