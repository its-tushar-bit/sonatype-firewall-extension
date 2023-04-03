/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;

public class BaseUrlConfigurationDeleteModal
    extends BasicElement<BaseUrlConfigurationDeleteModal>
{
  public BaseUrlConfigurationDeleteModal() {
    super("#base-url-config-delete-modal");
  }

  public Button okButton() {
    return new Button(childSelector(".nx-footer .nx-btn-bar .nx-btn--primary"));
  }

  public Button cancelButton() {
    return new Button(childSelector(".nx-footer .nx-btn-bar .nx-btn--secondary"));
  }
}
