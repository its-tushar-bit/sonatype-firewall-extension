/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React, { useState, useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { NxButton, NxFieldset, NxFormGroup, NxRadio, NxTextInput, NxTextLink } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { AUTHENTICATION_TYPES } from './utils';
import './_gitHubAppAuthenticationMethod.scss';

const GitHubAppAuthenticationMethod = ({
  sourceControl,
  setValue,
  areFieldsDisabled,
  onChangeToken,
  isGithubAppAuthenticationEnabled,
}) => {
  const dispatch = useDispatch();
  const [authMethod, setAuthMethod] = useState(() => {
    if (sourceControl?.authenticationType?.value) {
      return sourceControl.authenticationType.value;
    }
    if (sourceControl?.githubApp?.value?.installationId) {
      return AUTHENTICATION_TYPES.GITHUB_APP;
    }
    if (sourceControl?.token?.rscValue?.value) {
      return AUTHENTICATION_TYPES.PAT;
    }
    return null;
  });
  const handleOpenModal = () => dispatch(actions.openModal());
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
      <NxFieldset
        id="github-authentication-method"
        label="Authentication Method"
        isRequired
        disabled={areFieldsDisabled}
      >
        <NxRadio
          name="githubAuthMethod"
          value={AUTHENTICATION_TYPES.GITHUB_APP}
          onChange={() => handleAuthMethodChange(AUTHENTICATION_TYPES.GITHUB_APP)}
          isChecked={authMethod === AUTHENTICATION_TYPES.GITHUB_APP}
          disabled={areFieldsDisabled}
        >
          GitHub App (Recommended)
        </NxRadio>
        {authMethod === AUTHENTICATION_TYPES.GITHUB_APP && (
          <div className="iq-github-app-auth-status">
            {isConfigured ? (
              <dl className="iq-github-app-auth-status__configured-box">
                <dt>Organization:</dt>
                <dd>{sourceControl?.githubApp?.value?.accountName || ''}</dd>
                {sourceControl?.githubApp?.value?.name && (
                  <>
                    <dt>App:</dt>
                    <dd>{sourceControl.githubApp.value.name}</dd>
                  </>
                )}
                {sourceControl?.repositoryUrl?.value && (
                  <>
                    <dt>Repositories:</dt>
                    <dd>
                      <NxTextLink href={sourceControl.repositoryUrl.value} external>
                        Go to GitHub Repositories
                      </NxTextLink>
                    </dd>
                  </>
                )}
                {sourceControl?.githubApp?.value?.configurationDate && (
                  <>
                    <dt>Configuration Date:</dt>
                    <dd>
                      {new Date(sourceControl.githubApp.value.configurationDate).toLocaleString('en-US', {
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: 'numeric',
                        minute: '2-digit',
                        hour12: true,
                        timeZoneName: 'short',
                      })}
                    </dd>
                  </>
                )}
              </dl>
            ) : (
              <NxButton variant="tertiary" type="button" onClick={handleOpenModal} disabled={areFieldsDisabled}>
                Configure GitHub App
              </NxButton>
            )}
          </div>
        )}

        <NxRadio
          name="githubAuthMethod"
          value={AUTHENTICATION_TYPES.PAT}
          onChange={() => handleAuthMethodChange(AUTHENTICATION_TYPES.PAT)}
          isChecked={authMethod === AUTHENTICATION_TYPES.PAT}
          disabled={areFieldsDisabled}
        >
          Personal Access Token
        </NxRadio>
        {authMethod === AUTHENTICATION_TYPES.PAT && (
          <div className="iq-github-app-auth-status__token">
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
          </div>
        )}
      </NxFieldset>
    </>
  );
};

GitHubAppAuthenticationMethod.propTypes = {
  sourceControl: PropTypes.object,
  setValue: PropTypes.func,
  areFieldsDisabled: PropTypes.bool.isRequired,
  onChangeToken: PropTypes.func.isRequired,
  isGithubAppAuthenticationEnabled: PropTypes.bool.isRequired,
};

export default GitHubAppAuthenticationMethod;
