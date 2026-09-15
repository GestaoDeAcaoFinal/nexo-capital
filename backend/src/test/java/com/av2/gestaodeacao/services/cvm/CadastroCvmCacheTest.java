package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.cliente.cvm.CadastroCvmGateway;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroCvmCacheTest {

    @Test
    void reutilizaSnapshotImutavelAntesDoTtlEAtualizaNoVencimento() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        Queue<byte[]> respostas = new ArrayDeque<>();
        respostas.add(zipAtivo("12.345.678/0001-90"));
        respostas.add(zipAtivo("98.765.432/0001-10"));
        AtomicInteger downloads = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            downloads.incrementAndGet();
            return respostas.remove();
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24), clock);

        Set<String> primeiro = cache.intermediariosAtivos();
        assertThat(cache.intermediariosAtivos()).isSameAs(primeiro);
        assertThat(downloads).hasValue(1);
        assertThat(primeiro).containsExactly("12345678000190");
        assertThatThrownBy(() -> primeiro.add("11111111000111"))
                .isInstanceOf(UnsupportedOperationException.class);

        clock.avancar(Duration.ofHours(24));

        assertThat(cache.intermediariosAtivos()).containsExactly("98765432000110");
        assertThat(downloads).hasValue(2);
    }

    @Test
    void falhaFechadoSemSnapshotValidoInclusiveQuandoOAnteriorExpirou() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
        AtomicInteger chamada = new AtomicInteger();
        CadastroCvmGateway gateway = () -> {
            if (chamada.getAndIncrement() == 0) {
                return zipAtivo("12.345.678/0001-90");
            }
            throw new IllegalStateException("URL e conteúdo interno não devem vazar");
        };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24), clock);
        assertThat(cache.intermediarioAtivo("12.345.678/0001-90")).isTrue();

        clock.avancar(Duration.ofHours(24).plusSeconds(1));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("URL")
                .hasMessageNotContaining("conteúdo");
    }

    @Test
    void primeiraCargaComFalhaTambemResultaEmIndisponibilidade() {
        CadastroCvmGateway gateway = () -> { throw new IllegalStateException("segredo"); };
        CadastroCvmCache cache = new CadastroCvmCache(
                gateway, new CadastroCvmParser(), Duration.ofHours(24),
                Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(cache::intermediariosAtivos)
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("segredo");
    }

    private static byte[] zipAtivo(String cnpj) {
        String csv = "CNPJ;SIT\n" + cnpj + ";EM FUNCIONAMENTO NORMAL\n";
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
                zip.write(csv.getBytes(Charset.forName("windows-1252")));
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) { this.instant = instant; }
        private void avancar(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
