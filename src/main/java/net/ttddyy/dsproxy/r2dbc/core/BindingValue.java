package net.ttddyy.dsproxy.r2dbc.core;

/**
 * Represent a value for {@link io.r2dbc.spi.Statement#bind} and {@link io.r2dbc.spi.Statement#bindNull} operations.
 *
 * @author Tadaya Tsuyukubo
 */
public interface BindingValue {

    Object getValue();

    class NullBindingValue implements BindingValue {

        private Class<?> type;  // type of null

        public NullBindingValue(Class<?> type) {
            this.type = type;
        }

        @Override
        public Object getValue() {
            return null;  // value is always null
        }

        public Class<?> getType() {
            return type;
        }
    }

    class SimpleBindingValue implements BindingValue {

        private Object value;

        public SimpleBindingValue(Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

    }

}
