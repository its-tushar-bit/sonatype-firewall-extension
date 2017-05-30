/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqCheckbox from './iqCheckbox/iqCheckbox';
import iqRadio from './iqRadio/iqRadio';

export default angular.module('components', [])
    .component('iqCheckbox', iqCheckbox)
    .component('iqRadio', iqRadio);

