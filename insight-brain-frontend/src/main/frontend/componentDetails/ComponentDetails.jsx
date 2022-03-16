/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import cx from 'classnames';
import { NxInfoAlert, NxLoadWrapper, NxTextLink } from '@sonatype/react-shared-components';
import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  componentDetailsTagsPropTypes,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter, ComponentDetailsFooterPropTypes as footerPropTypes } from './ComponentDetailsFooter';

import ComponentDetailsBackButton from './ComponentDetailsBackButton';
import ComponentDetailsTabs from './ComponentDetailsTabs';
import UnknownComponentAlert from './UnknownComponentAlert';

export const getTabIdPerIndex = (isUnknown, isClaimed, isExact) => {
  if (isUnknown) {
    return ['overview', 'violations', 'claim'];
  }

  return isExact && !isClaimed
    ? ['overview', 'violations', 'security', 'legal', 'labels', 'audit']
    : ['overview', 'violations', 'security', 'legal', 'labels', 'claim', 'audit'];
};

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

  const classes = cx('nx-page-main nx-viewport-sized iq-component-details-page', {
    'iq-component-details-page--loading': loading,
    'iq-component-details-page--error': loadError,
  });

  const isUnknown = componentDetails?.matchState === 'unknown';
  const isExact = componentDetails?.matchState === 'exact';
  const isClaimed = componentDetails?.identificationSource === 'Manual';

  const tabIdPerIndex = getTabIdPerIndex(isUnknown, isClaimed, isExact);

  const handleTabChange = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === activeTabId) {
      return;
    }
    onTabChange(tabIdToMoveTo);
    if (tabIndex === tabIdPerIndex.indexOf('labels')) {
      loadComponentDetails();
    }
  };

  const goToClaim = () => {
    handleTabChange(tabIdPerIndex.indexOf('claim'));
  };

  return (
    <main className={classes}>
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
                  onClaimClick={goToClaim}
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
          activeTab={tabIdPerIndex.indexOf(activeTabId)}
          onTabChange={handleTabChange}
          isUnknown={isUnknown}
          isClaimed={isClaimed}
          isExact={isExact}
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
