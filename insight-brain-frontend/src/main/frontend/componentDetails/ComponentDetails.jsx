/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import cx from 'classnames';
import { useDispatch, useSelector } from 'react-redux';
import { NxInfoAlert, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter } from './ComponentDetailsFooter';
import { actions } from './componentDetailsSlice';
import AuditLogContainer from './auditLog/AuditLogContainer';
import { OverviewContainer } from './overview';
import PolicyViolations from './PolicyViolations/PolicyViolations';
import ComponentDetailsSecurityTab from './ComponentDetailsSecurityTab/ComponentDetailsSecurityTab';
import ComponentDetailsLegalTab from './ComponentDetailsLegalTab/ComponentDetailsLegalTab';
import ManageComponentLabelsContainer from './ManageComponentLabels/ManageComponentLabelsContainer';
import PolicyViolationDetailsPopover from './ViolationsTableTile/PolicyViolationDetailsPopover';
import { ClaimContainer } from './claim/ClaimContainer';

import ComponentDetailsBackButton from './ComponentDetailsBackButton';
import ComponentDetailsTabs from './ComponentDetailsTabs';
import UnknownComponentAlert from './UnknownComponentAlert';
import {
  selectComponentDetails,
  selectActiveTabId,
  selectComponentPagination,
  selectComponentDetailsLoadErrors,
  selectComponentDetailsLoading,
  selectIsProprietary,
  selectFilteredPathnames,
} from './componentDetailsSelectors';
import { selectDependencyTreeRouterParams } from 'MainRoot/applicationReport/applicationReportSelectors';

import {
  isUnknownComponent,
  createTabConfiguration,
  isExactComponent,
  isClaimedComponent,
} from './componentDetailsUtils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsStandaloneDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';

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
export default function ComponentDetails() {
  const dispatch = useDispatch();
  const uiRouterStateService = useRouterState();
  const componentDetails = useSelector(selectComponentDetails);
  const activeTabId = useSelector(selectActiveTabId);
  const pagination = useSelector((state) => selectComponentPagination(state, uiRouterStateService));
  const loadError = useSelector(selectComponentDetailsLoadErrors);
  const loading = useSelector(selectComponentDetailsLoading);
  const isProprietary = useSelector(selectIsProprietary);
  const pathnames = useSelector(selectFilteredPathnames);
  const dependencyTreeRouterParams = useSelector(selectDependencyTreeRouterParams);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const loadComponentDetails = () => dispatch(actions.loadComponentDetails());
  const onTabChange = (tabId) => dispatch(actions.onTabChange(tabId));
  const toggleShowMatchersPopover = () => dispatch(actions.toggleShowMatchersPopover());

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
    <>
      <PolicyViolationDetailsPopover />
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
                  <NxInfoAlert id="proprietary-component-matched-alert" role="alert">
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
        {!dependencyTreeRouterParams && !isStandaloneDeveloper && pagination && (
          <ComponentDetailsFooter {...pagination} />
        )}
      </main>
    </>
  );
}
