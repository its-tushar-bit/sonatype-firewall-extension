/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectChangeApplicationIdSlice, selectNewPublicId } from './changeApplicationIdSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  NxModal,
  NxWarningAlert,
  NxH2,
  NxStatefulForm,
  NxFormGroup,
  NxTextInput,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import { actions } from './changeApplicationIdSlice';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import { selectOwnersFlattenEntries } from '../ownerSideNav/ownerSideNavSelectors';

export default function ChangeApplicationIdModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectChangeApplicationIdSlice);
  const newPublicId = useSelector(selectNewPublicId);
  const { publicId } = useSelector(selectSelectedOwner);
  const appsList = useSelector(selectOwnersFlattenEntries).applications;

  const closeModal = () => dispatch(actions.closeModal());
  const changeApplicationId = () => dispatch(actions.changeApplicationId());
  const onChangeAppId = (value) => dispatch(actions.setNewPublicIdValue({ value, appsList }));

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="change-application-id-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={changeApplicationId}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Change"
        submitError={submitError}
        validationErrors={!isNilOrEmpty(newPublicId.validationErrors) ? GLOBAL_FORM_VALIDATION_ERROR : null}
      >
        <NxModal.Header>
          <NxH2>Change Application ID</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            Changing the Application ID will break existing integration points. They will have to be re-configured.
          </NxWarningAlert>

          <NxReadOnly>
            <NxReadOnly.Label>Current Application ID</NxReadOnly.Label>
            <NxReadOnly.Data>{publicId}</NxReadOnly.Data>
          </NxReadOnly>

          <NxFormGroup id="editor-new-id" label="New Application ID" isRequired>
            <NxTextInput onChange={onChangeAppId} {...newPublicId} validatable={true} />
          </NxFormGroup>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
