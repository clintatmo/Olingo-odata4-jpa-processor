/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.serializer.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;

import nl.buildforce.olingo.commons.api.edm.EdmAction;
import nl.buildforce.olingo.commons.api.edm.EdmActionImport;
import nl.buildforce.olingo.commons.api.edm.EdmAnnotatable;
import nl.buildforce.olingo.commons.api.edm.EdmAnnotation;
import nl.buildforce.olingo.commons.api.edm.EdmAnnotations;
import nl.buildforce.olingo.commons.api.edm.EdmBindingTarget;
import nl.buildforce.olingo.commons.api.edm.EdmComplexType;
import nl.buildforce.olingo.commons.api.edm.EdmEntityContainer;
import nl.buildforce.olingo.commons.api.edm.EdmEntitySet;
import nl.buildforce.olingo.commons.api.edm.EdmEntityType;
import nl.buildforce.olingo.commons.api.edm.EdmEnumType;
import nl.buildforce.olingo.commons.api.edm.EdmException;
import nl.buildforce.olingo.commons.api.edm.EdmFunction;
import nl.buildforce.olingo.commons.api.edm.EdmFunctionImport;
import nl.buildforce.olingo.commons.api.edm.EdmKeyPropertyRef;
import nl.buildforce.olingo.commons.api.edm.EdmMember;
import nl.buildforce.olingo.commons.api.edm.EdmNavigationProperty;
import nl.buildforce.olingo.commons.api.edm.EdmNavigationPropertyBinding;
import nl.buildforce.olingo.commons.api.edm.EdmOperation;
import nl.buildforce.olingo.commons.api.edm.EdmParameter;
import nl.buildforce.olingo.commons.api.edm.EdmProperty;
import nl.buildforce.olingo.commons.api.edm.EdmReferentialConstraint;
import nl.buildforce.olingo.commons.api.edm.EdmReturnType;
import nl.buildforce.olingo.commons.api.edm.EdmSchema;
import nl.buildforce.olingo.commons.api.edm.EdmSingleton;
import nl.buildforce.olingo.commons.api.edm.EdmStructuredType;
import nl.buildforce.olingo.commons.api.edm.EdmTerm;
import nl.buildforce.olingo.commons.api.edm.EdmType;
import nl.buildforce.olingo.commons.api.edm.EdmTypeDefinition;
import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;
import nl.buildforce.olingo.commons.api.edm.TargetType;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmApply;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmCast;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmConstantExpression;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmDynamicExpression;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmExpression;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmIf;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmIsOf;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLabeledElement;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLabeledElementReference;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmLogicalOrComparisonExpression;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNavigationPropertyPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNot;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmNull;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPropertyPath;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmPropertyValue;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmRecord;
import nl.buildforce.olingo.commons.api.edm.annotation.EdmUrlRef;
import nl.buildforce.olingo.commons.api.edm.constants.EdmTypeKind;
import nl.buildforce.olingo.commons.api.edmx.EdmxReference;
import nl.buildforce.olingo.commons.api.edmx.EdmxReferenceInclude;
import nl.buildforce.olingo.commons.api.edmx.EdmxReferenceIncludeAnnotation;
import nl.buildforce.olingo.server.api.ServiceMetadata;
import nl.buildforce.olingo.server.api.serializer.Kind;
import nl.buildforce.olingo.server.api.serializer.SerializerException;

import com.fasterxml.jackson.core.JsonGenerator;

public class MetadataDocumentJsonSerializer {
  
  private final ServiceMetadata serviceMetadata;
  private final Map<String, String> namespaceToAlias = new HashMap<>();
  private static final String DOLLAR = "$";
  private static final String VERSION = DOLLAR + "Version";
  private static final String REFERENCES = DOLLAR + "Reference";
  private static final String INCLUDE = DOLLAR + "Include";
  private static final String NAMESPACE = DOLLAR + "Namespace";
  private static final String ALIAS = DOLLAR + "Alias";
  private static final String INCLUDE_ANNOTATIONS = DOLLAR + "IncludeAnnotations";
  private static final String TERM_NAMESPACE = DOLLAR + "TermNamespace";
  private static final String TARGET_NAMESPACE = DOLLAR + "TargetNamespace";
  private static final String QUALIFIER = DOLLAR + "Qualifier";
  private static final String IS_FLAGS = DOLLAR + "IsFlags";
  private static final String UNDERLYING_TYPE = DOLLAR + "UnderlyingType";
  private static final String KIND = DOLLAR + "Kind";
  private static final String MAX_LENGTH = DOLLAR + "MaxLength";
  private static final String PRECISION = DOLLAR + "Precision";
  private static final String SCALE = DOLLAR + "Scale";
//private static final String SRID = DOLLAR + "SRID";
  private static final String COLLECTION = DOLLAR + "Collection";
  private static final String BASE_TYPE = DOLLAR + "BaseType";
  private static final String HAS_STREAM = DOLLAR + "HasStream";
  private static final String KEY = DOLLAR + "Key";
  private static final String ABSTRACT = DOLLAR + "Abstract";
  private static final String TYPE = DOLLAR + "Type";
  private static final String NULLABLE = DOLLAR + "Nullable";
  private static final String UNICODE = DOLLAR + "Unicode";
  private static final String DEFAULT_VALUE = DOLLAR + "DefaultValue";
  private static final String PARTNER = DOLLAR + "Partner";
  private static final String CONTAINS_TARGET = DOLLAR + "ContainsTarget";
  private static final String REFERENTIAL_CONSTRAINT = DOLLAR + "ReferentialConstraint";
  private static final String ISBOUND = DOLLAR + "IsBound";
  private static final String ENTITY_SET_PATH = DOLLAR + "EntitySetPath";
  private static final String PARAMETER = DOLLAR + "Parameter";
  private static final String RETURN_TYPE = DOLLAR + "ReturnType";
  private static final String ISCOMPOSABLE = DOLLAR + "IsComposable";
  private static final String PARAMETER_NAME = DOLLAR + "Name";
  private static final String BASE_TERM = DOLLAR + "BaseTerm";
  private static final String APPLIES_TO = DOLLAR + "AppliesTo";
  private static final String NAVIGATION_PROPERTY_BINDING = DOLLAR + "NavigationPropertyBinding";
  private static final String EXTENDS = DOLLAR + "Extends";
  private static final String INCLUDE_IN_SERV_DOC = DOLLAR + "IncludeInServiceDocument";
  private static final String ANNOTATION = DOLLAR + "Annotations";
  private static final String ANNOTATION_PATH = DOLLAR + "Path";
  private static final String NAME = DOLLAR + "Name";
  private static final String ON_DELETE = "OnDelete";
  private static final String ON_DELETE_PROPERTY = "Action";

