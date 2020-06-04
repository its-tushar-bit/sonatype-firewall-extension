/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import BackButton from '../react/BackButton';
import MaximizedContainer from '../react/MaximizedContainer';
import LoadWrapper from '../react/LoadWrapper';
import SidebarNavViolationList from './SidebarNavViolationList';

export default function SidebarNavList(props) {
  const {
    loadSidebarNav,
    loading,
    error,
    gotoNewVulnerability,
    data,
    contentType,
    backButtonStateName,
    $state,
    stateParams,
    scrollToSelection
  } = props;

  const { id, sidebarId, sidebarReference, type } = stateParams;

  useEffect(() => {
    loadSidebarNav(stateParams);
  }, [sidebarId, sidebarReference, type]);

  const sidebarDisplayComponent = (function(contentType) {
    switch (contentType) {
      case 'violations':
        return <SidebarNavViolationList currentViolationId={id}
                                        violations={data}
                                        onClick={gotoNewVulnerability}
                                        scrollToSelection={scrollToSelection} />;
      default:
        return null;
    }
  }(contentType));

  return (
    <aside className="nx-page-scrollbar nx-page-scrollbar--violations-list">
      { backButtonStateName && <BackButton $state={$state} stateName={backButtonStateName} /> }
      <LoadWrapper error={error} loading={loading}>
        <div id="sidebar-nav-list" className="nx-list nx-list--clickable sidebar-nav-list">
          <h4 className="nx-list__title">
            {contentType}
          </h4>
          <MaximizedContainer className="nx-scrollable nx-scrollable--violations-list">
            { sidebarDisplayComponent }
          </MaximizedContainer>
        </div>
      </LoadWrapper>
    </aside>
  );
}

SidebarNavList.propTypes = {
  loadSidebarNav: PropTypes.func.isRequired,
  gotoNewVulnerability: PropTypes.func.isRequired,
  $state: BackButton.propTypes.$state,
  backButtonStateName: PropTypes.string,
  contentType: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  error: LoadWrapper.propTypes.error,
  data: PropTypes.arrayOf(
      PropTypes.shape({
        policyName: PropTypes.string.isRequired,
        policyViolationId: PropTypes.string.isRequired,
        threatLevel: PropTypes.number.isRequired
      })
  ),
  stateParams: PropTypes.shape({
    id: PropTypes.string,
    type: PropTypes.string,
    sidebarReference: PropTypes.string,
    sidebarId: PropTypes.string
  }).isRequired,
  scrollToSelection: PropTypes.bool.isRequired
};
