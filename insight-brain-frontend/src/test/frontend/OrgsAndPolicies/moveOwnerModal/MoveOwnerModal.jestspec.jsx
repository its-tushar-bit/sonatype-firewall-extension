/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import MoveOwnerModal from 'MainRoot/OrgsAndPolicies/moveOwner/MoveOwnerModal';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import {
  getDestinationOrganizationsUrl,
  getMoveApplicationUrl,
  getMoveOrganizationUrl,
  getMoveOrganizationCSVErrorsUrl,
} from 'MainRoot/util/CLMLocation';
import { fireEvent } from '@testing-library/react';

describe('MoveOwnerModal', () => {
  let axiosMock;

  const rootOrgId = 'ROOT_ORGANIZATION_ID';
  const adminOrgId = 'd2612d914cfc41b7b0ee9be7539e4889';
  const adminOrgId2 = 'd2612d914cfc41b7b0ee9be7539e4875';
  const awesomeOrgId = '457800f1bd624699a224150aead48cf3';
  const testApplicationPublicId = 'testApplicationPublicID';
  const rootOrg = {
    type: 'organization',
    id: rootOrgId,
    name: 'Root Organization',
    synthetic: false,
    parentOrganizationId: null,
    applicationIds: [],
    organizationIds: [adminOrgId, awesomeOrgId],
    subOrgs: 2,
    totalApps: 1,
  };
  const adminOrg = {
    type: 'organization',
    id: adminOrgId,
    name: 'admin',
    synthetic: false,
    parentOrganization: rootOrgId,
    parentOrganizationId: rootOrgId,
    applicationIds: [testApplicationPublicId],
    organizationIds: [],
    subOrgs: 0,
    totalApps: 1,
  };
  const adminOrg2 = {
    type: 'organization',
    id: adminOrgId2,
    name: 'admin2',
    synthetic: false,
    parentOrganization: rootOrgId,
    parentOrganizationId: rootOrgId,
    applicationIds: [],
    organizationIds: [],
    subOrgs: 0,
    totalApps: 0,
  };
  const awesomeOrg = {
    type: 'organization',
    id: awesomeOrgId,
    name: 'Awesome Org',
    synthetic: false,
    parentOrganization: rootOrgId,
    parentOrganizationId: rootOrgId,
    applicationIds: [],
    organizationIds: [],
    subOrgs: 12,
    totalApps: 3,
  };
  const testApplication = {
    type: 'application',
    id: 'b96799515b294417859c5d6e400dd0b8',
    name: 'Test Application',
    publicId: testApplicationPublicId,
    organizationId: adminOrgId,
  };
  const preloadedStateForApp = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'b96799515b294417859c5d6e400dd0b8',
          publicId: 'testApplicationPublicID',
          name: 'Test Application',
          organizationId: 'd2612d914cfc41b7b0ee9be7539e4889',
          organizationName: 'admin',
          contact: null,
        },
      },
      ownerActions: {
        moveOwner: {
          isMoveOwnerModalOpen: true,
          fetchOrgs: {
            organizations: [],
            loadError: null,
            loading: false,
            isShowNoAvailableOrgsWarning: false,
          },
          selectedOrganization: null,
          isShowSuccessModal: false,
          isDirty: false,
          submitError: null,
          submitMaskState: null,
          warnings: null,
        },
      },
      ownerSideNav: {
        ownersMap: {
          ROOT_ORGANIZATION_ID: rootOrg,
          d2612d914cfc41b7b0ee9be7539e4889: adminOrg,
          '457800f1bd624699a224150aead48cf3': awesomeOrg,
          testApplicationPublicID: testApplication,
        },
        topParentOrganizationId: rootOrgId,
        displayedOrganization: adminOrg,
        flattenEntries: {
          organizations: [rootOrg, adminOrg, awesomeOrg],
          applications: [testApplication],
        },
        filteredEntries: {
          organizations: [],
          applications: [],
        },
      },
    },
    router: { currentState: { name: 'application' } },
  };

  const preloadedStateForOrg = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: '457800f1bd624699a224150aead48cf3',
          name: 'Awesome Org',
          parentOrganizationId: 'd2612d914cfc41b7b0ee9be7539e4889',
        },
      },
      ownerActions: {
        moveOwner: {
          isMoveOwnerModalOpen: true,
          fetchOrgs: {
            organizations: [],
            loadError: null,
            loading: false,
            isShowNoAvailableOrgsWarning: false,
          },
          selectedOrganization: null,
          isShowSuccessModal: false,
          isDirty: false,
          submitError: null,
          submitMaskState: null,
          warnings: null,
        },
      },
      ownerSideNav: {
        ownersMap: {
          ROOT_ORGANIZATION_ID: rootOrg,
          d2612d914cfc41b7b0ee9be7539e4889: adminOrg,
          d2612d914cfc41b7b0ee9be7539e4875: adminOrg2,
          '457800f1bd624699a224150aead48cf3': awesomeOrg,
          testApplicationPublicID: testApplication,
        },
        topParentOrganizationId: rootOrgId,
        displayedOrganization: adminOrg,
        flattenEntries: {
          organizations: [adminOrg, adminOrg2, awesomeOrg, rootOrg],
          applications: [testApplication],
        },
        filteredEntries: {
          organizations: [],
          applications: [],
        },
      },
    },
    router: { currentState: { name: 'organization' } },
  };

  const renderComponent = (preloadedState) =>
    render(<MoveOwnerModal />, { preloadedState: preloadedState || preloadedStateForApp });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  it("doesn't show modal without being open", () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          moveOwner: {
            isMoveOwnerModalOpen: false,
            fetchOrgs: {
              organizations: [],
              loadError: null,
              loading: false,
              isShowNoAvailableOrgsWarning: false,
            },
            selectedOrganization: null,
            isShowSuccessModal: false,
            isDirty: false,
            submitError: null,
            submitMaskState: null,
            warnings: null,
          },
        },
      },
    });
    const appMoveModalTitle = screen.queryByText('Move Test Application');
    expect(appMoveModalTitle).toBeNull();
  });

  it('shows modal with the correct title for app', () => {
    renderComponent();
    const appMoveModalTitle = screen.getByText('Move Test Application');
    expect(appMoveModalTitle).toBeVisible();
  });

  it('shows modal with the correct title for org and messages with descendants', () => {
    renderComponent(preloadedStateForOrg);
    const orgMoveModalTitle = screen.getByText('Move Awesome Org');
    const orgMoveModalMessage = screen.getByText(
      'Moving Awesome Org will move 15 descendants. Confirm inheritance details after the move is complete.'
    );
    expect(orgMoveModalTitle).toBeVisible();
    expect(orgMoveModalMessage).toBeVisible();
  });

  // TODO: when the new endpoint for fetching organizations when moving an orgs is ready,
  // implement the unit test for that request here. See https://sonatype.atlassian.net/browse/CLM-26075.

  describe('applications', () => {
    it('fetches organizations, when opening modal for app', () => {
      renderComponent();
      expect(axiosMock.history.get).toContainEqual(
        expect.objectContaining({ url: getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true) })
      );
    });

    it('shows loading spinner', () => {
      axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true)).reply(200);
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('shows warning message, if there are no available organizations', async () => {
      axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true)).reply(200, []);
      renderComponent();
      expect(axiosMock.history.get).toContainEqual(
        expect.objectContaining({ url: getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true) })
      );
      const warningMsg = await screen.findByText('No available destination organizations.');
      expect(warningMsg).toBeVisible();
    });

    describe('successfully fetched available to move organizations', () => {
      beforeEach(() => {
        axiosMock
          .onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true))
          .reply(200, [rootOrg, adminOrg2, awesomeOrg]);
      });
      it('shows Submit and Cancel buttons', async () => {
        renderComponent();
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        expect(submitButton).toBeVisible();
        expect(cancelButton).toBeVisible();
      });

      it('shows current organization in the selector when open modal', async () => {
        renderComponent();
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const options = await screen.findAllByRole('option');
        expect(options.length).toBe(4);
        expect(options[0].selected).toBeTruthy();
        expect(options[0].value).toBe('d2612d914cfc41b7b0ee9be7539e4889');
        expect(submitButton).toBeVisible();
        fireEvent.click(submitButton);
        expect(axiosMock.history.post.length).toBe(0);
      });

      it('closes modal when clicking on "Cancel" Button', async () => {
        renderComponent();
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        fireEvent.click(cancelButton);
        const appMoveModalTitle = await screen.queryByText('Move Application');
        expect(appMoveModalTitle).toBeNull();
      });

      it('submits the form and shows Success Modal', async () => {
        axiosMock
          .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
          .reply(200);
        renderComponent();
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const select = await screen.findByRole('combobox');
        const options = await screen.findAllByRole('option');
        fireEvent.change(select, { target: { value: '457800f1bd624699a224150aead48cf3' } });
        expect(options[3].selected).toBeTruthy();
        fireEvent.click(submitButton);
        expect(axiosMock.history.post.length).toBe(1);
        expect(axiosMock.history.post[0].url).toBe(
          getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3')
        );
        const successModalTitle = await screen.findByText('Application Moved Successfully');
        const successModalMessage = await screen.findByText(
          'Please check the following configurations to ensure they do what you expect :'
        );
        expect(successModalTitle).toBeVisible();
        expect(successModalMessage).toBeVisible();
      });

      it('shows warning if move application from organization with continuous monitoring settings to organization which does not use continuous policy monitoring', async () => {
        axiosMock
          .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
          .reply(200, {
            warnings: ['The new parent organization does not use continuous policy monitoring.', 'Second warning'],
          });
        renderComponent();
        const select = await screen.findByRole('combobox');
        fireEvent.change(select, { target: { value: '457800f1bd624699a224150aead48cf3' } });
        const enabledSubmitButton = await screen.findByRole('button', { name: 'Move' });
        fireEvent.click(enabledSubmitButton);
        const successModalTitle = await screen.findByText('Application Moved Successfully');
        const continuousMonitoringWarningTitle = await screen.findByText(
          'Policy configurations that differ between the old and new parent:'
        );
        const continuousMonitoringWarningMessage1 = await screen.findByText(
          'The new parent organization does not use continuous policy monitoring.'
        );
        const continuousMonitoringWarningMessage2 = await screen.findByText('Second warning');

        expect(successModalTitle).toBeVisible();
        expect(continuousMonitoringWarningTitle).toBeVisible();
        expect(continuousMonitoringWarningMessage1).toBeVisible();
        expect(continuousMonitoringWarningMessage2).toBeVisible();
      });
    });

    describe('error cases', () => {
      const checkAlertAndRetry = async () => {
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();
      };
      it('renders error message, retry and ok button when loading orgs fails', async () => {
        axiosMock
          .onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true))
          .reply(500, 'Error Messages');
        renderComponent();
        await checkAlertAndRetry();
        expect(await screen.findByText('An error occurred loading data. Error Messages')).toBeVisible();
        expect(await screen.findByRole('button', { name: 'OK' })).toBeVisible();
      });
      describe('error cases when moving applications', () => {
        beforeEach(() => {
          axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8', true)).reply(200, [
            {
              id: '457800f1bd624699a224150aead48cf3',
              parentOrganizationId: 'ROOT_ORGANIZATION_ID',
              name: 'Awesome Org',
              nameLowercaseNoWhitespace: 'awesomeorg',
              legacyViolationEnabled: null,
              allowLegacyViolationOverride: true,
              repositoryConnectionEnabled: null,
              allowRepositoryConnectionOverride: true,
              artifactoryConnectionEnabled: null,
              allowArtifactoryConnectionOverride: true,
            },
          ]);
        });
        const goToNextWindow = async () => {
          const select = await screen.findByRole('combobox');
          fireEvent.change(select, { target: { value: '457800f1bd624699a224150aead48cf3' } });
          const enabledSubmitButton = await screen.findByRole('button', { name: 'Move' });
          fireEvent.click(enabledSubmitButton);
        };
        it('renders error message after clicking the "Move" button', async () => {
          axiosMock
            .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
            .reply(500, 'Error Messages');
          renderComponent();
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('An error occurred saving data. Error Messages')).toBeVisible();
        });
        it('renders error message for incompatible destination.', async () => {
          axiosMock
            .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
            .reply(409, { errors: ['Error 1', 'Error 2'] });
          renderComponent();
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('Incompatible Destination:')).toBeVisible();
          expect(await screen.findByText('Error 1. Error 2')).toBeVisible();
        });
        it('fetch csv button should not be rendered for apps', async () => {
          axiosMock
            .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
            .reply(500, 'Error Messages');
          renderComponent();
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('An error occurred saving data. Error Messages')).toBeVisible();
          expect(screen.queryByText('Fetch CSV')).not.toBeInTheDocument();
        });
      });
    });
  });

  describe('organizations', () => {
    it('shows warning message, if there are no available organizations', async () => {
      axiosMock.onGet(getDestinationOrganizationsUrl('457800f1bd624699a224150aead48cf3', false)).reply(200, []);
      renderComponent(preloadedStateForOrg);
      const warningMsg = await screen.findByText('No available destination organizations.');
      expect(warningMsg).toBeVisible();
    });

    describe('successfully fetched available to move organizations', () => {
      beforeEach(() => {
        axiosMock
          .onGet(getDestinationOrganizationsUrl('457800f1bd624699a224150aead48cf3', false))
          .reply(200, [rootOrg, adminOrg2]);
      });
      it('shows Submit and Cancel buttons', async () => {
        renderComponent(preloadedStateForOrg);
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        expect(submitButton).toBeVisible();
        expect(cancelButton).toBeVisible();
      });

      it('shows current organization in the selector when open modal', async () => {
        renderComponent(preloadedStateForOrg);
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const options = await screen.findAllByRole('option');
        expect(options.length).toBe(3);
        expect(options[0].selected).toBeTruthy();
        expect(options[0].value).toBe('d2612d914cfc41b7b0ee9be7539e4889');
        expect(submitButton).toBeVisible();
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(0);
      });

      it('closes modal when clicking on "Cancel" Button', async () => {
        renderComponent(preloadedStateForOrg);
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        let orgMoveModalTitle = screen.getByText('Move Awesome Org');
        expect(orgMoveModalTitle).toBeVisible();
        fireEvent.click(cancelButton);
        orgMoveModalTitle = screen.queryByText('Move Awesome Org');
        expect(orgMoveModalTitle).toBeNull();
      });

      it('submits the form and shows Success Modal', async () => {
        axiosMock
          .onPut(getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875'))
          .reply(200);
        renderComponent(preloadedStateForOrg);
        const submitButton = await screen.findByRole('button', { name: 'Move' });
        const select = await screen.findByRole('combobox');
        const options = await screen.findAllByRole('option');
        fireEvent.change(select, { target: { value: 'd2612d914cfc41b7b0ee9be7539e4875' } });
        expect(options[2].selected).toBeTruthy();
        fireEvent.click(submitButton);
        expect(axiosMock.history.put.length).toBe(1);
        expect(axiosMock.history.put[0].url).toBe(
          getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875')
        );
        const successModalTitle = await screen.findByText('Organization Moved Successfully');
        const infoAlert = await screen.findByText(
          'Local configuration settings are unaffected and remain unchanged by a move. Be sure to confirm any previously inherited parent-level configurations to ensure the desired behavior.'
        );
        expect(successModalTitle).toBeVisible();
        expect(infoAlert).toBeVisible();
      });

      it('shows warning if move organization from organization with continuous monitoring settings to organization which does not use continuous policy monitoring', async () => {
        axiosMock
          .onPut(getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875'))
          .reply(200, {
            warnings: [{ message: 'Some random warnings' }, { message: 'Second warning' }],
          });
        renderComponent(preloadedStateForOrg);
        const select = await screen.findByRole('combobox');
        fireEvent.change(select, { target: { value: 'd2612d914cfc41b7b0ee9be7539e4875' } });
        const enabledSubmitButton = await screen.findByRole('button', { name: 'Move' });
        fireEvent.click(enabledSubmitButton);
        const successModalTitle = await screen.findByText('Organization Moved Successfully');
        const infoAlert = await screen.findByText(
          'Local configuration settings are unaffected and remain unchanged by a move. Be sure to confirm any previously inherited parent-level configurations to ensure the desired behavior.'
        );
        const warningAlert1 = await screen.findByText('Some random warnings');
        const warningAlert2 = await screen.findByText('Second warning');

        expect(successModalTitle).toBeVisible();
        expect(infoAlert).toBeVisible();
        expect(warningAlert1).toBeVisible();
        expect(warningAlert2).toBeVisible();
      });
    });

    describe('error cases', () => {
      const checkAlertAndRetry = async () => {
        expect(await screen.findByRole('alert')).toBeVisible();
        expect(await screen.findByRole('button', { name: 'Retry' })).toBeVisible();
      };
      it('renders error message, retry and ok button when loading orgs fails', async () => {
        axiosMock
          .onGet(getDestinationOrganizationsUrl('457800f1bd624699a224150aead48cf3', false))
          .reply(500, 'Error Messages');
        renderComponent(preloadedStateForOrg);
        await checkAlertAndRetry();
        expect(await screen.findByText('An error occurred loading data. Error Messages')).toBeVisible();
        expect(await screen.findByRole('button', { name: 'OK' })).toBeVisible();
      });
      describe('error cases when moving organizations', () => {
        beforeEach(() => {
          axiosMock
            .onGet(getDestinationOrganizationsUrl('457800f1bd624699a224150aead48cf3', false))
            .reply(200, [rootOrg, adminOrg2]);
        });
        const goToNextWindow = async () => {
          const select = await screen.findByRole('combobox');
          fireEvent.change(select, { target: { value: 'd2612d914cfc41b7b0ee9be7539e4875' } });
          const enabledSubmitButton = await screen.findByRole('button', { name: 'Move' });
          fireEvent.click(enabledSubmitButton);
        };
        it('renders error message after clicking the "Move" button', async () => {
          axiosMock
            .onPut(getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875'))
            .reply(500, 'Error Messages');
          renderComponent(preloadedStateForOrg);
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('An error occurred saving data. Error Messages')).toBeVisible();
        });
        it('renders error message for incompatible destination and shows Fetch CSV button', async () => {
          axiosMock
            .onPut(getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875'))
            .reply(409, { errors: ['Error 1', 'Error 2'] });
          renderComponent(preloadedStateForOrg);
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('Incompatible Destination:')).toBeVisible();
          expect(
            await screen.findByText(
              'There are configuration conflicts preventing the move operation. Errors details can be accessed by fetching a CSV file for download.'
            )
          ).toBeVisible();
          expect(await screen.findByText('Fetch CSV')).toBeVisible();
        });
        it('does not render fetch csv button to get csv file of errors when there is a generic error', async () => {
          axiosMock
            .onPut(getMoveOrganizationUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875'))
            .reply(500, 'Error Messages');
          axiosMock
            .onGet(
              getMoveOrganizationCSVErrorsUrl('457800f1bd624699a224150aead48cf3', 'd2612d914cfc41b7b0ee9be7539e4875')
            )
            .reply(200, `Type,Description\r\nERROR,Error message description`);
          renderComponent(preloadedStateForOrg);
          await goToNextWindow();
          await checkAlertAndRetry();
          expect(await screen.findByText('An error occurred saving data. Error Messages')).toBeVisible();
          expect(screen.queryByText('Fetch CSV')).not.toBeInTheDocument();
        });
      });
    });
  });
});
