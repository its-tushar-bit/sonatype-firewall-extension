/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import { saveFilter, cancelSaveFilter } from './manageLegalFiltersActions';
import { Messages } from '../../../utilAngular/CommonServices';
import SaveFilterModalContent from '../../../dashboard/filter/saveFilterModal/SaveFilterModalContent';

const mapDispatchToProps = {
  saveFilter,
  cancelSaveFilter,
};

function mapStateToProps({ manageLegalFilters }) {
  return {
    ...pick(
      ['appliedFilterName', 'existingDuplicateFilterName', 'saveFilterMaskState', 'saveFilterWarning'],
      manageLegalFilters
    ),
    saveError: Messages.getHttpErrorMessage(manageLegalFilters.saveFilterError),
  };
}

const SaveLegalFilterModalContainer = connect(mapStateToProps, mapDispatchToProps)(SaveFilterModalContent);
export default SaveLegalFilterModalContainer;
