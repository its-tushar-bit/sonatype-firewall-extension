/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { head, split, toUpper, keys } from 'ramda';
import {
  NxH1,
  NxPageMain,
  NxPageTitle,
  NxTile,
  NxStatefulForm,
  NxH2,
  NxSmallThreatCounter,
  NxInfoAlert,
  NxFormSelect,
  NxDateInput,
  NxFieldset,
  NxTextInput,
  hasValidationErrors,
  NxFormGroup,
} from '@sonatype/react-shared-components';
import classnames from 'classnames';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { getContainerReportParams } from 'MainRoot/applicationReport/applicationReportActions';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import ViolationExclamation from 'MainRoot/react/ViolationExclamation';
import { actions } from './addContainerImageWaiverPageSlice';
import { selectAddContainerImageWaiverPage, selectWaiverReasons } from './addContainerImageWaiverPageSelectors';
import { getExpirationDaysMessage, isCustomExpiryTimeSelected, useWaiverExpirations } from 'MainRoot/util/waiverUtils';
import {
  selectIsContainerImagesEvaluationEnabled,
  selectIsExpireWhenRemediationAvailableWaiversEnabled,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

import './_addContainerImageWaiverPage.scss';

const INFO_ALERT_TEXT = `Proceeding to create a waiver will waive all failing policy violations identified in this evaluation. 
  After applying this waiver, you can review waived policy violations per component within the 
  Container Image Report.`;

const FEATURE_NOT_SUPPORTED_TEXT = 'This feature is not supported.';

export default function AddContainerImageWaiverPage() {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const { publicId, scanId, origin } = useSelector(selectRouterCurrentParams);
  const containerReportParams = getContainerReportParams(publicId, scanId, origin);
  const backButtonHref = uiRouterState.href('firewall.containerReport', containerReportParams);
  const isContainerImagesEvalEnabled = useSelector(selectIsContainerImagesEvaluationEnabled);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const {
    loading,
    error: errorProp,
    containerImageName,
    failViolationsCount,
    affectedComponentsCount,
    policyNameList,
    threatLevelCounts,
    expiryTime,
    customExpiryTime,
    waiverComments,
    submitError,
    submitMaskState,
  } = useSelector(selectAddContainerImageWaiverPage);
  const loadError = !isContainerImagesEvalEnabled && !isProductFeaturesLoading ? FEATURE_NOT_SUPPORTED_TEXT : errorProp;
  const waiverReasons = useSelector(selectWaiverReasons);
  const containerImageLabel = head(split(' : ', containerImageName));
  const isExpireWhenRemediationAvailable = useSelector(selectIsExpireWhenRemediationAvailableWaiversEnabled);
  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);
  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);
  const plural = (n) => (n === 1 ? '' : 's');
  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);
  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason', type: 'system' }, ...waiverReasons];

  const loadData = () => dispatch(actions.load(publicId));
  const setExpiryTime = (value) => dispatch(actions.setExpiryTime(value));
  const setCustomExpiryTime = (value) => dispatch(actions.setCustomExpiryTime(value));
  const setWaiverReason = (value) => dispatch(actions.setWaiverReason(value));
  const setWaiverComment = (value) => dispatch(actions.setWaiverComment(value));
  const save = () => dispatch(actions.save({ publicId, scanId, origin }));

  useEffect(() => {
    loadData();
  }, []);

  const onExpiryTimeChange = (value) => {
    setExpiryTime(value === 'never' ? null : value);
  };

  const onReasonChange = (value) => {
    setWaiverReason(value ?? null);
  };

  const returnToContainerReportPage = () => {
    dispatch(actions.returnToContainerReportPage(publicId, scanId, origin));
  };

  const formValidationErrors = hasValidationErrors(customExpiryTime.validationErrors)
    ? 'Date must be in the future.'
    : null;

  return (
    <NxPageMain className="add-firewall-container-image-waiver-page">
      <MenuBarBackButton text="Back to Container Report" href={backButtonHref} />
      <NxPageTitle>
        <NxH1 className="nx-h1">Add Waiver</NxH1>
      </NxPageTitle>
      <NxTile>
        <NxStatefulForm
          loading={loading}
          doLoad={loadData}
          loadError={loadError}
          onSubmit={save}
          onCancel={returnToContainerReportPage}
          validationErrors={formValidationErrors}
          submitError={submitError}
          submitMaskState={submitMaskState}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2 id="container-waiver-config-header">Waiver Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <div className="add-waiver-threat-indicators">
              {keys(threatLevelCounts).length && (
                <NxSmallThreatCounter
                  data-testid="add-container-image-waiver-threat-counter"
                  criticalCount={threatLevelCounts?.critical || null}
                  severeCount={threatLevelCounts?.severe || null}
                  moderateCount={threatLevelCounts?.moderate || null}
                />
              )}
              <div className="iq-caption">
                <h3 className="iq-caption__text">
                  {failViolationsCount} FAILED VIOLATION
                  {toUpper(plural(failViolationsCount))}
                </h3>
                <p className="iq-caption__sub-text">
                  Affecting {affectedComponentsCount} component
                  {plural(affectedComponentsCount)}
                </p>
              </div>
            </div>

            <NxInfoAlert role="status">{INFO_ALERT_TEXT}</NxInfoAlert>

            <dl className="nx-read-only add-waiver-policy">
              <dt className="nx-read-only__label">{policyNameList?.length > 1 ? 'Policies' : 'Policy'}</dt>
              <dd className="nx-read-only__data">
                {policyNameList &&
                  policyNameList.map(({ policyName, threatLevelCategory }, index) => (
                    <div key={index}>
                      <ViolationExclamation threatLevelCategory={threatLevelCategory} />
                      <span className={classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`)}>
                        {policyName}
                      </span>
                    </div>
                  ))}
              </dd>
            </dl>

            <dl className="nx-read-only add-waiver-container-image">
              <dt className="nx-read-only__label">{containerImageLabel}</dt>
              <dd className="nx-read-only__data">{containerImageName} (Container)</dd>
            </dl>

            <NxFieldset className="add-waiver-expiryTime" label="Waiver Expiration" isRequired>
              <div className="add-container-image-waiver-form__expiryTime-block">
                <div className="add-container-image-waiver-form__controls-row">
                  <NxFormSelect id="add-container-image-waiver-expiration-select" onChange={onExpiryTimeChange}>
                    {waiverExpirations &&
                      waiverExpirations.map(({ name, value }, index) => (
                        <option key={index} value={value}>
                          {name}
                        </option>
                      ))}
                  </NxFormSelect>
                  {customExpiryTimeSelected && (
                    <NxDateInput
                      className="add-container-image-waiver-form__date-input"
                      {...customExpiryTime}
                      onChange={setCustomExpiryTime}
                      validatable={true}
                      data-testid="add-container-image-waiver-custom-date"
                    />
                  )}
                </div>
                {daysDiffMessage && (
                  <div className="add-container-image-waiver-form__expiration-days-diff visual-testing-ignore">
                    {daysDiffMessage}
                  </div>
                )}
              </div>
            </NxFieldset>

            <NxFieldset className="add-waiver-reason" label="Reason">
              <NxFormSelect id="add-container-image-waiver-reason-select" onChange={onReasonChange}>
                {waiverReasonsToRender.map(({ id, reasonText }) => (
                  <option key={id} value={id}>
                    {reasonText}
                  </option>
                ))}
              </NxFormSelect>
            </NxFieldset>

            <NxFormGroup className="add-waiver-comments" label="Comments">
              <NxTextInput
                id="add-container-image-comments"
                type="textarea"
                {...waiverComments}
                onChange={setWaiverComment}
              />
            </NxFormGroup>
          </NxTile.Content>
        </NxStatefulForm>
      </NxTile>
    </NxPageMain>
  );
}
