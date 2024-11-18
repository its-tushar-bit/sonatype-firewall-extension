/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
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
import ScmProviderOptions from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ScmProviderOptions';
import RenderMarkdown from 'MainRoot/react/RenderMarkdown';

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

  const doLoad = () => dispatch(actions.load());
  const save = () => dispatch(actions.save());
  const showResetModal = () => dispatch(actions.showResetModal());
  const onChangeProvider = (event) => dispatch(actions.setProvider(event.target.value));
  const onChangeUsername = (val) => dispatch(actions.setUsername(val));
  const onChangeToken = (val) => dispatch(actions.setToken(val));
  const onChangeBranch = (val) => dispatch(actions.setBaseBranch(val));
  const toggleValue = (property) => dispatch(actions.toggleValue(property));

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
      <NxFormRow>
        {providerNeedsUsername(sourceControl, serverSourceControl) && (
          <NxFormGroup label="Username" isRequired>
            <NxTextInput
              id="source-control-username"
              onChange={onChangeUsername}
              {...sourceControl?.username.rscValue}
              disabled={!sourceControl?.provider.rscValue.value}
              validatable
              autocomplete="off"
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
            autocomplete="new-password"
            validatable
          />
        </NxFormGroup>
      </NxFormRow>
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

      {sourceControlOptions.map(({ id, title, description, optionName }) => {
        return (
          <NxTooltip
            key={id}
            title={
              !isAutomationSupported && optionName !== 'sshEnabled'
                ? 'This feature is not supported by your license'
                : ''
            }
          >
            <NxToggle
              key={id}
              id={id}
              className="iq-source-control-toggle"
              onChange={() => toggleValue(optionName)}
              isChecked={sourceControl?.[optionName].value}
              disabled={
                !sourceControl?.provider.rscValue.value || (!isAutomationSupported && optionName !== 'sshEnabled')
              }
            >
              <span className="iq-source-control-toggle__title">{title}</span>
              <RenderMarkdown className="iq-source-control-toggle__text">{description}</RenderMarkdown>
            </NxToggle>
          </NxTooltip>
        );
      })}
    </NxStatefulForm>
  );
};

export default RootSourceControlConfiguration;
