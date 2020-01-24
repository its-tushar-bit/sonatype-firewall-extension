/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import MaximizedContainer from '../react/MaximizedContainer';
import BackButton from '../react/BackButton';

// TODO after backend call is implemented: uncomment error handling
export default function ViolationPage({ $state, loadViolation, loading/*, error */ }) {
  const { id } = $state.params;

  useEffect(() => { load(); }, [id]);

  function load() {
    loadViolation(id);
  }

  return (
    <MaximizedContainer id="violation-page" className="nx-root-container">
      <aside className="nx-page-sidebar">
        <BackButton $state={$state} stateName="dashboard.overview.violations"/>
      </aside>
      <div className="nx-page-main">
        <LoadWrapper { ...({ loading/*, error*/ }) }>
          <div className="nx-tile">
            <div className="nx-tile-header">
              <div className="nx-tile-header__title">
                <h2 className="nx-h2">
                  Violation {id}
                </h2>
              </div>
            </div>
          </div>
        </LoadWrapper>
      </div>
    </MaximizedContainer>
  );
}

ViolationPage.propTypes = {
  $state: PropTypes.shape({
    params: PropTypes.shape({
      id: PropTypes.string.isRequired
    }).isRequired
  }).isRequired,
  loadViolation: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  error: LoadWrapper.propTypes.error
};
