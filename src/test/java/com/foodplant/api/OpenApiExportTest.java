package com.foodplant.api;
import java.nio.file.*;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.test.context.ActiveProfiles; import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("demo") @EnabledIfSystemProperty(named="exportOpenApi",matches="true")
class OpenApiExportTest extends TestSecurityCredentials {
 @Autowired MockMvc mvc;
 @Test void export() throws Exception { var result=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn(); Files.createDirectories(Path.of("docs")); Files.writeString(Path.of("docs/openapi.json"),result.getResponse().getContentAsString()); }
}
