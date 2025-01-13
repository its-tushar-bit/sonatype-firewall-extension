/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import {
  NxCodeSnippet,
  NxTextLink,
  NxH1,
  NxPageTitle,
  NxTile,
  NxStatefulForm,
  NxPageMain,
  NxFieldset,
  NxTextInput,
  NxWarningAlert,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import LoadWrapper from '../react/LoadWrapper';
import AddAndRequestWaiversBackButton from './AddAndRequestWaiversBackButton';

import { getRequestWaiverUrl, getPolicyViolationUiLink, getAddWaiverUiLink } from '../util/CLMLocation';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import { useDispatch, useSelector } from 'react-redux';
import classNames from 'classnames';

import {
  selectComments,
  selectWaiverReasonId,
  selectLoadingViolation,
  selectSubmitError,
  selectSubmitMaskState,
  selectViolationDetails,
  selectViolationDetailsError,
  selectWaiverRequestWebhookState,
  selectWaiverReasons,
} from './requestWaiverSelectors';
import {
  selectPreviousRouteName,
  selectRouterPrevParams,
  selectViolationId,
  selectIsStandaloneDeveloper,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { loadViolation as loadViolationAction } from 'MainRoot/violation/violationActions';
import { actions } from './requestWaiverSlice';
import { returnToAddWaiverOriginPage } from './waiverActions';
import { actions as waiverActions } from './waiverSlice';
import { selectViolationSlice } from '../violation/violationSelectors';

const WaiverRequestWebhookAlert = () => {
  const { loading, error, waiverRequestWebhookAvailable } = useSelector(selectWaiverRequestWebhookState);

  return (
    <LoadWrapper loading={loading} error={error}>
      {!waiverRequestWebhookAvailable && (
        <NxWarningAlert id="iq-waiver-request-webhook-warning">
          Webhook event for Automatic Waiver Request is not configured. Contact your admin or request the waiver
          manually.
        </NxWarningAlert>
      )}
    </LoadWrapper>
  );
};

const RequestWaiversPage = () => {
  const dispatch = useDispatch();

  const loading = useSelector(selectLoadingViolation);
  const violationDetailsError = useSelector(selectViolationDetailsError);
  const violationDetails = useSelector(selectViolationDetails);
  const violationId = useSelector(selectViolationId);
  const name = useSelector(selectPreviousRouteName);
  const prevParams = useSelector(selectRouterPrevParams);
  const submitError = useSelector(selectSubmitError);
  const waiverComments = useSelector(selectComments);
  const waiverReasonId = useSelector(selectWaiverReasonId);
  const submitMaskState = useSelector(selectSubmitMaskState);
  const { loadApplicableWaiversError, vulnerabilityDetailsError } = useSelector(selectViolationSlice);
  const { loading: webhookInfoLoading, error: webhookInfoError, waiverRequestWebhookAvailable } = useSelector(
    selectWaiverRequestWebhookState
  );
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const waiverReasons = useSelector(selectWaiverReasons);

  const loadViolation = (id) => dispatch(loadViolationAction(id));
  const getWaiverRequestWebhooks = () => dispatch(actions.getWaiverRequestWebhooks());
  const onSubmitAction = () => dispatch(actions.submitRequestWaiver({ policyViolationLink, addWaiverLink }));
  const cancelAction = () => dispatch(returnToAddWaiverOriginPage());
  const setWaiverComments = (comment) => dispatch(actions.setRequestWaiverComments(comment));
  const setWaiverReasonId = (reasonId) => dispatch(actions.setWaiverReasonId(reasonId));

  const isSubmitButtonDisabled = webhookInfoLoading || !!webhookInfoError || !waiverRequestWebhookAvailable;

  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason', type: 'system' }, ...waiverReasons];

  const onSubmit = () => {
    if (!isSubmitButtonDisabled) {
      onSubmitAction();
    }
  };

  const { policyViolationId, policyName, constraintName, componentName, reasons = [] } = extractViolationDetails(
    violationDetails
  );

  const backButtonProps = {
    violationId,
    prevStateName: name,
    prevParams,
    isStandaloneDeveloper,
  };

  const error = violationId
    ? violationDetailsError || loadApplicableWaiversError || vulnerabilityDetailsError
    : 'No Violation ID provided.';

  const load = () => {
    if (violationId) {
      loadViolation(violationId);
      getWaiverRequestWebhooks();
    }
  };

  useEffect(() => {
    dispatch(waiverActions.loadCachedWaiverReasons());
    dispatch(actions.clearInitState());
  }, []);

  useEffect(load, [violationId]);

  const curlExample = `curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" ${getRequestWaiverUrl(
    policyViolationId
  )} --data-binary 'waiver comment (optional)'`;

  const reasonsElements = reasons.map((reason) => (
    <dd key={reason} className="nx-read-only__data">
      {reason}
    </dd>
  ));

  const onReasonChange = (event) => {
    setWaiverReasonId(event.currentTarget.value ?? null);
  };

  const policyViolationLink = getPolicyViolationUiLink(violationId);

  const addWaiverLink = getAddWaiverUiLink(violationId, waiverComments?.trimmedValue, waiverReasonId);

  const urlLinkEl = useRef();
  return (
    <NxPageMain id="request-waiver-page">
      <AddAndRequestWaiversBackButton {...backButtonProps} />
      <NxPageTitle>
        <NxH1>Request Waiver</NxH1>
        <NxPageTitle.Description>
          A waiver request will be sent to the designated approver upon submit, if a webhook event for waiver requests
          is configured. If you are unsure about the webhook configuration, share the policy violation ID and the curl
          command with the designated approver.
        </NxPageTitle.Description>
      </NxPageTitle>

      <NxTile>
        <NxTile.Content>
          <LoadWrapper loading={loading} error={error} retryHandler={load}>
            {() => (
              <NxStatefulForm
                className="iq-request-waiver-form"
                onCancel={cancelAction}
                submitError={submitError}
                showValidationErrors={!!submitError}
                submitBtnClasses={classNames('request-waiver-submit', { disabled: isSubmitButtonDisabled })}
                onSubmit={onSubmit}
                submitMaskState={submitMaskState}
              >
                <dl className="nx-read-only">
                  <dt className="nx-read-only__label">Component</dt>
                  <dd className="nx-read-only__data">{componentName}</dd>
                  <dt className="nx-read-only__label">Policy</dt>
                  <dd className="nx-read-only__data">{policyName}</dd>
                  <dt className="nx-read-only__label">Constraint Name</dt>
                  <dd className="nx-read-only__data">{constraintName}</dd>
                  <dt className="nx-read-only__label">Conditions</dt>
                  {reasonsElements}
                </dl>
                <NxFieldset className="iq-request-waiver-form__reason" label="Reason">
                  <NxFormSelect id="waiver-reason-select" onChange={onReasonChange}>
                    {waiverReasonsToRender.map(({ id, reasonText }) => (
                      <option key={id} value={id}>
                        {reasonText}
                      </option>
                    ))}
                  </NxFormSelect>
                </NxFieldset>
                <NxCodeSnippet
                  label="Policy Violation ID"
                  content={policyViolationId}
                  className="visual-testing-ignore iq-request-waivers-popover__violation-id"
                  id="request-waivers-policy-violation-id"
                />
                <NxCodeSnippet
                  label="Policy Violation Details Page"
                  content={policyViolationLink}
                  className="visual-testing-ignore iq-request-waivers-popover__page-url"
                  onCopyUsingBtn={() => urlLinkEl.current.select()}
                />
                <NxTextLink newTab href={policyViolationLink}>
                  <input
                    readOnly
                    ref={urlLinkEl}
                    value={policyViolationLink}
                    className="visual-testing-ignore iq-request-waivers-popover__link-input"
                  />
                </NxTextLink>
                <NxCodeSnippet
                  label="Curl Example"
                  content={curlExample}
                  className="visual-testing-ignore iq-request-waivers-popover__curl"
                />
                <NxFieldset className="iq-request-waiver-form__comments" label="Comments">
                  <NxTextInput type="textarea" maxLength={1000} {...waiverComments} onChange={setWaiverComments} />
                </NxFieldset>
                <WaiverRequestWebhookAlert />
              </NxStatefulForm>
            )}
          </LoadWrapper>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
};

export default RequestWaiversPage;
