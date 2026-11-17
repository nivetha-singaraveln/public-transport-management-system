package com.vcet.transport.model;

import java.math.BigDecimal;

public record CancellationResult(int bookingId, String ticketCode, BigDecimal farePaid,
                                 BigDecimal refund, int refundPercent) { }
