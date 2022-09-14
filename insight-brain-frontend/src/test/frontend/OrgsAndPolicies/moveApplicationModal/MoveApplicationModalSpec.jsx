/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import MoveApplicationModal from 'MainRoot/OrgsAndPolicies/moveApplicationModal/MoveApplicationModal';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getDestinationOrganizationsUrl, getMoveApplicationUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent } from '@testing-library/react';

describe('MoveApplicationModal', () => {
  let renderComponent, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
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
        ownerEditor: {
          moveApplication: {
            isMoveAppModalOpen: true,
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
    };

    renderComponent = (preloadedState) =>
      render(<MoveApplicationModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });
  it('doesn"t show modal without being open', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerEditor: {
          moveApplication: {
            isMoveAppModalOpen: false,
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
    const appMoveModalTitle = screen.queryByText('Move Application');
    expect(appMoveModalTitle).toBeNull();
  });

  it('shows modal with the correct title', () => {
    renderComponent();
    const appMoveModalTitle = screen.getByText('Move Application');
    expect(appMoveModalTitle).toBeVisible();
  });

  it('fetches organizations, when opening modal', () => {
    renderComponent();
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8'));
  });

  it('shows loading spinner', () => {
    axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8')).reply(200);
    renderComponent();
    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('shows warning message, if there are no available organizations', async () => {
    axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8')).reply(200, []);
    renderComponent();
    expect(axiosMock.history.get.length).toBe(1);
    const warningMsg = await screen.findByText('No available destination organizations.');
    expect(warningMsg).toBeVisible();
  });

  describe('successfully fetched available to move organizations', () => {
    beforeEach(() => {
      axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8')).reply(200, [
        {
          id: '457800f1bd624699a224150aead48cf3',
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Awesome Org',
          nameLowercaseNoWhitespace: 'awesomeorg',
          policyViolationGrandfatheringEnabled: null,
          allowPolicyViolationGrandfatheringOverride: true,
          repositoryConnectionEnabled: null,
          allowRepositoryConnectionOverride: true,
          artifactoryConnectionEnabled: null,
          allowArtifactoryConnectionOverride: true,
        },
      ]);
    });
    it('shows Submit and Cancel buttons', async () => {
      renderComponent();
      const submitButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
      expect(submitButton).toBeVisible();
      expect(cancelButton).toBeVisible();
    });

    it('shows current organization in the selector and disables Submit button, when open modal', async () => {
      renderComponent();
      const submitButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      const options = await screen.findAllByRole('option');
      expect(options.length).toBe(2);
      expect(options[0].selected).toBeTruthy();
      expect(options[0].value).toBe('d2612d914cfc41b7b0ee9be7539e4889');
      expect(submitButton).toBeVisible();
      expect(submitButton).toHaveClassName('disabled');
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
      const submitButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      const select = await screen.findByRole('combobox');
      const options = await screen.findAllByRole('option');
      expect(submitButton).toHaveClassName('disabled');
      fireEvent.change(select, { target: { value: '457800f1bd624699a224150aead48cf3' } });
      expect(options[1].selected).toBeTruthy();
      const enabledSubmitButton = await screen.findByRole('button', { name: 'Move' });
      expect(enabledSubmitButton).not.toHaveClassName('disabled');
      fireEvent.click(enabledSubmitButton);
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
      axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8')).reply(500, 'Error Messages');
      renderComponent();
      checkAlertAndRetry();
      expect(await screen.findByText('An error occurred loading data. Error Messages')).toBeVisible();
      expect(await screen.findByRole('button', { name: 'OK' })).toBeVisible();
    });
    describe('error cases when moving organizations', () => {
      beforeEach(() => {
        axiosMock.onGet(getDestinationOrganizationsUrl('b96799515b294417859c5d6e400dd0b8')).reply(200, [
          {
            id: '457800f1bd624699a224150aead48cf3',
            parentOrganizationId: 'ROOT_ORGANIZATION_ID',
            name: 'Awesome Org',
            nameLowercaseNoWhitespace: 'awesomeorg',
            policyViolationGrandfatheringEnabled: null,
            allowPolicyViolationGrandfatheringOverride: true,
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
        goToNextWindow();
        checkAlertAndRetry();
        expect(await screen.findByText('An error occurred saving data. Error Messages')).toBeVisible();
      });
      it('renders error message for incompatible destination.', async () => {
        axiosMock
          .onPost(getMoveApplicationUrl('b96799515b294417859c5d6e400dd0b8', '457800f1bd624699a224150aead48cf3'))
          .reply(409, { errors: ['Error 1', 'Error 2'] });
        renderComponent();
        goToNextWindow();
        checkAlertAndRetry();
        expect(await screen.findByText('Incompatible Destinations:')).toBeVisible();
        expect(await screen.findByText('Error 1. Error 2')).toBeVisible();
      });
    });
  });
});
