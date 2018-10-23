package net.ttddyy.dsproxy.r2dbc;

import net.ttddyy.dsproxy.r2dbc.core.CompositeProxyDataSourceListener;
import net.ttddyy.dsproxy.r2dbc.core.ConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.DefaultConnectionIdManager;
import net.ttddyy.dsproxy.r2dbc.core.ProxyDataSourceListener;

/**
 *
 * @author Tadaya Tsuyukubo
 */
public class ProxyConfig {

    private CompositeProxyDataSourceListener listeners = new CompositeProxyDataSourceListener();

    private ConnectionIdManager connectionIdManager = new DefaultConnectionIdManager();

    private ProxyFactory proxyFactory = new JdkProxyFactory();

    {
        this.proxyFactory.setProxyConfig(this);
    }

    //    public ProxyConfig(ProxyFactory proxyFactory) {
    //        this.proxyFactory = proxyFactory;
    //    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }

    public CompositeProxyDataSourceListener getListeners() {
        return this.listeners;
    }

    public void addListener(ProxyDataSourceListener listener) {
        this.listeners.add(listener);
    }

    public ConnectionIdManager getConnectionIdManager() {
        return connectionIdManager;
    }

    public void setConnectionIdManager(ConnectionIdManager connectionIdManager) {
        this.connectionIdManager = connectionIdManager;
    }
}
