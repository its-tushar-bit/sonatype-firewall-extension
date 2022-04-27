/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, within } from '../SpecUtil';

import DependencyTreePage from 'MainRoot/DependencyTree/DependencyTreePage';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
import * as componentDetailsSelectors from 'MainRoot/componentDetails/componentDetailsSelectors';
import { dependencyTreeData, flatDependencyTreeData } from './dependencyTreeMockData';

describe('DependencyTreePage', () => {
  let renderComponent;
  const reportParameters = { appId: 'appId', scanId: 'scanId' };
  const reportMetadata = {
    organizationName: 'Sonatype',
    applicationName: 'Demo App',
    reportTitle: 'Build Report',
    reportTime: '2011-11-21',
  };

  beforeEach(() => {
    spyOn(applicationReportSelectors, 'selectReportParameters').and.returnValue(reportParameters);
    spyOn(applicationReportSelectors, 'selectDisplayedDependencyTree').and.returnValue(dependencyTreeData);
    spyOn(componentDetailsSelectors, 'selectApplicationInfo').and.returnValue({
      applicationName: 'This is a test name',
    });
    spyOn(RouterStateContext, 'useRouterState').and.returnValue({
      get: jasmine.createSpy('useRouterState.get'),
      href: jasmine.createSpy('useRouterState.href'),
    });
    renderComponent = () => render(<DependencyTreePage />);
  });

  it('renders the tree with the correct title', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    renderComponent();
    expect(screen.getByText('Dependency Tree')).toBeVisible();
    expect(screen.getByText('This is a test name')).toBeVisible();
  });

  it('renders the loading message', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders the header breadcrumbs', () => {
    spyOn(componentDetailsSelectors, 'selectComponentMetaData').and.returnValue(reportMetadata);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    renderComponent();
    const breadcrumbs = screen.getByTestId('dependency-tree-page-header-breadcrumbs');
    const expectedRenderedReportTime = `${reportMetadata.reportTime} 00:00:00`;
    expect(within(breadcrumbs).getByText(reportMetadata.organizationName)).toBeVisible();
    expect(within(breadcrumbs).getByText(reportMetadata.applicationName)).toBeVisible();
    expect(within(breadcrumbs).getByText(`${reportMetadata.reportTitle} ${expectedRenderedReportTime}`)).toBeVisible();
  });

  it('renders NxErrorAlert if an error is thrown', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    spyOn(applicationReportSelectors, 'selectLoadError').and.returnValue('loaded error');
    renderComponent();
    expect(screen.getByText('An error occurred loading data.', { exact: false })).toBeVisible();
    expect(screen.getByText('loaded error', { exact: false })).toBeVisible();
  });

  it('renders NxWarningAlert if no dependency tree available', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(false);
    renderComponent();
    expect(screen.getByText('Dependency tree not available.')).toBeVisible();
    expect(screen.queryByTestId('dependency-tree-tile')).toBeNull();
  });

  it('renders tile if dependency tree is available', () => {
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    renderComponent();
    expect(screen.queryByText('Dependency tree not available.')).toBeNull();
    expect(screen.getByText('This is a test name')).toBeVisible();
  });

  it('renders permanent message', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);

    renderComponent();
    expect(screen.getByText('Only supported ecosystem components are displayed in dependency tree.')).toBeVisible();
  });

  it('renders expand all and collapse all buttons', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);

    renderComponent();
    const expandAlButton = screen.getByRole('button', { name: /expand all/i });
    const collapseAllButton = screen.getByRole('button', { name: /collapse all/i });
    expect(expandAlButton).toBeVisible();
    expect(expandAlButton).toBeEnabled();
    expect(collapseAllButton).toBeVisible();
    expect(collapseAllButton).toBeEnabled();
  });

  it('renders disabled expand all and collapse all buttons', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(false);
    spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').and.returnValue(true);
    applicationReportSelectors.selectDisplayedDependencyTree.and.returnValue(flatDependencyTreeData);

    renderComponent();
    const expandAlButton = screen.getByRole('button', { name: /expand all/i });
    const collapseAllButton = screen.getByRole('button', { name: /collapse all/i });
    expect(expandAlButton).toBeVisible();
    expect(expandAlButton).toBeDisabled();
    expect(collapseAllButton).toBeVisible();
    expect(collapseAllButton).toBeDisabled();
  });
});
