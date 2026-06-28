package com.mkr.commerce.vendor.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_vendor_name", columnList = "name")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Vendor extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 20)
    private String gstin;

    @Column(length = 500)
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
