/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import OriginalSourcesForm from './OriginalSourcesForm';
import { setObligationScope, setObligationStatus } from '../obligation/advancedLegalObligationActions';
import { saveOriginalSourcesOverride, setDisplayOriginalSourcesOverrideModal } from './originalSourcesFormActions';

const mapDispatchToProps = {
  saveOriginalSourcesOverride,
  setDisplayOriginalSourcesOverrideModal,
  setObligationScope,
  setObligationStatus,
};

function mapStateToProps({ advancedLegal, originalSourcesForm }) {
  return {
    availableScopes: advancedLegal.availableScopes,
    component: advancedLegal.component.component,
    existingObligation: advancedLegal.component.component.licenseLegalData.obligations.find(
      (o) => o.name === 'Required Disclosure of Original Source Code with Distribution'
    ),
    ...originalSourcesForm,
  };
}

const OriginalSourcesFormContainer = connect(mapStateToProps, mapDispatchToProps)(OriginalSourcesForm);
export default OriginalSourcesFormContainer;
