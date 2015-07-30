package lexek.httpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;

public class ViewResolvers {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Configuration templateEngine;

    public ViewResolvers(Configuration templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Configuration getTemplateEngine() {
        return templateEngine;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
