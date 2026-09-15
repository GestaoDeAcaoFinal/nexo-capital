package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadastroCvmParserTest {

    private final CadastroCvmParser parser = new CadastroCvmParser();

    @Test
    void indexaSomenteParticipantesAtivosComCnpjNormalizadoEConjuntoImutavel() {
        byte[] zip = zipComCsv("""
                CNPJ;SIT;DENOM_SOCIAL
                12.345.678/0001-90;EM FUNCIONAMENTO NORMAL;Ativa Çorretora
                98.765.432/0001-10;CANCELADA;Inativa
                ;EM FUNCIONAMENTO NORMAL;Sem CNPJ
                """);

        Set<String> ativos = parser.parse(zip);

        assertThat(ativos).containsExactly("12345678000190");
        assertThat(ativos).doesNotContain("98765432000110", "00000000000000");
        assertThatThrownBy(() -> ativos.add("11111111000111"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejeitaZipAusenteCorrompidoOuSemColunasObrigatorias() {
        assertThatThrownBy(() -> parser.parse(new byte[]{1, 2, 3}))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> parser.parse(zipComCsv("CNPJ;DENOM_SOCIAL\n123;Teste\n")))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> parser.parse(zipComEntrada("outro.csv", "CNPJ;SIT\n".getBytes())))
                .isInstanceOf(FalhaDeProvedorException.class);
    }

    @Test
    void rejeitaConteudoQueNaoPodeSerDecodificadoEmWindows1252() {
        byte[] csv = "CNPJ;SIT\n".getBytes(Charset.forName("windows-1252"));
        byte[] invalido = new byte[csv.length + 1];
        System.arraycopy(csv, 0, invalido, 0, csv.length);
        invalido[invalido.length - 1] = (byte) 0x81;

        assertThatThrownBy(() -> parser.parse(zipComEntrada("cad_intermed.csv", invalido)))
                .isInstanceOf(FalhaDeProvedorException.class);
    }

    private byte[] zipComCsv(String csv) {
        return zipComEntrada("cad_intermed.csv", csv.stripIndent().getBytes(Charset.forName("windows-1252")));
    }

    private byte[] zipComEntrada(String nome, byte[] conteudo) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry(nome));
                zip.write(conteudo);
                zip.closeEntry();
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