  public MetadataDocumentJsonSerializer(ServiceMetadata serviceMetadata) throws SerializerException {
    if (serviceMetadata == null || serviceMetadata.getEdm() == null) {
      throw new SerializerException("Service Metadata and EDM must not be null for a service.",
          SerializerException.MessageKeys.NULL_METADATA_OR_EDM);
    }
    this.serviceMetadata = serviceMetadata;
  }
  
  public void writeMetadataDocument(JsonGenerator json) throws SerializerException, IOException {
    json.writeStartObject();
    json.writeStringField(VERSION, "4.01");
    if (!serviceMetadata.getReferences().isEmpty()) {
      appendReference(json);
    }
    appendDataServices(json);
    json.writeEndObject();
  }

  private void appendDataServices(JsonGenerator json) throws SerializerException, IOException {
    for (EdmSchema schema : serviceMetadata.getEdm().getSchemas()) {
      appendSchema(json, schema);
    }
  }

  private void appendSchema(JsonGenerator json, EdmSchema schema) 
      throws SerializerException, IOException {
    json.writeFieldName(schema.getNamespace());
    json.writeStartObject();
    if (schema.getAlias() != null) {
      json.writeStringField(ALIAS, schema.getAlias());
      namespaceToAlias.put(schema.getNamespace(), schema.getAlias());
    }
    // EnumTypes
    appendEnumTypes(json, schema.getEnumTypes());
    
    // TypeDefinitions
    appendTypeDefinitions(json, schema.getTypeDefinitions());
    
    // EntityTypes
    appendEntityTypes(json, schema.getEntityTypes());
    
    // ComplexTypes
    appendComplexTypes(json, schema.getComplexTypes());
    
    // Actions
    appendActions(json, schema.getActions());
    
    // Functions
    appendFunctions(json, schema.getFunctions());
    
    //Terms
    appendTerms(json, schema.getTerms());
    
    // EntityContainer
    appendEntityContainer(json, schema.getEntityContainer());
 
    // AnnotationGroups
    appendAnnotationGroups(json, schema.getAnnotationGroups());

    appendAnnotations(json, schema, null);
    
    json.writeEndObject();
  }

  private void appendAnnotationGroups(JsonGenerator json,
                                      List<EdmAnnotations> annotationGroups) throws SerializerException, IOException {
    if (!annotationGroups.isEmpty()) {
      json.writeObjectFieldStart(ANNOTATION);
    }
    for (EdmAnnotations annotationGroup : annotationGroups) {
      appendAnnotationGroup(json, annotationGroup);
    }
    if (!annotationGroups.isEmpty()) {
      json.writeEndObject();
    }
  }

  private void appendAnnotationGroup(JsonGenerator json,
                                     EdmAnnotations annotationGroup) throws SerializerException, IOException {
    String targetPath = annotationGroup.getTargetPath();
    if (annotationGroup.getQualifier() != null) {
      json.writeObjectFieldStart(targetPath + "#" + annotationGroup.getQualifier());
    } else {
      json.writeObjectFieldStart(targetPath);
    }
    appendAnnotations(json, annotationGroup, null);
    json.writeEndObject();
  }

  private void appendEntityContainer(JsonGenerator json,
                                     EdmEntityContainer container) throws SerializerException, IOException {
    if (container != null) {
      json.writeObjectFieldStart(container.getName());
      json.writeStringField(KIND, Kind.EntityContainer.name());
      FullQualifiedName parentContainerName = container.getParentContainerName();
      if (parentContainerName != null) {
        String parentContainerNameString;
        if (namespaceToAlias.get(parentContainerName.getNamespace()) != null) {
          parentContainerNameString =
              namespaceToAlias.get(parentContainerName.getNamespace()) + "." + parentContainerName.getName();
        } else {
          parentContainerNameString = parentContainerName.getFullQualifiedNameAsString();
        }
        json.writeObjectFieldStart(Kind.Extending.name());
        json.writeStringField(KIND, Kind.EntityContainer.name());
        json.writeStringField(EXTENDS, parentContainerNameString);
        json.writeEndObject();
      }

      // EntitySets
      appendEntitySets(json, container.getEntitySets());

      String containerNamespace;
      if (namespaceToAlias.get(container.getNamespace()) != null) {
        containerNamespace = namespaceToAlias.get(container.getNamespace());
      } else {
        containerNamespace = container.getNamespace();
      }
      // ActionImports
      appendActionImports(json, container.getActionImports(), containerNamespace);
     
      // FunctionImports
      appendFunctionImports(json, container.getFunctionImports(), containerNamespace);

       
      // Singletons
      appendSingletons(json, container.getSingletons());

      // Annotations
      appendAnnotations(json, container, null);

      json.writeEndObject();
    }
    
  }

