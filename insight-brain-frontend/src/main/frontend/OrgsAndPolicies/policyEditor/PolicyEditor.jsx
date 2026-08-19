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
  NxDivider,
  NxErrorAlert,
  NxList,
  NxFontAwesomeIcon,
  NxInfoAlert,
  NxTextLink,
  NxFormGroup,
  NxTextInput,
  nxTextInputStateHelpers,
  NxTooltip,
} from '@sonatype/react-shared-components';
import EditPolicySummary from './editPolicySummary/EditPolicySummary';
import EditPolicyInheritance from './editPolicyInheritance/EditPolicyInheritance';
import ConstraintsEditor from './constraints/ConstraintsEditor';
import PolicyNotificationsEditor from './policyNotificationsEditor/PolicyNotificationsEditor';
import PolicyActionsEditor from './policyActionsEditor/PolicyActionsEditor';
import PolicyReadOnlyView from './PolicyReadOnlyView';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import { selectEntityId, selectOwnerProperties } from '../orgsAndPoliciesSelectors';
import {
  selectIsRepositoriesRelated,
  selectIsSbomManager,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectHasFirewallLicense, selectHasLifecycleLicense } from 'MainRoot/productFeatures/productLicenseSelectors';
import {
  selectHasCustomPolicies,
  selectIsEnterprisePreviewMode,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { faLock, faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
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
  const { policyId } = useSelector(selectRouterCurrentParams);
  const overrideNeedsToBeAdded = useSelector(selectOverrideNeedsToBeAdded);
  const overrideNeedsToBeRemoved = useSelector(selectOverrideNeedsToBeRemoved);
  const overrideNeedsToBeUpdated = useSelector(selectOverrideNeedsToBeUpdated);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const isLoading = ownerDetailTreeLoading || loading;
  const isSbomManager = useSelector(selectIsSbomManager);
  const selectedOwnerProperties = useSelector(selectOwnerProperties);

  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const hasFirewallLicense = useSelector(selectHasFirewallLicense);

  const isEnterprisePreviewMode = useSelector(selectIsEnterprisePreviewMode);
  const hasCustomPolicies = useSelector(selectHasCustomPolicies);
  const isFeatureGated = !hasCustomPolicies;

  const handleToggleMode = () => {
    const newMode = !isEnterprisePreviewMode;
    // Zero dirty synchronously so the router navigation guard (which reads the
    // raw state path) does not fire the unsaved-changes modal for non-saveable
    // preview edits made before the toggle.
    dispatch(actions.resetIsDirty());
    dispatch(productFeaturesActions.setEnterprisePreviewMode(newMode));
    if (newMode) {
      // Switching to Custom — reload from server so Custom starts clean
      loadPolicyEditor();
    } else {
      // Switching back to Lifecycle Pro — reload from server to discard Custom edits + scroll to top
      loadPolicyEditor();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Reset preview mode on unmount
  useEffect(() => {
    return () => {
      dispatch(productFeaturesActions.setEnterprisePreviewMode(false));
    };
  }, [dispatch]);

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

  const requiredValidator = (val) => (val === 'DELETE' ? null : 'Must type DELETE to confirm');

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [confirmDeleteState, setConfirmDeleteState] = useState(
    nxTextInputStateHelpers.initialState('', requiredValidator)
  );

  const onConfirmDeleteChange = (val) => {
    setConfirmDeleteState(nxTextInputStateHelpers.userInput(requiredValidator, val));
  };
  const modalFormValidationErrors = confirmDeleteState.validationErrors ? 'Required fields are missing' : null;

  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
    setConfirmDeleteState(nxTextInputStateHelpers.initialState('', requiredValidator));
    dispatch(actions.clearDeleteError());
  };

  useEffect(() => {
    loadPolicyEditor();
  }, [entityId, policyId]);

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
      <div className="iq-policy-editor__header">
        <NxPageTitle>
          <NxH1>Policy Settings</NxH1>
          {isSbomManager && <NxFontAwesomeIcon icon={faLock} data-testid="policy-editor-lock-icon" />}
        </NxPageTitle>
        {isFeatureGated && dirtyPolicy?.id && (
          <div className="iq-policy-editor__mode-switch">
            <NxButton
              variant={!isEnterprisePreviewMode ? 'primary' : 'secondary'}
              onClick={() => !isEnterprisePreviewMode || handleToggleMode()}
              className="iq-policy-editor__mode-button iq-policy-editor__mode-button--left"
            >
              Default
            </NxButton>
            <NxTooltip title="Enterprise Feature">
              <NxButton
                variant={isEnterprisePreviewMode ? 'primary' : 'secondary'}
                onClick={() => isEnterprisePreviewMode || handleToggleMode()}
                className="iq-policy-editor__mode-button iq-policy-editor__mode-button--right"
              >
                Custom <NxFontAwesomeIcon icon={faLock} />
              </NxButton>
            </NxTooltip>
          </div>
        )}
      </div>

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

      <NxTile
        className={classNames({
          'iq-banner-flush-top': isFeatureGated && (isEnterprisePreviewMode || !dirtyPolicy?.id),
          'iq-enterprise-mode-footer': isFeatureGated && (isEnterprisePreviewMode || !dirtyPolicy?.id),
        })}
      >
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
            dirtyPolicy?.id && !isInherited && !isSbomManager && !isEnterprisePreviewMode && !isFeatureGated ? (
              <NxButton
                id="delete-policy-button"
                variant="tertiary"
                onClick={() => setIsDeleteModalOpen(true)}
                disabled={isDisabled()}
                type="button"
              >
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Policy</span>
              </NxButton>
            ) : !dirtyPolicy?.id && !isEnterprisePreviewMode ? (
              <NxButton variant="secondary" onClick={() => window.history.back()} type="button">
                Back
              </NxButton>
            ) : undefined
          }
        >
          <NxTile.Content>
            {isFeatureGated && !isEnterprisePreviewMode && dirtyPolicy?.id ? (
              <>
                {/* Default Settings: Read-only Summary and Constraints; Editable Inheritance; Editable Actions and Notifications */}
                <PolicyReadOnlyView
                  policy={dirtyPolicy}
                  showActionsAndNotifications={false}
                  showSummary={true}
                  showInheritance={false}
                  showConstraints={false}
                />
                <NxDivider />
                {showInheritedSection && <EditPolicyInheritance />}
                <PolicyReadOnlyView
                  policy={dirtyPolicy}
                  showActionsAndNotifications={false}
                  showSummary={false}
                  showInheritance={false}
                  showConstraints={true}
                  showConstraintsPopover={true}
                  onSwitchToCustomMode={handleToggleMode}
                />
                <NxDivider />
              </>
            ) : isFeatureGated ? (
              <>
                {/* Custom Settings: Editable Summary, Inheritance, and Constraints */}
                <EnterpriseFullWidthBanner
                  title="Custom Policies"
                  description="Define and enforce policies that match your organization's risk and compliance needs."
                />
                <EditPolicySummary previewMode />
                {showInheritedSection && <EditPolicyInheritance />}
                <ConstraintsEditor hidePopover previewMode />
              </>
            ) : (
              <>
                <EditPolicySummary />
                {showInheritedSection && <EditPolicyInheritance />}
                <ConstraintsEditor />
              </>
            )}
            {/* Actions and Notifications: Editable in BOTH Default and Custom Settings */}
            {!isSbomManager && <PolicyActionsEditor />}
            <PolicyNotificationsEditor />
            {isFeatureGated && (isEnterprisePreviewMode || !dirtyPolicy?.id) && (
              <NxInfoAlert>
                This is an Enterprise feature. Changes can&apos;t be saved.
                {dirtyPolicy?.id && (
                  <>
                    {' '}
                    <NxTextLink onClick={handleToggleMode}>Return to Lifecycle Pro</NxTextLink>
                  </>
                )}
              </NxInfoAlert>
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>

      {isDeleteModalOpen && (
        <NxModal id="policy-delete-modal" aria-labelledby="policy-delete-modal-header" onClose={closeDeleteModal}>
          <NxStatefulForm
            submitMaskState={submitMaskState}
            submitError={deleteError}
            onCancel={closeDeleteModal}
            submitBtnText="Confirm Deletion"
            submitMaskMessage="Deleting…"
            onSubmit={onRemovePolicy}
            validationErrors={modalFormValidationErrors}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="category-delete-modal-header">
                Delete Policy
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxErrorAlert>
                You are about to permanently delete the policy &quot;{dirtyPolicy?.name.value}&quot;. This action cannot
                be undone.
              </NxErrorAlert>
              <p className="list-item-title">Deleting this policy will:</p>
              <NxList bulleted>
                <NxList.Item>
                  <NxList.Text>
                    Immediately unquarantine all components previously quarantined by this policy.
                  </NxList.Text>
                </NxList.Item>
                <NxList.Item>
                  <NxList.Text>
                    Permanently remove all associated waivers, which cannot be recovered without restoring from a
                    backup.
                  </NxList.Text>
                </NxList.Item>
              </NxList>
              <NxFormGroup label="To confirm, please type DELETE in the box below:" isRequired>
                <NxTextInput {...confirmDeleteState} validatable={true} onChange={onConfirmDeleteChange} />
              </NxFormGroup>
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
