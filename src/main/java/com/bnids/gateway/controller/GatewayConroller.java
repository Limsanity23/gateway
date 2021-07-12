/*
 *  BNIndustry Inc., Software License, Version 1.0
 *
 *   Copyright (c) 2019. BNIndustry Inc.,
 *   All rights reserved.
 *
 *    DON'T COPY OR REDISTRIBUTE THIS SOURCE CODE WITHOUT PERMISSION.
 *    THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *    WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *    DISCLAIMED. IN NO EVENT SHALL <<BNIndustry Inc.>> OR ITS
 *    CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *    SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *    USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *    ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *    OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *    SUCH DAMAGE.
 *
 *    For more information on this product, please see www.bnids.com
 */
package com.bnids.gateway.controller;

import com.bnids.core.api.response.ApiResponse;
import com.bnids.gateway.dto.CustomRestrictRequest;
import com.bnids.gateway.dto.InterlockRequestDto;
import com.bnids.gateway.dto.LprRequestDto;
import com.bnids.gateway.service.GatewayService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yannishin
 */
@RestController
@RequestMapping(value = "/api/gateway")
@RequiredArgsConstructor
public class GatewayConroller {
    @NonNull
    private final GatewayService gatewayService;

    @PostMapping("/interlock")
    public ApiResponse<String> interlock(@RequestBody LprRequestDto dto) {
        gatewayService.interlock(dto);
        return ApiResponse.createOK("처리완료");
    }

    @PostMapping("/customrestrict")
    public ApiResponse<String> customRestrict(@RequestBody CustomRestrictRequest dto) {
        InterlockRequestDto requestDto = InterlockRequestDto.builder()
        .carNo(dto.getCarNo())
        .gateId(dto.getGateId())
        .gateName(dto.getGateName())
        .addressDong(dto.getAddressDong())
        .addressHo(dto.getAddressHo())
        .carSection(4L)
        .siteCode(dto.getSiteCode()).build();
        String restrictedMessage = gatewayService.isCustomRestricted(requestDto);
        return ApiResponse.createOK(restrictedMessage);
    }
}
