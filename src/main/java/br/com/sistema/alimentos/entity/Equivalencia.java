package br.com.sistema.alimentos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "equivalencias")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Equivalencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_origem_id", nullable = false)
    private Alimento alimentoOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alimento_destino_id", nullable = false)
    private Alimento alimentoDestino;

    @Column(name = "fator_equivalencia", nullable = false, precision = 10, scale = 4)
    private BigDecimal fatorEquivalencia;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
