package br.com.sistema.alimentos.repository;

import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlimentoRepository extends JpaRepository<Alimento, Integer> {

    Page<Alimento> findByGrupo(GrupoAlimentar grupo, Pageable pageable);

    @Query("SELECT a FROM Alimento a WHERE LOWER(a.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))")
    Page<Alimento> buscarPorDescricao(@Param("descricao") String descricao, Pageable pageable);

    boolean existsByCodigoSubstituicao(String codigoSubstituicao);
}
