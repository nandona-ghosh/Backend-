package com.foodplant.api;
import java.net.URI; import java.net.http.*; import java.time.Duration; import java.util.*; import java.util.concurrent.*; import java.util.concurrent.locks.LockSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest; import org.springframework.boot.test.web.server.LocalServerPort; import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("demo") @EnabledIfSystemProperty(named="runLocalLoad",matches="true")
class LocalLoadTest extends TestSecurityCredentials {
 @LocalServerPort int port;
 @Test void repeatedListAt200PerSecond() throws Exception {
  HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(); String base="http://127.0.0.1:"+port;
  String body=new ObjectMapper().writeValueAsString(Map.of("username", "user", "password", TEST_PASSWORD));
  var login=HttpRequest.newBuilder(URI.create(base+"/api/v1/auth/token")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
  var auth=client.send(login,HttpResponse.BodyHandlers.ofString()); assertEquals(200,auth.statusCode());
  String token=new ObjectMapper().readTree(auth.body()).get("accessToken").asText();
  var request=HttpRequest.newBuilder(URI.create(base+"/api/v1/batches?size=20")).timeout(Duration.ofSeconds(10)).header("Authorization","Bearer "+token).GET().build();
  for(int i=0;i<30;i++) assertEquals(200,client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode());
  var pool=Executors.newFixedThreadPool(40); List<Future<long[]>> jobs=new ArrayList<>(); long start=System.nanoTime();
  for(int i=0;i<1000;i++) { long deadline=start+i*5_000_000L; while(System.nanoTime()<deadline) LockSupport.parkNanos(Math.max(1,deadline-System.nanoTime()));
   jobs.add(pool.submit(()->{long t=System.nanoTime(); try {int status=client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode(); return new long[]{System.nanoTime()-t,status};} catch(Exception e){return new long[]{System.nanoTime()-t,0};}})); }
  List<Long> timings=new ArrayList<>(); int errors=0; for(var job:jobs){long[] result=job.get(30,TimeUnit.SECONDS); timings.add(result[0]); if(result[1]!=200) errors++;}
  long elapsed=System.nanoTime()-start; pool.shutdown(); Collections.sort(timings);
  System.out.printf(Locale.ROOT,"LOCAL_LOAD requests=1000 errors=%d elapsed_s=%.3f completed_rps=%.1f p50_ms=%.2f p95_ms=%.2f p99_ms=%.2f%n",errors,elapsed/1e9,1000/(elapsed/1e9),timings.get(499)/1e6,timings.get(949)/1e6,timings.get(989)/1e6);
  assertEquals(0,errors);
 }
}
