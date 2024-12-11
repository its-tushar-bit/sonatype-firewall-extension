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
import {
  NxTile,
  NxCheckbox,
  NxFieldset,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxStatefulForm,
  NxH2,
  NxH4,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversSlice';
import './_autoWaiversConfiguration.scss';
import { selectWaiversConfigPage, selectWaiversSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import LicenseLockScreenForAutoWaivers from './LicenseLockScreenForAutoWaivers';
import ConfirmationModal from 'MainRoot/legal/application/ConfirmationModal';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';

const AutoWaiversConfiguration = () => {
  const dispatch = useDispatch();
  const { loading, loadError } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);

  const doLoad = () => dispatch(actions.loadAutoWaiversConfigurationPage());

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
      {isDeveloperDashboardEnabled && isAutoWaiversEnabled && !isSbomManager ? (
        <AutoWaiversConfigurationContents />
      ) : (
        <LicenseLockScreenForAutoWaivers />
      )}
    </NxLoadWrapper>
  );
};

function AutoWaiversConfigurationContents() {
  const dispatch = useDispatch();
  const [isDeleteConfirmationModalOpen, setIsDeleteConfirmationModalOpen] = useState(false);
  const setThreatLevel = (val) => dispatch(actions.setThreatLevel(val));

  const waiversConfigPage = useSelector(selectWaiversConfigPage);
  let { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectWaiversSlice);
  const reachable = waiversConfigPage?.reachable ?? false;
  const pathForward = waiversConfigPage?.pathForward ?? false;
  const threatLevel = waiversConfigPage?.threatLevel ?? setThreatLevel(7);
  const hasExistingWaiver = waiversConfigPage?.autoPolicyWaiverId != null;

  if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
    isDirty = true;
  }

  const handleDelete = () => {
    setIsDeleteConfirmationModalOpen(false);
    dispatch(actions.deleteAutoWaiver());
  };

  const shouldDeleteAutoWaiver = () => {
    return isDirty && !reachable && !pathForward && hasExistingWaiver;
  };

  const handleSubmit = () => {
    if (shouldDeleteAutoWaiver()) {
      setIsDeleteConfirmationModalOpen(true);
    } else if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
      dispatch(actions.createAutoWaiver());
    } else {
      dispatch(actions.saveAutoWaiversConfiguration());
    }
  };

  const validationError = () => {
    if (!reachable && !pathForward && !hasExistingWaiver) return 'Can not save without selecting at least one option';
    if (!isDirty) return MSG_NO_CHANGES_TO_SAVE;
    return undefined;
  };

  const doLoad = () => dispatch(actions.loadAutoWaiversConfigurationPage());

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
            <NxFieldset label="Max. Threat Level" sublabel="Violations with higher threats will not be waived">
              <div className="iq-waivers-configuration-upgrades">
                <ThreatDropdownSelector
                  className="edit-auto-waiver-threat-dropdown"
                  threatLevel={threatLevel}
                  onSelectThreatLevel={setThreatLevel}
                  id="editor-auto-waiver-threat-level"
                />
              </div>
            </NxFieldset>
            <NxFieldset label="Scope" sublabel="Eligible violations will be waived if/when:">
              <div className="iq-auto-waivers-configuration-upgrades-fieldset__item">
                <NxH4>No Upgrade Path</NxH4>
                <NxCheckbox onChange={() => dispatch(actions.toggleCheckboxPath())} isChecked={pathForward || false}>
                  No newer, non-violating component version is available
                </NxCheckbox>
              </div>
            </NxFieldset>
          </NxStatefulForm>
        </NxTile>
      </NxLoadWrapper>
      {isDeleteConfirmationModalOpen && (
        <ConfirmationModal
          id="delete-auto-waiver-modal"
          cancelHandler={() => setIsDeleteConfirmationModalOpen(false)}
          titleContent={<span>Confirm Delete</span>}
          confirmationMessage="Are you sure you want to delete this auto waiver configuration?"
          closeHandler={() => setIsDeleteConfirmationModalOpen(false)}
          confirmationHandler={handleDelete}
          confirmationButtonText="Delete"
        />
      )}
    </>
  );
}

export default AutoWaiversConfiguration;
