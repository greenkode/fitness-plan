package com.krachtix.config

import com.krachtix.commons.tenant.TenantContext
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

class TenantAwareDataSource(
    private val delegate: DataSource
) : DataSource {

    override fun getConnection(): Connection {
        val connection = delegate.connection
        setTenantId(connection)
        return connection
    }

    override fun getConnection(username: String?, password: String?): Connection {
        val connection = delegate.getConnection(username, password)
        setTenantId(connection)
        return connection
    }

    private fun setTenantId(connection: Connection) {
        val merchantId = TenantContext.getMerchantIdOrNull() ?: return
        connection.createStatement().use { stmt ->
            stmt.execute("SET app.tenant_id = '${merchantId}'")
        }
    }

    override fun getLogWriter(): PrintWriter? = delegate.logWriter
    override fun setLogWriter(out: PrintWriter?) { delegate.logWriter = out }
    override fun setLoginTimeout(seconds: Int) { delegate.loginTimeout = seconds }
    override fun getLoginTimeout(): Int = delegate.loginTimeout
    override fun getParentLogger(): Logger = delegate.parentLogger
    override fun <T : Any?> unwrap(iface: Class<T>): T = delegate.unwrap(iface)
    override fun isWrapperFor(iface: Class<*>): Boolean = delegate.isWrapperFor(iface)
}
