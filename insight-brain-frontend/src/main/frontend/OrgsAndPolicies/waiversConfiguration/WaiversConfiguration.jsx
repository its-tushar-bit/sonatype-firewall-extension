/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
  selectProductFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import LicenseLockScreenForWaivers from './LicenseLockScreenForWaivers';
import {
  NxTile,
  NxCheckbox,
  NxFieldset,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxStatefulForm,
  NxH2,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversSlice';
import './_waiversConfiguration.scss';
import { selectWaiversConfigPage, selectWaiversSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';

const WaiversConfiguration = () => {
  const dispatch = useDispatch();
  const { loading, loadError } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);

  const doLoad = () => dispatch(actions.loadWaiversConfigurationPage());

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      {isDeveloperDashboardEnabled && isAutoWaiversEnabled && !isSbomManager ? (
        <WaiversConfigurationContents />
      ) : (
        <LicenseLockScreenForWaivers />
      )}
    </NxLoadWrapper>
  );
};

function WaiversConfigurationContents() {
  const dispatch = useDispatch();

  const waiversConfigPage = useSelector(selectWaiversConfigPage);
  let { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectWaiversSlice);
  const reachable = waiversConfigPage?.reachable ?? false;
  const pathForward = waiversConfigPage?.pathForward ?? false;
  const threatLevel = waiversConfigPage?.threatLevel ?? 7;
  if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
    isDirty = true;
  }
  const handleSubmit = () => {
    if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
      dispatch(actions.createWaiver());
    } else {
      dispatch(actions.saveWaiversConfiguration());
    }
  };
  const doLoad = () => dispatch(actions.loadWaiversConfigurationPage());

  return (
    <>
      <NxPageTitle>
        <NxH1>Automated Waivers</NxH1>
        <NxPageTitle.Description>
          Limit disruptions by deprioritizing low-threat violations until a remediation path is available.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile aria-label="Configure Auto-Waiver">
          <NxStatefulForm
            submitBtnText="Update"
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            onSubmit={handleSubmit}
            validationErrors={isDirty ? undefined : MSG_NO_CHANGES_TO_SAVE}
            loadError={loadError}
            submitError={submitError}
          >
            <NxH2>Configure Auto-Waiver</NxH2>
            <NxFieldset label="Max. Threat Level">
              <div className="iq-waivers-configuration-upgrades">
                <NxThreatIndicator policyThreatLevel={threatLevel} />
                <strong>{threatLevel}</strong>
              </div>
            </NxFieldset>
            <NxFieldset label="Scope" sublabel="Apply to violations if/when the:">
              <NxCheckbox onChange={() => dispatch(actions.toggleCheckboxReachable())} isChecked={reachable || false}>
                Security vulnerability is Not Reachable
              </NxCheckbox>
              <NxCheckbox onChange={() => dispatch(actions.toggleCheckboxPath())} isChecked={pathForward || false}>
                Component version is current or latest non-violating
              </NxCheckbox>
            </NxFieldset>
          </NxStatefulForm>
        </NxTile>
      </NxLoadWrapper>
    </>
  );
}

export default WaiversConfiguration;
