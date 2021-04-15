/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';

import DeleteWaiverModal from './DeleteWaiverModal';
import { deleteWaiver, hideDeleteWaiverModal } from '../waiverActions';

const mapDispatchToProps = { deleteWaiver, hideDeleteWaiverModal };

const mapStateToProps = ({ deleteWaiver }) =>
  pick(['deleteWaiverSaving', 'deleteWaiverError', 'waiverToDelete'], deleteWaiver);

const DeleteWaiverModalContainer = connect(mapStateToProps, mapDispatchToProps)(DeleteWaiverModal);
export default DeleteWaiverModalContainer;
