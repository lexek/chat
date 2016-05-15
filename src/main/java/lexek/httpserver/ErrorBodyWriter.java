package lexek.httpserver;

import lexek.wschat.db.model.rest.ErrorModel;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ErrorBodyWriter implements MessageBodyWriter<ErrorModel> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return mediaType != MediaType.APPLICATION_JSON_TYPE;
    }

    @Override
    public long getSize(ErrorModel errorModel, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(
        ErrorModel errorModel,
        Class<?> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders,
        OutputStream entityStream
    ) throws IOException, WebApplicationException {
        entityStream.write(errorModel.getMessage().getBytes());
    }
}
