/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxPageMain,
  NxPageTitle,
  NxH1,
  NxH2,
  NxTile,
  NxReadOnly,
  NxButton,
  NxButtonBar,
  NxTextInput,
  NxFormGroup,
  NxFormSelect,
  NxFieldset,
  NxRadio,
  NxDateInput,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxBlockquote,
  NxErrorAlert,
  NxModal,
  nxDateInputStateHelpers,
} from '@sonatype/react-shared-components';
import { faArrowLeft } from '@fortawesome/pro-regular-svg-icons';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import { actions } from './firewallWaiverRequestsSlice';
import { actions as waiverSliceActions } from 'MainRoot/waivers/waiverSlice';
import {
  selectReviewPageLoading,
  selectReviewPageError,
  selectReviewPageWaiverRequest,
  selectReviewPageIsSubmitting,
  selectReviewPageSubmitError,
  selectRejectionReason,
  selectReviewPageHasWaivePermission,
} from './firewallWaiverRequestsSelectors';
import { selectWaiverReasons } from 'MainRoot/waivers/requestWaiverSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  useWaiverExpirations,
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  getExpirationDaysMessage,
  waiverMatcherStrategy,
} from 'MainRoot/util/waiverUtils';
import { getExpiryTime, isCustomExpiryTimeValid } from 'MainRoot/util/waiverUtils';
import { getISODateFromDateInput } from 'MainRoot/util/jsUtil';
import { selectIsExpireWhenRemediationAvailableWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatScopeOwnerType(type) {
  if (!type) return '';
  return type.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}

function getComponentDisplayName(waiverRequest) {
  if (waiverRequest.displayName?.parts?.length) {
    return waiverRequest.displayName.parts.map((part) => part.value).join('');
  }
  if (waiverRequest.componentIdentifier) {
    const { coordinates } = waiverRequest.componentIdentifier;
    if (coordinates) {
      const parts = [coordinates.groupId, coordinates.artifactId || coordinates.packageId, coordinates.version].filter(
        Boolean
      );
      if (parts.length > 0) return parts.join(':');
    }
  }
  return 'Unknown';
}

const customDateValidator = (value) => (isCustomExpiryTimeValid(value) ? null : 'Date must be in the future');

export default function FirewallReviewWaiverRequestPage() {
  const dispatch = useDispatch();

  const { waiverRequestId, ownerType, ownerId, origin } = useSelector(selectRouterCurrentParams);
  const backState = origin || 'firewall.waivers.components.requested';
  const loading = useSelector(selectReviewPageLoading);
  const error = useSelector(selectReviewPageError);
  const waiverRequest = useSelector(selectReviewPageWaiverRequest);
  const isSubmitting = useSelector(selectReviewPageIsSubmitting);
  const submitError = useSelector(selectReviewPageSubmitError);
  const rejectionReason = useSelector(selectRejectionReason);
  const hasWaivePermission = useSelector(selectReviewPageHasWaivePermission);
  const waiverReasons = useSelector(selectWaiverReasons) || [];
  const isExpireWhenRemediationAvailable = useSelector(selectIsExpireWhenRemediationAvailableWaiversEnabled);

  // Container image waiver requests are scoped to REPOSITORY_CONTAINER_ID and use ALL_COMPONENTS.
  // They don't need the Scope or Constraint fields shown on the review page.
  const isContainerImageWaiverRequest = waiverRequest?.scopeOwnerType === 'all_repositories';

  // Editable form state
  const [matcherStrategy, setMatcherStrategy] = useState(waiverMatcherStrategy.EXACT_COMPONENT);
  const [expiryTime, setExpiryTime] = useState('30');
  const [customExpiryTime, setCustomExpiryTime] = useState(nxDateInputStateHelpers.initialState(''));
  const [waiverReasonId, setWaiverReasonId] = useState('');
  const [comment, setComment] = useState('');
  const [scopeOwnerId, setScopeOwnerId] = useState('');
  const [availableScopes, setAvailableScopes] = useState([]);
  const [isRejectionModalOpen, setIsRejectionModalOpen] = useState(false);

  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);

  // Initialize form state from loaded waiver request
  useEffect(() => {
    if (waiverRequest) {
      setMatcherStrategy(waiverRequest.matcherStrategy || waiverMatcherStrategy.EXACT_COMPONENT);
      setComment(waiverRequest.comment || '');
      setWaiverReasonId(waiverRequest.policyWaiverReasonId || '');
      setScopeOwnerId(waiverRequest.scopeOwnerId || '');

      // Build available scopes from the waiver request's current scope
      const currentScope = {
        id: waiverRequest.scopeOwnerId,
        label: `${formatScopeOwnerType(waiverRequest.scopeOwnerType)} - ${
          waiverRequest.scopeOwnerName || waiverRequest.scopeOwnerId
        }`,
      };
      setAvailableScopes([currentScope]);

      // Set expiry time based on existing value
      if (waiverRequest.expiryTime) {
        setExpiryTime('custom');
        const d = new Date(waiverRequest.expiryTime);
        const dateStr = d.toISOString().split('T')[0];
        setCustomExpiryTime(nxDateInputStateHelpers.userInput(customDateValidator, dateStr));
      }
    }
  }, [waiverRequest]);

  useEffect(() => {
    if (waiverRequestId && ownerType && ownerId) {
      dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId: waiverRequestId }));
    }
    dispatch(waiverSliceActions.loadCachedWaiverReasons());
  }, [waiverRequestId, ownerType, ownerId]);

  const handleBack = () => {
    dispatch(stateGo(backState));
  };

  const handleApprove = () => {
    let computedExpiryTime = null;
    if (isCustomExpiryTimeSelected(expiryTime) && customExpiryTime.value) {
      computedExpiryTime = getISODateFromDateInput(customExpiryTime.value);
    } else if (
      !isNeverExpiryTimeSelected(expiryTime) &&
      !isExpireWhenRemediationAvailableSelected(expiryTime) &&
      expiryTime
    ) {
      computedExpiryTime = getExpiryTime(parseInt(expiryTime, 10));
    }

    dispatch(
      actions.approveWaiverRequest({
        ownerType,
        ownerId,
        policyWaiverRequestId: waiverRequestId,
        matcherStrategy,
        expiryTime: computedExpiryTime,
        waiverReasonId: waiverReasonId || null,
        comment: comment || null,
        expireWhenRemediationAvailable: isExpireWhenRemediationAvailableSelected(expiryTime),
      })
    ).then((result) => {
      if (!result.error) {
        dispatch(stateGo(backState));
      }
    });
  };

  const handleReject = () => {
    dispatch(
      actions.rejectWaiverRequest({ ownerType, ownerId, policyWaiverRequestId: waiverRequestId, rejectionReason })
    ).then((result) => {
      if (!result.error) {
        setIsRejectionModalOpen(false);
        dispatch(stateGo(backState));
      }
    });
  };

  const retryHandler = () =>
    dispatch(actions.loadWaiverRequestForReview({ ownerType, ownerId, policyWaiverRequestId: waiverRequestId }));

  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);
  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  const isRequested = waiverRequest?.status === 'REQUESTED';
  const isRejected = waiverRequest?.status === 'REJECTED';
  const isEditable = isRequested || isRejected;
  const threatLevelCategory = categoryByPolicyThreatLevel[waiverRequest?.threatLevel];
  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason' }, ...waiverReasons];

  return (
    <NxPageMain id="fw-review-waiver-request-page">
      <a
        role="link"
        href="#"
        className="iq-fw-review-waiver-page__back-link"
        onClick={(e) => {
          e.preventDefault();
          handleBack();
        }}
      >
        <NxFontAwesomeIcon icon={faArrowLeft} />
        {' Back to Requested Waivers'}
      </a>

      <NxPageTitle>
        <NxH1>Review Requested Waiver</NxH1>
      </NxPageTitle>

      {/* Rejection banner */}
      {waiverRequest?.status === 'REJECTED' && (
        <NxErrorAlert>
          This Waiver Request was rejected by {waiverRequest.reviewerName} for the following reason:
          <br />
          {waiverRequest.rejectionReason || 'No reason provided.'}
        </NxErrorAlert>
      )}

      <NxTile>
        <NxTile.Content>
          <NxLoadWrapper loading={loading} error={error} retryHandler={retryHandler}>
            {waiverRequest && (
              <>
                {/* ── Requested Waiver Information ─────────────────── */}
                <NxTile.Header>
                  <NxTile.HeaderTitle>
                    <NxH2>Requested Waiver Information</NxH2>
                  </NxTile.HeaderTitle>
                </NxTile.Header>

                <div className="nx-tile-content iq-request-waiver-info">
                  <NxReadOnly>
                    <NxReadOnly.Label>Requested By</NxReadOnly.Label>
                    <NxReadOnly.Data>{waiverRequest.requesterName}</NxReadOnly.Data>

                    <NxReadOnly.Label>Date Requested</NxReadOnly.Label>
                    <NxReadOnly.Data>{formatDate(waiverRequest.requestTime)}</NxReadOnly.Data>

                    <NxReadOnly.Label>Note to Reviewer</NxReadOnly.Label>
                    <NxReadOnly.Data className="nx-sub-label">
                      This note will only be visible on the waiver request. It will not be visible on the waiver if it
                      is approved.
                    </NxReadOnly.Data>
                    <NxReadOnly.Data>
                      <NxBlockquote>{waiverRequest.noteToReviewer || '—'}</NxBlockquote>
                    </NxReadOnly.Data>
                  </NxReadOnly>
                </div>

                {/* ── Waiver Configuration ──────────────────────────── */}
                <NxTile.Header>
                  <NxTile.HeaderTitle>
                    <NxH2>Waiver Configuration</NxH2>
                  </NxTile.HeaderTitle>
                </NxTile.Header>

                <div className="nx-tile-content">
                  {/* Read-only fields */}
                  <NxReadOnly>
                    {!isContainerImageWaiverRequest && (
                      <>
                        <NxReadOnly.Label>Component</NxReadOnly.Label>
                        <NxReadOnly.Data>{getComponentDisplayName(waiverRequest)}</NxReadOnly.Data>
                      </>
                    )}
                    <NxReadOnly.Label>Policy</NxReadOnly.Label>
                    <NxReadOnly.Data>
                      <span className="iq-fw-review-waiver-page__policy">
                        <ViolationExclamation threatLevelCategory={threatLevelCategory} />
                        <span>{waiverRequest.policyName}</span>
                      </span>
                    </NxReadOnly.Data>

                    {!isContainerImageWaiverRequest && (
                      <>
                        <NxReadOnly.Label>Constraint</NxReadOnly.Label>
                        <NxReadOnly.Data>
                          {waiverRequest.constraintFacts?.map((cf) => cf.constraintName).join(', ') || '—'}
                        </NxReadOnly.Data>
                      </>
                    )}
                  </NxReadOnly>

                  {/* Scope — editable dropdown (not applicable for container image waiver requests) */}
                  {!isContainerImageWaiverRequest && (
                    <NxFieldset label="Scope" isRequired>
                      <NxFormSelect
                        id="fw-review-scope-select"
                        value={scopeOwnerId}
                        onChange={(val) => setScopeOwnerId(val)}
                        disabled={!isEditable}
                        aria-label="select waiver scope"
                      >
                        {availableScopes.map((scope) => (
                          <option key={scope.id} value={scope.id}>
                            {scope.label}
                          </option>
                        ))}
                      </NxFormSelect>
                    </NxFieldset>
                  )}

                  {/* Components — editable radio buttons (not applicable for container image waiver requests) */}
                  {!isContainerImageWaiverRequest && (
                    <NxFieldset className="iq-request-waiver-form__components" label="Components" isRequired>
                      <NxRadio
                        name="fw-review-components"
                        value={waiverMatcherStrategy.EXACT_COMPONENT}
                        isChecked={matcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
                        onChange={setMatcherStrategy}
                        disabled={!isEditable}
                      >
                        {getComponentDisplayName(waiverRequest)}
                      </NxRadio>
                      <NxRadio
                        name="fw-review-components"
                        value={waiverMatcherStrategy.ALL_VERSIONS}
                        isChecked={matcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
                        onChange={setMatcherStrategy}
                        disabled={!isEditable}
                      >
                        All Versions
                      </NxRadio>
                      <NxRadio
                        name="fw-review-components"
                        value={waiverMatcherStrategy.ALL_COMPONENTS}
                        isChecked={matcherStrategy === waiverMatcherStrategy.ALL_COMPONENTS}
                        onChange={setMatcherStrategy}
                        disabled={!isEditable}
                      >
                        All Components
                      </NxRadio>
                    </NxFieldset>
                  )}
                  {/* Waiver Expiration */}
                  <NxFieldset className="iq-request-waiver-form__expiryTime" label="Waiver Expiration" isRequired>
                    <div className="iq-fw-review-waiver-page__expiry-block">
                      <NxFormSelect
                        id="fw-review-expiration-select"
                        onChange={(e) => setExpiryTime(e)}
                        value={expiryTime}
                        disabled={!isEditable}
                        aria-label="select waiver expiration"
                      >
                        {waiverExpirations.map(({ name, value }, index) => (
                          <option key={index} value={value}>
                            {name}
                          </option>
                        ))}
                      </NxFormSelect>
                      {customExpiryTimeSelected && (
                        <NxDateInput
                          className="iq-request-waiver-form__date-input"
                          {...customExpiryTime}
                          onChange={(val) =>
                            setCustomExpiryTime(nxDateInputStateHelpers.userInput(customDateValidator, val))
                          }
                          validatable={true}
                          disabled={!isEditable}
                          aria-label="set custom expiration date"
                        />
                      )}
                      {daysDiffMessage && (
                        <div className="iq-fw-review-waiver-page__expiry-days-message">{daysDiffMessage}</div>
                      )}
                    </div>
                  </NxFieldset>

                  {/* Reason */}
                  <NxFieldset label="Reason">
                    <NxFormSelect
                      id="fw-review-reason-select"
                      onChange={(e) => setWaiverReasonId(e)}
                      value={waiverReasonId}
                      disabled={!isEditable}
                      aria-label="select waiver reason"
                    >
                      {waiverReasonsToRender.map(({ id, reasonText }) => (
                        <option key={id} value={id}>
                          {reasonText}
                        </option>
                      ))}
                    </NxFormSelect>
                  </NxFieldset>

                  {/* Comments */}
                  <NxFormGroup label="Comments">
                    <NxTextInput
                      type="textarea"
                      id="fw-review-comments"
                      aria-label="Comments"
                      value={comment}
                      onChange={setComment}
                      disabled={!isEditable}
                    />
                  </NxFormGroup>
                </div>

                {/* ── Actions ──────────────────────────────────────── */}
                <>
                  {submitError && <NxErrorAlert>{submitError}</NxErrorAlert>}
                  <NxButtonBar>
                    {hasWaivePermission && (
                      <NxButton
                        type="button"
                        id="fw-review-reject-btn"
                        onClick={() => setIsRejectionModalOpen(true)}
                        disabled={isSubmitting || !isEditable}
                      >
                        Reject Waiver Request
                      </NxButton>
                    )}
                    <NxButton type="button" variant="tertiary" onClick={handleBack} disabled={isSubmitting}>
                      Cancel
                    </NxButton>
                    {hasWaivePermission && (
                      <NxButton
                        type="button"
                        variant="primary"
                        id="fw-review-approve-btn"
                        onClick={handleApprove}
                        disabled={isSubmitting || !isEditable}
                      >
                        {isSubmitting ? 'Approving…' : 'Approve'}
                      </NxButton>
                    )}
                  </NxButtonBar>
                </>

                {/* ── Rejection Modal ──────────────────────────────── */}
                {isRejectionModalOpen && (
                  <NxModal onClose={() => setIsRejectionModalOpen(false)}>
                    <NxModal.Header>
                      <NxH2>Reject Waiver Request</NxH2>
                    </NxModal.Header>
                    <NxModal.Content>
                      <NxFormGroup label="Rejection Reason">
                        <NxTextInput
                          type="textarea"
                          id="fw-review-rejection-reason"
                          aria-label="Rejection Reason"
                          value={rejectionReason}
                          onChange={(value) => dispatch(actions.setRejectionReason(value))}
                          placeholder="Enter the reason the waiver request was rejected here"
                          className="iq-fw-review-waiver-page__rejection-reason"
                        />
                      </NxFormGroup>
                      {submitError && <NxErrorAlert>{submitError}</NxErrorAlert>}
                    </NxModal.Content>
                    <footer className="nx-footer">
                      <div className="nx-btn-bar">
                        <NxButton type="button" variant="tertiary" onClick={() => setIsRejectionModalOpen(false)}>
                          Cancel
                        </NxButton>
                        <NxButton
                          type="button"
                          variant="primary"
                          id="fw-review-send-rejection-btn"
                          onClick={handleReject}
                          disabled={isSubmitting}
                        >
                          {isSubmitting ? 'Sending…' : 'Send'}
                        </NxButton>
                      </div>
                    </footer>
                  </NxModal>
                )}
              </>
            )}
          </NxLoadWrapper>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
