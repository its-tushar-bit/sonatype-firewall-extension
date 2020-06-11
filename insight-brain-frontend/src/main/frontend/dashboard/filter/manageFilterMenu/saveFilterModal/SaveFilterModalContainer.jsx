/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import SaveFilterModalContent from './SaveFilterModalContent';
import { saveFilter } from '../../manageFiltersActions';
import { setDisplaySaveFilterModal } from '../../dashboardFilterActions';
import { Messages } from '../../../../util/CommonServices';

const mapDispatchToProps = {
  saveFilter,
  setDisplaySaveFilterModal
};

function mapStateToProps({ manageFilters }) {
  return {
    ...pick(['savedFilters',
      'appliedFilterName',
      'saveFilterSaving',
      'saveFilterSuccess'
    ], manageFilters),
    saveError: Messages.getHttpErrorMessage(manageFilters.saveFilterError)
  };
}

const SaveFilterModalContainer = connect(mapStateToProps, mapDispatchToProps)(SaveFilterModalContent);
export default SaveFilterModalContainer;
