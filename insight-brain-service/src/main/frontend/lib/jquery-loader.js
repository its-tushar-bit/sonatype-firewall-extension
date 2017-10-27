/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Load jquery and IMMEDIATELY bind it to the typical global variables.
 * This allows the global bindings to be set up before other imports after this one are processed
 * See https://github.com/rollup/rollup/issues/592#issuecomment-205783255
 */
import $ from 'jquery';

window.$ = window.jQuery = $;
