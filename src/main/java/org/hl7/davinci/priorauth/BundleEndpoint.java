package org.hl7.davinci.priorauth;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.hl7.davinci.priorauth.Endpoint.RequestType;

/**
 * The Bundle endpoint to READ, SEARCH for, and DELETE submitted Bundles.
 */
@RestController
@RequestMapping("/fhir/Bundle")
public class BundleEndpoint {

  private static String uri;

  @GetMapping(value = "", produces = "application/fhir+json")
  public ResponseEntity<String> readBundle(HttpServletRequest request,
      @RequestParam(name = "identifier", required = false) String id,
      @RequestParam(name = "patient.identifier") String patient) {
    uri = request.getRequestURL().toString();
    Map<String, Object> constraintMap = new HashMap<String, Object>();
    constraintMap.put("id", id);
    constraintMap.put("patient", patient);
    return Endpoint.read(Database.BUNDLE, constraintMap, uri, RequestType.JSON);
  }

  @GetMapping(value = "", produces = "application/fhir+xml")
  public ResponseEntity<String> readBundleXml(HttpServletRequest request,
      @RequestParam(name = "identifier", required = false) String id,
      @RequestParam(name = "patient.identifier") String patient) {
    uri = request.getRequestURL().toString();
    Map<String, Object> constraintMap = new HashMap<String, Object>();
    constraintMap.put("id", id);
    constraintMap.put("patient", patient);
    return Endpoint.read(Database.BUNDLE, constraintMap, uri, RequestType.XML);
  }

  @DeleteMapping(value = "", produces = "application/fhir+json")
  public ResponseEntity<String> deleteBundle(@RequestParam(name = "identifier") String id,
      @RequestParam(name = "patient.identifier") String patient) {
    return Endpoint.delete(id, patient, Database.BUNDLE, RequestType.JSON);
  }

  @DeleteMapping(value = "", produces = "application/fhir+xml")
  public ResponseEntity<String> deleteBundleXml(@RequestParam(name = "identifier") String id,
      @RequestParam(name = "patient.identifier") String patient) {
    return Endpoint.delete(id, patient, Database.BUNDLE, RequestType.XML);
  }
}
