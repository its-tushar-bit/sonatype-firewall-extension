/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  NxH1,
  NxPageTitle,
  NxTile,
  NxStatefulForm,
  NxPageMain,
  NxFieldset,
  NxTextInput,
  NxFormSelect,
  NxRadio,
  NxDateInput,
  NxTooltip,
  NxErrorAlert,
  NxInfoAlert,
  NxTextLink,
} from '@sonatype/react-shared-components';
import LoadWrapper from '../react/LoadWrapper';
import AddAndRequestWaiversBackButton from './AddAndRequestWaiversBackButton';

import { extractViolationDetails } from '../util/violationDetailsUtil';
import { useDispatch, useSelector } from 'react-redux';

import {
  selectLoadingViolation,
  selectSubmitError,
  selectSubmitMaskState,
  selectViolationDetails,
  selectViolationDetailsError,
  selectWaiverReasons,
  selectAddWaiverData,
  selectAddWaiverDataLoading,
  selectAddWaiverDataError,
  selectSelectedWaiverScope,
  selectWaiverSelectedScopeLoading,
  selectWaiverSelectedScopeError,
  selectComponentMatcherStrategy,
  selectExpiryTime,
  selectCustomExpiryTime,
  selectWaiverReasonId,
  selectComments,
  selectNoteToReviewer,
} from './requestWaiverSelectors';
import {
  selectPreviousRouteName,
  selectRouterPrevParams,
  selectViolationId,
  selectIsStandaloneDeveloper,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import {
  selectIsExpireWhenRemediationAvailableWaiversEnabled,
  selectHasWaiverRequestWorkflow,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import TierTag from 'MainRoot/react/shared/TierTag';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import { loadViolation as loadViolationAction } from 'MainRoot/violation/violationActions';
import { actions } from './requestWaiverSlice';
import { loadAddWaiverData as loadAddWaiverDataAction, returnToAddOrRequestWaiverOriginPage } from './waiverActions';
import { actions as waiverActions } from './waiverSlice';
import { selectViolationSlice } from '../violation/violationSelectors';
import ArtifactNameDisplay from 'MainRoot/react/ArtifactNameDisplay';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';
import {
  useWaiverExpirations,
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  getExpirationDaysMessage,
  waiverMatcherStrategy,
  waiverRequestStatus,
} from 'MainRoot/util/waiverUtils';
import { find, propEq } from 'ramda';
import classnames from 'classnames';
import { actions as requestWaiverDetailsActions } from './requestWaiverDetails/requestWaiverDetailsSlice';
import {
  selectWaiverRequestDetails,
  selectWaiverRequestDetailsLoading,
  selectWaiverRequestDetailsError,
} from './requestWaiverDetails/requestWaiverDetailsSelectors';

const RequestWaiversPage = () => {
  const dispatch = useDispatch();
  const hasWaiverRequestWorkflow = useSelector(selectHasWaiverRequestWorkflow);

  const currentParams = useSelector(selectRouterCurrentParams);
  const { policyWaiverRequestId } = currentParams || {};
  const loading = useSelector(selectLoadingViolation);
  const selectedWaiverScopeLoading = useSelector(selectWaiverSelectedScopeLoading);
  const selectedWaiverScopeError = useSelector(selectWaiverSelectedScopeError);
  const waiverRequestDetailsLoading = useSelector(selectWaiverRequestDetailsLoading);
  const waiverRequestDetails = useSelector(selectWaiverRequestDetails);
  const name = useSelector(selectPreviousRouteName);
  const prevParams = useSelector(selectRouterPrevParams);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const { componentIdentifier, componentDisplayName } = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const isExpireWhenRemediationAvailable = useSelector(selectIsExpireWhenRemediationAvailableWaiversEnabled);
  const violationDetails = useSelector(selectViolationDetails);
  const violationDetailsError = useSelector(selectViolationDetailsError);
  const violationId = useSelector(selectViolationId);
  const { loadApplicableWaiversError, vulnerabilityDetailsError } = useSelector(selectViolationSlice);
  const waiverRequestDetailsError = useSelector(selectWaiverRequestDetailsError);
  const {
    availableWaiverScopes,
    selectedWaiverScope: initialSelectedWaiverScope,
    componentMatcherStrategy: initialComponentMatcherStrategy,
  } = useSelector(selectAddWaiverData);
  const addWaiverDataLoading = useSelector(selectAddWaiverDataLoading);
  const addWaiverDataError = useSelector(selectAddWaiverDataError);
  const waiverReasons = useSelector(selectWaiverReasons);
  const submitError = useSelector(selectSubmitError);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const selectedWaiverScope = useSelector(selectSelectedWaiverScope);
  const componentMatcherStrategy = useSelector(selectComponentMatcherStrategy);
  const expiryTime = useSelector(selectExpiryTime);
  const customExpiryTime = useSelector(selectCustomExpiryTime);
  const waiverReasonId = useSelector(selectWaiverReasonId);
  const waiverComments = useSelector(selectComments);
  const noteToReviewer = useSelector(selectNoteToReviewer);

  const loadViolation = (id) => dispatch(loadViolationAction(id));
  const loadAddWaiverData = (id) => dispatch(loadAddWaiverDataAction(id));
  const onSubmitAction = () => {
    if (policyWaiverRequestId) {
      // If policyWaiverRequestId exists, we're updating an existing waiver request
      return dispatch(actions.updatePolicyWaiverRequest({ expiration, expireWhenRemediationAvailableSelected }));
    } else {
      // Otherwise, we're creating a new waiver request
      return dispatch(actions.createRequestWaiver({ expiration, expireWhenRemediationAvailableSelected }));
    }
  };
  const cancelAction = () => dispatch(returnToAddOrRequestWaiverOriginPage());
  const loadSelectedWaiverScope = (target) => dispatch(actions.loadSelectedWaiverScope(target));
  const setSelectedWaiverScope = (target) => dispatch(actions.setSelectedWaiverScope(target));
  const setComponentMatcherStrategy = (strategy) => dispatch(actions.setComponentMatcherStrategy(strategy));
  const setExpiryTime = (time) => dispatch(actions.setExpiryTime(time));
  const setCustomExpiryTime = (time) => dispatch(actions.setCustomExpiryTime(time));
  const setWaiverReasonId = (reasonId) => dispatch(actions.setWaiverReasonId(reasonId));
  const setWaiverComments = (comment) => dispatch(actions.setRequestWaiverComments(comment));
  const setNoteToReviewer = (note) => dispatch(actions.setNoteToReviewer(note));

  // initialize read-only state
  const [isReadOnly, setIsReadOnly] = useState(false);

  const onSubmit = () => {
    // Don't submit if the form is read-only
    if (!isReadOnly) {
      onSubmitAction();
    }
  };

  const {
    policyName,
    artifactName,
    constraintName,
    componentName,
    allVersionsComponentName,
    reasons = [],
    threatLevelCategory,
  } = extractViolationDetails(violationDetails);

  const backButtonProps = {
    // On the requestWaiverUpdate route the page is keyed by policyWaiverRequestId and the URL has no
    // violationId, so fall back to the loaded waiver request's policyViolationId. Otherwise the
    // "Back to Violation Details" link points at the violation page with no id, which can't look up the
    // violation ("...does not have a valid database identifier") (CLM-41118).
    violationId: violationId || waiverRequestDetails?.policyViolationId,
    prevStateName: name,
    prevParams,
    isStandaloneDeveloper,
  };

  const error = policyWaiverRequestId
    ? waiverRequestDetailsError
    : violationId
    ? violationDetailsError || loadApplicableWaiversError || vulnerabilityDetailsError
    : 'No Violation ID or Waiver Request ID provided.';

  const load = () => {
    if (policyWaiverRequestId) {
      // When editing an existing waiver request, we'll load the violation after we get the waiver request details
      return;
    } else if (violationId) {
      // For new waiver requests, directly load using violationId
      loadViolation(violationId);
      loadAddWaiverData(violationId);
    }
  };

  const retryLoadHandler = () => {
    if (policyWaiverRequestId) {
      dispatch(requestWaiverDetailsActions.loadWaiverRequest());
    } else {
      load();
    }
  };

  useEffect(() => {
    dispatch(waiverActions.loadCachedWaiverReasons());
    dispatch(actions.clearInitState());
    // Clear any existing waiver request details
    dispatch(requestWaiverDetailsActions.clearWaiverRequestDetails());
  }, []);

  // Load waiver request details if policyWaiverRequestId is provided
  useEffect(() => {
    // Clear previous state when switching between different routes
    dispatch(actions.clearInitState());
    dispatch(requestWaiverDetailsActions.clearWaiverRequestDetails());

    if (policyWaiverRequestId) {
      dispatch(requestWaiverDetailsActions.loadWaiverRequest());
    }
  }, [policyWaiverRequestId, violationId]);

  // Initialize form with waiver request details when they are loaded
  useEffect(() => {
    if (waiverRequestDetails) {
      // Check status and make read-only if it's approved
      if (waiverRequestDetails.status === waiverRequestStatus.APPROVED) {
        setIsReadOnly(true);
      }
      // Now that we have waiver request details, load the associated violation data
      const violationId = waiverRequestDetails.policyViolationId;
      if (violationId) {
        loadViolation(violationId);
        loadAddWaiverData(violationId);
      }
      dispatch(actions.initializeStateFromDetails(waiverRequestDetails));
    }
  }, [waiverRequestDetails]);

  useEffect(() => {
    if (waiverRequestDetails) {
      loadSelectedWaiverScope({
        id: waiverRequestDetails.scopeOwnerId,
        type:
          waiverRequestDetails.scopeOwnerType === 'root_organization'
            ? 'organization'
            : waiverRequestDetails.scopeOwnerType,
        name: waiverRequestDetails.scopeOwnerName,
      });
    } else if (initialSelectedWaiverScope) {
      loadSelectedWaiverScope(initialSelectedWaiverScope);
    }
  }, [initialSelectedWaiverScope, waiverRequestDetails]);

  useEffect(() => {
    setComponentMatcherStrategy(initialComponentMatcherStrategy);
  }, [initialComponentMatcherStrategy]);

  useEffect(load, [violationId]);

  const replaceUnknownComponentNameByComponentDisplayName = (componentName) =>
    componentName === 'Unknown' ? componentDisplayName : componentName;

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    setSelectedWaiverScope(target);
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

  const handleComponentsChange = (value) => {
    setComponentMatcherStrategy(value);
  };

  const getAllVersionsRadioButton = () => {
    if (componentIdentifier === null) {
      return (
        <NxTooltip title="Claim this component to apply all versions waiver">
          <NxRadio
            id="all-versions"
            name="request-waiver-components"
            value={waiverMatcherStrategy.ALL_VERSIONS}
            isChecked={false}
            onChange={() => {}}
            disabled={true}
          >
            {allVersionsComponentName === 'Unknown' ? 'All Versions' : `${allVersionsComponentName} (all versions)`}
          </NxRadio>
        </NxTooltip>
      );
    } else {
      return (
        <NxRadio
          id="all-versions"
          name="add-waiver-components"
          value={waiverMatcherStrategy.ALL_VERSIONS}
          isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
          onChange={handleComponentsChange}
        >
          {allVersionsComponentName} (all versions)
        </NxRadio>
      );
    }
  };

  const onExpiryTimeChange = (value) => {
    setExpiryTime(value === 'never' ? null : value);
  };

  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);

  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);

  const neverExpiryTimeSelected = isNeverExpiryTimeSelected(expiryTime);

  const expireWhenRemediationAvailableSelected = isExpireWhenRemediationAvailableSelected(expiryTime);

  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  const getExpiration = () => {
    if (customExpiryTimeSelected) {
      return customExpiryTime.value;
    }
    if (neverExpiryTimeSelected || expireWhenRemediationAvailableSelected) {
      return null;
    }
    return parseInt(expiryTime, 10);
  };

  const expiration = getExpiration();

  const onReasonChange = (value) => {
    setWaiverReasonId(value ?? null);
  };

  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason', type: 'system' }, ...waiverReasons];

  // Only affects "update" - when editing an existing waiver request, we need to wait for the details to load
  const isLoadingWaiverRequestDetails = policyWaiverRequestId && waiverRequestDetailsLoading;

  return (
    <NxPageMain id="request-waiver-page">
      <AddAndRequestWaiversBackButton {...backButtonProps} />
      <NxPageTitle>
        <NxH1>
          Request Waiver
          {!hasWaiverRequestWorkflow && <TierTag>Enterprise Feature</TierTag>}
        </NxH1>
      </NxPageTitle>

      {/* Display error alert when waiver request is rejected */}
      {waiverRequestDetails && waiverRequestDetails.status === waiverRequestStatus.REJECTED && (
        <NxErrorAlert>
          This Waiver Request was rejected by {waiverRequestDetails.reviewerName} for the following reason:
          <br />
          {waiverRequestDetails.rejectionReason || 'No reason provided.'}
        </NxErrorAlert>
      )}

      <NxTile className={!hasWaiverRequestWorkflow ? 'iq-hide-form-footer iq-banner-flush-top' : ''}>
        {!hasWaiverRequestWorkflow && (
          <EnterpriseFullWidthBanner description="Enable your team to request waivers for policy violations with structured workflows and approval processes." />
        )}
        <NxTile.Content>
          <LoadWrapper
            loading={loading || addWaiverDataLoading || selectedWaiverScopeLoading || isLoadingWaiverRequestDetails}
            error={error || addWaiverDataError || selectedWaiverScopeError}
            retryHandler={retryLoadHandler}
          >
            {() => (
              <NxStatefulForm
                className="iq-request-waiver-form"
                onCancel={cancelAction}
                submitError={submitError}
                showValidationErrors={!!submitError}
                submitBtnClasses={classnames('request-waiver-submit', { disabled: isReadOnly })}
                onSubmit={onSubmit}
                submitMaskState={submitMaskState}
              >
                <header className="nx-tile-header">
                  <div className="nx-tile-header__title">
                    <h2 className="nx-h2">Waiver Configuration</h2>
                  </div>
                </header>

                <div className="nx-tile-content">
                  {/* Component Info */}
                  <div className="nx-read-only iq-request-waiver-form__component">
                    <header className="nx-read-only__label">
                      <ArtifactNameDisplay
                        {...{ artifactName: replaceUnknownComponentNameByComponentDisplayName(artifactName) }}
                      />
                    </header>
                    <div className="nx-read-only__data">{componentName}</div>
                  </div>

                  {/* Policy Info */}
                  <div className="nx-read-only iq-request-waiver-form__policy">
                    <header className="nx-read-only__label">Policy</header>
                    <div className="nx-read-only__data">
                      <ViolationExclamation threatLevelCategory={threatLevelCategory} />
                      <span className={policyClassnames}>{policyName}</span>
                    </div>
                  </div>

                  {/* Constraint Info */}
                  <div className="nx-read-only iq-request-waiver-form__constraint">
                    <header className="nx-read-only__label">Constraint Name</header>
                    <div className="nx-read-only__data">{constraintName}</div>
                  </div>

                  {/* Conditions */}
                  <div className="nx-read-only iq-request-waiver-form__conditions">
                    <header className="nx-read-only__label">Conditions</header>
                    {reasons &&
                      reasons.map((reason, index) => (
                        <div className="nx-read-only__data" key={index}>
                          <span>{reason}</span>
                        </div>
                      ))}
                  </div>

                  {/* Scope */}
                  <NxFieldset className="iq-request-waiver-form__scope" label="Scope" isRequired>
                    <IqScopeDropdown
                      id="iq-request-waiver-scope"
                      onChangeHandler={handleScopeChange}
                      availableScopes={availableWaiverScopes}
                      getOptionText={extractScopeOptionText}
                      currentValue={selectedWaiverScope.id}
                      isDisabled={isReadOnly}
                    />
                  </NxFieldset>

                  {/* Components */}
                  <NxFieldset className="iq-request-waiver-form__components" label="Components" isRequired>
                    <NxRadio
                      id="current-component"
                      name="request-waiver-components"
                      value={waiverMatcherStrategy.EXACT_COMPONENT}
                      isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                      onChange={handleComponentsChange}
                      disabled={isReadOnly}
                    >
                      {componentName}
                    </NxRadio>
                    {getAllVersionsRadioButton()}
                    <NxRadio
                      id="all-components"
                      name="request-waiver-components"
                      value={waiverMatcherStrategy.ALL_COMPONENTS}
                      isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_COMPONENTS}
                      onChange={handleComponentsChange}
                    >
                      All Components
                    </NxRadio>
                  </NxFieldset>

                  {/* Expiry time */}
                  <NxFieldset className="iq-request-waiver-form__expiryTime" label="Waiver Expiration" isRequired>
                    <div className="nx-form-row iq-request-waiver-form__expiryTime-block">
                      <div className="iq-request-waiver-form__select-block">
                        <NxFormSelect
                          id="waiver-expiration-select"
                          onChange={onExpiryTimeChange}
                          defaultValue={expiryTime}
                          disabled={isReadOnly}
                          aria-label="select waiver expiration"
                        >
                          {waiverExpirations.map(({ name, value }, index) => (
                            <option key={index} value={value}>
                              {name}
                            </option>
                          ))}
                        </NxFormSelect>
                        <div className="iq-request-waiver-form__expiration-days-diff visual-testing-ignore">
                          {daysDiffMessage}
                        </div>
                        {customExpiryTimeSelected && (
                          <NxDateInput
                            className="iq-request-waiver-form__date-input"
                            {...customExpiryTime}
                            onChange={setCustomExpiryTime}
                            validatable={true}
                            disabled={isReadOnly}
                          />
                        )}
                      </div>
                    </div>
                  </NxFieldset>

                  {/* Reason */}
                  <NxFieldset className="iq-request-waiver-form__reason" label="Reason">
                    <NxFormSelect id="waiver-reason-select" onChange={onReasonChange} disabled={isReadOnly}>
                      {waiverReasonsToRender.map(({ id, reasonText }) => (
                        <option key={id} value={id} selected={waiverReasonId && id === waiverReasonId}>
                          {reasonText}
                        </option>
                      ))}
                    </NxFormSelect>
                  </NxFieldset>

                  {/* Comments */}
                  <NxFieldset className="iq-request-waiver-form__comments" label="Comments">
                    <NxTextInput
                      type="textarea"
                      inputAttributes={{ maxLength: 1000 }}
                      {...waiverComments}
                      onChange={setWaiverComments}
                      disabled={isReadOnly}
                    />
                  </NxFieldset>

                  {/* Note to reviewer */}
                  <NxFieldset
                    className="iq-request-waiver-form__note-to-reviewer"
                    label="Note to Reviewer"
                    sublabel="This note will only be visible on the waiver request. It will not be visible on the waiver if it is approved."
                  >
                    <NxTextInput
                      type="textarea"
                      inputAttributes={{ maxLength: 1000 }}
                      {...noteToReviewer}
                      onChange={setNoteToReviewer}
                      disabled={isReadOnly}
                    />
                  </NxFieldset>
                </div>
              </NxStatefulForm>
            )}
          </LoadWrapper>
        </NxTile.Content>
        {!hasWaiverRequestWorkflow && (
          <NxInfoAlert>
            This is an Enterprise feature. Changes can&apos;t be saved. Go back to{' '}
            <NxTextLink onClick={cancelAction}>Component Details</NxTextLink>
          </NxInfoAlert>
        )}
      </NxTile>
    </NxPageMain>
  );
};

export default RequestWaiversPage;
