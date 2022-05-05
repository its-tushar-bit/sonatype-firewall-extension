/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxInfoAlert, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  componentDetailsTagsPropTypes,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter, ComponentDetailsFooterPropTypes as footerPropTypes } from './ComponentDetailsFooter';
import AuditLogContainer from './auditLog/AuditLogContainer';
import { OverviewContainer } from './overview';
import PolicyViolations from './PolicyViolations/PolicyViolations';
import ComponentDetailsSecurityTab from './ComponentDetailsSecurityTab/ComponentDetailsSecurityTab';
import ComponentDetailsLegalTab from './ComponentDetailsLegalTab/ComponentDetailsLegalTab';
import ManageComponentLabelsContainer from './ManageComponentLabels/ManageComponentLabelsContainer';
import { ClaimContainer } from './claim/ClaimContainer';

import ComponentDetailsBackButton from './ComponentDetailsBackButton';
import ComponentDetailsTabs from './ComponentDetailsTabs';
import UnknownComponentAlert from './UnknownComponentAlert';
import {
  isUnknownComponent,
  createTabConfiguration,
  isExactComponent,
  isClaimedComponent,
} from './componentDetailsUtils';
import cx from 'classnames';

export function getTabsConfiguration(isUnknown, isExact, isClaimed) {
  let tabsConfiguration = [
    createTabConfiguration('overview', 'Overview', <OverviewContainer />),
    createTabConfiguration('violations', 'Policy Violations', <PolicyViolations />),
  ];

  if (!isUnknown) {
    tabsConfiguration = [
      ...tabsConfiguration,
      createTabConfiguration('security', 'Security', <ComponentDetailsSecurityTab />),
      createTabConfiguration('legal', 'Legal', <ComponentDetailsLegalTab />),
      createTabConfiguration('labels', 'Labels', <ManageComponentLabelsContainer />),
    ];
  }

  if (!(isExact && !isClaimed)) {
    tabsConfiguration = [...tabsConfiguration, createTabConfiguration('claim', 'Claim', <ClaimContainer />)];
  }

  if (!isUnknown) {
    tabsConfiguration = [...tabsConfiguration, createTabConfiguration('audit', 'Audit Log', <AuditLogContainer />)];
  }

  return tabsConfiguration;
}
export default function ComponentDetails({
  componentDetails,
  activeTabId,
  onTabChange,
  pagination,
  loadComponentDetails,
  loadError,
  loading,
  toggleShowMatchersPopover,
  isProprietary,
  pathnames,
  dependencyTreeRouterParams,
}) {
  useEffect(() => {
    loadComponentDetails();
  }, []);

  const isUnknown = isUnknownComponent(componentDetails);

  const handleTabChange = (tabIdToMoveTo) => {
    if (tabIdToMoveTo === activeTabId) {
      return;
    }
    onTabChange(tabIdToMoveTo);
    if (tabIdToMoveTo === 'labels') {
      loadComponentDetails();
    }
  };

  const tabsConfiguration = getTabsConfiguration(
    isUnknown,
    isExactComponent(componentDetails),
    isClaimedComponent(componentDetails)
  );

  const getClasses = () =>
    cx('nx-page-main iq-component-details-page', {
      'iq-component-details-page--loading': loading,
      'iq-component-details-page--error': loadError,
    });

  return (
    <main className={`nx-viewport-sized ${getClasses()}`}>
      <ComponentDetailsBackButton {...dependencyTreeRouterParams} />
      <div className="nx-viewport-sized__scrollable nx-scrollable iq-component-details-page__content">
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadComponentDetails}>
          {() => (
            <Fragment>
              <ComponentDetailsHeader>
                <Title id="component-details-title">{componentDetails.name}</Title>
                <ComponentDetailsReportInfo {...componentDetails.metadata} />
                <ComponentDetailsTags
                  format={componentDetails.format}
                  dependencyType={componentDetails.dependencyType}
                  isInnerSource={componentDetails.isInnerSource}
                  labels={componentDetails.labels}
                />
              </ComponentDetailsHeader>
              {isUnknown && !isProprietary && (
                <UnknownComponentAlert
                  onClaimClick={() => handleTabChange('claim')}
                  toggleShowMatchersPopover={toggleShowMatchersPopover}
                  pathnames={pathnames}
                />
              )}
              {isUnknown && isProprietary && (
                <NxInfoAlert id="proprietary-component-matched-alert">
                  This component has been matched as a Proprietary Component.{' '}
                  <NxTextLink
                    external
                    href="http://links.sonatype.com/products/nxiq/doc/managing-proprietary-components"
                  >
                    Learn more here
                  </NxTextLink>
                </NxInfoAlert>
              )}
            </Fragment>
          )}
        </NxLoadWrapper>
        <ComponentDetailsTabs
          activeTabId={activeTabId}
          onTabChange={handleTabChange}
          tabsConfiguration={tabsConfiguration}
        />
      </div>
      {!dependencyTreeRouterParams && pagination && <ComponentDetailsFooter {...pagination} />}
    </main>
  );
}

ComponentDetails.propTypes = {
  componentDetails: PropTypes.shape({
    name: PropTypes.string.isRequired,
    hash: PropTypes.string.isRequired,
    format: componentDetailsTagsPropTypes.form,
    dependencyType: componentDetailsTagsPropTypes.dependencyType,
    isInnerSource: componentDetailsTagsPropTypes.isInnerSource,
    labels: componentDetailsTagsPropTypes.label,
    metadata: PropTypes.shape({
      applicationName: PropTypes.string,
      organizationName: PropTypes.string,
      reportTime: PropTypes.number,
      reportTitle: PropTypes.string,
    }),
    matchState: PropTypes.string,
    identificationSource: PropTypes.string,
  }),
  loadComponentDetails: PropTypes.func.isRequired,

  // activeTabId should be required but marking it as such causes proptype errors when navigating away
  activeTabId: PropTypes.string,
  onTabChange: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  pagination: PropTypes.shape(footerPropTypes),
  toggleShowMatchersPopover: PropTypes.func.isRequired,
  isProprietary: PropTypes.bool,
  pathnames: PropTypes.arrayOf(PropTypes.string),
  dependencyTreeRouterParams: PropTypes.shape({ publicId: PropTypes.string, scanId: PropTypes.string }),
};
