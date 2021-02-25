/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.uri.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import nl.buildforce.olingo.commons.api.edm.Edm;
import nl.buildforce.olingo.commons.api.edm.EdmElement;
import nl.buildforce.olingo.commons.api.edm.EdmFunction;
import nl.buildforce.olingo.commons.api.edm.EdmNavigationProperty;
import nl.buildforce.olingo.commons.api.edm.EdmParameter;
import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import nl.buildforce.olingo.commons.api.edm.EdmProperty;
import nl.buildforce.olingo.commons.api.edm.EdmReturnType;
import nl.buildforce.olingo.commons.api.edm.EdmStructuredType;
import nl.buildforce.olingo.commons.api.edm.EdmType;
import nl.buildforce.olingo.commons.api.edm.FullQualifiedName;
import nl.buildforce.olingo.commons.api.edm.constants.EdmTypeKind;
import nl.buildforce.olingo.server.api.OData;
import nl.buildforce.olingo.server.api.uri.UriResourcePartTyped;
import nl.buildforce.olingo.server.api.uri.queryoption.ApplyOption;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.AggregateExpression;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.CustomFunction;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.AggregateExpressionImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.CustomFunctionImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.DynamicProperty;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.DynamicStructuredType;
import nl.buildforce.olingo.server.core.uri.queryoption.expression.MemberImpl;
import nl.buildforce.olingo.server.api.uri.UriInfo;
import nl.buildforce.olingo.server.api.uri.UriParameter;
import nl.buildforce.olingo.server.api.uri.UriResource;
import nl.buildforce.olingo.server.api.uri.queryoption.AliasQueryOption;
import nl.buildforce.olingo.server.api.uri.queryoption.ApplyItem;
import nl.buildforce.olingo.server.api.uri.queryoption.ExpandOption;
import nl.buildforce.olingo.server.api.uri.queryoption.FilterOption;
import nl.buildforce.olingo.server.api.uri.queryoption.SearchOption;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.Aggregate;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.BottomTop;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.Compute;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.Concat;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.GroupBy;
import nl.buildforce.olingo.server.api.uri.queryoption.apply.GroupByItem;
import nl.buildforce.olingo.server.api.uri.queryoption.expression.Expression;
import nl.buildforce.olingo.server.core.uri.UriInfoImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceComplexPropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceCountImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceNavigationPropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourcePrimitivePropertyImpl;
import nl.buildforce.olingo.server.core.uri.UriResourceStartingTypeFilterImpl;
import nl.buildforce.olingo.server.core.uri.parser.UriTokenizer.TokenKind;
import nl.buildforce.olingo.server.core.uri.queryoption.ApplyOptionImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.ExpandItemImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.ExpandOptionImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.AggregateImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.BottomTopImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.ComputeExpressionImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.ComputeImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.ConcatImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.ExpandImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.FilterImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.GroupByImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.GroupByItemImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.IdentityImpl;
import nl.buildforce.olingo.server.core.uri.queryoption.apply.SearchImpl;
import nl.buildforce.olingo.server.core.uri.validator.UriValidationException;

public class ApplyParser {

  private static final Map<TokenKind, AggregateExpression.StandardMethod> TOKEN_KIND_TO_STANDARD_METHOD;
  static {
    Map<TokenKind, AggregateExpression.StandardMethod> temp = new EnumMap<>(TokenKind.class);
    temp.put(TokenKind.SUM, AggregateExpression.StandardMethod.SUM);
    temp.put(TokenKind.MIN, AggregateExpression.StandardMethod.MIN);
    temp.put(TokenKind.MAX, AggregateExpression.StandardMethod.MAX);
    temp.put(TokenKind.AVERAGE, AggregateExpression.StandardMethod.AVERAGE);
    temp.put(TokenKind.COUNTDISTINCT, AggregateExpression.StandardMethod.COUNT_DISTINCT);
    TOKEN_KIND_TO_STANDARD_METHOD = Collections.unmodifiableMap(temp);
  }

