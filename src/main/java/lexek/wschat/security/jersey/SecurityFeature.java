package lexek.wschat.security.jersey;

import lexek.wschat.chat.model.GlobalRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.lang.annotation.Annotation;

public class SecurityFeature implements DynamicFeature {
    private final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        RequiredRole a = null;
        for (Annotation annotation : resourceInfo.getResourceMethod().getDeclaredAnnotations()) {
            if (annotation instanceof RequiredRole) {
                a = (RequiredRole) annotation;
            }
        }
        //check class annotations if no method annotation found
        if (a == null) {
            for (Annotation annotation : resourceInfo.getResourceClass().getAnnotations()) {
                if (annotation instanceof RequiredRole) {
                    a = (RequiredRole) annotation;
                }
            }
        }
        if (a != null && a.value() != GlobalRole.UNAUTHENTICATED) {
            context.register(new SecurityFilter(a.value()));
            logger.debug("registering security filter for {}", resourceInfo);
        }
    }
}
