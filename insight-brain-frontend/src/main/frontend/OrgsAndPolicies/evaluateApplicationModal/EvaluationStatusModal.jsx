/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxButtonBar,
  NxDescriptionList,
  NxFooter,
  NxH2,
  NxInfoAlert,
  NxLoadError,
  NxModal,
  NxProgressBar,
} from '@sonatype/react-shared-components';
import React from 'react';
import PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectEvaluateApplicationSlice } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import NavLink from 'MainRoot/mainHeader/MenuBar/MenuButton/NavLink';
import { getApplicationReportUrl } from 'MainRoot/util/CLMLocation';

const EvaluationStatusModal = ({
  close,
  evaluate,
  evaluationStatus: { currentStepName, currentStep, totalSteps, scanId, error },
}) => {
  const {
    stages,
    selectedStageId,
    file: { files },
  } = useSelector(selectEvaluateApplicationSlice);
  const { name: applicationName, publicId: applicationId } = useSelector(selectSelectedOwner);
  const selectedStageName = stages.find((stage) => {
    if (stage.stageTypeId === selectedStageId) {
      return stage;
    }
  })?.stageName;

  return (
    <NxModal id="evaluation-status-modal" className="evaluation-status" onCancel={close}>
      <NxModal.Header>
        <NxH2>Evaluation Status</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxInfoAlert>
          Closing this panel will not interrupt the evaluation. It will still progress until it is complete. When the
          evaluation has been completed you can view the results by clicking on the button below.
        </NxInfoAlert>
        <NxDescriptionList>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>File:</NxDescriptionList.Term>
            <NxDescriptionList.Description>{files?.[0]?.name}</NxDescriptionList.Description>
          </NxDescriptionList.Item>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Application:</NxDescriptionList.Term>
            <NxDescriptionList.Description>{applicationName}</NxDescriptionList.Description>
          </NxDescriptionList.Item>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Stage:</NxDescriptionList.Term>
            <NxDescriptionList.Description>{selectedStageName}</NxDescriptionList.Description>
          </NxDescriptionList.Item>
          <NxDescriptionList.Item>
            <NxProgressBar
              value={currentStep}
              max={totalSteps}
              variant="full"
              showSteps
              label={currentStepName}
              labelSuccess="Done"
            />
          </NxDescriptionList.Item>
        </NxDescriptionList>
        <NxLoadError error={error} retryHandler={evaluate} />
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={close}>Close</NxButton>
          <NavLink openInNewTab href={getApplicationReportUrl(applicationId, scanId)} disabled={!scanId}>
            <NxButton variant="primary" disabled={!scanId}>
              View Report
            </NxButton>
          </NavLink>
        </NxButtonBar>
      </NxFooter>
    </NxModal>
  );
};

export default EvaluationStatusModal;

EvaluationStatusModal.propTypes = {
  close: PropTypes.func.isRequired,
  evaluate: PropTypes.func.isRequired,
  evaluationStatus: PropTypes.shape({
    currentStepName: PropTypes.string,
    currentStep: PropTypes.number,
    totalSteps: PropTypes.number,
    scanId: PropTypes.string,
    error: PropTypes.oneOfType([PropTypes.instanceOf(null), PropTypes.string]),
  }),
};
