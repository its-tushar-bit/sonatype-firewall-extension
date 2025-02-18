/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';
import * as R from 'ramda';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectLoadError,
  selectLoading,
  selectIsInherited,
  selectIsOrgOwner,
  selectCurrentPolicy,
  selectIfSubmitButtonShouldBeDisabled,
  selectSubmitError,
  selectCurrentSubmitMaskState,
  selectPolicyDeleteError,
  selectOverrideNeedsToBeAdded,
  selectOverrideNeedsToBeRemoved,
  selectOverrideNeedsToBeUpdated,
  selectHasEditIqPermission,
  selectIsDirty,
  selectIsRepositoryContainerOwner,
  selectIsRepositoryManagerOwner,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectLoading as selectOwnerDetailTreeLoading } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';

import {
  NxButton,
  NxStatefulForm,
  NxH1,
  NxModal,
  NxPageTitle,
  NxTile,
  NxWarningAlert,
  NxFontAwesomeIcon,
  NxInfoAlert,
  NxTextLink,
} from '@sonatype/react-shared-components';
import EditPolicySummary from './editPolicySummary/EditPolicySummary';
import EditPolicyInheritance from './editPolicyInheritance/EditPolicyInheritance';
import ConstraintsEditor from './constraints/ConstraintsEditor';
import PolicyNotificationsEditor from './policyNotificationsEditor/PolicyNotificationsEditor';
import PolicyActionsEditor from './policyActionsEditor/PolicyActionsEditor';
import { selectEntityId, selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import { selectIsRepositoriesRelated, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectHasFirewallLicense, selectHasLifecycleLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { faLock } from '@fortawesome/pro-regular-svg-icons';
import './PolicyEditor.scss';

export default function PolicyEditor() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const deleteError = useSelector(selectPolicyDeleteError);
  const dirtyPolicy = useSelector(selectCurrentPolicy);
  const ownerDetailTreeLoading = useSelector(selectOwnerDetailTreeLoading);
  const isOrgOwner = useSelector(selectIsOrgOwner);
  const isRepoContainerOwner = useSelector(selectIsRepositoryContainerOwner);
  const isRepoManagerOwner = useSelector(selectIsRepositoryManagerOwner);
  const isInherited = useSelector(selectIsInherited);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const isDirty = useSelector(selectIsDirty);
  const validationError = useSelector(selectIfSubmitButtonShouldBeDisabled);
  const submitError = useSelector(selectSubmitError);
  const submitMaskState = useSelector(selectCurrentSubmitMaskState);
  const entityId = useSelector(selectEntityId);
  const overrideNeedsToBeAdded = useSelector(selectOverrideNeedsToBeAdded);
  const overrideNeedsToBeRemoved = useSelector(selectOverrideNeedsToBeRemoved);
  const overrideNeedsToBeUpdated = useSelector(selectOverrideNeedsToBeUpdated);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isLoading = ownerDetailTreeLoading || loading;
  const isSbomManager = useSelector(selectIsSbomManager);
  const selectedOwnerProperties = useSelector(selectOwnerProperties);

  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const hasFirewallLicense = useSelector(selectHasFirewallLicense);

  const showInheritedSection = isOrgOwner || isRepoContainerOwner || isRepoManagerOwner;
  const loadPolicyEditor = () => dispatch(actions.loadPolicyEditor());
  const savePolicy = () => dispatch(actions.savePolicy());
  const updateOverrides = () => dispatch(actions.updateOverrides());
  const removePolicy = () => dispatch(actions.removePolicy());

  const onSave = () => {
    if (isDisabled()) {
      return;
    }

    if (isRepositoriesRelated && !isInherited) {
      return savePolicy();
    }

    if (!hasEditIqPermission || !isDirty) {
      return;
    }
    if (isInherited && (overrideNeedsToBeAdded || overrideNeedsToBeRemoved || overrideNeedsToBeUpdated)) {
      updateOverrides();
      return;
    }
    savePolicy();
  };

  const onRemovePolicy = () => {
    removePolicy();
  };

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
    dispatch(actions.clearDeleteError());
  };

  useEffect(() => {
    loadPolicyEditor();
  }, [entityId]);

  const OWNER_TYPE_ID_MAP = {
    application: `applicationPublicId`,
    organization: `organizationId`,
  };

  const policyManagementHref = uiRouterState.href(
    `${!hasLifecycleLicense && hasFirewallLicense ? 'firewall.' : ''}management.edit.${
      selectedOwnerProperties.ownerType
    }.policy`,
    {
      [OWNER_TYPE_ID_MAP[selectedOwnerProperties.ownerType]]: selectedOwnerProperties.ownerId,
      policyId: dirtyPolicy?.id,
    }
  );

  const linkHref =
    hasFirewallLicense || hasLifecycleLicense
      ? policyManagementHref
      : 'https://links.sonatype.com/nexus-lifecycle-sbom';

  return (
    <div id="policy-editor-summary">
      <NxPageTitle>
        <NxH1>{dirtyPolicy?.id ? (isInherited ? 'View' : 'Edit') : 'New'} Policy</NxH1>
        {isSbomManager && <NxFontAwesomeIcon icon={faLock} data-testid="policy-editor-lock-icon" />}
      </NxPageTitle>

      {isSbomManager && dirtyPolicy && (
        <NxInfoAlert>
          {R.cond([
            [R.always(hasLifecycleLicense), R.always('Switch to Lifecycle to manage your policies. ')],
            [R.always(hasFirewallLicense), R.always('Switch to Repository Firewall to manage your policies. ')],
            [R.T, R.always('Custom policies are available with Lifecycle. ')],
          ])()}
          <NxTextLink className="policy-editor-lifecycle-link" href={linkHref} noReferrer newTab>
            {R.cond([
              [R.always(hasLifecycleLicense), R.always('Manage in Lifecycle')],
              [R.always(hasFirewallLicense), R.always('Manage in Repository Firewall')],
              [R.T, R.always('Start your demo today')],
            ])()}
          </NxTextLink>
        </NxInfoAlert>
      )}

      <NxTile>
        <NxStatefulForm
          onSubmit={onSave}
          submitBtnText={dirtyPolicy?.id ? 'Update' : 'Create'}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          submitBtnClasses={classNames({
            disabled: isDisabled(),
          })}
          doLoad={loadPolicyEditor}
          loadError={loadError}
          loading={isLoading}
          validationErrors={getValidationErrors()}
          submitError={submitError}
          additionalFooterBtns={
            dirtyPolicy?.id && !isInherited && !isSbomManager ? (
              <NxButton
                id="delete-policy-button"
                variant="tertiary"
                onClick={() => setIsDeleteModalOpen(true)}
                disabled={isDisabled()}
                type="button"
              >
                Delete
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
            <EditPolicySummary />
            {showInheritedSection && <EditPolicyInheritance />}
            <ConstraintsEditor />
            {!isSbomManager && <PolicyActionsEditor />}
            <PolicyNotificationsEditor />
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>

      {isDeleteModalOpen && (
        <NxModal id="policy-delete-modal" aria-labelledby="policy-delete-modal-header" onClose={closeDeleteModal}>
          <NxStatefulForm
            submitMaskState={submitMaskState}
            submitError={deleteError}
            onCancel={closeDeleteModal}
            submitBtnText="Continue"
            submitMaskMessage="Deleting…"
            onSubmit={onRemovePolicy}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="category-delete-modal-header">
                Delete Policy
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                You are about to permanently remove {dirtyPolicy?.name.value}. This action cannot be undone.
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </div>
  );

  function getValidationErrors() {
    if (!validationError || isDisabled()) {
      // when the form is readonly we suppress validation errors, we do not have a way to fully disable the button,
      // we mimic disabling it by giving it a disabled class, short circuiting submit logic, and suppressing validation
      return null;
    } else {
      // Validation errors have this logic to avoid empty tooltips when disabled
      return validationError === true ? '' : validationError;
    }
  }

  function isDisabled() {
    if (isRepositoriesRelated && !isInherited) {
      return false;
    }

    if (isInherited) {
      // Check if policy allows overrides and we have permission
      return !(
        hasEditIqPermission &&
        (dirtyPolicy?.policyActionsOverrideAllowed || dirtyPolicy?.policyNotificationsOverrideAllowed)
      );
    }

    return !hasEditIqPermission;
  }
}
