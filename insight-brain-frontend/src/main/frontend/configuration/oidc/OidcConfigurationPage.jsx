/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxTextLink } from '@sonatype/react-shared-components';
import NxLoadWrapper from '../../react/LoadWrapper';
import OidcConfigurationForm from './OidcConfigurationForm';
import { selectOidcConfigurationSlice, selectIsDirty } from './oidcConfigurationSelectors';
import { actions } from './oidcConfigurationSlice';

export default function OidcConfigurationPage() {
  const dispatch = useDispatch();

  const configurationSlice = useSelector(selectOidcConfigurationSlice);
  const isDirty = useSelector(selectIsDirty);

  const {
    configurationValues,
    loadError,
    isLoading,
    submitState,
    isConfigured,
    submitMaskError,
    isDeleteModalShown,
  } = configurationSlice;

  const {
    toggleDeleteModal,
    loadOidcConfiguration,
    updateOidcConfiguration,
    deleteOidcConfiguration,
    onOidcConfigurationValueChange,
    onRestoreConfigurationValues,
    onRestoreConfigurationValue,
  } = actions;

  const onBlur = (name) => {
    if (!configurationValues[name].value) {
      dispatch(onRestoreConfigurationValue(name));
    }
  };

  const onChange = (value, name) => dispatch(onOidcConfigurationValueChange(value, name));

  const onDelete = () => dispatch(deleteOidcConfiguration());

  const onCancel = () => dispatch(onRestoreConfigurationValues());

  const loadConfiguration = () => dispatch(loadOidcConfiguration());

  const onToggleDeleteModal = () => dispatch(toggleDeleteModal());

  // Disable save button only if there are no changes to save
  // Backend will handle validation and return appropriate errors
  const isSubmitButtonDisabled = !isDirty;

  const onSubmit = () => {
    if (isSubmitButtonDisabled) {
      return;
    }
    dispatch(updateOidcConfiguration());
  };

  useEffect(() => {
    loadConfiguration();
  }, []);

  return (
    <main id="oidc-configuration-page" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">OpenID Connect (OIDC) Configuration</h1>
        {!isConfigured && <p className="iq-oidc-subtitle">* Currently not configured</p>}
      </div>

      <p className="nx-p iq-oidc-description">
        Configure OpenID Connect (OIDC) authentication for IQ Server. Once configured, this will become the default way
        to sign in to IQ Server. See{' '}
        <NxTextLink id="oidc-explanation" external href="https://links.sonatype.com/products/nxiq/doc/oidc-integration">
          how to configure OIDC integration
        </NxTextLink>{' '}
        between IQ Server and your identity provider.
      </p>

      <section className="nx-tile">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadConfiguration}>
          {() => (
            <OidcConfigurationForm
              onCancel={onCancel}
              onChange={onChange}
              onBlur={onBlur}
              onSubmit={onSubmit}
              configurationValues={configurationValues}
              isConfigured={isConfigured}
              isSubmitButtonDisabled={isSubmitButtonDisabled}
              deleteConfiguration={onDelete}
              submitState={submitState}
              submitMaskError={submitMaskError}
              toggleDeleteModal={onToggleDeleteModal}
              isDeleteModalShown={isDeleteModalShown}
            />
          )}
        </NxLoadWrapper>
      </section>
    </main>
  );
}
