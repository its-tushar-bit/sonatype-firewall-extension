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
  NxDivider,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxInfoAlert,
  NxRadio,
  NxReadOnly,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { actions as firewallIqProxyActions } from 'MainRoot/firewall/iqProxy/firewallIqProxySlice';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { Messages } from 'MainRoot/util/CommonServices';
import {
  DEFAULT_UPSTREAM_URLS,
  getFormatIcon,
  getFormatLabel,
  isPackageHostUrlRequired,
  isPccsEligible,
} from 'MainRoot/firewall/iqProxy/proxyRepositoryFormats';

const PCCS_TOOLTIP = 'PCCS — Quarantine, plus metadata filtering to help clients select a policy compliance version.';

function isValidHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

export default function ProxyRepositoryConfigurationTile({ canEdit }) {
  const dispatch = useDispatch();
  const repository = useSelector(selectSelectedOwner) ?? {};
  const currentParams = useSelector(selectRouterCurrentParams) || {};
  const managerId = repository.repositoryManagerId || currentParams.repositoryManagerId;
  const format = repository.format || '';
  const readOnly = !canEdit;

  const persistedUpstreamUrl = repository.upstreamUrl || '';
  const persistedPackageHostUrl = repository.packageHostUrl || '';
  const persistedPccsEnabled = Boolean(repository.policyCompliantComponentSelectionEnabled ?? repository.pccsEnabled);
  const persistedProtocolVersion = repository.protocolVersion || 'v3';

  const [upstreamUrl, setUpstreamUrl] = useState(persistedUpstreamUrl);
  const [packageHostUrl, setPackageHostUrl] = useState(persistedPackageHostUrl);
  const [pccsEnabled, setPccsEnabled] = useState(persistedPccsEnabled);
  const [nugetProtocolVersion, setNugetProtocolVersion] = useState(persistedProtocolVersion);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setUpstreamUrl(persistedUpstreamUrl);
    setPackageHostUrl(persistedPackageHostUrl);
    setPccsEnabled(persistedPccsEnabled);
    setNugetProtocolVersion(persistedProtocolVersion);
  }, [repository?.id, persistedUpstreamUrl, persistedPackageHostUrl, persistedPccsEnabled, persistedProtocolVersion]);

  const pccsVisible = isPccsEligible(format);
  const packageHostRequired = isPackageHostUrlRequired(format);
  const selectedFormatIcon = getFormatIcon(format);
  const selectedFormatLabel = getFormatLabel(format);

  const toastError = (message) => dispatch(toastActions.addToast({ type: 'error', message }));
  const toastSuccess = (message) => dispatch(toastActions.addToast({ type: 'success', message }));

  const onSave = () => {
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
    if (!managerId || !repository.id) {
      toastError('Cannot determine target repository. Refresh the page and try again.');
      return;
    }
    const dto = { upstreamUrl: upstreamUrl.trim() };
    if (pccsVisible) {
      dto.pccsEnabled = pccsEnabled;
    }
    if (packageHostRequired) {
      dto.packageHostUrl = packageHostUrl.trim();
    }
    setSaving(true);
    dispatch(firewallIqProxyActions.updateProxyRepository({ managerId, repositoryId: repository.id, dto }))
      .unwrap()
      .then(() => {
        toastSuccess('Proxy repository updated.');
      })
      .catch((error) => {
        toastError(Messages.getHttpErrorMessage(error) || 'An error occurred while updating the proxy repository.');
      })
      .finally(() => {
        setSaving(false);
      });
  };

  return (
    <div className={`iq-firewall-proxy-form${readOnly ? ' iq-firewall-proxy-form--embedded' : ''}`}>
      <div className="iq-firewall-proxy-form__name-field">
        <NxReadOnly>
          <NxReadOnly.Label>Repository Name</NxReadOnly.Label>
          <NxReadOnly.Data>{repository.publicId || '-'}</NxReadOnly.Data>
        </NxReadOnly>
      </div>
      <NxReadOnly>
        <NxReadOnly.Label>Repository Format</NxReadOnly.Label>
        <NxReadOnly.Data>
          {format ? (
            <span className="iq-firewall-proxy-form__format-display">
              {selectedFormatIcon && (
                <img src={selectedFormatIcon} alt="" className="iq-firewall-proxy-form__format-icon" />
              )}
              <span className="iq-firewall-proxy-form__format-label">{selectedFormatLabel}</span>
            </span>
          ) : (
            '-'
          )}
        </NxReadOnly.Data>
      </NxReadOnly>
      {format === 'nuget' && (
        <NxFormGroup label="Protocol version" isRequired>
          <div className="iq-firewall-proxy-form__protocol-radio-group">
            <NxRadio
              name="nugetProtocolVersion"
              value="v2"
              isChecked={nugetProtocolVersion === 'v2'}
              onChange={setNugetProtocolVersion}
              disabled={readOnly}
            >
              Nuget V2
            </NxRadio>
            <NxRadio
              name="nugetProtocolVersion"
              value="v3"
              isChecked={nugetProtocolVersion === 'v3'}
              onChange={setNugetProtocolVersion}
              disabled={readOnly}
            >
              Nuget V3
            </NxRadio>
          </div>
        </NxFormGroup>
      )}
      <NxFieldset label="Upstream URL:" isRequired>
        {format && <NxInfoAlert>Location of the remote repository e.g. {DEFAULT_UPSTREAM_URLS[format]}</NxInfoAlert>}
        <div className="iq-firewall-proxy-form__url-input">
          <NxTextInput
            inputAttributes={{ maxLength: 2048 }}
            value={upstreamUrl}
            onChange={(value) => setUpstreamUrl(value)}
            className="nx-text-input--full"
            placeholder="Upstream repository URL"
            isPristine
            disabled={readOnly}
          />
        </div>
      </NxFieldset>
      {packageHostRequired && (
        <NxFieldset label="Package host URL:" isRequired>
          <div className="iq-firewall-proxy-form__url-input">
            <NxTextInput
              inputAttributes={{ maxLength: 2048 }}
              value={packageHostUrl}
              onChange={(value) => setPackageHostUrl(value)}
              className="nx-text-input--full"
              placeholder="Hosted repository URL"
              isPristine
              disabled={readOnly}
            />
          </div>
        </NxFieldset>
      )}
      {pccsVisible && (
        <NxFormGroup
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
          <NxCheckbox isChecked={pccsEnabled} onChange={setPccsEnabled} disabled={readOnly}>
            Enable PCCS for this proxy repository
          </NxCheckbox>
        </NxFormGroup>
      )}
      {!readOnly && (
        <>
          <NxDivider className="iq-firewall-proxy-form__actions-divider" />
          <div className="iq-firewall-proxy-form__actions">
            <div className="nx-btn-bar">
              <NxButton variant="primary" type="button" onClick={onSave} disabled={saving}>
                {saving ? 'Updating…' : 'Update'}
              </NxButton>
            </div>
          </div>
        </>
      )}
    </div>
  );
}

ProxyRepositoryConfigurationTile.propTypes = {
  canEdit: PropTypes.bool,
};
