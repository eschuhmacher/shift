package gov.bnl.shift;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Response;
import javax.xml.ws.WebServiceException;

/**
 * Database connection handling: connections and transactions.
 *
 */
public class DbConnection {

    private static ThreadLocal<DbConnection> instance = new ThreadLocal<DbConnection>() {

        @Override
        protected DbConnection initialValue() {
            return new DbConnection();
        }
    };
    private static final String dbResourceName = "jdbc/shift";
    private Connection con;
    private DataSource ds;

    public DbConnection() {
        try {
            InitialContext ic = new InitialContext();
            ds = (DataSource) ic.lookup("java:comp/env/" + dbResourceName);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot find JDBC DataSource '"
                    + dbResourceName + "'", e);
        }
    }

    /**
     * Returns the DbConnection instance.
     *
     * @return DbConnection instance
     */
    public static DbConnection getInstance() {
        return instance.get();
    }

    /**
     * Returns the DataSource Connection, requesting a new one (thread local) if needed.
     *
     * @return Connection to the JDBC DataSource
     * @throws ShiftFinderException wrapping an SQLException
     */
    public Connection getConnection() throws ShiftFinderException {
        if (con == null) {
            try {
                con = ds.getConnection();
            } catch (SQLException e) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR, "Could not get db connection", e);
            }
        }
        return con;
    }

    /**
     * Release the current (thread local) DataSource Connection.
     */
    public void releaseConnection() {
        if (con != null) {
            try {
                con.rollback();
                con.close();
                con = null;
            } catch (Exception e) {
                throw new WebServiceException("Could not release db connection", e);
            }
        }
    }

    /**
     * Begins a database transaction.
     * @throws ShiftFinderException wrapping an SQLException
     */
    public void beginTransaction() throws ShiftFinderException {
        getConnection();
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            con.setAutoCommit(false);
        } catch (Exception e) {
            throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR, "Could not begin db transaction", e);
        }
    }

    /**
     * Ends a transaction by committing.
     * @throws ShiftFinderException wrapping an SQLException
     */
    public void commit() throws ShiftFinderException {
        if (con != null) {
            try {
                con.commit();
            } catch (Exception e) {
                throw new ShiftFinderException(Response.Status.INTERNAL_SERVER_ERROR, "Could not commit db transaction", e);
            }
        }
    }

    /**
     * Ends a transaction by rolling back.
     */
    public void rollback() {
        if (con != null) {
            try {
                con.rollback();
            } catch (Exception e) {
                throw new WebServiceException("Could not rollback db transaction", e);
            }
        }
    }
}

