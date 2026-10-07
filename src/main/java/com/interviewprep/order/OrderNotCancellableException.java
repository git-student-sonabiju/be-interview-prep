package com.interviewprep.order;

import com.interviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotCancellableException extends ApiException {

    public OrderNotCancellableException(Long orderId) {
        super(HttpStatus.CONFLICT, "Order " + orderId + " is already cancelled");
    }
}
