package halo.query.dal;

import halo.query.HaloQueryDALDebugInfo;
import halo.query.HaloQueryMSLDBDebugInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;

/**
 * 支持分布式数据源访问的数据源。数据源中包含了需要访问的所有真实数据源.<br>
 * 目前不支持单数据源访问<br>
 * 配置的第一个数据源是默认数据源
 *
 * @author akwei
 */
public class HaloDALDataSource implements DataSource, InitializingBean {

    public static final String DEFAULT_DS_NAME = "default_ds";

    private Map<String, DataSource> dataSourceMap;

    protected final Map<String, List<String>> masterSlaveDsKeyMap = new
            HashMap<String, List<String>>();

    private String defaultDsKey;

    private PrintWriter logWriter;

    private int loginTimeout = 0;

    private final Log logger = LogFactory.getLog(HaloDALDataSource.class);


    /**
     * 获得当可用的数据源，如果没有指定，获得默认的数据源
     *
     * @return
     */
    private DataSource getCurrentDataSource() {
        String master = DALStatus.getDsKey();
        String slave = null;
        if (DALStatus.isEnableSlave()) {
            slave = DALStatus.getSlaveDsKey();
            if (slave == null) {
                slave = this.getRandomSlaveDsKey(master);
                if (slave != null) {
                    DALStatus.setSlaveDsKey(slave);
                }
            }
        }
        String name;
        if (slave == null) {
            name = master;
        } else {
            name = slave;
        }
        DataSource ds = this.dataSourceMap.get(name);
        if (ds == null) {
            throw new DALRunTimeException("no datasource forKey [" + name + "]");
        }
        if (HaloQueryDALDebugInfo.getInstance().isEnableDebug()) {
            logger.info("get real datasource from dsKey [" + name + "]");
        }
        return ds;
    }

    public String getRandomSlaveDsKey(String masterDsKey) {
        List<String> slaveDsKeys = this.masterSlaveDsKeyMap.get(masterDsKey);
        if (slaveDsKeys == null || slaveDsKeys.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(slaveDsKeys.size());
        if (HaloQueryMSLDBDebugInfo.getInstance().isEnableDebug()) {
            logger.info("will return slave datasource " + index);
        }
        return slaveDsKeys.get(index);
    }


    /**
     * 设置默认的数据源key
     *
     * @param defaultDsKey 默认数据源key
     */
    public void setDefaultDsKey(String defaultDsKey) {
        this.defaultDsKey = defaultDsKey;
    }

    /**
     * 设定数据源key与真实数据源的对应关系.<br>
     * map中的key为数据源key,value为真实数据源
     *
     * @param dataSourceMap 数据源的map
     */
    public void setDataSourceMap(Map<String, DataSource> dataSourceMap) {
        this.dataSourceMap = dataSourceMap;
    }

    public Connection getConnection() throws SQLException {
        return new DALConnection(this);
    }

    /**
     * 获得当前可用的连接
     *
     * @return
     * @throws SQLException
     */
    public Connection getCurrentConnection() throws SQLException {
        return this.getCurrentDataSource().getConnection();
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        throw new SQLException("only support getConnection()");
    }

    public PrintWriter getLogWriter() throws SQLException {
        return this.logWriter;
    }

    public int getLoginTimeout() throws SQLException {
        return this.loginTimeout;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.getCurrentDataSource().unwrap(iface);
    }

    public void destory() {
        Set<Map.Entry<String, DataSource>> set = this.dataSourceMap.entrySet();
        for (Map.Entry<String, DataSource> e : set) {
            C3p0DataSourceUtil.destory(e.getValue());
        }
    }

    public void afterPropertiesSet() throws Exception {
        if (this.defaultDsKey != null) {
            DataSource ds = this.dataSourceMap.get(this.defaultDsKey);
            if (ds == null) {
                throw new RuntimeException("default ds must be not empty");
            } else {
                this.dataSourceMap.put(DEFAULT_DS_NAME, ds);
            }
        }
    }

}