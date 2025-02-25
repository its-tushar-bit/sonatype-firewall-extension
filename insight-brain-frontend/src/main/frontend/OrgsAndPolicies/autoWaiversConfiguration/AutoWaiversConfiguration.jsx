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
  selectIsNewScanProcessEnabled,
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
  NxInfoAlert,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/OrgsAndPolicies/automatedWaiversSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import './_autoWaiversConfiguration.scss';
import { selectWaiversConfigPage, selectWaiversSlice } from 'MainRoot/OrgsAndPolicies/automatedWaiversSelectors';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import LicenseLockScreenForAutoWaivers from './LicenseLockScreenForAutoWaivers';
import ConfirmationModal from 'MainRoot/legal/application/ConfirmationModal';
import ThreatDropdownSelector from 'MainRoot/react/ThreatDropdownSelector';
import ExclusionLogTable from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/ExclusionLogTable';
import { selectExclusions } from 'MainRoot/OrgsAndPolicies/automatedWaiversExclusionsSelector';
import { isNil } from 'ramda';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
const AutoWaiversConfiguration = () => {
  const dispatch = useDispatch();
  const { loading, loadError, productFeatures } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);

  const doLoad = () => {
    if (isNilOrEmpty(productFeatures)) {
      dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
    }
  };

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
  const exclusions = useSelector(selectExclusions);
  const { loading, loadError, isDirty, submitMaskState, submitError } = useSelector(selectWaiversSlice);
  const isNewScanProcessEnabled = useSelector(selectIsNewScanProcessEnabled);
  const reachability = isNewScanProcessEnabled ? waiversConfigPage?.reachability ?? false : false;
  const pathForward = waiversConfigPage?.pathForward ?? false;
  const threatLevel = waiversConfigPage?.threatLevel ?? 7;
  const hasExistingWaiver = !isNil(waiversConfigPage?.autoPolicyWaiverId);

  useEffect(() => {
    if (isNil(waiversConfigPage?.threatLevel)) {
      setThreatLevel(7);
      dispatch(actions.setIsDirty(false)); // reset dirty flag since this isn't a user-made change
    }
  }, [waiversConfigPage.threatLevel]);

  const handleDelete = () => {
    setIsDeleteConfirmationModalOpen(false);
    dispatch(actions.deleteAutoWaiver());
  };

  const invalidConfigState = !isNewScanProcessEnabled && waiversConfigPage?.reachability && !pathForward;

  const shouldDeleteAutoWaiver = (isDirty && !reachability && !pathForward && hasExistingWaiver) || invalidConfigState;

  const handleSubmit = () => {
    if (shouldDeleteAutoWaiver) {
      setIsDeleteConfirmationModalOpen(true);
    } else if (waiversConfigPage?.isInherited === null || waiversConfigPage?.isInherited === true) {
      dispatch(actions.createAutoWaiver());
    } else {
      dispatch(actions.saveAutoWaiversConfiguration());
    }
  };

  const validationError = () => {
    if (!reachability && !pathForward && !hasExistingWaiver)
      return 'Can not save without selecting at least one option';
    if (invalidConfigState) return undefined;
    if (!isDirty) return MSG_NO_CHANGES_TO_SAVE;
    return undefined;
  };

  const doLoad = () => dispatch(actions.loadAllAutoWaiverData());

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <div data-testid="auto-waivers-configuration">
      <NxPageTitle>
        <NxH1>Automated Waivers</NxH1>
        <NxPageTitle.Description>
          Limit disruptions by deprioritizing low-threat violations until a remediation path is available.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile aria-label="Configure Auto-Waiver">
          <NxStatefulForm
            submitBtnText={shouldDeleteAutoWaiver ? 'Delete Auto Waiver' : 'Update'}
            submitMaskState={submitMaskState}
            submitMaskMessage="Saving…"
            onSubmit={handleSubmit}
            validationErrors={validationError()}
            submitError={submitError}
          >
            <NxH2>Configure Auto-Waiver</NxH2>
            {waiversConfigPage?.isInherited === true && (
              <NxInfoAlert>
                Automated waivers are enabled for the parent organization. Changes made here will only affect this
                application.
              </NxInfoAlert>
            )}
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
                <NxTooltip title={waiversConfigPage?.isInherited ? 'Inheriting from parent organization' : ''}>
                  <NxCheckbox
                    onChange={() => dispatch(actions.toggleCheckboxPath())}
                    isChecked={pathForward || false}
                    disabled={waiversConfigPage?.isInherited}
                  >
                    No newer, non-violating component version is available
                  </NxCheckbox>
                </NxTooltip>
              </div>
              {isNewScanProcessEnabled && (
                <div className="iq-auto-waivers-configuration-upgrades-fieldset__item">
                  <div className="iq-auto-waivers-configuration-upgrades-fieldset__item-reachability-header">
                    <NxH4>Reachability Analysis</NxH4>
                    <NxTooltip title="Callflow must be enabled via Jenkins or Sonatype CLI">
                      <NxFontAwesomeIcon
                        data-testid="auto-waivers-configuration-reachability-icon"
                        icon={faInfoCircle}
                        className="iq-auto-waivers-configuration-upgrades-fieldset__item-icon"
                      />
                    </NxTooltip>
                  </div>
                  <NxTooltip title={waiversConfigPage?.isInherited ? 'Inheriting from parent organization' : ''}>
                    <NxCheckbox
                      onChange={() => dispatch(actions.toggleCheckboxReachability())}
                      isChecked={reachability || false}
                      disabled={waiversConfigPage?.isInherited}
                    >
                      Security vulnerability is <ReachabilityStatus reachabilityStatus={'NOT_REACHABLE'} />
                    </NxCheckbox>
                  </NxTooltip>
                </div>
              )}
            </NxFieldset>
          </NxStatefulForm>
        </NxTile>

        <NxTile className="iq-exclusion-log-tile">
          <NxH2>Exclusion Log</NxH2>
          <ExclusionLogTable exclusions={exclusions || []} />
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
    </div>
  );
}

export default AutoWaiversConfiguration;
