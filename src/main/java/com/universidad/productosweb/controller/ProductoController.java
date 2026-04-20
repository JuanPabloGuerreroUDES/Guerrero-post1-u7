package com.universidad.productosweb.controller;

import com.universidad.productosweb.model.Producto;
import com.universidad.productosweb.service.ProductoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador que maneja las operaciones CRUD de productos.
 * Implementa el patrón MVC y Post/Redirect/Get.
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService servicio;

    // GET /productos → lista todos los productos
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", servicio.obtenerTodos());
        return "productos/lista";
    }

    // GET /productos/nuevo → muestra formulario de creación
    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("accion", "Crear");
        return "productos/formulario";
    }

    // GET /productos/editar/{id} → muestra formulario prellenado
    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Producto producto = servicio.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));

        model.addAttribute("producto", producto);
        model.addAttribute("accion", "Editar");

        return "productos/formulario";
    }

    // POST /productos/guardar → guarda y redirige (patrón PRG)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto) {
        servicio.guardar(producto);
        return "redirect:/productos";
    }

    // GET /productos/eliminar/{id} → elimina y redirige
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return "redirect:/productos";
    }
}