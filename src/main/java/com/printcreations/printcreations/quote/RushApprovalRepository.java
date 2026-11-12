package com.printcreations.printcreations.quote;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RushApprovalRepository {

    private final JdbcClient jdbcClient;

    public RushApprovalRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<RushApprovalState> find(UUID referenceNumber) {
        return jdbcClient.sql("""
                        SELECT reference_number, rush_requested, rush_approved, rush_fee_amount, version
                        FROM quote_requests
                        WHERE reference_number = :referenceNumber
                        """)
                .param("referenceNumber", referenceNumber)
                .query(RushApprovalRepository::mapState)
                .optional();
    }

    public Optional<RushApprovalState> updateIfVersionMatches(
            UUID referenceNumber, long expectedVersion, boolean approved, BigDecimal fee
    ) {
        return jdbcClient.sql("""
                        UPDATE quote_requests
                        SET rush_approved = :approved, rush_fee_amount = :fee, version = version + 1
                        WHERE reference_number = :referenceNumber AND version = :expectedVersion
                        RETURNING reference_number, rush_requested, rush_approved, rush_fee_amount, version
                        """)
                .param("referenceNumber", referenceNumber)
                .param("expectedVersion", expectedVersion)
                .param("approved", approved)
                .param("fee", fee, Types.NUMERIC)
                .query(RushApprovalRepository::mapState)
                .optional();
    }

    public void insertAudit(String actor, RushApprovalState before, RushApprovalState after) {
        int rows = jdbcClient.sql("""
                        INSERT INTO quote_rush_approval_audit (
                            reference_number, actor, old_version, new_version,
                            old_rush_approved, new_rush_approved,
                            old_rush_fee_amount, new_rush_fee_amount
                        ) VALUES (
                            :referenceNumber, :actor, :oldVersion, :newVersion,
                            :oldApproved, :newApproved, :oldFee, :newFee
                        )
                        """)
                .param("referenceNumber", before.referenceNumber())
                .param("actor", actor)
                .param("oldVersion", before.version())
                .param("newVersion", after.version())
                .param("oldApproved", before.rushApproved())
                .param("newApproved", after.rushApproved())
                .param("oldFee", before.rushFeeAmount(), Types.NUMERIC)
                .param("newFee", after.rushFeeAmount(), Types.NUMERIC)
                .update();
        if (rows != 1) {
            throw new IllegalStateException("Expected one rush approval audit row.");
        }
    }

    private static RushApprovalState mapState(ResultSet row, int rowNumber) throws SQLException {
        return new RushApprovalState(
                row.getObject("reference_number", UUID.class),
                row.getBoolean("rush_requested"),
                row.getBoolean("rush_approved"),
                row.getBigDecimal("rush_fee_amount"),
                row.getLong("version")
        );
    }
}
