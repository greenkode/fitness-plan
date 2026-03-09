/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2020 jPOS Software SRL
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.krachtix.util

import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Objects
import java.util.regex.Pattern


class TxnId {
    private var id: Long = 0

    private constructor() : super()

    private constructor(l: Long) {
        this.id = l
    }

    fun id(): Long {
        return id
    }

    private fun init(year: Int, dayOfYear: Int, nanosecondOfDay: Long, node: Int, transactionId: Long): TxnId {
        id = (year * YMUL + dayOfYear * DMUL + nanosecondOfDay * SMUL + (node % 1000) * NMUL + transactionId % 100000)
        return this
    }

    /**
     * Returns a file suitable to store contents of *this* transaction.
     * File format is *yyyy/mm/dd/hh-mm-ss-node-id*
     *
     * @return file in said format
     */
    fun toFile(): File {
        var l = id
        val yy = (id / YMUL).toInt()
        l -= yy * YMUL
        val dd = (l / DMUL).toInt()
        l -= dd * DMUL
        val sod = (l / SMUL).toInt()
        l -= sod * SMUL
        val node = (l / NMUL).toInt()
        l -= node * NMUL
        val hh = sod / 3600
        val mm = (sod - 3600 * hh) / 60
        val ss = sod % 60

        val dt = LocalDateTime.now().withYear(2000 + yy)
            .withDayOfYear(dd)
            .withHour(hh)
            .withMinute(mm)
            .withSecond(ss)

        return File(
            String.format(
                "%04d/%02d/%02d/%02d-%02d-%02d-%03d-%05d",
                dt.year,
                dt.monthValue,
                dt.dayOfMonth,
                dt.hour,
                dt.minute,
                dt.second,
                node,
                l
            )
        )
    }

    override fun toString(): String {
        var l = id
        val yy = (id / YMUL).toInt()
        l -= yy * YMUL

        val dd = (l / DMUL).toInt()
        l -= dd * DMUL

        val ss = (l / SMUL).toInt()
        l -= ss * SMUL

        val node = (l / NMUL).toInt()
        l -= node * NMUL
        return String.format("%03d%03d%05d%03d%05d", yy, dd, ss, node, l)
    }

    fun toRrn(): String {
        return id.toString(36)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val tranLogId = o as TxnId
        return id == tranLogId.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    companion object {
        private const val YMUL = 10000000000000000L
        private const val DMUL = 10000000000000L
        private const val SMUL = 100000000L
        private const val NMUL = 100000L
        private val MAX_VALUE = "zzzzzzzzzzzz".toLong(36)
        private val pattern: Pattern = Pattern.compile("^([\\d]{3})-([\\d]{3})-([\\d]{5})-([\\d]{3})-([\\d]{5})$")

        /**
         * Creates new TxnId object
         *
         * @param dt            Transaction's TIMESTAMP LocalDateTime
         * @param node          node id
         * @param transactionId TransactionManager's ID
         */
        fun create(dt: LocalDateTime, node: Int, transactionId: Long): TxnId {
            var dt = dt
            val id = TxnId()

            dt = dt.atOffset(ZoneOffset.UTC).toLocalDateTime()

            return id.init(dt.year - 2000, dt.dayOfYear, dt.toLocalTime().toNanoOfDay(), node, transactionId)
        }

        /**
         * @param idString TxnId in YYYY-DDD-SSS-NN-TTTTT format
         *
         *
         *  * `CYY` Century Year Year
         *  * `DDD` day of year
         *  * `SSS` second of day
         *  * `NNN` unique node number (000 to 999)
         *  * `TTTTT` last 5 digits of transaction manager's transaction id
         *
         */
        fun parse(idString: String): TxnId {
            val matcher = pattern.matcher(idString)
            require(!(!matcher.matches() && matcher.groupCount() != 5)) { "Invalid idString '$idString'" }
            return TxnId().init(
                matcher.group(1).toInt(),
                matcher.group(2).toInt(),
                matcher.group(3).toInt().toLong(),
                matcher.group(4).toInt(),
                matcher.group(5).toInt().toLong()
            )
        }

        /**
         * Parse TxnId from long
         *
         * @param id value
         * @return newly created TxnId
         */
        fun parse(id: Long): TxnId {
            require(!(id < 0 || id > MAX_VALUE)) { "Invalid id $id" }
            return TxnId(id)
        }

        /**
         * Parse TxnId from rrn
         *
         * @param rrn value
         * @return newly created TxnId
         */
        fun fromRrn(rrn: String): TxnId {
            val id = rrn.toLong(36)
            require(!(id < 0 || id > MAX_VALUE)) { "Invalid rrn $rrn" }
            return TxnId(id)
        }

        fun createRn() : String {
            return create(LocalDateTime.now(), 1, System.currentTimeMillis()).toRrn().replace("-", "").uppercase()
        }
    }
}