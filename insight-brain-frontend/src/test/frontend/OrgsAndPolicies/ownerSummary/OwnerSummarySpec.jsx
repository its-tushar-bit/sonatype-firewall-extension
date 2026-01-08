/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import {
  getOrganizationUrl,
  getApplicationsUrl,
  getDashboardStageUrl,
  getApplicationSummaryUrl,
} from 'MainRoot/util/CLMLocation';
import { render, axiosMockAdapter, within, screen, fireEvent, spyOn } from 'TestRoot/SpecUtil';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as productLicenseSelectors from 'MainRoot/productFeatures/productLicenseSelectors';

describe('OwnerSummary', () => {
  let axiosMock, preloadedState;

  const ownerId = 'be17ea5538de4679ba3a9220734ddbf7';
  const renderComponent = (preloadedState) => render(<OwnerSummary />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('if owner is an organization', () => {
    beforeAll(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
          ownersMap: {},
          policyMonitoring: {
            monitoredStage: {},
          },
        },
        stages: {
          cli: {
            stageTypes: null,
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock.onGet(getOrganizationUrl(ownerId)).reply(200, {
        allowArtifactoryConnectionOverride: true,
        allowLegacyViolationOverride: true,
        allowPolicyViolationGrandfatheringOverride: true,
        allowRepositoryConnectionOverride: true,
        artifactoryConnectionEnabled: null,
        id: ownerId,
        name: 'broadcast',
        nameLowercaseNoWhitespace: 'broadcast',
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        policyViolationGrandfatheringEnabled: null,
        legacyViolationEnabled: null,
        repositoryConnectionEnabled: null,
        contact: {
          displayName: 'Provided Contact Display Name',
        },
      });
    });

    it('renders a loading indicator', () => {
      renderComponent(preloadedState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders an alert with retry if something goes wrong', async () => {
      axiosMock.onGet(getOrganizationUrl(ownerId)).reply(() => Promise.reject('An error occurred loading data.'));

      renderComponent(preloadedState);

      let failureAlert = await screen.findByRole('alert');

      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');

      let retryButton = await within(failureAlert).getByRole('button');

      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(await screen.findByText('Loading…')).toBeVisible();
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });

    it('renders an alert if there is no matching owner', async () => {
      axiosMock.onGet(getOrganizationUrl(ownerId)).reply('some error');

      renderComponent(preloadedState);

      let failureAlert = await screen.findByRole('alert');

      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('some error');
    });

    it('renders proper header with selected contact if contact is provided', async () => {
      renderComponent(preloadedState);

      expect(await screen.findByText('broadcast')).toBeVisible();
      expect(await screen.findByText('Provided Contact Display Name')).toBeVisible();
    });

    it('renders insufficient permissions tree', async () => {
      const state = {
        ...preloadedState,
        orgsAndPolicies: {
          ownerSideNav: { displayedOrganization: { synthetic: true } },
        },
      };
      renderComponent(state);
      const treeMessage = await screen.findByText(
        'View all organizations and applications on which you have permissions. Click on the link for the org or app below to access details.'
      );
      expect(treeMessage).toBeVisible();
      expect(screen.queryByText('broadcast')).not.toBeInTheDocument();
      expect(screen.queryByText('Provided Contact Display Name')).not.toBeInTheDocument();
    });

    it('renders limited firewall access alert when showLimitedFirewallAccessAlert is true and in firewall mode', async () => {
      const state = {
        ...preloadedState,
        router: {
          currentState: {
            name: 'firewall.management.view.organization',
            url: '/firewall/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: { displayedOrganization: { synthetic: false } },
          root: {
            showLimitedFirewallAccessAlert: true,
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
      };
      renderComponent(state);
      expect(
        await screen.findByText(/You have limited access to Repository Firewall based on your current permissions/)
      ).toBeVisible();
      expect(screen.getByText(/Some data or settings may not be visible. Contact your administrator/)).toBeVisible();
      expect(screen.queryByText('broadcast')).not.toBeInTheDocument();
      expect(screen.queryByTestId('owner-summary-action-dropdown-container')).not.toBeInTheDocument();
    });

    it('does NOT render limited firewall access alert when showLimitedFirewallAccessAlert is false in firewall mode', async () => {
      const state = {
        ...preloadedState,
        router: {
          currentState: {
            name: 'firewall.management.view.organization',
            url: '/firewall/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: { displayedOrganization: { synthetic: false } },
          root: {
            showLimitedFirewallAccessAlert: false,
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
      };
      renderComponent(state);
      expect(await screen.findByText('broadcast')).toBeVisible();
      const alertMessage = screen.queryByText(/You have limited access to Repository Firewall/);
      expect(alertMessage).not.toBeInTheDocument();
    });

    it('does NOT render limited firewall access alert when showLimitedFirewallAccessAlert is true but NOT in firewall mode', async () => {
      const state = {
        ...preloadedState,
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: { displayedOrganization: { synthetic: false } },
          root: {
            showLimitedFirewallAccessAlert: true,
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
      };
      renderComponent(state);
      expect(await screen.findByText('broadcast')).toBeVisible();
      // Should NOT show the firewall alert because we're not in firewall mode
      const alertMessage = screen.queryByText(/You have limited access to Repository Firewall/);
      expect(alertMessage).not.toBeInTheDocument();
    });

    it('renders insufficient permissions tree when synthetic is true, even if showLimitedFirewallAccessAlert is also true in firewall mode', async () => {
      const stateWithBothTrue = {
        ...preloadedState,
        router: {
          currentState: {
            name: 'firewall.management.view.organization',
            url: '/firewall/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          ownerSideNav: { displayedOrganization: { synthetic: true } },
          root: {
            showLimitedFirewallAccessAlert: true,
            selectedOwner: {
              id: ownerId,
              name: 'broadcast',
            },
          },
        },
      };
      renderComponent(stateWithBothTrue);
      const treeMessage = await screen.findByText(
        'View all organizations and applications on which you have permissions. Click on the link for the org or app below to access details.'
      );
      expect(treeMessage).toBeVisible();
      expect(screen.queryByTestId('owner-summary-action-dropdown-container')).not.toBeInTheDocument();
      // Should show tree, not the firewall alert
      expect(screen.queryByText(/You have limited access to Repository Firewall/)).not.toBeInTheDocument();
    });

    it("does NOT render an SBOM tile because it's on the organization page", async () => {
      const stateSBOMtrue = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
          },
        },
      };
      renderComponent(stateSBOMtrue);

      expect(await screen.queryByRole('heading', { name: 'SBOMs' })).not.toBeInTheDocument();
      expect(await screen.queryByRole('heading', { name: 'Access' })).not.toBeInTheDocument();
      expect(await screen.queryByRole('owner-summary-action-dropdown-container')).not.toBeInTheDocument();
    });

    it('hides non-SBOM pills when in SBOM Manager and sbom-continuous-monitoring-ui is enabled', async () => {
      const stateSBOMtrue = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'policy-monitoring': true,
            'sbom-continuous-monitoring-ui': true,
            'sbom-policies': true,
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.management.view.organization',
            url: '/sbomManager/management/view/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
      };
      renderComponent(stateSBOMtrue);

      expect(await screen.findByTestId('policies-tile')).toBeVisible();
      expect(await screen.findByRole('heading', { name: 'Access' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Access' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continuous monitoring' })).toBeVisible();

      expect(screen.queryByRole('heading', { name: 'SBOMs' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Application Categories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Legacy Violations' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Proprietary Component Configuration' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Component Labels' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'License Threat Groups' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Source Control' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'InnerSource Repositories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'SBOMs' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Application Categories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Legacy Violations' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Proprietary Components' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Component Labels' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'License Threat Groups' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Source Control' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'InnerSource Repositories' })).not.toBeInTheDocument();
      expect(screen.queryByTestId('owner-summary-action-dropdown-container')).toBeInTheDocument();
    });

    it('hides Continuous Monitoring tile when in SBOM Manager and sbom-continuous-monitoring-ui is disabled', async () => {
      const stateSbomTrueCmUiFalse = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'policy-monitoring': true,
            'sbom-continuous-monitoring-ui': false,
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.management.view.organization',
            url: '/sbomManager/management/view/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
      };
      renderComponent(stateSbomTrueCmUiFalse);

      expect(screen.queryByRole('heading', { name: 'Continuous monitoring' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Continuous monitoring' })).not.toBeInTheDocument();
    });

    it('hides Polices Tile when sbom-policies is disabled', async () => {
      const state = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'sbom-policies': false,
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.management.view.organization',
            url: '/sbomManager/management/view/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
      };
      renderComponent(state);

      expect(await screen.queryByTestId('policies-tile')).not.toBeInTheDocument();
    });

    it('does render the Actions button if SBOM Manager disabled', async () => {
      preloadedState.orgsAndPolicies.ownerSideNav = { displayedOrganization: { synthetic: false } };
      renderComponent(preloadedState);

      expect(await screen.queryByRole('heading', { name: 'SBOMs' })).not.toBeInTheDocument();
      expect(await screen.queryByRole('heading', { name: 'Access' })).not.toBeInTheDocument();
      expect(await screen.queryByRole('heading', { name: 'Continuous monitoring' })).not.toBeInTheDocument();
      expect(await screen.findByTestId('owner-summary-action-dropdown-container')).toBeInTheDocument();
    });
  });

  describe('if owner is an application', () => {
    const ownerId = 'be17ea5538de4679ba3a9220734ddbf7';

    it('renders proper header with selected contact and publicId', async () => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'a-aws-4-lll_app_filter',
          },
        },
        orgsAndPolicies: {
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
        },
      };

      axiosMock.onGet(getApplicationsUrl()).reply(200, [
        {
          id: ownerId,
          name: 'a-aws 4-lll app filter',
          organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
          organizationName: '4-level-org',
          publicId: 'a-aws-4-lll_app_filter',
          contact: {
            displayName: 'Provided Contact Display Name',
          },
        },
        {
          id: 'fd6c2140f61c460d9d900d8f72620475',
          name: 'www',
          organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
          organizationName: '4-level-org',
          publicId: 'www',
          contact: null,
        },
      ]);
      axiosMock.onGet(getDashboardStageUrl()).reply(200, []);
      axiosMock.onGet(getApplicationSummaryUrl('a-aws-4-lll_app_filter')).reply(200, {
        contact: {
          displayName: 'Provided Contact Display Name',
        },
        id: ownerId,
        name: 'a-aws 4-lll app filter',
        organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
        organizationName: '4-level-org',
        publicId: 'a-aws-4-lll_app_filter',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      });

      renderComponent(preloadedState);

      expect(await screen.findByText('a-aws 4-lll app filter')).toBeVisible();
      expect(await screen.findByText('(a-aws-4-lll_app_filter)')).toBeVisible();
      expect(await screen.findByText('Provided Contact Display Name')).toBeVisible();
    });

    it('does NOT render limited firewall access alert when showLimitedFirewallAccessAlert is true (firewall mode only for organizations)', async () => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'a-aws-4-lll_app_filter',
          },
        },
        orgsAndPolicies: {
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
          root: {
            showLimitedFirewallAccessAlert: true,
            selectedOwner: {
              id: ownerId,
              publicId: 'a-aws-4-lll_app_filter',
              name: 'a-aws 4-lll app filter',
            },
          },
        },
      };

      axiosMock.onGet(getApplicationsUrl()).reply(200, [
        {
          id: ownerId,
          name: 'a-aws 4-lll app filter',
          organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
          organizationName: '4-level-org',
          publicId: 'a-aws-4-lll_app_filter',
        },
      ]);
      axiosMock.onGet(getDashboardStageUrl()).reply(200, []);
      axiosMock.onGet(getApplicationSummaryUrl('a-aws-4-lll_app_filter')).reply(200, {
        id: ownerId,
        name: 'a-aws 4-lll app filter',
        organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
        organizationName: '4-level-org',
        publicId: 'a-aws-4-lll_app_filter',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      });

      renderComponent(preloadedState);

      expect(await screen.findByText('a-aws 4-lll app filter')).toBeVisible();
      // Should NOT show the firewall alert when viewing an application (only for organizations/firewall mode)
      const alertMessage = screen.queryByText(/You have limited access to Repository Firewall/);
      expect(alertMessage).not.toBeInTheDocument();
    });
  });

  describe('and selectIsSbomManagerEnabled is', () => {
    beforeEach(() => {
      const ownerId = '12345';
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'sbom_test',
          },
        },
        orgsAndPolicies: {
          labels: {
            applicableLabels: [],
            inheritedLabelsOpen: {},
            loadError: null,
            loading: false,
            ownerId: ownerId,
          },
          ownerSummary: {
            hasEditIqPermission: false,
          },
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
          root: {
            selectedOwner: {
              name: 'Owner name',
            },
          },
        },
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'sbom-policies': true,
          },
        },
      };
      axiosMock.onGet(getApplicationSummaryUrl('sbom_test')).reply(200, {
        contact: {
          displayName: 'Provided Contact Display Name',
        },
        id: ownerId,
        name: 'sbom_test',
        organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
        organizationName: '4-level-org',
        publicId: 'sbom_test',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      });
    });

    it('true, then SBOMs, Access and Continuous Monitoring tiles are visible and no other tiles are', async () => {
      const stateSBOMtrue = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'policy-monitoring': true,
            'sbom-continuous-monitoring-ui': true,
            'sbom-policies': true,
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'sbom_test',
          },
        },
      };
      renderComponent(stateSBOMtrue);

      expect(await screen.findByRole('heading', { name: 'SBOMs' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'SBOMs' })).toBeVisible();

      expect(await screen.findByTestId('policies-tile')).toBeVisible();

      expect(screen.getByRole('heading', { name: 'Access' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Access' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'SBOMs' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continuous monitoring' })).toBeVisible();

      expect(screen.queryByRole('heading', { name: 'Application Categories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Legacy Violations' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Proprietary Component Configuration' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Component Labels' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'License Threat Groups' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Source Control' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'InnerSource Repositories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Application Categories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Legacy Violations' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Proprietary Components' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Component Labels' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'License Threat Groups' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Source Control' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'InnerSource Repositories' })).not.toBeInTheDocument();
      expect(screen.queryByRole('heading', { name: 'Component Labels' })).not.toBeInTheDocument();

      expect(screen.queryByTestId('owner-summary-action-dropdown-container')).toBeInTheDocument();
    });

    it('true, but does not render Continuous Monitoring tile when sbom-continuous-monitoring-ui is disabled', async () => {
      const stateSbomCmUiDisabled = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {
            'sbom-manager': true,
            'policy-monitoring': true,
            'sbom-continuous-monitoring-ui': false,
          },
        },
        router: {
          currentState: {
            name: 'sbomManager.management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'sbom_test',
          },
        },
      };
      renderComponent(stateSbomCmUiDisabled);

      expect(screen.queryByRole('heading', { name: 'Continuous monitoring' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Continuous monitoring' })).not.toBeInTheDocument();
    });

    it('false, and shows error when the SBOM Manager license is disabled', async () => {
      const stateSBOMfalse = {
        ...preloadedState,
        productFeatures: {
          productFeatures: {},
        },
        router: {
          currentState: { name: 'sbomManager.dashboard' },
        },
      };
      renderComponent(stateSBOMfalse);

      const errorMessage = await screen.findByText(
        'An error occurred loading data. The SBOM Manager license feature is not enabled.'
      );
      expect(errorMessage).toBeVisible();
    });
  });

  it('stays in the loading state when there is a route change and OwnerSummary is already loaded', () => {
    preloadedState = {
      router: {
        currentState: {
          name: 'management.view',
          url: '/view',
          data: {
            title: 'Management',
          },
        },
        currentParams: {
          '#': null,
        },
      },
      orgsAndPolicies: {
        ownerSummary: {
          loading: false,
        },
      },
    };

    renderComponent(preloadedState);
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  describe('Public Data Sources', () => {
    const preloadedState = {
      router: {
        currentState: {
          name: 'management.view.application',
          url: '/application/{applicationPublicId}',
          data: {
            title: 'Application Management',
            viewportSized: true,
          },
        },
        currentParams: {
          applicationPublicId: 'sbom_test',
        },
      },
      orgsAndPolicies: {
        labels: {
          applicableLabels: [],
          inheritedLabelsOpen: {},
          loadError: null,
          loading: false,
          ownerId: ownerId,
        },
        ownerSummary: {
          hasEditIqPermission: false,
        },
        sourceControl: {
          data: {
            repositoryUrl: null,
            provider: {
              value: null,
              parentValue: 'github',
            },
            token: {
              value: null,
            },
          },
        },
        root: {
          selectedOwner: {
            name: 'Owner name',
          },
        },
      },
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
          'sbom-policies': true,
        },
      },
    };

    beforeEach(() => {
      axiosMock.onGet(getApplicationSummaryUrl('sbom_test')).reply(200, {
        contact: {
          displayName: 'Provided Contact Display Name',
        },
        id: ownerId,
        name: 'sbom_test',
        organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
        organizationName: '4-level-org',
        publicId: 'sbom_test',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      });
    });

    it('renders pill and tile', async () => {
      spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense', () => false);
      spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported', () => true);
      renderComponent(preloadedState);

      expect(await screen.findByTestId('owner-pill-public-data-sources-button')).toBeVisible();
      expect(await screen.findByTestId('owner-pill-public-data-sources')).toBeVisible();
    });

    it('will not render pill and tile because when Sbom Manager is the only product in license', async () => {
      spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense', () => true);
      spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported', () => true);
      renderComponent(preloadedState);

      expect(await screen.queryByTestId('owner-pill-public-data-sources-button')).not.toBeInTheDocument();
      expect(await screen.queryByTestId('owner-pill-public-data-sources')).not.toBeInTheDocument();
    });

    it('will not render pill and tile when cpe matching feature is not enabled', async () => {
      spyOn(productLicenseSelectors, 'selectIsSbomManagerOnlyLicense', () => true);
      spyOn(productFeaturesSelectors, 'selectIsCpeMatchingSupported', () => false);
      renderComponent(preloadedState);

      expect(await screen.queryByTestId('owner-pill-public-data-sources-button')).not.toBeInTheDocument();
      expect(await screen.queryByTestId('owner-pill-public-data-sources')).not.toBeInTheDocument();
    });
  });
});
