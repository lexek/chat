package lexek.wschat.db.tx;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jooq.DSLContext;

public class TransactionalInterceptor implements MethodInterceptor {
    private final DSLContext ctx;

    public TransactionalInterceptor(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return ctx.transactionResult(configuration -> {
            try {
                return invocation.proceed();
            } catch (Exception e) {
                throw e;
            } catch (Throwable throwable) {
                throw new Exception(throwable);
            }
        });
    }
}
