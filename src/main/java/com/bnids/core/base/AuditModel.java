package com.bnids.core.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author yannishin
 */
@Getter @Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(
        value = {"id"},
        allowGetters = true
)
public abstract class AuditModel<PK extends Serializable> extends BaseEntity<PK> {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDt;

    @LastModifiedDate
    @Column(updatable = false)
    private LocalDateTime updatedDt;
}
