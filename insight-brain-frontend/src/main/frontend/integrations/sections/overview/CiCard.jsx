/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useDispatch, useSelector } from 'react-redux';
import React, { useEffect } from 'react';
import { actions } from 'MainRoot/integrations/sections/overview/ciUsageSlice';
import { selectCiUsageSlice } from 'MainRoot/integrations/integrationsSelectors';
import { NxBinaryDonutChart, NxTile, NxH3, NxLoadWrapper, NxP, NxTextLink } from '@sonatype/react-shared-components';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';
import useGetIntegrationsLink from 'MainRoot/integrations/useGetIntegrationsLink';
import { isNil } from 'ramda';
import getThreeMonthsAgo from 'MainRoot/integrations/utils/getThreeMonthsAgo';
import { CiUsageAppPreviewTable } from 'MainRoot/integrations/sections/overview/CiUsageAppPreviewTable';

export default function CiCard() {
  const { loading, loadError, result: ciUsage, reload } = useLoadCiUsage({ sinceUtcTimestamp: getThreeMonthsAgo() });
  const ciUrl = useGetIntegrationsLink(SECTIONS.CICD);
  const percentWithoutCiCd = percentAppsWithoutCiCd();

  const integrationMessage =
    percentWithoutCiCd === 0
      ? 'All of your apps are integrated with CI'
      : `${percentWithoutCiCd}% of your apps are not integrated with CI`;

  return (
    <NxTile>
      <NxTile.Content>
        <div className="iq-integrations-cicard__content iq-integrations-card--align-left">
          <div data-testid="iq-integrations-cicard--stats-section" className="iq-integrations-cicard__left">
            <NxLoadWrapper loading={loading} error={loadError} retryHandler={reload}>
              <div className="iq-integrations-cicard__donut-wrapper">
                <NxBinaryDonutChart
                  data-testid="iq-integrations-cicard__donut"
                  className="iq-integrations-cicard__donut"
                  value={percentAppsWithoutCiCd()}
                />

                <div className="iq-integrations-cicard__donut-col iq-integrations-cicard__donut-caption">
                  {integrationMessage}
                </div>
              </div>
            </NxLoadWrapper>

            <NxH3>What are Sonatype CI Integrations used for?</NxH3>

            <NxP>
              Lifecycle's CI integrations perform{' '}
              <NxTextLink
                external
                href="https://links.sonatype.com/products/nxiq/doc/integrations/overrides/cicd/ABFAdvancedBinaryFingerprinting"
                data-analytics-id="sonatype-developer-overview-binary-scanning"
              >
                binary scanning
              </NxTextLink>{' '}
              at multiple stages of your deployment. You can reduce disruption by warning of risk during the CI build
              while blocking critical issues from automatically deploying to production.
            </NxP>

            <NxP>
              Analyzing manifests alone could miss changes made during a build and end up in production. Sonatype's
              binary fingerprint scanning, run during your CI builds, provides a more accurate assessment of open-source
              risk.
            </NxP>

            <NxP>
              Learn more{' '}
              <NxTextLink href={ciUrl} data-analytics-id="sonatype-developer-overview-ci-integrations">
                about our CI systems integrations
              </NxTextLink>
              .
            </NxP>
          </div>

          <div className="iq-integrations-cicard__right">
            <NxH3>Apps Without CI System Integrations</NxH3>

            <CiUsageAppPreviewTable />
          </div>
        </div>
      </NxTile.Content>
    </NxTile>
  );

  function percentAppsWithoutCiCd() {
    if (isNil(ciUsage)) {
      return null;
    }

    const { numAppsWithoutCITriggeredEvals, numTotalApps } = ciUsage;

    if (numTotalApps === 0) {
      return 0;
    }

    return Math.round((numAppsWithoutCITriggeredEvals / numTotalApps) * 100);
  }
}

function useLoadCiUsage({ sinceUtcTimestamp }) {
  const dispatch = useDispatch();

  const doLoad = () => dispatch(actions.loadCiUsage({ sinceUtcTimestamp }));

  useEffect(() => {
    doLoad();
  }, []);

  return { ...useSelector(selectCiUsageSlice), reload: doLoad };
}
