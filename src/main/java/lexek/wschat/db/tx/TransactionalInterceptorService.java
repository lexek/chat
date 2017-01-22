package lexek.wschat.db.tx;

import com.google.common.collect.ImmutableList;
import org.aopalliance.intercept.ConstructorInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InterceptionService;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jooq.DSLContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

@Singleton
public class TransactionalInterceptorService implements InterceptionService {
    private final TransactionalInterceptor transactionalInterceptor;

    @Inject
    public TransactionalInterceptorService(DSLContext ctx) {
        transactionalInterceptor = new TransactionalInterceptor(ctx);
    }

    @Override
    public Filter getDescriptorFilter() {
        return BuilderHelper.allFilter();
    }

    @Override
    public List<MethodInterceptor> getMethodInterceptors(Method method) {
        if (method.isAnnotationPresent(Transactional.class)) {
            return ImmutableList.of(transactionalInterceptor);
        }
        return null;
    }

    @Override
    public List<ConstructorInterceptor> getConstructorInterceptors(Constructor<?> constructor) {
        return null;
    }
}
