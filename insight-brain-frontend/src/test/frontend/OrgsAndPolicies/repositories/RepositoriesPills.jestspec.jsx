/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import RepositoriesPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesPills';

describe('RepositoriesPills — Proxy Repositories label switch (FIRE-665)', () => {
  beforeEach(() => {
    jest.restoreAllMocks();
    jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
  });

  it('renders "Proxy Repositories" on the virtual repository container view', () => {
    jest.spyOn(routerSelectors, 'selectIsVirtualRepositoryContainer').mockReturnValue(true);

    render(<RepositoriesPills />);

    expect(screen.getByText('Proxy Repositories')).toBeVisible();
    expect(screen.queryByText('Configuration')).toBeNull();
  });

  it('renders "Configuration" on the traditional repository container view', () => {
    jest.spyOn(routerSelectors, 'selectIsVirtualRepositoryContainer').mockReturnValue(false);

    render(<RepositoriesPills />);

    expect(screen.getByText('Configuration')).toBeVisible();
    expect(screen.queryByText('Proxy Repositories')).toBeNull();
  });
});
