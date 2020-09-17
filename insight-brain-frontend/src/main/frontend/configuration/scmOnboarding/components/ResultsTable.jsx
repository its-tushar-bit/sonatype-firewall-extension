/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import LoadWrapper from '../../../react/LoadWrapper';
import {
  NxCheckbox,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import React, {Fragment, useState} from 'react';
import * as PropTypes from 'prop-types';
import NxTextInput from '@sonatype/react-shared-components/components/NxTextInput/NxTextInput';
import {repositoryPropType} from '../ScmOnboarding';

export default function ResultsTable(props) {

  const {
    loadingRepositories,
    repositories
  } = props;

  function filterRepository(repo) {
    return repo.httpCloneUrl.includes(urlFilter);
  }

  const [urlFilter, setUrlFilter] = useState('');

  return (
    <Fragment>
      <div className="iq-tile-header">
        <div className="iq-tile-header__title">
          <h2>Results</h2>
        </div>
      </div>
      <LoadWrapper loading={loadingRepositories}>
        <NxTable id="iq-scm-onboarding-repositories">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell>URL</NxTableCell>
              <NxTableCell>Selected</NxTableCell>
            </NxTableRow>
            <NxTableRow>
              <NxTableCell>
                <NxTextInput
                    isPristine={false}
                    value={ urlFilter }
                    onChange={ newValue => setUrlFilter(newValue)} />
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody>
            { repositories.filter(repo => filterRepository(repo)).map(repo =>
              <NxTableRow key={repo.httpCloneUrl}>
                <NxTableCell className="iq-scm-repository-url">{repo.httpCloneUrl}</NxTableCell>
                <NxTableCell><NxCheckbox isChecked={false} /></NxTableCell>
              </NxTableRow>
            )}
          </NxTableBody>
        </NxTable>
      </LoadWrapper>
    </Fragment>
  );
}

ResultsTable.propTypes = {
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired
};
