package com.mkr.commerce.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "lookup_value",
       indexes = @Index(name = "idx_lookup_value_list_id", columnList = "lookup_list_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LookupValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lookup_list_id", nullable = false)
    private LookupList lookupList;

    @Column(nullable = false, length = 200)
    private String value;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
