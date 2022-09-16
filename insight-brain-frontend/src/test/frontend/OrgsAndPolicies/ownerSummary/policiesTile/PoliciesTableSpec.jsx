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
} from './policiesTileTestData';

describe('PoliciesTable ', () => {
  let owner, stages, sorting, isFirewallSupported, isEnforcementSupported, goToEditPolicySpy, props;

  stages = [
    { stageTypeId: 'proxy', shortName: 'Proxy', stageName: 'Proxy' },
    { stageTypeId: 'develop', shortName: 'Develop', stageName: 'Develop' },
    { stageTypeId: 'source', shortName: 'Source', stageName: 'Source' },
    { stageTypeId: 'build', shortName: 'Build', stageName: 'Build' },
    { stageTypeId: 'stage-release', shortName: 'Stage', stageName: 'Stage Release' },
    { stageTypeId: 'release', shortName: 'Release', stageName: 'Release' },
    { stageTypeId: 'operate', shortName: 'Operate', stageName: 'Operate' },
  ];
  sorting = {
    'Artifactory Test': {
      key: 'threatLevel',
      dir: 'desc',
      ownerName: 'Artifactory Test',
    },
    'Consumer Support': {
      key: 'threatLevel',
      dir: 'desc',
      ownerName: 'Consumer Support',
    },
    'Root Organization': {
      key: 'threatLevel',
      dir: 'desc',
      ownerName: 'Root Organization',
    },
  };
  isFirewallSupported = true;
  isEnforcementSupported = true;

  const renderComponent = (testProps) => render(<PoliciesTable {...testProps} />);

  beforeEach(() => {
    props = {
      ariaLabel: 'Policy tile local policies',
      emptyMessage: 'No local policies defined',
      owner,
      stages,
      isFirewallSupported,
      isEnforcementSupported,
      sorting,
    };
    goToEditPolicySpy = spyOn(actions, 'goToEditPolicy').and.callThrough();
  });

  it('renders an empty table with a message if owner has no policies', async () => {
    owner = applicationWithNoPolicies;

    renderComponent({ ...props, owner });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, owner, stages, sorting);
  });

  it('renders table with policies', async () => {
    owner = rootOrganizationWithPolicies;

    renderComponent({ ...props, owner });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, owner, stages, sorting);
  });

  it('renders table with policies sorting by name ascending', async () => {
    owner = rootOrganizationWithPolicies;
    sorting = {
      'Artifactory Test': {
        key: 'threatLevel',
        dir: 'desc',
        ownerName: 'Artifactory Test',
      },
      'Consumer Support': {
        key: 'threatLevel',
        dir: 'desc',
        ownerName: 'Consumer Support',
      },
      'Root Organization': {
        key: 'name',
        dir: 'asc',
        ownerName: 'Root Organization',
      },
    };

    renderComponent({ ...props, owner, sorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, owner, stages, sorting);
  });

  it('renders table with one policy and no sorting enable', async () => {
    owner = rootOrganizationWithOnePolicy;
    sorting = {
      'Artifactory Test': {
        key: 'threatLevel',
        dir: 'desc',
        ownerName: 'Artifactory Test',
      },
      'Consumer Support': {
        key: 'threatLevel',
        dir: 'desc',
        ownerName: 'Consumer Support',
      },
      'Root Organization': {
        key: 'name',
        dir: 'asc',
        ownerName: 'Root Organization',
      },
    };

    renderComponent({ ...props, owner, sorting });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, owner, stages, sorting);
  });

  it('renders table with policies when enforcement is not supported', async () => {
    owner = rootOrganizationWithPolicies;
    isFirewallSupported = false;
    isEnforcementSupported = false;

    renderComponent({ ...props, owner, isFirewallSupported, isEnforcementSupported });
    const table = await screen.findByRole('table');

    verifyPoliciesTable(table, goToEditPolicySpy, owner, stages, sorting, isFirewallSupported, isEnforcementSupported);
  });
});
