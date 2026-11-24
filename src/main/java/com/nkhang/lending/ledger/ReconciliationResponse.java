package com.nkhang.lending.ledger;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationResponse(UUID userId, BigDecimal usedLimit,
                                     BigDecimal drawdownOutstanding, BigDecimal ledgerOutstanding,
                                     long unbalancedJournals, boolean balanced) { }
