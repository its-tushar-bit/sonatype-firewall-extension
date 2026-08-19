/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import Claim from './Claim';
import { actions } from './claimSlice';
import { selectComponentDetailsClaimSlice, selectClaimId } from './claimSelectors';

function mapStateToProps(state) {
  return {
    ...selectComponentDetailsClaimSlice(state),
    claimId: selectClaimId(state),
  };
}

const mapDispatchToProps = { ...actions };

export const ClaimContainer = connect(mapStateToProps, mapDispatchToProps)(Claim);
