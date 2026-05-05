/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as R from 'ramda';

import { render, screen, axiosMockAdapter, fireEvent, within, waitFor } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import {
  getActionStageUrl,
  getApplicableCategoriesUrl,
  getApplicablePolicies,
  getConditionTypeUrl,
  getConditionValueTypeUrl,
  getPolicyOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
  getOwnerDetailsUrl,
  getNotificationWebhooksUrl,
  getRoleMappingForCurrentOwnerUrl,
  getIsJiraEnabledUrl,
  getJiraProjectsUrl,
  getPermissionContextTestUrl,
} from 'MainRoot/util/CLMLocation';
import PolicyEditor from 'MainRoot/OrgsAndPolicies/policyEditor/PolicyEditor';
import {
  actionStage,
  applicableCategories,
  applicablePolicies,
  conditionType,
  conditionValueType,
  policyTag,
  savedPolicy,
  existingPolicy,
} from './mockData';
import { initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import { mergeDeepRight } from 'ramda';

describe('PolicyEditorSpec', () => {
  let initState, sbomState;
  const POLICY_ID_OVERRIDE_ENABLED_INHERITED = '9d5c30f793a54446a9601cf36c18e9e3';
  const POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN = '12f2086417ab44f9a63ba5e91786c570';
  const POLICY_ID_OVERRIDE_NOT_ENABLED = '2a1cb71651d14a60b0fa77ef829f5ec0';
  const REPOSITORY_POLICY_ID = 'ec1394dcbd344633a82f1f0d6fd54e97';
  const ROOT_ORG_ID = 'ROOT_ORGANIZATION_ID';
  const REPOSITORY_CONTAINER_ID = 'REPOSITORY_CONTAINER_ID';
  const REPOSITORY_MANAGER_ID = 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232';
  const ORG_ID = '05602dd5ba934c318ad011ca4e4f5cfe';
  const APP_ID = 'testapp';
  const REPO_CONTAINER_ID = 'REPOSITORY_CONTAINER_ID';
  const REPO_ID = 'sonatype-internal';
  let mockAxiosCalls;

  const setInitStateAndMockHttpRequests = (ownerType, ownerId, policyId, mockNotificationEndpoints, permissions) => {
    initState = {
      router: {
        currentState: { name: ownerType },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: ownerId,
            type: ownerType,
          },
        },
      },
      productFeatures: {
        productFeatures: {
          firewall: true,
          'custom-policies': true,
        },
      },
    };
    if (ownerType === 'repository_container') {
      initState.router.currentParams = { repositoryContainerId: REPO_CONTAINER_ID };
    } else if (ownerType === 'repository_manager') {
      initState.router.currentParams = { repositoryManagerId: ownerId };
    } else if (ownerType === 'organization') {
      initState.router.currentParams = { organizationId: ownerId };
    } else if (ownerType === 'repository') {
      initState.router.currentParams = { repositoryId: ownerId };
    } else {
      initState.router.currentParams = { applicationPublicId: ownerId };
    }

    if (policyId) {
      initState.router.currentParams.policyId = policyId;
    }

    const conditionTypeUrl = getConditionTypeUrl();
    const actionStageUrl = getActionStageUrl();

    const applicablePoliciesUrl = getApplicablePolicies(ownerType, ownerId);
    const conditionValueTypeUrl = getConditionValueTypeUrl(ownerType, ownerId);
    const applicableCategoriesUrl = getApplicableCategoriesUrl(ownerType, ownerId);

    const policyTagUrl = getPolicyTagUrl(policyId, ownerType, ownerId);

    mockAxiosCalls.onGet(conditionTypeUrl).reply(200, conditionType);
    mockAxiosCalls.onGet(actionStageUrl).reply(200, actionStage);

    mockAxiosCalls.onGet(conditionValueTypeUrl).reply(200, conditionValueType);
    mockAxiosCalls.onGet(applicableCategoriesUrl).reply(200, applicableCategories[ownerType]?.[ownerId] ?? {});
    mockAxiosCalls.onGet(applicablePoliciesUrl).reply(200, applicablePolicies[ownerType]?.[ownerId]);
    mockAxiosCalls.onGet(policyTagUrl).reply(200, policyTag[ownerType]?.[ownerId]?.[policyId] ?? {});

    if (mockNotificationEndpoints) {
      const webhooksUrl = getNotificationWebhooksUrl(ownerType, ownerId);
      const rolesUrl = getRoleMappingForCurrentOwnerUrl(ownerType, ownerId);
      const isJiraEnabledUrl = getIsJiraEnabledUrl();
      const jiraProjectsUrl = getJiraProjectsUrl();
      const notificationWebhooks = [
        {
          description: 'webhook1name',
          eventTypes: null,
          id: 'webhook1',
          secretKey: null,
          url: 'http://sdf.com',
        },
      ];
      mockAxiosCalls.onGet(webhooksUrl).reply(200, notificationWebhooks);
      const roles = [{ roleId: '1', roleName: 'developer' }];
      mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
      mockAxiosCalls.onGet(isJiraEnabledUrl).reply(200, true);
      const jiraProjects = [
        {
          key: 'key1',
          name: 'Project One',
          issueTypes: [
            {
              id: 1,
              name: 'Bug',
            },
            {
              id: 2,
              name: 'Task',
            },
          ],
        },
        {
          key: 'key2',
          name: 'Project Two',
          issueTypes: [
            {
              id: 1,
              name: 'Bug',
            },
            {
              id: 3,
              name: 'Issue',
            },
          ],
        },
      ];
      mockAxiosCalls.onGet(jiraProjectsUrl).reply(200, jiraProjects);
    }
    mockAxiosCalls
      .onPut(getPermissionContextTestUrl(ownerType, ownerId), ['WRITE'])
      .reply(200, [...(permissions ? permissions : ['WRITE'])]);
  };

  const setSbomState = (withFirewallLicense = false, withLifecycleLicense = false) => {
    sbomState = {
      productLicense: {
        license: {
          products: R.compose(
            R.when(R.always(withLifecycleLicense), R.append('Sonatype Lifecycle')),
            R.when(R.always(withFirewallLicense), R.append('Sonatype Repository Firewall'))
          )(['Sonatype SBOM Manager']),
        },
      },
      router: {
        currentParams: {
          organizationId: ROOT_ORG_ID,
          applicationPublicId: ROOT_ORG_ID,
          policyId: POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN,
        },
        currentState: { name: 'sbomManager.management.view.organization' },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: ROOT_ORG_ID,
            type: 'organization',
          },
        },
        policy: {
          currentPolicy: existingPolicy,
          isInherited: false,
          notificationsEditor: {},
          loadError: null,
          loadingCategories: false,
          loadingPolicyEditor: false,
          loadingSavePolicy: false,
        },
      },
    };
  };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  const renderComponent = (preloadedState) => render(<PolicyEditor />, { preloadedState });

  it('disables the Update button when there is no permission', async () => {
    setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
    renderComponent(initState);
    const updateButton = await screen.findByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClass('disabled');
  });

  it('disables the Delete button when there is no permission', async () => {
    setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
    renderComponent(initState);
    const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
    expect(deleteButton).toBeVisible();
    expect(deleteButton).toBeDisabled();
  });

  describe('SBOM Manager', () => {
    it('renders the edit policy page and not new policy page', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      setSbomState();
      renderComponent(sbomState);
      expect(await screen.findByRole('heading', { name: 'Policy Settings' })).toBeVisible();
    });

    it('does not render Delete button under SBOM Manager', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      setSbomState();
      renderComponent(sbomState);
      expect(await screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    });

    it('does not render the Actions tile under SBOM Manager', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      setSbomState();
      renderComponent(sbomState);
      expect(await screen.queryByRole('heading', { name: 'Actions' })).not.toBeInTheDocument();
    });

    describe('lock icon and alert message', () => {
      it('displays the correct alert message when both Sonatype Repository Firewall and Sonatype SBOM Manager are enabled', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
        setSbomState(true, false);
        renderComponent(sbomState);
        const alert = await screen.findByText('Switch to Repository Firewall to manage your policies.');
        expect(alert).toBeVisible();
      });

      it('displays the correct alert message when Sonatype Lifecycle, Sonatype Repository Firewall, and Sonatype SBOM Manager are enabled', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
        setSbomState(true, true);
        renderComponent(sbomState);
        const alert = await screen.findByText('Switch to Lifecycle to manage your policies.');
        expect(alert).toBeVisible();
      });

      it('displays the correct alert message when both Sonatype Lifecycle and Sonatype SBOM Manager are enabled', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
        setSbomState(false, true);
        renderComponent(sbomState);
        const alert = await screen.findByText('Switch to Lifecycle to manage your policies.');
        expect(alert).toBeVisible();
      });

      it('displays the correct alert message when Sonatype Lifecycle is not enabled', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
        setSbomState();
        renderComponent(sbomState);
        const alert = await screen.findByText('Custom policies are available with Lifecycle.');
        expect(alert).toBeVisible();
      });

      it('displays lock icon on sbom manager page', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
        setSbomState();
        renderComponent(sbomState);
        const lockIcon = await screen.findByTestId('policy-editor-lock-icon');
        expect(lockIcon).toBeVisible();
      });
    });
  });

  describe('Local policy', () => {
    describe('Update policy', () => {
      describe('Rendering', () => {
        it('renders the form with a hidden update button and delete button, shows the loading state before', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          const updateButton = await screen.findByText('Update');
          const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
          const policyTitle = screen.getByText('Policy Settings');
          expect(policyTitle).toBeVisible();
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText('There were validation errors. There are no changes to save.');
          expect(alert).toBeVisible();

          expect(deleteButton).toBeVisible();
          expect(deleteButton).not.toHaveClass('disabled');
        });

        it('styles update button as disabled when read only', async () => {
          setInitStateAndMockHttpRequests(
            'organization',
            ROOT_ORG_ID,
            POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN,
            false,
            []
          );
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          expect(updateButton.className).toContain('disabled');

          expect(updateButton).toBeVisible();

          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();

          // the button is styled as disabled by applying the disabled class, but we do not have access
          // to truly disable it inside of NxStatefulForm, we prevent validation errors from throwing using
          // a workaround where validation errors should always be null when read only. This is why
          // we are testing a click on a "disabled" button :-)
          fireEvent.click(updateButton);

          const alert = screen.queryByText(/There were validation errors/i);
          expect(alert).not.toBeInTheDocument();
        });

        it('enables the Update button with valid data', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
          updateButton = screen.getByText('Update');
        });

        it('enables the Update button when removing a constraint', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          expect(updateButton).toBeVisible();
          const deleteConstraintBtn = await screen.findAllByLabelText('Delete constraint');
          fireEvent.click(deleteConstraintBtn[0]);
          updateButton = screen.getByText('Update');
          expect(updateButton).not.toHaveClass('disabled');
        });

        it('disables the Update button for policy name duplicated', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: 'Security-Critical' } });
          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();
        });

        it('disables the Update button for policy name empty', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: '' } });
          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();
        });

        it('disables the Update button for policy name invalid chars', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: 'License-Ba!' } });
          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();
        });

        it('disables the Update button for policy name too long', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, {
            target: { value: 'License-Banned too long too long too long too long too long too long' },
          });
          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();
        });

        it('disables the Update button for invalid constraint', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
          updateButton = screen.getByText('Update');
          expect(updateButton).not.toHaveClass('disabled');
          const addConstraintButton = screen.getByText('Add Constraint');
          fireEvent.click(addConstraintButton);
          updateButton = screen.getByText('Update');
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();
        });
      });

      describe('Saving changes', () => {
        it('saves a policy successfully, shows the save mask with the success message', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          mockAxiosCalls
            .onPut(getPolicyUrl('organization', ROOT_ORG_ID))
            .reply(200, { id: POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN });
          mockAxiosCalls
            .onPut(getPolicyTagUrl(POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, 'organization', ROOT_ORG_ID))
            .reply(200, {});

          renderComponent(initState);
          const policyNameInput = await screen.findByLabelText('Policy Name');
          fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
          let updateButton = await screen.findByText('Update');
          fireEvent.click(updateButton);
          const savingMask = screen.getByText('Saving…');
          expect(savingMask).toBeVisible();
          const successMask = await screen.findByText(/Success/);
          expect(successMask).toBeVisible();
        });

        it('fails to save a policy successfully, shows the save mask and then an error', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          mockAxiosCalls.onPut(getPolicyUrl('organization', ROOT_ORG_ID)).reply(404, 'Error');

          renderComponent(initState);
          const policyNameInput = await screen.findByLabelText('Policy Name');
          fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
          let updateButton = await screen.findByText('Update');
          fireEvent.click(updateButton);
          const savingMask = screen.getByText('Saving…');
          expect(savingMask).toBeVisible();

          const error = screen.getByText('An error occurred loading data. Error 404');
          expect(error).toBeVisible();
        });

        it('deletes a policy successfully, shows the delete mask with the success message', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          mockAxiosCalls
            .onDelete(getPolicyCRUDUrl('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
            .reply(200, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);

          renderComponent(initState);
          const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
          fireEvent.click(deleteButton);
          const deleteDesc = screen.getByRole('heading', { name: 'Delete Policy' });
          const confirmInput = screen.getByLabelText('To confirm, please type DELETE in the box below:');
          fireEvent.change(confirmInput, { target: { value: 'DELETE' } });
          const confirmDeletionButton = screen.getByText('Confirm Deletion');
          expect(deleteDesc).toBeVisible();
          fireEvent.click(confirmDeletionButton);
          const savingMask = screen.getByText('Deleting…');
          expect(savingMask).toBeVisible();
          const successMask = await screen.findAllByText(/Success/);
          expect(successMask[0]).toBeVisible();
        });

        it('fails to delete a policy successfully, shows the delete mask and then an error', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          mockAxiosCalls
            .onDelete(getPolicyCRUDUrl('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
            .reply(404, 'Error');

          renderComponent(initState);
          const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
          fireEvent.click(deleteButton);
          const deleteDesc = screen.getByRole('heading', { name: 'Delete Policy' });
          const confirmInput = screen.getByLabelText('To confirm, please type DELETE in the box below:');
          fireEvent.change(confirmInput, { target: { value: 'DELETE' } });
          const confirmDeletionButton = screen.getByText('Confirm Deletion');
          expect(deleteDesc).toBeVisible();
          fireEvent.click(confirmDeletionButton);
          const savingMask = screen.getByText('Deleting…');
          expect(savingMask).toBeVisible();

          const error = await screen.findByText('An error occurred loading data. Error 404');
          expect(error).toBeVisible();
        });

        it('renders delete modal with warning message and confirmation input', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
          fireEvent.click(deleteButton);

          expect(screen.getByRole('heading', { name: 'Delete Policy' })).toBeVisible();
          expect(screen.getByText(/You are about to permanently delete the policy/i)).toBeVisible();
          expect(screen.getByText(/Deleting this policy will:/i)).toBeVisible();
          expect(
            screen.getByText(/Immediately unquarantine all components previously quarantined by this policy/i)
          ).toBeVisible();
          expect(
            screen.getByText(
              /Permanently remove all associated waivers, which cannot be recovered without restoring from a backup/i
            )
          ).toBeVisible();
          expect(screen.getByLabelText('To confirm, please type DELETE in the box below:')).toBeVisible();
        });

        it('renders errors until DELETE is typed correctly', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);

          const deleteButton = await screen.findByRole('button', { name: 'Delete Policy' });
          fireEvent.click(deleteButton);

          const confirmInput = screen.getByLabelText('To confirm, please type DELETE in the box below:');

          fireEvent.change(confirmInput, { target: { value: 'delete' } });
          expect(screen.getByText('Must type DELETE to confirm')).toBeVisible();

          fireEvent.change(confirmInput, { target: { value: 'DELETE' } });
          expect(screen.queryByText('Must type DELETE to confirm')).not.toBeInTheDocument();
        });
      });
    });

    describe('Create policy', () => {
      it('renders the form with disabled create button and no delete button', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
        renderComponent(initState);
        const updateButton = await screen.findByText('Create');
        const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
        const policyTitle = screen.getByText('Policy Settings');
        expect(policyTitle).toBeVisible();
        expect(updateButton).toBeVisible();
        fireEvent.click(updateButton);

        const alert = screen.getByText('There were validation errors. There are no changes to save.');
        expect(alert).toBeVisible();
        expect(deleteButton).toBeNull();
      });

      it('validates that the create button gets enabled appropriately', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
        renderComponent(initState);
        let updateButton = await screen.findByText('Create');
        const policyTitle = screen.getByText('Policy Settings');

        expect(policyTitle).toBeVisible();
        expect(updateButton).toBeVisible();
        fireEvent.click(updateButton);

        const alert = screen.getByText('There were validation errors. There are no changes to save.');
        expect(alert).toBeVisible();

        const policyNameInput = await screen.findByLabelText('Policy Name');
        fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
        updateButton = screen.getByText('Create');

        const constraintNameInput = screen.getByLabelText('Constraint Name');
        fireEvent.change(constraintNameInput, { target: { value: 'New Value' } });
        updateButton = screen.getByText('Create');

        const conditionAgeInput = screen.getByPlaceholderText('Age');
        fireEvent.change(conditionAgeInput, { target: { value: '2' } });
        updateButton = screen.getByText('Create');
        expect(updateButton).not.toHaveClass('disabled');
      });

      describe('validate pristine state and validation of coordinates condition constraint', () => {
        it('for maven', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });

          const conditionGroupIdInput = screen.getByPlaceholderText('Group ID');
          const conditionArtifactIdInput = screen.getByPlaceholderText('Artifact ID');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionExtensionInput = screen.getByPlaceholderText('Extension');
          const conditionClassifierInput = screen.getByPlaceholderText('Classifier');

          expect(conditionGroupIdInput).toBeVisible();
          expect(conditionArtifactIdInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionExtensionInput).toBeVisible();
          expect(conditionExtensionInput).toHaveValue('*');
          expect(conditionClassifierInput).toBeVisible();
          expect(conditionClassifierInput).toHaveValue('*');

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionGroupIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionArtifactIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionExtensionInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionClassifierInput.parentElement.parentElement).toHaveClass('pristine');

          const user = userEvent.setup();
          await user.clear(conditionExtensionInput);
          await user.clear(conditionClassifierInput);

          expect(conditionExtensionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionClassifierInput.parentElement.parentElement).toHaveClass('valid');
        });

        it('for a-name', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');
          fireEvent.change(coordinatesFormatSelect, { target: { value: 'a-name' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionQualifierInput = screen.getByPlaceholderText('Qualifier');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNameInput).toBeVisible();
          expect(conditionQualifierInput).toBeVisible();
          expect(conditionQualifierInput).toHaveValue('*');
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionQualifierInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');

          const user = userEvent.setup();
          await user.clear(conditionQualifierInput);

          expect(conditionQualifierInput.parentElement.parentElement).toHaveClass('valid');
        });

        it('for pypi', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');
          fireEvent.change(coordinatesFormatSelect, { target: { value: 'pypi' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionQualifierInput = screen.getByPlaceholderText('Qualifier');
          const conditionExtensionInput = screen.getByPlaceholderText('Extension');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionQualifierInput).toBeVisible();
          expect(conditionQualifierInput).toHaveValue('*');
          expect(conditionExtensionInput).toBeVisible();
          expect(conditionExtensionInput).toHaveValue('*');

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionQualifierInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionExtensionInput.parentElement.parentElement).toHaveClass('pristine');

          const user = userEvent.setup();
          await user.clear(conditionQualifierInput);
          await user.clear(conditionExtensionInput);

          expect(conditionQualifierInput.parentElement.parentElement).toHaveClass('valid');
          expect(conditionExtensionInput.parentElement.parentElement).toHaveClass('valid');
        });

        it('for npm', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');
          fireEvent.change(coordinatesFormatSelect, { target: { value: 'npm' } });

          const conditionPackageIdInput = screen.getByPlaceholderText('Package ID');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionPackageIdInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionPackageIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for cocoapods', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'cocoapods' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for conan', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'conan' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionChannelInput = screen.getByPlaceholderText('Channel');
          const conditionOwnerInput = screen.getByPlaceholderText('Owner');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionChannelInput).toBeVisible();
          expect(conditionChannelInput).toHaveValue('*');
          expect(conditionOwnerInput).toBeVisible();
          expect(conditionOwnerInput).toHaveValue('*');

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionChannelInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionOwnerInput.parentElement.parentElement).toHaveClass('pristine');

          const user = userEvent.setup();
          await user.clear(conditionChannelInput);
          await user.clear(conditionOwnerInput);

          expect(conditionChannelInput.parentElement.parentElement).toHaveClass('valid');
          expect(conditionOwnerInput.parentElement.parentElement).toHaveClass('valid');
        });

        it('for composer', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'composer' } });

          const conditionNamespaceInput = screen.getByPlaceholderText('Namespace');
          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNamespaceInput).toBeVisible();
          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNamespaceInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for cargo', async () => {
          const user = userEvent.setup();
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'cargo' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionTypeInput = screen.getByPlaceholderText('Type');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionTypeInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          await user.clear(conditionTypeInput);

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionTypeInput.parentElement.parentElement).not.toHaveClass('invalid');
        });

        it('for cran', async () => {
          const user = userEvent.setup();
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'cran' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionTypeInput = screen.getByPlaceholderText('Type');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionTypeInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          await user.clear(conditionTypeInput);

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionTypeInput.parentElement.parentElement).not.toHaveClass('invalid');
        });

        it('for gem', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'gem' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionPlatformInput = screen.getByPlaceholderText('Platform');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionPlatformInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionPlatformInput.parentElement.parentElement).toHaveClass('pristine');
        });

        it('for golang', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'golang' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for hf-model', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'hf-model' } });

          const conditionRepoIdInput = screen.getByPlaceholderText('Repo ID');
          const conditionModelInput = screen.getByPlaceholderText('Model');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionExtensionInput = screen.getByPlaceholderText('Extension');
          const conditionModelFormatInput = screen.getByPlaceholderText('Model Format');

          expect(conditionRepoIdInput).toBeVisible();
          expect(conditionModelInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionExtensionInput).toBeVisible();
          expect(conditionModelFormatInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionRepoIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionModelInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionExtensionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionModelFormatInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for conda', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'conda' } });

          const conditionChannelInput = screen.getByPlaceholderText('Channel');
          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionBuildInput = screen.getByPlaceholderText('Build');
          const conditionSubdirInput = screen.getByPlaceholderText('Subdir');
          const conditionTypeInput = screen.getByPlaceholderText('Type');

          expect(conditionChannelInput).toBeVisible();
          expect(conditionChannelInput).toHaveValue('*');
          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionBuildInput).toBeVisible();
          expect(conditionBuildInput).toHaveValue('*');
          expect(conditionSubdirInput).toBeVisible();
          expect(conditionSubdirInput).toHaveValue('*');
          expect(conditionTypeInput).toBeVisible();
          expect(conditionTypeInput).toHaveValue('*');

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionChannelInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionBuildInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionSubdirInput.parentElement.parentElement).toHaveClass('pristine');
          expect(conditionTypeInput.parentElement.parentElement).toHaveClass('pristine');

          const user = userEvent.setup();
          await user.clear(conditionChannelInput);
          await user.clear(conditionBuildInput);
          await user.clear(conditionSubdirInput);
          await user.clear(conditionTypeInput);

          expect(conditionChannelInput.parentElement.parentElement).toHaveClass('valid');
          expect(conditionBuildInput.parentElement.parentElement).toHaveClass('valid');
          expect(conditionSubdirInput.parentElement.parentElement).toHaveClass('valid');
          expect(conditionTypeInput.parentElement.parentElement).toHaveClass('valid');
        });

        it('for nuget', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');
          fireEvent.change(coordinatesFormatSelect, { target: { value: 'nuget' } });

          const conditionPackageIdInput = screen.getByPlaceholderText('Package ID');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionPackageIdInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionPackageIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for pecoff', async () => {
          const user = userEvent.setup();
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'pecoff' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionNamespaceInput = screen.getByPlaceholderText('Namespace');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionNamespaceInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          await user.clear(conditionNamespaceInput);

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionNamespaceInput.parentElement.parentElement).not.toHaveClass('invalid');
        });

        it('for pub', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'pub' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for rpm', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'rpm' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionArchitectureInput = screen.getByPlaceholderText('Architecture');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionArchitectureInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionArchitectureInput.parentElement.parentElement).toHaveClass('invalid');
        });

        it('for swid', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'swid' } });

          const conditionNamespaceInput = screen.getByPlaceholderText('Namespace');
          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');
          const conditionTagIdInput = screen.getByPlaceholderText('Tag ID');
          const conditionTagVersionInput = screen.getByPlaceholderText('Tag Version');
          const conditionTagCreatorNameInput = screen.getByPlaceholderText('Tag Creator Name');
          const conditionTagCreatorRegidInput = screen.getByPlaceholderText('Tag Creator Regid');
          const conditionPatchInput = screen.getByPlaceholderText('Patch');

          expect(conditionNamespaceInput).toBeVisible();
          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();
          expect(conditionTagIdInput).toBeVisible();
          expect(conditionTagVersionInput).toBeVisible();
          expect(conditionTagCreatorNameInput).toBeVisible();
          expect(conditionTagCreatorRegidInput).toBeVisible();
          expect(conditionPatchInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNamespaceInput.parentElement.parentElement).not.toHaveClass('invalid');
          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionTagIdInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionTagVersionInput.parentElement.parentElement).not.toHaveClass('invalid');
          expect(conditionTagCreatorNameInput.parentElement.parentElement).not.toHaveClass('invalid');
          expect(conditionTagCreatorRegidInput.parentElement.parentElement).not.toHaveClass('invalid');
          expect(conditionPatchInput.parentElement.parentElement).not.toHaveClass('invalid');
        });

        it('for swift', async () => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
          renderComponent(initState);

          const conditionTypeSelect = await screen.findByTestId('constraint__condition-type');
          fireEvent.change(conditionTypeSelect, { target: { value: 'Coordinates' } });
          const coordinatesFormatSelect = await screen.findByTestId('constraint__coordinates-format');

          fireEvent.change(coordinatesFormatSelect, { target: { value: 'swift' } });

          const conditionNameInput = screen.getByPlaceholderText('Name');
          const conditionVersionInput = screen.getByPlaceholderText('Version');

          expect(conditionNameInput).toBeVisible();
          expect(conditionVersionInput).toBeVisible();

          let createButton = await screen.findByText('Create');

          fireEvent.click(createButton);
          const alert = screen.getByText(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
          expect(alert).toBeVisible();

          expect(conditionNameInput.parentElement.parentElement).toHaveClass('invalid');
          expect(conditionVersionInput.parentElement.parentElement).toHaveClass('invalid');
        });
      });
    });
  });

  describe('Inherited policy', () => {
    it('renders the form with disabled update button, disabled fields and no delete button', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_NOT_ENABLED, true);
      renderComponent(initState);
      const updateButton = await screen.findByText('Update');
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
      const policyTitle = screen.getByText('Policy Settings');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveAttribute('aria-label');
      expect(deleteButton).toBeNull();
      expect(overrideParentActionsInput).toBeDisabled();
      expect(overrideParentNotificationsInput).toBeDisabled();
    });

    it('enables the update button when the actions override status changes', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      renderComponent({
        ...initState,
        orgsAndPolicies: {
          ...(initState?.orgsAndPolicies || {}),
          policy: {
            ...(initState?.orgsAndPolicies?.policy || initialState),
            isInherited: true,
            overrideActionsFlag: true,
            originalOverrideActionsFlag: true,
            overrideNotificationsFlag: true,
            originalOverrideNotificationsFlag: true,
            currentPolicy: {
              ...(initState?.orgsAndPolicies?.policy?.currentPolicy || initialState.currentPolicy),
              policyActionsOverrideAllowed: true,
              policyNotificationsOverrideAllowed: true,
            },
          },
        },
      });
      let updateButton = await screen.findByText('Update');
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
      const policyTitle = screen.getByText('Policy Settings');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentActionsInput).not.toBeDisabled();
      fireEvent.click(overrideParentActionsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('enables the update button when the notifications override status changes', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true);
      renderComponent(initState);
      let updateButton = await screen.findByText('Update');
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
      const policyTitle = screen.getByText('Policy Settings');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentNotificationsInput).not.toBeDisabled();
      fireEvent.click(overrideParentNotificationsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('saves a policy successfully when adding an action override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      fireEvent.click(overrideParentActionsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when adding a notification override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      fireEvent.click(overrideParentNotificationsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when removing an action override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const inheritParentActionsInput = await screen.findByLabelText('Inherit parent actions');
      fireEvent.click(inheritParentActionsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when removing a notification override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const inheritParentNotificationsInput = await screen.findByLabelText('Inherit parent notifications');
      fireEvent.click(inheritParentNotificationsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when updating a notification override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      await screen.findByLabelText('Inherit parent notifications');
      const table = await screen.getByRole('table', { name: 'Edit policy notifications table' });
      const checkboxes = within(table).getAllByRole('checkbox');
      expect(checkboxes[8]).toBeEnabled();

      fireEvent.click(checkboxes[8]);

      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when removing a single notification override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      await screen.findByLabelText('Inherit parent notifications');
      const table = await screen.getByRole('table', { name: 'Edit policy notifications table' });
      const removeButtons = within(table).getAllByLabelText('Remove recipient');
      expect(removeButtons[0]).toBeEnabled();

      fireEvent.click(removeButtons[0]);

      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('enables the update button when the actions override status changes for repository container', async () => {
      setInitStateAndMockHttpRequests(
        'repository_container',
        REPO_CONTAINER_ID,
        POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN,
        true
      );
      renderComponent({
        ...initState,
        orgsAndPolicies: {
          ...(initState?.orgsAndPolicies || {}),
          policy: {
            ...(initState?.orgsAndPolicies?.policy || initialState),
            isInherited: true,
            overrideActionsFlag: true,
            originalOverrideActionsFlag: true,
            overrideNotificationsFlag: true,
            originalOverrideNotificationsFlag: true,
            currentPolicy: {
              ...(initState?.orgsAndPolicies?.policy?.currentPolicy || initialState.currentPolicy),
              policyActionsOverrideAllowed: true,
              policyNotificationsOverrideAllowed: true,
            },
          },
        },
      });
      let updateButton = await screen.findByText('Update');
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
      const policyTitle = screen.getByText('Policy Settings');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentActionsInput).not.toBeDisabled();
      fireEvent.click(overrideParentActionsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('enables the update button when the notifications override status changes for repository container', async () => {
      setInitStateAndMockHttpRequests(
        'repository_container',
        REPO_CONTAINER_ID,
        POLICY_ID_OVERRIDE_ENABLED_INHERITED,
        true
      );
      const rolesUrl = getRoleMappingForCurrentOwnerUrl('global', 'global');
      const roles = [{ roleId: '1', roleName: 'developer' }];
      mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
      renderComponent(initState);
      let updateButton = await screen.findByText('Update');
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      const deleteButton = screen.queryByRole('button', { name: 'Delete Policy' });
      const policyTitle = screen.getByText('Policy Settings');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentNotificationsInput).not.toBeDisabled();
      fireEvent.click(overrideParentNotificationsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('saves a policy successfully when adding an action override from repository container, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('repository_container', REPO_CONTAINER_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('repository_container', REPO_CONTAINER_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('repository_container', REPO_CONTAINER_ID)).reply(200, {});
      renderComponent(initState);
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      fireEvent.click(overrideParentActionsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when adding a notification override from repository container, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests(
        'repository_container',
        REPO_CONTAINER_ID,
        POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN,
        true
      );
      const rolesUrl = getRoleMappingForCurrentOwnerUrl('global', 'global');
      const roles = [{ roleId: '1', roleName: 'developer' }];
      mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('repository_container', REPO_CONTAINER_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('repository_container', REPO_CONTAINER_ID)).reply(200, {});
      renderComponent(initState);
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      fireEvent.click(overrideParentNotificationsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });
  });

  describe('Inheritance section', () => {
    it('shows the inheritance section when the owner is an org', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      renderComponent({ ...initState });
      const inheritance = await screen.findByText('This Policy Inherits to:');
      expect(inheritance).toBeVisible();
    });

    it('shows the inheritance section when the owner is a repository container', async () => {
      setInitStateAndMockHttpRequests(
        'repository_container',
        REPOSITORY_CONTAINER_ID,
        POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN
      );
      renderComponent({ ...initState });
      const inheritance = await screen.findByText('This Policy Inherits to:');
      expect(inheritance).toBeVisible();
    });

    it('shows the inheritance section when the owner is a repository manager', async () => {
      setInitStateAndMockHttpRequests(
        'repository_manager',
        REPOSITORY_MANAGER_ID,
        POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN
      );
      renderComponent({ ...initState });
      const inheritance = await screen.findByText('This Policy Inherits to:');
      expect(inheritance).toBeVisible();
    });

    it('hides the inheritance section when the owner is an app', async () => {
      setInitStateAndMockHttpRequests('application', APP_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      renderComponent({ ...initState });
      let inheritance;
      try {
        inheritance = await screen.findByText('This Policy Inherits to:');
      } catch (error) {
        expect(inheritance).toBeUndefined();
      }
    });

    it('hides the inheritance section when the owner is an repository', async () => {
      setInitStateAndMockHttpRequests('repository', REPO_ID, REPOSITORY_POLICY_ID);
      renderComponent({ ...initState });

      await waitFor(() => expect(screen.getByText('Summary')).toBeVisible());

      const inheritance = screen.queryByText('This Policy Inherits to:');

      expect(inheritance).not.toBeInTheDocument();
    });

    it('disables the Update button when viewing an inherited policy without override permissions', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_NOT_ENABLED, true);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: false,
                policyNotificationsOverrideAllowed: false,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClass('disabled');
    });

    it('enables the Update button when viewing an inherited policy with both overrides allowed', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: true,
                policyNotificationsOverrideAllowed: true,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('enables the Update button when viewing an inherited policy with actions override is allowed', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: true,
                policyNotificationsOverrideAllowed: false,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('enables Update button when viewing an inherited policy when only notifications override is allowed', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: false,
                policyNotificationsOverrideAllowed: true,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('disable Update button when viewing an inherited policy when only notifications override is allowed but no edit permission', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true, []);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: false,
                policyNotificationsOverrideAllowed: true,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClass('disabled');
    });

    it('disable Update button when viewing an inherited policy when only actions override is allowed but no edit permission', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      renderComponent(
        mergeDeepRight(initState, {
          orgsAndPolicies: {
            policy: {
              ...initialState,
              isInherited: true,
              currentPolicy: {
                ...initialState.currentPolicy,
                policyActionsOverrideAllowed: true,
                policyNotificationsOverrideAllowed: false,
              },
            },
          },
        })
      );

      const updateButton = await screen.findByText('Update');
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClass('disabled');
    });
  });

  it('disable Update button when viewing an inherited policy when both actions and notifications override are allowed but no edit permission', async () => {
    setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true, []);
    renderComponent(
      mergeDeepRight(initState, {
        orgsAndPolicies: {
          policy: {
            ...initialState,
            isInherited: true,
            currentPolicy: {
              ...initialState.currentPolicy,
              policyActionsOverrideAllowed: true,
              policyNotificationsOverrideAllowed: true,
            },
          },
        },
      })
    );

    const updateButton = await screen.findByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClass('disabled');
  });

  it('disable Update button when viewing an inherited policy when both actions and notifications override are not allowed and no edit permission', async () => {
    setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true, []);
    renderComponent(
      mergeDeepRight(initState, {
        orgsAndPolicies: {
          policy: {
            ...initialState,
            isInherited: true,
            currentPolicy: {
              ...initialState.currentPolicy,
              policyActionsOverrideAllowed: false,
              policyNotificationsOverrideAllowed: false,
            },
          },
        },
      })
    );

    const updateButton = await screen.findByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClass('disabled');
  });

  describe('Pro Tier Gating', () => {
    const proTierState = () => {
      initState.productFeatures = { productFeatures: { firewall: true } };
      initState.productLicense = { license: { products: ['Sonatype Lifecycle Pro'] } };
    };

    it('shows mode switch with Default and Custom buttons when editing existing policy', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      proTierState();
      renderComponent(initState);

      expect(await screen.findByText('Default')).toBeVisible();
      expect(screen.getByText('Custom')).toBeVisible();
    });

    it('does not show Delete button for Pro tier user', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      proTierState();
      renderComponent(initState);

      await screen.findByText('Policy Settings');
      expect(screen.queryByRole('button', { name: 'Delete Policy' })).not.toBeInTheDocument();
    });

    it('shows lock icon on the Custom button', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      proTierState();
      renderComponent(initState);

      const customButton = await screen.findByText('Custom');
      expect(customButton).toBeVisible();
    });

    it('does not show mode switch when creating a new policy', async () => {
      setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, null);
      proTierState();
      renderComponent(initState);

      await screen.findByText('Policy Settings');
      expect(screen.queryByText('Default')).not.toBeInTheDocument();
    });
  });
});
