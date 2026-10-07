package com.interviewprep.order;

import com.interviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {

    public IdempotencyKeyReusedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key was already used for a different order request");
    }
}
