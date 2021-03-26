/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import {
  NxButton,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import { Messages } from '../../util/CommonServices';
import LegalApplicationDetailsComponentRow from './LegalApplicationDetailsComponentRow';

export default function LegalApplicationDetailsPage(props) {
  const {
    applicationPublicId,
    stageTypeId,
    application,
    stageType,
    components,
    loadApplication
  } = props;

  useEffect(() => {
    loadApplication(applicationPublicId, stageTypeId);
  }, [applicationPublicId, stageTypeId]);

  return (
    <main id="legal-application-details-container" className="nx-page-main nx-viewport-sized">
      <LoadWrapper loading={ application.loading || stageType.loading }
                   error={ application.error || stageType.error }
                   retryHandler={ () => loadApplication(applicationPublicId, stageTypeId) }>
        <div className="nx-page-title">
          <h1 className="nx-h1">{ application.name } Obligations</h1>
          <div className="nx-btn-bar">
            <NxButton variant="primary">Create Attribution Report</NxButton>
          </div>
          <div className="nx-page-title__description">
            <div className="nx-tile-header__subtitle">{ stageType.name } Stage</div>
          </div>
        </div>
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <NxTable id="legal-application-details-table" className="legal-dashboard-table" >
            <NxTableHead>
              <NxTableRow>
                <NxTableCell>Component</NxTableCell>
                <NxTableCell>Licenses</NxTableCell>
                <NxTableCell className="legal-application-details-table-review-progress">
                  Completed Obligations
                </NxTableCell>
                <NxTableCell className="legal-application-details-table-review-status">
                  Review Status
                </NxTableCell>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody emptyMessage="No components found"
                         isLoading={components.loading}
                         error={Messages.getHttpErrorMessage(components.error)}>
              {components.results.map((row, index) =>
                <LegalApplicationDetailsComponentRow key={index} row={row} />)
              }
            </NxTableBody>
          </NxTable>
        </div>
      </LoadWrapper>
    </main>
  );
}

LegalApplicationDetailsPage.propTypes = {
  applicationPublicId: PropTypes.string.isRequired,
  stageTypeId: PropTypes.string.isRequired,
  application: PropTypes.shape({
    name: PropTypes.string,
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error
  }),
  stageType: PropTypes.shape({
    name: PropTypes.string,
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error
  }),
  components: PropTypes.shape({
    results: PropTypes.arrayOf(LegalApplicationDetailsComponentRow.propTypes.row),
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error
  }),
  loadApplication: PropTypes.func.isRequired
};
