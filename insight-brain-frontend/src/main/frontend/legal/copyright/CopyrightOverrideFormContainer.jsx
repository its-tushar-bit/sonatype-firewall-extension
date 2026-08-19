/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import CopyrightOverrideForm from './CopyrightOverrideForm';
import { saveCopyrightOverride, setDisplayCopyrightOverrideModal } from './copyrightOverrideFormActions';
import { setObligationScope, setObligationStatus } from '../obligation/advancedLegalObligationActions';

const mapDispatchToProps = {
  saveCopyrightOverride,
  setDisplayCopyrightOverrideModal,
  setObligationScope,
  setObligationStatus,
};

function mapStateToProps({ advancedLegal, copyrightOverrides }) {
  return {
    availableScopes: advancedLegal.availableScopes,
    component: advancedLegal.component.component,
    existingObligation: advancedLegal.component.component.licenseLegalData.obligations.find(
      (o) => o.name === 'Inclusion of Copyright'
    ),
    ...copyrightOverrides,
  };
}

const CopyrightOverrideFormContainer = connect(mapStateToProps, mapDispatchToProps)(CopyrightOverrideForm);
export default CopyrightOverrideFormContainer;
