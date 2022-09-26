/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';

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
  selectIsActionOverrideEnabled,
  selectOverrideNeedsToBeRemoved,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectLoading as selectOwnerDetailTreeLoading } from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';

import {
  NxButton,
  NxForm,
  NxH1,
  NxModal,
  NxPageTitle,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import EditPolicySummary from './editPolicySummary/EditPolicySummary';
import EditPolicyInheritance from './editPolicyInheritance/EditPolicyInheritance';
import ConstraintsEditor from './constraints/ConstraintsEditor';
import PolicyNotificationsEditor from './policyNotificationsEditor/PolicyNotificationsEditor';
import PolicyActionsEditor from './policyActionsEditor/PolicyActionsEditor';
import { selectSelectedOwner } from '../orgsAndPoliciesSelectors';

export default function PolicyEditor() {
  const dispatch = useDispatch();

  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const deleteError = useSelector(selectPolicyDeleteError);
  const dirtyPolicy = useSelector(selectCurrentPolicy);
  const ownerDetailTreeLoading = useSelector(selectOwnerDetailTreeLoading);
  const isOrgOwner = useSelector(selectIsOrgOwner);
  const isInherited = useSelector(selectIsInherited);
  const validationError = useSelector(selectIfSubmitButtonShouldBeDisabled);
  const submitError = useSelector(selectSubmitError);
  const submitMaskState = useSelector(selectCurrentSubmitMaskState);
  const isActionOverrideEnabled = useSelector(selectIsActionOverrideEnabled);
  const overrideNeedsToBeRemoved = useSelector(selectOverrideNeedsToBeRemoved);
  const selectedOwner = useSelector(selectSelectedOwner);
  const isLoading = ownerDetailTreeLoading || loading;

  const loadPolicyEditor = () => dispatch(actions.loadPolicyEditor());
  const checkEditIqPermission = () => dispatch(actions.checkEditIqPermission());
  const savePolicy = () => dispatch(actions.savePolicy());
  const removeActionsOverride = () => dispatch(actions.removeActionsOverride());
  const saveActionsOverride = () => dispatch(actions.saveActionsOverride());
  const removePolicy = () => dispatch(actions.removePolicy());

  const onSave = () => {
    if (isActionOverrideEnabled) {
      overrideNeedsToBeRemoved ? removeActionsOverride() : saveActionsOverride();
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
  }, []);

  useEffect(() => {
    checkEditIqPermission();
  }, [selectedOwner]);

  return (
    <div id="policy-editor-summary">
      <NxPageTitle>
        <NxH1>{dirtyPolicy?.id ? (isInherited ? 'View' : 'Edit') : 'New'} Policy</NxH1>
      </NxPageTitle>

      <NxTile>
        <NxForm
          onSubmit={onSave}
          submitBtnText={dirtyPolicy?.id ? 'Update' : 'Create'}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          doLoad={loadPolicyEditor}
          loadError={loadError}
          loading={isLoading}
          // Validation errors have this logic to avoid empty tooltips when disabled
          validationErrors={!validationError ? null : validationError === true ? '' : validationError}
          submitError={submitError}
          additionalFooterBtns={
            dirtyPolicy?.id && !isInherited ? (
              <NxButton
                id="delete-policy-button"
                variant="tertiary"
                onClick={() => setIsDeleteModalOpen(true)}
                type="button"
              >
                Delete
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
            <EditPolicySummary />
            {isOrgOwner && <EditPolicyInheritance></EditPolicyInheritance>}
            <ConstraintsEditor></ConstraintsEditor>
            <PolicyActionsEditor></PolicyActionsEditor>
            <PolicyNotificationsEditor></PolicyNotificationsEditor>
          </NxTile.Content>
        </NxForm>
      </NxTile>

      {isDeleteModalOpen && (
        <NxModal id="policy-delete-modal" aria-labelledby="policy-delete-modal-header" onClose={closeDeleteModal}>
          <NxForm
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
          </NxForm>
        </NxModal>
      )}
    </div>
  );
}