  private void appendSingletons(JsonGenerator json,
                                List<EdmSingleton> singletons) throws SerializerException, IOException {
    for (EdmSingleton singleton : singletons) {
      json.writeObjectFieldStart(singleton.getName());
      json.writeStringField(KIND, Kind.Singleton.name());
      json.writeStringField(TYPE, getAliasedFullQualifiedName(singleton.getEntityType()));
      
      appendNavigationPropertyBindings(json, singleton);
      appendAnnotations(json, singleton, null);
      json.writeEndObject();
    }
  }

  private void appendFunctionImports(JsonGenerator json, List<EdmFunctionImport> functionImports,
                                     String containerNamespace) throws SerializerException, IOException {
    for (EdmFunctionImport functionImport : functionImports) {
      json.writeObjectFieldStart(functionImport.getName());

      json.writeStringField(KIND, Kind.FunctionImport.name());
      String functionFQNString;
      FullQualifiedName functionFqn = functionImport.getFunctionFqn();
      if (namespaceToAlias.get(functionFqn.getNamespace()) != null) {
        functionFQNString = namespaceToAlias.get(functionFqn.getNamespace()) + "." + functionFqn.getName();
      } else {
        functionFQNString = functionFqn.getFullQualifiedNameAsString();
      }
      json.writeStringField(DOLLAR + Kind.Function.name(), functionFQNString);

      EdmEntitySet returnedEntitySet = functionImport.getReturnedEntitySet();
      if (returnedEntitySet != null) {
        json.writeStringField(DOLLAR + Kind.EntitySet.name(), 
            containerNamespace + "." + returnedEntitySet.getName());
      }
      // Default is false and we do not write the default
      if (functionImport.isIncludeInServiceDocument()) {
        json.writeBooleanField(INCLUDE_IN_SERV_DOC, functionImport.isIncludeInServiceDocument());
      }
      appendAnnotations(json, functionImport, null);
      json.writeEndObject();
    }
  }

  private void appendActionImports(JsonGenerator json,
                                   List<EdmActionImport> actionImports, String containerNamespace)
          throws SerializerException, IOException {
    for (EdmActionImport actionImport : actionImports) {
      json.writeObjectFieldStart(actionImport.getName());
      json.writeStringField(KIND, Kind.ActionImport.name());
      json.writeStringField(DOLLAR + Kind.Action.name(), getAliasedFullQualifiedName(actionImport.getUnboundAction()));
      if (actionImport.getReturnedEntitySet() != null) {
        json.writeStringField(DOLLAR + Kind.EntitySet.name(), 
            containerNamespace + "." + actionImport.getReturnedEntitySet().getName());
      }
      appendAnnotations(json, actionImport, null);
      json.writeEndObject();
    }
    
  }

  private void appendEntitySets(JsonGenerator json,
                                List<EdmEntitySet> entitySets) throws SerializerException, IOException {
    for (EdmEntitySet entitySet : entitySets) {
      json.writeObjectFieldStart(entitySet.getName());
      json.writeStringField(KIND, Kind.EntitySet.name());
      json.writeStringField(TYPE, getAliasedFullQualifiedName(entitySet.getEntityType()));
      if (!entitySet.isIncludeInServiceDocument()) {
        json.writeBooleanField(INCLUDE_IN_SERV_DOC, entitySet.isIncludeInServiceDocument());
      }

      appendNavigationPropertyBindings(json, entitySet);
      appendAnnotations(json, entitySet, null);
      json.writeEndObject();
    }
  }

  private void appendNavigationPropertyBindings(JsonGenerator json,
                                                EdmBindingTarget bindingTarget) throws IOException {
    if (bindingTarget.getNavigationPropertyBindings() != null && 
        !bindingTarget.getNavigationPropertyBindings().isEmpty()) {
      json.writeObjectFieldStart(NAVIGATION_PROPERTY_BINDING);
      for (EdmNavigationPropertyBinding binding : bindingTarget.getNavigationPropertyBindings()) {
        json.writeStringField(binding.getPath(), binding.getTarget());
      }
      json.writeEndObject();
    }
  }

