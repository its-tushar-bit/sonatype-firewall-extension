/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxButtonBar,
  NxFieldset,
  NxFileUpload,
  NxFooter,
  NxStatefulForm,
  NxFormGroup,
  NxFormSelect,
  NxH2,
  NxModal,
  NxRadio,
  NxProgressBar,
} from '@sonatype/react-shared-components';
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { selectEvaluateApplicationSlice } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSlice';
import { selectIsNotificationsSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { GLOBAL_FORM_VALIDATION_ERROR } from 'MainRoot/util/validationUtil';
import EvaluationStatusModal from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/EvaluationStatusModal';

const EvaluateApplicationModal = () => {
  const dispatch = useDispatch();
  const {
    isEvaluationModalOpen,
    isStatusModalOpen,
    stages,
    notify,
    file,
    loading,
    loadError,
    submitError,
    isValid,
    evaluationStatus,
    uploadFileProgress,
    isUploadingFile,
  } = useSelector(selectEvaluateApplicationSlice);
  const isNotificationSupported = useSelector(selectIsNotificationsSupported);

  useEffect(() => {
    if (isEvaluationModalOpen) {
      doLoad();
    }
  }, [isEvaluationModalOpen]);

  useEffect(
    () => () => {
      dispatch(closeEvaluateAppModal());
      dispatch(closeStatusModal());
    },
    []
  );

  const selectStage = (event) => dispatch(actions.selectStageId(event.target.value));
  const closeEvaluateAppModal = () => dispatch(actions.closeEvaluateAppModal());
  const closeStatusModal = () => dispatch(actions.closeEvalStatusModal());
  const selectFile = (file) => dispatch(actions.selectFile(file));
  const doLoad = () => dispatch(actions.doLoad());
  const evaluate = () => dispatch(actions.evaluate());
  const changeNotification = (value) => dispatch(actions.changeNotification(value));

  return (
    <>
      {isEvaluationModalOpen && (
        <NxModal id="evaluate-application-modal" onCancel={closeEvaluateAppModal}>
          <NxModal.Header>
            <NxH2>Evaluate a File</NxH2>
          </NxModal.Header>
          {!isUploadingFile && (
            <NxStatefulForm
              doLoad={doLoad}
              loading={loading}
              loadError={loadError}
              onSubmit={evaluate}
              submitError={submitError}
              submitBtnText="Upload"
              onCancel={closeEvaluateAppModal}
              validationErrors={!isValid ? GLOBAL_FORM_VALIDATION_ERROR : null}
            >
              <NxModal.Content>
                <NxFormGroup label="Select the file you want evaluated" isRequired>
                  <NxFileUpload
                    onChange={selectFile}
                    {...file}
                    isRequired
                    disabled={isUploadingFile}
                    data-testid="evaluate-application-upload-file"
                  />
                </NxFormGroup>
                <NxFieldset label="Which stage should this evaluation be associated with?" isRequired>
                  <NxFormSelect onChange={selectStage}>
                    <option value="" key="title">
                      Select Stage
                    </option>
                    {stages.map(({ stageName, stageTypeId }) => (
                      <option key={stageTypeId} value={stageTypeId}>
                        {stageName}
                      </option>
                    ))}
                  </NxFormSelect>
                </NxFieldset>
                {isNotificationSupported && (
                  <NxFieldset
                    label="Should notifications be sent if this application violates any policies?"
                    isRequired
                  >
                    <NxRadio name="notify" value="true" onChange={changeNotification} isChecked={notify === 'true'}>
                      Yes
                    </NxRadio>
                    <NxRadio name="notify" value="false" onChange={changeNotification} isChecked={notify === 'false'}>
                      No
                    </NxRadio>
                  </NxFieldset>
                )}
              </NxModal.Content>
            </NxStatefulForm>
          )}
          {isUploadingFile && (
            <NxProgressBar
              value={uploadFileProgress}
              label={`Uploading ${file.files?.[0]?.name} file`}
              labelSuccess="File uploaded successfully"
            />
          )}
          {!!loadError && (
            <NxFooter>
              <NxButtonBar>
                <NxButton onClick={closeEvaluateAppModal}>Close</NxButton>
              </NxButtonBar>
            </NxFooter>
          )}
        </NxModal>
      )}
      {isStatusModalOpen && (
        <EvaluationStatusModal evaluationStatus={evaluationStatus} close={closeStatusModal} evaluate={evaluate} />
      )}
    </>
  );
};

export default EvaluateApplicationModal;
