/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { NxH3, NxButton, NxFontAwesomeIcon, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import { faCopy } from '@fortawesome/pro-regular-svg-icons';
import { faCheck } from '@fortawesome/free-solid-svg-icons';

import classnames from 'classnames';
import { useDispatch, useSelector } from 'react-redux';

import { selectEnterpriseReportingSupportInfo } from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSelectors';
import { actions } from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSlice';

export default function FirewallEnterpriseReportingSupportInfo() {
  const dispatch = useDispatch();
  const { telemetryStatus, loading, loadError } = useSelector(selectEnterpriseReportingSupportInfo);
  const telemetryToString = JSON.stringify(telemetryStatus, null, ' ');
  const [telemetryCopied, setTelemetryCopied] = useState(false);
  const [isCopying, setIsCopying] = useState(false);
  const load = () => dispatch(actions.load());

  useEffect(() => {
    load();
  }, []);

  const copyToClipboard = async () => {
    try {
      setIsCopying(true);
      await window.navigator.clipboard.writeText(telemetryToString);
      setTelemetryCopied(true);
      // Auto-dismiss success message after 3 seconds
      setTimeout(() => {
        setTelemetryCopied(false);
      }, 3000);
    } catch (error) {
      console.error('Failed to copy to clipboard:', error);
      // Note: In a production environment, you might want to show an error message to the user
    } finally {
      setIsCopying(false);
    }
  };

  return (
    <div className="fw-iq-enterprise-reporting-support-info">
      <NxH3 className="fw-iq-enterprise-reporting-support-info__title fw-iq-enterprise-reporting-support-info__title--help">
        Help Documentation
      </NxH3>
      <NxTextLink
        className="fw-iq-enterprise-reporting-support-info__link"
        external
        href="https://links.sonatype.com/products/firewall/enterprise-reporting"
      >
        Enterprise Reporting
      </NxTextLink>
      <NxH3 className="fw-iq-enterprise-reporting-support-info__title">Support Information</NxH3>
      <NxLoadWrapper loading={loading} retryHandler={load} error={loadError}>
        <NxButton className="fw-iq-enterprise-reporting-support-info__btn" onClick={copyToClipboard} variant="tertiary">
          <NxFontAwesomeIcon
            className={classnames({ copied: telemetryCopied })}
            icon={telemetryCopied ? faCheck : faCopy}
          />
          Copy Support Info to Clipboard
        </NxButton>
        {telemetryCopied && !isCopying && (
          <div className="fw-iq-enterprise-reporting-support-info__message">Support info copied to clipboard</div>
        )}
      </NxLoadWrapper>
    </div>
  );
}
