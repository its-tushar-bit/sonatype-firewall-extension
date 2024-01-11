/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.UntargettedBinding;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequiredExplicitBindingModuleTest
{
  private RequiredExplicitBindingModule underTest;

  @Mock
  private Binder binder;

  @Mock
  private BannedImplementationService banned;

  @Test
  public void test_EnsureExplicitBindingsEnabled() {

    underTest = new RequiredExplicitBindingModule(Collections.emptyList(), null);
    underTest.configure(binder);

    verify(binder).requireExplicitBindings();
  }

  @Test
  public void test_WhenElementUntargettedAndBannedApplyToSkipped() {
    Module module = untargetedBindingModule();

    when(banned.isBanned(any())).thenReturn(true);
    underTest = new RequiredExplicitBindingModule(Arrays.asList(module), banned);

    List<Element> elements = Elements.getElements(underTest);
    assertThat(elements).doNotHave(
        new Condition<Element>(UntargettedBinding.class::isInstance, "instanceof UntargettedBinding")
    );

    verify(banned).isBanned(any());
  }

  @Test
  public void test_WhenElementUntargettedAndNotBannedApplyToCalled() {
    Module module = untargetedBindingModule();

    when(banned.isBanned(any())).thenReturn(false);
    underTest = new RequiredExplicitBindingModule(Arrays.asList(module), banned);

    List<Element> elements = Elements.getElements(underTest);

    assertThat(elements).hasAtLeastOneElementOfType(UntargettedBinding.class);

    UntargettedBinding<?> binding = (UntargettedBinding<?>)elements.stream()
        .filter(UntargettedBinding.class::isInstance)
        .findFirst()
        .get();

    assertThat(binding.getKey().getTypeLiteral().getRawType()).isEqualTo(Stub.class);
  }

  @Test
  public void test_WhenElementDefinedAsInterfaceAndBannedApplyToSkipped() {
    Module module = linkedKeyBindingModule();

    when(banned.isBanned(any())).thenReturn(true);
    underTest = new RequiredExplicitBindingModule(Arrays.asList(module), banned);

    List<Element> elements = Elements.getElements(underTest);
    assertThat(elements).doNotHave(
        new Condition<Element>(LinkedKeyBinding.class::isInstance, "instanceof LinkedKeyBinding")
    );

    verify(banned).isBanned(any());
  }

  @Test
  public void test_WhenElementDefinedAsInterfaceAndNotBannedApplyToCalled() {
    Module module = linkedKeyBindingModule();

    when(banned.isBanned(any())).thenReturn(false);
    underTest = new RequiredExplicitBindingModule(Arrays.asList(module), banned);

    List<Element> elements = Elements.getElements(underTest);

    assertThat(elements).hasAtLeastOneElementOfType(LinkedKeyBinding.class);

    LinkedKeyBinding<?> binding = (LinkedKeyBinding<?>)elements.stream()
        .filter(LinkedKeyBinding.class::isInstance)
        .findFirst()
        .get();

    assertThat(binding.getKey().getTypeLiteral().getRawType()).isEqualTo(StubInterface.class);
    assertThat(binding.getLinkedKey().getTypeLiteral().getRawType()).isEqualTo(Stub.class);
  }

  private Module untargetedBindingModule() {
    return new AbstractModule() {
      @Override
      public void configure() {
        bind(Stub.class);
      }
    };
  }

  private Module linkedKeyBindingModule() {
    return new AbstractModule() {
      @Override
      public void configure() {
        bind(StubInterface.class).to(Stub.class);
      }
    };
  }

  private interface StubInterface
  {
  }

  private static class Stub
      implements StubInterface
  {
  }
}
