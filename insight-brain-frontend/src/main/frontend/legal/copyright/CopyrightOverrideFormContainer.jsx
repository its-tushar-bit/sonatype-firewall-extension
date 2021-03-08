/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {connect} from 'react-redux';
import CopyrightOverrideForm from './CopyrightOverrideForm';
import {saveCopyrightOverride, setDisplayCopyrightOverrideModal} from './copyrightOverrideFormActions';

const mapDispatchToProps = {
  saveCopyrightOverride,
  setDisplayCopyrightOverrideModal
};

function mapStateToProps({advancedLegal, copyrightOverrides}) {
  return {
    availableScopes: advancedLegal.availableScopes,
    component: advancedLegal.component.component,
    ...copyrightOverrides
  };
}

const CopyrightOverrideFormContainer = connect(mapStateToProps, mapDispatchToProps)(CopyrightOverrideForm);
export default CopyrightOverrideFormContainer;
