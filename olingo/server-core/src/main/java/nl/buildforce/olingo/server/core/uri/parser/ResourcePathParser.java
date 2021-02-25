/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.uri.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.buildforce.olingo.commons.api.edm.Edm;
import nl.buildforce.olingo.commons.api.edm.EdmAction;
import nl.buildforce.olingo.commons.api.edm.EdmActionImport;
import nl.buildforce.olingo.commons.api.edm.EdmEntityContainer;
import nl.buildforce.olingo.commons.api.edm.EdmEntitySet;
import nl.buildforce.olingo.commons.api.edm.EdmEntityType;
import nl.buildforce.olingo.commons.api.edm.EdmFunction;
import nl.buildforce.olingo.commons.api.edm.EdmFunctionImport;
import nl.buildforce.olingo.commons.api.edm.EdmNavigationProperty;
import nl.buildforce.olingo.commons.api.edm.EdmProperty;
import nl.buildforce.olingo.commons.api.edm.EdmSingleton;
import nl.buildforce.olingo.commons.api.edm.EdmStructuredType;
import nl.buildforce.olingo.commons.api.edm.EdmType;
import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;
import nl.buildforce.olingo.commons.api.edm.constants.EdmTypeKind;
import nl.buildforce.olingo.server.api.uri.UriResourceEntitySet;
import nl.buildforce.olingo.server.api.uri.UriResourceFunction;
import nl.buildforce.olingo.server.api.uri.UriResourceNavigation;
import nl.buildforce.olingo.server.api.uri.UriResourcePartTyped;
import nl.buildforce.olingo.server.api.uri.UriParameter;
import nl.buildforce.olingo.server.api.uri.UriResource;
import nl.buildforce.olingo.server.api.uri.queryoption.AliasQueryOption;
import nl.buildforce.olingo.server.core.uri.UriResourceActionImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceComplexPropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceCountImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceEntitySetImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceFunctionImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceNavigationPropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourcePrimitivePropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceRefImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceSingletonImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceTypedImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceValueImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceWithKeysImpl;
import nl.buildforce.olingo.server.core.uri.parser.UriTokenizer.TokenKind;
import nl.buildforce.olingo.server.core.uri.validator.UriValidationException;

public class ResourcePathParser {

  private final Edm edm;
  private final EdmEntityContainer edmEntityContainer;
  private final Map<String, AliasQueryOption> aliases;
  private UriTokenizer tokenizer;

  public ResourcePathParser(Edm edm, Map<String, AliasQueryOption> aliases) {
    this.edm = edm;
    this.aliases = aliases;
    edmEntityContainer = edm.getEntityContainer();
  }

  public UriResource parsePathSegment(String pathSegment, UriResource previous)
      throws UriParserException, UriValidationException {
    tokenizer = new UriTokenizer(pathSegment);

    // The order is important.
    // A qualified name should not be parsed as identifier and let the tokenizer halt at '.'.

    if (previous == null) {
      if (tokenizer.next(TokenKind.QualifiedName)) {
        throw new UriParserSemanticException("The initial segment must not be namespace-qualified.",
            UriParserSemanticException.MessageKeys.NAMESPACE_NOT_ALLOWED_AT_FIRST_ELEMENT,
            new FullQualifiedName(tokenizer.getText()).getNamespace());
      } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
        return leadingResourcePathSegment();
      }

    } else {
      if (tokenizer.next(TokenKind.REF)) {
        return ref(previous);
      } else if (tokenizer.next(TokenKind.VALUE)) {
        return value(previous);
      } else if (tokenizer.next(TokenKind.COUNT)) {
        return count(previous);
      } else if (tokenizer.next(TokenKind.QualifiedName)) {
        return boundOperationOrTypeCast(previous);
      } else if (tokenizer.next(TokenKind.ODataIdentifier)) {
        return navigationOrProperty(previous);
      }
    }

