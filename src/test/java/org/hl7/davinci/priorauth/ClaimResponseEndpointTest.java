package org.hl7.davinci.priorauth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import ca.uhn.fhir.validation.ValidationResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ClaimResponseEndpointTest {

  @LocalServerPort
  private int port;

  private static OkHttpClient client;

  @BeforeClass
  public static void setup() throws FileNotFoundException {
    client = new OkHttpClient();
    App.initializeAppDB();

    // Create a single test Claim
    Path modulesFolder = Paths.get("src/test/resources");
    Path fixture = modulesFolder.resolve("claimresponse-minimal.json");
    FileInputStream inputStream = new FileInputStream(fixture.toString());

    Path claimFixture = modulesFolder.resolve("claim-minimal.json");
    FileInputStream claimInputStream = new FileInputStream(claimFixture.toString());
    Claim claim = (Claim) App.FHIR_CTX.newJsonParser().parseResource(claimInputStream);
    Map<String, Object> claimMap = new HashMap<String, Object>();
    claimMap.put("id", "minimal");
    claimMap.put("patient", "1");
    claimMap.put("status", FhirUtils.getStatusFromResource(claim));
    claimMap.put("resource", claim);
    App.getDB().write(Database.CLAIM, claimMap);

    Bundle claimResponse = (Bundle) App.FHIR_CTX.newJsonParser().parseResource(inputStream);
    Map<String, Object> claimResponseMap = new HashMap<String, Object>();
    claimResponseMap.put("id", "minimal");
    claimResponseMap.put("claimId", "minimal");
    claimResponseMap.put("patient", "1");
    claimResponseMap.put("status", FhirUtils.getStatusFromResource(claimResponse));
    claimResponseMap.put("resource", claimResponse);
    App.getDB().write(Database.CLAIM_RESPONSE, claimResponseMap);
  }

  @AfterClass
  public static void cleanup() {
    App.getDB().delete(Database.CLAIM, "minimal", "1");
    App.getDB().delete(Database.CLAIM_RESPONSE, "minimal", "1");
  }

  @Test
  public void searchClaimResponses() throws IOException {
    String base = "http://localhost:" + port + "/fhir";

    // Test that we can GET /fhir/ClaimResponse.
    Request request = new Request.Builder().url(base + "/ClaimResponse?patient.identifier=1")
        .header("Accept", "application/fhir+json").build();
    Response response = client.newCall(request).execute();
    Assert.assertEquals(200, response.code());

    // Test the response has CORS headers
    String cors = response.header("Access-Control-Allow-Origin");
    Assert.assertEquals("*", cors);

    // Test the response is a JSON Bundle
    String body = response.body().string();
    Bundle bundle = (Bundle) App.FHIR_CTX.newJsonParser().parseResource(body);
    Assert.assertNotNull(bundle);

    // Validate the response.
    ValidationResult result = ValidationHelper.validate(bundle);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void searchClaimResponsesXml() throws IOException {
    String base = "http://localhost:" + port + "/fhir";

    // Test that we can GET /fhir/ClaimResponse.
    Request request = new Request.Builder().url(base + "/ClaimResponse?patient.identifier=1")
        .header("Accept", "application/fhir+xml").build();
    Response response = client.newCall(request).execute();
    Assert.assertEquals(200, response.code());

    // Test the response has CORS headers
    String cors = response.header("Access-Control-Allow-Origin");
    Assert.assertEquals("*", cors);

    // Test the response is an XML Bundle
    String body = response.body().string();
    Bundle bundle = (Bundle) App.FHIR_CTX.newXmlParser().parseResource(body);
    Assert.assertNotNull(bundle);

    // Validate the response.
    ValidationResult result = ValidationHelper.validate(bundle);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void claimResponseExists() {
    Map<String, Object> constraintMap = new HashMap<String, Object>();
    constraintMap.put("id", "minimal");
    constraintMap.put("patient", "1");
    Bundle bundleResponse = (Bundle) App.getDB().read(Database.CLAIM_RESPONSE, constraintMap);
    Assert.assertNotNull(bundleResponse);
  }

  @Test
  public void getClaimResponse() throws IOException {
    String base = "http://localhost:" + port + "/fhir";

    // Test that we can GET /fhir/ClaimResponse/minimal
    Request request = new Request.Builder().url(base + "/ClaimResponse?identifier=minimal&patient.identifier=1")
        .header("Accept", "application/fhir+json").build();
    Response response = client.newCall(request).execute();
    Assert.assertEquals(200, response.code());

    // Test the response has CORS headers
    String cors = response.header("Access-Control-Allow-Origin");
    Assert.assertEquals("*", cors);

    // Test the response is a JSON Bundle
    String body = response.body().string();
    Bundle bundleResponse = (Bundle) App.FHIR_CTX.newJsonParser().parseResource(body);
    Assert.assertNotNull(bundleResponse);

    // Validate the response.
    ValidationResult result = ValidationHelper.validate(bundleResponse);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void getClaimResponseXml() throws IOException {
    String base = "http://localhost:" + port + "/fhir";

    // Test that we can GET /fhir/ClaimResponse/minimal
    Request request = new Request.Builder().url(base + "/ClaimResponse?identifier=minimal&patient.identifier=1")
        .header("Accept", "application/fhir+xml").build();
    Response response = client.newCall(request).execute();
    Assert.assertEquals(200, response.code());

    // Test the response has CORS headers
    String cors = response.header("Access-Control-Allow-Origin");
    Assert.assertEquals("*", cors);

    // Test the response is an XML Bundle
    String body = response.body().string();
    Bundle responseBundle = (Bundle) App.FHIR_CTX.newXmlParser().parseResource(body);
    Assert.assertNotNull(responseBundle);

    // Validate the response.
    ValidationResult result = ValidationHelper.validate(responseBundle);
    Assert.assertTrue(result.isSuccessful());
  }

  @Test
  public void getClaimResponseThatDoesNotExist() throws IOException {
    String base = "http://localhost:" + port + "/fhir";

    // Test that non-existent ClaimResponse returns 404.
    Request request = new Request.Builder()
        .url(base + "/ClaimResponse?identifier=ClaimResponseThatDoesNotExist&patient.identifier=45")
        .header("Accept", "application/fhir+json").build();
    Response response = client.newCall(request).execute();
    Assert.assertEquals(404, response.code());
  }
}
