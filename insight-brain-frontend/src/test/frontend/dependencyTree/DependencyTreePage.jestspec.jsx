/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, within } from '../SpecUtil';

import DependencyTreePage from 'MainRoot/DependencyTree/DependencyTreePage';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import router from 'MainRoot/router/routerInstance';
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
    jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue(reportParameters);
    jest.spyOn(applicationReportSelectors, 'selectDisplayedDependencyTree').mockReturnValue(dependencyTreeData);
    jest.spyOn(componentDetailsSelectors, 'selectApplicationInfo').mockReturnValue({
      applicationName: 'This is a test name',
    });
    jest.spyOn(router.stateService, 'get').mockReturnValue(null);
    jest.spyOn(router.stateService, 'href').mockReturnValue('#');
    jest.spyOn(router.stateService, 'includes').mockReturnValue(false);
    renderComponent = () => render(<DependencyTreePage />);
  });

  it('renders the tree with the correct title', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('Dependency Tree')).toBeVisible();
    expect(screen.getByText('This is a test name')).toBeVisible();
  });

  it('renders the loading message', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders the header breadcrumbs', () => {
    jest.spyOn(componentDetailsSelectors, 'selectComponentMetaData').mockReturnValue(reportMetadata);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    renderComponent();
    const header = screen.getByRole('banner');
    const expectedRenderedReportTime = `${reportMetadata.reportTime} 00:00:00`;
    expect(within(header).getByText(reportMetadata.organizationName)).toBeVisible();
    expect(within(header).getByText(reportMetadata.applicationName)).toBeVisible();
    expect(within(header).getByText(`${reportMetadata.reportTitle} ${expectedRenderedReportTime}`)).toBeVisible();
  });

  it('renders NxErrorAlert if an error is thrown', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectLoadError').mockReturnValue('loaded error');
    renderComponent();
    expect(screen.getByText('An error occurred loading data.', { exact: false })).toBeVisible();
    expect(screen.getByText('loaded error', { exact: false })).toBeVisible();
  });

  it('renders NxWarningAlert if no dependency tree available', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(false);
    renderComponent();
    expect(screen.getByText('Dependency tree not available.')).toBeVisible();
    expect(screen.queryByRole('button', { name: /expand all/i })).toBeNull();
  });

  it('renders tile if dependency tree is available', () => {
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    renderComponent();
    expect(screen.queryByText('Dependency tree not available.')).toBeNull();
    expect(screen.getByText('This is a test name')).toBeVisible();
  });

  it('renders permanent message', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);

    renderComponent();
    expect(screen.getByText('Only supported ecosystem components are displayed in dependency tree.')).toBeVisible();
  });

  it('renders expand all and collapse all buttons', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);

    renderComponent();
    const expandAlButton = screen.getByRole('button', { name: /expand all/i });
    const collapseAllButton = screen.getByRole('button', { name: /collapse all/i });
    expect(expandAlButton).toBeVisible();
    expect(expandAlButton).toBeEnabled();
    expect(collapseAllButton).toBeVisible();
    expect(collapseAllButton).toBeEnabled();
  });

  it('renders disabled expand all and collapse all buttons', () => {
    jest.spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').mockReturnValue(false);
    jest.spyOn(applicationReportSelectors, 'selectDependencyTreeIsAvailable').mockReturnValue(true);
    jest.spyOn(applicationReportSelectors, 'selectDisplayedDependencyTree').mockReturnValue(flatDependencyTreeData);

    renderComponent();
    const expandAlButton = screen.getByRole('button', { name: /expand all/i });
    const collapseAllButton = screen.getByRole('button', { name: /collapse all/i });
    expect(expandAlButton).toBeVisible();
    expect(expandAlButton).toBeDisabled();
    expect(collapseAllButton).toBeVisible();
    expect(collapseAllButton).toBeDisabled();
  });
});
