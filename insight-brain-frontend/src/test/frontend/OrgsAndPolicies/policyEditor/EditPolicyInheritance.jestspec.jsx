/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import EditPolicyInheritance from 'MainRoot/OrgsAndPolicies/policyEditor/editPolicyInheritance/EditPolicyInheritance';
import { actions as policyActions, initialState } from 'MainRoot/OrgsAndPolicies/policySlice';
import { within } from '@testing-library/react';

describe('EditPolicyInheritance', () => {
  let state, renderComponent;

  beforeEach(() => {
    state = {
      orgsAndPolicies: {
        root: {
          policiesByOwner: [],
          selectedOwner: {
            id: 'ownerId',
          },
        },
        policy: {
          ...initialState,
          isRootOrg: false,
          isInherited: false,
          isOrgOwner: true,
          isRepositoryContainerOwner: false,
          isRepositoryManagerOwner: false,
          isRepositoryOwner: false,
          hasEditIqPermission: true,
          categories: [],
          currentPolicy: {
            ownerId: 'ownerId',
            policyActionsOverrideAllowed: true,
            policyNotificationsOverrideAllowed: true,
            constraints: [],
          },
          originalPolicy: {
            ownerId: 'ownerId',
            policyActionsOverrideAllowed: true,
            policyNotificationsOverrideAllowed: true,
            constraints: [],
          },
        },
      },
      productFeatures: {
        productFeatures: {
          notifications: true,
          firewall: true,
          enforcement: true,
          'policy-monitoring': true,
        },
      },
      router: {
        currentState: {
          name: 'sidebarView.application',
        },
      },
    };

    renderComponent = (preloadedState = state) => render(<EditPolicyInheritance />, { preloadedState });
  });

  it('renders disabled radios if inherited', () => {
    state.orgsAndPolicies.policy.isInherited = true;

    renderComponent();

    const hasNoCategoriesRadio = screen.getByLabelText(/all applications/i);
    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeDisabled();
    expect(hasNoCategoriesRadio).toBeDisabled();
  });

  describe('SBOM Manager', () => {
    it('renders radios and checkboxes correctly if root organization policy', () => {
      state.router.currentState.name = 'sbomManager.management.edit.organization.policy';
      state.orgsAndPolicies.policy.isRootOrg = true;
      renderComponent();

      const allApplicationsRadio = screen.getByLabelText(/all applications/i);
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(allApplicationsRadio).toBeDisabled();
      expect(
        screen.queryByLabelText(/Applications of the specified Application Categories in/i)
      ).not.toBeInTheDocument();
      expect(
        screen.queryByLabelText(/Allow action overrides at organization and application levels/i)
      ).not.toBeInTheDocument();
      expect(notificationsOverrideCheckbox).not.toBeDisabled();
    });

    it('renders radios and checkboxes correctly if child organization policy', () => {
      state.router.currentState.name = 'sbomManager.management.edit.organization.policy';
      state.orgsAndPolicies.policy.currentPolicyOwner = {
        name: 'child_org1',
      };
      renderComponent();

      const allApplicationsRadio = screen.getByLabelText(/all applications in child_org1/i);
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(allApplicationsRadio).toBeDisabled();
      expect(
        screen.queryByLabelText(/Applications of the specified Application Categories in/i)
      ).not.toBeInTheDocument();
      expect(
        screen.queryByLabelText(/Allow action overrides at organization and application levels/i)
      ).not.toBeInTheDocument();
      expect(notificationsOverrideCheckbox).not.toBeDisabled();
    });
  });

  it('renders disabled radios if it does not have permission', () => {
    state.orgsAndPolicies.policy.hasEditIqPermission = false;

    renderComponent();

    const hasNoCategoriesRadio = screen.getByLabelText(/all applications/i);
    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeDisabled();
    expect(hasNoCategoriesRadio).toBeDisabled();
  });

  it('renders disabled hasCategoriesRadio radio if there isn"t any categories', () => {
    renderComponent();

    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeDisabled();
  });

  it('renders IqAssociationEditor', () => {
    renderComponent();

    const nonVisibleAssociationEditorLabel = screen.queryByText('Application Categories:');
    expect(nonVisibleAssociationEditorLabel).not.toBeInTheDocument();

    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);
    fireEvent.click(hasCategoriesRadio);

    const associationEditorLabel = screen.getByText('Application Categories:');
    expect(associationEditorLabel).toBeVisible();
  });

  it('checks and renders correct checkbox values', () => {
    renderComponent();

    const hasNoCategoriesRadio = screen.getByLabelText(/all applications/i);
    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasNoCategoriesRadio).toBeChecked();
    expect(hasCategoriesRadio).not.toBeChecked();

    fireEvent.click(hasCategoriesRadio);

    expect(hasNoCategoriesRadio).not.toBeChecked();
    expect(hasCategoriesRadio).toBeChecked();
  });

  it('enables category checkboxes when user does have permissions to edit', () => {
    state.orgsAndPolicies.policy.hasEditIqPermission = true;
    const testCategory = givenAppCategoryBasedInheritanceSelectedAndCategoriesExist();

    renderComponent();

    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeChecked();

    const editorPolicyInherit = screen.getByTestId('editor-policy-inherit');

    // the radio-checkbox for the category was rendered and rendered disabled.
    within(editorPolicyInherit).getByLabelText(testCategory.name);
    const inputs = within(editorPolicyInherit).getByRole('checkbox');

    expect(inputs).not.toBeDisabled();
  });

  it('disables category checkboxes when user does not have permissions to edit', () => {
    state.orgsAndPolicies.policy.hasEditIqPermission = false;
    const testCategory = givenAppCategoryBasedInheritanceSelectedAndCategoriesExist();

    renderComponent();

    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeChecked();

    const editorPolicyInherit = screen.getByTestId('editor-policy-inherit');

    // the radio-checkbox for the category was rendered and rendered disabled.
    within(editorPolicyInherit).getByLabelText(testCategory.name);
    const inputs = within(editorPolicyInherit).getByRole('checkbox');

    expect(inputs).toBeDisabled();
  });

  it('disables category checkboxes when repository is inherited', () => {
    state.orgsAndPolicies.policy.nameIncludesRepository = true;
    state.orgsAndPolicies.policy.isInherited = true;

    const testCategory = givenAppCategoryBasedInheritanceSelectedAndCategoriesExist();

    renderComponent();

    const hasCategoriesRadio = screen.getByLabelText(/Applications of the specified Application Categories in/i);

    expect(hasCategoriesRadio).toBeChecked();

    const editorPolicyInherit = screen.getByTestId('editor-policy-inherit');

    // the radio-checkbox for the category was rendered and rendered disabled.
    within(editorPolicyInherit).getByLabelText(testCategory.name);
    const inputs = within(editorPolicyInherit).getByRole('checkbox');

    expect(inputs).toBeDisabled();
  });

  it('categories not rendered for repository container', () => {
    state.router.currentState.name = 'sidebarView.repository_container';
    state.orgsAndPolicies.policy.isOrgOwner = false;
    state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
    renderComponent();

    expect(screen.queryByLabelText(/all applications/i)).toBeNull();
    expect(screen.queryByLabelText(/Applications of the specified Application Categories in/i)).toBeNull();
  });

  it('categories not rendered for repository manager', () => {
    state.router.currentState.name = 'sidebarView.repository_manager';
    state.orgsAndPolicies.policy.isOrgOwner = false;
    state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
    renderComponent();

    expect(screen.queryByLabelText(/all applications/i)).toBeNull();
    expect(screen.queryByLabelText(/Applications of the specified Application Categories in/i)).toBeNull();
  });

  describe('actions overrides section', () => {
    it('renders actions override checkbox', () => {
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('renders actions override checkbox for root org', () => {
      state.orgsAndPolicies.policy.isRootOrg = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization, application and repositories levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('renders actions override checkbox for repository container', () => {
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('renders unchecked actions override checkbox when policy actions override is not allowed for repository containers', () => {
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).not.toBeChecked();
    });

    it('renders actions override checkbox for repository manager', () => {
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('renders unchecked actions override checkbox when policy actions override is not allowed for repository manager', () => {
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).not.toBeChecked();
    });

    it('renders unchecked actions override checkbox when policy actions override is not allowed ', () => {
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).not.toBeChecked();
    });

    it('renders disabled actions override checkbox when is inherited policy', () => {
      state.orgsAndPolicies.policy.isInherited = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when is inherited policy for repository container', () => {
      state.orgsAndPolicies.policy.isInherited = true;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when user has no edit permission for repository containers', () => {
      state.orgsAndPolicies.policy.hasEditIqPermission = false;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when is inherited policy for repository manager', () => {
      state.orgsAndPolicies.policy.isInherited = true;
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when user has no edit permission for repository manager', () => {
      state.orgsAndPolicies.policy.hasEditIqPermission = false;
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when user has no edit permission', () => {
      state.orgsAndPolicies.policy.hasEditIqPermission = false;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      expect(actionsOverrideCheckbox).not.toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action for repository container', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );
      expect(actionsOverrideCheckbox).not.toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('dispatches toggleShowActionsOverridesConfirmationModal action when disabling if action overrides exist for repository containers', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowActionsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();

      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
      expect(screen.getByText('Caution: Disabling overrides will reset actions for 1 repository.')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches toggleShowActionsOverridesConfirmationModal action when disabling if action overrides exist for repository manager', async () => {
      const spy = jest.spyOn(policyActions, 'toggleShowActionsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { proxy: 'warn' },
      };
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);
      expect(actionsOverrideCheckbox).toBeChecked();

      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
      expect(
        await screen.findByText('Caution: Disabling overrides will reset actions for 1 repository.')
      ).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Continue' })).toBeVisible();
      expect(await screen.findByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action for repository manager', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      state.orgsAndPolicies.policy.currentPolicy.policyActionsOverrideAllowed = false;
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);
      expect(actionsOverrideCheckbox).not.toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('dispatches toggleShowActionsOverridesConfirmationModal action when disabling if action overrides exist', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowActionsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();

      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
      expect(
        screen.getByText('Caution: Disabling overrides will reset actions for 1 organizations and applications.')
      ).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches toggleShowActionsOverridesConfirmationModal action when disabling if action overrides exist for root organization', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowActionsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.isRootOrg = true;
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization, application and repositories levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();

      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
      expect(
        screen.getByText(
          'Caution: Disabling overrides will reset actions for 1 organizations, applications and repositories.'
        )
      ).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action when disabling if action overrides do not exist', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();

      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).not.toBeChecked();
    });

    it('dispatches toggleShowActionsOverridesConfirmationModal action when cancelling', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowActionsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      fireEvent.click(actionsOverrideCheckbox);
      expect(spy).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText('Caution: Disabling overrides will reset actions for 1 organizations and applications.')
      ).toBeVisible();
      const cancel = screen.getByRole('button', { name: 'Cancel' });

      fireEvent.click(cancel);

      expect(spy).toHaveBeenCalledTimes(2);
      expect(
        screen.queryByText('Caution: Disabling overrides will reset actions for 1 organizations and applications.')
      ).toBeNull();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action when continuing', () => {
      const spyTogglePolicyActionsOverrideAllowed = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      const spyToggleShowActionsOverridesConfirmationModal = jest.spyOn(
        policyActions,
        'toggleShowActionsOverridesConfirmationModal'
      );
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText('Caution: Disabling overrides will reset actions for 1 organizations and applications.')
      ).toBeVisible();
      const continueButton = screen.getByRole('button', { name: 'Continue' });

      fireEvent.click(continueButton);

      expect(actionsOverrideCheckbox).not.toBeChecked();
      expect(spyTogglePolicyActionsOverrideAllowed).toHaveBeenCalled();
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(2);
      expect(
        screen.queryByText('Caution: Disabling overrides will reset actions for 1 organizations and applications.')
      ).toBeNull();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action when continuing for repository container', () => {
      const spyTogglePolicyActionsOverrideAllowed = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      const spyToggleShowActionsOverridesConfirmationModal = jest.spyOn(
        policyActions,
        'toggleShowActionsOverridesConfirmationModal'
      );
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
        '15602dd5ba934c318ad011ca4e4f5cfe': { build: 'warn' },
      };
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at repository manager and repository levels/i
      );
      expect(actionsOverrideCheckbox).toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(1);
      expect(screen.getByText('Caution: Disabling overrides will reset actions for 2 repositories.')).toBeVisible();
      const continueButton = screen.getByRole('button', { name: 'Continue' });

      fireEvent.click(continueButton);

      expect(actionsOverrideCheckbox).not.toBeChecked();
      expect(spyTogglePolicyActionsOverrideAllowed).toHaveBeenCalled();
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(2);
      expect(screen.queryByText('Caution: Disabling overrides will reset actions for 2 repositories.')).toBeNull();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action when continuing for repository manager', () => {
      const spyTogglePolicyActionsOverrideAllowed = jest.spyOn(policyActions, 'togglePolicyActionsOverrideAllowed');
      const spyToggleShowActionsOverridesConfirmationModal = jest.spyOn(
        policyActions,
        'toggleShowActionsOverridesConfirmationModal'
      );
      state.orgsAndPolicies.policy.originalPolicy.policyActionsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': { proxy: 'warn' },
        '15602dd5ba934c318ad011ca4e4f5cfe': { proxy: 'warn' },
      };
      state.router.currentState.name = 'sidebarView.repository_manager';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryManagerOwner = true;
      renderComponent();
      const actionsOverrideCheckbox = screen.getByLabelText(/Allow action overrides at repository level/i);
      expect(actionsOverrideCheckbox).toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(1);
      expect(screen.getByText('Caution: Disabling overrides will reset actions for 2 repositories.')).toBeVisible();
      const continueButton = screen.getByRole('button', { name: 'Continue' });

      fireEvent.click(continueButton);

      expect(actionsOverrideCheckbox).not.toBeChecked();
      expect(spyTogglePolicyActionsOverrideAllowed).toHaveBeenCalled();
      expect(spyToggleShowActionsOverridesConfirmationModal).toHaveBeenCalledTimes(2);
      expect(screen.queryByText('Caution: Disabling overrides will reset actions for 2 repositories.')).toBeNull();
    });
  });

  describe('notification overrides section', () => {
    it('renders checked notifications override checkbox when policy notifications override is allowed', () => {
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(notificationsOverrideCheckbox).toBeVisible();
      expect(notificationsOverrideCheckbox).toBeChecked();
    });

    it('renders checked notifications override checkbox when policy notifications override is allowed for root org', () => {
      state.orgsAndPolicies.policy.isRootOrg = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization, application and repositories levels/i
      );

      expect(notificationsOverrideCheckbox).toBeVisible();
      expect(notificationsOverrideCheckbox).toBeChecked();
    });

    it('renders checked notifications override checkbox when policy notifications override is allowed for repository container', () => {
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );

      expect(notificationsOverrideCheckbox).toBeVisible();
      expect(notificationsOverrideCheckbox).toBeChecked();
    });

    it('renders unchecked notifications override checkbox when policy notifications override is not allowed ', () => {
      state.orgsAndPolicies.policy.currentPolicy.policyNotificationsOverrideAllowed = false;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(notificationsOverrideCheckbox).toBeVisible();
      expect(notificationsOverrideCheckbox).not.toBeChecked();
    });

    it('renders unchecked notifications override checkbox when policy notifications override is not allowed for repository container', () => {
      state.orgsAndPolicies.policy.currentPolicy.policyNotificationsOverrideAllowed = false;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );

      expect(notificationsOverrideCheckbox).toBeVisible();
      expect(notificationsOverrideCheckbox).not.toBeChecked();
    });

    it('renders disabled notifications override checkbox when policy is inherited', () => {
      state.orgsAndPolicies.policy.isInherited = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(notificationsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled notifications override checkbox when policy is inherited for repository container', () => {
      state.orgsAndPolicies.policy.isInherited = true;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );

      expect(notificationsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled notifications override checkbox when user has no edit permission', () => {
      state.orgsAndPolicies.policy.hasEditIqPermission = false;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );

      expect(notificationsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled notifications override checkbox when user has no edit permission for repository container', () => {
      state.orgsAndPolicies.policy.hasEditIqPermission = false;
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();

      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );

      expect(notificationsOverrideCheckbox).toBeDisabled();
    });

    it('dispatches togglePolicyNotificationsOverrideAllowed action when enabling', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyNotificationsOverrideAllowed');
      state.orgsAndPolicies.policy.currentPolicy.policyNotificationsOverrideAllowed = false;
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );
      expect(notificationsOverrideCheckbox).not.toBeChecked();

      fireEvent.click(notificationsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(notificationsOverrideCheckbox).toBeChecked();
    });

    it('dispatches toggleShowNotificationsOverridesConfirmationModal action when disabling if notification overrides exist', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowNotificationsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyNotificationsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
        },
      };
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );
      expect(notificationsOverrideCheckbox).toBeChecked();

      fireEvent.click(notificationsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(notificationsOverrideCheckbox).toBeChecked();
      expect(
        screen.getByText('Caution: Disabling overrides will reset notifications for 1 organizations and applications.')
      ).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches toggleShowNotificationsOverridesConfirmationModal action when disabling if notification overrides exist for repository container', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowNotificationsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyNotificationsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
        },
      };
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );
      expect(notificationsOverrideCheckbox).toBeChecked();

      fireEvent.click(notificationsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(notificationsOverrideCheckbox).toBeChecked();
      expect(screen.getByText('Caution: Disabling overrides will reset notifications for 1 repository.')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Continue' })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    });

    it('dispatches togglePolicyNotificationsOverrideAllowed action when disabling if notification overrides do not exist', () => {
      const spy = jest.spyOn(policyActions, 'togglePolicyNotificationsOverrideAllowed');
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );
      expect(notificationsOverrideCheckbox).toBeChecked();

      fireEvent.click(notificationsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(notificationsOverrideCheckbox).not.toBeChecked();
    });

    it('dispatches toggleShowNotificationsOverridesConfirmationModal action when cancelling', () => {
      const spy = jest.spyOn(policyActions, 'toggleShowNotificationsOverridesConfirmationModal');
      state.orgsAndPolicies.policy.originalPolicy.policyNotificationsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
        },
      };
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );
      fireEvent.click(notificationsOverrideCheckbox);
      expect(spy).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText('Caution: Disabling overrides will reset notifications for 1 organizations and applications.')
      ).toBeVisible();
      const cancel = screen.getByRole('button', { name: 'Cancel' });

      fireEvent.click(cancel);

      expect(spy).toHaveBeenCalledTimes(2);
      expect(
        screen.queryByText(
          'Caution: Disabling overrides will reset notifications for 1 organizations and applications.'
        )
      ).toBeNull();
    });

    it('dispatches togglePolicyNotificationsOverrideAllowed action when continuing', () => {
      const spyTogglePolicyNotificationsOverrideAllowed = jest.spyOn(
        policyActions,
        'togglePolicyNotificationsOverrideAllowed'
      );
      const spyToggleShowNotificationsOverridesConfirmationModal = jest.spyOn(
        policyActions,
        'toggleShowNotificationsOverridesConfirmationModal'
      );
      state.orgsAndPolicies.policy.originalPolicy.policyNotificationsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
        },
      };
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at organization and application levels/i
      );
      expect(notificationsOverrideCheckbox).toBeChecked();
      fireEvent.click(notificationsOverrideCheckbox);
      expect(spyToggleShowNotificationsOverridesConfirmationModal).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText('Caution: Disabling overrides will reset notifications for 1 organizations and applications.')
      ).toBeVisible();
      const continueButton = screen.getByRole('button', { name: 'Continue' });

      fireEvent.click(continueButton);

      expect(notificationsOverrideCheckbox).not.toBeChecked();
      expect(spyTogglePolicyNotificationsOverrideAllowed).toHaveBeenCalled();
      expect(spyToggleShowNotificationsOverridesConfirmationModal).toHaveBeenCalledTimes(2);
      expect(
        screen.queryByText(
          'Caution: Disabling overrides will reset notifications for 1 organizations and applications.'
        )
      ).toBeNull();
    });

    it('dispatches togglePolicyNotificationsOverrideAllowed action when continuing for repository container', () => {
      const spyTogglePolicyNotificationsOverrideAllowed = jest.spyOn(
        policyActions,
        'togglePolicyNotificationsOverrideAllowed'
      );
      const spyToggleShowNotificationsOverridesConfirmationModal = jest.spyOn(
        policyActions,
        'toggleShowNotificationsOverridesConfirmationModal'
      );
      state.orgsAndPolicies.policy.originalPolicy.policyNotificationsOverrides = {
        '05602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email1@email.com', stageIds: ['build', 'release'] }],
        },
        '15602dd5ba934c318ad011ca4e4f5cfe': {
          userNotifications: [{ emailAddress: 'email2@email.com', stageIds: ['build', 'release'] }],
        },
      };
      state.router.currentState.name = 'sidebarView.repository_container';
      state.orgsAndPolicies.policy.isOrgOwner = false;
      state.orgsAndPolicies.policy.isRepositoryContainerOwner = true;
      renderComponent();
      const notificationsOverrideCheckbox = screen.getByLabelText(
        /Allow notification overrides at repository manager and repository levels/i
      );
      expect(notificationsOverrideCheckbox).toBeChecked();
      fireEvent.click(notificationsOverrideCheckbox);
      expect(spyToggleShowNotificationsOverridesConfirmationModal).toHaveBeenCalledTimes(1);
      expect(
        screen.getByText('Caution: Disabling overrides will reset notifications for 2 repositories.')
      ).toBeVisible();
      const continueButton = screen.getByRole('button', { name: 'Continue' });

      fireEvent.click(continueButton);

      expect(notificationsOverrideCheckbox).not.toBeChecked();
      expect(spyTogglePolicyNotificationsOverrideAllowed).toHaveBeenCalled();
      expect(spyToggleShowNotificationsOverridesConfirmationModal).toHaveBeenCalledTimes(2);
      expect(
        screen.queryByText('Caution: Disabling overrides will reset notifications for 2 repositories.')
      ).toBeNull();
    });
  });

  function givenAppCategoryBasedInheritanceSelectedAndCategoriesExist() {
    const categoryForTest = {
      id: 'ad9c8255617e41708c6a76d4e62cffc9',
      name: 'some-category',
      description: 'some-description',
      isApplied: false,
      organizationId: 'ROOT_ORGANIZATION_ID',
      color: 'yellow',
    };

    state.orgsAndPolicies.policy.hasPolicyCategories = true;
    state.orgsAndPolicies.policy.categories = [categoryForTest];

    return categoryForTest;
  }
});
