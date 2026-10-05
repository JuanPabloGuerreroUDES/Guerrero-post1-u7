package com.example.multas.repository;

import com.example.multas.model.EstadoMulta;
import com.example.multas.model.Multa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultaRepository extends JpaRepository<Multa, Long> {

    List<Multa> findByEstudianteId(String estudianteId);

    // Spring Data lo traduce a un COUNT en SQL, no trae las multas a memoria (Punto de decisión 2)
    long countByEstudianteIdAndEstado(String estudianteId, EstadoMulta estado);
}
