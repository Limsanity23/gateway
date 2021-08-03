package com.bnids.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "carNo",
        "purpose",
        "dong",
        "ho",
        "isVisitor",
        "regDate"
})

@Getter @Setter
public class AptnerResult implements Serializable {

    private static final long serialVersionUID = -7625790714189456386L;

    @JsonProperty("carNo")
    private String carNo;
    @JsonProperty("purpose")
    private String purpose;
    @JsonProperty("dong")
    private String dong;
    @JsonProperty("ho")
    private String ho;
    @JsonProperty("isVisitor")
    private String isVisitor;
    @JsonProperty("regDate")
    private String regDate;


}

