package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lookup_list")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LookupList extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String fieldType = "SELECT";

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(length = 200)
    private String defaultValue;

    @OneToMany(mappedBy = "lookupList", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<LookupValue> values = new ArrayList<>();
}
