package lexek.wschat.frontend.http.admin;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpMethod;
import lexek.httpserver.Request;
import lexek.httpserver.Response;
import lexek.httpserver.SimpleHttpHandler;
import lexek.wschat.chat.GlobalRole;
import lexek.wschat.db.jooq.tables.pojos.Metric;
import lexek.wschat.db.jooq.tables.pojos.Stream;
import lexek.wschat.security.AuthenticationManager;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static lexek.wschat.db.jooq.tables.Metric.METRIC;
import static lexek.wschat.db.jooq.tables.Stream.STREAM;

public class MetricHandler extends SimpleHttpHandler {
    private final DataSource dataSource;
    private final AuthenticationManager authenticationManager;

    public MetricHandler(DataSource dataSource, AuthenticationManager authenticationManager) {
        this.dataSource = dataSource;
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void handle(Request request, Response response) throws Exception {
        if (request.method() == HttpMethod.GET) {
            if (authenticationManager.hasRole(request, GlobalRole.ADMIN)) {
                List<Metric> metrics;
                List<Stream> streams;
                try (Connection connection = dataSource.getConnection()) {
                    long startTime = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(24, TimeUnit.HOURS);
                    metrics = DSL.using(connection)
                        .select(METRIC.NAME, METRIC.TIME, METRIC.VALUE)
                        .from(METRIC)
                        .where(METRIC.TIME.greaterOrEqual(startTime))
                        .orderBy(METRIC.TIME)
                        .fetch()
                        .into(Metric.class);
                    streams = DSL.using(connection)
                        .select()
                        .from(STREAM)
                        .where(STREAM.ENDED.greaterOrEqual(new Timestamp(startTime)))
                        .orderBy(STREAM.STARTED)
                        .fetch()
                        .into(Stream.class);
                    response.jsonContent(ImmutableMap.of("metrics", metrics, "streams", streams));
                } catch (DataAccessException | SQLException e) {
                    response.internalError();
                }
                return;
            }
        }
        response.badRequest();
    }
}
