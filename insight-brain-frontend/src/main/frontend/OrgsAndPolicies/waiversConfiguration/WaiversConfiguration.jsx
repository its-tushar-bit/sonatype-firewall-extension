/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
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
import ConfirmationModal from 'MainRoot/legal/application/ConfirmationModal';

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
  const [isDeleteConfirmationModalOpen, setisDeleteConfirmationModalOpen] = useState(false);

  const waiversConfigPage = useSelector(selectWaiversConfigPage);
  let { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectWaiversSlice);
  const reachable = waiversConfigPage?.reachable ?? false;
  const pathForward = waiversConfigPage?.pathForward ?? false;
  const threatLevel = waiversConfigPage?.threatLevel ?? 7;
  const hasExistingWaiver = waiversConfigPage?.autoPolicyWaiverId != null;

  if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
    isDirty = true;
  }

  const handleDelete = () => {
    dispatch(actions.deleteWaiver());
    setisDeleteConfirmationModalOpen(false);
  };

  const shouldDeleteAutoWaiver = () => {
    return isDirty && !reachable && !pathForward && hasExistingWaiver;
  };

  const handleSubmit = () => {
    if (shouldDeleteAutoWaiver()) {
      setisDeleteConfirmationModalOpen(true);
    } else if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
      dispatch(actions.createWaiver());
    } else {
      dispatch(actions.saveWaiversConfiguration());
    }
  };

  const validationError = () => {
    if (!reachable && !pathForward && !hasExistingWaiver) return 'Can not save without selecting at least one option';
    if (!isDirty) return MSG_NO_CHANGES_TO_SAVE;
    return undefined;
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
            submitBtnText={shouldDeleteAutoWaiver() ? 'Delete Auto Waiver' : 'Update'}
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            onSubmit={handleSubmit}
            validationErrors={validationError()}
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
              <NxCheckbox onChange={() => dispatch(actions.toggleCheckboxPath())} isChecked={pathForward || false}>
                No newer, non-violating component version is available
              </NxCheckbox>
            </NxFieldset>
          </NxStatefulForm>
        </NxTile>
      </NxLoadWrapper>
      {isDeleteConfirmationModalOpen && (
        <ConfirmationModal
          id="delete-auto-waiver-modal"
          cancelHandler={() => setisDeleteConfirmationModalOpen(false)}
          titleContent={<span>Confirm Delete</span>}
          confirmationMessage="Are you sure you want to delete this auto waiver configuration?"
          closeHandler={() => setisDeleteConfirmationModalOpen(false)}
          confirmationHandler={handleDelete}
          confirmationButtonText="Delete"
        />
      )}
    </>
  );
}

export default WaiversConfiguration;
