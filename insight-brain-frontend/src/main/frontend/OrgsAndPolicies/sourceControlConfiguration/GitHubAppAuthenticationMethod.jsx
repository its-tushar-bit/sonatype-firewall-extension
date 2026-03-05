/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React, { useState, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { NxButton, NxFieldset, NxFormGroup, NxRadio, NxTextInput } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import GitHubAppDetailsBox from './GitHubAppDetailsBox';
import { AUTHENTICATION_TYPES } from './utils';
import './_gitHubAppAuthenticationMethod.scss';

const GitHubAppAuthenticationMethod = ({
  sourceControl,
  setValue,
  setIsInherited,
  areFieldsDisabled,
  onChangeToken,
  isGithubAppAuthenticationEnabled,
}) => {
  const dispatch = useDispatch();
  const [authMethod, setAuthMethod] = useState(() => {
    // If authenticationType has an explicit value, use it
    if (sourceControl?.authenticationType?.value) {
      return sourceControl.authenticationType.value;
    }
    // If inherited, don't infer from token/githubApp (use parent's value)
    if (sourceControl?.authenticationType?.isInherited) {
      return null;
    }
    // For non-inherited cases without explicit value, infer from what's configured
    if (sourceControl?.githubApp?.value?.installationId) {
      return AUTHENTICATION_TYPES.GITHUB_APP;
    }
    if (sourceControl?.token?.rscValue?.value) {
      return AUTHENTICATION_TYPES.PAT;
    }
    return null;
  });
  const handleOpenModal = () => dispatch(actions.openModal());
  // Only show inheritance options when setIsInherited is provided (Org/App level)
  const supportsInheritance = Boolean(setIsInherited);
  // Check authenticationType.isInherited instead of githubApp.isInherited
  // This fixes the issue where selecting PAT (no GitHub App) would show as inherited on reload
  const isAuthMethodInherited = supportsInheritance && sourceControl?.authenticationType?.isInherited;

  const parentGithubApp = sourceControl?.githubApp?.parentValue;
  const hasParentConfig = parentGithubApp?.installationId;
  // Check if parent has any authentication configured (GitHub App OR PAT)
  const hasParentAuth = hasParentConfig || Boolean(sourceControl?.token?.parentValue?.value);
  // Determine the effective authentication method (use parent's if inheriting)
  const parentAuthType = sourceControl?.authenticationType?.parentValue;
  const effectiveAuthMethod = isAuthMethodInherited ? parentAuthType || authMethod : authMethod;

  useEffect(() => {
    if (authMethod && setValue && !sourceControl?.authenticationType?.value) {
      setValue('authenticationType', authMethod);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    // Intentionally run only on mount to set initial authentication type value
    // Re-running on authMethod changes would override user's manual selection
  }, []);

  const handleAuthMethodChange = (method) => {
    setAuthMethod(method);
    if (setValue) {
      setValue('authenticationType', method);
    }
  };

  //TODO: Determine if GitHub App is already configured via separate API
  const isConfigured = sourceControl?.githubApp?.value?.installationId;

  // If GitHub App authentication is not enabled, only show Personal Access Token option
  if (!isGithubAppAuthenticationEnabled) {
    return (
      <NxFormGroup label="Access Token" type="password" isRequired>
        <NxTextInput
          id="source-control-token"
          onChange={onChangeToken}
          {...sourceControl?.token.rscValue}
          disabled={areFieldsDisabled}
          type="password"
          autoComplete="new-password"
          validatable
        />
      </NxFormGroup>
    );
  }

  return (
    <>
      <NxFieldset id="github-authentication-method" label="Authentication Method" isRequired={!isAuthMethodInherited}>
        {supportsInheritance && (
          <>
            <NxRadio
              key={`auth-method-inherit`}
              radioId="auth-method-inherit-radio"
              name="authMethodInheritance"
              value="Inherit"
              onChange={() => setIsInherited('authenticationType', true)}
              isChecked={isAuthMethodInherited}
              disabled={areFieldsDisabled}
            >
              {sourceControl?.provider?.parentName && hasParentAuth
                ? `Inherit from ${sourceControl?.provider?.parentName}`
                : 'Inherit (Not Configured)'}
            </NxRadio>
            <NxRadio
              key={`auth-method-override`}
              radioId="auth-method-override-radio"
              name="authMethodInheritance"
              value="Override"
              onChange={() => setIsInherited('authenticationType', false)}
              isChecked={!isAuthMethodInherited}
              disabled={areFieldsDisabled}
            >
              Override
            </NxRadio>
          </>
        )}

        {supportsInheritance && <div className="iq-github-app-auth-status__section-label">Authentication Type</div>}
        <NxRadio
          radioId="auth-type-github-app-radio"
          name="githubAuthMethod"
          value={AUTHENTICATION_TYPES.GITHUB_APP}
          onChange={() => handleAuthMethodChange(AUTHENTICATION_TYPES.GITHUB_APP)}
          isChecked={effectiveAuthMethod === AUTHENTICATION_TYPES.GITHUB_APP}
          disabled={areFieldsDisabled || isAuthMethodInherited}
        >
          GitHub App (Recommended)
        </NxRadio>
        {/* Show inherited config when inheriting and parent is using GitHub App */}
        {isAuthMethodInherited && effectiveAuthMethod === AUTHENTICATION_TYPES.GITHUB_APP && hasParentConfig && (
          <div className="iq-github-app-auth-status">
            <GitHubAppDetailsBox githubApp={parentGithubApp} linkText="View GitHub App configuration" />
          </div>
        )}

        {/* Show own config when overriding and GitHub App is selected */}
        {effectiveAuthMethod === AUTHENTICATION_TYPES.GITHUB_APP && !isAuthMethodInherited && (
          <div className="iq-github-app-auth-status">
            {isConfigured ? (
              <GitHubAppDetailsBox
                githubApp={sourceControl?.githubApp?.value}
                linkText="Go to GitHub Installation Settings"
                onReconfigure={handleOpenModal}
                disabled={areFieldsDisabled}
              />
            ) : (
              <NxButton variant="tertiary" type="button" onClick={handleOpenModal} disabled={areFieldsDisabled}>
                Configure GitHub App
              </NxButton>
            )}
          </div>
        )}

        <NxRadio
          radioId="auth-type-pat-radio"
          name="githubAuthMethod"
          value={AUTHENTICATION_TYPES.PAT}
          onChange={() => handleAuthMethodChange(AUTHENTICATION_TYPES.PAT)}
          isChecked={effectiveAuthMethod === AUTHENTICATION_TYPES.PAT}
          disabled={areFieldsDisabled || isAuthMethodInherited}
        >
          Personal Access Token
        </NxRadio>
        {(effectiveAuthMethod === AUTHENTICATION_TYPES.PAT || effectiveAuthMethod === null) && (
          <div className="iq-github-app-auth-status__token">
            <NxFormGroup label="Access Token" type="password" isRequired>
              <NxTextInput
                id="source-control-token"
                onChange={onChangeToken}
                {...sourceControl?.token.rscValue}
                value={isAuthMethodInherited ? '#~FAKE~SECRET~KEY~#' : sourceControl?.token?.rscValue?.value}
                type="password"
                autoComplete="new-password"
                validatable
                disabled={areFieldsDisabled || isAuthMethodInherited}
              />
            </NxFormGroup>
          </div>
        )}
      </NxFieldset>
    </>
  );
};

GitHubAppAuthenticationMethod.propTypes = {
  areFieldsDisabled: PropTypes.bool.isRequired,
  onChangeToken: PropTypes.func.isRequired,
  isGithubAppAuthenticationEnabled: PropTypes.bool.isRequired,
  sourceControl: PropTypes.shape({
    authenticationType: PropTypes.shape({
      value: PropTypes.string,
      isInherited: PropTypes.bool,
      parentValue: PropTypes.string,
    }),
    githubApp: PropTypes.shape({
      value: PropTypes.shape({
        installationId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        name: PropTypes.string,
        accountName: PropTypes.string,
      }),
      isInherited: PropTypes.bool,
      parentValue: PropTypes.shape({
        installationId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        name: PropTypes.string,
        accountName: PropTypes.string,
      }),
      parentName: PropTypes.string,
    }),
    provider: PropTypes.shape({
      parentName: PropTypes.string,
    }),
    token: PropTypes.shape({
      rscValue: PropTypes.object,
      isInherited: PropTypes.bool,
      parentValue: PropTypes.object,
    }),
  }),
  setValue: PropTypes.func,
  setIsInherited: PropTypes.func,
};

export default GitHubAppAuthenticationMethod;
