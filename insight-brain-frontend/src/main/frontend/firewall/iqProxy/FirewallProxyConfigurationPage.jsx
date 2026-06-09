/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  NxH1,
  NxH2,
  NxPageMain,
  NxPageTitle,
  NxTextInput,
  NxTile,
  NxFieldset,
  NxButton,
  NxFormSelect,
  NxFormGroup,
  NxInfoAlert,
  NxModal,
  NxSuccessAlert,
  NxFooter,
} from '@sonatype/react-shared-components';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { actions as repositoriesConfigActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';

import { selectRepositoryManagerId } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/firewall/iqProxy/firewallIqProxySlice';
import { selectSaving, selectSaveError, selectSaveErrorId } from 'MainRoot/firewall/iqProxy/firewallIqProxySelectors';

const DEFAULT_UPSTREAM_URLS = {
  maven2: 'https://repo1.maven.org/maven2/',
  npm: 'https://registry.npmjs.org',
  pypi: 'https://pypi.org',
  nuget: 'https://api.nuget.org/v3/index.json',
};

FirewallProxyForm.propTypes = {
  embedded: PropTypes.bool,
};

function FirewallProxyForm({ embedded }) {
  const [upstreamUrl, setUpstreamUrl] = useState('');
  const [repoFormat, setRepoFormat] = useState('');
  const [name, setName] = useState('');
  const [proxyUrl, setProxyUrl] = useState(null);
  const [showSuccessModal, setShowSuccessModal] = useState(false);
  const [copied, setCopied] = useState(false);

  const repositoryManagerId = useSelector(selectRepositoryManagerId);
  const saving = useSelector(selectSaving);
  const saveError = useSelector(selectSaveError);
  const saveErrorId = useSelector(selectSaveErrorId);
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(ownerSideNavActions.loadOwnerList());
  }, []);

  useEffect(() => {
    if (proxyUrl) {
      setShowSuccessModal(true);
    }
  }, [proxyUrl]);

  useEffect(() => {
    if (saveError) {
      dispatch(toastActions.addToast({ type: 'error', message: saveError }));
    }
  }, [saveErrorId]);

  function onNameChange(value) {
    setName(value);
  }

  function onUpstreamUrlChange(value) {
    setUpstreamUrl(value);
  }

  function clearAll() {
    setName('');
    setRepoFormat('');
    setUpstreamUrl('');
  }

  function onSave() {
    if (!name) {
      dispatch(toastActions.addToast({ type: 'error', message: 'Repository name is required.' }));
      return;
    }
    if (!repoFormat) {
      dispatch(toastActions.addToast({ type: 'error', message: 'Repository format is required.' }));
      return;
    }
    if (!upstreamUrl) {
      dispatch(toastActions.addToast({ type: 'error', message: 'Upstream URL is required.' }));
      return;
    }
    setProxyUrl(null);
    dispatch(actions.saveRepository({ repositoryManagerId, name, repoFormat, upstreamUrl }))
      .unwrap()
      .then((savedRepository) => {
        setProxyUrl(savedRepository.proxyUrl);
        clearAll();
        dispatch(ownerSideNavActions.forceReload());
        dispatch(repositoriesConfigActions.loadRepositoriesByManagerId(repositoryManagerId));
      });
  }

  async function onCopy() {
    try {
      await navigator.clipboard.writeText(proxyUrl);
      setCopied(true);
    } catch (error) {
      dispatch(toastActions.addToast({ type: 'error', message: 'Unable to copy URL to clipboard.' }));
    }
  }

  function onCloseModal() {
    setShowSuccessModal(false);
    setCopied(false);
  }

  const formContent = (
    <>
      <div className="iq-firewall-proxy-form">
        <div className="iq-firewall-proxy-form__name-field">
          <NxFormGroup label="Repository Name" isRequired>
            <NxTextInput
              inputAttributes={{ maxLength: 40 }}
              value={name}
              onChange={onNameChange}
              className="nx-text-input--full"
              isPristine
              placeholder="Repository name"
            />
          </NxFormGroup>
        </div>
        <NxFormGroup label="Repository Format" isRequired>
          <NxFormSelect id="proxy-repo-format" value={repoFormat} onChange={(value) => setRepoFormat(value)}>
            <option value="">Select format</option>
            <option value="maven2">Maven</option>
            <option value="npm">npm</option>
            <option value="pypi">PyPI</option>
            <option value="nuget">NuGet</option>
          </NxFormSelect>
        </NxFormGroup>
        <NxFieldset label="Upstream Url:" isRequired>
          {repoFormat && (
            <NxInfoAlert>Location of the remote repository e.g. {DEFAULT_UPSTREAM_URLS[repoFormat]}</NxInfoAlert>
          )}
          <div className="iq-firewall-proxy-form__url-row">
            <NxTextInput
              inputAttributes={{ maxLength: 2048 }}
              value={upstreamUrl}
              onChange={onUpstreamUrlChange}
              className="nx-text-input--full"
              placeholder="Upstream repository URL"
              isPristine
            />
            <NxButton variant="primary" onClick={onSave} disabled={saving}>
              {saving ? 'Saving…' : 'Save'}
            </NxButton>
          </div>
        </NxFieldset>
      </div>
      {showSuccessModal && (
        <NxModal id="iq-proxy-success-modal" onCancel={onCloseModal}>
          <NxModal.Header>
            <NxH2>Repository Created</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxSuccessAlert>Repository created successfully.</NxSuccessAlert>
            <p className="iq-firewall-proxy-modal__proxy-url-label">
              Use the following URL to configure your proxy repository:
            </p>
            <p>
              <strong>{proxyUrl}</strong>
            </p>
          </NxModal.Content>
          <NxFooter>
            <div className="nx-btn-bar">
              <NxButton onClick={onCloseModal}>Close</NxButton>
              <NxButton variant="primary" onClick={onCopy}>
                {copied ? 'Copied!' : 'Copy URL'}
              </NxButton>
            </div>
          </NxFooter>
        </NxModal>
      )}
    </>
  );

  if (embedded) {
    return formContent;
  }

  return <NxTile>{formContent}</NxTile>;
}

export default function FirewallProxyConfigurationPage({ embedded }) {
  if (embedded) {
    return <FirewallProxyForm embedded />;
  }

  return (
    <NxPageMain>
      <NxPageTitle>
        <NxH1>IQ Proxy</NxH1>
      </NxPageTitle>
      <FirewallProxyForm />
    </NxPageMain>
  );
}

FirewallProxyConfigurationPage.propTypes = {
  embedded: PropTypes.bool,
};
