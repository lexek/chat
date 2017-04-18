package lexek.wschat.security.jersey;

import lexek.wschat.chat.model.User;
import lexek.wschat.db.model.UserDto;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
import org.glassfish.jersey.server.model.Parameter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

@Singleton
public class UserParamValueFactoryProvider extends AbstractValueFactoryProvider {
    @Inject
    public UserParamValueFactoryProvider(MultivaluedParameterExtractorProvider mpep, ServiceLocator injector) {
        super(mpep, injector, Parameter.Source.UNKNOWN);
    }

    @Override
    protected Factory<?> createValueFactory(Parameter parameter) {
        if (parameter.getRawType().equals(UserDto.class)) {
            return new UserParamValueFactory();
        } else {
            return null;
        }
    }

    @Singleton
    public static final class InjectionResolver extends ParamInjectionResolver<Auth> {
        public InjectionResolver() {
            super(UserParamValueFactoryProvider.class);
        }
    }

    private static final class UserParamValueFactory extends AbstractContainerRequestValueFactory<User> {
        @Override
        public User provide() {
            ContainerRequest request = getContainerRequest();
            SecurityContext securityContext = request.getSecurityContext();
            if (securityContext != null) {
                Principal principal = securityContext.getUserPrincipal();
                if (principal != null && principal instanceof UserDto) {
                    return (User) principal;
                }
            }
            return null;
        }
    }
}
