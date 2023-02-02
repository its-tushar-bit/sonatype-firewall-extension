/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
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
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';

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
} from '../labelsSelectors';
import { angularToRscColorMap, rscToAngularColorMap } from '../utility/util';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

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

  const { color, description, label } = useSelector(selectLabelsCurrentLabel);

  const saveLabelColor = (color) => dispatch(actions.setLabelColor(color));
  const onChangeLabel = (val) => dispatch(actions.setLabelName(val));
  const onChangeDescription = (val) => dispatch(actions.setLabelDescription(val));
  const saveLabel = () => dispatch(actions.saveLabel());
  const removeLabel = () => dispatch(actions.removeLabel());
  const doLoad = () => dispatch(actions.loadLabelsEditor());

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
      <NxPageTitle>
        <NxH1>{labelId ? 'Edit' : 'New'} Component Label</NxH1>
      </NxPageTitle>
      <NxTile>
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
            labelId ? (
              <NxButton id="delete-label-button" variant="tertiary" onClick={openDeleteModal} type="button">
                <span>Delete</span>
              </NxButton>
            ) : null
          }
        >
          <NxTile.Content>
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
              value={angularToRscColorMap[color]}
              onChange={(value) => saveLabelColor(rscToAngularColorMap[value])}
              isRequired
            />
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
