/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import RepositoriesSummaryView from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesSummaryView';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as accessSelectors from 'MainRoot/OrgsAndPolicies/access/accessSelectors';
import * as ownerPolicySelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as RouterStateContext from 'MainRoot/react/RouterStateContext';
describe('RepositoriesSummaryView', () => {
  let renderComponent;

  beforeEach(() => {
    spyOn(accessSelectors, 'selectExtendedMembersByRole').and.returnValue([]);
    spyOn(ownerPolicySelectors, 'selectSelectedOwnerName').and.returnValue('Test');
    spyOn(accessSelectors, 'selectRolesWithoutLocalMembersExist').and.returnValue(true);
    spyOn(RouterStateContext, 'useRouterState').and.returnValue({
      href: jasmine.createSpy('useRouterState.href').and.returnValue('test'),
    });
    spyOn(routerSelectors, 'selectRouterState').and.callFake(() => {
      return { name: 'test/application', data: {} };
    });

    renderComponent = () => render(<RepositoriesSummaryView />);
  });

  it('renders Access and Configuration tiles', () => {
    renderComponent();

    expect(screen.getByTestId('repositories_configuration')).toBeVisible();
    expect(screen.getByTestId('repositories_access')).toBeVisible();
    expect(screen.getByText('Configuration')).toBeInTheDocument();
    expect(screen.getByText('Access')).toBeInTheDocument();
  });
});
