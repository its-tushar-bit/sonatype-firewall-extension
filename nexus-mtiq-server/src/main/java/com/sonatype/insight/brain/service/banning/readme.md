<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Banning Implementations

To safeguard against accidental use of classes that should not be applicable to mtiq, the introduction of a banning
service has been added at the Guice and Sisu level.

## Why

As an example of why this is needed/useful, `InactiveRepositoryViolationCleaner` was a cleanup feature added to remove
inactive repository violations. Given that inactive repository violations are not stored since version 1.90 then we
can safely disable this feature for mtiq (see https://issues.sonatype.org/browse/CLM-14555 for further details).
To protect against the possibility of any data leakage by accidentally re-introducing this class or any other disabled
classes the desire is to disable them in such a way that mtiq will fail to start if any are accidentally used.
Adding a simple feature toggle is felt insufficient (although may compliment this solution) because accidental use
of classes for other features is always a possibility.

## Details

Guice tries to be clever and for anything that is required to be instantiated at startup it will look in the class
loader and attempt to find a suitable candidate. This is regardless of whether the class is explicitly bound in a 
module. Guice terms this
as [JustInTime/JIT](https://github.com/google/guice/wiki/JustInTimeBindings) binding.

In the majority of circumstances JIT is a useful and required feature and is still made use of for mtiq. 
`RequiredExplicitBindingModule` takes all elements from all the modules that are needed for IQ and iterates over them 
all and calls `applyTo`. With the exception of banned classes, `applyTo` essentially invokes JIT if not explicitly
bound to ensure their requirements are satisfied. The remaining items are skipped and `explicitBinding` is enabled to
prevent the JIT from binding against these banned classes. `IgnoreBannedImplementationStrategy` applies the same logic 
and is used to prevent Sisu from binding the classes annotated with `@Named`.

What may happen is a developer unsuspectingly makes use of a class which wanted to be explicitly disabled. Feature
flagging a resource / service at the top level does not prevent accidental misuse of classes/subclasses and thus
potential data leakage.
Banning at the Guice/Sisu level protects against misuse and mtiq will fail to start

## How

See `BannedImplementationService` holds a list of `BannedImplementation`'s. These `BannedImplementations`'s can be at
the class level or package level