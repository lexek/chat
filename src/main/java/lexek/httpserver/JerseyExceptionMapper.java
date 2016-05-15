package lexek.httpserver;

import lexek.wschat.chat.e.*;
import lexek.wschat.db.model.rest.ErrorModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class JerseyExceptionMapper implements ExceptionMapper<Throwable> {
    private final Logger logger = LoggerFactory.getLogger(JerseyExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        logger.trace("", exception);
        if (exception instanceof DomainException) {
            if (exception instanceof EntityNotFoundException) {
                return Response.status(404).entity(new ErrorModel(exception.getMessage())).build();
            }
            if (exception instanceof InvalidStateException) {
                return Response.status(400).entity(new ErrorModel(exception.getMessage())).build();
            }
            if (exception instanceof InvalidInputException) {
                return Response.status(400).entity(new ErrorModel(
                    "error",
                    ((InvalidInputException) exception).getData()
                )).build();
            }
            if (exception instanceof AccessDeniedException) {
                return Response.status(403).entity(new ErrorModel(exception.getMessage())).build();
            }
            logger.warn("exception", exception);
            return Response.status(500).entity(new ErrorModel(exception.getMessage())).build();
        }
        if (exception instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) exception;
            if (webApplicationException.getResponse() != null) {
                if (webApplicationException.getResponse().getStatus() == 500) {
                    logger.warn("exception", exception);
                }
                if (webApplicationException.getResponse().hasEntity()) {
                    return webApplicationException.getResponse();
                } else {
                    return Response
                        .fromResponse(webApplicationException.getResponse())
                        .entity(new ErrorModel(webApplicationException.getResponse().getStatusInfo().getReasonPhrase()))
                        .build();
                }
            }
        }
        if (exception instanceof ValidationException) {
            ValidationException validationException = (ValidationException) exception;
            return Response.status(400).entity(new ErrorModel(validationException.getMessage())).build();
        }
        if (exception instanceof NotFoundException) {
            return Response.status(404).entity(new ErrorModel("Not found.")).build();
        }
        logger.warn("exception", exception);
        return Response.status(500).entity(new ErrorModel("Internal error.")).build();
    }
}
