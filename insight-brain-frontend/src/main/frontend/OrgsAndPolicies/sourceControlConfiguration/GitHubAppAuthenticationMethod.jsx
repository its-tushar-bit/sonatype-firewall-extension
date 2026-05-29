/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxFieldset, NxFormGroup, NxRadio, NxTextInput, NxTooltip } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { actions as sourceControlActions } from './sourceControlConfigurationSlice';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectHasEditPermission } from './sourceControlConfigurationSelectors';
import { AUTHENTICATION_TYPES, hasConfiguredGitHubApp, getScmFormStateStorageKey, saveFormStateWithFallback } from './utils';
import './_gitHubAppAuthenticationMethod.scss';

const GitHubAppAuthenticationMethod = ({
  sourceControl,
  setValue,
  setIsInherited,
  areFieldsDisabled,
  onChangeToken,
  isApplication,
}) => {
  const dispatch = useDispatch();
  const hasEditPermission = useSelector(selectHasEditPermission);
  const hasLocalGithubApp = hasConfiguredGitHubApp(sourceControl?.githubApps?.value);
  const hasParentGithubApp = hasConfiguredGitHubApp(sourceControl?.githubApps?.parentValue);
  const githubAppCount = sourceControl?.githubApps?.localCount ?? 0;
  const parentAppCount = sourceControl?.githubApps?.parentCount ?? 0;
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
    if (hasLocalGithubApp) {
      return AUTHENTICATION_TYPES.GITHUB_APP;
    }
    if (sourceControl?.token?.rscValue?.value) {
      return AUTHENTICATION_TYPES.PAT;
    }
    return null;
  });
  const handleOpenModal = () => {
    sessionStorage.setItem('githubAppReturnTo', JSON.stringify({ returnTo: 'sourceControl', ownerId: sourceControl?.ownerId }));
    dispatch(actions.openModal());
  };
  const handleManageGitHubApps = () => {
    const ownerType = isApplication ? 'application' : 'organization';
    const storageKey = getScmFormStateStorageKey(ownerType, sourceControl?.ownerId);
    const sourceControlToSave = { ...sourceControl };
    delete sourceControlToSave.token;
    delete sourceControlToSave.githubApps;
    saveFormStateWithFallback(storageKey, sourceControlToSave);
    dispatch(sourceControlActions.clearDirty());
    dispatch(stateGo('^.manage-github-apps'));
  };
  // Only show inheritance options when setIsInherited is provided (Org/App level)
  const supportsInheritance = Boolean(setIsInherited);
  // Check authenticationType.isInherited instead of githubApp.isInherited
  // This fixes the issue where selecting PAT (no GitHub App) would show as inherited on reload
  const isAuthMethodInherited = Boolean(supportsInheritance && sourceControl?.authenticationType?.isInherited);

  const hasParentConfig = hasParentGithubApp;
  // Check if parent has any authentication configured (GitHub App OR PAT)
  const hasParentAuth = hasParentConfig || Boolean(sourceControl?.token?.parentValue?.value);
  const parentAuthType = sourceControl?.authenticationType?.parentValue;
  const effectiveAuthMethod = isAuthMethodInherited ? parentAuthType || authMethod : authMethod;

  useEffect(() => {
    if (authMethod && setValue && !sourceControl?.authenticationType?.value) {
      setValue('authenticationType', authMethod);
    }
    // Intentionally run only on mount to set initial authentication type value
    // Re-running on authMethod changes would override user's manual selection
  }, []);

  // Sync local authMethod state with loaded data from backend after save/reload
  useEffect(() => {
    // Only update if we have a definitive value from backend
    if (sourceControl?.authenticationType?.value && sourceControl.authenticationType.value !== authMethod) {
      setAuthMethod(sourceControl.authenticationType.value);
    }
    // If not inherited and no explicit value, but we can infer from token/githubApp
    else if (
      !sourceControl?.authenticationType?.isInherited &&
      !sourceControl?.authenticationType?.value &&
      authMethod === null
    ) {
      if (hasLocalGithubApp) {
        setAuthMethod(AUTHENTICATION_TYPES.GITHUB_APP);
        if (setValue) {
          setValue('authenticationType', AUTHENTICATION_TYPES.GITHUB_APP);
        }
      } else if (sourceControl?.token?.rscValue?.value) {
        setAuthMethod(AUTHENTICATION_TYPES.PAT);
        if (setValue) {
          setValue('authenticationType', AUTHENTICATION_TYPES.PAT);
        }
      }
    }
  }, [
    sourceControl?.authenticationType?.value,
    sourceControl?.authenticationType?.isInherited,
    sourceControl?.githubApps?.value?.id,
    sourceControl?.githubApps?.value?.installationId,
    sourceControl?.token?.rscValue?.value,
  ]);

  const handleAuthMethodChange = (method) => {
    setAuthMethod(method);
    if (setValue) {
      setValue('authenticationType', method);
    }
    if (method === AUTHENTICATION_TYPES.PAT && setIsInherited && !isAuthMethodInherited) {
      setIsInherited('token', false);
    }
  };

  //TODO: Determine if GitHub App is already configured via separate API
  const isConfigured = hasLocalGithubApp;
  const shouldDisableAddButton = isApplication && githubAppCount >= 1;

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
              onChange={() => {
                setIsInherited('authenticationType', true);
                setIsInherited('token', true);
              }}
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
              onChange={() => {
                setIsInherited('authenticationType', false);
                setIsInherited('token', false);
              }}
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
        {isAuthMethodInherited && effectiveAuthMethod === AUTHENTICATION_TYPES.GITHUB_APP && parentAppCount > 0 && (
          <div className="iq-github-app-auth-status">
            <span className="iq-github-app-auth-status__count">
              Inherit {parentAppCount} GitHub App{parentAppCount !== 1 ? 's' : ''} from{' '}
              {sourceControl?.githubApps?.parentName}
            </span>
          </div>
        )}
        {/* Show own config when overriding and GitHub App is selected */}
        {!isAuthMethodInherited && effectiveAuthMethod === AUTHENTICATION_TYPES.GITHUB_APP && (
          <div className="iq-github-app-auth-status">
            <span className="iq-github-app-auth-status__count">
              {githubAppCount} GitHub App{githubAppCount !== 1 ? 's' : ''} configured
            </span>
            <div className="iq-github-app-auth-status__buttons">
              {hasEditPermission && (shouldDisableAddButton ? (
                <NxTooltip title="IQ applications can only have one GitHub App associated">
                  <span>
                    <NxButton id="github-app-add-button" variant="tertiary" type="button" disabled>
                      Add GitHub App
                    </NxButton>
                  </span>
                </NxTooltip>
              ) : (
                <NxButton
                  id="github-app-add-button"
                  variant="tertiary"
                  type="button"
                  onClick={handleOpenModal}
                  disabled={areFieldsDisabled}
                >
                  Add GitHub App
                </NxButton>
              ))}
              <NxButton
                id="manage-github-apps-button"
                variant="tertiary"
                type="button"
                onClick={handleManageGitHubApps}
                disabled={areFieldsDisabled}
              >
                Manage GitHub Apps
              </NxButton>
            </div>
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
        {effectiveAuthMethod === AUTHENTICATION_TYPES.PAT && (
          <div className="iq-github-app-auth-status__token">
            <NxFormGroup label="Access Token" type="password" isRequired>
              <NxTextInput
                id="source-control-token"
                onChange={onChangeToken}
                {...sourceControl?.token.rscValue}
                type="password"
                inputAttributes={{ autoComplete: 'new-password' }}
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
  isApplication: PropTypes.bool,
  onChangeToken: PropTypes.func.isRequired,
  sourceControl: PropTypes.shape({
    authenticationType: PropTypes.shape({
      value: PropTypes.string,
      isInherited: PropTypes.bool,
      parentValue: PropTypes.string,
    }),
    githubApp: PropTypes.shape({
      value: PropTypes.shape({
        id: PropTypes.string,
        installationId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
        name: PropTypes.string,
        accountName: PropTypes.string,
      }),
      isInherited: PropTypes.bool,
      parentValue: PropTypes.shape({
        id: PropTypes.string,
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
