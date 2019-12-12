/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { pick } from 'ramda';
import {connect} from 'react-redux';
import * as PropTypes from 'prop-types';
import SaveFilterModalContent from './SaveFilterModalContent';

export default function SaveFilterModalContainer({ manageFiltersActions, dashboardFilterActions, Messages }) {

  const mapStateToProps = ({ manageFilters }) => ({
    ...pick(['savedFilters',
      'appliedFilterName',
      'saveFilterSaving',
      'saveFilterSuccess'
    ], manageFilters),
    saveError: Messages.getHttpErrorMessage(manageFilters.saveFilterError)
  });

  const mapDispatchToProps = {
    ...pick(['setDisplaySaveFilterModal'], dashboardFilterActions),
    ...pick(['saveFilter'], manageFiltersActions)
  };

  const ConnectedSaveFilterModalPage = connect(mapStateToProps, mapDispatchToProps)(SaveFilterModalContent);

  return (
    <ConnectedSaveFilterModalPage />
  );
}

SaveFilterModalContainer.propTypes = {
  manageFiltersActions: PropTypes.shape({
    saveFilter: PropTypes.func
  }),
  dashboardFilterActions: PropTypes.shape({
    setDisplaySaveFilterModal: PropTypes.func.isRequired
  }),
  Messages: PropTypes.shape({
    getHttpErrorMessage: PropTypes.func.isRequired
  })
};
