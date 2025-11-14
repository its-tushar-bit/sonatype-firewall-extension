/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classNames from 'classnames';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxH2,
  NxStatefulForm,
  NxTextInput,
} from '@sonatype/react-shared-components';
import { faTrash } from '@fortawesome/pro-solid-svg-icons';
import OidcConfigurationDeleteModal from './OidcConfigurationDeleteModal';

export default function OidcConfigurationForm({
  configurationValues,
  onCancel,
  onChange,
  onBlur,
  onSubmit,
  isConfigured,
  isSubmitButtonDisabled,
  deleteConfiguration,
  submitState,
  submitMaskError,
  toggleDeleteModal,
  isDeleteModalShown,
}) {
  const getInputTextProps = (name, isRequired = true, otherProps = {}) => ({
    ...configurationValues[name],
    ...otherProps,
    'aria-required': isRequired,
    onChange: (value) => onChange(value, name),
    onBlur: () => onBlur(name),
    validatable: true,
  });

  const getTextInput = (name, isRequired = true, otherProps = {}) => {
    return <NxTextInput {...getInputTextProps(name, isRequired, otherProps)} />;
  };

  const getTextarea = (name, isRequired = true, otherProps = {}) => {
    return (
      <NxTextInput
        {...getInputTextProps(name, isRequired, otherProps)}
        type="textarea"
        className="nx-text-input--long"
      />
    );
  };

  const additionalFooterBtns = (
    <>
      <NxButton disabled={!isConfigured} id="oidc-delete" onClick={toggleDeleteModal} type="button" variant="tertiary">
        <NxFontAwesomeIcon icon={faTrash} />
        <span>Delete Configuration</span>
      </NxButton>
      <NxButton type="button" id="oidc-cancel" onClick={onCancel}>
        Cancel
      </NxButton>
    </>
  );

  return (
    <>
      <NxStatefulForm
        className="nx-form iq-oidc-configuration-form"
        submitBtnClasses={classNames('iq-oidc-configuration-save-button', {
          disabled: isSubmitButtonDisabled,
        })}
        onSubmit={onSubmit}
        additionalFooterBtns={additionalFooterBtns}
        submitBtnText="Save"
        submitMaskState={submitState}
        submitError={submitMaskError}
      >
        <div className="nx-tile-content">
          <section className="iq-oidc-section">
            <NxH2>OIDC Connection Settings</NxH2>
            <hr className="iq-oidc-divider" />

            <NxFormGroup
              label="Client ID"
              sublabel="The OAuth2 Client ID provided by your identity provider"
              id="oidc-client-id"
              isRequired
            >
              {getTextInput('oidcClientId')}
            </NxFormGroup>

            <NxFormGroup
              label="Client Secret"
              sublabel="The OAuth2 Client Secret provided by your identity provider"
              id="oidc-client-secret"
              isRequired
            >
              {getTextInput('oidcClientSecret', true, { type: 'password', autoComplete: 'new-password' })}
            </NxFormGroup>

            <NxFormGroup
              label="IDP Issuer"
              sublabel="The issuer URL for the OIDC provider. This single field is used for both OIDC and OAuth2 JWT validation."
              id="oidc-idp-issuer"
              isRequired
            >
              {getTextInput('oidcIdpIssuer')}
            </NxFormGroup>

            <NxFormGroup
              label="Authorization URL"
              sublabel="The authorization endpoint URL"
              id="oidc-authorization-url"
              isRequired
            >
              {getTextInput('oidcIdpAuthorizationUrl')}
            </NxFormGroup>

            <NxFormGroup label="Token URL" sublabel="The token endpoint URL" id="oidc-token-url" isRequired>
              {getTextInput('oidcIdpTokenUrl')}
            </NxFormGroup>

            <NxFormGroup
              label="Authorization Custom Parameters (JSON)"
              sublabel="Additional custom parameters to send with authorization requests (JSON format)"
              id="oidc-authorization-custom-params"
            >
              {getTextarea('oidcAuthorizationCustomParamsJson', false)}
            </NxFormGroup>

            <NxFormGroup
              label="Token Request Custom Parameters (JSON)"
              sublabel="Additional custom parameters to send with token requests (JSON format)"
              id="oidc-token-request-custom-params"
            >
              {getTextarea('oidcTokenRequestCustomParamsJson', false)}
            </NxFormGroup>
          </section>

          <section className="iq-oidc-section">
            <NxH2>OAuth2 JWT Configuration</NxH2>
            <hr className="iq-oidc-divider" />

            <NxFormGroup
              label="JWKS URL"
              sublabel="The JSON Web Key Set URL for token validation (required if JWKS JSON not provided)"
              id="oauth2-jwks-url"
            >
              {getTextInput('oauth2IdpJwksUrl', false)}
            </NxFormGroup>

            <NxFormGroup
              label="JWS Algorithm"
              sublabel="The signing algorithm used for JWTs (e.g., RS256)"
              id="oauth2-jws-algorithm"
              isRequired
            >
              {getTextInput('oauth2IdpJwsAlgorithm')}
            </NxFormGroup>

            <NxFormGroup
              label="JWKS (JSON)"
              sublabel="Alternatively, paste the JWKS JSON directly instead of using a URL (required if JWKS URL not provided)"
              id="oauth2-jwks"
            >
              {getTextarea('oauth2IdpJwks', false)}
            </NxFormGroup>
          </section>

          <section className="iq-oidc-section">
            <NxH2>User Attribute Mapping</NxH2>
            <hr className="iq-oidc-divider" />

            <NxFormGroup label="Username Claim" id="oauth2-username-claim">
              {getTextInput('oauth2UsernameClaim', false)}
            </NxFormGroup>

            <NxFormGroup label="Email Claim" id="oauth2-email-claim">
              {getTextInput('oauth2EmailClaim', false)}
            </NxFormGroup>

            <NxFormGroup label="First Name Claim" id="oauth2-first-name-claim">
              {getTextInput('oauth2FirstNameClaim', false)}
            </NxFormGroup>

            <NxFormGroup label="Last Name Claim" id="oauth2-last-name-claim">
              {getTextInput('oauth2LastNameClaim', false)}
            </NxFormGroup>

            <NxFormGroup label="Groups Claim" id="oauth2-groups-claim">
              {getTextInput('oauth2GroupsClaim', false)}
            </NxFormGroup>

            <NxFormGroup
              label="Exact Match Claims (JSON)"
              sublabel="Additional claims that must match exactly for authentication (JSON format)"
              id="oauth2-exact-match-claims"
            >
              {getTextarea('oauth2ExactMatchClaimsJson', false)}
            </NxFormGroup>
          </section>
        </div>
      </NxStatefulForm>
      {isDeleteModalShown && (
        <OidcConfigurationDeleteModal deleteConfiguration={deleteConfiguration} toggleDeleteModal={toggleDeleteModal} />
      )}
    </>
  );
}

OidcConfigurationForm.propTypes = {
  onCancel: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  onBlur: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
  deleteConfiguration: PropTypes.func.isRequired,
  toggleDeleteModal: PropTypes.func.isRequired,
  isConfigured: PropTypes.bool,
  submitState: PropTypes.bool,
  isSubmitButtonDisabled: PropTypes.bool,
  submitMaskError: PropTypes.string,
  isDeleteModalShown: PropTypes.bool,
  configurationValues: PropTypes.shape({
    oauth2IdpJwksUrl: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2IdpJwsAlgorithm: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2IdpJwks: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2UsernameClaim: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2FirstNameClaim: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2LastNameClaim: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2EmailClaim: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2GroupsClaim: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oauth2ExactMatchClaimsJson: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcIdpIssuer: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcClientId: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcClientSecret: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcIdpAuthorizationUrl: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcIdpTokenUrl: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcAuthorizationCustomParamsJson: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
    oidcTokenRequestCustomParamsJson: PropTypes.shape({ value: PropTypes.string, isPristine: PropTypes.bool }),
  }).isRequired,
};
