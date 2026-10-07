package com.onrender.zipai.config;

import com.onrender.zipai.service.NativeDemoService;
import com.onrender.zipai.service.ZipaiPasswordService;
import jakarta.servlet.FilterChain;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NativeDemoFilterTest {
    private final NativeDemoService demo=new NativeDemoService(new ZipaiPasswordService());
    @Test void demoWritesNeverReachOperatingController() throws Exception {
        var session=new MockHttpSession();demo.start(session,"admin");
        var request=new MockHttpServletRequest("POST","/api/unknown-import");request.setSession(session);
        var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);
        new NativeDemoFilter(demo,new ObjectMapper()).doFilter(request,response,chain);
        assertEquals(404,response.getStatus());verifyNoInteractions(chain);
    }
    @Test void ordinaryRequestsKeepExistingFlow() throws Exception {
        var request=new MockHttpServletRequest("GET","/api/auth/me");
        var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);
        new NativeDemoFilter(demo,new ObjectMapper()).doFilter(request,response,chain);
        verify(chain).doFilter(request,response);
    }
    @Test void modesShareOnlyVisitorsOwnInquiry() {
        var session=new MockHttpSession();demo.start(session,"user");
        var result=(Map<?,?>)demo.request(session,"/api/inquiries","POST",Map.of("title","체험 문의","message","질문"),Map.of());
        long id=((Number)result.get("id")).longValue();
        demo.start(session,"admin");demo.request(session,"/api/admin/inquiries/"+id+"/answer","POST",Map.of("answer","체험 답변"),Map.of());
        demo.start(session,"user");
        var rows=(java.util.List<?>)((Map<?,?>)demo.request(session,"/api/inquiries","GET",Map.of(),Map.of())).get("items");
        assertEquals("체험 답변",((Map<?,?>)rows.get(1)).get("answer"));
        var other=new MockHttpSession();demo.start(other,"user");
        assertEquals(1,((java.util.List<?>)((Map<?,?>)demo.request(other,"/api/inquiries","GET",Map.of(),Map.of())).get("items")).size());
    }
}
