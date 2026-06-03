/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxStatefulForm,
  NxFieldset,
  NxTextInput,
  NxFormSelect,
  NxRadio,
  NxDateInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import classnames from 'classnames';

import LoadWrapper from 'MainRoot/react/LoadWrapper';
import AddAndRequestWaiversBackButton from 'MainRoot/waivers/AddAndRequestWaiversBackButton';
import ArtifactNameDisplay from 'MainRoot/react/ArtifactNameDisplay';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';

import { extractViolationDetails } from 'MainRoot/util/violationDetailsUtil';
import {
  useWaiverExpirations,
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  getExpirationDaysMessage,
  waiverMatcherStrategy,
} from 'MainRoot/util/waiverUtils';

import { loadFirewallViolationDetails } from 'MainRoot/firewall/firewallActions';
import {
  loadAddWaiverData as loadAddWaiverDataAction,
  returnToAddOrRequestWaiverOriginPage,
} from 'MainRoot/waivers/waiverActions';
import { actions as waiverSliceActions } from 'MainRoot/waivers/waiverSlice';

import { selectViolationSlice } from 'MainRoot/violation/violationSelectors';
import {
  selectWaiverReasons,
  selectAddWaiverData,
  selectAddWaiverDataLoading,
  selectAddWaiverDataError,
} from 'MainRoot/waivers/requestWaiverSelectors';
import {
  selectViolationId,
  selectRepositoryId,
  selectPreviousRouteName,
  selectRouterPrevParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectFirewallComponentDetailsPageRouteParams,
  selectFirewallViolationDetails,
  selectFirewallIsLoading,
  selectFirewallLoadingError,
} from 'MainRoot/firewall/firewallSelectors';
import { selectIsExpireWhenRemediationAvailableWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

import { find, propEq } from 'ramda';

import { actions } from './firewallRequestWaiverSlice';
import {
  selectFirewallRequestWaiverSubmitError,
  selectFirewallRequestWaiverSubmitMaskState,
  selectFirewallRequestWaiverComponentMatcherStrategy,
  selectFirewallRequestWaiverSelectedScope,
  selectFirewallRequestWaiverExpiryTime,
  selectFirewallRequestWaiverCustomExpiryTime,
  selectFirewallRequestWaiverReasonId,
  selectFirewallRequestWaiverComments,
  selectFirewallRequestWaiverNoteToReviewer,
} from './firewallRequestWaiverSelectors';

export default function FirewallRequestWaiverPage() {
  const dispatch = useDispatch();

  // Route params
  const violationId = useSelector(selectViolationId);
  const repositoryId = useSelector(selectRepositoryId);
  const prevStateName = useSelector(selectPreviousRouteName);
  const prevParams = useSelector(selectRouterPrevParams);
  const { componentIdentifier, componentDisplayName } = useSelector(selectFirewallComponentDetailsPageRouteParams);

  // Violation data
  const violationLoading = useSelector(selectFirewallIsLoading);
  const violationDetailsError = useSelector(selectFirewallLoadingError);
  const violationDetails = useSelector(selectFirewallViolationDetails);
  const { loadApplicableWaiversError } = useSelector(selectViolationSlice);

  // Add-waiver data (scopes, initial matcher strategy)
  const {
    availableWaiverScopes,
    selectedWaiverScope: initialSelectedWaiverScope,
    componentMatcherStrategy: initialComponentMatcherStrategy,
  } = useSelector(selectAddWaiverData);
  const addWaiverDataLoading = useSelector(selectAddWaiverDataLoading);
  const addWaiverDataError = useSelector(selectAddWaiverDataError);

  // Waiver reasons
  const waiverReasons = useSelector(selectWaiverReasons);

  // Feature flags
  const isExpireWhenRemediationAvailable = useSelector(selectIsExpireWhenRemediationAvailableWaiversEnabled);

  // Page-specific slice state
  const componentMatcherStrategy = useSelector(selectFirewallRequestWaiverComponentMatcherStrategy);
  const selectedWaiverScope = useSelector(selectFirewallRequestWaiverSelectedScope);
  const expiryTime = useSelector(selectFirewallRequestWaiverExpiryTime);
  const customExpiryTime = useSelector(selectFirewallRequestWaiverCustomExpiryTime);
  const waiverReasonId = useSelector(selectFirewallRequestWaiverReasonId);
  const comments = useSelector(selectFirewallRequestWaiverComments);
  const noteToReviewer = useSelector(selectFirewallRequestWaiverNoteToReviewer);
  const submitError = useSelector(selectFirewallRequestWaiverSubmitError);
  const submitMaskState = useSelector(selectFirewallRequestWaiverSubmitMaskState);

  // Actions
  const cancelAction = () => dispatch(returnToAddOrRequestWaiverOriginPage());
  const setComponentMatcherStrategy = (value) => dispatch(actions.setComponentMatcherStrategy(value));
  const setExpiryTime = (value) => dispatch(actions.setExpiryTime(value === 'never' ? null : value));
  const setCustomExpiryTime = (value) => dispatch(actions.setCustomExpiryTime(value));
  const setWaiverReasonId = (value) => dispatch(actions.setWaiverReasonId(value ?? null));
  const setComments = (value) => dispatch(actions.setComments(value));
  const setNoteToReviewer = (value) => dispatch(actions.setNoteToReviewer(value));

  const load = () => {
    if (violationId) {
      dispatch(loadFirewallViolationDetails(violationId));
      dispatch(loadAddWaiverDataAction(violationId));
    }
  };

  useEffect(() => {
    dispatch(waiverSliceActions.loadCachedWaiverReasons());
    dispatch(actions.clearState());
  }, []);

  useEffect(load, [violationId]);

  useEffect(() => {
    if (initialSelectedWaiverScope) {
      dispatch(actions.setSelectedWaiverScope(initialSelectedWaiverScope));
    }
  }, [initialSelectedWaiverScope]);

  useEffect(() => {
    if (initialComponentMatcherStrategy) {
      dispatch(actions.setComponentMatcherStrategy(initialComponentMatcherStrategy));
    }
  }, [initialComponentMatcherStrategy]);

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    dispatch(actions.setSelectedWaiverScope(target));
  };

  const extractScopeOptionText = ({ label, name }) => {
    switch (label) {
      case 'Repository_container':
        return name;
      case 'Repository_manager':
        return `Repository Manager - ${name}`;
      default:
        return `${label} - ${name}`;
    }
  };

  // Derived violation info
  const {
    policyName,
    artifactName,
    constraintName,
    componentName,
    allVersionsComponentName,
    reasons = [],
    threatLevelCategory,
  } = extractViolationDetails(Array.isArray(violationDetails) ? null : violationDetails);

  const replaceUnknownByDisplayName = (name) => (name === 'Unknown' ? componentDisplayName : name);

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  // Expiry helpers
  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);
  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);
  const expireWhenRemediationAvailableSelected = isExpireWhenRemediationAvailableSelected(expiryTime);
  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  const getExpiration = () => {
    if (customExpiryTimeSelected) return customExpiryTime.value;
    if (isNeverExpiryTimeSelected(expiryTime) || expireWhenRemediationAvailableSelected) return null;
    return parseInt(expiryTime, 10);
  };

  const expiration = getExpiration();

  const onSubmit = () => {
    dispatch(actions.submitFirewallWaiverRequest({ expiration, expireWhenRemediationAvailableSelected }));
  };

  // Component radio buttons
  const getAllVersionsRadioButton = () => {
    if (componentIdentifier === null) {
      return (
        <NxTooltip title="Claim this component to apply all versions waiver">
          <NxRadio
            id="fw-rw-all-versions"
            name="fw-rw-components"
            value={waiverMatcherStrategy.ALL_VERSIONS}
            isChecked={false}
            onChange={() => {}}
            disabled
          >
            {allVersionsComponentName === 'Unknown' ? 'All Versions' : `${allVersionsComponentName} (all versions)`}
          </NxRadio>
        </NxTooltip>
      );
    }
    return (
      <NxRadio
        id="fw-rw-all-versions"
        name="fw-rw-components"
        value={waiverMatcherStrategy.ALL_VERSIONS}
        isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
        onChange={setComponentMatcherStrategy}
      >
        {allVersionsComponentName} (all versions)
      </NxRadio>
    );
  };

  // Reason dropdown
  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason', type: 'system' }, ...(waiverReasons || [])];

  // Back button
  const backButtonProps = { violationId, prevStateName, prevParams, isFirewallOrRepositoryComponent: true };

  // Loading / error
  const loading = violationLoading || addWaiverDataLoading;
  const error = violationId ? violationDetailsError || loadApplicableWaiversError : 'No Violation ID provided.';

  return (
    <main id="firewall-request-waiver-page" className="nx-page-main">
      <AddAndRequestWaiversBackButton {...backButtonProps} />
      <div className="nx-page-title">
        <h1 className="nx-h1">Request Waiver</h1>
      </div>

      <LoadWrapper loading={loading} error={error || addWaiverDataError} retryHandler={load}>
        {() => (
          <NxStatefulForm
            className="iq-firewall-request-waiver-form"
            onCancel={cancelAction}
            submitError={submitError}
            showValidationErrors={!!submitError}
            onSubmit={onSubmit}
            submitMaskState={submitMaskState}
          >
            <header className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">Waiver Configuration</h2>
              </div>
            </header>

            <div className="nx-tile-content">
              {/* Component */}
              <div className="nx-read-only iq-firewall-request-waiver-form__component">
                <header className="nx-read-only__label">
                  <ArtifactNameDisplay artifactName={replaceUnknownByDisplayName(artifactName)} />
                </header>
                <div className="nx-read-only__data">{componentName}</div>
              </div>

              {/* Repository */}
              <div className="nx-read-only iq-firewall-request-waiver-form__repository">
                <header className="nx-read-only__label">Repository</header>
                <div className="nx-read-only__data">{repositoryId}</div>
              </div>

              {/* Policy */}
              <div className="nx-read-only iq-firewall-request-waiver-form__policy">
                <header className="nx-read-only__label">Policy</header>
                <div className="nx-read-only__data">
                  <ViolationExclamation threatLevelCategory={threatLevelCategory} />
                  <span className={policyClassnames}>{policyName}</span>
                </div>
              </div>

              {/* Constraint */}
              <div className="nx-read-only iq-firewall-request-waiver-form__constraint">
                <header className="nx-read-only__label">Constraint Name</header>
                <div className="nx-read-only__data">{constraintName}</div>
              </div>

              {/* Conditions */}
              <div className="nx-read-only iq-firewall-request-waiver-form__conditions">
                <header className="nx-read-only__label">Conditions</header>
                {reasons.map((reason, index) => (
                  <div className="nx-read-only__data" key={index}>
                    <span>{reason}</span>
                  </div>
                ))}
              </div>

              {/* Scope */}
              <NxFieldset className="iq-firewall-request-waiver-form__scope" label="Scope" isRequired>
                <IqScopeDropdown
                  id="fw-rw-scope"
                  onChangeHandler={handleScopeChange}
                  availableScopes={availableWaiverScopes}
                  getOptionText={extractScopeOptionText}
                  currentValue={selectedWaiverScope?.id}
                />
              </NxFieldset>

              {/* Components matcher */}
              <NxFieldset className="iq-firewall-request-waiver-form__components" label="Components" isRequired>
                <NxRadio
                  id="fw-rw-current-component"
                  name="fw-rw-components"
                  value={waiverMatcherStrategy.EXACT_COMPONENT}
                  isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                  onChange={setComponentMatcherStrategy}
                >
                  {componentName}
                </NxRadio>
                {getAllVersionsRadioButton()}
                <NxRadio
                  id="fw-rw-all-components"
                  name="fw-rw-components"
                  value={waiverMatcherStrategy.ALL_COMPONENTS}
                  isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_COMPONENTS}
                  onChange={setComponentMatcherStrategy}
                >
                  All Components
                </NxRadio>
              </NxFieldset>

              {/* Expiry time */}
              <NxFieldset className="iq-firewall-request-waiver-form__expiryTime" label="Waiver Expiration" isRequired>
                <div className="nx-form-row iq-firewall-request-waiver-form__expiryTime-block">
                  <div className="iq-firewall-request-waiver-form__select-block">
                    <NxFormSelect
                      id="fw-rw-expiration-select"
                      onChange={setExpiryTime}
                      defaultValue={expiryTime}
                      aria-label="select waiver expiration"
                    >
                      {waiverExpirations.map(({ name, value }, index) => (
                        <option key={index} value={value}>
                          {name}
                        </option>
                      ))}
                    </NxFormSelect>
                    <div className="iq-firewall-request-waiver-form__expiration-days-diff visual-testing-ignore">
                      {daysDiffMessage}
                    </div>
                    {customExpiryTimeSelected && (
                      <NxDateInput
                        className="iq-firewall-request-waiver-form__date-input"
                        {...customExpiryTime}
                        onChange={setCustomExpiryTime}
                        validatable
                      />
                    )}
                  </div>
                </div>
              </NxFieldset>

              {/* Reason */}
              <NxFieldset className="iq-firewall-request-waiver-form__reason" label="Reason">
                <NxFormSelect id="fw-rw-reason-select" onChange={setWaiverReasonId}>
                  {waiverReasonsToRender.map(({ id, reasonText }) => (
                    <option key={id} value={id} selected={waiverReasonId && id === waiverReasonId}>
                      {reasonText}
                    </option>
                  ))}
                </NxFormSelect>
              </NxFieldset>

              {/* Comments */}
              <NxFieldset className="iq-firewall-request-waiver-form__comments" label="Comments">
                <NxTextInput
                  type="textarea"
                  inputAttributes={{ maxLength: 1000 }}
                  {...comments}
                  onChange={setComments}
                />
              </NxFieldset>

              {/* Note to Reviewer */}
              <NxFieldset
                className="iq-firewall-request-waiver-form__note-to-reviewer"
                label="Note to Reviewer"
                sublabel="This note will only be visible on the waiver request. It will not be visible on the waiver if it is approved."
              >
                <NxTextInput
                  type="textarea"
                  inputAttributes={{ maxLength: 1000 }}
                  {...noteToReviewer}
                  onChange={setNoteToReviewer}
                />
              </NxFieldset>
            </div>
          </NxStatefulForm>
        )}
      </LoadWrapper>
    </main>
  );
}
