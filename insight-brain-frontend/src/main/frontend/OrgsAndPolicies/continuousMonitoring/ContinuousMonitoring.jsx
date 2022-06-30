/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxFieldset,
  NxRadio,
  NxErrorAlert,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { actions } from './policyMonitoringSlice';
import {
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectSelectedMonitoredStage,
  selectContinousMonitoringIsDirty,
  selectContinuousMonitoringSubmitMaskState,
} from './policyMonitoringSelectors';
import { selectCliStagesWithInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { selectIsMonitoringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function ContinuousMonitoring() {
  const dispatch = useDispatch();
  const loading = useSelector(selectPolicyMonitoringLoading);
  const loadError = useSelector(selectPolicyMonitoringLoadError);
  const submitError = useSelector(selectPolicyMonitoringSubmitError);
  const stages = useSelector(selectCliStagesWithInheritOrNoMonitorOption);
  const monitoredStage = useSelector(selectSelectedMonitoredStage);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isDirty = useSelector(selectContinousMonitoringIsDirty);
  const submitMaskState = useSelector(selectContinuousMonitoringSubmitMaskState);

  const doLoad = () => {
    dispatch(actions.loadApplicablePolicyMonitoring());
  };

  const handleSubmit = () => {
    monitoredStage.stageTypeId ? dispatch(actions.savePolicyMonitoring()) : dispatch(actions.removePolicyMonitoring());
  };

  const handleMonitorChange = (stage) => {
    dispatch(actions.setMonitoredStage(stage));
  };

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      <NxPageTitle>
        <NxH1>Continuous Monitoring</NxH1>
        <NxPageTitle.Description>
          Keep daily visibility on applications that are not being built or scanned regularly. Violation notifications
          can be configured per policy.
        </NxPageTitle.Description>
      </NxPageTitle>
      {isMonitoringSupported ? (
        <NxTile>
          <NxTile.Content>
            <NxForm
              submitBtnText="Update"
              submitMaskState={submitMaskState}
              submitMaskMessage="Saving…"
              validationErrors={isDirty ? undefined : 'There are no changes to save'}
              onSubmit={handleSubmit}
              doLoad={doLoad}
              loadError={loadError}
              submitError={submitError}
            >
              <NxFieldset label="Monitoring Stage" isRequired>
                {stages?.map((stage) => {
                  return (
                    <NxRadio
                      name="monitor"
                      key={stage.stageName}
                      value={stage.stageName}
                      isChecked={monitoredStage.stageTypeId === stage.stageTypeId}
                      onChange={() => handleMonitorChange(stage)}
                    >
                      {stage.stageName}
                    </NxRadio>
                  );
                })}
              </NxFieldset>
            </NxForm>
          </NxTile.Content>
        </NxTile>
      ) : (
        <NxErrorAlert>Continuous monitoring is not supported by your license.</NxErrorAlert>
      )}
    </NxLoadWrapper>
  );
}
