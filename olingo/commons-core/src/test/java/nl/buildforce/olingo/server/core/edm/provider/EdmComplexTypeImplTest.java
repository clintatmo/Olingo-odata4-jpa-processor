/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.edm.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import nl.buildforce.olingo.commons.api.edm.EdmComplexType;
import nl.buildforce.olingo.commons.api.edm.EdmElement;
import nl.buildforce.olingo.commons.api.edm.EdmException;
import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlComplexType;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlEdmProvider;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlProperty;
import nl.buildforce.olingo.commons.core.edm.EdmComplexTypeImpl;
import nl.buildforce.olingo.commons.core.edm.EdmProviderImpl;
import org.junit.Before;
import org.junit.Test;

public class EdmComplexTypeImplTest {

  private EdmComplexType baseType;

  private EdmComplexType type;

  @Before
  public void setupTypes() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    EdmProviderImpl edm = new EdmProviderImpl(provider);

    FullQualifiedName baseName = new FullQualifiedName("namespace", "BaseTypeName");
    CsdlComplexType baseComplexType = new CsdlComplexType();
    List<CsdlProperty> baseProperties = new ArrayList<>();
    baseProperties.add(new CsdlProperty().setName("prop1").setType(
        EdmPrimitiveTypeKind.String.getFullQualifiedName()));
    List<CsdlNavigationProperty> baseNavigationProperties = new ArrayList<>();
    baseNavigationProperties.add(new CsdlNavigationProperty().setName("nav1"));
    baseComplexType.setName("BaseTypeName").setAbstract(false).setOpenType(false).setProperties(baseProperties)
        .setNavigationProperties(baseNavigationProperties);
    when(provider.getComplexType(baseName)).thenReturn(baseComplexType);

    baseType = new EdmComplexTypeImpl(edm, baseName, baseComplexType);

    FullQualifiedName name = new FullQualifiedName("namespace", "typeName");
    CsdlComplexType complexType = new CsdlComplexType().setBaseType(baseName);
    List<CsdlProperty> properties = new ArrayList<>();
    properties.add(new CsdlProperty().setName("prop2").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()));
    List<CsdlNavigationProperty> navigationProperties = new ArrayList<>();
    navigationProperties.add(new CsdlNavigationProperty().setName("nav2"));
    complexType.setName("BaseTypeName").setAbstract(false).setOpenType(false).setProperties(properties)
        .setNavigationProperties(navigationProperties);
    when(provider.getComplexType(name)).thenReturn(complexType);

    type = new EdmComplexTypeImpl(edm, name, complexType);
  }

  @Test
  public void noPropertiesAndNoNavPropertiesMustNotResultInException() {
    EdmProviderImpl edm = mock(EdmProviderImpl.class);
    CsdlComplexType complexType = new CsdlComplexType().setName("n");
    new EdmComplexTypeImpl(edm, new FullQualifiedName("n", "n"), complexType);
  }

  @Test
  public void typeMustBeCompatibletoBasetype() {
    assertTrue(type.compatibleTo(baseType));
  }

  @Test
  public void baseTypeMustNotBeCompatibleToType() {
    assertFalse(baseType.compatibleTo(type));
  }

  @Test(expected = EdmException.class)
  public void nullForCompatibleTypeMustResultInEdmException() {
    assertFalse(type.compatibleTo(null));
  }

  @Test
  public void getBaseType() {
    assertNull(baseType.getBaseType());
    assertNotNull(type.getBaseType());
  }

  @Test
  public void propertiesBehaviour() {
    List<String> propertyNames = baseType.getPropertyNames();
    assertEquals(1, propertyNames.size());
    assertEquals("prop1", baseType.getProperty("prop1").getName());
  }

  @Test
  public void propertiesBehaviourWithBaseType() {
    List<String> propertyNames = type.getPropertyNames();
    assertEquals(2, propertyNames.size());
    assertEquals("prop1", type.getProperty("prop1").getName());
    assertEquals("prop2", type.getProperty("prop2").getName());
  }

  @Test
  public void navigationPropertiesBehaviour() {
    List<String> navigationPropertyNames = baseType.getNavigationPropertyNames();
    assertEquals(1, navigationPropertyNames.size());
    assertEquals("nav1", baseType.getProperty("nav1").getName());
  }

  @Test
  public void navigationPropertiesBehaviourWithBaseType() {
    List<String> navigationPropertyNames = type.getNavigationPropertyNames();
    assertEquals(2, navigationPropertyNames.size());
    assertEquals("nav1", type.getProperty("nav1").getName());
    assertEquals("nav2", type.getProperty("nav2").getName());
  }

  @Test
  public void propertyCaching() {
    EdmElement property = type.getProperty("prop1");
    assertSame(property, type.getProperty("prop1"));

    property = type.getProperty("prop2");
    assertSame(property, type.getProperty("prop2"));

    property = type.getProperty("nav1");
    assertSame(property, type.getProperty("nav1"));

    property = type.getProperty("nav2");
    assertSame(property, type.getProperty("nav2"));
  }

  @Test(expected = EdmException.class)
  public void nonExistingBaseType() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    EdmProviderImpl edm = new EdmProviderImpl(provider);
    FullQualifiedName typeWithNonexistingBaseTypeName = new FullQualifiedName("namespace", "typeName");
    CsdlComplexType complexTypeForNonexistingBaseType =
        new CsdlComplexType().setBaseType(new FullQualifiedName("wrong", "wrong"));
    complexTypeForNonexistingBaseType.setName("typeName");
    when(provider.getComplexType(typeWithNonexistingBaseTypeName)).thenReturn(complexTypeForNonexistingBaseType);
    EdmComplexTypeImpl instance =
        new EdmComplexTypeImpl(edm, typeWithNonexistingBaseTypeName, complexTypeForNonexistingBaseType);
    instance.getBaseType();
  }

}