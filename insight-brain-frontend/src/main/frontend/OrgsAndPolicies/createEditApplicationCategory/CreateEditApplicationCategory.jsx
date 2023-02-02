/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
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
} from '@sonatype/react-shared-components';

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
} from './createEditApplicationCategoriesSelectors';

import { angularToRscColorMap, rscToAngularColorMap } from '../utility/util';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

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

  const { color, description, name } = useSelector(selectCurrentCategory);

  const saveCategoryColor = (color) => dispatch(actions.setCategoryColor(color));
  const onChangeName = (val) => dispatch(actions.setCategoryName(val));
  const onChangeDescription = (val) => dispatch(actions.setCategoryDescription(val));
  const saveCategory = () => dispatch(actions.saveApplicationCategory());
  const removeCategory = () => dispatch(actions.removeApplicationCategory());
  const doLoad = () => dispatch(actions.loadCategoryEditor());

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
      <NxPageTitle>
        <NxH1>{categoryId ? 'Edit' : 'New'} Application Category</NxH1>
        {!categoryId && (
          <NxPageTitle.Description>
            Applications with similar attributes can be grouped and categorized, which then can be used to apply
            policies to a subset of applications through inheritance.
          </NxPageTitle.Description>
        )}
      </NxPageTitle>

      <NxTile>
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
            categoryId ? (
              <NxButton id="delete-category-button" variant="tertiary" onClick={openDeleteModal} type="button">
                Delete
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
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
              value={angularToRscColorMap[color]}
              onChange={(value) => saveCategoryColor(rscToAngularColorMap[value])}
              isRequired
            />
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
