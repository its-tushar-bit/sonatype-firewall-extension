/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxTextInput,
  NxColorPicker,
  NxFormGroup,
  NxModal,
  NxWarningAlert,
  NxInfoAlert,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from '../labelsSlice';
import {
  selectLabelsSubmitError,
  selectLabelsLoadError,
  selectLabelsLoading,
  selectLabelsCurrentLabel,
  selectLabelsSubmitMaskState,
  selectValidationError,
  selectLabelsIsDirty,
  selectLabelsDeleteError,
  selectLabelsDeleteMaskState,
  selectLabelsIsEditMode,
} from '../labelsSelectors';
import { backendToRscColorMap, rscToBackendColorMap } from '../utility/util';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import ComponentLabelsReadOnlyView from './ComponentLabelsReadOnlyView';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import {
  selectHasCustomComponentLabels,
  selectIsEnterprisePreviewMode,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import './_createComponentLabel.scss';

const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

export default function CreateComponentLabel() {
  const dispatch = useDispatch();
  const loadError = useSelector(selectLabelsLoadError);
  const submitError = useSelector(selectLabelsSubmitError);
  const loading = useSelector(selectLabelsLoading);
  const isDirty = useSelector(selectLabelsIsDirty);
  const submitMaskState = useSelector(selectLabelsSubmitMaskState);
  const validationError = useSelector(selectValidationError);
  const deleteError = useSelector(selectLabelsDeleteError);
  const deleteMaskState = useSelector(selectLabelsDeleteMaskState);
  const { labelId } = useSelector(selectRouterCurrentParams);
  const isEditMode = useSelector(selectLabelsIsEditMode);
  const isEnterprisePreviewMode = useSelector(selectIsEnterprisePreviewMode);
  const hasCustomComponentLabels = useSelector(selectHasCustomComponentLabels);
  const isFeatureGated = !hasCustomComponentLabels;

  const currentLabel = useSelector(selectLabelsCurrentLabel);
  const { color, description, label } = currentLabel;

  const saveLabelColor = (color) => dispatch(actions.setLabelColor(color));
  const onChangeLabel = (val) => dispatch(actions.setLabelName(val));
  const onChangeDescription = (val) => dispatch(actions.setLabelDescription(val));
  const saveLabel = () => dispatch(actions.saveLabel());
  const removeLabel = () => dispatch(actions.removeLabel());
  const doLoad = () => dispatch(actions.loadLabelsEditor());
  const handleToggleMode = () => {
    const newMode = !isEnterprisePreviewMode;
    // Zero dirty synchronously so the router navigation guard (which reads the
    // raw state path) does not fire the unsaved-changes modal for non-saveable
    // preview edits made before the toggle.
    dispatch(actions.resetIsDirty());
    dispatch(productFeaturesActions.setEnterprisePreviewMode(newMode));
    if (newMode) {
      doLoad(); // Reload for clean Custom view
    } else {
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
  const openDeleteModal = () => setIsDeleteModalOpen(true);
  const closeDeleteModal = () => {
    setIsDeleteModalOpen(false);
    dispatch(actions.clearDeleteError());
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <div className="iq-component-label-editor__header">
        <NxPageTitle>
          <NxH1>Component Label Settings</NxH1>
        </NxPageTitle>
        {isFeatureGated && isEditMode && (
          <div className="iq-component-label-editor__mode-switch">
            <NxButton
              variant={!isEnterprisePreviewMode ? 'primary' : 'secondary'}
              onClick={() => !isEnterprisePreviewMode || handleToggleMode()}
              className="iq-component-label-editor__mode-button iq-component-label-editor__mode-button--left"
            >
              Default
            </NxButton>
            <NxTooltip title="Enterprise Feature">
              <NxButton
                variant={isEnterprisePreviewMode ? 'primary' : 'secondary'}
                onClick={() => isEnterprisePreviewMode || handleToggleMode()}
                className="iq-component-label-editor__mode-button iq-component-label-editor__mode-button--right"
              >
                Custom <NxFontAwesomeIcon icon={faLock} />
              </NxButton>
            </NxTooltip>
          </div>
        )}
      </div>
      <NxTile
        className={classNames({
          'iq-banner-flush-top': isFeatureGated && (isEnterprisePreviewMode || !isEditMode),
          'iq-enterprise-mode-footer': isFeatureGated,
        })}
      >
        <NxStatefulForm
          onSubmit={saveLabel}
          submitBtnText={labelId ? 'Update' : 'Create'}
          submitMaskMessage="Saving…"
          submitMaskState={submitMaskState}
          doLoad={doLoad}
          loadError={loadError}
          loading={loading}
          validationErrors={getValidationMessage(isDirty, validationError)}
          submitError={submitError}
          additionalFooterBtns={
            isEditMode && isEnterprisePreviewMode ? (
              <NxButton variant="secondary" onClick={() => window.history.back()} type="button">
                Back
              </NxButton>
            ) : labelId && !isFeatureGated ? (
              <NxButton id="delete-label-button" variant="tertiary" onClick={openDeleteModal} type="button">
                <span>Delete</span>
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
            {isFeatureGated && !isEnterprisePreviewMode && isEditMode ? (
              <ComponentLabelsReadOnlyView label={currentLabel} />
            ) : isFeatureGated ? (
              <>
                <EnterpriseFullWidthBanner
                  title="Custom Component Labels"
                  description="Create custom labels to organize and categorize components according to your workflow."
                />
                <NxFormGroup id="editor-label-name" label="Label Name" isRequired>
                  <NxTextInput onChange={onChangeLabel} {...label} aria-required={true} validatable={true} />
                </NxFormGroup>

                <NxFormGroup id="editor-label-description" label="Description">
                  <NxTextInput
                    className="nx-text-input--long"
                    onChange={onChangeDescription}
                    {...description}
                    validatable={true}
                    type="textarea"
                  />
                </NxFormGroup>

                <NxColorPicker
                  id="editor-label-color-picker"
                  label="Color"
                  value={backendToRscColorMap[color]}
                  onChange={(value) => saveLabelColor(rscToBackendColorMap[value])}
                  isRequired
                />

                {((isEditMode && isEnterprisePreviewMode) || !isEditMode) && (
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
              </>
            ) : (
              <>
                <NxFormGroup id="editor-label-name" label="Label Name" isRequired>
                  <NxTextInput onChange={onChangeLabel} {...label} aria-required={true} validatable={true} />
                </NxFormGroup>
                <NxFormGroup id="editor-label-description" label="Description">
                  <NxTextInput
                    className="nx-text-input--long"
                    onChange={onChangeDescription}
                    {...description}
                    validatable={true}
                    type="textarea"
                  />
                </NxFormGroup>
                <NxColorPicker
                  id="editor-label-color-picker"
                  label="Color"
                  value={backendToRscColorMap[color]}
                  onChange={(value) => saveLabelColor(rscToBackendColorMap[value])}
                  isRequired
                />
              </>
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
      {isDeleteModalOpen && (
        <NxModal
          id="label-config-delete-modal"
          aria-labelledby="label-config-delete-modal-header"
          onClose={closeDeleteModal}
        >
          <NxStatefulForm
            onSubmit={removeLabel}
            submitMaskState={deleteMaskState}
            onCancel={closeDeleteModal}
            submitBtnText="Delete"
            submitMaskMessage="Deleting…"
            submitError={deleteError}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="label-config-delete-modal-header">
                <NxFontAwesomeIcon icon={faTrashAlt} />
                <span>Delete Label</span>
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                <span>You are about to permanently remove {label.value}. This action cannot be undone.</span>
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}
