package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class CadastroCvmParser {

    private static final String ARQUIVO = "cad_intermed.csv";
    private static final String SITUACAO_ATIVA = "EM FUNCIONAMENTO NORMAL";
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    public Set<String> parse(byte[] zipBytes) {
        try {
            byte[] csvBytes = extrairCsv(zipBytes);
            String conteudo = decodificar(csvBytes);
            if (conteudo.startsWith("\uFEFF")) {
                conteudo = conteudo.substring(1);
            } else if (conteudo.startsWith("ï»¿")) {
                conteudo = conteudo.substring(3);
            }
            return lerAtivos(new StringReader(conteudo));
        } catch (FalhaDeProvedorException e) {
            throw e;
        } catch (RuntimeException | IOException e) {
            throw new FalhaDeProvedorException("CVM", e);
        }
    }

    private byte[] extrairCsv(byte[] zipBytes) throws IOException {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new FalhaDeProvedorException("CVM");
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String nome = entry.getName().replace('\\', '/');
                if (!entry.isDirectory() && nome.substring(nome.lastIndexOf('/') + 1).equals(ARQUIVO)) {
                    ByteArrayOutputStream csv = new ByteArrayOutputStream();
                    zip.transferTo(csv);
                    return csv.toByteArray();
                }
            }
        }
        throw new FalhaDeProvedorException("CVM");
    }

    private String decodificar(byte[] bytes) throws CharacterCodingException {
        return WINDOWS_1252.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private Set<String> lerAtivos(Reader reader) throws IOException {
        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();
        try (CSVParser csv = formato.parse(reader)) {
            if (!csv.getHeaderMap().containsKey("CNPJ") || !csv.getHeaderMap().containsKey("SIT")) {
                throw new FalhaDeProvedorException("CVM");
            }
            Set<String> ativos = new HashSet<>();
            for (CSVRecord registro : csv) {
                String situacao = registro.get("SIT").trim().toUpperCase(Locale.ROOT);
                if (!SITUACAO_ATIVA.equals(situacao)) {
                    continue;
                }
                String cnpjOriginal = registro.get("CNPJ").trim();
                if (cnpjOriginal.isEmpty()) {
                    continue;
                }
                String cnpj = cnpjOriginal.replaceAll("\\D", "");
                if (cnpj.length() != 14) {
                    throw new FalhaDeProvedorException("CVM");
                }
                ativos.add(cnpj);
            }
            return Set.copyOf(ativos);
        }
    }
}
