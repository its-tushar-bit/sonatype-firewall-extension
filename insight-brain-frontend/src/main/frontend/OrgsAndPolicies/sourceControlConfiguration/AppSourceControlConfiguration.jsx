/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxFieldset,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxFormRow,
  NxFormSelect,
  NxRadio,
  NxStatefulForm,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import SourceControlInheritedInput from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlInheritedInput';
import TestConfigurationResults from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/TestConfigurationResults';
import TestConfigurationButton from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/TestConfigurationButton';
import {
  getValidationMessage,
  isAccessTokenRequiredOnNode,
  providerNeedsUsername,
  PROVIDERS_WITH_USERNAME,
  SCM_FEATURE_UNSUPPORTED_MESSAGE,
  DEFAULT_BRANCH_SUBLABEL,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectSourceControlConfigurationSlice,
  selectValidationError,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import {
  selectIsAutomationSupported,
  selectTenantScmOptionsTypes,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import ScmProviderOptions from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ScmProviderOptions';

const AppSourceControlConfiguration = () => {
  const dispatch = useDispatch();

  const {
    scmConfigValidation: { loading: configValidationLoading },
    formLoading,
    loadError,
    submitError,
    submitMaskState,
    isDirty,
    isRepoUrlDirty,
    sourceControl,
    serverSourceControl,
  } = useSelector(selectSourceControlConfigurationSlice);

  const validationError = useSelector(selectValidationError);
  const isApp = useSelector(selectIsApplication);
  const isAutomationSupported = useSelector(selectIsAutomationSupported);
  const sourceControlOptions = useSelector(selectTenantScmOptionsTypes);

  const doLoad = () => dispatch(actions.load());
  const showResetModal = () => dispatch(actions.showResetModal());
  const showConfirmUpdateModal = () => dispatch(actions.showConfirmUpdateModal());
  const onSave = () => dispatch(actions.save());
  const onChangeProvider = (event) => dispatch(actions.setProvider(event.target.value));
  const onChangeRepositoryUrl = (val) => dispatch(actions.setRepositoryUrl(val));
  const onChangeUsername = (val) => dispatch(actions.setUsername(val));
  const onChangeToken = (val) => dispatch(actions.setToken(val));
  const onChangeBranch = (val) => dispatch(actions.setBaseBranch(val));
  const setValue = (property, val) => dispatch(actions.setValue({ property, val }));
  const setIsInherited = (property, val) => dispatch(actions.setIsInherited({ property, val }));
  const shouldShowConfirmationOnUpdate = () => sourceControl?.id && isRepoUrlDirty;

  const onChangeCredentialsInherited = (val) => {
    const isUserNameNeeded = PROVIDERS_WITH_USERNAME.includes(
      sourceControl?.provider.isInherited
        ? sourceControl?.provider.parentValue.value
        : sourceControl?.provider.rscValue.value
    );
    // This trigger the validation when the credentials are not inherited
    if (!val) {
      onChangeToken(sourceControl?.token.rscValue.value);
      if (isUserNameNeeded) {
        onChangeUsername(sourceControl?.username.rscValue.value);
      }
    }
    setIsInherited('token', val);
    if (isUserNameNeeded) {
      setIsInherited('username', val);
    }
  };

  const credentialsDisabledMessage = providerNeedsUsername(sourceControl, serverSourceControl)
    ? 'Username and password cannot be inherited. No inheritable username defined.'
    : 'Access token cannot be inherited. No inheritable access token defined.';

  const additionalFooterBtns = (
    <>
      <NxButton
        id="reset-source-control-button"
        variant="tertiary"
        type="button"
        disabled={!sourceControl?.id}
        onClick={showResetModal}
      >
        Reset
      </NxButton>
      <TestConfigurationButton isDisabled={isDirty || configValidationLoading} />
    </>
  );

  const areFieldsDisabled =
    (!sourceControl?.provider.isInherited && !sourceControl?.provider.rscValue.value) ||
    (sourceControl?.provider.isInherited && !sourceControl?.provider.parentValue.value);

  const isUserNameDisabled =
    areFieldsDisabled || (sourceControl?.username.isInherited && sourceControl?.provider.isInherited);

  const isTokenDisabled =
    areFieldsDisabled || (sourceControl?.token.isInherited && sourceControl?.provider.isInherited);

  const credentialsLabel = !sourceControl?.provider.isInherited
    ? ''
    : providerNeedsUsername(sourceControl, serverSourceControl)
    ? 'Credentials'
    : 'Access Token';

  return (
    <NxStatefulForm
      onSubmit={shouldShowConfirmationOnUpdate() ? showConfirmUpdateModal : onSave}
      doLoad={doLoad}
      loading={formLoading}
      loadError={loadError}
      validationErrors={getValidationMessage(isDirty, validationError)}
      submitMaskState={submitMaskState}
      submitError={submitError}
      submitBtnText="Update"
      additionalFooterBtns={additionalFooterBtns}
    >
      <NxForm.RequiredFieldNotice />
      <NxFormGroup id="repository-url-row" label="Repository Clone URL" isRequired>
        <NxTextInput
          id="editor-source-control-url"
          onChange={onChangeRepositoryUrl}
          {...sourceControl?.repositoryUrl}
          validatable
        />
      </NxFormGroup>
      <NxFieldset
        id="editor-source-control-provider"
        label="Source Control Management System"
        isRequired={!sourceControl?.provider.isInherited}
      >
        <NxRadio
          name="provider"
          value={'Inherit'}
          onChange={() => setIsInherited('provider', true)}
          isChecked={sourceControl?.provider.isInherited}
        >
          {sourceControl?.provider.parentName
            ? `Inherit from ${sourceControl?.provider.parentName}`
            : 'Inherit (Not Configured)'}
        </NxRadio>
        <NxRadio
          name="provider"
          value={'Override'}
          onChange={() => setIsInherited('provider', false)}
          isChecked={!sourceControl?.provider.isInherited}
        >
          Override
        </NxRadio>
        <NxFormSelect
          id="source-control-provider-select"
          onChange={onChangeProvider}
          {...(sourceControl?.provider.isInherited
            ? sourceControl?.provider.parentValue
            : sourceControl?.provider.rscValue)}
          className="iq-source-control-provider-select"
          disabled={sourceControl?.provider.isInherited}
          validatable
        >
          <ScmProviderOptions />
        </NxFormSelect>
      </NxFieldset>
      <NxFieldset
        id="editor-source-control-token"
        label={credentialsLabel}
        disabled={areFieldsDisabled}
        isRequired={!sourceControl?.token.isInherited && sourceControl?.provider.isInherited}
      >
        {sourceControl?.provider.isInherited && (
          <>
            <NxTooltip
              title={
                isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp) ? credentialsDisabledMessage : ''
              }
            >
              <NxRadio
                name="Credentials"
                value="Inherit"
                onChange={() => onChangeCredentialsInherited(true)}
                isChecked={sourceControl?.token.isInherited}
                disabled={isAccessTokenRequiredOnNode(sourceControl, serverSourceControl, isApp) || areFieldsDisabled}
              >
                {sourceControl?.token.parentName
                  ? `Inherit from ${sourceControl?.token.parentName}`
                  : 'Inherit (Not Configured)'}
              </NxRadio>
            </NxTooltip>
            <NxRadio
              name="Credentials"
              value="Override"
              onChange={() => onChangeCredentialsInherited(false)}
              isChecked={!sourceControl?.token.isInherited}
              disabled={areFieldsDisabled}
            >
              Override
            </NxRadio>
          </>
        )}
        <NxFormRow>
          {providerNeedsUsername(sourceControl, serverSourceControl) && (
            <NxFormGroup label="Username" isRequired>
              <NxTextInput
                id="source-control-username"
                onChange={onChangeUsername}
                {...(sourceControl?.username.isInherited && sourceControl?.provider.isInherited
                  ? sourceControl?.username.parentValue
                  : sourceControl?.username.rscValue)}
                disabled={isUserNameDisabled}
                validatable
                data-testid="username-input"
                autocomplete="off"
              />
            </NxFormGroup>
          )}
          <NxFormGroup
            label={
              !sourceControl?.provider.isInherited || providerNeedsUsername(sourceControl, serverSourceControl)
                ? 'Access Token'
                : ''
            }
            type="password"
            className={`${sourceControl?.provider.isInherited ? 'iq-source-control-token' : ''}`}
            isRequired
          >
            <NxTextInput
              id="source-control-token"
              onChange={onChangeToken}
              {...(sourceControl?.token.isInherited && sourceControl?.provider.isInherited
                ? sourceControl?.token.parentValue
                : sourceControl?.token.rscValue)}
              disabled={isTokenDisabled}
              type="password"
              validatable
              data-testid="token-input"
              autocomplete="new-password"
            />
          </NxFormGroup>
        </NxFormRow>
      </NxFieldset>
      {/* Unsupported for some licenses */}
      <NxTooltip title={!isAutomationSupported ? SCM_FEATURE_UNSUPPORTED_MESSAGE : ''}>
        <NxFieldset
          id="source-control-default-branch"
          label={
            <>
              Default branch{' '}
              <NxTooltip title={isAutomationSupported ? DEFAULT_BRANCH_SUBLABEL : ''}>
                <NxFontAwesomeIcon icon={faQuestionCircle} />
              </NxTooltip>
            </>
          }
          disabled={areFieldsDisabled || !isAutomationSupported}
          isRequired={!sourceControl?.baseBranch.isInherited && !areFieldsDisabled}
        >
          <NxRadio
            name="baseBranch"
            value="Inherit"
            onChange={() => setIsInherited('baseBranch', true)}
            isChecked={sourceControl?.baseBranch.isInherited}
            disabled={areFieldsDisabled || !isAutomationSupported}
          >
            {sourceControl?.baseBranch.parentName
              ? `Inherit from ${sourceControl?.baseBranch.parentName}`
              : 'Inherit (Not Configured)'}
          </NxRadio>
          <NxRadio
            name="baseBranch"
            value="Override"
            onChange={() => setIsInherited('baseBranch', false)}
            isChecked={!sourceControl?.baseBranch.isInherited}
            disabled={areFieldsDisabled || !isAutomationSupported}
          >
            Override
          </NxRadio>
          <NxFormGroup label="" className="iq-source-control-base-branch" isRequired>
            <NxTextInput
              id="editor-source-control-branch"
              onChange={onChangeBranch}
              {...(sourceControl?.baseBranch.isInherited
                ? sourceControl?.baseBranch.parentValue
                : sourceControl?.baseBranch.rscValue)}
              disabled={sourceControl?.baseBranch.isInherited || !isAutomationSupported}
              validatable
            />
          </NxFormGroup>
        </NxFieldset>
      </NxTooltip>
      {sourceControlOptions.map(({ id, title, description, optionName }) => {
        return (
          <SourceControlInheritedInput
            id={id}
            sourceControl={sourceControl}
            optionName={optionName}
            title={title}
            description={description}
            onChange={(val) => setValue(optionName, val === null ? val : val === 'Enabled')}
            type="boolean"
            key={id}
            isDisabled={!isAutomationSupported && optionName !== 'sshEnabled'}
          />
        );
      })}

      <TestConfigurationResults />
    </NxStatefulForm>
  );
};

export default AppSourceControlConfiguration;