  private static final Map<TokenKind, BottomTop.Method> TOKEN_KIND_TO_BOTTOM_TOP_METHOD;
  static {
    Map<TokenKind, BottomTop.Method> temp = new EnumMap<>(TokenKind.class);
    temp.put(TokenKind.BottomCountTrafo, BottomTop.Method.BOTTOM_COUNT);
    temp.put(TokenKind.BottomPercentTrafo, BottomTop.Method.BOTTOM_PERCENT);
    temp.put(TokenKind.BottomSumTrafo, BottomTop.Method.BOTTOM_SUM);
    temp.put(TokenKind.TopCountTrafo, BottomTop.Method.TOP_COUNT);
    temp.put(TokenKind.TopPercentTrafo, BottomTop.Method.TOP_PERCENT);
    temp.put(TokenKind.TopSumTrafo, BottomTop.Method.TOP_SUM);
    TOKEN_KIND_TO_BOTTOM_TOP_METHOD = Collections.unmodifiableMap(temp);
  }

  private final Edm edm;
  private final OData odata;

  private UriTokenizer tokenizer;
  private Collection<String> crossjoinEntitySetNames;
  private Map<String, AliasQueryOption> aliases;

  public ApplyParser(Edm edm, OData odata) {
    this.edm = edm;
    this.odata = odata;
  }

  public ApplyOption parse(UriTokenizer tokenizer, EdmStructuredType referencedType,
                           Collection<String> crossjoinEntitySetNames, Map<String, AliasQueryOption> aliases)
      throws UriParserException, UriValidationException {
    this.tokenizer = tokenizer;
    this.crossjoinEntitySetNames = crossjoinEntitySetNames;
    this.aliases = aliases;

    return parseApply(referencedType);
  }

  private ApplyOption parseApply(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    ApplyOptionImpl option = new ApplyOptionImpl();
    option.setEdmStructuredType(referencedType);
    do {
      option.add(parseTrafo(referencedType));
    } while (tokenizer.next(TokenKind.SLASH));
    return option;
  }

