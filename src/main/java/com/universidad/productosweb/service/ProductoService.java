package com.universidad.productosweb.service;

import com.universidad.productosweb.model.Producto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service // Spring gestiona esta instancia como singleton
public class ProductoService {

    private final Map<Long, Producto> productos = new LinkedHashMap<>();
    private Long contadorId = 1L;

    public ProductoService() {
        // Datos de ejemplo para arrancar
        guardar(new Producto(null, "Laptop", "Laptop 15 pulgadas 16GB RAM", 1299.99));
        guardar(new Producto(null, "Mouse", "Mouse inalámbrico ergonómico", 29.99));
        guardar(new Producto(null, "Teclado", "Teclado mecánico TKL", 89.99));
    }

    public List<Producto> obtenerTodos() {
        return new ArrayList<>(productos.values());
    }

    public Optional<Producto> buscarPorId(Long id) {
        return Optional.ofNullable(productos.get(id));
    }

    public Producto guardar(Producto producto) {
        if (producto.getId() == null) {
            producto.setId(contadorId++);
        }
        productos.put(producto.getId(), producto);
        return producto;
    }

    public void eliminar(Long id) {
        productos.remove(id);
    }
}