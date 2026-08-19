/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import DeleteFilterModal from './DeleteFilterModal';
import { deleteFilter, hideDeleteFilterModal } from '../manageFiltersActions';

const mapDispatchToProps = {
  deleteFilter,
  hideDeleteFilterModal,
};

const mapStateToProps = ({ manageFilters }) =>
  pick(['filterToDelete', 'deleteFilterError', 'deleteFilterMaskState'], manageFilters);

const DeleteFilterModalContainer = connect(mapStateToProps, mapDispatchToProps)(DeleteFilterModal);
export default DeleteFilterModalContainer;
