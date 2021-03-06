package net.ttddyy.dsproxy.r2dbc.proxy;

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Result;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.support.LastExecutionAwareListener;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tadaya Tsuyukubo
 */
public class ReactiveBatchCallbackTest {

    private static Method ADD_METHOD = ReflectionUtils.findMethod(Batch.class, "add", String.class);
    private static Method EXECUTE_METHOD = ReflectionUtils.findMethod(Batch.class, "execute");
    private static Method GET_TARGET_METHOD = ReflectionUtils.findMethod(ProxyObject.class, "getTarget");

    @Test
    void batchOperation() throws Throwable {
        LastExecutionAwareListener testListener = new LastExecutionAwareListener();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.addListener(testListener);
        Batch batch = mock(Batch.class);
        ReactiveBatchCallback callback = new ReactiveBatchCallback(batch, connectionInfo, proxyConfig);

        // mock batch execution
        when(batch.execute()).thenReturn(Flux.empty());

        String query1 = "QUERY-1";
        String query2 = "QUERY-2";

        callback.invoke(null, ADD_METHOD, new String[]{query1});
        callback.invoke(null, ADD_METHOD, new String[]{query2});

        Object result = callback.invoke(null, EXECUTE_METHOD, new String[]{});


        StepVerifier.create((Publisher<? extends Result>) result)
                .verifyComplete();

        QueryExecutionInfo beforeQueryInfo = testListener.getBeforeQueryExecutionInfo();
        QueryExecutionInfo afterQueryInfo = testListener.getAfterQueryExecutionInfo();

        assertThat(beforeQueryInfo).isNotNull();
        assertThat(beforeQueryInfo.getBatchSize()).isEqualTo(2);
        assertThat(beforeQueryInfo.getBindingsSize()).isEqualTo(0);
        assertThat(beforeQueryInfo.isSuccess()).isTrue();
        assertThat(beforeQueryInfo.getType()).isEqualTo(ExecutionType.BATCH);
        assertThat(beforeQueryInfo.getConnectionInfo()).isSameAs(connectionInfo);
        assertThat(beforeQueryInfo.getQueries())
                .extracting(QueryInfo::getQuery)
                .containsExactly("QUERY-1", "QUERY-2");

        assertThat(afterQueryInfo).isNotNull();
        assertThat(afterQueryInfo.getBatchSize()).isEqualTo(2);
        assertThat(afterQueryInfo.getBindingsSize()).isEqualTo(0);
        assertThat(afterQueryInfo.isSuccess()).isTrue();
        assertThat(afterQueryInfo.getType()).isEqualTo(ExecutionType.BATCH);
        assertThat(afterQueryInfo.getConnectionInfo()).isSameAs(connectionInfo);
        assertThat(afterQueryInfo.getQueries())
                .extracting(QueryInfo::getQuery)
                .containsExactly("QUERY-1", "QUERY-2");

    }

    @Test
    void getTarget() throws Throwable {
        Batch batch = mock(Batch.class);
        ConnectionInfo connectionInfo = new ConnectionInfo();
        ProxyConfig proxyConfig = new ProxyConfig();

        ReactiveBatchCallback callback = new ReactiveBatchCallback(batch, connectionInfo, proxyConfig);

        Object result = callback.invoke(null, GET_TARGET_METHOD, null);
        assertSame(batch, result);
    }

}
