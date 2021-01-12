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
package nl.buildforce.olingo.server.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import nl.buildforce.olingo.commons.api.data.EntityMediaObject;
import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveType;
import nl.buildforce.olingo.commons.api.edm.EdmPrimitiveTypeException;
import nl.buildforce.olingo.server.api.ODataResponse;
import nl.buildforce.olingo.server.api.serializer.FixedFormatSerializer;
import nl.buildforce.olingo.server.api.serializer.PrimitiveValueSerializerOptions;
import nl.buildforce.olingo.server.core.ODataWritableContent;
import nl.buildforce.olingo.server.api.deserializer.batch.ODataResponsePart;
import nl.buildforce.olingo.server.api.serializer.BatchSerializerException;
import nl.buildforce.olingo.server.api.serializer.SerializerException;
import nl.buildforce.olingo.server.api.serializer.SerializerStreamResult;

public class FixedFormatSerializerImpl implements FixedFormatSerializer {

  @Override
  public InputStream binary(byte[] binary) throws SerializerException {
    return new ByteArrayInputStream(binary);
  }
  
  protected void binary(EntityMediaObject mediaEntity,
                        OutputStream outputStream) throws SerializerException {
	  try {
		outputStream.write(mediaEntity.getBytes());
	} catch (IOException e) {
		throw new SerializerException("IO Exception occured ", e, SerializerException.MessageKeys.IO_EXCEPTION);
	}
  }
  
  public void binaryIntoStreamed(EntityMediaObject mediaEntity,
                                 OutputStream outputStream) throws SerializerException {
	binary(mediaEntity, outputStream);
  }
  
  @Override
  public SerializerStreamResult mediaEntityStreamed(EntityMediaObject mediaEntity) throws SerializerException {
	  return ODataWritableContent.with(mediaEntity, this).build();
  }

  @Override
  public InputStream count(Integer count) throws SerializerException {
      return new ByteArrayInputStream(count.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public InputStream primitiveValue(EdmPrimitiveType type, Object value,
                                    PrimitiveValueSerializerOptions options) throws SerializerException {
    try {
      String result = type.valueToString(value,
          options.isNullable(), options.getMaxLength(),
          options.getPrecision(), options.getScale(), options.isUnicode());
      return new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
    } catch (EdmPrimitiveTypeException e) {
      throw new SerializerException("Error in primitive-value formatting.", e,
          SerializerException.MessageKeys.WRONG_PRIMITIVE_VALUE,
          type.getFullQualifiedName().getFullQualifiedNameAsString(), value.toString());
    }
  }

  @Override
  public InputStream asyncResponse(ODataResponse odataResponse) throws SerializerException {
    AsyncResponseSerializer serializer = new AsyncResponseSerializer();
    return serializer.serialize(odataResponse);
  }

  // TODO: Signature refactoring for writeBatchResponse
  @Override
  public InputStream batchResponse(List<ODataResponsePart> batchResponses, String boundary)
      throws BatchSerializerException {
    BatchResponseSerializer serializer = new BatchResponseSerializer();

    return serializer.serialize(batchResponses, boundary);
  }
}
