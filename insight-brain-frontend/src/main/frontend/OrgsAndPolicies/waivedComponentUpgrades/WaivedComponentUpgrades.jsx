/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxPageTitle,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxTile,
  NxStatefulForm,
  NxFieldset,
  NxRadio,
  NxP,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectWaivedComponentUpgrades } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/waivedComponentUpgradesSlice';
import { selectCliStagesWithNoneOption } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import ownerConstant from 'MainRoot/utility/services/owner.constant';

export default function WaivedComponentUpgrades() {
  const dispatch = useDispatch();

  // Selectors
  const stages = useSelector(selectCliStagesWithNoneOption);
  const { organizationId } = useSelector(selectRouterCurrentParams);
  const { loading, loadError, isDirty, submitMaskState, submitError, configuredStage } = useSelector(
    selectWaivedComponentUpgrades
  );

  // Load existing configuration
  const doLoad = () => {
    dispatch(actions.loadUpgradeStage());
  };

  // Update selected option and isDirty
  const handleStageChange = (val) => {
    dispatch(actions.setConfiguredStage(val));
    dispatch(actions.setIsDirty(configuredStage !== val));
  };

  // Save changes
  const handleSubmit = () => {
    dispatch(actions.saveUpgradeStage());
  };

  useEffect(function () {
    doLoad();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      <NxPageTitle>
        <NxH1>Waived Component Upgrades</NxH1>
        <NxPageTitle.Description>
          <NxP>
            You can upgrade vulnerable versions of components that broke builds and violated policies. The Waiver
            Dashboard will indicate when a component upgrade is available under the "Upgrade" column. For more
            information, see{' '}
            <NxTextLink external href="https://links.sonatype.com/products/nxiq/doc/waived-component-upgrades">
              Waived Component Upgrades
            </NxTextLink>
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile>
        <NxTile.Headings>
          <NxTile.HeaderTitle>
            <NxH2>Enable Monitoring for Upgrades</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Headings>
        <NxTile.Content>
          <NxStatefulForm
            id="form-waived-component-upgrades"
            submitBtnText="Update"
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            validationErrors={isDirty ? undefined : 'There are no changes to save'}
            onSubmit={handleSubmit}
            doLoad={doLoad}
            loadError={loadError}
            submitError={submitError}
          >
            <NxFieldset
              label="Stage Selection"
              sublabel="Select a stage to enable monitoring. Stage selections can only be made at the Root Org level."
              isRequired
            >
              {stages?.map((stage) => (
                <NxRadio
                  key={stage.stageTypeId}
                  name="configured-stage"
                  value={stage.stageTypeId}
                  onChange={handleStageChange}
                  isChecked={configuredStage?.toLowerCase() === stage.stageTypeId?.toLowerCase()}
                  disabled={organizationId !== ownerConstant.ROOT_ORGANIZATION_ID}
                  radioId={`configured-stage-${stage.stageTypeId}`}
                >
                  {stage.stageName}
                </NxRadio>
              ))}
            </NxFieldset>
          </NxStatefulForm>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}
