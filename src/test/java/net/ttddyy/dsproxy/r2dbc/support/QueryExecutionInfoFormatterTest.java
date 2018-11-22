package net.ttddyy.dsproxy.r2dbc.support;

import net.ttddyy.dsproxy.r2dbc.core.BindingValue.NullBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.BindingValue.SimpleBindingValue;
import net.ttddyy.dsproxy.r2dbc.core.Bindings;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionInfo;
import net.ttddyy.dsproxy.r2dbc.core.ExecutionType;
import net.ttddyy.dsproxy.r2dbc.core.QueryExecutionInfo;
import net.ttddyy.dsproxy.r2dbc.core.QueryInfo;
import net.ttddyy.dsproxy.r2dbc.support.QueryExecutionInfoFormatter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Tadaya Tsuyukubo
 */
public class QueryExecutionInfoFormatterTest {

    @Test
    void batchExecution() {

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId("conn-id");

        // Batch Query
        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);
        execInfo.setConnectionInfo(connectionInfo);
        execInfo.setSuccess(true);
        execInfo.setExecuteDuration(Duration.of(35, ChronoUnit.MILLIS));
        execInfo.setType(ExecutionType.BATCH);
        execInfo.setBatchSize(20);
        execInfo.setBindingsSize(10);
        execInfo.getQueries().addAll(Arrays.asList(new QueryInfo("SELECT A"), new QueryInfo("SELECT B")));

        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();

