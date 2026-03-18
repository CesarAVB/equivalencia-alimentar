package br.com.sistema.alimentos.repository;

import br.com.sistema.alimentos.entity.Equivalencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquivalenciaRepository extends JpaRepository<Equivalencia, UUID> {

    Page<Equivalencia> findAll(Pageable pageable);

    List<Equivalencia> findByAlimentoOrigemId(Integer alimentoOrigemId);

    List<Equivalencia> findByAlimentoDestinoId(Integer alimentoDestinoId);
}
