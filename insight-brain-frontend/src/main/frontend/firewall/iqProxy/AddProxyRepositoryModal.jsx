/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFooter,
  NxFormGroup,
  NxFormSelect,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxP,
  NxRadio,
  NxSuccessAlert,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

import { actions } from 'MainRoot/firewall/iqProxy/firewallIqProxySlice';
import {
  selectCreateProxyRepositoryError,
  selectCreatingProxyRepository,
} from 'MainRoot/firewall/iqProxy/firewallIqProxySelectors';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import {
  DEFAULT_UPSTREAM_URLS,
  FORMAT_OPTIONS,
  getFormatIcon,
  getFormatLabel,
  isPackageHostUrlRequired,
  isPccsEligible,
} from 'MainRoot/firewall/iqProxy/proxyRepositoryFormats';

const NAME_REGEX = /^[A-Za-z0-9._-]+$/;
const PCCS_TOOLTIP = 'PCCS — Quarantine, plus metadata filtering to help clients select a policy compliance version.';

function isValidHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export default function AddProxyRepositoryModal({ managerId, onClose }) {
  const dispatch = useDispatch();
  const creating = useSelector(selectCreatingProxyRepository);
  const createError = useSelector(selectCreateProxyRepositoryError);

  const [name, setName] = useState('');
  const [repoFormat, setRepoFormat] = useState('');
  const [nugetProtocolVersion, setNugetProtocolVersion] = useState('v3');
  const [upstreamUrl, setUpstreamUrl] = useState('');
  const [packageHostUrl, setPackageHostUrl] = useState('https://files.pythonhosted.org');
  const [pccsEnabled, setPccsEnabled] = useState(false);
  const [createdRepo, setCreatedRepo] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    dispatch(actions.clearCreateProxyRepositoryError());
  }, [dispatch]);

  const clearErrorIfPresent = () => {
    if (createError) {
      dispatch(actions.clearCreateProxyRepositoryError());
    }
  };

  const onNameChange = (value) => {
    setName(value);
    clearErrorIfPresent();
  };

  const onFormatChange = (value) => {
    setRepoFormat(value);
    if (!isPccsEligible(value)) {
      setPccsEnabled(false);
    }
    clearErrorIfPresent();
  };

  const onUpstreamUrlChange = (value) => {
    setUpstreamUrl(value);
    clearErrorIfPresent();
  };

  const onPackageHostUrlChange = (value) => {
    setPackageHostUrl(value);
    clearErrorIfPresent();
  };

  const pccsVisible = isPccsEligible(repoFormat);
  const packageHostRequired = isPackageHostUrlRequired(repoFormat);
  const selectedFormat = FORMAT_OPTIONS.find((f) => f.value === repoFormat);

  const toastError = (message) => dispatch(toastActions.addToast({ type: 'error', message }));

  const onSave = () => {
    const trimmedName = name.trim();
    if (!trimmedName) {
      toastError('Repository name is required.');
      return;
    }
    if (!NAME_REGEX.test(trimmedName)) {
      toastError('Only letters, digits, dot, underscore, and hyphen are allowed in the repository name.');
      return;
    }
    if (!repoFormat) {
      toastError('Repository format is required.');
      return;
    }
    if (!upstreamUrl) {
      toastError('Upstream URL is required.');
      return;
    }
    if (!isValidHttpUrl(upstreamUrl)) {
      toastError('Upstream URL must be a valid http(s) URL.');
      return;
    }
    if (packageHostRequired) {
      if (!packageHostUrl) {
        toastError('Package host URL is required.');
        return;
      }
      if (!isValidHttpUrl(packageHostUrl)) {
        toastError('Package host URL must be a valid http(s) URL.');
        return;
      }
    }

    const dto = {
      publicId: trimmedName,
      format: repoFormat,
      upstreamUrl: upstreamUrl.trim(),
    };
    if (repoFormat === 'nuget') {
      dto.protocolVersion = nugetProtocolVersion;
    }
    if (packageHostRequired) {
      dto.packageHostUrl = packageHostUrl.trim();
    }
    if (pccsVisible) {
      dto.pccsEnabled = pccsEnabled;
    }

    dispatch(actions.createProxyRepository({ managerId, dto }))
      .unwrap()
      .then((created) => {
        setCreatedRepo(created);
      })
      .catch(() => {
        // Failure surfaced inline via createProxyRepositoryError.
      });
  };

  const closeSuccessModal = () => {
    setCopied(false);
    onClose(createdRepo);
  };

  const onCopySuccessUrl = async () => {
    if (!createdRepo?.proxyUrl) {
      return;
    }
    try {
      await navigator.clipboard.writeText(createdRepo.proxyUrl);
      setCopied(true);
    } catch {
      dispatch(toastActions.addToast({ type: 'error', message: 'Unable to copy URL to clipboard.' }));
    }
  };

  return (
    <>
      <NxModal id="add-proxy-repository-modal" onCancel={() => onClose(null)}>
        <NxModal.Header>
          <NxH2>Add Proxy Repository</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          {createError && <NxErrorAlert className="iq-add-proxy-repository-modal__error">{createError}</NxErrorAlert>}
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
            <NxP className="iq-add-proxy-repository-modal__creation-caption">
              These fields cannot be edited after creation.
            </NxP>
            <NxFormGroup label="Repository Format" isRequired>
              <div className="iq-firewall-proxy-form__format-selector">
                {selectedFormat ? (
                  <div className="iq-firewall-proxy-form__format-display">
                    <img
                      src={getFormatIcon(selectedFormat.value)}
                      alt=""
                      className="iq-firewall-proxy-form__format-icon"
                    />
                    <span className="iq-firewall-proxy-form__format-label">{getFormatLabel(selectedFormat.value)}</span>
                  </div>
                ) : (
                  <NxFormSelect id="add-proxy-repo-format" value={repoFormat} onChange={onFormatChange}>
                    <option value="">Select format</option>
                    {FORMAT_OPTIONS.map((f) => (
                      <option key={f.value} value={f.value}>
                        {f.label}
                      </option>
                    ))}
                  </NxFormSelect>
                )}
              </div>
            </NxFormGroup>
            <NxP className="iq-add-proxy-repository-modal__creation-caption">
              These fields cannot be edited after creation.
            </NxP>
            {repoFormat === 'nuget' && (
              <NxFormGroup label="Protocol version" isRequired>
                <div className="iq-firewall-proxy-form__protocol-radio-group">
                  <NxRadio
                    name="nugetProtocolVersion"
                    value="v2"
                    isChecked={nugetProtocolVersion === 'v2'}
                    onChange={setNugetProtocolVersion}
                  >
                    Nuget V2
                  </NxRadio>
                  <NxRadio
                    name="nugetProtocolVersion"
                    value="v3"
                    isChecked={nugetProtocolVersion === 'v3'}
                    onChange={setNugetProtocolVersion}
                  >
                    Nuget V3
                  </NxRadio>
                </div>
              </NxFormGroup>
            )}
            <NxFieldset label="Upstream URL:" isRequired>
              {repoFormat && (
                <NxInfoAlert>Location of the remote repository e.g. {DEFAULT_UPSTREAM_URLS[repoFormat]}</NxInfoAlert>
              )}
              <div className="iq-firewall-proxy-form__url-input">
                <NxTextInput
                  inputAttributes={{ maxLength: 2048 }}
                  value={upstreamUrl}
                  onChange={onUpstreamUrlChange}
                  className="nx-text-input--full"
                  placeholder="Upstream repository URL"
                  isPristine
                />
              </div>
            </NxFieldset>
            {packageHostRequired && (
              <NxFieldset label="Package host URL:" isRequired>
                <div className="iq-firewall-proxy-form__url-input">
                  <NxTextInput
                    inputAttributes={{ maxLength: 2048 }}
                    value={packageHostUrl}
                    onChange={onPackageHostUrlChange}
                    className="nx-text-input--full"
                    placeholder="Package host URL"
                    isPristine
                  />
                </div>
              </NxFieldset>
            )}
            {pccsVisible && (
              <NxFieldset
                label={
                  <>
                    Enable PCCS
                    <span className="iq-firewall-proxy-form__info-icon">
                      <NxTooltip title={PCCS_TOOLTIP}>
                        <NxFontAwesomeIcon icon={faInfoCircle} />
                      </NxTooltip>
                    </span>
                  </>
                }
              >
                <NxCheckbox
                  isChecked={pccsEnabled}
                  onChange={(checked) => {
                    setPccsEnabled(checked);
                    clearErrorIfPresent();
                  }}
                >
                  Enable PCCS for this proxy repository
                </NxCheckbox>
              </NxFieldset>
            )}
          </div>
        </NxModal.Content>
        <NxFooter>
          <div className="nx-btn-bar">
            <NxButton variant="tertiary" onClick={() => onClose(null)} disabled={creating}>
              Cancel
            </NxButton>
            <NxButton variant="primary" onClick={onSave} disabled={creating}>
              {creating ? 'Saving…' : 'Save'}
            </NxButton>
          </div>
        </NxFooter>
      </NxModal>
      {createdRepo && (
        <NxModal id="add-proxy-repository-success-modal" onCancel={closeSuccessModal}>
          <NxModal.Header>
            <NxH2>Repository Created</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxSuccessAlert>Repository created successfully.</NxSuccessAlert>
            <p className="iq-add-proxy-repository-modal__proxy-url-label">
              Use the following URL to configure your proxy repository:
            </p>
            <p className="iq-add-proxy-repository-modal__proxy-url-value">
              <strong>{createdRepo.proxyUrl}</strong>
            </p>
          </NxModal.Content>
          <NxFooter>
            <div className="nx-btn-bar">
              <NxButton onClick={closeSuccessModal}>Close</NxButton>
              <NxButton variant="primary" onClick={onCopySuccessUrl} disabled={!createdRepo.proxyUrl}>
                {copied ? 'Copied!' : 'Copy URL'}
              </NxButton>
            </div>
          </NxFooter>
        </NxModal>
      )}
    </>
  );
}

AddProxyRepositoryModal.propTypes = {
  managerId: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
