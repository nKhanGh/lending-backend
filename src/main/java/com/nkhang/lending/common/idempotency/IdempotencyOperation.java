package com.nkhang.lending.common.idempotency;

public enum IdempotencyOperation {
    CREATE_CHECKOUT,
    CONFIRM_CHECKOUT,
    PAY_INSTALMENT,
    SETTLE_ORDER
}

