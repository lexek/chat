package lexek.httpserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import freemarker.template.Configuration;

import java.util.Date;

public class ViewResolvers {
    private final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new GsonBuilder()
                .registerTypeAdapter(Date.class, (JsonSerializer<Date>) (src, typeOfSrc, context) -> new JsonPrimitive(src.getTime()))
                .create();
        }
    };

    private final Configuration templateEngine;

    public ViewResolvers(Configuration templateEngine) {
        this.templateEngine = templateEngine;
    }

    public Gson getGson() {
        return gson.get();
    }

    public Configuration getTemplateEngine() {
        return templateEngine;
    }
}
