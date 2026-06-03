/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */

import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { faTrashAlt } from '@fortawesome/pro-regular-svg-icons';
import {
  hasValidationErrors,
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxH2,
  NxSuccessAlert,
  NxModal,
  NxP,
  NxPageMain,
  NxStatefulForm,
  NxTextInput,
  NxTextLink,
  NxTile,
  NxWarningAlert,
  NxLoadError,
  NxStatefulFilterDropdown,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { reject, isNil } from 'ramda';
import LoadError from '../../react/LoadError';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import classnames from 'classnames';
import ZscalerConfigLimits from './ZscalerConfigLimits';

import './ZscalerConfig.scss';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

const AUTH_ERROR_MESSAGE =
  'It appears you do not have permission to access this page. ' +
  'If you believe this to be incorrect please contact your administrator.';

const TEST_CONFIG_ERROR_TITLE = 'Test Zscaler configuration failed.';

const TEST_CONFIG_SUCCESS_MESSAGE = 'Connection to Zscaler successful.';

const REQUIRED_DETAILS_MESSAGE =
  'Username, Password, Hostname, Zscaler API Key, Configured format and End User License Agreement are required details.';

const REQUIRED_DETAILS_TEST_CONFIG_MESSAGE = 'Username, Password, Hostname and Zscaler API Key are required details.';

const PASSWORD_REENTER_MESSAGE = 'Password must be re-entered when any fields are modified.';

const PASSWORD_REENTER_TEST_CONFIG_MESSAGE = 'Password must be re-entered for testing configuration.';

const TOOLTIP_TITLE =
  'URLs pushed to Zscaler are based on official package sources. Limiting formats ' +
  'reduces noise and optimizes security rules. Dependencies from unofficial or custom sources are not fully protected ' +
  'by this integration.';

export default function ZscalerConfig(props) {
  const {
      load,
      save,
      del,
      resetForm,
      setUsername,
      setPassword,
      setHostname,
      setApiKey,
      setShowDeleteModal,
      testConfig,
      setEulaCheckbox,
      loadLimits,
      setConfiguredFormats,
    } = props,
    {
      loading,
      submitMaskState,
      submitMaskMessage,
      hasAllRequiredData,
      hasAllRequiredDataForTestConfig,
      isDirty,
      loadError: loadErrorProp,
      saveError,
      deleteError,
      testConfigError,
      serverData,
      showDeleteModal,
      hostnameState,
      usernameState,
      passwordState,
      apiKeyState,
      eulaState,
      configuredFormatState,
      mustReenterPassword,
      testConfigSuccess,
      isAuthorized,
      zscalerConfigLimitsState,
    } = props,
    loadError = isAuthorized ? loadErrorProp : AUTH_ERROR_MESSAGE;

  // Fetch ZScaler Configuration when page is opened
  useEffect(() => {
    load();
  }, []);

  function warningMessage() {
    return 'This action cannot be undone. Are you sure you want to delete this configuration?';
  }

  function field(
    fieldState,
    onChange,
    placeholder,
    id,
    label,
    type = 'text',
    sublabel = null,
    onFocus = () => {},
    optional = false,
    validatable = true
  ) {
    // The autoComplete setting is a hack to stop chrome autofilling the user's username and password
    // https://stackoverflow.com/a/55292734
    return (
      <NxFormGroup label={label} sublabel={sublabel} isRequired={!optional}>
        <NxTextInput
          {...fieldState}
          {...{ onChange, onFocus, placeholder, id, type, validatable }}
          className="nx-text-input--long"
          inputAttributes={{ autoComplete: 'new-password' }}
        />
      </NxFormGroup>
    );
  }

  const modal = (
    <NxModal id="zscaler-config-delete-modal" onClose={() => setShowDeleteModal(false)}>
      <NxStatefulForm
        onSubmit={del}
        onCancel={() => setShowDeleteModal(false)}
        submitBtnText="OK"
        submitError={deleteError}
      >
        <NxModal.Header>
          <NxH2>Delete Zscaler Configuration?</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            <span>{warningMessage()}</span>
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );

  const formValidationErrors = reject(isNil, [
    mustReenterPassword ? PASSWORD_REENTER_MESSAGE : null,
    hasAllRequiredData ? null : REQUIRED_DETAILS_MESSAGE,
    isDirty ? null : MSG_NO_CHANGES_TO_SAVE,
  ]);

  const testConfigTooltip = !hasAllRequiredDataForTestConfig
    ? REQUIRED_DETAILS_TEST_CONFIG_MESSAGE
    : passwordState.isPristine
    ? PASSWORD_REENTER_TEST_CONFIG_MESSAGE
    : null;

  /*
   * Do not pass the submitError to the form when there are validation errors.
   * This is a workaround for a likely bug in RSC where it prefers to show the submit error over the
   * validation error when both are present, while it really should probably do the opposite. If the
   * submit error is displayed when there are also validation errors, the Retry button in the submit
   * error does nothing
   */
  const submitError = hasValidationErrors(formValidationErrors) ? null : saveError;

  const passwordSublabel = hasAllRequiredData && mustReenterPassword ? PASSWORD_REENTER_MESSAGE : null;

  const isTestConfigEnabled = hasAllRequiredDataForTestConfig && !passwordState.isPristine;

  const apiKeySublabel = (
    <span>
      Generate a Zscaler API Key through the Admin Portal under API Management.
      <br />
      <NxTextLink
        external
        id="zscaler-api-key-link"
        href="https://links.sonatype.com/products/nxrm3/docs/zscaler/api-keys"
      >
        Learn how to retrieve Zscaler API Key
      </NxTextLink>
    </span>
  );

  const learnMoreAboutZscalerLink = (
    <NxTextLink
      external
      id="zscaler-doc-link"
      href="https://links.sonatype.com/products/nxrm3/docs/zscaler/configuration"
    >
      Learn more about the Zscaler integration
    </NxTextLink>
  );

  const licenseTermsLink = (
    <NxTextLink
      external
      id="zscaler-eula-link"
      href="https://links.sonatype.com/products/firewall/docs/zscaler/zscaler-eula"
    >
      License Terms.
    </NxTextLink>
  );

  const options = [
    { id: 'mavenFormatEnabled', displayName: 'Maven' },
    { id: 'npmFormatEnabled', displayName: 'Npm' },
    { id: 'nugetFormatEnabled', displayName: 'Nuget' },
    { id: 'pypiFormatEnabled', displayName: 'Pypi' },
  ];

  function testConfigHandler() {
    if (isTestConfigEnabled) {
      testConfig();
    }
  }

  const additionalBtns = (
    <>
      <NxButton
        type="button"
        id="zscaler-config-delete"
        onClick={() => setShowDeleteModal(true)}
        disabled={!serverData}
      >
        <NxFontAwesomeIcon icon={faTrashAlt} />
        <span>Delete Configuration</span>
      </NxButton>
      <NxButton type="button" id="zscaler-config-cancel" onClick={resetForm} disabled={!isDirty}>
        Cancel
      </NxButton>
      <NxButton
        title={testConfigTooltip}
        type="button"
        id="zscaler-config-test"
        onClick={testConfigHandler}
        className={classnames({ disabled: !isTestConfigEnabled })}
      >
        <span>Test Configuration</span>
      </NxButton>
    </>
  );

  return (
    <NxPageMain id="zscaler-config-page-container">
      {isAuthorized && (
        <ZscalerConfigLimits
          zscalerConfigLimitsState={zscalerConfigLimitsState}
          loadLimits={loadLimits}
          serverData={serverData}
        />
      )}
      <NxTile id="zscaler-configuration">
        <NxStatefulForm
          loading={loading}
          doLoad={load}
          loadError={loadError}
          onSubmit={save}
          submitBtnText={serverData ? 'Update' : 'Save'}
          submitError={submitError}
          submitBtnClasses="zscaler-submit-button"
          validationErrors={submitMaskMessage !== 'Deleting' ? formValidationErrors : null}
          // If there is a validationError alert, it's cleared on "Delete Configuration"
          submitMaskState={submitMaskState}
          submitMaskMessage={submitMaskMessage}
          additionalFooterBtns={additionalBtns}
        >
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2 id="zscaler-config-header">Zscaler Configuration</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              To protect users at the network level, integrate Sonatype data with your Zscaler infrastructure.
              <br />
              {learnMoreAboutZscalerLink}
            </NxP>
            {/* Input Fields */}
            {field(usernameState, setUsername, 'user', 'zscaler-config-username', 'Username')}
            {field(
              passwordState,
              setPassword,
              null,
              'zscaler-config-password',
              'Password',
              'password',
              passwordSublabel,
              (evt) => {
                evt.target.select();
              }
            )}
            {field(
              hostnameState,
              setHostname,
              'https://zsapi.zscalertwo.net',
              'zscaler-config-hostname',
              'Hostname',
              'text',
              'Enter the base ZScaler URL (e.g., https://zsapi.zscalertwo.net)'
            )}
            {field(
              apiKeyState,
              setApiKey,
              '465',
              'zscaler-config-api-key',
              'Zscaler API Key',
              'password',
              apiKeySublabel
            )}
            <NxFieldset
              id="zscaler-config-format"
              label="Configured Formats"
              sublabel="Limit the number of urls pushed to Zscaler."
              isRequired={true}
              isPristine={configuredFormatState.isPristine}
              validationErrors={configuredFormatState.validationErrors}
            >
              <NxStatefulFilterDropdown
                placeholder="Formats"
                options={options}
                selectedIds={configuredFormatState.formats}
                onChange={setConfiguredFormats}
                showReset={false}
              ></NxStatefulFilterDropdown>
              <NxTooltip title={TOOLTIP_TITLE}>
                <NxFontAwesomeIcon
                  className="config-format-tooltip__icon"
                  data-testid="tooltip-icon"
                  icon={faInfoCircle}
                />
              </NxTooltip>
            </NxFieldset>
            <NxFieldset
              label="End User License Agreement"
              isRequired={true}
              isPristine={eulaState.isPristine}
              validationErrors={eulaState.validationErrors}
            >
              <NxCheckbox
                isChecked={eulaState.value}
                onChange={setEulaCheckbox}
                disabled={eulaState.disabled}
                id="zscaler-eula-checkbox"
              >
                By clicking &quot;Save&quot; below, I hereby acknowledge and agree that
                <br />
                access to and use of Sonatype&apos;s Zscaler integration is subject to
                <br />
                and governed by these {licenseTermsLink}
              </NxCheckbox>
            </NxFieldset>
            {testConfigSuccess && <NxSuccessAlert>{TEST_CONFIG_SUCCESS_MESSAGE}</NxSuccessAlert>}
            {testConfigError && (
              <NxLoadError titleMessage={TEST_CONFIG_ERROR_TITLE} error={testConfigError} retryHandler={testConfig} />
            )}
          </NxTile.Content>
        </NxStatefulForm>
        {showDeleteModal && modal}
      </NxTile>
    </NxPageMain>
  );
}

const textInputPropType = PropTypes.shape({
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string]),
});

