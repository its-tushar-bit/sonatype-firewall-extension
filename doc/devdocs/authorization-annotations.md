<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Usage of the @Authorize Annotation

## When to use authorization annotation

Whenever we want to restrict a method call to an authenticated user that has a specific permission over the resource we are operating over; this is usually a resource that can be classified as an Owner, i.e, implements the Owner interface in code (Application, Organization, Repository, etc)

## Which permissions can I check

The list of available permissions resides in the `Permission` class of the `insight-brain-data` module in the `com.sonatype.insight.brain.model.security`package. This class specifies which permissions are global and other characteristics.

## Important notes on the use of the annotation

- Always verify the method call with a user that only has the set of permissions that you are expecting and nothing extra. Testing with the built-in admin is not enough to verify this functionality.

- Always pair the `@Authorize` annotation with at least one `@AuthzContext` annotation on the parameter that represents the Owner when you are trying to verify a specific permission for an Owner. Failing to do so can render your method incapable of being called by any user beyond the built-in admin.

- Authorizations for global permissions like Permission.CONFIGURE_SYSTEM can be used without an `@AuthzContext`.

- The method on which you are using the annotation must be at least a package private (no access modifier) for it to actually perform the authorization check.

- The method should be unit tested not only in its functionality but also in its authorization behaviour. This is usually done by creating an "Authz" test that verifies the correct exceptions are thrown (or not) when the method is called by a user that is authorized or not authorized. This typically also includes testing if the user is authenticated or not, which is done through a different mechanism to the @Authorize annotation.

## On using @AuthzContext

Consider the context of the parameter you want to use to identify the context of the the authorization check.
Several values can be used from the `Key` enumeration defined on `AuthzContext`.

If you have an id (usually `ownerId`) but are not sure which entity it belongs to, always pair it with an additional `type` (usually `ownerType`) parameter and use the context annotation on both.

## Examples

- Restrict the method to a user with permissions to write on a particular application with known id.

<pre>
<code>
  @Authorize(permission = Permission.WRITE)
  public void method(@AuthzContext(Key.APPLICATION_ID) String appInternalId, String scanId) {};
</code>
</pre>
<br>

- Restrict the method to a user with permissions to write to 2 `Owners` in the same method. In this case, the method will have to be partitioned as the `@Authorize` annotation can only work with one `@AuthzContext` at a time.

<pre>
<code>
  @Authorize(permission = Permission.WRITE)
  public ApiMoveApplicationResponseDTOV2 moveApplication(
      @AuthzContext(Key.APPLICATION_ID) String applicationId,
      String organizationId)
  {
    return moveApplication(application, organizationId);
  }

  @Authorize(permission = Permission.WRITE)
  List<String> moveApplication(
      Application application, 
      @AuthzContext(AuthzContext.Key.ORGANIZATION_ID) String organizationId) {};
</code>
</pre>
<br>

- Restrict the method to a user with permissions to an Owner with a known id and type.

<pre>
<code>
 @Authorize(permission = Permission.READ)
 public AppliedLabels getComponentLabels(
      @AuthzContext(AuthzContext.Key.TYPE) final OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId,
      final String hash)
</code>
</pre>
<br>

- Restrict the method to a user with permissions to an Owner while having the owner object as a parameter.

<pre>
<code>
 @Authorize(permission = Permission.EVALUATE_COMPONENT)
 void checkEvaluateComponentPermission(@SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.OWNER) Owner owner) {
  // Do nothing as this method is only used to perform authz check for the caller
 }
</code>
</pre>
<br>

- Restrict the method to a user with the CONFIGURE_SYSTEM Permission. Since this is a global permission, it does not need an `@AuthzContext`.

<pre>
<code>
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void disableFeature(String feature) {};
</code>
</pre>
