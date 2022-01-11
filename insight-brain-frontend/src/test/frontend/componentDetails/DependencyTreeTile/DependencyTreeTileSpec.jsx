/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import DependencyTreeTile from 'MainRoot/componentDetails/overview/DependencyTreeTile/DependencyTreeTile';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as componentDetailsSelectors from 'MainRoot/componentDetails/componentDetailsSelectors';
import { dependencyTreeData } from 'TestRoot/dependencyTree/dependencyTreeMockData';

describe('DependencyTreeTile', () => {
  let selectRouterCurrentParamsSpy, renderComponent, selectDependencyTreeSubsetSpy, selectIsLabelsLoadingSpy;

  beforeEach(() => {
    selectDependencyTreeSubsetSpy = spyOn(componentDetailsSelectors, 'selectDependencyTreeSubset').and.returnValue(
      dependencyTreeData
    );
    spyOn(componentDetailsSelectors, 'selectApplicationInfo').and.returnValue({
      applicationName: 'This is a test name',
    });
    spyOn(componentDetailsSelectors, 'selectComponentDetails').and.returnValue({
      hash: 'hash',
    });

    selectRouterCurrentParamsSpy = spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      dependencyTreeEnabled: true,
    });
    selectIsLabelsLoadingSpy = spyOn(componentDetailsSelectors, 'selectIsLabelsLoading').and.returnValue(false);

    renderComponent = () => render(<DependencyTreeTile />);
  });

  it('renders tile with title', () => {
    renderComponent();

    expect(screen.getByText('Dependency Tree')).toBeVisible();
  });

  it('renders alert if dependencyTreeSubset does not exist', () => {
    selectDependencyTreeSubsetSpy.and.returnValue(null);

    renderComponent();

    expect(screen.getByText('Dependency tree not available')).toBeVisible();
  });

  it('does not render tile with title if dependencyTreeEnabled is falsy', () => {
    selectRouterCurrentParamsSpy.and.returnValue({
      dependencyTreeEnabled: null,
    });

    renderComponent();

    expect(screen.queryByText('Dependency Tree')).not.toBeInTheDocument();
  });

  it('does not render tile with title if data is still loading', () => {
    selectIsLabelsLoadingSpy.and.returnValue(true);

    renderComponent();

    expect(screen.queryByText('Dependency Tree')).not.toBeInTheDocument();
  });
});