        String result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id " +
                "Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35 " +
                "Type:Batch BatchSize:20 BindingsSize:10 " +
                "Query:[\"SELECT A\",\"SELECT B\"] Bindings:[]", result);

    }

    @Test
    void statementExecution() {

        Bindings indexBindings1 = new Bindings();
        indexBindings1.addIndexBinding(0, new SimpleBindingValue("100"));
        indexBindings1.addIndexBinding(1, new SimpleBindingValue("101"));
        indexBindings1.addIndexBinding(2, new SimpleBindingValue("102"));

        Bindings indexBindings2 = new Bindings();
        indexBindings2.addIndexBinding(2, new SimpleBindingValue("202"));
        indexBindings2.addIndexBinding(1, new NullBindingValue(String.class));
        indexBindings2.addIndexBinding(0, new SimpleBindingValue("200"));


        Bindings idBindings1 = new Bindings();
        idBindings1.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        idBindings1.addIdentifierBinding("$1", new SimpleBindingValue("101"));
        idBindings1.addIdentifierBinding("$2", new SimpleBindingValue("102"));

        Bindings idBindings2 = new Bindings();
        idBindings2.addIdentifierBinding("$2", new SimpleBindingValue("202"));
        idBindings2.addIdentifierBinding("$1", new NullBindingValue(Integer.class));
        idBindings2.addIdentifierBinding("$0", new SimpleBindingValue("200"));

        QueryInfo queryWithIndexBindings = new QueryInfo("SELECT WITH-INDEX");
        QueryInfo queryWithIdBindings = new QueryInfo("SELECT WITH-IDENTIFIER");
        QueryInfo queryWithNoBindings = new QueryInfo("SELECT NO-BINDINGS");

        queryWithIndexBindings.getBindingsList().addAll(Arrays.asList(indexBindings1, indexBindings2));
        queryWithIdBindings.getBindingsList().addAll(Arrays.asList(idBindings1, idBindings2));

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId("conn-id");

        // Statement Query
        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);
        execInfo.setConnectionInfo(connectionInfo);
        execInfo.setSuccess(true);
        execInfo.setExecuteDuration(Duration.of(35, ChronoUnit.MILLIS));
        execInfo.setType(ExecutionType.STATEMENT);
        execInfo.setBatchSize(20);
        execInfo.setBindingsSize(10);


        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
        String result;

        // with index bindings
        execInfo.setQueries(Collections.singletonList(queryWithIndexBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id" +
                " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-INDEX\"]" +
                " Bindings:[(100,101,102),(200,null(String),202)]", result);

        // with identifier bindings
        execInfo.setQueries(Collections.singletonList(queryWithIdBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id" +
                " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT WITH-IDENTIFIER\"]" +
                " Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Integer),$2=202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(queryWithNoBindings));
        result = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99) Connection:conn-id" +
                " Transaction:{Create:0 Rollback:0 Commit:0} Success:True Time:35" +
                " Type:Statement BatchSize:20 BindingsSize:10 Query:[\"SELECT NO-BINDINGS\"]" +
                " Bindings:[]", result);

    }


    @Test
    void showThread() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showThread();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setThreadName("my-thread");
        execInfo.setThreadId(99);

        String str = formatter.format(execInfo);
        assertEquals("Thread:my-thread(99)", str);
    }

    @Test
    void showConnection() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showConnection();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setConnectionId("99");

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setConnectionInfo(connectionInfo);

        String str = formatter.format(execInfo);
        assertEquals("Connection:99", str);
    }

    @Test
    void showTransactionInfo() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTransaction();

        // 1 transaction, 2 rollback, 3 commit
        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.incrementTransactionCount();
        connectionInfo.incrementRollbackCount();
        connectionInfo.incrementRollbackCount();
        connectionInfo.incrementCommitCount();
        connectionInfo.incrementCommitCount();
        connectionInfo.incrementCommitCount();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setConnectionInfo(connectionInfo);

        String str = formatter.format(execInfo);
        assertEquals("Transaction:{Create:1 Rollback:2 Commit:3}", str);
    }

    @Test
    void showSuccess() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showSuccess();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setSuccess(true);

        String str = formatter.format(execInfo);
        assertEquals("Success:True", str);

        execInfo.setSuccess(false);

        str = formatter.format(execInfo);
        assertEquals("Success:False", str);
    }

    @Test
    void showTime() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTime();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setExecuteDuration(Duration.of(55, ChronoUnit.MILLIS));

        String str = formatter.format(execInfo);
        assertEquals("Time:55", str);
    }

    @Test
    void showType() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showType();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String str;

        execInfo.setType(ExecutionType.BATCH);
        str = formatter.format(execInfo);
        assertEquals("Type:Batch", str);

        execInfo.setType(ExecutionType.STATEMENT);
        str = formatter.format(execInfo);
        assertEquals("Type:Statement", str);
    }

    @Test
    void showBatchSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBatchSize();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setBatchSize(99);

        String str = formatter.format(execInfo);
        assertEquals("BatchSize:99", str);
    }

    @Test
    void showBindingsSize() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindingsSize();

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        execInfo.setBindingsSize(99);

        String str = formatter.format(execInfo);
        assertEquals("BindingsSize:99", str);
    }

    @Test
    void showQuery() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showQuery();

        QueryInfo query1 = new QueryInfo("QUERY-1");
        QueryInfo query2 = new QueryInfo("QUERY-2");
        QueryInfo query3 = new QueryInfo("QUERY-3");

        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Arrays.asList(query1, query2, query3));
        result = formatter.format(execInfo);
        assertEquals("Query:[\"QUERY-1\",\"QUERY-2\",\"QUERY-3\"]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Query:[\"QUERY-2\"]", result);

        // with no bindings
        execInfo.setQueries(Collections.emptyList());
        result = formatter.format(execInfo);
        assertEquals("Query:[]", result);

    }

    @Test
    void showBindingsWithIndexBinding() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings1 = new Bindings();
        bindings1.addIndexBinding(0, new SimpleBindingValue("100"));
        bindings1.addIndexBinding(1, new SimpleBindingValue("101"));
        bindings1.addIndexBinding(2, new SimpleBindingValue("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIndexBinding(2, new SimpleBindingValue("202"));
        bindings2.addIndexBinding(1, new NullBindingValue(String.class));
        bindings2.addIndexBinding(0, new SimpleBindingValue("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIndexBinding(1, new SimpleBindingValue("300"));
        bindings3.addIndexBinding(2, new SimpleBindingValue("302"));
        bindings3.addIndexBinding(0, new NullBindingValue(Integer.class));

        QueryInfo query1 = new QueryInfo();  // will have 3 bindings
        QueryInfo query2 = new QueryInfo();  // will have 1 bindings
        QueryInfo query3 = new QueryInfo();  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[(100,101,102),(200,null(String),202),(null(Integer),300,302)]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[(200,null(String),202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[]", result);

    }

    @Test
    void showBindingsWithIdentifierBinding() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings();

        Bindings bindings1 = new Bindings();
        bindings1.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        bindings1.addIdentifierBinding("$1", new SimpleBindingValue("101"));
        bindings1.addIdentifierBinding("$2", new SimpleBindingValue("102"));

        Bindings bindings2 = new Bindings();
        bindings2.addIdentifierBinding("$2", new SimpleBindingValue("202"));
        bindings2.addIdentifierBinding("$1", new NullBindingValue(Long.class));
        bindings2.addIdentifierBinding("$0", new SimpleBindingValue("200"));

        Bindings bindings3 = new Bindings();
        bindings3.addIdentifierBinding("$1", new SimpleBindingValue("300"));
        bindings3.addIdentifierBinding("$2", new SimpleBindingValue("302"));
        bindings3.addIdentifierBinding("$0", new NullBindingValue(String.class));

        QueryInfo query1 = new QueryInfo();  // will have 3 bindings
        QueryInfo query2 = new QueryInfo();  // will have 1 bindings
        QueryInfo query3 = new QueryInfo();  // will have empty bindings

        query1.getBindingsList().addAll(Arrays.asList(bindings1, bindings2, bindings3));
        query2.getBindingsList().addAll(Arrays.asList(bindings2));


        QueryExecutionInfo execInfo = new QueryExecutionInfo();
        String result;


        // with multiple bindings
        execInfo.setQueries(Collections.singletonList(query1));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[($0=100,$1=101,$2=102),($0=200,$1=null(Long),$2=202),($0=null(String),$1=300,$2=302)]", result);

        // with single bindings
        execInfo.setQueries(Collections.singletonList(query2));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[($0=200,$1=null(Long),$2=202)]", result);

        // with no bindings
        execInfo.setQueries(Collections.singletonList(query3));
        result = formatter.format(execInfo);
        assertEquals("Bindings:[]", result);

    }

    @Test
    void showAll() {
        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(connectionInfo);

        String result = formatter.format(queryExecutionInfo);
        assertThat(result)
                .containsSubsequence("Thread", "Connection", "Transaction", "Success", "Time", "Type",
                        "BatchSize", "BindingsSize", "Query", "Bindings");
    }

    @Test
    void defaultInstance() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("", result, "Does not generate anything.");
    }


    @Test
    void showThreadWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showThread(((executionInfo, sb) -> sb.append("my-thread")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-thread", result);
    }


    @Test
    void showConnectionWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showConnection(((executionInfo, sb) -> sb.append("my-connection")));

        ConnectionInfo connectionInfo = new ConnectionInfo();
        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(connectionInfo);

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-connection", result);
    }

    @Test
    void showTransactionWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTransaction(((executionInfo, sb) -> sb.append("my-transaction")));

        ConnectionInfo connectionInfo = new ConnectionInfo();
        QueryExecutionInfo queryExecutionInfo = new QueryExecutionInfo();
        queryExecutionInfo.setConnectionInfo(connectionInfo);

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-transaction", result);
    }

    @Test
    void showSuccessWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showSuccess(((executionInfo, sb) -> sb.append("my-success")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-success", result);
    }


    @Test
    void showTimeWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showTime(((executionInfo, sb) -> sb.append("my-time")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-time", result);
    }


    @Test
    void showTypeWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showType(((executionInfo, sb) -> sb.append("my-type")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-type", result);
    }


    @Test
    void showBatchSizeWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBatchSize(((executionInfo, sb) -> sb.append("my-batchsize")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-batchsize", result);
    }


    @Test
    void showBindingsSizeWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindingsSize(((executionInfo, sb) -> sb.append("my-bindingsize")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-bindingsize", result);
    }

    @Test
    void showQueryWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showQuery(((executionInfo, sb) -> sb.append("my-query")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-query", result);
    }

    @Test
    void showBindingsWithConsumer() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.showBindings(((executionInfo, sb) -> sb.append("my-binding")));
        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("my-binding", result);
    }

    @Test
    void delimiter() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
                .showTime()
                .showSuccess()
                .delimiter("ZZZ");

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals("Time:0ZZZSuccess:False", result);
    }

    @Test
    void newLine() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter()
                .showTime()
                .newLine()
                .showSuccess()
                .newLine()
                .showBatchSize();

        String lineSeparator = System.lineSeparator();
        String expected = format("Time:0 %sSuccess:False %sBatchSize:0", lineSeparator, lineSeparator);

        String result = formatter.format(new QueryExecutionInfo());
        assertEquals(expected, result);
    }


    @Test
    void bindingValue() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.bindingValue((bindingValue, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(0, new SimpleBindingValue("100"));
        bindingsByIndex.addIndexBinding(1, new NullBindingValue(Object.class));

        Bindings bindingsByIdentifier = new Bindings();
        bindingsByIdentifier.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        bindingsByIdentifier.addIdentifierBinding("$1", new NullBindingValue(Object.class));

        QueryInfo queryWithIndexBindings = new QueryInfo();
        QueryInfo queryWithIdentifierBindings = new QueryInfo();

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));
        queryWithIdentifierBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIdentifier));

        QueryExecutionInfo execInfoWithIndexBindings = new QueryExecutionInfo();
        QueryExecutionInfo execInfoWithIdentifierBindings = new QueryExecutionInfo();
        execInfoWithIndexBindings.setQueries(Collections.singletonList(queryWithIndexBindings));
        execInfoWithIdentifierBindings.setQueries(Collections.singletonList(queryWithIdentifierBindings));

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertEquals("Bindings:[(FOO,FOO)]", result);

        result = formatter.format(execInfoWithIdentifierBindings);
        assertEquals("Bindings:[($0=FOO,$1=FOO)]", result);
    }

    @Test
    void indexBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.indexBindings((bindings, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIndex = new Bindings();
        bindingsByIndex.addIndexBinding(0, new SimpleBindingValue("100"));
        bindingsByIndex.addIndexBinding(1, new NullBindingValue(Object.class));

        QueryInfo queryWithIndexBindings = new QueryInfo();

        queryWithIndexBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIndex));

        QueryExecutionInfo execInfoWithIndexBindings = new QueryExecutionInfo();
        execInfoWithIndexBindings.setQueries(Collections.singletonList(queryWithIndexBindings));

        String result;

        result = formatter.format(execInfoWithIndexBindings);
        assertEquals("Bindings:[(FOO)]", result);

    }

    @Test
    void identifierBindings() {
        QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
        formatter.identifierBindings((bindingValue, sb) -> {
            sb.append("FOO");
        });
        formatter.showBindings();

        Bindings bindingsByIdentifier = new Bindings();
        bindingsByIdentifier.addIdentifierBinding("$0", new SimpleBindingValue("100"));
        bindingsByIdentifier.addIdentifierBinding("$1", new NullBindingValue(Object.class));

        QueryInfo queryWithIdentifierBindings = new QueryInfo();

        queryWithIdentifierBindings.getBindingsList().addAll(Collections.singletonList(bindingsByIdentifier));

        QueryExecutionInfo execInfoWithIdentifierBindings = new QueryExecutionInfo();
        execInfoWithIdentifierBindings.setQueries(Collections.singletonList(queryWithIdentifierBindings));

        String result;

        result = formatter.format(execInfoWithIdentifierBindings);
        assertEquals("Bindings:[(FOO)]", result);
    }

}
