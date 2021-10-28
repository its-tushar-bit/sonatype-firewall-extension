/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import cx from 'classnames';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  componentDetailsTagsPropTypes,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter, ComponentDetailsFooterPropTypes as footerPropTypes } from './ComponentDetailsFooter';

import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import ComponentDetailsTabs from './ComponentDetailsTabs';
import UnknownComponentAlert from './UnknownComponentAlert';

export const getTabIdPerIndex = (isUnknown, isClaimed) => {
  if (isUnknown) {
    return ['overview', 'violations', 'claim'];
  }

  return isClaimed
    ? ['overview', 'violations', 'security', 'legal', 'labels', 'claim', 'audit']
    : ['overview', 'violations', 'security', 'legal', 'labels', 'audit'];
};

export default function ComponentDetails({
  componentDetails,
  activeTabId,
  onTabChange,
  backToOffspringOnClick,
  pagination,
  loadComponentDetails,
  loadError,
  loading,
  toggleShowMatchersPopover,
}) {
  useEffect(() => {
    loadComponentDetails();
  }, []);

  const classes = cx('nx-page-main nx-viewport-sized iq-component-details-page', {
    'iq-component-details-page--loading': loading,
    'iq-component-details-page--error': loadError,
  });

  const isUnknown = componentDetails?.matchState === 'unknown';
  const isClaimed = componentDetails?.identificationSource === 'Manual';

  const tabIdPerIndex = getTabIdPerIndex(isUnknown, isClaimed);

  const handleTabChange = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === activeTabId) {
      return;
    }
    onTabChange(tabIdToMoveTo);
  };

  const goToClaim = () => {
    handleTabChange(tabIdPerIndex.indexOf('claim'));
  };

  return (
    <main className={classes}>
      <MenuBarBackButton stateName="applicationReport.policy" />
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
              {isUnknown && (
                <UnknownComponentAlert onClaimClick={goToClaim} toggleShowMatchersPopover={toggleShowMatchersPopover} />
              )}
            </Fragment>
          )}
        </NxLoadWrapper>
        <ComponentDetailsTabs
          activeTab={tabIdPerIndex.indexOf(activeTabId)}
          onTabChange={handleTabChange}
          isUnknown={isUnknown}
          isClaimed={isClaimed}
        />
      </div>
      {pagination && <ComponentDetailsFooter {...pagination} backToOffspringOnClick={backToOffspringOnClick} />}
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
  backToOffspringOnClick: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  pagination: PropTypes.shape(footerPropTypes),
  toggleShowMatchersPopover: PropTypes.func.isRequired,
};