    throw new UriParserSyntaxException("Unexpected start of resource-path segment.",
        UriParserSyntaxException.MessageKeys.SYNTAX);
  }

  public EdmEntityType parseDollarEntityTypeCast(String pathSegment) throws UriParserException {
    tokenizer = new UriTokenizer(pathSegment);
    ParserHelper.requireNext(tokenizer, TokenKind.QualifiedName);
    String name = tokenizer.getText();
    ParserHelper.requireTokenEnd(tokenizer);
    EdmEntityType type = edm.getEntityType(new FullQualifiedName(name));
    if (type == null) {
      throw new UriParserSemanticException("Type '" + name + "' not found.",
          UriParserSemanticException.MessageKeys.UNKNOWN_TYPE, name);
    }
    return type;
  }

  public List<String> parseCrossjoinSegment(String pathSegment) throws UriParserException {
    tokenizer = new UriTokenizer(pathSegment);
    ParserHelper.requireNext(tokenizer, TokenKind.CROSSJOIN);
    ParserHelper.requireNext(tokenizer, TokenKind.OPEN);
    // At least one entity-set name is mandatory. Try to fetch all.
    List<String> entitySetNames = new ArrayList<>();
    do {
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      String name = tokenizer.getText();
      EdmEntitySet edmEntitySet = edmEntityContainer.getEntitySet(name);
      if (edmEntitySet == null) {
        throw new UriParserSemanticException("Expected Entity Set Name.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, name);
      } else {
        entitySetNames.add(name);
      }
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    ParserHelper.requireTokenEnd(tokenizer);
    return entitySetNames;
  }

  private UriResource ref(UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$ref");
    if (((UriResourcePartTyped) previous).getType() instanceof EdmEntityType) {
      return new UriResourceRefImpl();
    } else {
      throw new UriParserSemanticException("$ref is only allowed on entity types.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_ENTITY_TYPES, "$ref");
    }
  }

  private UriResource value(UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$value");
    if (!((UriResourcePartTyped) previous).isCollection()) {
      requireMediaResourceInCaseOfEntity(previous);
      return new UriResourceValueImpl();
    } else {
      throw new UriParserSemanticException("$value is only allowed on typed path segments.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_TYPED_PARTS, "$value");
    }
  }

  private void requireMediaResourceInCaseOfEntity(UriResource resource) throws UriParserSemanticException {
    // If the resource is an entity or navigatio
    if (resource instanceof UriResourceEntitySet && !((UriResourceEntitySet) resource).getEntityType().hasStream()
        || resource instanceof UriResourceNavigation
        && !((EdmEntityType) ((UriResourceNavigation) resource).getType()).hasStream()) {
      throw new UriParserSemanticException("$value on entity is only allowed on media resources.",
          UriParserSemanticException.MessageKeys.NOT_A_MEDIA_RESOURCE, resource.getSegmentValue());
    }

    // Functions can also deliver an entity. In this case we have to check if the returned entity is a media resource
    if (resource instanceof UriResourceFunction) {
      EdmType returnType = ((UriResourceFunction) resource).getFunction().getReturnType().getType();
      //Collection check is above so not needed here
      if (returnType instanceof EdmEntityType && !((EdmEntityType) returnType).hasStream()) {
        throw new UriParserSemanticException("$value on returned entity is only allowed on media resources.",
            UriParserSemanticException.MessageKeys.NOT_A_MEDIA_RESOURCE, resource.getSegmentValue());
      }
    }
  }

  private UriResource count(UriResource previous) throws UriParserException {
    ParserHelper.requireTokenEnd(tokenizer);
    requireTyped(previous, "$count");
    if (((UriResourcePartTyped) previous).isCollection()) {
      return new UriResourceCountImpl();
    } else {
      throw new UriParserSemanticException("$count is only allowed on collections.",
          UriParserSemanticException.MessageKeys.ONLY_FOR_COLLECTIONS, "$count");
    }
  }

  private UriResource leadingResourcePathSegment() throws UriParserException, UriValidationException {
    String oDataIdentifier = tokenizer.getText();

    EdmEntitySet edmEntitySet = edmEntityContainer.getEntitySet(oDataIdentifier);
    if (edmEntitySet != null) {
      UriResourceEntitySetImpl entitySetResource = new UriResourceEntitySetImpl(edmEntitySet);

      if (tokenizer.next(TokenKind.OPEN)) {
        List<UriParameter> keyPredicates =
            ParserHelper.parseKeyPredicate(tokenizer, entitySetResource.getEntityType(), null, edm, null, aliases);
        entitySetResource.setKeyPredicates(keyPredicates);
      }

      ParserHelper.requireTokenEnd(tokenizer);
      return entitySetResource;
    }

    EdmSingleton edmSingleton = edmEntityContainer.getSingleton(oDataIdentifier);
    if (edmSingleton != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceSingletonImpl(edmSingleton);
    }

    EdmActionImport edmActionImport = edmEntityContainer.getActionImport(oDataIdentifier);
    if (edmActionImport != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceActionImpl(edmActionImport);
    }

    EdmFunctionImport edmFunctionImport = edmEntityContainer.getFunctionImport(oDataIdentifier);
    if (edmFunctionImport != null) {
      return functionCall(edmFunctionImport, null, null, false);
    }

    if (tokenizer.next(TokenKind.OPEN) || tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSemanticException("Unexpected start of resource-path segment.",
          UriParserSemanticException.MessageKeys.RESOURCE_NOT_FOUND, oDataIdentifier);
    } else {
      throw new UriParserSyntaxException("Unexpected start of resource-path segment.",
          UriParserSyntaxException.MessageKeys.SYNTAX);
    }
  }

  private UriResource navigationOrProperty(UriResource previous)
      throws UriParserException, UriValidationException {
    String name = tokenizer.getText();

    UriResourcePartTyped previousTyped = null;
    EdmStructuredType structType = null;
    requireTyped(previous, name);
    if (((UriResourcePartTyped) previous).getType() instanceof EdmStructuredType) {
      previousTyped = (UriResourcePartTyped) previous;
      EdmType previousTypeFilter = getPreviousTypeFilter(previousTyped);
      structType = (EdmStructuredType) (previousTypeFilter == null ? previousTyped.getType() : previousTypeFilter);
    } else {
      throw new UriParserSemanticException(
          "Cannot parse '" + name + "'; previous path segment is not a structural type.",
          UriParserSemanticException.MessageKeys.RESOURCE_PART_MUST_BE_PRECEDED_BY_STRUCTURAL_TYPE, name);
    }

    if (previousTyped.isCollection()) {
      throw new UriParserSemanticException("Property '" + name + "' is not allowed after collection.",
          UriParserSemanticException.MessageKeys.PROPERTY_AFTER_COLLECTION, name);
    }

    EdmProperty property = structType.getStructuralProperty(name);
    if (property != null) {
      return property.isPrimitive()
          || property.getType().getKind() == EdmTypeKind.ENUM
          || property.getType().getKind() == EdmTypeKind.DEFINITION ?
          new UriResourcePrimitivePropertyImpl(property) :
          new UriResourceComplexPropertyImpl(property);
    }
    EdmNavigationProperty navigationProperty = structType.getNavigationProperty(name);
    if (navigationProperty == null) {
      throw new UriParserSemanticException("Property '" + name + "' not found in type '"
          + structType.getFullQualifiedName().getFullQualifiedNameAsString() + "'",
          UriParserSemanticException.MessageKeys.PROPERTY_NOT_IN_TYPE,
          structType.getFullQualifiedName().getFullQualifiedNameAsString(), name);
    }
    List<UriParameter> keyPredicate =
        ParserHelper.parseNavigationKeyPredicate(tokenizer, navigationProperty, edm, null, aliases);
    ParserHelper.requireTokenEnd(tokenizer);
    return new UriResourceNavigationPropertyImpl(navigationProperty)
        .setKeyPredicates(keyPredicate);
  }

  private UriResource boundOperationOrTypeCast(UriResource previous)
      throws UriParserException, UriValidationException {
    FullQualifiedName name = new FullQualifiedName(tokenizer.getText());
    requireTyped(previous, name.getFullQualifiedNameAsString());
    UriResourcePartTyped previousTyped = (UriResourcePartTyped) previous;
    EdmType previousTypeFilter = getPreviousTypeFilter(previousTyped);
    EdmType previousType = previousTypeFilter == null ? previousTyped.getType() : previousTypeFilter;

    // We check for bound actions first because they cannot be followed by anything.
    EdmAction boundAction =
        edm.getBoundAction(name, previousType.getFullQualifiedName(), previousTyped.isCollection());
    if (boundAction != null) {
      ParserHelper.requireTokenEnd(tokenizer);
      return new UriResourceActionImpl(boundAction);
    }

    // Type casts can be syntactically indistinguishable from bound function calls in the case of additional keys.
    // But normally they are shorter, so they come next.
    EdmStructuredType type = previousTyped.getType() instanceof EdmEntityType ?
        edm.getEntityType(name) :
        edm.getComplexType(name);
    if (type != null) {
      return typeCast(name, type, previousTyped);
    }
    if (tokenizer.next(TokenKind.EOF)) {
      throw new UriParserSemanticException("Type '" + name.getFullQualifiedNameAsString() + "' not found.",
          UriParserSemanticException.MessageKeys.UNKNOWN_TYPE, name.getFullQualifiedNameAsString());
    }

    // Now a bound function call is the only remaining option.
    return functionCall(null, name, previousType.getFullQualifiedName(), previousTyped.isCollection());
  }

  private void requireTyped(UriResource previous, String forWhat) throws UriParserException {
    if (!(previous instanceof UriResourcePartTyped)) {
      throw new UriParserSemanticException("Path segment before '" + forWhat + "' is not typed.",
          UriParserSemanticException.MessageKeys.PREVIOUS_PART_NOT_TYPED, forWhat);
    }
  }

  private UriResource typeCast(FullQualifiedName name, EdmStructuredType type,
                               UriResourcePartTyped previousTyped) throws UriParserException, UriValidationException {
    if (type.compatibleTo(previousTyped.getType())) {
      EdmType previousTypeFilter = null;
      if (previousTyped instanceof UriResourceWithKeysImpl) {
        if (previousTyped.isCollection()) {
          previousTypeFilter = ((UriResourceWithKeysImpl) previousTyped).getTypeFilterOnCollection();
          if (previousTypeFilter != null) {
            throw new UriParserSemanticException("Type filters are not chainable.",
                UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
                previousTypeFilter.getName(), type.getName());
          }
          ((UriResourceWithKeysImpl) previousTyped).setCollectionTypeFilter(type);
        } else {
          previousTypeFilter = ((UriResourceWithKeysImpl) previousTyped).getTypeFilterOnEntry();
          if (previousTypeFilter != null) {
            throw new UriParserSemanticException("Type filters are not chainable.",
                UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
                previousTypeFilter.getName(), type.getName());
          }
          ((UriResourceWithKeysImpl) previousTyped).setEntryTypeFilter(type);
        }
        if (tokenizer.next(TokenKind.OPEN)) {
          List<UriParameter> keys =
              ParserHelper.parseKeyPredicate(tokenizer, (EdmEntityType) type, null, edm, null, aliases);
          if (previousTyped.isCollection()) {
            ((UriResourceWithKeysImpl) previousTyped).setKeyPredicates(keys);
          } else {
            throw new UriParserSemanticException("Key not allowed here.",
                UriParserSemanticException.MessageKeys.KEY_NOT_ALLOWED);
          }
        }
      } else {
        previousTypeFilter = ((UriResourceTypedImpl) previousTyped).getTypeFilter();
        if (previousTypeFilter != null) {
          throw new UriParserSemanticException("Type filters are not chainable.",
              UriParserSemanticException.MessageKeys.TYPE_FILTER_NOT_CHAINABLE,
              previousTypeFilter.getName(), type.getName());
        }
        ((UriResourceTypedImpl) previousTyped).setTypeFilter(type);
      }
      ParserHelper.requireTokenEnd(tokenizer);
      return null;
    } else {
      throw new UriParserSemanticException(
          "Type filter not compatible to previous path segment: " + name.getFullQualifiedNameAsString(),
          UriParserSemanticException.MessageKeys.INCOMPATIBLE_TYPE_FILTER, name.getFullQualifiedNameAsString());
    }
  }

  private EdmType getPreviousTypeFilter(UriResourcePartTyped previousTyped) {
    if (previousTyped instanceof UriResourceWithKeysImpl) {
      return ((UriResourceWithKeysImpl) previousTyped).getTypeFilterOnEntry() == null ?
          ((UriResourceWithKeysImpl) previousTyped).getTypeFilterOnCollection() :
          ((UriResourceWithKeysImpl) previousTyped).getTypeFilterOnEntry();
    } else {
      return ((UriResourceTypedImpl) previousTyped).getTypeFilter();
    }
  }

  private UriResource functionCall(EdmFunctionImport edmFunctionImport,
                                   FullQualifiedName boundFunctionName, FullQualifiedName bindingParameterTypeName,
                                   boolean isBindingParameterCollection) throws UriParserException, UriValidationException {
    List<UriParameter> parameters = ParserHelper.parseFunctionParameters(tokenizer, edm, null, false, aliases);
    List<String> names = ParserHelper.getParameterNames(parameters);
    EdmFunction function = null;
    if (edmFunctionImport != null) {
      function = edmFunctionImport.getUnboundFunction(names);
      if (function == null) {
        throw new UriParserSemanticException(
            "Function of function import '" + edmFunctionImport.getName() + "' "
                + "with parameters " + names + " not found.",
            UriParserSemanticException.MessageKeys.FUNCTION_NOT_FOUND, edmFunctionImport.getName(), names.toString());
      }
    } else {
      function = edm.getBoundFunction(boundFunctionName,
          bindingParameterTypeName, isBindingParameterCollection, names);
      if (function == null) {
        throw new UriParserSemanticException(
            "Function " + boundFunctionName + " not found.",
            UriParserSemanticException.MessageKeys.UNKNOWN_PART, boundFunctionName.getFullQualifiedNameAsString());
      }
    }
    ParserHelper.validateFunctionParameters(function, parameters, edm, null, aliases);
    ParserHelper.validateFunctionParameterFacets(function, parameters, edm, aliases);
    UriResourceFunctionImpl resource = new UriResourceFunctionImpl(edmFunctionImport, function, parameters);
    if (tokenizer.next(TokenKind.OPEN)) {
      if (function.getReturnType() != null
          && function.getReturnType().getType().getKind() == EdmTypeKind.ENTITY
          && function.getReturnType().isCollection()) {
        resource.setKeyPredicates(
            ParserHelper.parseKeyPredicate(tokenizer,
                (EdmEntityType) function.getReturnType().getType(), null, edm, null, aliases));
      } else {
        throw new UriParserSemanticException("A key is not allowed.",
            UriParserSemanticException.MessageKeys.KEY_NOT_ALLOWED);
      }
    }
    ParserHelper.requireTokenEnd(tokenizer);
    return resource;
  }

}