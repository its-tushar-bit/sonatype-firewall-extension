/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import OwnerDetailSidebar from 'MainRoot/OrgsAndPolicies/navigation/OwnerDetailSidebar';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getOwnerDetailsUrl, getOwnerListUrl } from 'MainRoot/util/CLMLocation';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

const APPS = [
  {
    id: 'applicationOneID',
    publicId: 'applicationOnePublicID',
    organizationId: 'organizationOneID',
    name: 'ApplicationOneName',
  },
];

const ORGS = [
  {
    id: 'organizationOneID',
    name: 'OrganizationOneName',
  },
];

const defaultPreloadedState = {
  router: {
    currentState: {
      name: 'management.edit.organization.create-category',
      url: '/category',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
  productFeatures: {
    productFeatures: {
      'saas-lifecycle-scm-enabled': true,
    },
  },
  orgsAndPolicies: {
    applications: {
      applications: APPS,
    },
    organizations: {
      organizations: ORGS,
    },
    root: {
      selectedOwner: {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
      },
    },
    policyMonitoring: {
      isLegacyViolationSupported: false,
      isMonitoringSupported: true,
    },
  },
};

const ownerDetailMockData = {
  tags: [
    {
      id: 'a14b4fe0c71a40ad9e09ca03c5df3c4b',
      organizationId: 'ROOT_ORGANIZATION_ID',
      name: 'categoryTest',
      color: 'yellow',
    },
  ],
  policies: [
    {
      id: '2dcd81057072496ca08545c8ef96bb2a',
      name: 'policyTest',
      ownerId: 'ROOT_ORGANIZATION_ID',
      threatLevel: 1,
    },
  ],
  labels: [
    {
      id: '2db308c46db84fbeb0ecc43e98e5b85d',
      ownerId: 'ROOT_ORGANIZATION_ID',
      label: 'labelTest',
      color: 'orange',
    },
  ],
  licenseThreatGroups: [
    {
      id: 'f67e3b5c58204e91ae8ca8b0c0a15683',
      ownerId: 'ROOT_ORGANIZATION_ID',
      name: 'licenseThreatGroupsTest',
      threatLevel: 10,
    },
  ],
  roles: {
    membersByRole: [
      {
        roleId: '2cb71b3468d649789163ea2e212b541e',
        roleName: 'Application Evaluator',
        membersByOwner: [
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            members: [
              {
                type: 'User',
                internalName: 'admin',
                displayName: 'Admin',
                email: 'admin@localhost',
                realm: 'IQ Server',
              },
            ],
          },
        ],
      },
    ],
  },
};

const repositoryState = {
  ...defaultPreloadedState,
  router: {
    currentState: {
      name: 'management.edit.repositories.add-access',
      url: '/access',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
};

const appsLevelState = {
  ...defaultPreloadedState,
  router: {
    currentState: {
      name: 'management.edit.application.category',
      url: '/category',
    },
    currentParams: {
      applicationPublicId: 'applicationOnePublicID',
    },
  },
  orgsAndPolicies: {
    root: {
      selectedOwner: {
        id: 'app1',
        publicId: 'applicationOnePublicID',
        name: 'App 1',
      },
    },
  },
};

describe('OwnerDetailSidebar', () => {
  let renderComponent, mock;

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    mock.onGet(getOwnerListUrl()).reply(200, {
      ownersMap: {
        ROOT_ORGANIZATION_ID: {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
        },
        applicationOnePublicID: {
          id: 'app1',
          publicId: 'applicationOnePublicID',
          name: 'App 1',
        },
      },
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
    });

    renderComponent = (preloadedState) =>
      render(<OwnerDetailSidebar />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders correct sidebar with correct list open at Organization levels when FirewallOnlyLicense only', async () => {
    const state = {
      productLicense: {
        license: {
          products: ['Firewall'],
        },
      },
      productFeatures: {
        productFeatures: {
          'saas-lifecycle-scm-enabled': true,
        },
      },
      router: {
        currentState: {
          name: 'management.edit.organization.create-category',
          url: '/category',
        },
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
      },
      orgsAndPolicies: {
        applications: {
          applications: APPS,
        },
        organizations: {
          organizations: ORGS,
        },
        root: {
          selectedOwner: {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
          },
        },
        policyMonitoring: {
          isLegacyViolationSupported: false,
          isMonitoringSupported: true,
        },
      },
    };

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent(state);

    expect(await screen.findByText('Policies')).toBeVisible();
    expect(await screen.findByText('Component Labels')).toBeVisible();
    expect(await screen.findByText('License Threat Groups')).toBeVisible();
    expect(screen.getByText('Access')).toBeVisible();
    expect(screen.queryByText('Legacy Violations')).not.toBeInTheDocument();
    expect(screen.queryByText('Application Categories')).not.toBeInTheDocument();
    expect(screen.queryByText('Continuous Monitoring')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
  });

  it('renders correct sidebar with correct list open at Organization levels when not FirewallOnlyLicense only', async () => {
    const state = {
      productLicense: {
        license: {
          products: ['SonatypeCLM', 'Firewall'],
        },
      },
      productFeatures: {
        productFeatures: {
          'saas-lifecycle-scm-enabled': true,
        },
      },
      router: {
        currentState: {
          name: 'management.edit.organization.create-category',
          url: '/category',
        },
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
      },
      orgsAndPolicies: {
        applications: {
          applications: APPS,
        },
        organizations: {
          organizations: ORGS,
        },
        root: {
          selectedOwner: {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
          },
        },
        policyMonitoring: {
          isLegacyViolationSupported: false,
          isMonitoringSupported: true,
        },
      },
    };

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent(state);

    expect(await screen.findByText('Application Categories')).toBeVisible();
    expect(await screen.findByText('Policies')).toBeVisible();
    expect(await screen.findByText('Component Labels')).toBeVisible();
    expect(await screen.findByText('License Threat Groups')).toBeVisible();
    expect(await screen.findByText('Application Evaluator')).toBeVisible();
    expect(await screen.findByText('Continuous Monitoring')).toBeVisible();
    expect(await screen.findByText('Legacy Violations')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Source Control' })).toBeVisible();
    expect(screen.getByText('Legacy Violations').parentElement).toHaveClass('disabled');
  });

  it('renders correct sidebar with correct list open at Organization levels', async () => {
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(await screen.findByText('categoryTest')).toBeVisible();
    expect(await screen.findByText('policyTest')).toBeVisible();
    expect(await screen.findByText('labelTest')).toBeVisible();
    expect(await screen.findByText('licenseThreatGroupsTest')).toBeVisible();
    expect(await screen.findByText('Application Evaluator')).toBeVisible();
    expect(await screen.findByText('Continuous Monitoring')).toBeVisible();
    expect(await screen.findByText('Legacy Violations')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Source Control' })).toBeVisible();
    expect(screen.getByText('Legacy Violations').parentElement).toHaveClass('disabled');
  });

  it('renders correct sidebar with correct list open at Repositories', () => {
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: 'REPOSITORY_CONTAINER_ID',
      ownerType: 'repository_container',
    });
    jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);

    mock
      .onGet(getOwnerDetailsUrl('repository_container', 'REPOSITORY_CONTAINER_ID', true))
      .reply(200, ownerDetailMockData);

    renderComponent(repositoryState);
    expect(screen.getByText('Access')).toBeVisible();

    expect(screen.queryAllByText('Application Categories').length).toBe(0);
    expect(screen.queryAllByText('Policies').length).toBe(1);
    expect(screen.queryAllByText('Component Labels').length).toBe(0);
    expect(screen.queryAllByText('License Threat Groups').length).toBe(0);
    expect(screen.queryAllByRole('link', { name: 'Source Control' }).length).toBe(0);
  });

  it('renders correct sidebar modifying Repository Container', () => {
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: 'REPOSITORY_CONTAINER_ID',
      ownerType: 'repository_container',
    });
    jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);

    mock
      .onGet(getOwnerDetailsUrl('repository_container', 'REPOSITORY_CONTAINER_ID', true))
      .reply(200, ownerDetailMockData);

    renderComponent(repositoryState);
    expect(screen.getByText('Access')).toBeVisible();
    expect(screen.queryAllByText('Policies').length).toBe(1);
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
  });

  it('renders correct sidebar with correct list open at Application level', () => {
    renderComponent(appsLevelState);

    expect(screen.getByText('Application Categories')).toBeVisible();
    expect(screen.getByText('Assign App Categories')).toBeVisible();
    expect(screen.getByText('Policies')).toBeVisible();
    expect(screen.getByText('Component Labels')).toBeVisible();
    expect(screen.getByText('Access')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Source Control' })).toBeVisible();

    expect(screen.queryAllByText('License Threat Groups').length).toBe(0);
  });

  it('does not render the Source Control link when saas-lifecycle-scm-enabled is false', () => {
    renderComponent({
      ...defaultPreloadedState,
      productFeatures: {
        productFeatures: { 'saas-lifecycle-scm-enabled': false },
      },
    });

    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
  });

  it('does not render the Source Control link when saas-lifecycle-scm-enabled is missing', () => {
    renderComponent({
      ...defaultPreloadedState,
      productFeatures: {
        productFeatures: {},
      },
    });

    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
  });

  it('has correct collapse menu behavior', () => {
    renderComponent();

    expect(screen.getAllByRole('group')[0]).toHaveClass('nx-collapsible-items--expanded');
    expect(screen.getAllByRole('group')[1]).not.toHaveClass('nx-collapsible-items--expanded');

    const secondMenuButton = screen.getByRole('button', { name: 'Policies' });
    fireEvent.click(secondMenuButton);

    expect(screen.getAllByRole('group')[0]).toHaveClass('nx-collapsible-items--expanded');
    expect(screen.getAllByRole('group')[1]).toHaveClass('nx-collapsible-items--expanded');

    const firstMenuButton = screen.getByRole('button', { name: 'Application Categories' });
    fireEvent.click(firstMenuButton);

    expect(screen.getAllByRole('group')[0]).not.toHaveClass('nx-collapsible-items--expanded');
    expect(screen.getAllByRole('group')[1]).toHaveClass('nx-collapsible-items--expanded');
  });
});
