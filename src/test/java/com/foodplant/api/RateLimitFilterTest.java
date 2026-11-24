package com.foodplant.api;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*;
class RateLimitFilterTest {
 @Test void limitsIndependentlyAndResets() { var f=new RateLimitFilter(3,2); assertTrue(f.allow("a",true,0)); assertTrue(f.allow("a",true,1)); assertFalse(f.allow("a",true,2)); assertTrue(f.allow("b",true,2)); assertTrue(f.allow("a",false,2)); assertTrue(f.allow("a",true,60000)); }
 @Test void returns429FromFilter() throws Exception {
  var f=new RateLimitFilter(1,1); var request=new org.springframework.mock.web.MockHttpServletRequest("GET","/api/v1/batches");
  var first=new org.springframework.mock.web.MockHttpServletResponse(); var second=new org.springframework.mock.web.MockHttpServletResponse();
  f.doFilter(request,first,new org.springframework.mock.web.MockFilterChain());
  f.doFilter(request,second,new org.springframework.mock.web.MockFilterChain());
  assertEquals(429,second.getStatus()); assertEquals("60",second.getHeader("Retry-After"));
 }
 @Test void rejectsInvalidSettings() {assertThrows(IllegalArgumentException.class,()->new RateLimitFilter(0,1));}
}
