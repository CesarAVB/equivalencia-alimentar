package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.request.CriarEquivalenciaRequest;
import br.com.sistema.alimentos.dtos.response.EquivalenciaResponse;
import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.entity.Equivalencia;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.repository.AlimentoRepository;
import br.com.sistema.alimentos.repository.EquivalenciaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquivalenciaServiceTest {

    @Mock
    private EquivalenciaRepository equivalenciaRepository;

    @Mock
    private AlimentoRepository alimentoRepository;

    @InjectMocks
    private EquivalenciaService equivalenciaService;

    @Test
    @DisplayName("Deve listar equivalências paginadas")
    void deveListarEquivalenciasPaginadas() {
        Equivalencia equivalencia = equivalencia("Banana", "Maçã");
        when(equivalenciaRepository.findAll(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(equivalencia)));

        Page<EquivalenciaResponse> resultado = equivalenciaService.listar(PageRequest.of(0, 20));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Banana", resultado.getContent().getFirst().alimentoOrigemDescricao());
    }

    @Test
    @DisplayName("Deve buscar equivalências por alimento de origem")
    void deveBuscarEquivalenciasPorAlimentoOrigem() {
        when(equivalenciaRepository.findByAlimentoOrigemId(1)).thenReturn(List.of(equivalencia("Banana", "Mamão")));

        List<EquivalenciaResponse> resultado = equivalenciaService.buscarPorAlimentoOrigem(1, new BigDecimal("100"));

        assertEquals(1, resultado.size());
        assertEquals("Mamão", resultado.getFirst().alimentoDestinoDescricao());
    }

    @Test
    @DisplayName("Deve buscar equivalência por id quando existir")
    void deveBuscarEquivalenciaPorIdQuandoExistir() {
        UUID id = UUID.randomUUID();
        Equivalencia equivalencia = equivalencia("Banana", "Pera");
        equivalencia.setId(id);
        when(equivalenciaRepository.findById(id)).thenReturn(Optional.of(equivalencia));

        EquivalenciaResponse response = equivalenciaService.buscarPorId(id);

        assertEquals(id, response.id());
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar equivalência inexistente")
    void deveLancarExcecaoAoBuscarEquivalenciaInexistente() {
        UUID id = UUID.randomUUID();
        when(equivalenciaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> equivalenciaService.buscarPorId(id));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar quando alimento origem não existir")
    void deveLancarExcecaoAoCriarQuandoAlimentoOrigemNaoExistir() {
        CriarEquivalenciaRequest request = new CriarEquivalenciaRequest(1, 2, new BigDecimal("1.2"), "obs");
        when(alimentoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> equivalenciaService.criar(request));
        verify(equivalenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar quando alimento destino não existir")
    void deveLancarExcecaoAoCriarQuandoAlimentoDestinoNaoExistir() {
        CriarEquivalenciaRequest request = new CriarEquivalenciaRequest(1, 2, new BigDecimal("1.2"), "obs");
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento(1, "Banana")));
        when(alimentoRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> equivalenciaService.criar(request));
        verify(equivalenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar equivalência quando dados forem válidos")
    void deveCriarEquivalenciaQuandoDadosForemValidos() {
        CriarEquivalenciaRequest request = new CriarEquivalenciaRequest(1, 2, new BigDecimal("1.2"), "obs");
        Alimento origem = alimento(1, "Banana");
        Alimento destino = alimento(2, "Maçã");
        Equivalencia salvo = equivalencia("Banana", "Maçã");

        when(alimentoRepository.findById(1)).thenReturn(Optional.of(origem));
        when(alimentoRepository.findById(2)).thenReturn(Optional.of(destino));
        when(equivalenciaRepository.save(any(Equivalencia.class))).thenReturn(salvo);

        EquivalenciaResponse response = equivalenciaService.criar(request);

        assertEquals("Banana", response.alimentoOrigemDescricao());
        assertEquals("Maçã", response.alimentoDestinoDescricao());
    }

    @Test
    @DisplayName("Deve atualizar equivalência quando id existir")
    void deveAtualizarEquivalenciaQuandoIdExistir() {
        UUID id = UUID.randomUUID();
        AtualizarEquivalenciaRequest request = new AtualizarEquivalenciaRequest(1, 2, new BigDecimal("2.5"), "nova obs");
        Equivalencia existente = equivalencia("Banana", "Pera");
        existente.setId(id);

        when(equivalenciaRepository.findById(id)).thenReturn(Optional.of(existente));
        when(alimentoRepository.findById(1)).thenReturn(Optional.of(alimento(1, "Banana")));
        when(alimentoRepository.findById(2)).thenReturn(Optional.of(alimento(2, "Maçã")));
        when(equivalenciaRepository.save(eq(existente))).thenReturn(existente);

        EquivalenciaResponse response = equivalenciaService.atualizar(id, request);

        assertEquals(new BigDecimal("2.5"), response.fatorEquivalencia());
        assertEquals("nova obs", response.observacao());
    }

    @Test
    @DisplayName("Deve remover equivalência quando id existir")
    void deveRemoverEquivalenciaQuandoIdExistir() {
        UUID id = UUID.randomUUID();
        when(equivalenciaRepository.existsById(id)).thenReturn(true);

        equivalenciaService.remover(id);

        verify(equivalenciaRepository).deleteById(id);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover equivalência inexistente")
    void deveLancarExcecaoAoRemoverEquivalenciaInexistente() {
        UUID id = UUID.randomUUID();
        when(equivalenciaRepository.existsById(id)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> equivalenciaService.remover(id));
    }

    private static Alimento alimento(Integer id, String descricao) {
        return Alimento.builder()
                .id(id)
                .codigoSubstituicao("A" + id)
                .grupo(GrupoAlimentar.FRUTAS)
                .descricao(descricao)
                .energiaKcal(new BigDecimal("100"))
                .build();
    }

    private static Equivalencia equivalencia(String origemDescricao, String destinoDescricao) {
        return Equivalencia.builder()
                .id(UUID.randomUUID())
                .alimentoOrigem(alimento(1, origemDescricao))
                .alimentoDestino(alimento(2, destinoDescricao))
                .fatorEquivalencia(new BigDecimal("1.2"))
                .observacao("obs")
                .build();
    }
}
