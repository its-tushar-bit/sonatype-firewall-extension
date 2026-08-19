/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as ReactDOM from 'react-dom';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import SidebarNavViolationList, { SidebarViolationDataType } from './SidebarNavViolationList';
import SidebarNavWaiversList, {
  SidebarWaiverDetailsDataType,
  SidebarWaiverFilterDataType,
} from './SidebarNavWaiversList';
import { IQ_SIDEBAR_CONTAINER_ID } from 'MainRoot/util/constants';

export default function SidebarNavList(props) {
  const portalContainer = document.getElementById(IQ_SIDEBAR_CONTAINER_ID);

  const {
    loadSidebarNav,
    loading,
    error,
    gotoNewVulnerability,
    goToWaiverWithType,
    data,
    contentType,
    stateParams,
    prevParams,
    scrollToSelection,
  } = props;

  const { id, sidebarId, sidebarReference, type, waiverId } = stateParams;
  const { type: prevType } = prevParams;

  function load() {
    loadSidebarNav(stateParams);
  }

  function shouldSkipLoad(type, prevType) {
    return (type === 'waiver' && prevType === 'autoWaiver') || (type === 'autoWaiver' && prevType === 'waiver');
  }

  useEffect(() => {
    if (!shouldSkipLoad(type, prevType)) {
      load();
    }
  }, [sidebarId, sidebarReference, type]);

  const sidebarDisplayComponent = (function (contentType) {
    switch (contentType) {
      case 'violations':
        return (
          <SidebarNavViolationList
            currentViolationId={id}
            violations={data}
            onClick={gotoNewVulnerability}
            scrollToSelection={scrollToSelection}
          />
        );
      case 'waivers':
        return (
          <SidebarNavWaiversList
            currentWaiverId={waiverId}
            waivers={data}
            scrollToSelection={scrollToSelection}
            onClick={goToWaiverWithType}
          />
        );
      default:
        return null;
    }
  })(contentType);

  return ReactDOM.createPortal(
    <aside id="sidebar-nav-list" className="nx-page-sidebar nx-viewport-sized__container">
      <LoadWrapper error={error} loading={loading} retryHandler={load}>
        <h4 className="nx-h4">{contentType}</h4>
        <div className="nx-scrollable nx-scrollable--nav-list nx-viewport-sized__scrollable">
          {sidebarDisplayComponent}
        </div>
      </LoadWrapper>
    </aside>,
    portalContainer
  );
}

SidebarNavList.propTypes = {
  loadSidebarNav: PropTypes.func.isRequired,
  gotoNewVulnerability: PropTypes.func.isRequired,
  goToWaiverWithType: PropTypes.func.isRequired,
  contentType: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  error: LoadWrapper.propTypes.error,
  data: PropTypes.arrayOf(
    PropTypes.oneOfType([SidebarViolationDataType, SidebarWaiverDetailsDataType, SidebarWaiverFilterDataType])
  ),
  stateParams: PropTypes.shape({
    id: PropTypes.string,
    type: PropTypes.string,
    sidebarReference: PropTypes.string,
    sidebarId: PropTypes.string,
    ownerType: PropTypes.string,
    ownerId: PropTypes.string,
    waiverId: PropTypes.string,
  }).isRequired,
  prevParams: PropTypes.shape({
    id: PropTypes.string,
    type: PropTypes.string,
    sidebarReference: PropTypes.string,
    sidebarId: PropTypes.string,
    ownerType: PropTypes.string,
    ownerId: PropTypes.string,
    waiverId: PropTypes.string,
  }),
  scrollToSelection: PropTypes.bool.isRequired,
};
