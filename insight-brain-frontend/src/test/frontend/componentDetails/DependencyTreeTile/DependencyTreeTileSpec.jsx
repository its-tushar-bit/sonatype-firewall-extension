/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import DependencyTreeTile from 'MainRoot/componentDetails/overview/DependencyTreeTile/DependencyTreeTile';

import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('DependencyTreeTile', () => {
  let selectRouterCurrentParamsSpy;

  beforeEach(() => {
    selectRouterCurrentParamsSpy = spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      dependencyTreeEnabled: true,
    });
  });

  it('renders tile with title if dependencyTreeEnabled is true', () => {
    render(<DependencyTreeTile />);

    expect(screen.getByText('Dependency Tree')).toBeVisible();
  });

  it('does not render tile with title if dependencyTreeEnabled is falsy', () => {
    selectRouterCurrentParamsSpy.and.returnValue({
      dependencyTreeEnabled: null,
    });

    render(<DependencyTreeTile />);

    expect(screen.queryByText('Dependency Tree')).not.toBeInTheDocument();
  });
});