  private void appendTerms(JsonGenerator json, List<EdmTerm> terms)
      throws SerializerException, IOException {
    for (EdmTerm term : terms) {
      json.writeObjectFieldStart(term.getName());
      json.writeStringField(KIND, Kind.Term.name());

      json.writeStringField(TYPE, getAliasedFullQualifiedName(term.getType()));

      if (term.getBaseTerm() != null) {
        json.writeStringField(BASE_TERM, getAliasedFullQualifiedName(term.getBaseTerm().getFullQualifiedName()));
      }

      if (term.getAppliesTo() != null && !term.getAppliesTo().isEmpty()) {
        StringBuilder appliesToString = new StringBuilder();
        boolean first = true;
        for (TargetType target : term.getAppliesTo()) {
          if (first) {
            first = false;
            appliesToString = Optional.ofNullable(target.toString()).map(StringBuilder::new).orElse(null);
          } else {
            appliesToString = (appliesToString == null ? new StringBuilder() : appliesToString).append(" ").append(target.toString());
          }
        }
        json.writeStringField(APPLIES_TO, appliesToString == null ? null : appliesToString.toString());
      }

      // Facets
      if (!term.isNullable()) {
        json.writeBooleanField(NULLABLE, term.isNullable());
      }

      if (term.getDefaultValue() != null) {
        json.writeStringField(DEFAULT_VALUE, term.getDefaultValue());
      }

      if (term.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, term.getMaxLength());
      }

      if (term.getPrecision() != null) {
        json.writeNumberField(PRECISION, term.getPrecision());
      }

      if (term.getScale() != null) {
        json.writeNumberField(SCALE, term.getScale());
      }
      
      appendAnnotations(json, term, null);
      json.writeEndObject();
    }
    
  }

  private void appendFunctions(JsonGenerator json,
                               List<EdmFunction> functions) throws SerializerException, IOException {
    Map<String, List<EdmFunction>> functionsMap = new HashMap<>();
    for (EdmFunction function : functions) {
      if (functionsMap.containsKey(function.getName())) {
        List<EdmFunction> actionsWithSpecificActionName = functionsMap.get(function.getName());
        actionsWithSpecificActionName.add(function);
        functionsMap.put(function.getName(), actionsWithSpecificActionName);
      } else {
        List<EdmFunction> functionList = new ArrayList<>();
        functionList.add(function);
        functionsMap.put(function.getName(), functionList);
      }
    }
    
    for (Entry<String, List<EdmFunction>> functionsMapEntry : functionsMap.entrySet()) {
      json.writeArrayFieldStart(functionsMapEntry.getKey());
      List<EdmFunction> functionEntry = functionsMapEntry.getValue();
      for (EdmFunction function : functionEntry) {
        json.writeStartObject();
        json.writeStringField(KIND, Kind.Function.name());
        if (function.getEntitySetPath() != null) {
          json.writeStringField(ENTITY_SET_PATH, function.getEntitySetPath());
        }
        if (function.isBound()) {
          json.writeBooleanField(ISBOUND, function.isBound());
        }

        if (function.isComposable()) {
          json.writeBooleanField(ISCOMPOSABLE, function.isComposable());
        }
        
        appendOperationParameters(json, function);

        appendOperationReturnType(json, function);

        appendAnnotations(json, function, null);

        json.writeEndObject();
      }
      json.writeEndArray();
    }
  }

  private void appendActions(JsonGenerator json,
                             List<EdmAction> actions) throws SerializerException, IOException {
    Map<String, List<EdmAction>> actionsMap = new HashMap<>();
    for (EdmAction action : actions) {
      if (actionsMap.containsKey(action.getName())) {
        List<EdmAction> actionsWithSpecificActionName = actionsMap.get(action.getName());
        actionsWithSpecificActionName.add(action);
        actionsMap.put(action.getName(), actionsWithSpecificActionName);
      } else {
        List<EdmAction> actionList = new ArrayList<>();
        actionList.add(action);
        actionsMap.put(action.getName(), actionList);
      }
    }
    for (Entry<String, List<EdmAction>> actionsMapEntry : actionsMap.entrySet()) {
      json.writeArrayFieldStart(actionsMapEntry.getKey());
      List<EdmAction> actionEntry = actionsMapEntry.getValue();
      for (EdmAction action : actionEntry) {
        json.writeStartObject();
        json.writeStringField(KIND, Kind.Action.name());
        if (action.getEntitySetPath() != null) {
          json.writeStringField(ENTITY_SET_PATH, action.getEntitySetPath());
        }
        json.writeBooleanField(ISBOUND, action.isBound());

        appendOperationParameters(json, action);

        appendOperationReturnType(json, action);

        appendAnnotations(json, action, null);

        json.writeEndObject();
      }
      json.writeEndArray();
    }
  }

  private void appendOperationReturnType(JsonGenerator json, EdmOperation operation) throws IOException {
    EdmReturnType returnType = operation.getReturnType();
    if (returnType != null) {
      json.writeObjectFieldStart(RETURN_TYPE);
      String returnTypeFqnString;
      if (EdmTypeKind.PRIMITIVE.equals(returnType.getType().getKind())) {
        returnTypeFqnString = getFullQualifiedName(returnType.getType());
      } else {
        returnTypeFqnString = getAliasedFullQualifiedName(returnType.getType());
      }
      json.writeStringField(TYPE, returnTypeFqnString);
      if (returnType.isCollection()) {
        json.writeBooleanField(COLLECTION, returnType.isCollection());
      }
      
      appendReturnTypeFacets(json, returnType);
      json.writeEndObject();
    }
  }

  private void appendReturnTypeFacets(JsonGenerator json,
                                      EdmReturnType returnType) throws IOException {
    if (!returnType.isNullable()) {
      json.writeBooleanField(NULLABLE, returnType.isNullable());
    }
    if (returnType.getMaxLength() != null) {
      json.writeNumberField(MAX_LENGTH, returnType.getMaxLength());
    }
    if (returnType.getPrecision() != null) {
      json.writeNumberField(PRECISION, returnType.getPrecision());
    }
    if (returnType.getScale() != null) {
      json.writeNumberField(SCALE, returnType.getScale());
    }
  }

  private void appendOperationParameters(JsonGenerator json,
                                         EdmOperation operation) throws SerializerException, IOException {
    if (!operation.getParameterNames().isEmpty()) {
      json.writeArrayFieldStart(PARAMETER);
    }
    for (String parameterName : operation.getParameterNames()) {
      EdmParameter parameter = operation.getParameter(parameterName);
      json.writeStartObject();
      json.writeStringField(PARAMETER_NAME, parameterName);
      String typeFqnString;
      if (EdmTypeKind.PRIMITIVE.equals(parameter.getType().getKind())) {
        typeFqnString = getFullQualifiedName(parameter.getType());
      } else {
        typeFqnString = getAliasedFullQualifiedName(parameter.getType());
      }
      json.writeStringField(TYPE, typeFqnString);
      if (parameter.isCollection()) {
        json.writeBooleanField(COLLECTION, parameter.isCollection());
      }
      
      appendParameterFacets(json, parameter);

      appendAnnotations(json, parameter, null);
      json.writeEndObject();
    }
    if (!operation.getParameterNames().isEmpty()) {
      json.writeEndArray();
    }
  }

  private void appendParameterFacets(JsonGenerator json,
                                     EdmParameter parameter) throws IOException {
    if (!parameter.isNullable()) {
      json.writeBooleanField(NULLABLE, parameter.isNullable());
    }
    if (parameter.getMaxLength() != null) {
      json.writeNumberField(MAX_LENGTH, parameter.getMaxLength());
    }
    if (parameter.getPrecision() != null) {
      json.writeNumberField(PRECISION, parameter.getPrecision());
    }
    if (parameter.getScale() != null) {
      json.writeNumberField(SCALE, parameter.getScale());
    }
  }

  private void appendComplexTypes(JsonGenerator json,
                                  List<EdmComplexType> complexTypes) throws SerializerException, IOException {
    for (EdmComplexType complexType : complexTypes) {
      json.writeObjectFieldStart(complexType.getName());

      json.writeStringField(KIND, Kind.ComplexType.name());
      if (complexType.getBaseType() != null) {
        json.writeStringField(BASE_TYPE, getAliasedFullQualifiedName(complexType.getBaseType()));
      }

      if (complexType.isAbstract()) {
        json.writeBooleanField(ABSTRACT, complexType.isAbstract());
      }

      appendProperties(json, complexType);

      appendNavigationProperties(json, complexType);

      appendAnnotations(json, complexType, null);

      json.writeEndObject();
    }
  }

  private void appendEntityTypes(JsonGenerator json, 
      List<EdmEntityType> entityTypes) throws SerializerException, IOException {
    for (EdmEntityType entityType : entityTypes) {
      json.writeObjectFieldStart(entityType.getName());
      json.writeStringField(KIND, Kind.EntityType.name());
      if (entityType.hasStream()) {
        json.writeBooleanField(HAS_STREAM, entityType.hasStream());
      }

      if (entityType.getBaseType() != null) {
        json.writeStringField(BASE_TYPE, getAliasedFullQualifiedName(entityType.getBaseType()));
      }

      if (entityType.isAbstract()) {
        json.writeBooleanField(ABSTRACT, entityType.isAbstract());
      }

      appendKey(json, entityType);

      appendProperties(json, entityType);

      appendNavigationProperties(json, entityType);

      appendAnnotations(json, entityType, null);

      json.writeEndObject();
    }
  }

  private void appendNavigationProperties(JsonGenerator json,
                                          EdmStructuredType type) throws SerializerException, IOException {
    List<String> navigationPropertyNames = new ArrayList<>(type.getNavigationPropertyNames());
    if (type.getBaseType() != null) {
      navigationPropertyNames.removeAll(type.getBaseType().getNavigationPropertyNames());
    }
    for (String navigationPropertyName : navigationPropertyNames) {
      EdmNavigationProperty navigationProperty = type.getNavigationProperty(navigationPropertyName);
      json.writeObjectFieldStart(navigationPropertyName);
      json.writeStringField(KIND, Kind.NavigationProperty.name());
      
      json.writeStringField(TYPE, getAliasedFullQualifiedName(navigationProperty.getType()));
      if (navigationProperty.isCollection()) {
        json.writeBooleanField(COLLECTION, navigationProperty.isCollection());
      }
      
      if (!navigationProperty.isNullable()) {
        json.writeBooleanField(NULLABLE, navigationProperty.isNullable());
      }

      if (navigationProperty.getPartner() != null) {
        EdmNavigationProperty partner = navigationProperty.getPartner();
        json.writeStringField(PARTNER, partner.getName());
      }

      if (navigationProperty.containsTarget()) {
        json.writeBooleanField(CONTAINS_TARGET, navigationProperty.containsTarget());
      }

      if (navigationProperty.getReferentialConstraints() != null) {
        for (EdmReferentialConstraint constraint : navigationProperty.getReferentialConstraints()) {
          json.writeObjectFieldStart(REFERENTIAL_CONSTRAINT);
          json.writeStringField(constraint.getPropertyName(), constraint.getReferencedPropertyName());
          for (EdmAnnotation annotation : constraint.getAnnotations()) {
            appendAnnotations(json, annotation, null);
          }
          json.writeEndObject();
        }
      }
      
      if (navigationProperty.getOnDelete() != null) {
        json.writeObjectFieldStart(ON_DELETE);
        json.writeStringField(ON_DELETE_PROPERTY, navigationProperty.getOnDelete().getAction());
        appendAnnotations(json, navigationProperty.getOnDelete(), null);
        json.writeEndObject();
      }

      appendAnnotations(json, navigationProperty, null);

      json.writeEndObject();
    }
  }

  private void appendProperties(JsonGenerator json,
                                EdmStructuredType type) throws SerializerException, IOException {
    List<String> propertyNames = new ArrayList<>(type.getPropertyNames());
    if (type.getBaseType() != null) {
      propertyNames.removeAll(type.getBaseType().getPropertyNames());
    }
    for (String propertyName : propertyNames) {
      EdmProperty property = type.getStructuralProperty(propertyName);
      json.writeObjectFieldStart(propertyName);
      String fqnString;
      if (property.isPrimitive()) {
        fqnString = getFullQualifiedName(property.getType());
      } else {
        fqnString = getAliasedFullQualifiedName(property.getType());
      }
      json.writeStringField(TYPE, fqnString);
      if (property.isCollection()) {
        json.writeBooleanField(COLLECTION, property.isCollection());
      }

      // Facets
      if (!property.isNullable()) {
        json.writeBooleanField(NULLABLE, property.isNullable());
      }

      if (!property.isUnicode()) {
        json.writeBooleanField(UNICODE, property.isUnicode());
      }

      if (property.getDefaultValue() != null) {
        json.writeStringField(DEFAULT_VALUE, property.getDefaultValue());
      }

      if (property.getMaxLength() != null) {
        json.writeNumberField(MAX_LENGTH, property.getMaxLength());
      }

      if (property.getPrecision() != null) {
        json.writeNumberField(PRECISION, property.getPrecision());
      }

      if (property.getScale() != null) {
        json.writeNumberField(SCALE, property.getScale());
      }
      
/*      if (property.getSrid() != null) {
          json.writeStringField(SRID, "" + property.getSrid());
      }*/

      appendAnnotations(json, property, null);
      json.writeEndObject();
    }
  }

  private void appendKey(JsonGenerator json,
                         EdmEntityType entityType) throws IOException {
    List<EdmKeyPropertyRef> keyPropertyRefs = entityType.getKeyPropertyRefs();
    if (keyPropertyRefs != null && !keyPropertyRefs.isEmpty()) {
      // Resolve Base Type key as it is shown in derived type
      EdmEntityType baseType = entityType.getBaseType();
      if (baseType != null && baseType.getKeyPropertyRefs() != null && !(baseType.getKeyPropertyRefs().isEmpty())) {
        return;
      }
      json.writeArrayFieldStart(KEY);
      for (EdmKeyPropertyRef keyRef : keyPropertyRefs) {
        
        if (keyRef.getAlias() != null) {
          json.writeStartObject();
          json.writeStringField(keyRef.getAlias(), keyRef.getName());
          json.writeEndObject();
        } else {
          json.writeString(keyRef.getName());
        }
      }
      json.writeEndArray();
    }
  }

  private String getAliasedFullQualifiedName(EdmType type) {
    FullQualifiedName fqn = type.getFullQualifiedName();
    return getAliasedFullQualifiedName(fqn);
  }
  
  private void appendTypeDefinitions(JsonGenerator json,
                                     List<EdmTypeDefinition> typeDefinitions) throws SerializerException, IOException {
    for (EdmTypeDefinition definition : typeDefinitions) {
      json.writeObjectFieldStart(definition.getName());
      json.writeStringField(KIND, definition.getKind().name());
      json.writeStringField(UNDERLYING_TYPE, getFullQualifiedName(definition.getUnderlyingType()));
      
      // Facets
      if (definition.getMaxLength() != null) {
        json.writeStringField(MAX_LENGTH, "" + definition.getMaxLength());
      }

      if (definition.getPrecision() != null) {
        json.writeStringField(PRECISION, "" + definition.getPrecision());
      }

      if (definition.getScale() != null) {
        json.writeStringField(SCALE, "" + definition.getScale());
      }
      
      /*if (definition.getSrid() != null) {
        json.writeStringField(SRID, "" + definition.getSrid());
      }*/

      appendAnnotations(json, definition, null);
      json.writeEndObject();
    }
  }

  private void appendEnumTypes(JsonGenerator json, List<EdmEnumType> enumTypes) 
      throws SerializerException, IOException {
    for (EdmEnumType enumType : enumTypes) {
      json.writeObjectFieldStart(enumType.getName());
      json.writeStringField(KIND, Kind.EnumType.name());
      json.writeBooleanField(IS_FLAGS, enumType.isFlags());
      json.writeStringField(UNDERLYING_TYPE, getFullQualifiedName(enumType.getUnderlyingType()));

      for (String memberName : enumType.getMemberNames()) {

        EdmMember member = enumType.getMember(memberName);
        if (member.getValue() != null) {
          json.writeStringField(memberName, member.getValue());
        }

        appendAnnotations(json, member, memberName);
      }
      json.writeEndObject();
    }
  }
  
  private void appendAnnotations(JsonGenerator json, 
      EdmAnnotatable annotatable, String memberName) throws SerializerException, IOException {
    List<EdmAnnotation> annotations = annotatable.getAnnotations();
    if (annotations != null && !annotations.isEmpty()) {
      for (EdmAnnotation annotation : annotations) {
        String termName = memberName != null ? memberName : "";
        if (annotation.getTerm() != null) {
          termName += "@" + getAliasedFullQualifiedName(annotation.getTerm().getFullQualifiedName());
        }
        if (annotation.getQualifier() != null) {
          termName += "#" + annotation.getQualifier();
        } 
        if (annotation.getExpression() == null && termName.length() > 0) {
          json.writeBooleanField(termName, true);
        } else {
          appendExpression(json, annotation.getExpression(), termName);
        }
        appendAnnotations(json, annotation, termName);
      }
    }
  }
  
  private void appendExpression(JsonGenerator json,
                                EdmExpression expression, String termName) throws SerializerException, IOException {
    if (expression == null) {
      return;
    }
    if (expression.isConstant()) {
      appendConstantExpression(json, expression.asConstant(), termName);
    } else if (expression.isDynamic()) {
      appendDynamicExpression(json, expression.asDynamic(), termName);
    } else {
      throw new IllegalArgumentException("Unkown expressiontype in metadata");
    }
  }
  
  private void appendDynamicExpression(JsonGenerator json, 
      EdmDynamicExpression dynExp, String termName) throws SerializerException, IOException {
    json.writeFieldName(termName);
      switch (dynExp.getExpressionType()) {
          // Logical
          case And -> appendLogicalOrComparisonExpression(json, dynExp.asAnd());
          case Or  -> appendLogicalOrComparisonExpression(json, dynExp.asOr());
          case Not -> appendNotExpression(json, dynExp.asNot());

          // Comparison
          case Eq -> appendLogicalOrComparisonExpression(json, dynExp.asEq());
          case Ne -> appendLogicalOrComparisonExpression(json, dynExp.asNe());
          case Gt -> appendLogicalOrComparisonExpression(json, dynExp.asGt());
          case Ge -> appendLogicalOrComparisonExpression(json, dynExp.asGe());
          case Lt -> appendLogicalOrComparisonExpression(json, dynExp.asLt());
          case Le -> appendLogicalOrComparisonExpression(json, dynExp.asLe());
          case AnnotationPath -> {
              json.writeStartObject();
              json.writeStringField(ANNOTATION_PATH, dynExp.asAnnotationPath().getValue());
              json.writeEndObject();
          }
          case Apply -> {
              EdmApply asApply = dynExp.asApply();
              json.writeStartObject();
              json.writeArrayFieldStart(DOLLAR + asApply.getExpressionName());
              for (EdmExpression parameter : asApply.getParameters()) {
                  appendExpression(json, parameter, null);
              }
              json.writeEndArray();
              json.writeStringField(DOLLAR + Kind.Function.name(), asApply.getFunction());
              appendAnnotations(json, asApply, null);
              json.writeEndObject();
          }
          case Cast -> {
              EdmCast asCast = dynExp.asCast();
              json.writeStartObject();
              appendExpression(json, asCast.getValue(), DOLLAR + asCast.getExpressionName());
              json.writeStringField(TYPE, getAliasedFullQualifiedName(asCast.getType()));
              if (asCast.getMaxLength() != null) {
                  json.writeNumberField(MAX_LENGTH, asCast.getMaxLength());
              }
              if (asCast.getPrecision() != null) {
                  json.writeNumberField(PRECISION, asCast.getPrecision());
              }
              if (asCast.getScale() != null) {
                  json.writeNumberField(SCALE, asCast.getScale());
              }
              appendAnnotations(json, asCast, null);
              json.writeEndObject();
          }
          case Collection -> {
              json.writeStartArray();
              for (EdmExpression item : dynExp.asCollection().getItems()) {
                  appendExpression(json, item, null);
              }
              json.writeEndArray();
          }
          case If -> {
              EdmIf asIf = dynExp.asIf();
              json.writeStartObject();
              json.writeArrayFieldStart(DOLLAR + asIf.getExpressionName());
              appendExpression(json, asIf.getGuard(), null);
              appendExpression(json, asIf.getThen(), null);
              appendExpression(json, asIf.getElse(), null);
              json.writeEndArray();
              appendAnnotations(json, asIf, null);
              json.writeEndObject();
          }
          case IsOf -> {
              EdmIsOf asIsOf = dynExp.asIsOf();
              json.writeStartObject();
              appendExpression(json, asIsOf.getValue(), DOLLAR + asIsOf.getExpressionName());
              json.writeStringField(TYPE, getAliasedFullQualifiedName(asIsOf.getType()));
              if (asIsOf.getMaxLength() != null) {
                  json.writeNumberField(MAX_LENGTH, asIsOf.getMaxLength());
              }
              if (asIsOf.getPrecision() != null) {
                  json.writeNumberField(PRECISION, asIsOf.getPrecision());
              }
              if (asIsOf.getScale() != null) {
                  json.writeNumberField(SCALE, asIsOf.getScale());
              }
              appendAnnotations(json, asIsOf, null);
              json.writeEndObject();
          }
          case LabeledElement -> {
              EdmLabeledElement asLabeledElement = dynExp.asLabeledElement();
              json.writeStartObject();
              appendExpression(json, asLabeledElement.getValue(), DOLLAR + asLabeledElement.getExpressionName());
              json.writeStringField(NAME, asLabeledElement.getName());
              appendAnnotations(json, asLabeledElement, null);
              json.writeEndObject();
          }
          case LabeledElementReference -> {
              EdmLabeledElementReference asLabeledElementReference = dynExp.asLabeledElementReference();
              json.writeStartObject();
              json.writeStringField(DOLLAR + asLabeledElementReference.getExpressionName(),
                      asLabeledElementReference.getValue());
              json.writeEndObject();
          }
          case Null -> {
              EdmNull asNull = dynExp.asNull();
              json.writeStartObject();
              json.writeStringField(DOLLAR + asNull.getExpressionName(), null);
              appendAnnotations(json, dynExp.asNull(), null);
              json.writeEndObject();
          }
          case NavigationPropertyPath -> {
              EdmNavigationPropertyPath asNavigationPropertyPath = dynExp.asNavigationPropertyPath();
              json.writeStartObject();
              json.writeStringField(DOLLAR + asNavigationPropertyPath.getExpressionName(),
                      asNavigationPropertyPath.getValue());
              json.writeEndObject();
          }
          case Path -> {
              EdmPath asPath = dynExp.asPath();
              json.writeStartObject();
              json.writeStringField(DOLLAR + asPath.getExpressionName(), asPath.getValue());
              json.writeEndObject();
          }
          case PropertyPath -> {
              EdmPropertyPath asPropertyPath = dynExp.asPropertyPath();
              json.writeStartObject();
              json.writeStringField(DOLLAR + asPropertyPath.getExpressionName(), asPropertyPath.getValue());
              json.writeEndObject();
          }
          case Record -> {
              EdmRecord asRecord = dynExp.asRecord();
              json.writeStartObject();
              try {
                  EdmStructuredType structuredType = asRecord.getType();
                  if (structuredType != null) {
                      json.writeStringField(TYPE, getAliasedFullQualifiedName(structuredType));
                  }
              } catch (EdmException e) {
                  FullQualifiedName type = asRecord.getTypeFQN();
                  if (type != null) {
                      json.writeStringField(TYPE, getAliasedFullQualifiedName(type));
                  }
              }
              for (EdmPropertyValue propValue : asRecord.getPropertyValues()) {
                  appendExpression(json, propValue.getValue(), propValue.getProperty());
                  appendAnnotations(json, propValue, propValue.getProperty());
              }
              appendAnnotations(json, asRecord, null);
              json.writeEndObject();
          }
          case UrlRef -> {
              EdmUrlRef asUrlRef = dynExp.asUrlRef();
              json.writeStartObject();
              appendExpression(json, asUrlRef.getValue(), DOLLAR + asUrlRef.getExpressionName());
              appendAnnotations(json, asUrlRef, null);
              json.writeEndObject();
          }
          default -> throw new IllegalArgumentException("Unkown ExpressionType for dynamic expression: " + dynExp.getExpressionType());
      }
  }

  private void appendNotExpression(JsonGenerator json, EdmNot exp)
      throws SerializerException, IOException {
    json.writeStartObject();
    appendExpression(json, exp.getLeftExpression(), DOLLAR + exp.getExpressionName());
    appendAnnotations(json, exp, null);
    json.writeEndObject();
  }

  private void appendLogicalOrComparisonExpression(JsonGenerator json,
                                                   EdmLogicalOrComparisonExpression exp) throws SerializerException, IOException {
    json.writeStartObject();
    json.writeArrayFieldStart(DOLLAR + exp.getExpressionName());
    appendExpression(json, exp.getLeftExpression(), null);
    appendExpression(json, exp.getRightExpression(), null);
    json.writeEndArray();
    appendAnnotations(json, exp, null);
    json.writeEndObject();
  }

  private void appendConstantExpression(JsonGenerator json,
                                        EdmConstantExpression constExp, String termName) throws IOException {
    switch (constExp.getExpressionType()) {
    case Binary:
      case TimeOfDay:
      case EnumMember:
      case Duration:
      case Int:
      case Float:
      case Decimal:
      case DateTimeOffset:
      case Date:
        json.writeObjectFieldStart(termName);
      json.writeStringField(DOLLAR + constExp.getExpressionName(), constExp.getValueAsString());
      json.writeEndObject();
      break;
      case Guid:
      json.writeObjectFieldStart(termName);
      json.writeStringField("$" + constExp.getExpressionName(), constExp.getValueAsString());
      json.writeEndObject();
      break;
      case Bool:
      if (termName != null && termName.length() > 0) {
        json.writeBooleanField(termName, Boolean.parseBoolean(constExp.getValueAsString()));
      } else {
        json.writeBoolean(Boolean.parseBoolean(constExp.getValueAsString()));
      }
      break;
    case String:
      if (termName != null && termName.length() > 0) {
        json.writeStringField(termName, constExp.getValueAsString());
      } else {
        json.writeString(constExp.getValueAsString());
      }
      break;
    default:
      throw new IllegalArgumentException("Unkown ExpressionType "
          + "for constant expression: " + constExp.getExpressionType());
    }
  }

  private String getAliasedFullQualifiedName(FullQualifiedName fqn) {
    String name;
    if (namespaceToAlias.get(fqn.getNamespace()) != null) {
      name = namespaceToAlias.get(fqn.getNamespace()) + "." + fqn.getName();
    } else {
      name = fqn.getFullQualifiedNameAsString();
    }

    return name;
  }
  
  private String getFullQualifiedName(EdmType type) {
    return type.getFullQualifiedName().getFullQualifiedNameAsString();
  }

  private void appendReference(JsonGenerator json) throws IOException {
    json.writeObjectFieldStart(REFERENCES);
    for (EdmxReference reference : serviceMetadata.getReferences()) {
      json.writeObjectFieldStart(reference.getUri().toASCIIString());

      List<EdmxReferenceInclude> includes = reference.getIncludes();
      if (!includes.isEmpty()) {
        appendIncludes(json, includes);
      }

      List<EdmxReferenceIncludeAnnotation> includeAnnotations = reference.getIncludeAnnotations();
      if (!includeAnnotations.isEmpty()) {
        appendIncludeAnnotations(json, includeAnnotations);
      }
      json.writeEndObject();
    }
    json.writeEndObject();
  }

  private void appendIncludeAnnotations(JsonGenerator json, 
      List<EdmxReferenceIncludeAnnotation> includeAnnotations) throws IOException {
    json.writeArrayFieldStart(INCLUDE_ANNOTATIONS);
    for (EdmxReferenceIncludeAnnotation includeAnnotation : includeAnnotations) {
      json.writeStartObject();
      json.writeStringField(TERM_NAMESPACE, includeAnnotation.getTermNamespace());
      if (includeAnnotation.getQualifier() != null) {
        json.writeStringField(QUALIFIER, includeAnnotation.getQualifier());
      }
      if (includeAnnotation.getTargetNamespace() != null) {
        json.writeStringField(TARGET_NAMESPACE, includeAnnotation.getTargetNamespace());
      }
      json.writeEndObject();
    }
    json.writeEndArray();
  }

  private void appendIncludes(JsonGenerator json, 
      List<EdmxReferenceInclude> includes) throws IOException {
   json.writeArrayFieldStart(INCLUDE);
   for (EdmxReferenceInclude include : includes) {
     json.writeStartObject();
     json.writeStringField(NAMESPACE, include.getNamespace());
     if (include.getAlias() != null) {
       namespaceToAlias.put(include.getNamespace(), include.getAlias());
       // Reference Aliases are ignored for now since they are not V2 compatible
       json.writeStringField(ALIAS, include.getAlias());
     }
     json.writeEndObject();
   }
   json.writeEndArray();
  }

}