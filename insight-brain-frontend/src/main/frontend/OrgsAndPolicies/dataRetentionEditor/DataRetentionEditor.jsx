/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  selectApplicationReportsStages,
  selectApplicationReportsStagesServerData,
  selectApplicationReportsParentStages,
  selectValidationErrors,
  selectRetentionSlice,
} from 'MainRoot/OrgsAndPolicies/retentionSelectors';

import { actions } from 'MainRoot/OrgsAndPolicies/retentionSlice';
import DataRetentionEditorItem from './DataRetentionEditorItem';

import { NxPageTitle, NxH1, NxH2, NxTile, NxStatefulForm, NxDivider } from '@sonatype/react-shared-components';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

const getValidationMessage = (isDirty, validationErrors) => {
  if (!isDirty) {
    return MSG_NO_CHANGES_TO_SAVE;
  }
  return validationErrors;
};

export default function DataRetentionEditor() {
  const dispatch = useDispatch();

  const stages = useSelector(selectApplicationReportsStages);
  const stagesServerData = useSelector(selectApplicationReportsStagesServerData);
  const parentData = useSelector(selectApplicationReportsParentStages);
  const validationErrors = useSelector(selectValidationErrors);

  const {
    loading,
    submitError,
    submitMaskState,
    loadError,
    isDirty,
    successMetrics,
    successMetricsServerData,
    successMetricsParent,
  } = useSelector(selectRetentionSlice);

  const doLoad = () => dispatch(actions.loadRetention());

  const handleSubmit = () => {
    dispatch(actions.updateRetention());
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <>
      <NxPageTitle>
        <NxH1>Data Retention</NxH1>
      </NxPageTitle>
      <NxTile id="retention-editor">
        <NxStatefulForm
          submitBtnText="Update"
          submitMaskMessage="Saving…"
          onSubmit={handleSubmit}
          doLoad={doLoad}
          loading={loading}
          loadError={loadError}
          submitMaskState={submitMaskState}
          submitError={submitError}
          validationErrors={getValidationMessage(isDirty, validationErrors)}
        >
          <NxTile.Content>
            <NxH2>Application Reports</NxH2>
            {stages &&
              stagesServerData &&
              Object.keys(stages).map((stage) => {
                return <DataRetentionEditorItem key={stage} stage={stage} stages={stages} parentData={parentData} />;
              })}
            <NxDivider />
            <NxH2>Success Metrics</NxH2>
            {successMetrics && successMetricsServerData && (
              <DataRetentionEditorItem
                stage="successMetrics"
                stages={{ successMetrics: successMetrics }}
                parentData={{ successMetrics: successMetricsParent }}
              />
            )}
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
    </>
  );
}
