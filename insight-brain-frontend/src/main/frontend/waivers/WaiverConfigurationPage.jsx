/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxButton,
  NxButtonBar,
  NxPageMain,
  NxTile,
  NxH2,
  NxFieldset,
  NxFormSelect,
  NxTextInput,
  NxRadio,
  NxDateInput,
  NxLoadWrapper,
  NxInfoAlert,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {
  selectWaiverReasons,
  selectAddWaiverData,
  selectCustomExpiryTime,
  selectWaiverReasonsState,
  selectAddWaiverDataLoading,
  selectAddWaiverDataError,
} from './requestWaiverSelectors';
import {
  selectBulkWaiverConfiguration,
  selectBulkWaiverSelectedViolations,
  selectHasMixedViolations,
  selectOnlyUnknownViolations,
} from './bulkWaiverSelectors';
import { actions as waiverActions } from './waiverSlice';
import { actions as requestWaiverActions } from './requestWaiverSlice';
import {
  useWaiverExpirations,
  waiverMatcherStrategy,
  isCustomExpiryTimeSelected,
  isCustomExpiryTimeValid,
  getExpirationDaysMessage,
} from 'MainRoot/util/waiverUtils';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';
import { loadAddWaiverData } from './waiverActions';
import { find, propEq } from 'ramda';
import { cancelBulkWaive, goToBulkWaivePage, goToWaiverConfirmationPage } from './waiverActions';
import BulkWaiveTitle from './BulkWaiveTitle';

