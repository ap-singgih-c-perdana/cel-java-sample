package org.example.celjavasample.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rules")
public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Column(columnDefinition = "TEXT")
    private String celExpression;

    private String rewardType;
    private Double multiplier;
    private Double maxCap;
    private Double fixedAmount;
    private Boolean active = true;
}
