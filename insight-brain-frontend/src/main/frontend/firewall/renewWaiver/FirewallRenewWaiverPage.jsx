/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxLoadWrapper,
  NxSubmitMask,
  NxButton,
  NxButtonBar,
  NxTextInput,
  NxFormSelect,
  NxDateInput,
  NxReadOnly,
  NxFieldset,
  NxFontAwesomeIcon,
  NxBackButton,
} from '@sonatype/react-shared-components';
import { faSitemap, faTerminal } from '@fortawesome/pro-solid-svg-icons';
import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import {
  selectRenewWaiverLoading,
  selectRenewWaiverError,
  selectRenewWaiverWaiver,
  selectRenewWaiverNewExpiryTime,
  selectRenewWaiverCustomExpiryTime,
  selectRenewWaiverComment,
  selectRenewWaiverReasonId,
  selectRenewWaiverSubmitMaskState,
  selectRenewWaiverSubmitError,
  selectRenewWaiverReasons,
  selectRenewWaiverReturnStateName,
  selectRenewWaiverReturnParams,
} from './renewWaiverSelectors';
import { actions } from './renewWaiverSlice';
import { selectRouterCurrentParams, selectRouterPrevState, selectRouterPrevParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { formatWaiverDetails, isWaiverAllVersionsOrExact, isWaiverExpired } from 'MainRoot/util/waiverUtils';
import {
  useFirewallWaiverExpirations,
  isCustomExpiryTimeSelected,
  isCustomExpiryTimeValid,
} from 'MainRoot/firewall/bulkWaive/firewallWaiverUtils';
import moment from 'moment';

export default function FirewallRenewWaiverPage() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const routerParams = useSelector(selectRouterCurrentParams);

  const waiverExpirations = useFirewallWaiverExpirations(false);

  const loading = useSelector(selectRenewWaiverLoading);
  const loadError = useSelector(selectRenewWaiverError);
  const waiver = useSelector(selectRenewWaiverWaiver);
  const newExpiryTime = useSelector(selectRenewWaiverNewExpiryTime);
  const customExpiryTime = useSelector(selectRenewWaiverCustomExpiryTime);
  const comment = useSelector(selectRenewWaiverComment);
  const reasonId = useSelector(selectRenewWaiverReasonId);
  const waiverReasons = useSelector(selectRenewWaiverReasons);
  const submitMaskState = useSelector(selectRenewWaiverSubmitMaskState);
  const submitError = useSelector(selectRenewWaiverSubmitError);
  const submitReturnStateName = useSelector(selectRenewWaiverReturnStateName);
  const submitReturnParams = useSelector(selectRenewWaiverReturnParams);

  const prevState = useSelector(selectRouterPrevState);
  const prevParams = useSelector(selectRouterPrevParams);

  const { ownerType, ownerId, waiverId, type, sidebarReference, sidebarId, page } = routerParams;

  const returnStateName = prevState?.name || 'firewall.waiver.details';
  const returnParams = returnStateName === 'firewall.waiver.details'
    ? { ownerType, ownerId, waiverId, type, sidebarReference, sidebarId, page }
    : prevParams;

  const backHref = uiRouterState.href(returnStateName, returnParams);

  useEffect(() => {
    dispatch(actions.loadWaiverForRenewal());
    dispatch(actions.loadWaiverReasons());
    return () => dispatch(actions.resetRenewWaiverState());
  }, [dispatch, waiverId]);

  useEffect(() => {
    if (submitMaskState === true) {
      dispatch(actions.resetRenewWaiverState());
      dispatch(stateGo(submitReturnStateName || 'firewall.waiver.details', submitReturnParams));
      setTimeout(() => {
        dispatch(toastActions.addToast({ type: 'success', message: 'Waiver renewed successfully.' }));
      }, 500);
    }
  }, [submitMaskState, dispatch, submitReturnStateName, submitReturnParams]);

  const handleExpiryChange = useCallback(
    (value) => {
      dispatch(actions.setNewExpiryTime(value === 'never' ? null : value));
    },
    [dispatch]
  );

  const handleCustomExpiryChange = useCallback(
    (value) => {
      dispatch(actions.setCustomExpiryTime(value));
      dispatch(actions.setNewExpiryTime('custom'));
    },
    [dispatch]
  );

  const handleCommentChange = useCallback(
    (value) => {
      dispatch(actions.setComment(value));
    },
    [dispatch]
  );

  const handleReasonChange = useCallback(
    (value) => {
      dispatch(actions.setReasonId(value || null));
    },
    [dispatch]
  );

  const handleSubmit = useCallback(() => {
    dispatch(actions.submitRenewal(waiverId));
  }, [dispatch, waiverId]);

  const handleCancel = useCallback(() => {
    dispatch(stateGo(returnStateName, returnParams));
  }, [dispatch, returnStateName, returnParams]);


  const getDetails = () => dispatch(actions.loadWaiverForRenewal());

  const { expiration, policyName, component, creatorName } = formatWaiverDetails(waiver);

  const isApplication = waiver?.scopeOwnerType === 'application';
  const scopeIcon = isApplication ? (
    <NxFontAwesomeIcon icon={faTerminal} />
  ) : (
    <NxFontAwesomeIcon icon={faSitemap} />
  );

  const customExpiryTimeSelected = isCustomExpiryTimeSelected(newExpiryTime);
  const isNeverSelected = !newExpiryTime || newExpiryTime === 'never';
  const waiverAlreadyNeverExpires = !waiver?.expiryTime;
  const isNoOpRenewal = isNeverSelected && waiverAlreadyNeverExpires;
  const isExpired = isWaiverExpired(waiver?.expiryTime);

  const isSubmitDisabled = isNoOpRenewal ||
    (customExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime?.value));

  const getRenewalMessage = () => {
    if (!newExpiryTime || newExpiryTime === 'never') {
      return null;
    }
    if (customExpiryTimeSelected) {
      if (!isCustomExpiryTimeValid(customExpiryTime?.value)) {
        return null;
      }
      const customDate = moment(customExpiryTime.value, 'YYYY-MM-DD');
      if (isExpired) {
        const diff = Math.ceil(customDate.diff(moment(), 'days', true));
        return diff > 0 ? { text: `Renews from today for ${diff} days → ${customDate.format('YYYY-MM-DD')}` } : null;
      }
      if (!waiver?.expiryTime) {
        return { text: `New expiry → ${customDate.format('YYYY-MM-DD')}` };
      }
      const currentExpiry = moment(waiver.expiryTime);
      const diff = Math.ceil(customDate.diff(currentExpiry, 'days', true));
      if (diff > 0) {
        return { text: `Extends current expiry by ${diff} days → ${customDate.format('YYYY-MM-DD')}` };
      }
      return { text: `New expiry → ${customDate.format('YYYY-MM-DD')}` };
    }
    if (isExpired) {
      const newExpiryDate = moment().add(parseInt(newExpiryTime, 10), 'days').format('YYYY-MM-DD');
      return { text: `Renews from today for ${newExpiryTime} days → ${newExpiryDate}` };
    }
    if (!waiver?.expiryTime) {
      const newExpiryDate = moment().add(parseInt(newExpiryTime, 10), 'days').format('YYYY-MM-DD');
      return { text: `New expiry in ${newExpiryTime} days → ${newExpiryDate}` };
    }
    const newExpiryDate = moment(waiver.expiryTime).add(parseInt(newExpiryTime, 10), 'days').format('YYYY-MM-DD');
    return { text: `Extends current expiry by ${newExpiryTime} days → ${newExpiryDate}` };
  };

  const renewalMessage = getRenewalMessage();

  return (
    <main id="iq-renew-waiver-page" className="nx-page-main iq-renew-waiver-page" data-testid="renew-waiver-page">
      {submitMaskState !== null && (
        <NxSubmitMask success={submitMaskState} message="Renewing waiver…" successMessage="Success!" />
      )}

      <NxBackButton href={backHref} targetPageTitle="Waiver Details" />

      <div className="nx-page-title">
        <h1 className="nx-h1">Renew Waiver</h1>
      </div>

      <section className="nx-tile">
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={getDetails}>
          <div className="iq-renew-waiver-content">
            <NxReadOnly className="iq-renew-waiver__policy">
              <NxReadOnly.Label>Policy</NxReadOnly.Label>
              <NxReadOnly.Data>{policyName}</NxReadOnly.Data>
            </NxReadOnly>

            <NxReadOnly className="iq-renew-waiver__scope">
              <NxReadOnly.Label>Scope</NxReadOnly.Label>
              <NxReadOnly.Data>
                {scopeIcon}
                {waiver?.scopeOwnerName}
              </NxReadOnly.Data>
            </NxReadOnly>

            {component && isWaiverAllVersionsOrExact(component) && (
              <NxReadOnly className="iq-renew-waiver__component">
                <NxReadOnly.Label>Component</NxReadOnly.Label>
                <NxReadOnly.Data>
                  <ComponentDisplay component={component} truncate={true} matcherStrategy={component.matcherStrategy} />
                </NxReadOnly.Data>
              </NxReadOnly>
            )}

            <NxReadOnly className="iq-renew-waiver__current-expiry">
              <NxReadOnly.Label>Current Expiry</NxReadOnly.Label>
              <NxReadOnly.Data>
                {expiration}
                {isExpired && (
                  <span className="iq-renew-waiver__expired-tag">EXPIRED</span>
                )}
              </NxReadOnly.Data>
            </NxReadOnly>

            <NxFieldset className="iq-renew-waiver__new-expiry" label="New Expiry Date" isRequired>
              <div className="iq-renew-waiver__expiry-block">
                <div className="iq-renew-waiver__expiry-controls">
                  <NxFormSelect
                    id="renew-waiver-expiration-select"
                    value={newExpiryTime ?? 'never'}
                    onChange={handleExpiryChange}
                  >
                    {waiverExpirations.map(({ name, value }, index) => (
                      <option
                        key={index}
                        value={value}
                      >
                        {name}
                      </option>
                    ))}
                  </NxFormSelect>
                  {customExpiryTimeSelected && (
                    <NxDateInput
                      className="iq-renew-waiver__date-input"
                      {...customExpiryTime}
                      onChange={handleCustomExpiryChange}
                      validatable={true}
                      validationErrors={
                        !customExpiryTime.isPristine && !isCustomExpiryTimeValid(customExpiryTime?.value)
                          ? 'Date must be in the future'
                          : null
                      }
                    />
                  )}
                </div>
                <div className="iq-renew-waiver__expiry-messages">
                  {isNoOpRenewal && (
                    <div className="iq-renew-waiver__no-op-notice">
                      Waiver already has no expiry date. Select a new expiry to renew.
                    </div>
                  )}
                  {!isNoOpRenewal && !customExpiryTimeSelected && !isNeverSelected && (
                    <div className="iq-renew-waiver__expiry-base-hint">
                      {isExpired ? 'Days added from today (waiver is expired)' : waiverAlreadyNeverExpires ? 'Days added from today' : 'Days added from current expiry date'}
                    </div>
                  )}
                  {!isNoOpRenewal && renewalMessage && (
                    <div className="iq-renew-waiver__expiration-days-diff visual-testing-ignore iq-renew-waiver__expiration-days-diff--highlight">
                      {renewalMessage.text}
                    </div>
                  )}
                </div>
              </div>
            </NxFieldset>

            <NxFieldset className="iq-renew-waiver__reason" label="Reason">
              <NxFormSelect id="renew-waiver-reason-select" value={reasonId || ''} onChange={handleReasonChange}>
                <option value="" disabled>
                  Select a reason
                </option>
                {waiverReasons.map(({ id, reasonText }) => (
                  <option key={id} value={id}>
                    {reasonText}
                  </option>
                ))}
              </NxFormSelect>
            </NxFieldset>

            <NxFieldset className="iq-renew-waiver__comment" label="Comment">
              <NxTextInput
                type="textarea"
                className="iq-renew-waiver__comment-input"
                inputAttributes={{ maxLength: 1000 }}
                {...comment}
                onChange={handleCommentChange}
              />
            </NxFieldset>

            <div className="nx-read-only iq-renew-waiver__updated-by">
              <header className="nx-read-only__label">Updated By</header>
              <div className="nx-read-only__data">{creatorName}</div>
            </div>

            {submitError && (
              <div className="iq-renew-waiver__submit-error">{submitError}</div>
            )}
          </div>

          <footer className="iq-renew-waiver__footer">
            <hr className="iq-renew-waiver__divider" />
            <NxButtonBar>
              <NxButton variant="tertiary" onClick={handleCancel}>
                Cancel
              </NxButton>
              <NxButton variant="primary" onClick={handleSubmit} disabled={isSubmitDisabled}>
                Renew
              </NxButton>
            </NxButtonBar>
          </footer>
        </NxLoadWrapper>
      </section>
    </main>
  );
}
