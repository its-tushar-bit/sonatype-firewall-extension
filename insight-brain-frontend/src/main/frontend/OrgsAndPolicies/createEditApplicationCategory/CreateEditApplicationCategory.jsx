/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxButton,
  NxTextInput,
  NxColorPicker,
  NxFormGroup,
  NxModal,
  NxWarningAlert,
  NxFontAwesomeIcon,
  NxInfoAlert,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faLock } from '@fortawesome/pro-regular-svg-icons';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from './createEditApplicationCategoriesSlice';
import {
  selectSubmitError,
  selectLoadError,
  selectIsLoading,
  selectCurrentCategory,
  selectSubmitMaskState,
  selectValidationError,
  selectIsDirty,
  selectDeleteError,
  selectDeleteMaskState,
  selectAssociatedApplicationNames,
  selectTagPolicyList,
  selectIsEditMode,
} from './createEditApplicationCategoriesSelectors';

import { backendToRscColorMap, rscToBackendColorMap } from '../utility/util';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import ApplicationCategoryReadOnlyView from './ApplicationCategoryReadOnlyView';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import {
  selectHasCustomAppCategories,
  selectIsEnterprisePreviewMode,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import './_CreateEditApplicationCategory.scss';

const getValidationMessage = (isDirty, validationError) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }

  return validationError;
};

const getDeleteModalWarningMessage = (isCategoryCannotBeDeleted, tagPolicyList, associatedApplicationNames) => {
  let warningMessage;
  if (isCategoryCannotBeDeleted) {
    warningMessage = `You cannot delete this application category because
      it is associated with the following policies: ${tagPolicyList.join(', ')}`;
  } else {
    warningMessage = 'Are you sure you want to delete this application category? ';
    if (associatedApplicationNames?.length) {
      warningMessage += `It is in use by the following applications: ${associatedApplicationNames.join(', ')}.`;
    }
  }
  return warningMessage;
};

export default function CreateEditApplicationCategory() {
  const dispatch = useDispatch();
  const loadError = useSelector(selectLoadError);
  const submitError = useSelector(selectSubmitError);
  const loading = useSelector(selectIsLoading);
  const isDirty = useSelector(selectIsDirty);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const validationError = useSelector(selectValidationError);
  const deleteError = useSelector(selectDeleteError);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const { categoryId } = useSelector(selectRouterCurrentParams);
  const associatedApplicationNames = useSelector(selectAssociatedApplicationNames);
  const tagPolicyList = useSelector(selectTagPolicyList);
  const isEditMode = useSelector(selectIsEditMode);
  const isEnterprisePreviewMode = useSelector(selectIsEnterprisePreviewMode);
  const hasCustomAppCategories = useSelector(selectHasCustomAppCategories);
  const isFeatureGated = !hasCustomAppCategories;

  const currentCategory = useSelector(selectCurrentCategory);
  const { color, description, name } = currentCategory;

  const saveCategoryColor = (color) => dispatch(actions.setCategoryColor(color));
  const onChangeName = (val) => dispatch(actions.setCategoryName(val));
  const onChangeDescription = (val) => dispatch(actions.setCategoryDescription(val));
  const saveCategory = () => dispatch(actions.saveApplicationCategory());
  const removeCategory = () => dispatch(actions.removeApplicationCategory());
  const doLoad = () => dispatch(actions.loadCategoryEditor());
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

  const isCategoryCannotBeDeleted = tagPolicyList?.length;
  const getDeleteModalProps = (isCategoryCannotBeDeleted) => {
    if (isCategoryCannotBeDeleted) {
      return {
        onSubmit: closeDeleteModal,
        submitBtnText: 'OK',
      };
    }
    return {
      onSubmit: removeCategory,
      onCancel: closeDeleteModal,
      submitBtnText: 'Continue',
      submitMaskMessage: 'Deleting…',
    };
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <div className="iq-application-category-editor__header">
        <NxPageTitle>
          <NxH1>Application Category Settings</NxH1>
        </NxPageTitle>
        {isFeatureGated && isEditMode && (
          <div className="iq-application-category-editor__mode-switch">
            <NxButton
              variant={!isEnterprisePreviewMode ? 'primary' : 'secondary'}
              onClick={() => !isEnterprisePreviewMode || handleToggleMode()}
              className="iq-application-category-editor__mode-button iq-application-category-editor__mode-button--left"
            >
              Default
            </NxButton>
            <NxTooltip title="Enterprise Feature">
              <NxButton
                variant={isEnterprisePreviewMode ? 'primary' : 'secondary'}
                onClick={() => isEnterprisePreviewMode || handleToggleMode()}
                className="iq-application-category-editor__mode-button iq-application-category-editor__mode-button--right"
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
          id="create-edit-category"
          onSubmit={saveCategory}
          submitBtnText={categoryId ? 'Update' : 'Create'}
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
            ) : categoryId && !isFeatureGated ? (
              <NxButton id="delete-category-button" variant="tertiary" onClick={openDeleteModal} type="button">
                Delete
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
            {isFeatureGated && !isEnterprisePreviewMode && isEditMode ? (
              <ApplicationCategoryReadOnlyView category={currentCategory} />
            ) : isFeatureGated ? (
              <>
                <EnterpriseFullWidthBanner
                  title="Custom Application Categories"
                  description="Create custom categories to organize applications according to your business needs."
                />
                <NxFormGroup id="editor-category-name" label="Category Name" isRequired>
                  <NxTextInput onChange={onChangeName} {...name} validatable={true} />
                </NxFormGroup>

                <NxFormGroup id="editor-category-description" label="Brief Description" isRequired>
                  <NxTextInput
                    className="nx-text-input--long"
                    onChange={onChangeDescription}
                    {...description}
                    validatable={true}
                    type="textarea"
                  />
                </NxFormGroup>

                <NxColorPicker
                  id="editor-category-color-picker"
                  label="Color"
                  value={backendToRscColorMap[color]}
                  onChange={(value) => saveCategoryColor(rscToBackendColorMap[value])}
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
                <NxFormGroup id="editor-category-name" label="Category Name" isRequired>
                  <NxTextInput onChange={onChangeName} {...name} validatable={true} />
                </NxFormGroup>
                <NxFormGroup id="editor-category-description" label="Brief Description" isRequired>
                  <NxTextInput
                    className="nx-text-input--long"
                    onChange={onChangeDescription}
                    {...description}
                    validatable={true}
                    type="textarea"
                  />
                </NxFormGroup>
                <NxColorPicker
                  id="editor-category-color-picker"
                  label="Color"
                  value={backendToRscColorMap[color]}
                  onChange={(value) => saveCategoryColor(rscToBackendColorMap[value])}
                  isRequired
                />
              </>
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>

      {isDeleteModalOpen && (
        <NxModal id="category-delete-modal" aria-labelledby="category-delete-modal-header" onClose={closeDeleteModal}>
          <NxStatefulForm
            submitMaskState={deleteMaskState}
            submitError={deleteError}
            {...getDeleteModalProps(isCategoryCannotBeDeleted)}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2" id="category-delete-modal-header">
                Delete Application Category
              </h2>
            </header>
            <div className="nx-modal-content">
              <NxWarningAlert>
                {getDeleteModalWarningMessage(isCategoryCannotBeDeleted, tagPolicyList, associatedApplicationNames)}
              </NxWarningAlert>
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}
