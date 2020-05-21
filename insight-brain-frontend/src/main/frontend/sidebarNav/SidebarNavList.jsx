/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect} from 'react';
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
    $state
  } = props;
  const { params: stateParams } = $state;

  useEffect(load, [stateParams.type, stateParams.sidebarReference, stateParams.sidebarId]);

  function load() {
    loadSidebarNav(stateParams);
  }

  const sidebarDisplayComponent = (function(contentType) {
    switch (contentType) {
      case 'violations':
        return <SidebarNavViolationList currentViolationId={stateParams.id}
                                        violations={data}
                                        onClick={ gotoNewVulnerability } />;
      default:
        return null;
    }
  }(contentType));

  return (
    <aside className="nx-page-sidebar nx-page-scrollbar--violations-list">
      { backButtonStateName && <BackButton $state={$state} stateName={backButtonStateName} /> }
      <LoadWrapper error={error} loading={loading}>
        <div id="sidebar-nav-list" className="nx-list nx-list--clickable">
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
  $state: PropTypes.shape({
    params: PropTypes.shape({
      id: PropTypes.string.isRequired,
      type: PropTypes.string,
      sidebarReference: PropTypes.string,
      sidebarId: PropTypes.string
    }).isRequired
  }).isRequired,
  backButtonStateName: PropTypes.string,
  contentType: PropTypes.string.isRequired,
  loading: PropTypes.bool.isRequired,
  error: LoadWrapper.propTypes.error,
  data: PropTypes.arrayOf(
      PropTypes.shape({
        policyName: PropTypes.string.isRequired,
        policyViolationId: PropTypes.string.isRequired,
        threatLevel: PropTypes.number.isRequired
      })
  )
};
