/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.buildforce.olingo.commons.core.edm.primitivetype;

import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Implementation of the EDM primitive type Guid.
 */
public final class EdmGuid extends SingletonPrimitiveType {

  private static final String PATTERN = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

  private static final EdmGuid INSTANCE = new EdmGuid();

  public static EdmGuid getInstance() {
    return INSTANCE;
  }

  @Override
  public Class<?> getDefaultType() {
    return UUID.class;
  }

  @Override
  public boolean validate(String value,
                          Boolean isNullable, Integer maxLength, Integer precision,
                          Integer scale, Boolean isUnicode) {
    return value == null ? isNullable == null || isNullable : validateLiteral(value);
  }

  private boolean validateLiteral(String value) {
    return value.matches(PATTERN);
  }

  @Override
  protected <T> T internalValueOfString(String value,
                                        Boolean isNullable, Integer maxLength, Integer precision,
                                        Integer scale, Boolean isUnicode,
                                        Class<T> returnType) throws EdmPrimitiveTypeException {
    try {
      UUID valueUUID = UUID.fromString(value);

      if (returnType == String.class) {
        return returnType.cast(value);
      } else if (returnType.isAssignableFrom(UUID.class)) {
        return returnType.cast(valueUUID);
      } else if (returnType == Byte[].class) {
        byte[] buffer = new byte[16];
        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.putLong(valueUUID.getMostSignificantBits());
        bb.putLong(valueUUID.getLeastSignificantBits());
        return returnType.cast(ArrayUtils.toObject(buffer));
      }
      throw new EdmPrimitiveTypeException("The value type " + returnType + " is not supported.");
    } catch (IllegalArgumentException e) {
      throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.", e);
    }
  }

  @Override
  protected <T> String internalValueToString(T value,
                                             Boolean isNullable,
                                             Integer maxLength,
                                             Integer precision,
                                             Integer scale,
                                             Boolean isUnicode) throws EdmPrimitiveTypeException {

    if (value instanceof UUID) {
      return value.toString();
    } else {
      throw new EdmPrimitiveTypeException("The value type " + value.getClass() + " is not supported.");
    }
  }

}