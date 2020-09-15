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
    return repo.project.includes(projectFilter)
        && repo.namespace.includes(namespaceFilter)
        && repo.description.includes(descriptionFilter);
  }

  const [projectFilter, setProjectFilter] = useState(''),
      [namespaceFilter, setNamespaceFilter] = useState(''),
      [descriptionFilter, setDescriptionFilter] = useState('');

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
              <NxTableCell>Title</NxTableCell>
              <NxTableCell>Namespace</NxTableCell>
              <NxTableCell>Description</NxTableCell>
              <NxTableCell>Selected</NxTableCell>
            </NxTableRow>
            <NxTableRow>
              <NxTableCell>
                <NxTextInput
                    isPristine={false}
                    value={ projectFilter }
                    onChange={ newValue => setProjectFilter(newValue)} />
              </NxTableCell>
              <NxTableCell>
                <NxTextInput
                    isPristine={false}
                    value={ namespaceFilter }
                    onChange={ newValue => setNamespaceFilter(newValue)} />
              </NxTableCell>
              <NxTableCell>
                <NxTextInput
                    isPristine={false}
                    value={ descriptionFilter }
                    onChange={ newValue => setDescriptionFilter(newValue)} />
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody>
            { repositories.filter(repo => filterRepository(repo)).map(repo =>
              <NxTableRow key={repo.project}>
                <NxTableCell class="iq-scm-repository-project">{repo.project}</NxTableCell>
                <NxTableCell class="iq-scm-repository-namespace">{repo.namespace}</NxTableCell>
                <NxTableCell class="iq-scm-repository-description">{repo.description}</NxTableCell>
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
  repositories: PropTypes.arrayOf(repositoryPropType).isRequired
};
