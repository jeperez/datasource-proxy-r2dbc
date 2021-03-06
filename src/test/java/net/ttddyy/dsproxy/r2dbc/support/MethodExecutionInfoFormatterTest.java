package net.ttddyy.dsproxy.r2dbc.support;

import io.r2dbc.spi.ConnectionFactory;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.MethodExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.support.MethodExecutionInfoFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Tadaya Tsuyukubo
 */
public class MethodExecutionInfoFormatterTest {

    @Test
    void withDefault() {

        // String#indexOf(int) method
        Method method = ReflectionUtils.findMethod(String.class, "indexOf", int.class);

        Long target = 100L;

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId("ABC");

        MethodExecutionInfo executionInfo = new MethodExecutionInfo();
        executionInfo.setThreadId(5);
        executionInfo.setConnectionInfo(connectionInfo);
        executionInfo.setExecuteDuration(Duration.of(23, ChronoUnit.MILLIS));
        executionInfo.setMethod(method);
        executionInfo.setTarget(target);

        MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
        String result = formatter.format(executionInfo);

        assertEquals("  1: Thread:5 Connection:ABC Time:23  Long#indexOf()", result);

        // second time should increase the sequence
        result = formatter.format(executionInfo);
        assertEquals("  2: Thread:5 Connection:ABC Time:23  Long#indexOf()", result);

    }

    @Test
    void nullConnectionId() {

        // connection id is null for before execution of "ConnectionFactory#create"
        Method method = ReflectionUtils.findMethod(ConnectionFactory.class, "create");

        Long target = 100L;

        // null ConnectionInfo
        MethodExecutionInfo executionInfo = new MethodExecutionInfo();
        executionInfo.setThreadId(5);
        executionInfo.setConnectionInfo(null);
        executionInfo.setExecuteDuration(Duration.of(23, ChronoUnit.MILLIS));
        executionInfo.setMethod(method);
        executionInfo.setTarget(target);

        MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();
        String result = formatter.format(executionInfo);

        assertEquals("  1: Thread:5 Connection:n/a Time:23  Long#create()", result);

        // null ConnectionId
        executionInfo.setConnectionInfo(new ConnectionInfo());
        result = formatter.format(executionInfo);

        assertEquals("  2: Thread:5 Connection:n/a Time:23  Long#create()", result);
    }

    @Test
    void customConsumer() {

        MethodExecutionInfoFormatter formatter = new MethodExecutionInfoFormatter();
        formatter.addConsumer((executionInfo, sb) -> {
            sb.append("ABC");
        });
        String result = formatter.format(new MethodExecutionInfo());

        assertEquals("ABC", result);

    }

}
