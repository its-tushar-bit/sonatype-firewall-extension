/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { AncestorsList } from './AncestorsList';

export const RecommendedRemediation = ({ ancestors, ancestorOnClick, expanded, toggleAncestorsList }) => {
  return (
    <section className="iq-dependency-information nx-grid-col__section">
      <header className="nx-grid-header">
        <h3 className="nx-h3 nx-grid-header__title">Recommended Remediation</h3>
      </header>
      <div className="iq-grid-content">
        <p className="nx-p">
          This dependency was brought in by the component(s) listed below. Clicking on a component will take you to its
          Component Details Page.
        </p>
        <AncestorsList
          ancestors={ancestors}
          ancestorOnClick={ancestorOnClick}
          toggleAncestorsList={toggleAncestorsList}
          itemsToShow={3}
          expanded={expanded}
        />
      </div>
    </section>
  );
};

RecommendedRemediation.propTypes = {
  ...AncestorsList.PropTypes,
  ancestorOnClick: PropTypes.func.isRequired,
};
