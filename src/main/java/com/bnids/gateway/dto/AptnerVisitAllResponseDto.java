package com.bnids.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "result"
})
public class AptnerVisitAllResponseDto implements Serializable {
    private static final long serialVersionUID = 8171333549019161663L;
    @JsonProperty("result")
    private List<AptnerReserve> result;


    @JsonProperty("result")
    public List<AptnerReserve> getResult() {
        return result;
    }

}
