/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxCheckbox,
  NxDivider,
  NxFieldset,
  NxH2,
  NxModal,
  NxP,
  NxRadio,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { propEq } from 'ramda';

import {
  selectCategories,
  selectHasPolicyCategories,
  selectIsRootOrg,
  selectIsInherited,
  selectCurrentPolicyOwnerName,
  selectCurrentPolicy,
  selectHasEditIqPermission,
  selectShowActionsOverridesConfirmationModal,
  selectShowNotificationsOverridesConfirmationModal,
  selectActionsOverridesCount,
  selectNotificationsOverridesCount,
  selectIsOrgOwner,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectIsRepositoryContainer, selectIsRepositoryManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { IqAssociationEditor, FieldType } from 'MainRoot/react/IqAssociationEditor';

export default function EditPolicyInheritance() {
  const dispatch = useDispatch();

  const isRootOrg = useSelector(selectIsRootOrg);
  const hasPolicyCategories = useSelector(selectHasPolicyCategories);
  const ownerName = useSelector(selectCurrentPolicyOwnerName);
  const categories = useSelector(selectCategories);
  const currentPolicy = useSelector(selectCurrentPolicy);
  const isInherited = useSelector(selectIsInherited);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const hasCategories = categories?.length;
  const showActionsOverridesConfirmationModal = useSelector(selectShowActionsOverridesConfirmationModal);
  const showNotificationsOverridesConfirmationModal = useSelector(selectShowNotificationsOverridesConfirmationModal);
  const actionsOverridesCount = useSelector(selectActionsOverridesCount);
  const notificationsOverridesCount = useSelector(selectNotificationsOverridesCount);
  const isOrgOwner = useSelector(selectIsOrgOwner);
  const isRepositoryContainer = useSelector(selectIsRepositoryContainer);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);

  const onCategoryToggled = (category) => {
    const categoryIndexForToggle = categories.findIndex(propEq('id', category.id));
    dispatch(policyActions.toggleCategoryIsApplied(categoryIndexForToggle));
  };
  const onHasCategoriesChange = (hasCategories) => dispatch(policyActions.setHasPolicyCategories(!!hasCategories));
  const togglePolicyActionsOverrideAllowed = () => dispatch(policyActions.togglePolicyActionsOverrideAllowed());
  const togglePolicyNotificationsOverrideAllowed = () =>
    dispatch(policyActions.togglePolicyNotificationsOverrideAllowed());
  const toggleShowActionsOverridesConfirmationModal = () =>
    dispatch(policyActions.toggleShowActionsOverridesConfirmationModal());
  const toggleShowNotificationsOverridesConfirmationModal = () =>
    dispatch(policyActions.toggleShowNotificationsOverridesConfirmationModal());

  const repositoryPluralized = (count) => (count > 1 ? 'repositories' : 'repository');

  const createOverridesConfirmationModal = (overridesType, overridesCount, onCancel, onContinue) => {
    return (
      <NxModal
        id="policy-overrides-confirmation-modal"
        role="alertdialog"
        onCancel={onCancel}
        aria-labelledby="policy-overrides-confirmation-modal-header"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2" id="policy-overrides-confirmation-modal-header">
            Existing Overrides Configured
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            {isOrgOwner &&
              `Caution: Disabling overrides will reset ${overridesType} for ${overridesCount} organizations and applications.`}
            {isRepositoryContainer &&
              `Caution: Disabling overrides will reset ${overridesType} for ${overridesCount} ${repositoryPluralized(
                overridesCount
              )}.`}
            {isRepositoryManager &&
              `Caution: Disabling overrides will reset ${overridesType} for ${overridesCount} ${repositoryPluralized(
                overridesCount
              )}.`}
          </NxWarningAlert>
        </div>
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <NxButton onClick={onCancel}>Cancel</NxButton>
            <NxButton variant="primary" onClick={onContinue}>
              Continue
            </NxButton>
          </div>
        </footer>
      </NxModal>
    );
  };

  return (
    <div id="policy-edit-inheritance">
      <NxH2>Inheritance</NxH2>

      {isOrgOwner && (
        <NxFieldset id="editor-policy-inherit" label="This Policy Inherits to:" isRequired={true}>
          <NxRadio
            name="hasCategories"
            value={null}
            disabled={isInherited || !hasEditIqPermission}
            isChecked={!hasPolicyCategories}
            onChange={onHasCategoriesChange}
          >
            All Applications {isRootOrg ? 'and Repositories' : `in ${ownerName}`}
          </NxRadio>

          <NxRadio
            name="hasCategories"
            value={'hasCategories'}
            disabled={!hasCategories || isInherited || !hasEditIqPermission}
            isChecked={hasPolicyCategories}
            onChange={onHasCategoriesChange}
          >
            Applications of the specified Application Categories in {ownerName}
          </NxRadio>

          {hasPolicyCategories && (
            <IqAssociationEditor
              label="Application Categories:"
              items={categories}
              selectedParam="isApplied"
              description="name"
              icon="hexagon"
              disabled={isInherited}
              onChange={onCategoryToggled}
              fieldType={FieldType.CheckBox}
            />
          )}
        </NxFieldset>
      )}

      {isRepositoryContainer && <NxP>This Policy Inherits to All Repository Managers and Repositories within Them</NxP>}

      {isRepositoryManager && <NxP>This policy inherits to all Repositories in {ownerName}</NxP>}

      {showActionsOverridesConfirmationModal &&
        createOverridesConfirmationModal(
          'actions',
          actionsOverridesCount,
          toggleShowActionsOverridesConfirmationModal,
          () => {
            togglePolicyActionsOverrideAllowed();
            toggleShowActionsOverridesConfirmationModal();
          }
        )}

      {showNotificationsOverridesConfirmationModal &&
        createOverridesConfirmationModal(
          'notifications',
          notificationsOverridesCount,
          toggleShowNotificationsOverridesConfirmationModal,
          () => {
            togglePolicyNotificationsOverrideAllowed();
            toggleShowNotificationsOverridesConfirmationModal();
          }
        )}

      <NxFieldset id="editor-inheritance-overrides-fieldset" label="Inheritance Overrides">
        <NxCheckbox
          id="editor-policy-actions-override"
          isChecked={!!currentPolicy.policyActionsOverrideAllowed}
          disabled={isInherited || !hasEditIqPermission}
          onChange={
            currentPolicy.policyActionsOverrideAllowed && actionsOverridesCount > 0
              ? toggleShowActionsOverridesConfirmationModal
              : togglePolicyActionsOverrideAllowed
          }
        >
          {isRepositoryContainer && 'Allow action overrides at repository manager and repository levels'}
          {isOrgOwner && 'Allow action overrides at organization and application levels'}
          {isRepositoryManager && 'Allow action overrides at repository level'}
        </NxCheckbox>
        <NxCheckbox
          id="editor-policy-notifications-override"
          isChecked={!!currentPolicy.policyNotificationsOverrideAllowed}
          disabled={isInherited || !hasEditIqPermission}
          onChange={
            currentPolicy.policyNotificationsOverrideAllowed && notificationsOverridesCount > 0
              ? toggleShowNotificationsOverridesConfirmationModal
              : togglePolicyNotificationsOverrideAllowed
          }
        >
          {isRepositoryContainer && 'Allow notification overrides at repository manager and repository levels'}
          {isOrgOwner && 'Allow notification overrides at organization and application levels'}
          {isRepositoryManager && 'Allow notification overrides at repository level'}
        </NxCheckbox>
      </NxFieldset>

      <NxDivider />
    </div>
  );
}
