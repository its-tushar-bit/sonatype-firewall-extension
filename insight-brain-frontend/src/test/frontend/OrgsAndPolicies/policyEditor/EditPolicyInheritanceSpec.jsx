/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import EditPolicyInheritance from 'MainRoot/OrgsAndPolicies/policyEditor/editPolicyInheritance/EditPolicyInheritance';
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';

const currentPolicy = { ownerId: 'ownerId', policyActionsOverrideAllowed: true };

describe('EditPolicyInheritance', () => {
  let renderComponent;

  beforeEach(() => {
    spyOn(policySelectors, 'selectCategories').and.returnValue([]);
    spyOn(policySelectors, 'selectIsInherited').and.returnValue(false);
    spyOn(policySelectors, 'selectHasEditIqPermission').and.returnValue(true);
    spyOn(policySelectors, 'selectCurrentPolicy').and.returnValue(currentPolicy);

    renderComponent = () => render(<EditPolicyInheritance />);
  });

  it('renders disabled radios', () => {
    policySelectors.selectIsInherited.and.returnValue(true);

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

  describe('actions overrides section', () => {
    beforeEach(() => {
      policySelectors.selectIsInherited.and.returnValue(false);
    });

    it('renders actions override checkbox', () => {
      policySelectors.selectCurrentPolicy.and.returnValue({ ...currentPolicy, policyActionsOverrideAllowed: true });
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).toBeChecked();
    });

    it('renders unchecked actions override checkbox when policy actions override is not allowed ', () => {
      policySelectors.selectCurrentPolicy.and.returnValue({ ...currentPolicy, policyActionsOverrideAllowed: false });
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeVisible();
      expect(actionsOverrideCheckbox).not.toBeChecked();
    });

    it('renders disabled actions override checkbox when is inherited policy', () => {
      policySelectors.selectIsInherited.and.returnValue(true);
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('renders disabled actions override checkbox when user has no edit permission', () => {
      policySelectors.selectHasEditIqPermission.and.returnValue(false);
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );

      expect(actionsOverrideCheckbox).toBeDisabled();
    });

    it('dispatches togglePolicyActionsOverrideAllowed action', () => {
      const spy = spyOn(policyActions, 'togglePolicyActionsOverrideAllowed').and.callThrough();
      policySelectors.selectCurrentPolicy.and.callThrough();
      renderComponent();

      const actionsOverrideCheckbox = screen.getByLabelText(
        /Allow action overrides at organization and application levels/i
      );
      expect(actionsOverrideCheckbox).not.toBeChecked();
      fireEvent.click(actionsOverrideCheckbox);

      expect(spy).toHaveBeenCalled();
      expect(actionsOverrideCheckbox).toBeChecked();
    });
  });
});
