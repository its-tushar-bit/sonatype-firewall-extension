/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqTooltip from './iqTooltip/iqTooltip';
import iqScrollToTop from './iqScrollToTop/iqScrollToTop';

export default angular.module('directives', [])
    .directive('iqTooltip', iqTooltip)
    .directive('iqScrollToTop', iqScrollToTop);
