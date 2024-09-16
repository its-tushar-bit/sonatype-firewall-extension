/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxStatefulForm,
  NxPageTitle,
  NxH1,
  NxTile,
  NxFieldset,
  NxRadio,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { actions } from '../policyMonitoringSlice';
import {
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectSelectedMonitoredStage,
  selectContinuousMonitoringIsDirty,
  selectContinuousMonitoringSubmitMaskState,
} from '../policyMonitoringSelectors';
import { selectCliStagesWithInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function ContinuousMonitoringEditor() {
  const dispatch = useDispatch();
  const loading = useSelector(selectPolicyMonitoringLoading);
  const loadError = useSelector(selectPolicyMonitoringLoadError);
  const submitError = useSelector(selectPolicyMonitoringSubmitError);
  const stages = useSelector(selectCliStagesWithInheritOrNoMonitorOption);
  const monitoredStage = useSelector(selectSelectedMonitoredStage);
  const isDirty = useSelector(selectContinuousMonitoringIsDirty);
  const submitMaskState = useSelector(selectContinuousMonitoringSubmitMaskState);

  const doLoad = () => {
    dispatch(actions.loadApplicablePolicyMonitoring());
  };

  const handleSubmit = () => {
    monitoredStage.stageTypeId ? dispatch(actions.savePolicyMonitoring()) : dispatch(actions.removePolicyMonitoring());
  };

  const handleMonitorChange = (stage) => {
    dispatch(actions.setMonitoredStage({ stages: stages, monitoredStage: stage }));
  };

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <>
      <NxPageTitle>
        <NxH1>Continuous Monitoring</NxH1>
        <NxPageTitle.Description>
          Keep daily visibility on applications that are not being built or scanned regularly. Violation notifications
          can be configured per policy.
        </NxPageTitle.Description>
      </NxPageTitle>
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
              <NxFieldset label="Monitoring Stage" isRequired>
                {stages?.map((stage) => {
                  return (
                    <NxRadio
                      name="monitor"
                      key={stage.stageName}
                      value={stage.stageName}
                      isChecked={monitoredStage?.stageTypeId === stage.stageTypeId}
                      onChange={() => handleMonitorChange(stage)}
                    >
                      {stage.stageName}
                    </NxRadio>
                  );
                })}
              </NxFieldset>
            </NxStatefulForm>
          </NxTile.Content>
        </NxTile>
      </NxLoadWrapper>
    </>
  );
}
