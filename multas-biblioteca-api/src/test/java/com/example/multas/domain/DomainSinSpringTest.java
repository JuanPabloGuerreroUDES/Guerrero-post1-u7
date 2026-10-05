package com.example.multas.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Revisa que las clases de domain/ solo importen java.* o el mismo domain
class DomainSinSpringTest {

    @Test
    void domainNoImportaSpringNiJpaNiClientesHttp() throws IOException {
        Path carpeta = Path.of("src/main/java/com/example/multas/domain");
        List<String> imports;
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            imports = archivos.filter(p -> p.toString().endsWith(".java"))
                    .flatMap(p -> {
                        try {
                            return Files.readAllLines(p).stream();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(linea -> linea.startsWith("import "))
                    .toList();
        }

        assertFalse(imports.isEmpty());
        imports.forEach(linea -> assertTrue(
                linea.startsWith("import java.") || linea.startsWith("import com.example.multas.domain."),
                "Import no permitido en domain/: " + linea));
    }
}
