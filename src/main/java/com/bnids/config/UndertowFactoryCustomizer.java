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
package com.bnids.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * https://docs.spring.io/spring-boot/docs/current/reference/html/howto-embedded-web-servers.html
 * @author connect2sys
 */
@Component
@Slf4j
public class UndertowFactoryCustomizer implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * 4;
        int workerThreads = ioThreads * 8;
        int bufferSize = 512;
        boolean directBuffers = false;
        long maxMemory = Runtime.getRuntime().maxMemory();

        if (maxMemory < 64 * 1024 * 1024) {
            directBuffers = false;
            bufferSize = 512;
        } else if (maxMemory < 128 * 1024 * 1024) {
            directBuffers = true;
            bufferSize = 1024;
        } else {
            directBuffers = true;
            //generally the max amount of data that can be sent in a single write() call
            bufferSize = 1024 * 16;
        }
        factory.setBufferSize(bufferSize);
        factory.setUseDirectBuffers(directBuffers);
        factory.setIoThreads(ioThreads);
        factory.setWorkerThreads(workerThreads);
        factory.setAccessLogEnabled(true);
        int finalBufferSize = bufferSize;
        boolean finalDirectBuffers = directBuffers;
        //log.warn("Undertow Configuration. buffer:{}bytes, directBuffer:{}, ioThreads:{}, workerThreads:{}",
        //        ()-> finalBufferSize,()-> finalDirectBuffers,()->ioThreads,()->workerThreads );
    }
}
