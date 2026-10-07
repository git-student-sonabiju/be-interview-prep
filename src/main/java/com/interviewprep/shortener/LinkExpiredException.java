package com.interviewprep.shortener;

import com.interviewprep.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class LinkExpiredException extends ApiException {

    public LinkExpiredException(String code) {
        super(HttpStatus.GONE, "Short link " + code + " has expired");
    }
}
