/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.edm.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.buildforce.olingo.commons.api.edm.Edm;
import nl.buildforce.olingo.commons.api.edm.EdmComplexType;
import nl.buildforce.olingo.commons.api.edm.EdmEntityType;
import nl.buildforce.olingo.commons.api.edm.EdmEnumType;
import nl.buildforce.olingo.commons.api.edm.EdmException;
import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveType;
import nl.buildforce.olingo.commons.api.edm.EdmTerm;
import nl.buildforce.olingo.commons.api.edm.EdmTypeDefinition;
import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;
import nl.buildforce.olingo.commons.api.edm.TargetType;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlAnnotation;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlEdmProvider;
import nl.buildforce.olingo.commons.api.edm.provider.CsdlTerm;
import nl.buildforce.olingo.commons.core.edm.EdmProviderImpl;
import nl.buildforce.olingo.commons.core.edm.EdmTermImpl;
import org.junit.Before;
import org.junit.Test;

public class EdmTermImplTest {

  private EdmTerm initialTerm;
  private EdmTerm derivedTerm;

  @Test
  public void initialTerm() {
    EdmTerm term = new EdmTermImpl(mock(Edm.class), "namespace", new CsdlTerm());

    assertTrue(term.isNullable());
    assertNull(term.getName());
    assertNull(term.getMaxLength());
    assertNull(term.getPrecision());
    assertNull(term.getScale());
    // assertNull(term.getSrid());
    assertNotNull(term.getAnnotations());
    assertTrue(term.getAnnotations().isEmpty());

    assertNotNull(term.getFullQualifiedName());
    try {
      term.getType();
      fail("EdmException expected");
    } catch (EdmException e) {
      assertEquals("Terms must hava a full qualified type.", e.getMessage());
    }
  }

  @Before
  public void setupTypes() throws Exception {
    CsdlEdmProvider provider = mock(CsdlEdmProvider.class);
    EdmProviderImpl edm = new EdmProviderImpl(provider);

    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    csdlTerm.setType("Edm.String");
    when(provider.getTerm(csdlTerm1Name)).thenReturn(csdlTerm);
    initialTerm = new EdmTermImpl(edm, "namespace", csdlTerm);

    CsdlTerm derivedCsdlTerm = new CsdlTerm();
    FullQualifiedName derivedTermName = new FullQualifiedName("namespace", "name2");
    derivedCsdlTerm.setName(derivedTermName.getName());
    derivedCsdlTerm.setType("Edm.String");
    derivedCsdlTerm.setBaseTerm("namespace.name1");
    List<String> appliesTo = new ArrayList<String>();
    appliesTo.add("Property");
    derivedCsdlTerm.setAppliesTo(appliesTo);
    List<CsdlAnnotation> csdlAnnotations = new ArrayList<CsdlAnnotation>();
    csdlAnnotations.add(new CsdlAnnotation().setTerm("name1"));
    derivedCsdlTerm.setAnnotations(csdlAnnotations);

    derivedCsdlTerm.setNullable(false);
    derivedCsdlTerm.setMaxLength(15);
    derivedCsdlTerm.setDefaultValue("abc");
    derivedCsdlTerm.setPrecision(14);
    derivedCsdlTerm.setScale(13);

    when(provider.getTerm(derivedTermName)).thenReturn(derivedCsdlTerm);
    derivedTerm = new EdmTermImpl(edm, "namespace", derivedCsdlTerm);

  }

  @Test
  public void termBasics() throws Exception {
    assertEquals("name1", initialTerm.getName());
    assertEquals(new FullQualifiedName("namespace", "name1"), initialTerm.getFullQualifiedName());

    assertTrue(initialTerm.getAnnotations().isEmpty());
    assertTrue(initialTerm.getAppliesTo().isEmpty());
    assertNull(initialTerm.getBaseTerm());

    EdmPrimitiveType type = (EdmPrimitiveType) initialTerm.getType();
    assertEquals(type.getName(), "String");

    // initial facets
    assertTrue(initialTerm.isNullable());
    assertNull(initialTerm.getDefaultValue());
    assertNull(initialTerm.getMaxLength());
    assertNull(initialTerm.getPrecision());
    assertNull(initialTerm.getScale());
    // assertNull(initialTerm.getSrid());
  }

