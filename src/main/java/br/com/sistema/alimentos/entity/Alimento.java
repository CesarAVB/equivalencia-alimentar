package br.com.sistema.alimentos.entity;

import br.com.sistema.alimentos.enums.GrupoAlimentar;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alimentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo_substituicao", nullable = false, unique = true, length = 20)
    private String codigoSubstituicao;

    @Column(name = "grupo", nullable = false)
    private GrupoAlimentar grupo;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "energia_kcal", nullable = false, precision = 10, scale = 2)
    private BigDecimal energiaKcal;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
