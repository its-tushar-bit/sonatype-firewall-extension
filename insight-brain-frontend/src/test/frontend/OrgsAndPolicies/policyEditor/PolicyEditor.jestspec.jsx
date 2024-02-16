/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, axiosMockAdapter, fireEvent, within } from 'TestRoot/SpecUtil';
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
} from './mockData';
import { initialState } from 'MainRoot/OrgsAndPolicies/policySlice';

describe('PolicyEditorSpec', () => {
  let initState;
  const POLICY_ID_OVERRIDE_ENABLED_INHERITED = '9d5c30f793a54446a9601cf36c18e9e3';
  const POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN = '12f2086417ab44f9a63ba5e91786c570';
  const POLICY_ID_OVERRIDE_NOT_ENABLED = '2a1cb71651d14a60b0fa77ef829f5ec0';
  const ROOT_ORG_ID = 'ROOT_ORGANIZATION_ID';
  const REPOSITORY_CONTAINER_ID = 'REPOSITORY_CONTAINER_ID';
  const REPOSITORY_MANAGER_ID = 'F2BC2A0B-E7D0DDA9-425601AB-F0AAD535-FDF19232';
  const ORG_ID = '05602dd5ba934c318ad011ca4e4f5cfe';
  const APP_ID = 'testapp';
  const REPO_ID = 'REPOSITORY_CONTAINER_ID';
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
          },
        },
      },
      productFeatures: {
        productFeatures: {
          firewall: true,
        },
      },
    };
    if (ownerType === 'repository_container') {
      initState.router.currentParams = { repositoryContainerId: REPO_ID };
    } else if (ownerType === 'repository_manager') {
      initState.router.currentParams = { repositoryManagerId: ownerId };
    } else if (ownerType === 'organization') {
      initState.router.currentParams = { organizationId: ownerId };
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

  it('disables the Update button if there are no changes', async () => {
    setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true);
    renderComponent(initState);
    const updateButton = await screen.findByText('Update');
    expect(updateButton).toBeVisible();
    expect(updateButton).toHaveClass('disabled');
  });

  it('disables the Delete button when there is no permission', async () => {
    setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true, []);
    renderComponent(initState);
    const deleteButton = await screen.findByText('Delete');
    expect(deleteButton).toBeVisible();
    expect(deleteButton).toBeDisabled();
  });

  describe('Local policy', () => {
    describe('Update policy', () => {
      describe('Rendering', () => {
        beforeEach(() => {
          setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
          renderComponent(initState);
        });
        it('renders the form with disabled update button and delete button, shows the loading state before', async () => {
          const updateButton = await screen.findByText('Update');
          const deleteButton = await screen.findByText('Delete');
          const policyTitle = screen.getByText('Edit Policy');
          expect(policyTitle).toBeVisible();
          expect(updateButton).toBeVisible();
          fireEvent.click(updateButton);

          const alert = screen.getByText('There were validation errors. There are no changes to save.');
          expect(alert).toBeVisible();

          expect(deleteButton).toBeVisible();
          expect(deleteButton).not.toHaveClass('disabled');
        });

        it('enables the Update button with valid data', async () => {
          let updateButton = await screen.findByText('Update');
          const policyNameInput = await screen.findByLabelText('Policy Name');
          expect(updateButton).toBeVisible();
          fireEvent.change(policyNameInput, { target: { value: 'New Value' } });
          updateButton = screen.getByText('Update');
        });

        it('enables the Update button when removing a constraint', async () => {
          let updateButton = await screen.findByText('Update');
          expect(updateButton).toBeVisible();
          const deleteConstraintBtn = await screen.findAllByLabelText('Delete constraint');
          fireEvent.click(deleteConstraintBtn[0]);
          updateButton = screen.getByText('Update');
          expect(updateButton).not.toHaveClass('disabled');
        });

        it('disables the Update button for policy name duplicated', async () => {
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
          const deleteButton = await screen.findByText('Delete');
          fireEvent.click(deleteButton);
          const deleteDesc = screen.getByText('Delete Policy');
          const continueButton = screen.getByText('Continue');
          expect(deleteDesc).toBeVisible();
          fireEvent.click(continueButton);
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
          const deleteButton = await screen.findByText('Delete');
          fireEvent.click(deleteButton);
          const deleteDesc = screen.getByText('Delete Policy');
          const continueButton = screen.getByText('Continue');
          expect(deleteDesc).toBeVisible();
          fireEvent.click(continueButton);
          const savingMask = screen.getByText('Deleting…');
          expect(savingMask).toBeVisible();

          const error = await screen.findByText('An error occurred loading data. Error 404');
          expect(error).toBeVisible();
        });
      });
    });

    describe('Create policy', () => {
      it('renders the form with disabled create button and no delete button', async () => {
        setInitStateAndMockHttpRequests('organization', ROOT_ORG_ID);
        renderComponent(initState);
        const updateButton = await screen.findByText('Create');
        const deleteButton = screen.queryByText('Delete');
        const policyTitle = screen.getByText('New Policy');
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
        const policyTitle = screen.getByText('New Policy');

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
    });
  });

  describe('Inherited policy', () => {
    it('renders the form with disabled update button, disabled fields and no delete button', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_NOT_ENABLED, true);
      renderComponent(initState);
      const updateButton = await screen.findByText('Update');
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
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
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
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
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
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
      setInitStateAndMockHttpRequests('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true);
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
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentActionsInput).not.toBeDisabled();
      fireEvent.click(overrideParentActionsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('enables the update button when the notifications override status changes for repository container', async () => {
      setInitStateAndMockHttpRequests('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED, true);
      const rolesUrl = getRoleMappingForCurrentOwnerUrl('global', 'global');
      const roles = [{ roleId: '1', roleName: 'developer' }];
      mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
      renderComponent(initState);
      let updateButton = await screen.findByText('Update');
      const overrideParentNotificationsInput = await screen.findByLabelText('Override parent notifications');
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(deleteButton).toBeNull();
      expect(overrideParentNotificationsInput).not.toBeDisabled();
      fireEvent.click(overrideParentNotificationsInput);
      updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClass('disabled');
    });

    it('saves a policy successfully when adding an action override from repository container, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('repository_container', REPO_ID)).reply(200, {});
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
      setInitStateAndMockHttpRequests('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN, true);
      const rolesUrl = getRoleMappingForCurrentOwnerUrl('global', 'global');
      const roles = [{ roleId: '1', roleName: 'developer' }];
      mockAxiosCalls.onGet(rolesUrl).reply(200, { membersByRole: roles });
      mockAxiosCalls
        .onPut(getPolicyOverridesUrl('repository_container', REPO_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('repository_container', REPO_ID)).reply(200, {});
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
  });
});
