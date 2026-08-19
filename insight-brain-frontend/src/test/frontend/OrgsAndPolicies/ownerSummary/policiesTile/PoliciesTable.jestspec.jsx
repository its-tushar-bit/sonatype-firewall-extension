/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';

import PoliciesTable from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTable';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { verifyPoliciesTable } from 'TestRoot/OrgsAndPolicies/ownerSummary/policiesTile/policiesTableSpecUtil';
import {
  applicationWithNoPolicies,
  rootOrganizationWithPolicies,
  rootOrganizationWithOnePolicy,
  inheritanceOrgWithOnePolicy,
  inheritanceOrgWithNoPolicy,
} from './policiesTileTestData';

import 'TestRoot/SpecUtil';

describe('PoliciesTable ', () => {
  let policiesByOwner,
    stages,
    collapsibleSorting,
    isFirewallSupported,
    isEnforcementSupported,
    goToEditPolicySpy,
    props;

  stages = [
    { stageTypeId: 'proxy', shortName: 'Proxy', stageName: 'Proxy' },
    { stageTypeId: 'develop', shortName: 'Develop', stageName: 'Develop' },
    { stageTypeId: 'source', shortName: 'Source', stageName: 'Source' },
    { stageTypeId: 'build', shortName: 'Build', stageName: 'Build' },
    { stageTypeId: 'stage-release', shortName: 'Stage', stageName: 'Stage Release' },
    { stageTypeId: 'release', shortName: 'Release', stageName: 'Release' },
    { stageTypeId: 'operate', shortName: 'Operate', stageName: 'Operate' },
  ];
  collapsibleSorting = {
    key: 'threatLevel',
    dir: 'desc',
  };
  isFirewallSupported = true;
  isEnforcementSupported = true;

  const renderComponent = (testProps) => render(<PoliciesTable {...testProps} />);

  beforeEach(() => {
    props = {
      ariaLabel: 'Policy tile local policies',
      emptyMessage: 'No local policies defined',
      policiesByOwner,
      stages,
      isFirewallSupported,
      isEnforcementSupported,
      collapsibleSorting,
    };
    goToEditPolicySpy = jest.spyOn(actions, 'goToEditPolicy');
  });

  it('renders an empty messagess if owner has no policies', async () => {
    policiesByOwner = [applicationWithNoPolicies];

    renderComponent({ ...props, policiesByOwner });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, policiesByOwner, stages, collapsibleSorting);
  });

  it('renders table with policies', async () => {
    policiesByOwner = [rootOrganizationWithPolicies];

    renderComponent({ ...props, policiesByOwner });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, policiesByOwner, stages, collapsibleSorting);
  });

  it('renders table with local policies sorting by name ascending', async () => {
    policiesByOwner = [rootOrganizationWithPolicies];
    collapsibleSorting = {
      key: 'name',
      dir: 'asc',
    };

    renderComponent({ ...props, policiesByOwner, collapsibleSorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, policiesByOwner, stages, collapsibleSorting);
  });

  it('renders table with one owner policy and no sorting enable', async () => {
    policiesByOwner = [rootOrganizationWithOnePolicy];
    collapsibleSorting = {
      key: 'threatLevel',
      dir: 'asc',
    };

    renderComponent({ ...props, policiesByOwner, collapsibleSorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, policiesByOwner, stages, collapsibleSorting);
  });

  it('renders table with owner policies when enforcement is not supported', async () => {
    policiesByOwner = [rootOrganizationWithPolicies];
    isFirewallSupported = false;
    isEnforcementSupported = false;

    renderComponent({ ...props, policiesByOwner, isFirewallSupported, isEnforcementSupported });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(
      table,
      goToEditPolicySpy,
      policiesByOwner,
      stages,
      collapsibleSorting,
      isFirewallSupported,
      isEnforcementSupported
    );
  });

  it('renders table with owner and inheritance policies and sorting by threatLevel asc', async () => {
    policiesByOwner = [rootOrganizationWithPolicies, inheritanceOrgWithOnePolicy];
    isFirewallSupported = false;
    isEnforcementSupported = false;
    collapsibleSorting = {
      key: 'threatLevel',
      dir: 'asc',
    };

    renderComponent({ ...props, policiesByOwner, isFirewallSupported, isEnforcementSupported, collapsibleSorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(
      table,
      goToEditPolicySpy,
      policiesByOwner,
      stages,
      collapsibleSorting,
      isFirewallSupported,
      isEnforcementSupported
    );
  });

  it('renders table with owner and inheritance policies and an empty inheritance policies', async () => {
    policiesByOwner = [rootOrganizationWithPolicies, inheritanceOrgWithOnePolicy, inheritanceOrgWithNoPolicy];
    isFirewallSupported = false;
    isEnforcementSupported = false;
    collapsibleSorting = {
      key: 'threatLevel',
      dir: 'asc',
    };

    renderComponent({ ...props, policiesByOwner, isFirewallSupported, isEnforcementSupported, collapsibleSorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(
      table,
      goToEditPolicySpy,
      policiesByOwner,
      stages,
      collapsibleSorting,
      isFirewallSupported,
      isEnforcementSupported
    );
  });

  it('renders table with no owner and inheritance policies and an empty inheritance policies', async () => {
    policiesByOwner = [applicationWithNoPolicies, inheritanceOrgWithOnePolicy, inheritanceOrgWithNoPolicy];
    isFirewallSupported = false;
    isEnforcementSupported = false;
    collapsibleSorting = {
      key: 'threatLevel',
      dir: 'asc',
    };

    renderComponent({ ...props, policiesByOwner, isFirewallSupported, isEnforcementSupported, collapsibleSorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(
      table,
      goToEditPolicySpy,
      policiesByOwner,
      stages,
      collapsibleSorting,
      isFirewallSupported,
      isEnforcementSupported
    );
  });
});
