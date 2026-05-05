/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxButton,
  NxTextInput,
  NxFormGroup,
  NxModal,
  NxFontAwesomeIcon,
  NxWarningAlert,
  NxInfoAlert,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { faLock } from '@fortawesome/pro-regular-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  selectIsLoading,
  selectLicenseThreatGroupIsEditMode,
  selectLicenseThreatGroupIsDirty,
  selectLicenseThreatGroupSubmitError,
  selectLicenseThreatGroupLoadError,
  selectDirtyLicenseThreatGroup,
  selectSubmitMaskState,
  selectValidationError,
  selectDeleteMaskState,
  selectDeleteError,
  selectAvailableLicenses,

  selectLicenseThreatGroupId,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import LtgTransferList from './LtgTransferList';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import LicenseThreatGroupReadOnlyView from './LicenseThreatGroupReadOnlyView';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import { selectHasCustomLicenseThreatGroups, selectIsEnterprisePreviewMode } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import './_LicenseThreatGroupEditor.scss';

const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function LicenseThreatGroupEditor() {
  const dispatch = useDispatch();

  const setLicenseThreatGroupName = (val) => dispatch(actions.setLicenseThreatGroupName(val));
  const setLicenseThreatGroupThreatLevel = (val) => dispatch(actions.setLicenseThreatGroupThreatLevel(val));
  const setPickedLicenses = useCallback((val) => dispatch(actions.setPickedLicenses(val)), []);
  const saveLicenseThreatGroup = () => dispatch(actions.saveLicenseThreatGroup());
  const removeLicenseThreatGroup = () => dispatch(actions.removeLicenseThreatGroup());
  const clearDeleteError = () => dispatch(actions.clearDeleteError());
  const doLoad = () => dispatch(actions.loadLicenseThreatGroupEditor());

  const loading = useSelector(selectIsLoading);
  const loadError = useSelector(selectLicenseThreatGroupLoadError);
  const isEditMode = useSelector(selectLicenseThreatGroupIsEditMode);
  const isDirty = useSelector(selectLicenseThreatGroupIsDirty);
  const submitError = useSelector(selectLicenseThreatGroupSubmitError);
  const dirtyLTG = useSelector(selectDirtyLicenseThreatGroup);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const deleteError = useSelector(selectDeleteError);
  const validationError = useSelector(selectValidationError);
  const allLicenses = useSelector(selectAvailableLicenses);
  const isEnterprisePreviewMode = useSelector(selectIsEnterprisePreviewMode);
  const licenseThreatGroupId = useSelector(selectLicenseThreatGroupId);
  const hasCustomLicenseThreatGroups = useSelector(selectHasCustomLicenseThreatGroups);
  const isFeatureGated = !hasCustomLicenseThreatGroups;

  const handleToggleMode = () => {
    const newMode = !isEnterprisePreviewMode;
    // Zero dirty synchronously so the router navigation guard (which reads the
    // raw state path) does not fire the unsaved-changes modal for non-saveable
    // preview edits made before the toggle.
    dispatch(actions.resetIsDirty());
    dispatch(productFeaturesActions.setEnterprisePreviewMode(newMode));
    if (newMode) {
      doLoad(); // Reload for clean Custom view
    }
    else {
      doLoad(); // Reload to discard Custom edits
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Reset preview mode on unmount
  useEffect(() => {
    return () => {
      dispatch(productFeaturesActions.setEnterprisePreviewMode(false));
    };
  }, [dispatch]);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const openDeleteModal = () => {
    clearDeleteError();
    setIsDeleteModalOpen(true);
  };
  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
  };

  useEffect(() => {
    doLoad();
  }, [licenseThreatGroupId]);

  return (
    <>
      <div className="iq-license-threat-group-editor__header">
        <NxPageTitle>
          <NxH1>License Threat Group Settings</NxH1>
        </NxPageTitle>
        {isFeatureGated && isEditMode && (
          <div className="iq-license-threat-group-editor__mode-switch">
            <NxButton
              variant={!isEnterprisePreviewMode ? 'primary' : 'secondary'}
              onClick={() => !isEnterprisePreviewMode || handleToggleMode()}
              className="iq-license-threat-group-editor__mode-button iq-license-threat-group-editor__mode-button--left"
            >
              Default
            </NxButton>
            <NxTooltip title="Enterprise Feature">
              <NxButton
                variant={isEnterprisePreviewMode ? 'primary' : 'secondary'}
                onClick={() => isEnterprisePreviewMode || handleToggleMode()}
                className="iq-license-threat-group-editor__mode-button iq-license-threat-group-editor__mode-button--right"
              >
                Custom <NxFontAwesomeIcon icon={faLock} />
              </NxButton>
            </NxTooltip>
          </div>
        )}
      </div>
      <NxTile
        id="license-threat-group-editor"
        className={classNames({
          'iq-banner-flush-top': isFeatureGated && (isEnterprisePreviewMode || !isEditMode),
          'iq-enterprise-mode-footer': isFeatureGated,
        })}
      >
        <NxStatefulForm
          onSubmit={saveLicenseThreatGroup}
          submitBtnText={isEditMode ? 'Update' : 'Create'}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          doLoad={doLoad}
          loadError={loadError}
          loading={loading}
          validationErrors={getValidationMessage(isDirty, validationError)}
          submitError={submitError}
          additionalFooterBtns={
            isEditMode && !isEnterprisePreviewMode && !isFeatureGated ? (
              <NxButton id="delete-ltg-button" onClick={openDeleteModal} variant="tertiary" type="button">
                Delete
              </NxButton>
            ) : isEnterprisePreviewMode || !isEditMode ? (
              <NxButton variant="secondary" onClick={() => window.history.back()} type="button">
                Back
              </NxButton>
            ) : undefined
          }
        >
          {isFeatureGated && !isEnterprisePreviewMode && isEditMode ? (
            <LicenseThreatGroupReadOnlyView licenseThreatGroup={dirtyLTG} allLicenses={allLicenses} />
          ) : isFeatureGated ? (
            <NxTile.Content>
              <EnterpriseFullWidthBanner
                title="Custom License Threat Groups"
                description="Define license risk based on your legal standards to improve accuracy and reduce manual review."
              />
              <div className="nx-form-row">
                <NxFormGroup id="editor-label-name" label="Group Name" isRequired>
                  <NxTextInput validatable={true} {...dirtyLTG.name} onChange={setLicenseThreatGroupName} />
                </NxFormGroup>
                <ThreatDropdownSelector
                  threatLevel={dirtyLTG.threatLevel}
                  onSelectThreatLevel={setLicenseThreatGroupThreatLevel}
                />
              </div>
              <LtgTransferList
                licenseIds={dirtyLTG.licenseIds}
                allLicenses={allLicenses}
                setSelectedLicenses={setPickedLicenses}
              />
              {(isEnterprisePreviewMode || !isEditMode) && (
                <NxInfoAlert>
                  This is an Enterprise feature. Changes can&apos;t be saved.
                  {isEditMode && (
                    <>
                      {' '}
                      <NxTextLink onClick={handleToggleMode}>Return to Lifecycle Pro</NxTextLink>
                    </>
                  )}
                </NxInfoAlert>
              )}
            </NxTile.Content>
          ) : (
            <NxTile.Content>
              <div className="nx-form-row">
                <NxFormGroup id="editor-label-name" label="Group Name" isRequired>
                  <NxTextInput validatable={true} {...dirtyLTG.name} onChange={setLicenseThreatGroupName} />
                </NxFormGroup>
                <ThreatDropdownSelector
                  threatLevel={dirtyLTG.threatLevel}
                  onSelectThreatLevel={setLicenseThreatGroupThreatLevel}
                />
              </div>
              <LtgTransferList
                licenseIds={dirtyLTG.licenseIds}
                allLicenses={allLicenses}
                setSelectedLicenses={setPickedLicenses}
              />
            </NxTile.Content>
          )}
        </NxStatefulForm>
      </NxTile>
      {isDeleteModalOpen && (
        <NxModal
          id="ltg-config-delete-modal"
          aria-labelledby="ltg-config-delete-modal-header"
          onClose={closeDeleteModal}
        >
          <NxStatefulForm
            onSubmit={removeLicenseThreatGroup}
            submitMaskState={deleteMaskState}
            onCancel={closeDeleteModal}
            submitBtnText="Delete"
            submitMaskMessage="Deleting…"
            submitError={deleteError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="ltg-config-delete-modal-header">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete License Threat Group</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                You are about to permanently remove {dirtyLTG.name.value}. This action cannot be undone.
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}
