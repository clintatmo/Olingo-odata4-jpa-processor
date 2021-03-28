/* Copyright Buildƒorce Digital i.o. 2021
 * Licensed under the EUPL-1.2-or-later
*/
package nl.buildforce.olingo.server.core.batchhandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import nl.buildforce.olingo.commons.api.ex.ODataException;
import nl.buildforce.olingo.commons.api.format.ContentType;
import nl.buildforce.olingo.commons.api.http.HttpMethod;
import nl.buildforce.olingo.commons.api.http.HttpStatusCode;
import nl.buildforce.olingo.server.api.OData;
import nl.buildforce.olingo.server.api.ODataApplicationException;
import nl.buildforce.olingo.server.api.ODataLibraryException;
import nl.buildforce.olingo.server.api.ODataRequest;
import nl.buildforce.olingo.server.api.ODataResponse;
import nl.buildforce.olingo.server.api.ServiceMetadata;
import nl.buildforce.olingo.server.api.batch.BatchFacade;
import nl.buildforce.olingo.server.api.deserializer.batch.BatchDeserializerException;
import nl.buildforce.olingo.server.api.deserializer.batch.BatchOptions;
import nl.buildforce.olingo.server.api.deserializer.batch.BatchRequestPart;
import nl.buildforce.olingo.server.api.deserializer.batch.ODataResponsePart;
import nl.buildforce.olingo.server.api.processor.BatchProcessor;
import nl.buildforce.olingo.server.core.ODataHandlerImpl;
import nl.buildforce.olingo.server.core.deserializer.batch.BatchLineReader;
import nl.buildforce.olingo.server.core.deserializer.batch.BatchParserCommon;
import static nl.buildforce.olingo.commons.api.http.HttpHeader.CONTENT_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.LOCATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockedBatchHandlerTest {

  private BatchHandler batchHandler;
  private ODataHandlerImpl oDataHandler;
  private int entityCounter = 1;
  private static final String BASE_URI = "http://localhost:8080/odata";
  private static final String BATCH_CONTENT_TYPE = "multipart/mixed;boundary=batch_12345";
  private static final String BATCH_ODATA_PATH = "/$batch";
  private static final String BATCH_REQUEST_URI = "http://localhost:8080/odata/$batch";
  private static final String CRLF = "\r\n";

  @Before
  public void setup() {
    BatchProcessor batchProcessor = new BatchTestProcessorImpl();
    batchProcessor.init(OData.newInstance(), null);

    entityCounter = 1;
    oDataHandler = mock(ODataHandlerImpl.class);
    batchHandler = new BatchHandler(oDataHandler, batchProcessor);
  }

  @Test
  public void test() throws Exception {
    final String content = "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_12345" + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 3" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 4" + CRLF
        + CRLF
        + "PUT $3/PropertyInt32 HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "POST ESAllPrim HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 5" + CRLF
        + CRLF
        + "POST http://localhost:8080/odata/$1/NavPropertyETTwoPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 2" + CRLF
        + CRLF
        + "POST $1/NavPropertyETTwoPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 6" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345--" + CRLF
        + CRLF
        + "--batch_12345--";
    Map<String, List<String>> header = getMimeHeader();
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);

    batchHandler.process(request, response);

    BatchLineReader reader =
        new BatchLineReader(response.getContent());

    List<String> responseContent = reader.toList();
    reader.close();

    int line = 0;
    assertEquals(62, responseContent.size());

    // Check change set
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertTrue(responseContent.get(line++).contains("Content-Type: multipart/mixed; boundary=changeset_"));

    for (int i = 0; i < 6; i++) {
      String contentId = checkChangeSetPartHeader(responseContent, line);
      line += 6;

        switch (contentId) {
            case "1":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESAllPrim(1)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "2":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESTwoPrim(3)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "3":
            case "6":
            case "4":
                assertEquals("HTTP/1.1 200 OK" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "5":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESTwoPrim(2)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            default:
                fail();
                break;
        }
      assertEquals(CRLF, responseContent.get(line++));
    }

    // Close body part (change set)
    assertEquals(CRLF, responseContent.get(line++));
    assertTrue(responseContent.get(line++).contains("--changeset_"));

    // Close batch
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertEquals(62, line);
  }

  @Test
  public void testGetRequest() throws Exception {
    final String content = ""
        + "--batch_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + CRLF
        + "GET ESAllPrim(0) HTTP/1.1" + CRLF
        + CRLF
        + CRLF
        + "--batch_12345--";

    Map<String, List<String>> header = getMimeHeader();
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);

    batchHandler.process(request, response);

    BatchLineReader reader =
        new BatchLineReader(response.getContent());

    List<String> responseContent = reader.toList();
    int line = 0;

    assertEquals(9, responseContent.size());
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertEquals("Content-Type: application/http" + CRLF, responseContent.get(line++));
    assertEquals("Content-Transfer-Encoding: binary" + CRLF, responseContent.get(line++));
    assertEquals(CRLF, responseContent.get(line++));
    assertEquals("HTTP/1.1 200 OK" + CRLF, responseContent.get(line++));
    assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
    assertEquals(CRLF, responseContent.get(line++));
    assertEquals(CRLF, responseContent.get(line++));
    assertTrue(responseContent.get(line++).contains("--batch_"));

    assertEquals(9, line);

    reader.close();
  }

  @Test
  public void testMultipleChangeSets() throws Exception {
    final String content = ""
        + "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_12345" + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 2" + CRLF
        + CRLF
        + "POST $1/NavPropertyETTwoPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345--" + CRLF

        + "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_54321" + CRLF
        + CRLF
        + "--changeset_54321" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "PUT http://localhost:8080/odata/ESAllPrim(2) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_54321" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 2" + CRLF
        + CRLF
        + "POST $1/NavPropertyETTwoPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_54321--" + CRLF

        + CRLF
        + "--batch_12345--";
    Map<String, List<String>> header = getMimeHeader();
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);

    batchHandler.process(request, response);

    BatchLineReader reader =
        new BatchLineReader(response.getContent());

    List<String> responseContent = reader.toList();
    reader.close();

    int line = 0;
    assertEquals(47, responseContent.size());

    // Check first change set
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertTrue(responseContent.get(line++).contains("Content-Type: multipart/mixed; boundary=changeset_"));

    for (int i = 0; i < 2; i++) {
      String contentId = checkChangeSetPartHeader(responseContent, line);
      line += 6;

      if ("1".equals(contentId)) {
        assertEquals("HTTP/1.1 200 OK" + CRLF, responseContent.get(line++));
        assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
      } else if ("2".equals(contentId)) {
        assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
        assertEquals("Location: " + BASE_URI + "/ESTwoPrim(1)" + CRLF, responseContent.get(line++));
        assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
      } else {
        fail();
      }

      assertEquals(CRLF, responseContent.get(line++));
    }
    // Close body part (1st change set)
    assertEquals(CRLF, responseContent.get(line++));
    assertTrue(responseContent.get(line++).contains("--changeset_"));

    // Check second change set
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertTrue(responseContent.get(line++).contains("Content-Type: multipart/mixed; boundary=changeset_"));

    for (int i = 0; i < 2; i++) {
      String contentId = checkChangeSetPartHeader(responseContent, line);
      line += 6;

      if ("1".equals(contentId)) {
        assertEquals("HTTP/1.1 200 OK" + CRLF, responseContent.get(line++));
        assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
      } else if ("2".equals(contentId)) {
        assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
        assertEquals("Location: " + BASE_URI + "/ESTwoPrim(2)" + CRLF, responseContent.get(line++));
        assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
      } else {
        fail();
      }

      assertEquals(CRLF, responseContent.get(line++));
    }
    // Close body part (2nd change set)
    assertEquals(CRLF, responseContent.get(line++));
    assertTrue(responseContent.get(line++).contains("--changeset_"));

    // Close batch
    assertTrue(responseContent.get(line++).contains("--batch_"));

    assertEquals(47, line);
  }

  @Test
  public void mimeBodyPartTransitive() throws Exception {
    final String content = ""
        + "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_12345" + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 2" + CRLF
        + CRLF
        + "POST $1/NavPropertyETTwoPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 3" + CRLF
        + CRLF
        + "POST $2/NavPropertyETAllPrimMany HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 4" + CRLF
        + CRLF
        + "POST $3/NavPropertyETTwoPrimOne HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345--" + CRLF

        + CRLF
        + "--batch_12345--";

    Map<String, List<String>> header = getMimeHeader();
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);

    batchHandler.process(request, response);

    BatchLineReader reader =
        new BatchLineReader(response.getContent());

    List<String> responseContent = reader.toList();
    reader.close();

    int line = 0;
    assertEquals(44, responseContent.size());

    // Check change set
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertTrue(responseContent.get(line++).contains("Content-Type: multipart/mixed; boundary=changeset_"));

    for (int i = 0; i < 4; i++) {
      String contentId = checkChangeSetPartHeader(responseContent, line);
      line += 6;

        switch (contentId) {
            case "1":
                assertEquals("HTTP/1.1 200 OK" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "2":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESTwoPrim(1)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "3":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESAllPrim(2)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            case "4":
                assertEquals("HTTP/1.1 201 Created" + CRLF, responseContent.get(line++));
                assertEquals("Location: " + BASE_URI + "/ESTwoPrim(3)" + CRLF, responseContent.get(line++));
                assertEquals("Content-Length: 0" + CRLF, responseContent.get(line++));
                break;
            default:
                fail();
                break;
        }

      assertEquals(CRLF, responseContent.get(line++));
    }

    // Close body part (change set)
    assertEquals(CRLF, responseContent.get(line++));
    assertTrue(responseContent.get(line++).contains("--changeset_"));

    // Close batch
    assertTrue(responseContent.get(line++).contains("--batch_"));
    assertEquals(44, line);
  }

  @Test(expected = BatchDeserializerException.class)
  public void testInvalidMethod() throws Exception {
    final String content = ""
        + "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_12345" + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345--" + CRLF
        + CRLF
        + "--batch_12345--";

    Map<String, List<String>> header = getMimeHeader();
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);
    request.setMethod(HttpMethod.GET);

    batchHandler.process(request, response);
  }

  @Test(expected = BatchDeserializerException.class)
  public void testInvalidContentType() throws Exception {
    final String content = ""
        + "--batch_12345" + CRLF
        + "Content-Type: multipart/mixed; boundary=changeset_12345" + CRLF
        + CRLF
        + "--changeset_12345" + CRLF
        + "Content-Type: application/http" + CRLF
        + "Content-Transfer-Encoding: binary" + CRLF
        + "Content-Id: 1" + CRLF
        + CRLF
        + "PUT ESAllPrim(1) HTTP/1.1" + CRLF
        + "Content-Type: application/json;odata=verbose" + CRLF
        + CRLF
        + CRLF
        + "--changeset_12345--" + CRLF
        + CRLF
        + "--batch_12345--";

    Map<String, List<String>> header = new HashMap<>();
    header.put(CONTENT_TYPE, Collections.singletonList("application/http"));
    ODataResponse response = new ODataResponse();
    ODataRequest request = buildODataRequest(content, header);

    batchHandler.process(request, response);
  }

  /*
   * Helper methods
   */
  private String checkChangeSetPartHeader(List<String> response, int line) {
    int lineNumber = line;
    assertEquals(CRLF, response.get(lineNumber++));
    assertTrue(response.get(lineNumber++).contains("--changeset_"));
    assertEquals("Content-Type: application/http" + CRLF, response.get(lineNumber++));
    assertEquals("Content-Transfer-Encoding: binary" + CRLF, response.get(lineNumber++));

    assertTrue(response.get(lineNumber).contains("Content-ID:"));
    String contentId = response.get(lineNumber).split(":")[1].trim();
    lineNumber++;

    assertEquals(CRLF, response.get(lineNumber++));

    return contentId;
  }

  private Map<String, List<String>> getMimeHeader() {
    return Collections.singletonMap(CONTENT_TYPE, Collections.singletonList(BATCH_CONTENT_TYPE));
  }

  private ODataRequest buildODataRequest(String content, Map<String, List<String>> header) {
    ODataRequest request = new ODataRequest();

    for (String key : header.keySet()) {
      request.addHeader(key, header.get(key));
    }

    request.setMethod(HttpMethod.POST);
    request.setRawBaseUri(BASE_URI);
    request.setRawODataPath(BATCH_ODATA_PATH);
    request.setRawQueryPath("");
    request.setRawRequestUri(BATCH_REQUEST_URI);
    request.setRawServiceResolutionUri("");

    request.setBody(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

    return request;
  }

  /**
   * Batch processor
   */
  private class BatchTestProcessorImpl implements BatchProcessor {

    private OData odata;

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
      this.odata = odata;
    }

    @Override
    public ODataResponsePart processChangeSet(BatchFacade facade, List<ODataRequest> requests) {
      List<ODataResponse> responses = new ArrayList<>();

      for (ODataRequest request : requests) {
        try {
          responses.add(facade.handleODataRequest(request));
        } catch (ODataException e) {
          fail();
        }
      }

      return new ODataResponsePart(responses, true);
    }

    @Override
    public void processBatch(BatchFacade fascade, ODataRequest request, ODataResponse response)
        throws ODataApplicationException, ODataLibraryException {
      String boundary = getBoundary(request.getHeader(CONTENT_TYPE));
      BatchOptions options = BatchOptions.with().isStrict(true).rawBaseUri(BASE_URI).build();
      List<BatchRequestPart> parts =
          odata.createFixedFormatDeserializer().parseBatchRequest(request.getBody(), boundary, options);
      List<ODataResponsePart> responseParts = new ArrayList<>();

      for (BatchRequestPart part : parts) {
        for (ODataRequest oDataRequest : part.getRequests()) {
          // Mock the processor for a given requests
          when(oDataHandler.process(oDataRequest)).then((Answer<ODataResponse>) invocation -> {
            Object[] arguments = invocation.getArguments();

            return buildResponse((ODataRequest) arguments[0]);
          });
        }

        responseParts.add(fascade.handleBatchRequest(part));
      }

      String responeBoundary = "batch_" + UUID.randomUUID().toString();
      InputStream responseStream =
          odata.createFixedFormatSerializer().batchResponse(responseParts, responeBoundary);

      response.setStatusCode(HttpStatusCode.ACCEPTED.getStatusCode());
      response.setHeader(CONTENT_TYPE, ContentType.MULTIPART_MIXED + ";boundary=" + responeBoundary);
      response.setContent(responseStream);
    }

    private String getBoundary(String contentType) throws BatchDeserializerException {
      return BatchParserCommon.getBoundary(contentType, 0);
    }
  }

  private ODataResponse buildResponse(ODataRequest request) {
    ODataResponse oDataResponse = new ODataResponse();

    if (request.getMethod() == HttpMethod.POST) {
      oDataResponse.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
      oDataResponse.setHeader(LOCATION, createResourceUri(request));
    } else {
      oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
    }

    String contentId = request.getHeader(CONTENT_ID);
    if (contentId != null) {
      oDataResponse.setHeader(CONTENT_ID, contentId);
    }

    return oDataResponse;
  }

  private String createResourceUri(ODataRequest request) {
    String[] parts = request.getRawODataPath().split("/");
    String oDataPath = "";

    if (parts.length == 2) {
      // Entity Collection
      oDataPath = parts[1];
    } else {
      // Navigation property

      String navProperty = parts[parts.length - 1];
        switch (navProperty) {
            case "NavPropertyETTwoPrimMany":
            case "NavPropertyETTwoPrimOne":
                oDataPath = "ESTwoPrim";
                break;
            case "NavPropertyETAllPrimMany":
                oDataPath = "ESAllPrim";
                break;
        }
    }

    return BASE_URI + "/" + oDataPath + "(" + entityCounter++ + ")";
  }

}