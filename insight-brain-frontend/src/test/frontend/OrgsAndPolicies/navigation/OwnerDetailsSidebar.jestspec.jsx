/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import OwnerDetailSidebar from 'MainRoot/OrgsAndPolicies/navigation/OwnerDetailSidebar';
import { fireEvent, render, screen, setupPortalContainer } from 'TestRoot/SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getOwnerDetailsUrl, getOwnerListUrl } from 'MainRoot/util/CLMLocation';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as productLicenseSelectors from 'MainRoot/productFeatures/productLicenseSelectors';

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
      'policy-grandfathering': true,
      'orgs-and-apps': true,
      'policy-monitoring': true,
      'source-control': true,
      'sbom-continuous-monitoring-ui': true,
      notifications: true,
    },
  },
  orgsAndPolicies: {
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

const sbomRootOrgLevelState = {
  ...defaultPreloadedState,
  router: {
    currentState: {
      name: 'sbomManager.management.edit.organization.access',
      url: '/access',
    },
    currentParams: {
      organizationId: 'ROOT_ORGANIZATION_ID',
    },
  },
};

const sbomOrgLevelState = {
  ...defaultPreloadedState,
  router: {
    currentState: {
      name: 'sbomManager.management.edit.organization.access',
      url: '/access',
    },
    currentParams: {
      organizationId: 'org1',
    },
  },
};

const sbomAppsLevelState = {
  ...defaultPreloadedState,
  router: {
    currentState: {
      name: 'sbomManager.management.edit.application.access',
      url: '/access',
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
    setupPortalContainer();
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

  it('renders correct sidebar with correct list open at Organization levels when features are disabled', async () => {
    const state = {
      ...defaultPreloadedState,
      productFeatures: {},
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

  it('renders correct sidebar with correct list open at Organization levels', async () => {
    const user = userEvent.setup();
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    await user.click(await screen.findByRole('button', { name: 'Application Categories' }));
    await user.click(await screen.findByRole('button', { name: 'Policies' }));
    await user.click(await screen.findByRole('button', { name: 'Component Labels' }));
    await user.click(await screen.findByRole('button', { name: 'License Threat Groups' }));
    await user.click(await screen.findByRole('button', { name: 'Application Categories' }));
    await user.click(await screen.findByRole('button', { name: 'Access' }));

    expect(await screen.findByText('categoryTest')).toBeVisible();
    expect(await screen.findByText('policyTest')).toBeVisible();
    expect(await screen.findByText('labelTest')).toBeVisible();
    expect(await screen.findByText('licenseThreatGroupsTest')).toBeVisible();
    expect(await screen.findByText('Application Evaluator')).toBeVisible();
    expect(await screen.findByText('Continuous Monitoring')).toBeVisible();
    expect(await screen.findByText('Legacy Violations')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Source Control' })).toBeVisible();
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

  it('renders correct sidebar with correct list open at Repository manager', () => {
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232',
      ownerType: 'repository_manager',
    });
    jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);

    mock
      .onGet(getOwnerDetailsUrl('repository_manager', 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232', false))
      .reply(200, ownerDetailMockData);

    renderComponent(repositoryState);
    expect(screen.getByText('Access')).toBeVisible();
    expect(screen.queryAllByText('Policies').length).toBe(1);
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
  });

  it('renders correct sidebar with correct list open at Repository level', () => {
    jest.spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties').mockReturnValue({
      ownerId: '773e6dee1f0a45ada355ffe534a70533',
      ownerType: 'repository',
    });
    jest.spyOn(routerSelectors, 'selectIsRepositoriesRelated').mockReturnValue(true);

    mock
      .onGet(getOwnerDetailsUrl('repository', '773e6dee1f0a45ada355ffe534a70533', false))
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

  it('renders correct sidebar with correct list open at Root Organization level when SBOM Manager', async () => {
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent(sbomRootOrgLevelState);

    expect(await screen.findByText('Access')).toBeVisible();

    expect(screen.queryByText('Policies')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'New Policy' })).not.toBeInTheDocument();
    expect(screen.queryByText('Component Labels')).not.toBeInTheDocument();
    expect(screen.queryByText('License Threat Groups')).not.toBeInTheDocument();
    expect(screen.queryByText('Legacy Violations')).not.toBeInTheDocument();
    expect(screen.queryByText('Application Categories')).not.toBeInTheDocument();
    expect(screen.queryByText('Continuous Monitoring')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
    expect(screen.queryAllByText('License Threat Groups').length).toBe(0);
  });

  it('renders correct sidebar with correct list open at child organization level when SBOM Manager', async () => {
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent(sbomOrgLevelState);

    expect(await screen.findByText('Access')).toBeVisible();
    expect(await screen.findByText('Continuous Monitoring')).toBeVisible();

    expect(screen.queryByText('Policies')).not.toBeInTheDocument();
    expect(screen.queryByText('Component Labels')).not.toBeInTheDocument();
    expect(screen.queryByText('License Threat Groups')).not.toBeInTheDocument();
    expect(screen.queryByText('Legacy Violations')).not.toBeInTheDocument();
    expect(screen.queryByText('Application Categories')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
    expect(screen.queryAllByText('License Threat Groups').length).toBe(0);
  });

  it('does not render the Continuous Monitoring item at Organization levels when SBOM Manager and sbom-continuous-monitoring-ui is disabled', async () => {
    renderComponent({
      ...sbomOrgLevelState,
      productFeatures: {
        productFeatures: { 'sbom-continuous-monitoring-ui': false },
      },
    });

    expect(screen.queryByText('Continuous Monitoring')).not.toBeInTheDocument();
  });

  it('renders correct sidebar with correct list open at Application level when SBOM Manager', async () => {
    renderComponent(sbomAppsLevelState);

    expect(await screen.findByText('Access')).toBeVisible();
    expect(await screen.findByText('Continuous Monitoring')).toBeVisible();

    expect(screen.queryByText('Policies')).not.toBeInTheDocument();
    expect(screen.queryByText('Component Labels')).not.toBeInTheDocument();
    expect(screen.queryByText('License Threat Groups')).not.toBeInTheDocument();
    expect(screen.queryByText('Legacy Violations')).not.toBeInTheDocument();
    expect(screen.queryByText('Application Categories')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Source Control' })).not.toBeInTheDocument();
    expect(screen.queryAllByText('License Threat Groups').length).toBe(0);
  });

  it('does not render the Continuous Monitoring item at Application level when SBOM Manager and sbom-continuous-monitoring-ui is disabled', async () => {
    renderComponent({
      ...sbomAppsLevelState,
      productFeatures: {
        productFeatures: { 'sbom-continuous-monitoring-ui': false },
      },
    });

    expect(screen.queryByText('Continuous Monitoring')).not.toBeInTheDocument();
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

  it('does not render auto-waivers when developerDashboardEnabled is false', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsDeveloperDashboardEnabled').mockReturnValue(false);

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('does not render auto-waivers when isSbomManager is true', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('should not render auto-waivers when developerDashboardEnabled is true', () => {
    renderComponent({
      ...appsLevelState,
      productFeatures: {
        productFeatures: { 'developer-dashboard': true },
      },
    });

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('should not render auto-waivers when isAutoWaiversEnabled is false', () => {
    renderComponent({
      ...appsLevelState,
      productFeatures: {
        productFeatures: { 'auto-waivers': false },
      },
    });

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('should not render auto-waivers when isAutoWaiversEnabled is true, isDeveloperDashboard is true, and sbomManager is true', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);

    renderComponent({
      ...appsLevelState,
      productFeatures: {
        productFeatures: { 'auto-waivers': true, 'developer-dashboard': true },
      },
    });

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('should render auto-waivers when isAutoWaiversEnabled is true, isDeveloperDashboard is true, and sbomManager is false', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

    renderComponent({
      ...appsLevelState,
      productFeatures: {
        productFeatures: { 'auto-waivers': true, 'developer-dashboard': true },
      },
    });

    const link = screen.getByRole('link', { name: 'Auto-Waivers' });
    expect(link).toBeInTheDocument();
    const href = link.getAttribute('href');
    expect(href).toContain('/autowaivers');
  });

  it('should render public data sources when selectIsCpeMatchingSupported is true', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(true);
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.getByText('Public Data Sources')).toBeInTheDocument();
  });

  it('should not render public data sources when isPublicDataSourcesEnabled is false', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(false);
    jest.spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense').mockReturnValue(false);
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.queryByText('Public Data Sources')).not.toBeInTheDocument();
  });

  it('should not render public data sources when is not a multilicense SBOM Manager product', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    jest.spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense').mockReturnValue(true);
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(true);

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.queryByText('Public Data Sources')).not.toBeInTheDocument();
  });

  it('should render public data sources when is a multilicense SBOM Manager product and CPE matching is supported', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    jest.spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense').mockReturnValue(false);
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(true);

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.getByText('Public Data Sources')).toBeInTheDocument();
  });

  it('should not render auto-waivers when in Firewall view', () => {
    jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

    renderComponent({
      ...appsLevelState,
      productFeatures: {
        productFeatures: { 'auto-waivers': true, 'developer-dashboard': true },
      },
    });

    expect(screen.queryByText('Auto-Waivers')).not.toBeInTheDocument();
  });

  it('should not render public data sources when in Firewall view', () => {
    jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(true);
    jest.spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported').mockReturnValue(true);
    jest.spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense').mockReturnValue(false);

    mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

    renderComponent();

    expect(screen.queryByText('Public Data Sources')).not.toBeInTheDocument();
  });

  describe('Pro Tier Gating', () => {
    const getProState = () => ({
      ...defaultPreloadedState,
      productFeatures: {
        productFeatures: {
          ...defaultPreloadedState.productFeatures.productFeatures,
          'custom-policies': false,
          'custom-component-labels': false,
          'custom-application-categories': false,
          'custom-license-threat-groups': false,
          'auto-waiver-management': false,
        },
      },
      productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
    });

    it('renders sidebar sections with lock icons visible for Pro tier user', async () => {
      mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

      renderComponent(getProState());

      expect(await screen.findByText('Policies')).toBeVisible();
      expect(screen.getByText('Component Labels')).toBeVisible();
      expect(screen.getByText('License Threat Groups')).toBeVisible();

      // Lock icons should be present (iq-sidebar-crown class)
      const lockIcons = document.querySelectorAll('.iq-sidebar-crown');
      expect(lockIcons.length).toBeGreaterThan(0);
    });

    it('renders sidebar with Auto-Waivers lock icon when feature is absent', async () => {
      mock.onGet(getOwnerDetailsUrl('organization', 'ROOT_ORGANIZATION_ID', false)).reply(200, ownerDetailMockData);

      const proState = {
        ...getProState(),
        productFeatures: {
          productFeatures: {
            ...defaultPreloadedState.productFeatures.productFeatures,
            'auto-waiver-management': false,
            autoWaivers: true,
          },
        },
      };

      renderComponent(proState);

      expect(await screen.findByText('Policies')).toBeVisible();
    });
  });
});
