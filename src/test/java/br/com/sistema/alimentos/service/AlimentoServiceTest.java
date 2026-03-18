package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.AtualizarAlimentoRequest;
import br.com.sistema.alimentos.dtos.request.CriarAlimentoRequest;
import br.com.sistema.alimentos.dtos.response.AlimentoResponse;
import br.com.sistema.alimentos.entity.Alimento;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.repository.AlimentoRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlimentoServiceTest {

    @Mock
    private AlimentoRepository alimentoRepository;

    @InjectMocks
    private AlimentoService alimentoService;

    @Test
    @DisplayName("Deve listar por descrição quando filtro de descrição for informado")
    void deveListarPorDescricaoQuandoFiltroDescricaoInformado() {
        PageableFixture fixture = new PageableFixture();
        Alimento alimento = alimento(1, "Banana", GrupoAlimentar.FRUTAS, "A001");
        when(alimentoRepository.buscarPorDescricao("ban", fixture.pageable)).thenReturn(new PageImpl<>(List.of(alimento)));

        Page<AlimentoResponse> resultado = alimentoService.listar("ban", null, fixture.pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Banana", resultado.getContent().getFirst().descricao());
        verify(alimentoRepository).buscarPorDescricao("ban", fixture.pageable);
    }

    @Test
    @DisplayName("Deve listar por grupo quando descrição não for informada")
    void deveListarPorGrupoQuandoDescricaoNaoInformada() {
        PageableFixture fixture = new PageableFixture();
        Alimento alimento = alimento(2, "Arroz", GrupoAlimentar.CARBOIDRATOS, "A002");
        when(alimentoRepository.findByGrupo(GrupoAlimentar.CARBOIDRATOS, fixture.pageable)).thenReturn(new PageImpl<>(List.of(alimento)));

        Page<AlimentoResponse> resultado = alimentoService.listar("", GrupoAlimentar.CARBOIDRATOS, fixture.pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(GrupoAlimentar.CARBOIDRATOS, resultado.getContent().getFirst().grupo());
        verify(alimentoRepository).findByGrupo(GrupoAlimentar.CARBOIDRATOS, fixture.pageable);
    }

    @Test
    @DisplayName("Deve listar todos quando não houver filtros")
    void deveListarTodosQuandoNaoHouverFiltros() {
        PageableFixture fixture = new PageableFixture();
        Alimento alimento = alimento(3, "Queijo", GrupoAlimentar.LATICINEOS, "A003");
        when(alimentoRepository.findAll(fixture.pageable)).thenReturn(new PageImpl<>(List.of(alimento)));

        Page<AlimentoResponse> resultado = alimentoService.listar(null, null, fixture.pageable);

        assertEquals(1, resultado.getTotalElements());
        verify(alimentoRepository).findAll(fixture.pageable);
    }

    @Test
    @DisplayName("Deve buscar por id quando alimento existir")
    void deveBuscarPorIdQuandoAlimentoExistir() {
        Alimento alimento = alimento(10, "Maçã", GrupoAlimentar.FRUTAS, "A010");
        when(alimentoRepository.findById(10)).thenReturn(Optional.of(alimento));

        AlimentoResponse response = alimentoService.buscarPorId(10);

        assertEquals(10, response.id());
        assertEquals("Maçã", response.descricao());
    }

    @Test
    @DisplayName("Deve lançar exceção quando buscar id inexistente")
    void deveLancarExcecaoQuandoBuscarIdInexistente() {
        when(alimentoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> alimentoService.buscarPorId(99));
    }

    @Test
    @DisplayName("Deve lançar exceção quando criar alimento com código duplicado")
    void deveLancarExcecaoQuandoCriarAlimentoComCodigoDuplicado() {
        CriarAlimentoRequest request = new CriarAlimentoRequest("A001", GrupoAlimentar.FRUTAS, "Banana", new BigDecimal("89.5"));
        when(alimentoRepository.existsByCodigoSubstituicao("A001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> alimentoService.criar(request));
        verify(alimentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar alimento quando dados forem válidos")
    void deveCriarAlimentoQuandoDadosForemValidos() {
        CriarAlimentoRequest request = new CriarAlimentoRequest("A020", GrupoAlimentar.PROTEINA, "Ovo", new BigDecimal("155.0"));
        Alimento salvo = alimento(20, "Ovo", GrupoAlimentar.PROTEINA, "A020");

        when(alimentoRepository.existsByCodigoSubstituicao("A020")).thenReturn(false);
        when(alimentoRepository.save(any(Alimento.class))).thenReturn(salvo);

        AlimentoResponse response = alimentoService.criar(request);

        assertEquals(20, response.id());
        assertEquals("Ovo", response.descricao());
    }

    @Test
    @DisplayName("Deve atualizar alimento quando id existir")
    void deveAtualizarAlimentoQuandoIdExistir() {
        AtualizarAlimentoRequest request = new AtualizarAlimentoRequest("A021", GrupoAlimentar.PROTEINA, "Frango", new BigDecimal("165.0"));
        Alimento existente = alimento(21, "Frango antigo", GrupoAlimentar.PROTEINA, "A020");

        when(alimentoRepository.findById(21)).thenReturn(Optional.of(existente));
        when(alimentoRepository.save(eq(existente))).thenReturn(existente);

        AlimentoResponse response = alimentoService.atualizar(21, request);

        assertEquals("A021", response.codigoSubstituicao());
        assertEquals("Frango", response.descricao());
    }

    @Test
    @DisplayName("Deve remover alimento quando id existir")
    void deveRemoverAlimentoQuandoIdExistir() {
        when(alimentoRepository.existsById(30)).thenReturn(true);

        alimentoService.remover(30);

        verify(alimentoRepository).deleteById(30);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover alimento inexistente")
    void deveLancarExcecaoAoRemoverAlimentoInexistente() {
        when(alimentoRepository.existsById(300)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> alimentoService.remover(300));
    }

    private static Alimento alimento(Integer id, String descricao, GrupoAlimentar grupo, String codigo) {
        return Alimento.builder()
                .id(id)
                .descricao(descricao)
                .grupo(grupo)
                .codigoSubstituicao(codigo)
                .energiaKcal(new BigDecimal("100"))
                .build();
    }

    private static class PageableFixture {
        private final PageRequest pageable = PageRequest.of(0, 20);
    }
}
