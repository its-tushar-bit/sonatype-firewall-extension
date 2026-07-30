/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  NxPageTitle,
  NxTile,
  NxH1,
  NxH2,
  NxStatefulForm,
  NxPageMain,
  NxFieldset,
  NxTextInput,
  NxFormSelect,
  NxRadio,
  NxDateInput,
  NxTooltip,
  NxBlockquote,
  NxButton,
  NxModal,
  NxErrorAlert,
} from '@sonatype/react-shared-components';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import AddAndRequestWaiversBackButton from './AddAndRequestWaiversBackButton';

import { useDispatch, useSelector } from 'react-redux';

import {
  selectWaiverRequestDetails,
  selectWaiverRequestDetailsLoading,
  selectWaiverRequestDetailsError,
} from 'MainRoot/waivers/requestWaiverDetails/requestWaiverDetailsSelectors';
import {
  selectSubmitError,
  selectSubmitMaskState,
  selectViolationDetails,
  selectWaiverReasons,
  selectAddWaiverData,
  selectSelectedWaiverScope,
  selectComponentMatcherStrategy,
  selectExpiryTime,
  selectCustomExpiryTime,
  selectWaiverReasonId,
  selectComments,
  selectViolationDetailsError,
  selectLoadingViolation,
  selectAddWaiverDataLoading,
  selectAddWaiverDataError,
  selectWaiverSelectedScopeLoading,
  selectWaiverSelectedScopeError,
  selectRejectionReason,
} from 'MainRoot/waivers/requestWaiverSelectors';
import { selectPreviousRouteName, selectRouterPrevParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import { actions } from 'MainRoot/waivers/requestWaiverDetails/requestWaiverDetailsSlice';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import { actions as requestWaiverActions } from 'MainRoot/waivers/requestWaiverSlice';
import { loadViolation as loadViolationAction } from 'MainRoot/violation/violationActions';
import {
  loadAddWaiverData as loadAddWaiverDataAction,
  returnToReviewWaiverRequestOriginPage,
} from 'MainRoot/waivers/waiverActions';
import {
  formatCustomDate,
  getExpirationDaysMessage,
  isCustomExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  isNeverExpiryTimeSelected,
  useWaiverExpirations,
  waiverMatcherStrategy,
  waiverRequestStatus,
} from 'MainRoot/util/waiverUtils';
import { selectIsExpireWhenRemediationAvailableWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { extractViolationDetails } from 'MainRoot/util/violationDetailsUtil';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { selectFirewallComponentDetailsPageRouteParams } from 'MainRoot/firewall/firewallSelectors';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';
import { find, propEq } from 'ramda';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import classnames from 'classnames';
import ArtifactNameDisplay from 'MainRoot/react/ArtifactNameDisplay';

const RequestWaiverReviewPage = () => {
  const dispatch = useDispatch();

  const loading = useSelector(selectWaiverRequestDetailsLoading);
  const name = useSelector(selectPreviousRouteName);
  const prevParams = useSelector(selectRouterPrevParams);
  const loadError = useSelector(selectWaiverRequestDetailsError);
  const waiverRequestDetails = useSelector(selectWaiverRequestDetails);

  const { componentIdentifier, componentDisplayName } = useSelector(selectFirewallComponentDetailsPageRouteParams);
  const isExpireWhenRemediationAvailable = useSelector(selectIsExpireWhenRemediationAvailableWaiversEnabled);
  const violationDetails = useSelector(selectViolationDetails);
  const violationDetailsLoading = useSelector(selectLoadingViolation);
  const violationDetailsError = useSelector(selectViolationDetailsError);
  const { availableWaiverScopes } = useSelector(selectAddWaiverData);
  const addWaiverDataLoading = useSelector(selectAddWaiverDataLoading);
  const addWaiverDataError = useSelector(selectAddWaiverDataError);
  const selectedWaiverScopeLoading = useSelector(selectWaiverSelectedScopeLoading);
  const selectedWaiverScopeError = useSelector(selectWaiverSelectedScopeError);
  const waiverReasons = useSelector(selectWaiverReasons);
  const submitError = useSelector(selectSubmitError);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const selectedWaiverScope = useSelector(selectSelectedWaiverScope);
  const componentMatcherStrategy = useSelector(selectComponentMatcherStrategy);
  const expiryTime = useSelector(selectExpiryTime);
  const customExpiryTime = useSelector(selectCustomExpiryTime);
  const waiverReasonId = useSelector(selectWaiverReasonId);
  const waiverComments = useSelector(selectComments);
  const rejectionReason = useSelector(selectRejectionReason);

  const loadWaiverRequest = () => dispatch(actions.loadWaiverRequest());
  const loadViolation = (id) => dispatch(loadViolationAction(id));
  const loadAddWaiverData = (id) => dispatch(loadAddWaiverDataAction(id));
  const cancelAction = () => dispatch(returnToReviewWaiverRequestOriginPage());
  const onApproveAction = () =>
    dispatch(
      requestWaiverActions.reviewRequestWaiver({
        status: waiverRequestStatus.APPROVED,
        expiration,
        expireWhenRemediationAvailableSelected,
      })
    );
  const onRejectAction = () =>
    dispatch(requestWaiverActions.reviewRequestWaiver({ status: waiverRequestStatus.REJECTED }));
  const initializeForm = (details) => dispatch(requestWaiverActions.initializeStateFromDetails(details));
  const loadSelectedWaiverScope = (target) => dispatch(requestWaiverActions.loadSelectedWaiverScope(target));
  const setSelectedWaiverScope = (target) => dispatch(requestWaiverActions.setSelectedWaiverScope(target));
  const setComponentMatcherStrategy = (strategy) =>
    dispatch(requestWaiverActions.setComponentMatcherStrategy(strategy));
  const setExpiryTime = (time) => dispatch(requestWaiverActions.setExpiryTime(time));
  const setCustomExpiryTime = (time) => dispatch(requestWaiverActions.setCustomExpiryTime(time));
  const setWaiverReasonId = (reasonId) => dispatch(requestWaiverActions.setWaiverReasonId(reasonId));
  const setWaiverComments = (comment) => dispatch(requestWaiverActions.setRequestWaiverComments(comment));
  const setRejectionReason = (reason) => dispatch(requestWaiverActions.setRejectionReason(reason));

  // initialize read-only state
  const [isReadOnly, setIsReadOnly] = useState(false);

  const onApprove = () => {
    // Don't submit if the form is read-only
    if (!isReadOnly) {
      onApproveAction();
    }
  };

  const onReject = () => {
    if (!isReadOnly) {
      onRejectAction();
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
    violationId: waiverRequestDetails?.policyViolationId,
    prevStateName: name,
    prevParams,
    isWaiverRequestReview: true,
  };

  const load = () => {
    dispatch(waiverActions.loadCachedWaiverReasons());
    dispatch(requestWaiverActions.clearStateForReview());
    loadWaiverRequest();
  };

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

  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable, expiryTime);

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

  const [isRejectionModalOpen, setIsRejectionModalOpen] = useState(false);
  const openRejectionModal = () => setIsRejectionModalOpen(true);
  const closeRejectionModal = () => {
    setIsRejectionModalOpen(false);
    dispatch(requestWaiverActions.clearSubmitError());
  };

  const rejectButton = (
    <NxButton className="request-waiver-reject-btn" type="button" onClick={openRejectionModal} disabled={isReadOnly}>
      Reject Waiver Request
    </NxButton>
  );

  // Component hooks
  useEffect(() => {
    let isMounted = true;

    if (waiverRequestDetails) {
      const violationId = waiverRequestDetails.policyViolationId;
      loadViolation(violationId);
      loadAddWaiverData(violationId);
      initializeForm(waiverRequestDetails);

      // Check status and make read-only if it's approved
      if (waiverRequestDetails.status === waiverRequestStatus.APPROVED) {
        setIsReadOnly(true);
        return; // Exit early, no need to check permissions.
      }

      // Get owner details for the waiver request
      const ownerType = waiverRequestDetails.scopeOwnerType || 'global';
      const ownerId = waiverRequestDetails.scopeOwnerId || 'global';

      const permissionCheck =
        ownerType === 'root_organization'
          ? checkPermissions(['WAIVE_POLICY_VIOLATIONS'])
          : checkPermissions(['WAIVE_POLICY_VIOLATIONS'], ownerType, ownerId);

      permissionCheck
        .then(() => {
          // Check that component is still mounted
          if (isMounted) {
            // If the user has permission, set the form to editable
            setIsReadOnly(false);
          }
        })
        .catch(() => {
          // Check that component is still mounted
          if (isMounted) {
            // If the user does not have permission, set the form to read-only
            setIsReadOnly(true);
          }
        });
    }

    return () => {
      isMounted = false; // Cleanup function to set isMounted to false
    };
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
    }
  }, [waiverRequestDetails]);

  useEffect(load, []);

  return (
    <NxPageMain id="request-waiver-review-page">
      <AddAndRequestWaiversBackButton {...backButtonProps} />
      <NxPageTitle>
        <NxH1>Review Requested Waiver</NxH1>
      </NxPageTitle>

      {/* Display error alert when waiver request is rejected */}
      {waiverRequestDetails && waiverRequestDetails.status === waiverRequestStatus.REJECTED && (
        <NxErrorAlert>
          This Waiver Request was rejected by {waiverRequestDetails.reviewerName} for the following reason:
          <br />
          {waiverRequestDetails.rejectionReason || 'No reason provided.'}
        </NxErrorAlert>
      )}

      <NxTile>
        <NxTile.Content>
          <LoadWrapper
            loading={loading || violationDetailsLoading || addWaiverDataLoading || selectedWaiverScopeLoading}
            error={loadError || violationDetailsError || addWaiverDataError || selectedWaiverScopeError}
            retryHandler={load}
          >
            {() => (
              <>
                <NxTile.Header className="nx-tile-header">
                  <NxTile.HeaderTitle className="nx-tile-header__title">
                    <NxH2 className="nx-h2">Requested Waiver Information</NxH2>
                  </NxTile.HeaderTitle>
                </NxTile.Header>

                <div className="nx-tile-content iq-request-waiver-info">
                  {/* Requested By */}
                  <div className="nx-read-only iq-request-waiver-info__requested-by">
                    <header className="nx-read-only__label">Requested By</header>
                    <div className="nx-read-only__data">{waiverRequestDetails.requesterName}</div>
                  </div>

                  {/* Date Requested */}
                  <div className="nx-read-only iq-request-waiver-info__date-requested">
                    <header className="nx-read-only__label">Date Requested</header>
                    <div className="nx-read-only__data">{formatCustomDate(waiverRequestDetails.requestTime)}</div>
                  </div>

                  {/* Note to Reviewer */}
                  <div className="nx-read-only iq-request-waiver-info__note-to-reviewer">
                    <header className="nx-read-only__label">Note to Reviewer</header>
                    <div className="nx-sub-label">
                      This note will only be visible on the waiver request. It will not be visible on the waiver if it
                      is approved.
                    </div>
                    <NxBlockquote>{waiverRequestDetails.noteToReviewer}</NxBlockquote>
                  </div>
                </div>

                <NxStatefulForm
                  className="iq-request-waiver-form"
                  onCancel={cancelAction}
                  submitError={!isRejectionModalOpen ? submitError : null}
                  showValidationErrors={!!submitError}
                  submitBtnClasses={classnames('request-waiver-approve-btn', { disabled: isReadOnly })}
                  submitBtnText={'Approve'}
                  onSubmit={onApprove}
                  submitMaskState={submitMaskState}
                  additionalFooterBtns={rejectButton}
                >
                  <NxTile.Header className="nx-tile-header">
                    <NxTile.HeaderTitle className="nx-tile-header__title">
                      <NxH2 className="nx-h2">Waiver Configuration</NxH2>
                    </NxTile.HeaderTitle>
                  </NxTile.Header>

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
                        currentValue={selectedWaiverScope?.id || ''}
                        isDisabled={isReadOnly}
                      />
                    </NxFieldset>

                    {/* Components */}
                    <NxFieldset
                      className="iq-request-waiver-form__components"
                      label="Components"
                      disabled={isReadOnly}
                      isRequired
                    >
                      <NxRadio
                        id="current-component"
                        name="request-waiver-components"
                        value={waiverMatcherStrategy.EXACT_COMPONENT}
                        isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                        onChange={handleComponentsChange}
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
                              aria-label="set custom expiration date"
                            />
                          )}
                        </div>
                      </div>
                    </NxFieldset>

                    {/* Reason */}
                    <NxFieldset id="waiver-request-reason" className="iq-request-waiver-form__reason" label="Reason">
                      <NxFormSelect
                        id="waiver-reason-select"
                        onChange={onReasonChange}
                        defaultValue={waiverReasonId}
                        disabled={isReadOnly}
                        aria-labelledby="waiver-request-reason"
                      >
                        {waiverReasonsToRender.map(({ id, reasonText }) => (
                          <option key={id} value={id}>
                            {reasonText}
                          </option>
                        ))}
                      </NxFormSelect>
                    </NxFieldset>

                    {/* Comments */}
                    <NxFieldset
                      id="waiver-request-comments"
                      className="iq-request-waiver-form__comments"
                      label="Comments"
                    >
                      <NxTextInput
                        type="textarea"
                        inputAttributes={{ maxLength: 1000 }}
                        {...waiverComments}
                        onChange={setWaiverComments}
                        disabled={isReadOnly}
                        aria-labelledby="waiver-request-comments"
                      />
                    </NxFieldset>
                  </div>
                </NxStatefulForm>

                {/* Rejection Modal */}
                {isRejectionModalOpen && (
                  <NxModal className="iq-request-waiver-modal" onClose={closeRejectionModal}>
                    <NxModal.Header>
                      <NxH2>Reject Waiver Request</NxH2>
                    </NxModal.Header>
                    <NxStatefulForm
                      className="iq-request-waiver-form"
                      submitError={submitError}
                      showValidationErrors={!!submitError}
                      submitBtnClasses="request-waiver-send-rejection"
                      submitBtnText={'Send'}
                      onSubmit={onReject}
                      onCancel={closeRejectionModal}
                      submitMaskState={submitMaskState}
                    >
                      <div className="nx-tile-content">
                        <NxFieldset className="iq-request-waiver-form__rejection-reason" label="Rejection Reason">
                          <NxTextInput
                            type="textarea"
                            inputAttributes={{ maxLength: 1000 }}
                            {...rejectionReason}
                            onChange={setRejectionReason}
                            placeholder="Enter the reason the waiver request was rejected here"
                          />
                        </NxFieldset>
                      </div>
                    </NxStatefulForm>
                  </NxModal>
                )}
              </>
            )}
          </LoadWrapper>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
};

export default RequestWaiverReviewPage;
