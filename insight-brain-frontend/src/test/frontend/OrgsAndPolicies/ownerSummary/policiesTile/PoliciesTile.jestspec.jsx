/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, axiosMockAdapter, within, screen, fireEvent } from 'TestRoot/SpecUtil';

import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as ownerSideNavSelectors from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { getActionStageUrl, getApplicablePolicies } from 'MainRoot/util/CLMLocation';
import {
  applicationPoliciesByOwnerPayload,
  rootOrganizationPoliciesByOwnerPayload,
  rootOrganizationNoPoliciesByOwnerPayload,
  organizationPoliciesByOwnerPayload,
  actionStagesPayload,
  repositoryContainerByOwnerPayload,
  repositoryManagerByOwnerPayload,
  repositoryByOwnerPayload,
} from './policiesTileTestData';
import { getNumberOfTables } from '../utils/tileAndTableTestingUtils';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

describe('PoliciesTile', () => {
  let axiosMock, goToCreatePolicySpy, preloadedState, ownerName, ownerId, ownerType, numberOfTables;
  let firewallSupported = true;
  let enforcementSupported = true;
  const renderComponent = (preloadedState) => render(<PoliciesTile />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    goToCreatePolicySpy = jest.spyOn(policyActions, 'goToCreatePolicy');
    jest.spyOn(policyActions, 'loadPolicyTile');
  });

  describe('Loading and retry logic', () => {
    beforeAll(() => {
      ownerName = organizationPoliciesByOwnerPayload.ownerName;
      ownerId = organizationPoliciesByOwnerPayload.ownerId;
      ownerType = organizationPoliciesByOwnerPayload.ownerType;

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
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    it('renders a loading legend', () => {
      renderComponent(preloadedState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders an alert with retry if something goes wrong with policiesByOwner request', async () => {
      axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(500, 'An error occurred loading data.');
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
      let failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
      let retryButton = await within(failureAlert).getByRole('button');
      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(screen.getByText('Loading…')).toBeVisible();
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });

    it('renders an alert with retry if something goes wrong with action stages request', async () => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(500);

      renderComponent(preloadedState);
      let failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data. Error 500');
      let retryButton = await within(failureAlert).getByRole('button');
      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(screen.getByText('Loading…')).toBeVisible();
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });
  });

  describe('Owner is Root Organization', () => {
    beforeAll(() => {
      ownerName = rootOrganizationPoliciesByOwnerPayload.ownerName;
      ownerId = rootOrganizationPoliciesByOwnerPayload.ownerId;
      ownerType = rootOrganizationPoliciesByOwnerPayload.ownerType;
      numberOfTables = getNumberOfTables('policies', rootOrganizationPoliciesByOwnerPayload.policiesByOwner);

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
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Tile Content', () => {
      it('renders correct amount of tables', async () => {
        const tables = await screen.findAllByRole('table');
        expect(tables.length).toBe(numberOfTables);
      });

      it('render all correct titles', async () => {
        const ownersWithPolicies = rootOrganizationNoPoliciesByOwnerPayload.policiesByOwner.filter(
          (owner) => !isNilOrEmpty(owner.policies)
        );
        for (const owner of ownersWithPolicies) {
          const index = ownersWithPolicies.indexOf(owner);
          if (index === 0) {
            expect(await screen.findByText('Local')).toBeVisible();
          } else {
            let title = `Inherited from ${owner.ownerName}`;
            expect(await screen.findByText(title)).toBeVisible();
          }
        }
      });
    });
  });

  describe('Owner is Root Organization with no policies', () => {
    beforeAll(() => {
      ownerName = rootOrganizationNoPoliciesByOwnerPayload.ownerName;
      ownerId = rootOrganizationNoPoliciesByOwnerPayload.ownerId;
      ownerType = rootOrganizationNoPoliciesByOwnerPayload.ownerType;

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
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: rootOrganizationNoPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Tile Content', () => {
      it('renders correct empty message text', async () => {
        const emptyMessage = within(await screen.findByRole('list')).getAllByRole('listitem')[0];
        expect(emptyMessage).toHaveTextContent('No local policies defined');
      });

      it('render all correct titles', async () => {
        const ownersWithPolicies = rootOrganizationNoPoliciesByOwnerPayload.policiesByOwner.filter(
          (owner) => !isNilOrEmpty(owner.policies)
        );
        for (const owner of ownersWithPolicies) {
          const index = ownersWithPolicies.indexOf(owner);
          if (index === 0) {
            expect(await screen.findByText('Local')).toBeVisible();
          } else {
            let title = `Inherited from ${owner.ownerName}`;
            expect(await screen.findByText(title)).toBeVisible();
          }
        }
      });
    });
  });

  describe('Owner is an application with inherited policies', () => {
    beforeAll(() => {
      ownerName = applicationPoliciesByOwnerPayload.ownerName;
      ownerId = applicationPoliciesByOwnerPayload.ownerId;
      ownerType = applicationPoliciesByOwnerPayload.ownerType;
      numberOfTables = applicationPoliciesByOwnerPayload.policiesByOwner.length;

      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: ownerId,
          },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: applicationPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Policies table Content', () => {
      it('renders correct amount of tbodys', async () => {
        let rowgroups = await screen.findAllByRole('rowgroup');

        // filter rowgroups by collapsible rows
        rowgroups = rowgroups.filter((row) => {
          return row.children[0].className.includes('iq-collapsible-row');
        });

        // check that the total of collapsible rows are equal to the total of policies by owner
        expect(rowgroups.length).toBe(numberOfTables);
      });

      it('Correctly renders all collapsible headers rows titles', async () => {
        const ownersWithPolicies = applicationPoliciesByOwnerPayload.policiesByOwner;
        let rowgroups = await screen.findAllByRole('rowgroup');

        // filter rowgroups by collapsible rows
        rowgroups = rowgroups.filter((row) => {
          return row.children[0].className.includes('iq-collapsible-row');
        });

        rowgroups.forEach((row) => {
          const collapsibleHeader = within(row).getByRole('heading', { level: 3 });
          const index = rowgroups.indexOf(row);
          const policy = ownersWithPolicies[index];
          const title = !policy.inherited ? `Local to ${policy.ownerName}` : `Inherited from ${policy.ownerName}`;

          expect(collapsibleHeader).toHaveTextContent(title);
        });
      });
    });
  });

  describe('Owner is Repository Container with inherited policies', () => {
    beforeAll(() => {
      ownerName = repositoryContainerByOwnerPayload.ownerName;
      ownerId = repositoryContainerByOwnerPayload.ownerId;
      ownerType = repositoryContainerByOwnerPayload.ownerType;

      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.repository_container',
            url: '/repository_container/{repositoryContainerId}',
            data: {
              title: 'Repository Managers Management',
              viewportSized: true,
            },
          },
          currentParams: {
            repositoryContainerId: ownerId,
          },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: repositoryContainerByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Tile Content', () => {
      it('render all correct titles', async () => {
        const ownersWithPolicies = repositoryContainerByOwnerPayload.policiesByOwner.filter(
          (owner) => !isNilOrEmpty(owner.policies)
        );
        for (const owner of ownersWithPolicies) {
          const index = ownersWithPolicies.indexOf(owner);
          if (index === 0) {
            expect(await screen.findByText(`Local to ${owner.ownerName}`)).toBeVisible();
          } else {
            const title = `Inherited from ${owner.ownerName}`;
            expect(await screen.findByText(title)).toBeVisible();
          }
        }
      });
    });
  });

  describe('Owner is Repository Manager with inherited policies', () => {
    beforeAll(() => {
      ownerName = repositoryManagerByOwnerPayload.ownerName;
      ownerId = repositoryManagerByOwnerPayload.ownerId;
      ownerType = repositoryManagerByOwnerPayload.ownerType;

      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.repository_manager',
            url: '/repository_manager/{repositoryManagerId}',
            data: {
              title: 'Repository manager Management',
              viewportSized: true,
            },
          },
          currentParams: {
            repositoryManagerId: ownerId,
          },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: repositoryManagerByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Tile Content', () => {
      it('render all correct titles', async () => {
        const ownersWithPolicies = repositoryManagerByOwnerPayload.policiesByOwner.filter(
          (owner) => !isNilOrEmpty(owner.policies)
        );
        for (const owner of ownersWithPolicies) {
          const index = ownersWithPolicies.indexOf(owner);
          if (index === 0) {
            expect(await screen.findByText(`Local to ${owner.ownerName}`)).toBeVisible();
          } else {
            const title = `Inherited from ${owner.ownerName}`;
            expect(await screen.findByText(title)).toBeVisible();
          }
        }
      });
    });
  });

  describe('Owner is Repository with inherited policies', () => {
    beforeAll(() => {
      ownerName = repositoryByOwnerPayload.ownerName;
      ownerId = repositoryByOwnerPayload.ownerId;
      ownerType = repositoryByOwnerPayload.ownerType;

      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.repository',
            url: '/repository/{repositoryId}',
            data: {
              title: 'Repository Management',
              viewportSized: true,
            },
          },
          currentParams: {
            repositoryId: ownerId,
          },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: repositoryByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);

      renderComponent(preloadedState);
    });

    describe('Tile Header', () => {
      it('renders header with the correct title', async () => {
        expect(await screen.findByText('Policies')).toBeVisible();
      });

      it('Add Policy button is visible and navigates to policy create page', async () => {
        const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
        expect(addButton).toBeVisible();
        fireEvent.click(addButton);
        expect(goToCreatePolicySpy).toHaveBeenCalled();
      });
    });

    describe('Tile Content', () => {
      it('render all correct titles', async () => {
        const ownersWithPolicies = repositoryByOwnerPayload.policiesByOwner.filter(
          (owner) => !isNilOrEmpty(owner.policies)
        );
        for (const owner of ownersWithPolicies) {
          const index = ownersWithPolicies.indexOf(owner);
          if (index === 0) {
            expect(await screen.findByText(`Local to ${owner.ownerName}`)).toBeVisible();
          } else {
            const title = `Inherited from ${owner.ownerName}`;
            expect(await screen.findByText(title)).toBeVisible();
          }
        }
      });
    });
  });

  describe('Stage Columns', () => {
    const actionStagesWithCompliancePayload = [
      { stageTypeId: 'proxy', stageName: 'Proxy' },
      { stageTypeId: 'develop', stageName: 'Develop' },
      { stageTypeId: 'source', stageName: 'Source' },
      { stageTypeId: 'build', stageName: 'Build' },
      { stageTypeId: 'stage-release', stageName: 'Stage Release' },
      { stageTypeId: 'release', stageName: 'Release' },
      { stageTypeId: 'operate', stageName: 'Operate' },
      { stageTypeId: 'compliance', stageName: 'Compliance' },
    ];

    describe('Non SBOM Manager', () => {
      it('renders the correct table headers (without "Compliance")', async () => {
        ownerName = rootOrganizationPoliciesByOwnerPayload.ownerName;
        ownerId = rootOrganizationPoliciesByOwnerPayload.ownerId;
        ownerType = rootOrganizationPoliciesByOwnerPayload.ownerType;

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
          productFeatures: {
            loading: false,
            loadError: null,
            productFeatures: {
              firewall: firewallSupported,
              enforcement: enforcementSupported,
              'custom-policies': true,
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: ownerId,
                name: ownerName,
                legacyViolationEnabled: null,
                allowLegacyViolationOverride: true,
                repositoryConnectionEnabled: null,
                allowRepositoryConnectionOverride: true,
                artifactoryConnectionEnabled: null,
                allowArtifactoryConnectionOverride: true,
              },
            },
            stages: {
              action: {
                loading: false,
                error: null,
                stageTypes: null,
              },
            },
          },
        };
        jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(false);

        axiosMock
          .onGet(getApplicablePolicies(ownerType, ownerId))
          .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
        axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesWithCompliancePayload);

        renderComponent(preloadedState);

        expect(await screen.findByText('Policies')).toBeVisible();

        const headers = screen.getAllByRole('columnheader');

        expect(headers.length).toBe(10);
        expect(headers[1]).toHaveTextContent('Name');
        expect(headers[2]).toHaveTextContent('Proxy');
        expect(headers[3]).toHaveTextContent('Develop');
        expect(headers[4]).toHaveTextContent('Source');
        expect(headers[5]).toHaveTextContent('Build');
        expect(headers[6]).toHaveTextContent('Stage');
        expect(headers[7]).toHaveTextContent('Release');
        expect(headers[8]).toHaveTextContent('Operate');
        expect(headers[9]).toHaveTextContent('Select Row');
      });
    });

    describe('SBOM Manager', () => {
      it('renders the correct header title (without stages)', async () => {
        ownerName = rootOrganizationPoliciesByOwnerPayload.ownerName;
        ownerId = rootOrganizationPoliciesByOwnerPayload.ownerId;
        ownerType = rootOrganizationPoliciesByOwnerPayload.ownerType;

        preloadedState = {
          router: {
            currentState: {
              name: 'sbomManager.management.view.organization',
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
          productFeatures: {
            loading: false,
            loadError: null,
            productFeatures: {
              firewall: firewallSupported,
              enforcement: enforcementSupported,
              'custom-policies': true,
            },
          },
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: ownerId,
                name: ownerName,
                legacyViolationEnabled: null,
                allowLegacyViolationOverride: true,
                repositoryConnectionEnabled: null,
                allowRepositoryConnectionOverride: true,
                artifactoryConnectionEnabled: null,
                allowArtifactoryConnectionOverride: true,
              },
            },
            stages: {
              action: {
                loading: false,
                error: null,
                stageTypes: null,
              },
            },
          },
        };
        jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);

        axiosMock
          .onGet(getApplicablePolicies(ownerType, ownerId))
          .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
        axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesWithCompliancePayload);

        renderComponent(preloadedState);

        expect(await screen.findByText('Policies')).toBeVisible();

        const headers = screen.getAllByRole('columnheader');

        expect(headers.length).toBe(3);
        expect(headers[1]).toHaveTextContent('Name');
        expect(headers[2]).toHaveTextContent('Select Row');
      });
    });
  });

  describe('SBOM Manager', () => {
    beforeAll(() => {
      ownerName = rootOrganizationNoPoliciesByOwnerPayload.ownerName;
      ownerId = rootOrganizationNoPoliciesByOwnerPayload.ownerId;
      ownerType = rootOrganizationNoPoliciesByOwnerPayload.ownerType;
      preloadedState = {
        router: {
          currentState: {
            name: 'sbomManager.management.view.organization',
            url: '/sbomManager/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            'sbom-manager': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };

      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    });

    it('should hide Add Policy Button', async () => {
      renderComponent(preloadedState);
      expect(await screen.queryByRole('button', { name: 'Add a Policy' })).not.toBeInTheDocument();
    });
  });

  describe('Pro Tier Gating', () => {
    beforeAll(() => {
      ownerName = rootOrganizationPoliciesByOwnerPayload.ownerName;
      ownerId = rootOrganizationPoliciesByOwnerPayload.ownerId;
      ownerType = rootOrganizationPoliciesByOwnerPayload.ownerType;

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
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: true,
            enforcement: true,
          },
        },
        productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              id: ownerId,
              name: ownerName,
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          },
          stages: {
            action: {
              loading: false,
              error: null,
              stageTypes: null,
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: rootOrganizationPoliciesByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    });

    it('shows lock icon instead of plus icon when custom-policies feature is absent', async () => {
      renderComponent(preloadedState);
      const button = await screen.findByRole('button', { name: 'Preview Add a Policy' });
      expect(button).toBeVisible();
      expect(screen.queryByRole('button', { name: 'Add a Policy' })).not.toBeInTheDocument();
    });

    it('shows Enterprise Feature tooltip on the add policy button', async () => {
      renderComponent(preloadedState);
      await screen.findByRole('button', { name: 'Preview Add a Policy' });
      expect(screen.getByText('Preview Add a Policy')).toBeVisible();
    });
  });

  describe('Virtual Repository Manager scoping (FIRE-665)', () => {
    beforeAll(() => {
      ownerName = repositoryByOwnerPayload.ownerName;
      ownerId = repositoryByOwnerPayload.ownerId;
      ownerType = repositoryByOwnerPayload.ownerType;

      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.repository',
            url: '/repository/{repositoryId}',
            data: { title: 'Repository Management', viewportSized: true },
          },
          currentParams: { repositoryId: ownerId },
        },
        productFeatures: {
          loading: false,
          loadError: null,
          productFeatures: {
            firewall: firewallSupported,
            enforcement: enforcementSupported,
            'custom-policies': true,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: { id: ownerId, name: ownerName },
          },
          stages: { action: { loading: false, error: null, stageTypes: null } },
        },
      };
    });

    beforeEach(() => {
      axiosMock
        .onGet(getApplicablePolicies(ownerType, ownerId))
        .reply(200, { policiesByOwner: repositoryByOwnerPayload.policiesByOwner });
      axiosMock.onGet(getActionStageUrl()).reply(200, actionStagesPayload);
    });

    it('hides Add a Policy button and shows the inheritance info banner for a proxy under a VRM', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectShowRepositoryConfiguration').mockReturnValue(true);
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(false);

      renderComponent(preloadedState);

      expect(await screen.findByText('Policies')).toBeVisible();
      expect(
        screen.getByText(
          'Policies for a proxy repository are inherited from its Virtual Repository Manager and cannot be edited here.'
        )
      ).toBeVisible();
      expect(screen.queryByRole('button', { name: 'Add a Policy' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Preview Add a Policy' })).not.toBeInTheDocument();
    });

    it('shows Add a Policy button and the VRM-level info banner on a Virtual Repository Manager', async () => {
      jest.spyOn(ownerSideNavSelectors, 'selectShowRepositoryConfiguration').mockReturnValue(false);
      jest.spyOn(ownerSideNavSelectors, 'selectIsVirtualRepositoryManager').mockReturnValue(true);

      renderComponent(preloadedState);

      expect(await screen.findByText('Policies')).toBeVisible();
      expect(
        screen.getByText('Policies applied to this Virtual Repository Manager govern all proxy repositories below.')
      ).toBeVisible();
      const addButton = await screen.findByRole('button', { name: 'Add a Policy' });
      expect(addButton).toBeVisible();
      expect(addButton).toBeEnabled();
      fireEvent.click(addButton);
      expect(goToCreatePolicySpy).toHaveBeenCalled();
    });
  });
});
