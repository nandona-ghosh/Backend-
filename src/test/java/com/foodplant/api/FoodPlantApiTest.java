package com.foodplant.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo")
class FoodPlantApiTest extends TestSecurityCredentials {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
 String login(String name) throws Exception {
  String body=json.writeValueAsString(java.util.Map.of("username",name,"password",TEST_PASSWORD));
  String result=mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();
  return "Bearer "+json.readTree(result).get("accessToken").asText();
 }
 @Test void authenticationAndRoles() throws Exception {
  String user=login("user"), staff=login("staff"), admin=login("admin");
  mvc.perform(get("/api/v1/batches")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/batches").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/auth/token").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  String body="{\"code\":\"L-NEW\",\"name\":\"New line\"}";
  mvc.perform(post("/api/v1/product-lines").header("Authorization",user).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  String result=mvc.perform(post("/api/v1/product-lines").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  long id=json.readTree(result).get("id").asLong();
  mvc.perform(delete("/api/v1/product-lines/"+id).header("Authorization",staff)).andExpect(status().isForbidden());
  mvc.perform(delete("/api/v1/product-lines/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
 }
 @Test void batchTraceSearchAndMessyRecords() throws Exception {
  String user=login("user");
  mvc.perform(get("/api/v1/batches").param("number","B-001").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(2));
  mvc.perform(get("/api/v1/batches").param("lineId","2").param("status","HOLD").param("from","2025-11-01").param("to","2025-11-30").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
  mvc.perform(get("/api/v1/batches").param("size","1").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.totalItems").value(3));
  mvc.perform(get("/api/v1/batches/1/trace").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$.batch.numberingEpoch").value("PRE-RESET")).andExpect(jsonPath("$.qualityTests[0].recordedAt").value("2025-10-11T08:00:00Z")).andExpect(jsonPath("$.materials[0].supplier").value("Aravali Farms"));
  mvc.perform(get("/api/v1/batches/2/trace").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$.batch.numberingEpoch").value("POST-RESET")).andExpect(jsonPath("$.qualityTests[0].moisturePercent").doesNotExist());
  mvc.perform(get("/api/v1/inventory/material-lots").header("Authorization",user)).andExpect(status().isOk()).andExpect(jsonPath("$[0].availableKg").value(400));
  mvc.perform(get("/api/v1/batches/99999/trace").header("Authorization",user)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
 }
 @Test void crudValidationAndConflicts() throws Exception {
  String staff=login("staff"),admin=login("admin");
  mvc.perform(get("/api/v1/product-lines").param("search","frozen").header("Authorization",staff)).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
  mvc.perform(get("/api/v1/suppliers/1").header("Authorization",staff)).andExpect(status().isOk());
  String supplier="{\"code\":\"NEW-S\",\"name\":\"New supplier\",\"contact\":null}";
  String result=mvc.perform(post("/api/v1/suppliers").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(supplier)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  long id=json.readTree(result).get("id").asLong();
  mvc.perform(put("/api/v1/suppliers/"+id).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(supplier.replace("New supplier","Updated"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated"));
  mvc.perform(post("/api/v1/suppliers").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(supplier)).andExpect(status().isConflict());
  mvc.perform(delete("/api/v1/suppliers/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
  mvc.perform(delete("/api/v1/suppliers/1").header("Authorization",admin)).andExpect(status().isConflict());
  String batch="{\"batchNumber\":\"NEW\",\"numberingEpoch\":\"2026\",\"lineId\":1,\"productionDate\":\"2026-11-24\",\"shift\":\"DAY\",\"status\":\"PENDING\",\"quantityKg\":100}";
  String created=mvc.perform(post("/api/v1/batches").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(batch)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  long batchId=json.readTree(created).get("id").asLong();
  mvc.perform(put("/api/v1/batches/"+batchId).header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(batch.replace("PENDING","HOLD"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HOLD"));
  mvc.perform(delete("/api/v1/batches/"+batchId).header("Authorization",admin)).andExpect(status().isNoContent());
  mvc.perform(post("/api/v1/batches").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(batch.replace("\"lineId\":1","\"lineId\":99999"))).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/batches").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(batch.replace("DAY","INVALID"))).andExpect(status().isBadRequest());
  mvc.perform(put("/api/v1/batches/1").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(batch.replace("\"lineId\":1","\"lineId\":2"))).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/batches").param("size","101").header("Authorization",staff)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/batches").param("from","nonsense").header("Authorization",staff)).andExpect(status().isBadRequest());
 }
 @Test void rejectsUntrustedOrExpiredTokens() throws Exception {
  java.time.Instant now=java.time.Instant.now();
  for (String issuer : new String[]{"wrong", "food-plant"}) {
   var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer(issuer).subject("admin").issuedAt(now.minusSeconds(60)).expiresAt(issuer.equals("wrong") ? now.plusSeconds(60) : now.minusSeconds(1)).claim("roles",java.util.List.of("ADMIN")).build();
   var header=org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
   String token=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(header,claims)).getTokenValue();
   mvc.perform(get("/api/v1/batches").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
  }
  String good=login("admin"); String tampered=good.substring(0,good.lastIndexOf('.')+1)+"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
  mvc.perform(get("/api/v1/batches").header("Authorization",tampered)).andExpect(status().isUnauthorized());
 }
 @Test void traceWritesAndInventoryProtection() throws Exception {
  String staff=login("staff");
  String lot="{\"supplierId\":1,\"materialName\":\"Peas\",\"lotNumber\":\"TEST-LOT\",\"receivedKg\":100,\"receivedDate\":\"2026-01-01\"}";
  String created=mvc.perform(post("/api/v1/inventory/material-lots").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(lot)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  long lotId=json.readTree(created).get("id").asLong();
  String consume="{\"materialLotId\":"+lotId+",\"usedKg\":60}";
  mvc.perform(post("/api/v1/batches/1/materials").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(consume)).andExpect(status().isCreated());
  mvc.perform(post("/api/v1/batches/2/materials").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(consume)).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/machines").header("Authorization",staff)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
  String quality="{\"sampledAt\":\"2026-01-01T00:00:00Z\",\"recordedAt\":\"2026-01-02T00:00:00Z\",\"result\":\"PENDING\",\"moisturePercent\":null}";
  mvc.perform(post("/api/v1/batches/1/quality-tests").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(quality)).andExpect(status().isCreated());
  mvc.perform(post("/api/v1/batches/1/quality-tests").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(quality.replace("2026-01-02","2025-12-31"))).andExpect(status().isBadRequest());
  String run="{\"machineId\":1,\"startedAt\":\"2026-01-01T00:00:00Z\",\"endedAt\":\"2026-01-01T01:00:00Z\"}";
  mvc.perform(post("/api/v1/batches/1/machine-runs").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(run)).andExpect(status().isCreated());
  mvc.perform(post("/api/v1/batches/2/machine-runs").header("Authorization",staff).contentType(MediaType.APPLICATION_JSON).content(run)).andExpect(status().isBadRequest());
 }
 @Test void openApiPublished() throws Exception { mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/v1/batches']").exists()).andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists()); }
}
