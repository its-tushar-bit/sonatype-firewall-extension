/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { NxInfoAlert, NxButton } from '@sonatype/react-shared-components';

import MaximizedContainer from '../react/MaximizedContainer';

export default function AddWaiverPage(props) {
  const {
    addWaiver,
    stateParams: { policyViolationId }
  } = props;

  const warningText = 'You are currently viewing a temporary page for adding waivers. ';
  const policyViolationText = `The current policyViolationId associated with this page is ${policyViolationId}`;

  return (
    <MaximizedContainer id="timed-waivers" className="nx-page-content">
      { /** ToDo: replace with actual content */ }
      <div className="nx-page-main">
        <div className="nx-page-title">
          <h1 className="nx-h1">Add Waiver</h1>
        </div>

        <div className="nx-tile iq-timed-waivers">
          <NxInfoAlert id="temporary-content">
            <span>
              { warningText }
              { policyViolationId && policyViolationText }
            </span>
          </NxInfoAlert>
          <NxButton id="temporary-button"
                    onClick={() => addWaiver()}>
            Mock Add Waiver
          </NxButton>
        </div>
      </div>
    </MaximizedContainer>
  );
}

AddWaiverPage.propTypes = {
  addWaiver: PropTypes.func.isRequired,
  stateParams: PropTypes.shape({
    policyViolationId: PropTypes.string
  }).isRequired
};
