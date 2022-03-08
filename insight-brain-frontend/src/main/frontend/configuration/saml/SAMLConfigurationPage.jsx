/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import NxLoadWrapper from '../../react/LoadWrapper';
import SAMLConfigurationForm from './SAMLConfigurationForm';
import { selectSAMLConfigurationSlice } from './samlConfigurationSelectors';
import { actions } from './samlConfigurationSlice';
import { getSamlMetadataUrl } from '../../util/CLMLocation';

export default function SAMLConfigurationPage() {
  const dispatch = useDispatch();

  const configurationSlice = useSelector(selectSAMLConfigurationSlice);

  const {
    configurationValues,
    loadError,
    isLoading,
    submitState,
    isConfigured,
    submitMaskError,
    loadedConfigurationValues,
    isDeleteModalShown,
  } = configurationSlice;

  const {
    toggleDeleteModal,
    loadSAMLConfiguration,
    updateSAMLConfiguration,
    deleteSAMLConfiguration,
    onSAMLConfigurationValueChange,
    onSAMLConfigurationSelectValueChange,
    onRestoreConfigurationValues,
    onRestoreConfigurationValue,
  } = actions;

  const onBlur = (name) => {
    if (!configurationValues[name].value) {
      dispatch(onRestoreConfigurationValue(name));
    }
  };

  const onChange = (value, name) => dispatch(onSAMLConfigurationValueChange(value, name));

  const onChangeSelect = (value, name) => dispatch(onSAMLConfigurationSelectValueChange(value, name));

  const onDelete = () => dispatch(deleteSAMLConfiguration());

  const onCancel = () => dispatch(onRestoreConfigurationValues());

  const loadConfiguration = () => dispatch(loadSAMLConfiguration());

  const onToggleDeleteModal = () => dispatch(toggleDeleteModal());

  const getFileReader = () => {
    let fileReader = new FileReader();
    fileReader.addEventListener('load', () => {
      onChange(fileReader.result, 'identityProviderMetadataXml');
    });
    return fileReader;
  };

  const readIdentityProviderMetadataXml = (event) => {
    const input = event.target;
    const file = input.files[0];
    getFileReader().readAsText(file);
  };

  const isSubmitButtonDisabled = !configurationValues.identityProviderMetadataXml.value;

  const onSubmit = () => {
    if (isSubmitButtonDisabled) {
      return;
    }
    dispatch(updateSAMLConfiguration());
  };

  useEffect(() => {
    loadConfiguration();
  }, []);

  return (
    <main id="saml-configuration-page" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Security Assertion Markup Language (SAML)</h1>
      </div>

      <section className="nx-tile">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadConfiguration}>
          {() => (
            <SAMLConfigurationForm
              onCancel={onCancel}
              onChangeSelect={onChangeSelect}
              onChange={onChange}
              onBlur={onBlur}
              onSubmit={onSubmit}
              readIdentityProviderMetadataXml={readIdentityProviderMetadataXml}
              configurationValues={configurationValues}
              isConfigured={isConfigured}
              isSubmitButtonDisabled={isSubmitButtonDisabled}
              deleteConfiguration={onDelete}
              submitState={submitState}
              submitMaskError={submitMaskError}
              metaDataUrl={loadedConfigurationValues ? getSamlMetadataUrl() : undefined}
              toggleDeleteModal={onToggleDeleteModal}
              isDeleteModalShown={isDeleteModalShown}
            />
          )}
        </NxLoadWrapper>
      </section>
    </main>
  );
}