export default function WaiverConfigurationPage() {
  const dispatch = useDispatch();
  const selectedViolations = useSelector(selectBulkWaiverSelectedViolations);
  const selectedViolationsCount = selectedViolations.length;
  const waiverReasons = useSelector(selectWaiverReasons);
  const { availableWaiverScopes } = useSelector(selectAddWaiverData);
  const waiverReasonsState = useSelector(selectWaiverReasonsState);
  const waiverScopeLoading = useSelector(selectAddWaiverDataLoading);
  const waiverScopeError = useSelector(selectAddWaiverDataError);
  const customExpiryTime = useSelector(selectCustomExpiryTime);
  const waiverConfiguration = useSelector(selectBulkWaiverConfiguration);
  const hasMixedViolations = useSelector(selectHasMixedViolations);
  const onlyUnknownViolations = useSelector(selectOnlyUnknownViolations);
  const loading = waiverReasonsState?.loading || waiverScopeLoading;
  const error = () => {
    if (waiverReasonsState?.loadError) {
      return 'Error loading waiver reasons';
    }
    if (waiverScopeError) {
      return 'Error loading waiver scope';
    }
    return '';
  };

  // Local state for selected waiver reason, expiration, comments, and component matcher
  const [waiverReasonId, setWaiverReasonId] = useState(waiverConfiguration?.waiverReasonId || '');
  const [expiryTime, setExpiryTime] = useState(waiverConfiguration?.expiryTime || '');
  const [comments, setComments] = useState(waiverConfiguration?.comments || '');
  const [componentMatcherStrategy, setComponentMatcherStrategy] = useState(
    waiverConfiguration?.componentMatcherStrategy || waiverMatcherStrategy.ALL_VERSIONS
  );
  const [selectedWaiverScope, setSelectedWaiverScope] = useState(
    waiverConfiguration?.selectedWaiverScope || availableWaiverScopes?.[0]
  );

  // Get waiver expiration options
  const waiverExpirations = useWaiverExpirations(false);

  // Check if custom expiry is selected
  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);

  // Get expiration message
  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  const doLoad = () => {
    if (!isNilOrEmpty(selectedViolations)) {
      const firstViolation = selectedViolations[0];
      if (firstViolation.policyViolationId) {
        dispatch(loadAddWaiverData(firstViolation.policyViolationId));
      }
    }
  };

  // Load waiver data when component mounts using the first selected violation
  useEffect(() => {
    doLoad();
  }, []);

  // Force "Exact" selection when only unknown violations are selected
  useEffect(() => {
    if (onlyUnknownViolations && componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS) {
      setComponentMatcherStrategy(waiverMatcherStrategy.EXACT_COMPONENT);
    }
  }, [onlyUnknownViolations]);

  const onReasonChange = (value) => {
    setWaiverReasonId(value ?? '');
  };

  const onExpiryTimeChange = (value) => {
    setExpiryTime(value);
  };

  const onCommentsChange = (value) => {
    setComments(value);
  };

  const setCustomExpiryTime = (time) => {
    dispatch(requestWaiverActions.setCustomExpiryTime(time));
  };

  const handleComponentMatcherStrategyChange = (value) => {
    setComponentMatcherStrategy(value);
  };

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    setSelectedWaiverScope(target);
  };

  // Extract option text for scope dropdown
  const extractScopeOptionText = ({ label, name }) => {
    switch (label) {
      case 'Repository_container':
        return name;
      default:
        return `${label} - ${name}`;
    }
  };

  // Create dropdown options
  const waiverReasonsToRender = waiverReasons || [];

  const cancelClick = () => {
    dispatch(waiverActions.clearBulkWaiveCheckboxes());
    dispatch(waiverActions.resetWaiverConfiguration());
    dispatch(cancelBulkWaive());
  };

  const backClick = () => {
    dispatch(goToBulkWaivePage());
  };

  const nextClick = () => {
    // Collect all form data
    const waiverConfiguration = {
      waiverReasonId: waiverReasonId || null,
      expiryTime: expiryTime === 'never' ? null : expiryTime,
      customExpiryTime: customExpiryTimeSelected ? customExpiryTime : null,
      comments,
      componentMatcherStrategy,
      selectedWaiverScope: selectedWaiverScope || availableWaiverScopes?.[0],
    };

    // Store configuration in redux state
    dispatch(waiverActions.setWaiverConfiguration(waiverConfiguration));
    // Clear any previous submission errors before navigating to confirmation page
    dispatch(waiverActions.resetBulkWaiverSubmitState());
    dispatch(goToWaiverConfirmationPage());
  };

  if (isNilOrEmpty(selectedViolations) || !selectedViolations.length) {
    // The user managed to navigate here without going via the BulWaivePage
    dispatch(goToBulkWaivePage());
    return null;
  }

  return (
    <NxPageMain className="iq-bulk-waiver-configuration-page">
      <NxLoadWrapper loading={loading} error={error()} retryHandler={doLoad}>
        <BulkWaiveTitle />

        <NxTile>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Waiver configuration for {selectedViolationsCount} selected violations</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxFieldset label="Scope" isRequired>
              <IqScopeDropdown
                id="bulk-waiver-scope"
                onChangeHandler={handleScopeChange}
                availableScopes={availableWaiverScopes}
                getOptionText={extractScopeOptionText}
                currentValue={selectedWaiverScope?.id}
              />
            </NxFieldset>

            <NxFieldset label="Components" isRequired>
              <NxRadio
                id="exact-component"
                name="bulk-waiver-components"
                value={waiverMatcherStrategy.EXACT_COMPONENT}
                isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                onChange={handleComponentMatcherStrategyChange}
              >
                Exact
              </NxRadio>
              {onlyUnknownViolations ? (
                <NxTooltip title="Claim these components to apply all versions waiver">
                  <NxRadio
                    id="all-versions"
                    name="bulk-waiver-components"
                    value={waiverMatcherStrategy.ALL_VERSIONS}
                    isChecked={false}
                    onChange={() => {}}
                    disabled={true}
                  >
                    All Versions
                  </NxRadio>
                </NxTooltip>
              ) : (
                <NxRadio
                  id="all-versions"
                  name="bulk-waiver-components"
                  value={waiverMatcherStrategy.ALL_VERSIONS}
                  isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
                  onChange={handleComponentMatcherStrategyChange}
                >
                  All Versions
                </NxRadio>
              )}
            </NxFieldset>

            {hasMixedViolations && componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS && (
              <NxInfoAlert id="iq-bulk-waiver-mixed-violations-alert">
                The selected violations contain unknown/unclaimed components. When &quot;All Versions&quot; is selected,
                the bulk waiver will only apply to identified components.
              </NxInfoAlert>
            )}

            <NxFieldset label="Waiver Expiration" isRequired>
              <div className="iq-bulk-waiver-form__select-block">
                <NxFormSelect id="iq-bulk-waiver-expiry-select" value={expiryTime} onChange={onExpiryTimeChange}>
                  <option value="" disabled>
                    Select
                  </option>
                  {waiverExpirations.map(({ name, value }, index) => (
                    <option key={index} value={value}>
                      {name}
                    </option>
                  ))}
                </NxFormSelect>
                {customExpiryTimeSelected && (
                  <NxDateInput
                    className="iq-bulk-waiver-form__date-input"
                    {...customExpiryTime}
                    onChange={setCustomExpiryTime}
                    validatable={true}
                    aria-label="set custom expiration date"
                  />
                )}
              </div>
              {daysDiffMessage && expiryTime && expiryTime !== '' && (
                <div className="iq-bulk-waiver-form__expiration-days-diff visual-testing-ignore">{daysDiffMessage}</div>
              )}
            </NxFieldset>

            <NxFieldset label="Reason">
              <NxFormSelect id="iq-bulk-waiver-reason-select" value={waiverReasonId} onChange={onReasonChange}>
                <option value="" disabled>
                  Select
                </option>
                {waiverReasonsToRender.map(({ id, reasonText }) => (
                  <option key={id} value={id}>
                    {reasonText}
                  </option>
                ))}
              </NxFormSelect>
            </NxFieldset>

            <NxFieldset label="Comments">
              <div className="iq-bulk-waiver-form__comments">
                <NxTextInput
                  type="textarea"
                  inputAttributes={{ maxLength: 1000 }}
                  value={comments}
                  onChange={onCommentsChange}
                  className="nx-text-input--full"
                  isPristine={comments === ''}
                />
              </div>
            </NxFieldset>

            <NxButtonBar>
              <NxButton variant="tertiary" onClick={cancelClick}>
                Cancel
              </NxButton>
              <NxButton variant="secondary" onClick={backClick}>
                Back
              </NxButton>
              <NxButton
                variant="primary"
                onClick={nextClick}
                disabled={
                  !expiryTime || (customExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime?.value))
                }
              >
                Next
              </NxButton>
            </NxButtonBar>
          </NxTile.Content>
        </NxTile>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
