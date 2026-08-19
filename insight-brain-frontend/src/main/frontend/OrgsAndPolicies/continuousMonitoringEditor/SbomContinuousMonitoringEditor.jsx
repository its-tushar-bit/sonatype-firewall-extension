/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxLoadWrapper,
  NxH2,
  NxButton,
  NxToggle,
  NxP,
} from '@sonatype/react-shared-components';
import { actions } from '../policyMonitoringSlice';
import {
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectContinuousMonitoringIsDirty,
  selectContinuousMonitoringSubmitMaskState,
  selectSelectedStageLabelForSbomManager,
  selectMonitoredStageFromSbomStages,
} from '../policyMonitoringSelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectSbomStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';

export default function SbomContinuousMonitoringEditor() {
  const dispatch = useDispatch();
  const loading = useSelector(selectPolicyMonitoringLoading);
  const loadError = useSelector(selectPolicyMonitoringLoadError);
  const submitError = useSelector(selectPolicyMonitoringSubmitError);
  const isDirty = useSelector(selectContinuousMonitoringIsDirty);
  const submitMaskState = useSelector(selectContinuousMonitoringSubmitMaskState);
  const stages = useSelector(selectSbomStageTypes);
  const monitoredStage = useSelector(selectMonitoredStageFromSbomStages);
  const stageDetails = useSelector(selectSelectedStageLabelForSbomManager);

  const [isToggleChecked, setIsToggleChecked] = useState(false);

  useEffect(() => {
    setIsToggleChecked(!!stages?.find((stage) => stage.stageTypeId === monitoredStage?.stageTypeId));
  }, [stages, monitoredStage]);

  const toggleComplianceStageEnabled = () => {
    const updatedStage = isToggleChecked ? { stageName: 'Do Not Monitor' } : stages[0];
    dispatch(actions.setMonitoredStage({ stages: stages, monitoredStage: updatedStage }));
    setIsToggleChecked(!isToggleChecked);
  };

  const doLoad = () => {
    dispatch(actions.loadApplicablePolicyMonitoring());
  };

  const handleSubmit = () => {
    if (isToggleChecked) {
      dispatch(actions.savePolicyMonitoring());
    } else {
      dispatch(actions.removePolicyMonitoring());
    }
  };

  const handleLearnMoreClick = () => {
    window.open('https://links.sonatype.com/products/sbom/docs/monitoring', '_blank');
  };

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <>
      <div id={'sbom-manager-continuous-monitoring'} className={'sbom-nx-page-title'}>
        <div className={'sbom-continuous-monitoring-title'}>
          <NxH1>Continuous Monitoring</NxH1>
          <NxButton onClick={handleLearnMoreClick}>Learn more</NxButton>
        </div>
        <NxPageTitle.Description>
          Remain vigilant to new violations discovered during compliance monitoring. Enable the continuous monitoring
          audit on your latest SBOMs. Notifications are configured per policy and only trigger on new violations.
        </NxPageTitle.Description>
      </div>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile>
          <NxTile.Content>
            <NxStatefulForm
              submitBtnText="Update"
              submitMaskState={submitMaskState}
              submitMaskMessage="Saving…"
              validationErrors={isDirty ? undefined : MSG_NO_CHANGES_TO_SAVE}
              onSubmit={handleSubmit}
              doLoad={doLoad}
              loadError={loadError}
              submitError={submitError}
            >
              <NxH2>Configure Continuous monitoring</NxH2>
              <div className={'sbom-continuous-monitoring'}>
                <NxP id={'sbom-continuous-monitoring-status-label'}>{stageDetails?.label}</NxP>
                <NxToggle
                  id="enable-continuous-monitoring"
                  className="sbom-enable-continuous-monitoring"
                  onChange={() => toggleComplianceStageEnabled()}
                  isChecked={isToggleChecked}
                  disabled={!stageDetails?.toggleEnabled}
                >
                  {isToggleChecked ? 'Enabled' : 'Disabled'}
                </NxToggle>
              </div>
            </NxStatefulForm>
          </NxTile.Content>
        </NxTile>
      </NxLoadWrapper>
    </>
  );
}
