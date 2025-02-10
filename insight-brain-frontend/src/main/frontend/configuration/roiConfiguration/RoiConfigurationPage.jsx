/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxCheckbox,
  NxDivider,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxTile,
} from '@sonatype/react-shared-components';
import { faPen } from '@fortawesome/pro-solid-svg-icons';
import * as R from 'ramda';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectHasFirewallLicense, selectHasLifecycleLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

import { selectRoiConfigurationPageSlice } from './roiConfigurationPageSelectors';
import { actions, ROI_SECURITY_VIOLATION_TYPES } from './roiConfigurationPageSlice';

import './RoiConfigurationPage.scss';

const formatCurrency = (value) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

const RoiConfigurationPage = () => {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();

  const { loading, error, configuration } = useSelector(selectRoiConfigurationPageSlice);
  const hasLifecycleLicense = useSelector(selectHasLifecycleLicense);
  const hasFirewallLicense = useSelector(selectHasFirewallLicense);

  const licenseError = R.complement(R.or)(hasLifecycleLicense, hasFirewallLicense)
    ? 'Must have Lifecycle or Repository Firewall license to configure ROI metrics.'
    : null;

  const loadPage = () => dispatch(actions.loadConfiguration());

  const editRoiConfigurationPageHref = uiRouterState.href('editRoiConfiguration');

  useEffect(() => {
    loadPage();
  }, []);

  const formatNumericValue = R.curryN(2, (id, value, isInteger = false) => (
    <span
      className="roi-configuration-page__numeric-value"
      id={`roi-configuration-page__numeric-value__${id}`}
      data-testid={`roi-configuration-page__numeric-value__${id}`}
    >
      {R.when(R.always(!isInteger), formatCurrency)(value)}
    </span>
  ));

  return (
    <NxPageMain id="roi-configuration-page" className="roi-configuration-page">
      <NxPageTitle>
        <NxPageTitle.Headings>
          <NxH1>Return on Investment</NxH1>
        </NxPageTitle.Headings>
        <NxPageTitle.Description>
          Key metrics to quantify the Return on Investment (ROI) of your organization&apos;s partnership with Sonatype.
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxTile className="roi-configuration-page__tile">
        <NxLoadWrapper loading={loading} error={error || licenseError} retryHandler={loadPage}>
          <NxTile.Header>
            <NxTile.Headings>
              <NxTile.HeaderTitle>
                <NxH2>Return on Investment Values</NxH2>
              </NxTile.HeaderTitle>
            </NxTile.Headings>
            <NxTile.HeaderActions>
              <NxTextLink
                id="roi-configuration-page__button__edit"
                className="nx-btn nx-btn--tertiary"
                href={editRoiConfigurationPageHref}
              >
                <NxFontAwesomeIcon icon={faPen} />
                <span>Edit</span>
              </NxTextLink>
            </NxTile.HeaderActions>
          </NxTile.Header>
          <NxTile.Content>
            <NxP>
              Assess the ROI of your organization&apos;s partnership with Sonatype. Default values are provided based on
              industry benchmarks but can be customized to reflect the specific needs of your organization.
            </NxP>
            <dl className="roi-configuration-description-list">
              <div className="roi-configuration-description-list__item">
                <dt>Currency</dt>
                <dd>United States Dollar (USD)</dd>
              </div>
            </dl>
            <NxDivider />
            {hasLifecycleLicense && (
              <>
                <NxH2 id="roi-configuration-page__lifecycle-title">Lifecycle Metrics</NxH2>
                <NxP>
                  To determine the ROI for reported on policy violation in Sonatype Lifecycle, provide an estimate of
                  your cost per hour for your teams to remediate violations.
                </NxP>
                <dl className="roi-configuration-description-list">
                  <div className="roi-configuration-description-list__item">
                    <dt>Developer Hourly Rate</dt>
                    <dd>
                      <small>Hourly cost for working on remediation.</small>
                      {formatNumericValue('developer-hourly-rate', configuration.developerHourlyRate)}
                    </dd>
                  </div>
                  <div className="roi-configuration-description-list__item">
                    <dt>Fix Rate</dt>
                    <dd>
                      <small>The expected (estimated) number of hours to remediate by violation types</small>
                      {formatNumericValue('fix-rate', configuration.fixRate, true)}
                    </dd>
                  </div>
                  <div className="roi-configuration-description-list__item">
                    <dt>Security Violation Types</dt>
                    <dd>
                      <small>Types of violations configured to be enforced by Lifecycle</small>
                      <dl className="roi-configuration-security-violation-types">
                        {ROI_SECURITY_VIOLATION_TYPES.map((key) => (
                          <div className="roi-configuration-security-violation-types__item" key={key}>
                            <dt>{R.toUpper(key)}:</dt>
                            <dd
                              id={`roi-configuration-page__security-violation-types-content__${key}`}
                              data-testid={`roi-configuration-security-violation-types__content__${key}`}
                            >
                              {R.ifElse(
                                R.always(R.equals(configuration.securityViolation[`${key}Enabled`], true)),
                                formatNumericValue(`security-violation-${key}`),
                                R.always('Not Included')
                              )(configuration.securityViolation[key])}
                            </dd>
                          </div>
                        ))}
                      </dl>
                    </dd>
                  </div>
                  <div className="roi-configuration-description-list__item">
                    <dt>Waived Violations</dt>
                    <dd>
                      <NxCheckbox
                        id="roi-configuration-page__checkbox__waived-violations"
                        inputAttributes={{
                          'data-testid': 'roi-configuration-page__checkbox__waived-violations',
                        }}
                        isChecked={!!configuration.waivedViolations}
                        disabled
                      >
                        Include the waived violations when calculating the time taken to remediate the policy
                        violations.
                      </NxCheckbox>
                    </dd>
                  </div>
                </dl>
              </>
            )}
            {hasFirewallLicense && (
              <>
                <NxH2 id="roi-configuration-page__firewall-title">Repository Firewall Metrics</NxH2>
                <NxP>
                  To show the ROI for Repository Firewall provide the estimated cost/value to your team for each of the
                  provided features.
                </NxP>
                <dl className="roi-configuration-description-list">
                  <div className="roi-configuration-description-list__item">
                    <dt>Supply chain attacks blocked</dt>
                    <dd>
                      <small>Detected violations for security-malicious components.</small>
                      {formatNumericValue('supply-chain-attacks-blocked', configuration.supplyChainAttacksBlocked)}
                    </dd>
                  </div>
                  <div className="roi-configuration-description-list__item">
                    <dt>Namespace attacks blocked</dt>
                    <dd>
                      <small>Detected violations for namespace-conflict components.</small>
                      {formatNumericValue('namespace-attacks-blocked', configuration.namespaceAttacksBlocked)}
                    </dd>
                  </div>
                  <div className="roi-configuration-description-list__item">
                    <dt>Safe components auto-selected</dt>
                    <dd>
                      <small>Policy compliant components found when installing dependencies.</small>
                      {formatNumericValue('safe-components-auto-selected', configuration.safeComponentsAutoSelected)}
                    </dd>
                  </div>
                </dl>
              </>
            )}
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    </NxPageMain>
  );
};

export default RoiConfigurationPage;
