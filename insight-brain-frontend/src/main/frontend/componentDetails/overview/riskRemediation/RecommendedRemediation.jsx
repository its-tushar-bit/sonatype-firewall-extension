/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { AncestorsList } from './AncestorsList';
import { NxInfoAlert } from '@sonatype/react-shared-components';

export const RecommendedRemediation = ({
  dependencyTreeSubset,
  expanded,
  toggleAncestorsList,
  dependencyTreeIsNotSupported,
}) => {
  return (
    <section className="iq-dependency-information nx-grid-col__section" data-testid="iq-dependency-information">
      <header className="nx-grid-header">
        <h3 className="nx-h3 nx-grid-header__title">Recommended Remediation</h3>
      </header>
      <div className="iq-grid-content">
        <p className="nx-p">
          The direct dependencies that brought in this component are listed below. Clicking on a component will take you
          to its Component Details Page.
        </p>
        {dependencyTreeSubset?.length > 0 ? (
          <AncestorsList
            dependencyTreeSubset={dependencyTreeSubset}
            toggleAncestorsList={toggleAncestorsList}
            itemsToShow={3}
            expanded={expanded}
          />
        ) : (
          <NxInfoAlert className="iq-dependency-information__unavailable-tree-alert">
            {dependencyTreeIsNotSupported
              ? 'Dependency info not available for this report. Please re-scan the application.'
              : 'Dependency info not available for this report.'}
          </NxInfoAlert>
        )}
      </div>
    </section>
  );
};

RecommendedRemediation.propTypes = {
  ...AncestorsList.PropTypes,
  dependencyTreeIsNotSupported: PropTypes.bool,
};
