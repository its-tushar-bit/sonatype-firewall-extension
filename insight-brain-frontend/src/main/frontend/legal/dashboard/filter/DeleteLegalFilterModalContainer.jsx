/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import { deleteFilter, hideDeleteFilterModal } from './manageLegalFiltersActions';
import DeleteFilterModal from '../../../dashboard/filter/deleteFilterModal/DeleteFilterModal';

const mapDispatchToProps = {
  deleteFilter,
  hideDeleteFilterModal,
};

const mapStateToProps = ({ manageLegalFilters }) =>
  pick(['filterToDelete', 'deleteFilterError', 'deleteFilterMaskState'], manageLegalFilters);

const DeleteLegalFilterModalContainer = connect(mapStateToProps, mapDispatchToProps)(DeleteFilterModal);
export default DeleteLegalFilterModalContainer;
