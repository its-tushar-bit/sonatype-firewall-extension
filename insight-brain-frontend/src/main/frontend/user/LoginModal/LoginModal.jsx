/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './userLoginSlice';
import * as PropTypes from 'prop-types';
import useConditionalAutoFocus from 'MainRoot/react/useConditionalAutoFocus';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { not, includes } from 'ramda';

import {
  NxButton,
  NxStatefulForm,
  NxFormGroup,
  NxInfoAlert,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { selectLoginModalState, selectLoginModalSubmitState, selectSystemNoticeServerData } from './userLoginSelectors';
import { selectRouterState } from 'MainRoot/reduxUiRouter/routerSelectors';
import * as sbomManagerUtil from 'MainRoot/sbomManager/sbomManagerUtil';

export default function LoginModal({ onSubmit, onDismiss, onClickSSO }) {
  // State selectors
  const dispatch = useDispatch();
  const routeState = useSelector(selectRouterState);
  const systemNotice = useSelector(selectSystemNoticeServerData);
  const {
    isLicensed,
    products,
    showSamlSso,
    showLoginModal,
    username,
    password,
    isUnauthenticatedPagesEnabled,
  } = useSelector(selectLoginModalState);
  const { loginSubmitError, loginSubmitMaskState } = useSelector(selectLoginModalSubmitState);

  const uiRouterState = useRouterState();

  const vulnSearchHref = uiRouterState.href('vulnerabilitySearch');

  const { userInput } = nxTextInputStateHelpers;

  // Modal and form logic
  const isFormValid =
    username.value.length === 0 || password.value.length === 0 ? 'Username and password are required' : null;

  const samlSsoButtonRef = useConditionalAutoFocus(showSamlSso && isFormValid);

  const isSbomManagerOnlyLicense = sbomManagerUtil.isSbomManagerOnlyLicenseProduct(products);

  const renderVulnerabilityLink =
    isLicensed &&
    isUnauthenticatedPagesEnabled &&
    not(includes(routeState.name, ['vulnerabilitySearchDetail', 'vulnerabilitySearch'])) &&
    !isSbomManagerOnlyLicense;

  const userInputValidator = (val) => {
    return val.length ? null : 'Required field';
  };

  const additionalFooterBtns = () =>
    showSamlSso && (
      <NxButton ref={samlSsoButtonRef} type="button" id="iq-login-modal-sso-button" onClick={onClickSSO}>
        Single Sign-On (SSO)
      </NxButton>
    );

  const onChangeUsername = (val) => {
    dispatch(actions.setUsername(userInput(userInputValidator, val)));
  };

  const onChangePassword = (val) => {
    dispatch(actions.setPassword(userInput(userInputValidator, val)));
  };

  const onCancelHandler = (e) => {
    e.preventDefault();
    onDismiss();
  };

  const isShowCancel =
    includes(routeState.name, [
      'vulnerabilitySearchDetail',
      'vulnerabilitySearch',
      'firewall.quarantinedComponentReport',
    ]) &&
    isUnauthenticatedPagesEnabled &&
    isLicensed;

  return (
    <>
      {showLoginModal && (
        <NxModal id="iq-login-modal" onCancel={isShowCancel ? onCancelHandler : null}>
          <NxStatefulForm
            onSubmit={() => {
              onSubmit({ loginUsername: username.value, loginPassword: password.value });
            }}
            onCancel={isShowCancel ? onCancelHandler : null}
            submitBtnText="Sign in"
            submitError={loginSubmitError}
            submitErrorTitleMessage=" "
            submitMaskState={loginSubmitMaskState}
            validationErrors={isFormValid}
            additionalFooterBtns={additionalFooterBtns()}
          >
            <header className="nx-modal-header">
              <h2 className="nx-h2">Sign in</h2>
            </header>
            <div className="nx-modal-content">
              {systemNotice?.enabled && (
                <NxInfoAlert className="iq-login-modal-system-notice" iconLabel="system notice information">
                  {systemNotice?.message}
                </NxInfoAlert>
              )}
              {/* Username */}
              <NxFormGroup id="iq-login-modal-username" label="Username" isRequired={true}>
                <NxTextInput
                  id="iq-login-modal-username-input"
                  type="text"
                  validatable={true}
                  {...username}
                  onChange={onChangeUsername}
                  autoComplete="username"
                  aria-required={true}
                />
              </NxFormGroup>
              {/* Password */}
              <NxFormGroup id="iq-login-modal-password" label="Password" isRequired={true}>
                <NxTextInput
                  id="iq-login-modal-password-input"
                  type="password"
                  validatable={true}
                  {...password}
                  onChange={onChangePassword}
                  autoComplete="current-password"
                  aria-required={true}
                />
              </NxFormGroup>
              {renderVulnerabilityLink && (
                <p className="iq-login-modal-helper-text nx-p">
                  Look up a vulnerability without signing in at{' '}
                  <NxTextLink href={vulnSearchHref}>Vulnerability Lookup</NxTextLink>.
                </p>
              )}
            </div>
          </NxStatefulForm>
        </NxModal>
      )}
    </>
  );
}

LoginModal.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onDismiss: PropTypes.func.isRequired,
  onClickSSO: PropTypes.func.isRequired,
};
