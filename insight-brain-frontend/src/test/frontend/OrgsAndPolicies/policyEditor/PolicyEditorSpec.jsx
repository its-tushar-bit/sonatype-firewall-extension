/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, axiosMockAdapter, fireEvent } from 'TestRoot/SpecUtil';
import {
  getActionStageUrl,
  getApplicableCategoriesUrl,
  getApplicablePolicies,
  getConditionTypeUrl,
  getConditionValueTypeUrl,
  getPolicyActionsOverridesUrl,
  getPolicyCRUDUrl,
  getPolicyTagUrl,
  getPolicyUrl,
  getOwnerDetailsUrl,
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

describe('PolicyEditorSpec', () => {
  let initState;
  const POLICY_ID_OVERRIDE_ENABLED_INHERITED = '9d5c30f793a54446a9601cf36c18e9e3';
  const POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN = '12f2086417ab44f9a63ba5e91786c570';
  const POLICY_ID_OVERRIDE_NOT_ENABLED = '2a1cb71651d14a60b0fa77ef829f5ec0';
  const ROOT_ORG_ID = 'ROOT_ORGANIZATION_ID';
  const ORG_ID = '05602dd5ba934c318ad011ca4e4f5cfe';
  const APP_ID = 'testapp';
  let mockAxiosCalls;

  const setInitStateAndMockHttpRequests = (ownerType, ownerId, policyId) => {
    initState = {
      router: {
        currentState: { name: ownerType },
      },
    };
    if (ownerType === 'organization') {
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
    mockAxiosCalls.onGet(applicableCategoriesUrl).reply(200, applicableCategories[ownerType][ownerId]);
    mockAxiosCalls.onGet(applicablePoliciesUrl).reply(200, applicablePolicies[ownerType][ownerId]);

    mockAxiosCalls.onGet(policyTagUrl).reply(200, policyTag[ownerType][ownerId][policyId]);
  };

  beforeAll(() => {
    mockAxiosCalls = axiosMockAdapter();
  });

  const renderComponent = (preloadedState) => render(<PolicyEditor />, { preloadedState });

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
          expect(deleteButton).not.toHaveClassName('disabled');
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
          expect(updateButton).not.toHaveClassName('disabled');
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
          expect(updateButton).not.toHaveClassName('disabled');
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

          const error = await screen.findByText('An error occurred loading data. Error 404');
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
        expect(updateButton).not.toHaveClassName('disabled');
      });
    });
  });

  describe('Inherited policy', () => {
    it('renders the form with disabled update button, disabled fields and no delete button', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_NOT_ENABLED);
      renderComponent(initState);
      const updateButton = await screen.findByText('Update');
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      const deleteButton = screen.queryByText('Delete');
      const policyTitle = screen.getByText('View Policy');
      expect(policyTitle).toBeVisible();
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveAttribute('aria-label');
      expect(deleteButton).toBeNull();
      expect(overrideParentActionsInput).toBeDisabled();
    });

    it('enables the update button when the actions override status changes', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      renderComponent(initState);
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
      expect(updateButton).not.toHaveClassName('disabled');
    });

    it('saves a policy successfully when adding an action override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      mockAxiosCalls
        .onPut(getPolicyActionsOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const overrideParentActionsInput = await screen.findByLabelText('Override parent actions');
      fireEvent.click(overrideParentActionsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClassName('disabled');
      fireEvent.click(updateButton);
      const savingMask = screen.getByText('Saving…');
      expect(savingMask).toBeVisible();
      const successMask = await screen.findByText(/Success/);
      expect(successMask).toBeVisible();
    });

    it('saves a policy successfully when removing an action override, shows the save mask with the success message', async () => {
      setInitStateAndMockHttpRequests('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED);
      mockAxiosCalls
        .onDelete(getPolicyActionsOverridesUrl('organization', ORG_ID, POLICY_ID_OVERRIDE_ENABLED_INHERITED))
        .reply(200, savedPolicy);
      mockAxiosCalls.onGet(getOwnerDetailsUrl('organization', ORG_ID)).reply(200, {});
      renderComponent(initState);
      const inheritParentActionsInput = await screen.findByLabelText('Inherit parent actions');
      fireEvent.click(inheritParentActionsInput);
      const updateButton = screen.getByText('Update');
      expect(updateButton).not.toHaveClassName('disabled');
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
      renderComponent(initState);
      const inheritance = await screen.findByText('This Policy Inherits to:');
      expect(inheritance).toBeVisible();
    });

    it('hides the inheritance section when the owner is an app', async () => {
      setInitStateAndMockHttpRequests('application', APP_ID, POLICY_ID_OVERRIDE_ENABLED_OVERRIDDEN);
      renderComponent(initState);
      let inheritance;
      try {
        inheritance = await screen.findByText('This Policy Inherits to:');
      } catch (error) {
        expect(inheritance).toBeUndefined();
      }
    });
  });
});
