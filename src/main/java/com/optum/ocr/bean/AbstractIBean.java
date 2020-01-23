package com.optum.ocr.bean;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.MappedSuperclass;
import java.time.LocalDate;

@Data
@MappedSuperclass
public abstract class AbstractIBean {
    public abstract Long getId();
    public abstract void setId(Long id);

    public String createdBy;

    @CreationTimestamp
    public LocalDate createdDate;

    public String updatedBy;

    @UpdateTimestamp
    public LocalDate updatedDate;
}
