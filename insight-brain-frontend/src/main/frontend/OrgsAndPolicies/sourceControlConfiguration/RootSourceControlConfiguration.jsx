/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxCheckbox,
  NxForm,
  NxFormGroup,
  NxFormRow,
  NxFormSelect,
  NxStatefulForm,
  NxTextInput,
  NxToggle,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {
  getValidationMessage,
  providerNeedsUsername,
  DEFAULT_BRANCH_SUBLABEL,
  effectiveProvider,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectSourceControlConfigurationSlice,
  selectValidationError,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import {
  selectIsAutomationSupported,
  selectTenantScmOptionsTypes,
  selectIsGithubAppAuthenticationEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import ScmProviderOptions from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ScmProviderOptions';
import RenderMarkdown from 'MainRoot/react/RenderMarkdown';
import GitHubAppAuthenticationMethod from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppAuthenticationMethod';

const RootSourceControlConfiguration = () => {
  const dispatch = useDispatch();
  const {
    formLoading,
    loadError,
    submitError,
    submitMaskState,
    isDirty,
    sourceControl,
    serverSourceControl,
  } = useSelector(selectSourceControlConfigurationSlice);
  const isAutomationSupported = useSelector(selectIsAutomationSupported);
  const validationError = useSelector(selectValidationError);
  const sourceControlOptions = useSelector(selectTenantScmOptionsTypes);
  const isGithubAppAuthenticationEnabled = useSelector(selectIsGithubAppAuthenticationEnabled);

  const doLoad = () => dispatch(actions.load());
  const save = () => dispatch(actions.save());
  const showResetModal = () => dispatch(actions.showResetModal());
  const onChangeProvider = (event) => dispatch(actions.setProvider(event.target.value));
  const onChangeUsername = (val) => dispatch(actions.setUsername(val));
  const onChangeToken = (val) => dispatch(actions.setToken(val));
  const onChangeBranch = (val) => dispatch(actions.setBaseBranch(val));
  const setValue = (property, val) => dispatch(actions.setValue({ property, val }));
  const toggleValue = (property) => dispatch(actions.toggleValue(property));
  const onChangeClosePrAfterDaysOpen = (val) => dispatch(actions.setClosePrAfterDaysOpen(val));

  const getSCMProvider = () => {
    switch (sourceControl?.provider?.rscValue?.value) {
      case 'github':
        return 'Github';
      case 'gitlab':
        return 'Gitlab';
      case 'azure':
        return 'Azure DevOps';
      case 'bitbucket':
        return 'Bitbucket';
      default:
        return 'Git';
    }
  };

  const mapSourceControlOptionToToggle = (id, title, description, optionName) => {
    const scmProvider = sourceControl?.provider?.rscValue?.value;
    if (id === 'source-control-remediation-pull-requests' && sourceControl?.ownerId === 'ROOT_ORGANIZATION_ID') {
      return (
        <NxTooltip
          key={id}
          title={
            !isAutomationSupported && optionName !== 'sshEnabled' ? 'This feature is not supported by your license' : ''
          }
        >
          <div>
            <NxToggle
              key={id}
              id={id}
              className="iq-source-control-toggle"
              onChange={() => toggleValue(optionName)}
              isChecked={sourceControl?.[optionName].value ?? false}
              disabled={!scmProvider || (!isAutomationSupported && optionName !== 'sshEnabled')}
            >
              <span className="iq-source-control-toggle__title">{title}</span>
              <RenderMarkdown className="iq-source-control-toggle__text">{description}</RenderMarkdown>
            </NxToggle>
            <div className="git-advanced-options">
              <h5>Advanced {getSCMProvider()} Options</h5>
              {(scmProvider === 'github' || scmProvider === 'gitlab') && (
                <NxCheckbox
                  name="failed-checks-advanced-option"
                  onChange={() => toggleValue('closePrOnFailedChecksEnabled')}
                  isChecked={sourceControl?.closePrOnFailedChecksEnabled?.value}
                  disabled={!scmProvider || !sourceControl?.remediationPullRequestsEnabled?.value}
                >
                  Close AutoPRs when one or more required checks fail
                </NxCheckbox>
              )}
              <NxCheckbox
                name="after-days-open-advanced-option"
                onChange={() => toggleValue('closePrAfterDaysOpenEnabled')}
                isChecked={sourceControl?.closePrAfterDaysOpenEnabled?.value}
                disabled={!scmProvider || !sourceControl?.remediationPullRequestsEnabled.value}
              >
                Close AutoPRs that have not been merged or closed after:
              </NxCheckbox>
              <NxTextInput
                {...sourceControl?.closePrAfterDays.rscValue}
                onChange={onChangeClosePrAfterDaysOpen}
                validatable
                disabled={
                  !scmProvider ||
                  !sourceControl?.closePrAfterDaysOpenEnabled.value ||
                  !sourceControl?.remediationPullRequestsEnabled.value
                }
                placeholder="Ex. 7"
              />
              <span className="close-pr-after-days">Days</span>
            </div>
          </div>
        </NxTooltip>
      );
    }
    return (
      <NxTooltip
        key={id}
        title={
          !isAutomationSupported && optionName !== 'sshEnabled' ? 'This feature is not supported by your license' : ''
        }
      >
        <NxToggle
          key={id}
          id={id}
          className="iq-source-control-toggle"
          onChange={() => toggleValue(optionName)}
          isChecked={sourceControl?.[optionName].value ?? false}
          disabled={!sourceControl?.provider.rscValue.value || (!isAutomationSupported && optionName !== 'sshEnabled')}
        >
          <span className="iq-source-control-toggle__title">{title}</span>
          <RenderMarkdown className="iq-source-control-toggle__text">{description}</RenderMarkdown>
        </NxToggle>
      </NxTooltip>
    );
  };

  const additionalFooterBtns = (
    <NxButton
      id="reset-source-control-button"
      variant="tertiary"
      type="button"
      disabled={!sourceControl?.id}
      onClick={showResetModal}
    >
      Reset
    </NxButton>
  );

  const provider = effectiveProvider(sourceControl, serverSourceControl);
  const showGithubAppAuth = provider === 'github' && isGithubAppAuthenticationEnabled;

  return (
    <NxStatefulForm
      onSubmit={save}
      doLoad={doLoad}
      loading={formLoading}
      loadError={loadError}
      validationErrors={getValidationMessage(isDirty, validationError)}
      submitMaskState={submitMaskState}
      submitError={submitError}
      submitBtnText={sourceControl?.id ? 'Update' : 'Create'}
      additionalFooterBtns={additionalFooterBtns}
    >
      <NxForm.RequiredFieldNotice />
      <NxFormGroup id="source-control-provider" label="Source Control Management System" isRequired>
        <NxFormSelect
          id="source-control-provider-select"
          onChange={onChangeProvider}
          {...sourceControl?.provider.rscValue}
          className="iq-source-control-provider-select"
          validatable
        >
          <ScmProviderOptions />
        </NxFormSelect>
      </NxFormGroup>
      {showGithubAppAuth && (
        <GitHubAppAuthenticationMethod
          sourceControl={sourceControl}
          setValue={setValue}
          areFieldsDisabled={!sourceControl?.provider.rscValue.value}
          onChangeToken={onChangeToken}
          isGithubAppAuthenticationEnabled={isGithubAppAuthenticationEnabled}
        />
      )}
      {/* Only show credentials section for non-GitHub providers or when GitHub App auth is disabled */}
      {!showGithubAppAuth && (
        <NxFormRow>
          {providerNeedsUsername(sourceControl, serverSourceControl) && (
            <NxFormGroup label="Username" isRequired>
              <NxTextInput
                id="source-control-username"
                onChange={onChangeUsername}
                {...sourceControl?.username.rscValue}
                disabled={!sourceControl?.provider.rscValue.value}
                validatable
                autoComplete="off"
              />
            </NxFormGroup>
          )}
          <NxFormGroup label="Access Token" type="password" isRequired>
            <NxTextInput
              id="source-control-token"
              onChange={onChangeToken}
              {...sourceControl?.token.rscValue}
              disabled={!sourceControl?.provider.rscValue.value}
              type="password"
              autoComplete="new-password"
              validatable
            />
          </NxFormGroup>
        </NxFormRow>
      )}
      {/* Show username separately for GitHub when using GitHub App auth */}
      {showGithubAppAuth && providerNeedsUsername(sourceControl, serverSourceControl) && (
        <NxFormGroup label="Username" isRequired>
          <NxTextInput
            id="source-control-username"
            onChange={onChangeUsername}
            {...sourceControl?.username.rscValue}
            disabled={!sourceControl?.provider.rscValue.value}
            validatable
            autoComplete="off"
          />
        </NxFormGroup>
      )}
      {/* Unsupported for some licenses */}
      <NxTooltip title={!isAutomationSupported ? 'This feature is not supported by your license' : ''}>
        <NxFormGroup
          id="source-control-default-branch"
          label="Default Branch"
          sublabel={DEFAULT_BRANCH_SUBLABEL}
          isRequired
        >
          <NxTextInput
            id="editor-source-control-branch"
            onChange={onChangeBranch}
            {...sourceControl?.baseBranch.rscValue}
            disabled={!sourceControl?.provider.rscValue.value || !isAutomationSupported}
            validatable
          />
        </NxFormGroup>
      </NxTooltip>

      {sourceControlOptions.map(({ id, title, description, optionName }) =>
        mapSourceControlOptionToToggle(id, title, description, optionName)
      )}
    </NxStatefulForm>
  );
};

export default RootSourceControlConfiguration;