const checkboxPropType = PropTypes.shape({
  value: PropTypes.bool.isRequired,
  isPristine: PropTypes.bool.isRequired,
  disabled: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string]),
});

ZscalerConfig.propTypes = {
  load: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
  del: PropTypes.func.isRequired,
  resetForm: PropTypes.func.isRequired,
  setUsername: PropTypes.func.isRequired,
  setPassword: PropTypes.func.isRequired,
  setHostname: PropTypes.func.isRequired,
  setApiKey: PropTypes.func.isRequired,
  setShowDeleteModal: PropTypes.func.isRequired,
  setEulaCheckbox: PropTypes.func.isRequired,
  setConfiguredFormats: PropTypes.func.isRequired,
  loadLimits: PropTypes.func.isRequired,
  testConfig: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  submitMaskState: PropTypes.bool,
  submitMaskMessage: PropTypes.string,
  hostnameState: textInputPropType.isRequired,
  apiKeyState: textInputPropType.isRequired,
  usernameState: textInputPropType.isRequired,
  passwordState: textInputPropType.isRequired,
  hasAllRequiredData: PropTypes.bool.isRequired,
  isDirty: PropTypes.bool.isRequired,
  isValid: PropTypes.bool.isRequired,
  loadError: LoadError.propTypes.error,
  saveError: LoadError.propTypes.error,
  deleteError: LoadError.propTypes.error,
  testConfigError: LoadError.propTypes.error,
  testConfigSuccess: PropTypes.bool.isRequired,
  serverData: PropTypes.any,
  zscalerConfigLimitsState: PropTypes.shape({
    loading: PropTypes.bool,
    error: PropTypes.any,
    limits: PropTypes.object,
  }).isRequired,
  showDeleteModal: PropTypes.bool.isRequired,
  mustReenterPassword: PropTypes.bool.isRequired,
  isAuthorized: PropTypes.bool.isRequired,
  eulaState: checkboxPropType.isRequired,
  configuredFormatState: PropTypes.shape({
    formats: PropTypes.instanceOf(Set).isRequired,
    isPristine: PropTypes.bool.isRequired,
    validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string]),
  }).isRequired,
};
