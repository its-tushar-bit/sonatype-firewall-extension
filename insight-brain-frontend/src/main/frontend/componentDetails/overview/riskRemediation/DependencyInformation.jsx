/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { AncestorsList } from './AncestorsList';

export const DependencyInformation = ({ ancestors, ancestorOnClick }) => {
  return (
    <section className="iq-dependency-information nx-tile">
      <header className="nx-tile-header">
        <h3 className="nx-h3 nx-tile-header__title">Dependency Information</h3>
      </header>
      <div className="nx-tile-content">
        <p className="nx-p">
          This dependency was brought in by the component(s) listed below. Clicking on a component will take you to its
          Component Details Page.
        </p>
        <AncestorsList ancestors={ancestors} ancestorOnClick={ancestorOnClick} />
      </div>
    </section>
  );
};

DependencyInformation.propTypes = {
  ...AncestorsList.PropTypes,
  ancestorOnClick: PropTypes.func.isRequired,
};