  private ApplyItem parseTrafo(EdmStructuredType referencedType) throws UriParserException, UriValidationException {
    if (tokenizer.next(TokenKind.AggregateTrafo)) {
      return parseAggregateTrafo(referencedType);

    } else if (tokenizer.next(TokenKind.IDENTITY)) {
      return new IdentityImpl();

    } else if (tokenizer.next(TokenKind.ComputeTrafo)) {
      return parseComputeTrafo(referencedType);

    } else if (tokenizer.next(TokenKind.ConcatMethod)) {
      return parseConcatTrafo(referencedType);

    } else if (tokenizer.next(TokenKind.ExpandTrafo)) {
      return new ExpandImpl().setExpandOption(parseExpandTrafo(referencedType));

    } else if (tokenizer.next(TokenKind.FilterTrafo)) {
      FilterOption filterOption = new FilterParser(edm, odata)
          .parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases);
      ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
      return new FilterImpl().setFilterOption(filterOption);

    } else if (tokenizer.next(TokenKind.GroupByTrafo)) {
      return parseGroupByTrafo(referencedType);

    } else if (tokenizer.next(TokenKind.SearchTrafo)) {
      SearchOption searchOption = new SearchParser().parse(tokenizer);
      ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
      return new SearchImpl().setSearchOption(searchOption);

    } else if (tokenizer.next(TokenKind.QualifiedName)) {
      return parseCustomFunction(new FullQualifiedName(tokenizer.getText()), referencedType);

    } else {
      TokenKind kind = ParserHelper.next(tokenizer,
          TokenKind.BottomCountTrafo, TokenKind.BottomPercentTrafo, TokenKind.BottomSumTrafo,
          TokenKind.TopCountTrafo, TokenKind.TopPercentTrafo, TokenKind.TopSumTrafo);
      if (kind == null) {
        throw new UriParserSyntaxException("Invalid apply expression syntax.",
            UriParserSyntaxException.MessageKeys.SYNTAX);
      } else {
        return parseBottomTop(kind, referencedType);
      }
    }
  }

  private Aggregate parseAggregateTrafo(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    AggregateImpl aggregate = new AggregateImpl();
    do {
      aggregate.addExpression(parseAggregateExpr(referencedType));
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return aggregate;
  }

  private AggregateExpression parseAggregateExpr(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    AggregateExpressionImpl aggregateExpression = new AggregateExpressionImpl();
    tokenizer.saveState();

    // First try is checking for a (potentially empty) path prefix and the things that could follow it.
    UriInfoImpl uriInfo = new UriInfoImpl();
    String identifierLeft = parsePathPrefix(uriInfo, referencedType);
    if (identifierLeft != null) {
      String customAggregate = tokenizer.getText();
      // A custom aggregate (an OData identifier) is defined in the CustomAggregate
      // EDM annotation (in namespace Org.OData.Aggregation.V1) of the structured type or of the entity container.
      // Currently we don't look into annotations, so all custom aggregates are allowed and have no type.
      uriInfo.addResourcePart(new UriResourcePrimitivePropertyImpl(createDynamicProperty(customAggregate, null)));
      aggregateExpression.setPath(uriInfo);
      String alias = parseAsAlias(referencedType, false);
      aggregateExpression.setAlias(alias);
      if (alias != null) {
        ((DynamicStructuredType) referencedType).addProperty(createDynamicProperty(alias, null));
      }
      parseAggregateFrom(aggregateExpression, referencedType);
    } else if (tokenizer.next(TokenKind.OPEN)) {
      UriResource lastResourcePart = uriInfo.getLastResourcePart();
      if (lastResourcePart == null) {
        throw new UriParserSyntaxException("Invalid 'aggregateExpr' syntax.",
            UriParserSyntaxException.MessageKeys.SYNTAX);
      }
      aggregateExpression.setPath(uriInfo);
      DynamicStructuredType inlineType = new DynamicStructuredType((EdmStructuredType)
          ParserHelper.getTypeInformation((UriResourcePartTyped) lastResourcePart));
      aggregateExpression.setInlineAggregateExpression(parseAggregateExpr(inlineType));
      ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    } else if (tokenizer.next(TokenKind.COUNT)) {
      uriInfo.addResourcePart(new UriResourceCountImpl());
      aggregateExpression.setPath(uriInfo);
      String alias = parseAsAlias(referencedType, true);
      aggregateExpression.setAlias(alias);
      ((DynamicStructuredType) referencedType).addProperty(
          createDynamicProperty(alias,
              // The OData standard mandates Edm.Decimal (with no decimals), although counts are always integer.
              odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Decimal)));
    } else {
      // No legitimate continuation of a path prefix has been found.

      // Second try is checking for a common expression.
      tokenizer.returnToSavedState();
      Expression expression = new ExpressionParser(edm, odata)
          .parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases);
      aggregateExpression.setExpression(expression);
      parseAggregateWith(aggregateExpression);
      if (aggregateExpression.getStandardMethod() == null && aggregateExpression.getCustomMethod() == null) {
        throw new UriParserSyntaxException("Invalid 'aggregateExpr' syntax.",
            UriParserSyntaxException.MessageKeys.SYNTAX);
      }
      String alias = parseAsAlias(referencedType, true);
      aggregateExpression.setAlias(alias);
      DynamicProperty dynamicProperty = createDynamicProperty(alias,
          // Determine the type for standard methods; there is no way to do this for custom methods.
          getTypeForAggregateMethod(aggregateExpression.getStandardMethod(),
              ExpressionParser.getType(expression)));
      if (aggregateExpression.getStandardMethod() == AggregateExpression.StandardMethod.SUM
          || aggregateExpression.getStandardMethod() == AggregateExpression.StandardMethod.AVERAGE) {
        //by default a property with no precision/scale defaults to a 0 scale
        //this does not work for sum/average in general
        dynamicProperty.setScale(Integer.MAX_VALUE);
      }
      ((DynamicStructuredType) referencedType).addProperty(
          dynamicProperty);
      parseAggregateFrom(aggregateExpression, referencedType);
    }

    return aggregateExpression;
  }

  private void parseAggregateWith(AggregateExpressionImpl aggregateExpression) throws UriParserException {
    if (tokenizer.next(TokenKind.WithOperator)) {
      TokenKind kind = ParserHelper.next(tokenizer,
          TokenKind.SUM, TokenKind.MIN, TokenKind.MAX, TokenKind.AVERAGE, TokenKind.COUNTDISTINCT,
          TokenKind.QualifiedName);
      if (kind == null) {
        throw new UriParserSyntaxException("Invalid 'with' syntax.",
            UriParserSyntaxException.MessageKeys.SYNTAX);
      } else if (kind == TokenKind.QualifiedName) {
        // A custom aggregation method is announced in the CustomAggregationMethods
        // EDM annotation (in namespace Org.OData.Aggregation.V1) of the structured type or of the entity container.
        // Currently we don't look into annotations, so all custom aggregation methods are allowed and have no type.
        aggregateExpression.setCustomMethod(new FullQualifiedName(tokenizer.getText()));
      } else {
        aggregateExpression.setStandardMethod(TOKEN_KIND_TO_STANDARD_METHOD.get(kind));
      }
    }
  }

  private EdmType getTypeForAggregateMethod(AggregateExpression.StandardMethod method, EdmType type) {
    if (method == AggregateExpression.StandardMethod.SUM || method == AggregateExpression.StandardMethod.AVERAGE || method == AggregateExpression.StandardMethod.COUNT_DISTINCT) {
      return odata.createPrimitiveTypeInstance(EdmPrimitiveTypeKind.Decimal);
    } else if (method == AggregateExpression.StandardMethod.MIN || method == AggregateExpression.StandardMethod.MAX) {
      return type;
    } else {
      return null;
    }
  }

  private String parseAsAlias(EdmStructuredType referencedType, boolean isRequired)
      throws UriParserException {
    if (tokenizer.next(TokenKind.AsOperator)) {
      ParserHelper.requireNext(tokenizer, TokenKind.ODataIdentifier);
      String name = tokenizer.getText();
      if (referencedType.getProperty(name) != null) {
        throw new UriParserSemanticException("Alias '" + name + "' is already a property.",
            UriParserSemanticException.MessageKeys.IS_PROPERTY, name);
      }
      return name;
    } else if (isRequired) {
      throw new UriParserSyntaxException("Expected asAlias not found.", UriParserSyntaxException.MessageKeys.SYNTAX);
    }
    return null;
  }

  private void parseAggregateFrom(AggregateExpressionImpl aggregateExpression,
      EdmStructuredType referencedType) throws UriParserException {
    while (tokenizer.next(TokenKind.FromOperator)) {
      AggregateExpressionImpl from = new AggregateExpressionImpl();
      from.setExpression(new MemberImpl(parseGroupingProperty(referencedType), referencedType));
      parseAggregateWith(from);
      aggregateExpression.addFrom(from);
    }
  }

  private DynamicProperty createDynamicProperty(String name, EdmType type) {
    return name == null ? null : new DynamicProperty(name, type);
  }

  private Compute parseComputeTrafo(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    ComputeImpl compute = new ComputeImpl();
    do {
      Expression expression = new ExpressionParser(edm, odata)
          .parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases);
      EdmType expressionType = ExpressionParser.getType(expression);
      if (expressionType.getKind() != EdmTypeKind.PRIMITIVE) {
        throw new UriParserSemanticException("Compute expressions must return primitive values.",
            UriParserSemanticException.MessageKeys.ONLY_FOR_PRIMITIVE_TYPES, "compute");
      }
      String alias = parseAsAlias(referencedType, true);
      ((DynamicStructuredType) referencedType).addProperty(createDynamicProperty(alias, expressionType));
      compute.addExpression(new ComputeExpressionImpl()
          .setExpression(expression)
          .setAlias(alias));
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return compute;
  }

  private Concat parseConcatTrafo(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    ConcatImpl concat = new ConcatImpl();
    // A common type is used for all sub-transformations.
    // If one sub-transformation aggregates properties away,
    // this could have unintended consequences for subsequent sub-transformations.
    concat.addApplyOption(parseApply(referencedType));
    ParserHelper.requireNext(tokenizer, TokenKind.COMMA);
    do {
      concat.addApplyOption(parseApply(referencedType));
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return concat;
  }

  private ExpandOption parseExpandTrafo(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    ExpandItemImpl item = new ExpandItemImpl();
    item.setResourcePath(ExpandParser.parseExpandPath(tokenizer, edm, referencedType, item));
    EdmType type = ParserHelper.getTypeInformation((UriResourcePartTyped)
        ((UriInfoImpl) item.getResourcePath()).getLastResourcePart());
    if (tokenizer.next(TokenKind.COMMA)) {
      if (tokenizer.next(TokenKind.FilterTrafo)) {
        item.setSystemQueryOption(
            new FilterParser(edm, odata).parse(tokenizer,type, crossjoinEntitySetNames, aliases));
        ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
      } else {
        ParserHelper.requireNext(tokenizer, TokenKind.ExpandTrafo);
        item.setSystemQueryOption(parseExpandTrafo((EdmStructuredType) type));
      }
    }
    while (tokenizer.next(TokenKind.COMMA)) {
      ParserHelper.requireNext(tokenizer, TokenKind.ExpandTrafo);
      ExpandOption nestedExpand = parseExpandTrafo((EdmStructuredType) type);
      if (item.getExpandOption() == null) {
        item.setSystemQueryOption(nestedExpand);
      } else {
        // Add to the existing items.
        ((ExpandOptionImpl) item.getExpandOption())
            .addExpandItem(nestedExpand.getExpandItems().get(0));
      }
    }
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    ExpandOptionImpl expand = new ExpandOptionImpl();
    expand.addExpandItem(item);
    return expand;
  }

  private GroupBy parseGroupByTrafo(EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    GroupByImpl groupBy = new GroupByImpl();
    parseGroupByList(groupBy, referencedType);
    if (tokenizer.next(TokenKind.COMMA)) {
      groupBy.setApplyOption(parseApply(referencedType));
    }
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return groupBy;
  }

  private void parseGroupByList(GroupByImpl groupBy, EdmStructuredType referencedType)
      throws UriParserException {
    ParserHelper.requireNext(tokenizer, TokenKind.OPEN);
    do {
      groupBy.addGroupByItem(parseGroupByElement(referencedType));
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
  }

  private GroupByItem parseGroupByElement(EdmStructuredType referencedType)
      throws UriParserException {
    if (tokenizer.next(TokenKind.RollUpSpec)) {
      return parseRollUpSpec(referencedType);
    } else {
      return new GroupByItemImpl().setPath(parseGroupingProperty(referencedType));
    }
  }

  private GroupByItem parseRollUpSpec(EdmStructuredType referencedType)
      throws UriParserException {
    GroupByItemImpl item = new GroupByItemImpl();
    if (tokenizer.next(TokenKind.ROLLUP_ALL)) {
      item.setIsRollupAll();
    } else {
      item.addRollupItem(new GroupByItemImpl().setPath(
          parseGroupingProperty(referencedType)));
    }
    ParserHelper.requireNext(tokenizer, TokenKind.COMMA);
    do {
      item.addRollupItem(new GroupByItemImpl().setPath(
          parseGroupingProperty(referencedType)));
    } while (tokenizer.next(TokenKind.COMMA));
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return item;
  }

  private UriInfo parseGroupingProperty(EdmStructuredType referencedType) throws UriParserException {
    UriInfoImpl uriInfo = new UriInfoImpl();
    String identifierLeft = parsePathPrefix(uriInfo, referencedType);
    if (identifierLeft != null) {
      throw new UriParserSemanticException("Unknown identifier in grouping property path.",
          UriParserSemanticException.MessageKeys.EXPRESSION_PROPERTY_NOT_IN_TYPE,
          identifierLeft,
          uriInfo.getLastResourcePart() != null && uriInfo.getLastResourcePart() instanceof UriResourcePartTyped ?
              ((UriResourcePartTyped) uriInfo.getLastResourcePart())
                  .getType().getFullQualifiedName().getFullQualifiedNameAsString() :
              "");
    }
    return uriInfo;
  }

  /**
   * Parses the path prefix and a following OData identifier as one path, deviating from the ABNF.
   * @param uriInfo object to be filled with path segments
   * @return a parsed but not used OData identifier */
  private String parsePathPrefix(UriInfoImpl uriInfo, EdmStructuredType referencedType)
      throws UriParserException {
    EdmStructuredType typeCast = ParserHelper.parseTypeCast(tokenizer, edm, referencedType);
    if (typeCast != null) {
      uriInfo.addResourcePart(new UriResourceStartingTypeFilterImpl(typeCast, true));
      ParserHelper.requireNext(tokenizer, TokenKind.SLASH);
    }
    EdmStructuredType type = typeCast == null ? referencedType : typeCast;
    while (tokenizer.next(TokenKind.ODataIdentifier)) {
      String name = tokenizer.getText();
      EdmElement property = type.getProperty(name);
      UriResource segment = parsePathSegment(property);
      if (segment == null) {
        if (property == null) {
          return name;
        } else {
          uriInfo.addResourcePart(
              property instanceof EdmNavigationProperty ?
                  new UriResourceNavigationPropertyImpl((EdmNavigationProperty) property) :
                  property.getType().getKind() == EdmTypeKind.COMPLEX ?
                      new UriResourceComplexPropertyImpl((EdmProperty) property) :
                      new UriResourcePrimitivePropertyImpl((EdmProperty) property));
          return null;
        }
      } else {
        uriInfo.addResourcePart(segment);
      }
      type = (EdmStructuredType) ParserHelper.getTypeInformation((UriResourcePartTyped) segment);
    }
    return null;
  }

  private UriResource parsePathSegment(EdmElement property) throws UriParserException {
    if (property == null
        || !(property.getType().getKind() == EdmTypeKind.COMPLEX
        || property instanceof EdmNavigationProperty)) {
      // Could be a customAggregate or $count.
      return null;
    }
    if (tokenizer.next(TokenKind.SLASH)) {
      EdmStructuredType typeCast = ParserHelper.parseTypeCast(tokenizer, edm,
          (EdmStructuredType) property.getType());
      if (typeCast != null) {
        ParserHelper.requireNext(tokenizer, TokenKind.SLASH);
      }
      return property.getType().getKind() == EdmTypeKind.COMPLEX ?
          new UriResourceComplexPropertyImpl((EdmProperty) property).setTypeFilter(typeCast) :
          new UriResourceNavigationPropertyImpl((EdmNavigationProperty) property).setCollectionTypeFilter(typeCast);
    } else {
      return null;
    }
  }

  private CustomFunction parseCustomFunction(FullQualifiedName functionName,
                                             EdmStructuredType referencedType) throws UriParserException, UriValidationException {
    List<UriParameter> parameters =
        ParserHelper.parseFunctionParameters(tokenizer, edm, referencedType, true, aliases);
    List<String> parameterNames = ParserHelper.getParameterNames(parameters);
    EdmFunction function = edm.getBoundFunction(functionName,
        referencedType.getFullQualifiedName(), true, parameterNames);
    if (function == null) {
      throw new UriParserSemanticException("No function '" + functionName + "' found.",
          UriParserSemanticException.MessageKeys.FUNCTION_NOT_FOUND,
          functionName.getFullQualifiedNameAsString());
    }
    ParserHelper.validateFunctionParameters(function, parameters, edm, referencedType, aliases);

    // The binding parameter and the return type must be of type complex or entity collection.
    EdmParameter bindingParameter = function.getParameter(function.getParameterNames().get(0));
    EdmReturnType returnType = function.getReturnType();
    if (bindingParameter.getType().getKind() != EdmTypeKind.ENTITY
        && bindingParameter.getType().getKind() != EdmTypeKind.COMPLEX
        || !bindingParameter.isCollection()
        || returnType.getType().getKind() != EdmTypeKind.ENTITY
        && returnType.getType().getKind() != EdmTypeKind.COMPLEX
        || !returnType.isCollection()) {
      throw new UriParserSemanticException("Only entity- or complex-collection functions are allowed.",
          UriParserSemanticException.MessageKeys.FUNCTION_MUST_USE_COLLECTIONS,
          functionName.getFullQualifiedNameAsString());
    }

    return new CustomFunctionImpl().setFunction(function).setParameters(parameters);
  }

  private BottomTop parseBottomTop(TokenKind kind, EdmStructuredType referencedType)
      throws UriParserException, UriValidationException {
    BottomTopImpl bottomTop = new BottomTopImpl();
    bottomTop.setMethod(TOKEN_KIND_TO_BOTTOM_TOP_METHOD.get(kind));
    ExpressionParser expressionParser = new ExpressionParser(edm, odata);
    Expression number = expressionParser.parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases);
    expressionParser.checkIntegerType(number);
    bottomTop.setNumber(number);
    ParserHelper.requireNext(tokenizer, TokenKind.COMMA);
    Expression value = expressionParser.parse(tokenizer, referencedType, crossjoinEntitySetNames, aliases);
    expressionParser.checkNumericType(value);
    bottomTop.setValue(value);
    ParserHelper.requireNext(tokenizer, TokenKind.CLOSE);
    return bottomTop;
  }
}
