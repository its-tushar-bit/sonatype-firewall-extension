/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */

import React, { useEffect } from 'react';
import { NxGrid, NxH3, NxLoadWrapper, NxTile, NxErrorAlert, NxH2, NxTextLink } from '@sonatype/react-shared-components';
import classnames from 'classnames';

export default function ZscalerConfigLimits({ zscalerConfigLimitsState, loadLimits, serverData }) {
  const { loading, error, limits } = zscalerConfigLimitsState;
  const totalAllowedUrls = limits?.totalAllowedUrls ?? 0;
  const remainingUrls = limits?.remainingUrls ?? 0;
  const notConfiguredLimits = limits?.status === 'none';
  const underLimits = limits?.status === 'under';
  const overLimits = limits?.status === 'over';
  const statusIndicator = (
    <span
      role="status"
      className={classnames('zscaler-config-limits__status-indicator', 'nx-status-indicator', {
        'nx-status-indicator--positive': underLimits,
        'nx-status-indicator--intermediate': notConfiguredLimits,
        'nx-status-indicator--error': overLimits,
      })}
    >
      {notConfiguredLimits && 'Not Configured'}
      {underLimits && 'OSS Malware Catalog Synced'}
      {overLimits && 'Zscaler Custom URL Limit Exceeded'}
    </span>
  );

  useEffect(() => {
    loadLimits();
  }, [serverData]);

  return (
    <>
      <NxTile id="zscaler-limits">
        <NxLoadWrapper loading={loading} error={error} retryHandler={loadLimits}>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2 id="zscaler-custom-urls-header">Zscaler Custom URLs</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <NxGrid.Row>
              <NxGrid.Column className="nx-grid-col--25">
                <NxH3 className="nx-grid-header__title">Total Purchased</NxH3>
                <span>{totalAllowedUrls.toLocaleString()}</span>
              </NxGrid.Column>
              <NxGrid.Column className="nx-grid-col--25">
                <NxH3 className="nx-grid-header__title">Remaining</NxH3>
                <span>{remainingUrls.toLocaleString()}</span>
              </NxGrid.Column>
              <NxGrid.Column>
                <NxH3 className="nx-grid-header__title">Status</NxH3>
                {statusIndicator}
              </NxGrid.Column>
            </NxGrid.Row>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
      {overLimits && (
        <NxErrorAlert>
          Zscaler Custom URL limit exceeded. Some URLs were not added to the Sonatype Custom URL Categories. Review your
          configured formats or contact Zscaler to increase your Custom URL allowance for full protection against
          malware in open-source components.
          <br />
          <NxTextLink external href="https://links.sonatype.com/products/nxrm3/docs/zscaler/main">
            Learn more about Zscaler Custom URL limits
          </NxTextLink>
        </NxErrorAlert>
      )}
    </>
  );
}
