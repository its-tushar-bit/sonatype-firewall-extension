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
  selectIsRepositoryContainerOwner,
  selectIsRepositoryManagerOwner,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
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
  const isRepoContainerOwner = useSelector(selectIsRepositoryContainerOwner);
  const isRepoManagerOwner = useSelector(selectIsRepositoryManagerOwner);
  const isSbomManager = useSelector(selectIsSbomManager);

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
              (isRootOrg
                ? `Caution: Disabling overrides will reset ${overridesType} for ${overridesCount} organizations, applications and repositories.`
                : `Caution: Disabling overrides will reset ${overridesType} for ${overridesCount} organizations and applications.`)}
            {(isRepoContainerOwner || isRepoManagerOwner) &&
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
        <NxFieldset
          id="editor-policy-inherit"
          data-testid="editor-policy-inherit"
          label="This Policy Inherits to:"
          isRequired={true}
        >
          <NxRadio
            name="hasCategories"
            value={null}
            disabled={isInheritedOrUserAccessReadOnly() || isSbomManager}
            isChecked={!hasPolicyCategories}
            onChange={onHasCategoriesChange}
          >
            All Applications
            {isRootOrg && !isSbomManager && ` and Repositories`}
            {isRootOrg || ` in ${ownerName}`}
          </NxRadio>

          {!isSbomManager && (
            <NxRadio
              name="hasCategories"
              value={'hasCategories'}
              disabled={!hasCategories || isInheritedOrUserAccessReadOnly()}
              isChecked={hasPolicyCategories}
              onChange={onHasCategoriesChange}
            >
              Applications of the specified Application Categories in {ownerName}
            </NxRadio>
          )}

          {hasPolicyCategories && (
            <IqAssociationEditor
              label="Application Categories:"
              items={categories}
              selectedParam="isApplied"
              description="name"
              icon="hexagon"
              disabled={isInheritedOrUserAccessReadOnly()}
              onChange={onCategoryToggled}
              fieldType={FieldType.CheckBox}
            />
          )}
        </NxFieldset>
      )}

      {isRepoContainerOwner && <NxP>This Policy Inherits to All Repository Managers and Repositories within Them</NxP>}

      {isRepoManagerOwner && <NxP>This policy inherits to all Repositories in {ownerName}</NxP>}

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

      <NxFieldset
        id="editor-inheritance-overrides-fieldset"
        data-testid="editor-inheritance-overrides-fieldset"
        label="Inheritance Overrides"
      >
        {!isSbomManager && (
          <NxCheckbox
            id="editor-policy-actions-override"
            className="iq-policy-actions-override-checkbox"
            isChecked={!!currentPolicy.policyActionsOverrideAllowed}
            disabled={isInherited || !hasEditIqPermission}
            onChange={
              currentPolicy.policyActionsOverrideAllowed && actionsOverridesCount > 0
                ? toggleShowActionsOverridesConfirmationModal
                : togglePolicyActionsOverrideAllowed
            }
          >
            {isOrgOwner &&
              (isRootOrg
                ? 'Allow action overrides at organization, application and repositories levels'
                : 'Allow action overrides at organization and application levels')}
            {isRepoContainerOwner && 'Allow action overrides at repository manager and repository levels'}
            {isRepoManagerOwner && 'Allow action overrides at repository level'}
          </NxCheckbox>
        )}
        <NxCheckbox
          id="editor-policy-notifications-override"
          className="iq-policy-notifications-override-checkbox"
          isChecked={!!currentPolicy.policyNotificationsOverrideAllowed}
          disabled={isInheritedOrUserAccessReadOnly()}
          onChange={
            currentPolicy.policyNotificationsOverrideAllowed && notificationsOverridesCount > 0
              ? toggleShowNotificationsOverridesConfirmationModal
              : togglePolicyNotificationsOverrideAllowed
          }
        >
          {isOrgOwner &&
            (isRootOrg && !isSbomManager
              ? 'Allow notification overrides at organization, application and repositories levels'
              : 'Allow notification overrides at organization and application levels')}
          {isRepoContainerOwner && 'Allow notification overrides at repository manager and repository levels'}
          {isRepoManagerOwner && 'Allow notification overrides at repository level'}
        </NxCheckbox>
      </NxFieldset>

      <NxDivider />
    </div>
  );

  function isInheritedOrUserAccessReadOnly() {
    return isInherited || !hasEditIqPermission;
  }
}
