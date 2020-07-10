package com.bnids.gateway.dto;

import com.bnids.gateway.entity.WarningCarRegistEnum;
import com.bnids.gateway.entity.WarningCar;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WarningCarDto {
    @Setter
    @Getter
    @Builder
    public static class Create {
        private String carNo;

        private String carKind;

        private String registReason;

        private String warningCarRegistContent;

        private Long carSection;

        private String digitCarNo;

        private Integer lastVisitCarId;

        private WarningCarRegistEnum WarningCarRegistEnum;

        private String register;

        private String registMethodKind;

        public WarningCar toEntity() {
            return WarningCar.builder()
                        .carNo(this.carNo)
                        .carKind(this.carKind)
                        .registReason(this.registReason)
                        .warningCarRegistContent(this.warningCarRegistContent)
                        .carSection(this.carSection)
                        .lastVisitCarId(this.lastVisitCarId)
                        .registMethodKind(this.registMethodKind)
                        .register(this.register)
                        .digitCarNo(this.digitCarNo)
                    .build();
        }
    }

    @Builder
    @Setter
    @Getter
    public static class ResponseOk {
        private Long warningCarId;
        public ResponseOk(Long warningCarId) {
            this.warningCarId = warningCarId;
        }
        public static ResponseOk of(Long warningCarId) {
            return new ResponseOk(warningCarId);
        }
    }
}
