/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef } from 'react';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import {
  NxButton,
  NxTextLink,
  NxFormGroup,
  NxTextInput,
  NxFormSelect,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faTrash, faDownload } from '@fortawesome/pro-solid-svg-icons';
import { uriTemplate } from '../../util/urlUtil';
import SAMLConfigurationDeleteModal from './SAMLConfigurationDeleteModal';

export default function SAMLConfigurationForm({
  configurationValues,
  onCancel,
  onChangeSelect,
  onChange,
  onBlur,
  onSubmit,
  readIdentityProviderMetadataXml,
  isConfigured,
  isSubmitButtonDisabled,
  deleteConfiguration,
  submitState,
  submitMaskError,
  metaDataUrl,
  toggleDeleteModal,
  isDeleteModalShown,
}) {
  const fileInput = useRef(null);

  const removePreviousFile = () => (fileInput.current.value = null);

  const getInputTooltipMessage = (name) => {
    const defaultInputValues = {
      identityProviderName: 'identity provider',
      entityId: uriTemplate`/api/v2/config/saml/metadata`,
      usernameAttributeName: 'username',
      firstNameAttributeName: 'firstName',
      lastNameAttributeName: 'lastName',
      emailAttributeName: 'email',
      groupsAttributeName: 'groups',
    };
    return `If empty will default to "${defaultInputValues[name]}"`;
  };

  const additionalFooterBtns = (
    <>
      <NxButton disabled={!isConfigured} id="saml-delete" onClick={toggleDeleteModal} type="button" variant="tertiary">
        <NxFontAwesomeIcon icon={faTrash} />
        <span>Delete Configuration</span>
      </NxButton>
      <NxButton type="button" id="saml-cancel" onClick={onCancel}>
        Cancel
      </NxButton>
    </>
  );

  const getFormSelect = (name) => (
    <NxFormSelect
      value={configurationValues[name]}
      onChange={(event) => onChangeSelect(event.currentTarget.value, name)}
    >
      <option key="Default" value={'null'}>
        Default
      </option>
      <option key="True" value={'true'}>
        True
      </option>
      <option key="False" value={'false'}>
        False
      </option>
    </NxFormSelect>
  );

  const getInputTextProps = (name, otherProps = {}) => ({
    ...configurationValues[name],
    ...otherProps,
    'aria-required': true,
    onChange: (value) => onChange(value, name),
    onBlur: () => onBlur(name),
    validatable: true,
  });

  const getTextInput = (name, otherProps = {}) => {
    return <NxTextInput {...getInputTextProps(name, otherProps)} />;
  };

  const getDownloadServerMetadataTooltip = (isDisabled) =>
    isDisabled ? '' : 'Nothing to download until a SAML configuration is saved';

  const onLoadXMLButtonClick = () => {
    fileInput.current.click();
  };

  return (
    <>
      <NxStatefulForm
        className="nx-form"
        submitBtnClasses={classNames('iq-saml-configuration-save-button', {
          disabled: isSubmitButtonDisabled,
        })}
        onSubmit={onSubmit}
        additionalFooterBtns={additionalFooterBtns}
        submitBtnText="Save"
        submitMaskState={submitState}
        submitError={submitMaskError}
      >
        <header className="nx-tile-header">
          <hgroup className="nx-tile-header__headings">
            <h2 className="nx-tile-header__title">SAML Configuration</h2>
            {!isConfigured && (
              <h3 className="nx-tile-header__subtitle" id="saml-is-configured">
                * Currently not configured
              </h3>
            )}
          </hgroup>
          <div className="nx-tile__actions">
            <NxTooltip title={getDownloadServerMetadataTooltip(isConfigured)}>
              <a
                href={metaDataUrl}
                role="button"
                download
                className={classNames('iq-saml-configuration-metadata nx-btn nx-btn--tertiary', {
                  disabled: !isConfigured,
                })}
                id="saml-iq-server-metadata"
                aria-disabled={!isConfigured}
              >
                <NxFontAwesomeIcon icon={faDownload} />
                <span>Download IQ Server Metadata</span>
              </a>
            </NxTooltip>
          </div>
        </header>

        <div className="nx-tile-content">
          <p className="nx-p">
            Once configured this will become the default way to sign in to IQ Server. See{' '}
            <NxTextLink
              id="saml-explanation"
              external
              href="http://links.sonatype.com/products/nxiq/doc/saml-integration"
            >
              how to configure SAML integration
            </NxTextLink>{' '}
            between IQ Server and your identity provider. If you{"'"}re experiencing issues with this page, please
            submit feedback{' '}
            <NxTextLink id="saml-feedback-link" external href="http://links.sonatype.com/products/nxiq/feedback/saml">
              here.
            </NxTextLink>
          </p>

          <NxTooltip title={getInputTooltipMessage('identityProviderName')}>
            <NxFormGroup
              label="Identity Provider Name"
              sublabel="This will be displayed at the login screen."
              id="saml-identity-provider-name"
              isRequired
            >
              {getTextInput('identityProviderName', { maxLength: '200' })}
            </NxFormGroup>
          </NxTooltip>

          <div className="nx-form-group">
            <label for="saml-identity-provider-metadata-xml" className="nx-label">
              <span className="nx-label__text">Identity Provider Metadata XML</span>
              <div className="nx-sub-label">
                Your identity provider metadata XML must contain a signing key to use validation. Validation set to
                Default will validate if a signing key is present.
              </div>
            </label>
            <div className="nx-btn-bar iq-saml-configuration-load-xml-button-bar">
              <NxButton onClick={onLoadXMLButtonClick} type="button" variant="tertiary">
                Load XML File
              </NxButton>
              <input
                className="iq-input-file--hidden"
                type="file"
                id="saml-identity-provider-metadata-xml-load"
                accept=".xml"
                tabIndex="-1"
                ref={fileInput}
                onInput={readIdentityProviderMetadataXml}
                onClick={removePreviousFile}
              />
              <span>or paste your XML below</span>
            </div>
            <NxTextInput
              className="nx-text-input--long"
              id="saml-identity-provider-metadata-xml"
              name="identityProviderMetadataXml"
              type="textarea"
              {...getInputTextProps('identityProviderMetadataXml')}
            />
          </div>

          <div className="nx-form-row">
            <NxFormGroup label="Validate Response Signature" id="select-validate-response-signature" isRequired>
              {getFormSelect('validateResponseSignature')}
            </NxFormGroup>
            <NxFormGroup label="Validate Assertion Signature" id="select-validate-assertion-signature" isRequired>
              {getFormSelect('validateAssertionSignature')}
            </NxFormGroup>
          </div>
          <div className="nx-form-row">
            <NxTooltip title={getInputTooltipMessage('entityId')}>
              <NxFormGroup label="Entity ID" id="saml-entity-id" isRequired>
                {getTextInput('entityId')}
              </NxFormGroup>
            </NxTooltip>
            <NxTooltip title={getInputTooltipMessage('usernameAttributeName')}>
              <NxFormGroup label="Username Attribute" id="saml-username-attribute-name" isRequired>
                {getTextInput('usernameAttributeName')}
              </NxFormGroup>
            </NxTooltip>
          </div>
          <div className="nx-form-row">
            <NxTooltip title={getInputTooltipMessage('firstNameAttributeName')}>
              <NxFormGroup label="First Name Attribute" id="saml-first-name-attribute-name" isRequired>
                {getTextInput('firstNameAttributeName')}
              </NxFormGroup>
            </NxTooltip>
            <NxTooltip title={getInputTooltipMessage('lastNameAttributeName')}>
              <NxFormGroup label="Last Name Attribute" id="saml-last-name-attribute-name" isRequired>
                {getTextInput('lastNameAttributeName')}
              </NxFormGroup>
            </NxTooltip>
          </div>
          <div className="nx-form-row">
            <NxTooltip title={getInputTooltipMessage('emailAttributeName')}>
              <NxFormGroup label="Email Attribute" id="saml-email-attribute-name" isRequired>
                {getTextInput('emailAttributeName')}
              </NxFormGroup>
            </NxTooltip>
            <NxTooltip title={getInputTooltipMessage('groupsAttributeName')}>
              <NxFormGroup label="Groups Attribute" id="saml-groups-attribute-name" isRequired>
                {getTextInput('groupsAttributeName')}
              </NxFormGroup>
            </NxTooltip>
          </div>
        </div>
      </NxStatefulForm>
      {isDeleteModalShown && (
        <SAMLConfigurationDeleteModal deleteConfiguration={deleteConfiguration} toggleDeleteModal={toggleDeleteModal} />
      )}
    </>
  );
}

SAMLConfigurationForm.propTypes = {
  onCancel: PropTypes.func.isRequired,
  onChangeSelect: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  onBlur: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  deleteConfiguration: PropTypes.func.isRequired,
  readIdentityProviderMetadataXml: PropTypes.func.isRequired,
  toggleDeleteModal: PropTypes.func.isRequired,
  isConfigured: PropTypes.bool,
  submitState: PropTypes.bool,
  isSubmitButtonDisabled: PropTypes.bool,
  submitMaskError: PropTypes.string,
  metaDataUrl: PropTypes.string,
  isDeleteModalShown: PropTypes.bool,
  configurationValues: PropTypes.shape({
    identityProviderName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    entityId: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    usernameAttributeName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    firstNameAttributeName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    lastNameAttributeName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    emailAttributeName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    groupsAttributeName: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    identityProviderMetadataXml: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    validateResponseSignature: PropTypes.string,
    validateAssertionSignature: PropTypes.string,
  }).isRequired,
};
