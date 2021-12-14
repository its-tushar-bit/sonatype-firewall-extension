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
import { dependencyTreeData } from './dependencyTreeMockData';

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
    spyOn(applicationReportSelectors, 'selectDependencyTreeData').and.returnValue(dependencyTreeData);
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
    renderComponent();
    expect(screen.getByText('Dependency Tree')).toBeVisible();
    expect(screen.getByText('This is a test name')).toBeVisible();
  });

  it('renders the loading message', () => {
    spyOn(applicationReportSelectors, 'selectIsDependenciesLoading').and.returnValue(true);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders the header breadcrumbs', () => {
    spyOn(componentDetailsSelectors, 'selectComponentMetaData').and.returnValue(reportMetadata);
    renderComponent();
    const breadcrumbs = screen.getByTestId('dependency-tree-page-header-breadcrumbs');
    const expectedRenderedReportTime = `${reportMetadata.reportTime} 00:00:00`;
    expect(within(breadcrumbs).getByText(reportMetadata.organizationName)).toBeVisible();
    expect(within(breadcrumbs).getByText(reportMetadata.applicationName)).toBeVisible();
    expect(within(breadcrumbs).getByText(`${reportMetadata.reportTitle} ${expectedRenderedReportTime}`)).toBeVisible();
  });
});
