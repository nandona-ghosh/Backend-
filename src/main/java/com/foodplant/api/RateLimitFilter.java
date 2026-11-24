package com.foodplant.api;
import java.io.IOException; import java.util.HashMap; import java.util.Map;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component class RateLimitFilter extends OncePerRequestFilter {
 private record Window(long started,int count) {}
 private final Map<String,Window> windows=new HashMap<>(); private final int general; private final int login;
 RateLimitFilter(@Value("${plant.rate.general-per-minute:12000}") int general,@Value("${plant.rate.login-per-minute:10}") int login) {
   if(general<1 || login<1) throw new IllegalArgumentException("Rate limits must be positive"); this.general=general;this.login=login;
 }
 synchronized boolean allow(String address,boolean isLogin,long now) {
   windows.entrySet().removeIf(e -> now-e.getValue().started() >= 60_000);
   String key=address+(isLogin?":login":":general"); Window w=windows.get(key);
   if(w==null) { if(windows.size()>=10000) return false; windows.put(key,new Window(now,1)); return true; }
   int limit=isLogin?login:general; if(w.count()>=limit) return false;
   windows.put(key,new Window(w.started(),w.count()+1)); return true;
 }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
   boolean auth="POST".equals(req.getMethod()) && "/api/v1/auth/token".equals(req.getServletPath());
   if(!allow(req.getRemoteAddr(),auth,System.currentTimeMillis())) {res.setStatus(429); res.setContentType("application/json"); res.setHeader("Retry-After","60"); res.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests\"}"); return;}
   chain.doFilter(req,res);
 }
}