  @Test
  public void derivedTermTest() {
    assertEquals("name2", derivedTerm.getName());
    assertEquals(new FullQualifiedName("namespace", "name2"), derivedTerm.getFullQualifiedName());

    assertNotNull(derivedTerm.getBaseTerm());
    assertEquals("name1", derivedTerm.getBaseTerm().getName());
    assertFalse(derivedTerm.getAnnotations().isEmpty());
    assertEquals(1, derivedTerm.getAnnotations().size());
    assertFalse(derivedTerm.getAppliesTo().isEmpty());
    assertEquals(TargetType.Property, derivedTerm.getAppliesTo().get(0));

    EdmPrimitiveType type = (EdmPrimitiveType) derivedTerm.getType();
    assertEquals(type.getName(), "String");

    // set facets
    assertFalse(derivedTerm.isNullable());
    assertEquals("abc", derivedTerm.getDefaultValue());
    assertEquals(Integer.valueOf(15), derivedTerm.getMaxLength());
    assertEquals(Integer.valueOf(14), derivedTerm.getPrecision());
    assertEquals(Integer.valueOf(13), derivedTerm.getScale());
    // assertNull(derivedTerm.getSrid());
  }

  @Test
  public void termWithTypeDef() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    String namespaceAndName = "mySchema.TypeDef";
    String name = "TypeDef";
    csdlTerm.setType(namespaceAndName);
    Edm edm = mock(Edm.class);
    EdmTypeDefinition typeMock = mock(EdmTypeDefinition.class);
    when(typeMock.getName()).thenReturn(name);
    when(edm.getTypeDefinition(new FullQualifiedName(namespaceAndName))).thenReturn(typeMock);
    EdmTerm localTerm = new EdmTermImpl(edm, "namespace", csdlTerm);
    assertNotNull(localTerm.getType());
    assertEquals(name, localTerm.getType().getName());
  }

  @Test
  public void termWithEnumType() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    String namespaceAndName = "mySchema.Enum";
    String name = "Enum";
    csdlTerm.setType(namespaceAndName);
    Edm edm = mock(Edm.class);
    EdmEnumType typeMock = mock(EdmEnumType.class);
    when(typeMock.getName()).thenReturn(name);
    when(edm.getEnumType(new FullQualifiedName(namespaceAndName))).thenReturn(typeMock);
    EdmTerm localTerm = new EdmTermImpl(edm, "namespace", csdlTerm);
    assertNotNull(localTerm.getType());
    assertEquals(name, localTerm.getType().getName());
  }

  @Test
  public void termWithComplexType() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    String namespaceAndName = "mySchema.Complex";
    String name = "Complex";
    csdlTerm.setType(namespaceAndName);
    Edm edm = mock(Edm.class);
    EdmComplexType typeMock = mock(EdmComplexType.class);
    when(typeMock.getName()).thenReturn(name);
    when(edm.getComplexType(new FullQualifiedName(namespaceAndName))).thenReturn(typeMock);
    EdmTerm localTerm = new EdmTermImpl(edm, "namespace", csdlTerm);
    assertNotNull(localTerm.getType());
    assertEquals(name, localTerm.getType().getName());
  }

  @Test
  public void termWithEntityType() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    String namespaceAndName = "mySchema.Entity";
    String name = "Entity";
    csdlTerm.setType(namespaceAndName);
    Edm edm = mock(Edm.class);
    EdmEntityType typeMock = mock(EdmEntityType.class);
    when(typeMock.getName()).thenReturn(name);
    when(edm.getEntityType(new FullQualifiedName(namespaceAndName))).thenReturn(typeMock);
    EdmTerm localTerm = new EdmTermImpl(edm, "namespace", csdlTerm);
    assertNotNull(localTerm.getType());
    assertEquals(name, localTerm.getType().getName());
  }

  @Test
  public void invalidType() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    csdlTerm.setType("invalid.invalid");
    EdmTerm localTerm = new EdmTermImpl(mock(Edm.class), "namespace", csdlTerm);
    try {
      localTerm.getType();
      fail("Expected an EdmException");
    } catch (EdmException e) {
      assertEquals("Cannot find type with name: invalid.invalid", e.getMessage());
    }
  }

  @Test
  public void invalidAppliesToContent() {
    CsdlTerm csdlTerm = new CsdlTerm();
    FullQualifiedName csdlTerm1Name = new FullQualifiedName("namespace", "name1");
    csdlTerm.setName(csdlTerm1Name.getName());
    csdlTerm.setType("Edm.String");
    csdlTerm.setAppliesTo(Collections.singletonList("Invalid"));
    EdmTerm localTerm = new EdmTermImpl(mock(Edm.class), "namespace", csdlTerm);

    try {
      localTerm.getAppliesTo();
      fail("Expected an EdmException");
    } catch (EdmException e) {
      assertEquals("Invalid AppliesTo value: Invalid", e.getMessage());
    }
  }

}