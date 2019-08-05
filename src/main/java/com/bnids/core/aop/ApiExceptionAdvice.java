/*
 * BNIndustry Inc., Software License, Version 1.0
 *
 * Copyright (c) 2018 BNIndustry Inc.,
 * All rights reserved.
 *
 *  DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *
 *  For more information on this product, please see www.bnids.com
 */
package com.bnids.core.aop;

import com.bnids.core.api.response.ApiException;
import com.bnids.core.api.response.ApiResponse;
import com.bnids.core.api.response.ApiResponseCode;
import com.bnids.exception.CommunicationFailureException;
import com.bnids.exception.ConnectorClassException;
import com.bnids.exception.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.net.UnknownHostException;

/**
 * @author shinys
 */
@ControllerAdvice
public class ApiExceptionAdvice {
    Logger logger = LogManager.getLogger(ApiExceptionAdvice.class);

    @Order(1)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value={NullPointerException.class, ConnectorClassException.class, Exception.class})
    @ResponseBody
    public ApiResponse<String> handleInternalException(RuntimeException e, WebRequest request) {
        ApiResponse<String> exception = ApiResponse.createException(new ApiException(ApiResponseCode.SERVER_ERROR, e));
        logger.error(e::getMessage, e);
        return exception;
    }

    @Order(2)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler(value={CommunicationFailureException.class, UnknownHostException.class})
    @ResponseBody
    public ApiResponse<String> handleCommunicationException(RuntimeException e, WebRequest request) {
        ApiResponse<String> exception = ApiResponse.createException(new ApiException(ApiResponseCode.COMMUNICATION_FAILURE, e));
        logger.error(e::getMessage, e);
        return exception;
    }

    @Order(3)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value={NotFoundException.class})
    @ResponseBody
    public ApiResponse<String> handleValidException(RuntimeException e, WebRequest request) {
        ApiResponse<String> exception = ApiResponse.createException(new ApiException(ApiResponseCode.NOT_FOUND, e));
        logger.warn(e::getMessage, e);
        return exception;
    }

}
