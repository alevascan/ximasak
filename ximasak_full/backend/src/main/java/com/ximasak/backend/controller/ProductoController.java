package com.ximasak.backend.controller;

import com.ximasak.backend.entity.Categoria;
import com.ximasak.backend.entity.Marca;
import com.ximasak.backend.entity.Producto;
import com.ximasak.backend.repository.CategoriaRepository;
import com.ximasak.backend.repository.MarcaRepository;
import com.ximasak.backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {

    private final ProductoService productoService;
    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoController(
            ProductoService productoService,
            MarcaRepository marcaRepository,
            CategoriaRepository categoriaRepository) {
        this.productoService = productoService;
        this.marcaRepository = marcaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Producto>> buscar(@RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(productoService.buscarPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody Map<String, Object> payload) {
        try {
            Producto p = new Producto();
            p.setNombre((String) payload.get("nombre"));
            p.setDescripcion((String) payload.get("descripcion"));
            p.setPrecioUnitario(new BigDecimal(payload.get("precio").toString()));
            p.setStock(Integer.parseInt(payload.get("stock").toString()));
            p.setStockMinimo(payload.get("stockMinimo") != null ? Integer.parseInt(payload.get("stockMinimo").toString()) : 3);
            p.setEstado(true);

            Integer idMarca = payload.get("idMarca") != null ? Integer.parseInt(payload.get("idMarca").toString()) : 1;
            Marca marca = marcaRepository.findById(idMarca).orElse(marcaRepository.findAll().stream().findFirst().orElse(null));
            p.setMarca(marca);

            Integer idCat = payload.get("idCategoria") != null ? Integer.parseInt(payload.get("idCategoria").toString()) : 1;
            Categoria cat = categoriaRepository.findById(idCat).orElse(categoriaRepository.findAll().stream().findFirst().orElse(null));
            p.setCategoria(cat);

            Producto guardado = productoService.guardar(p);
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        try {
            Producto p = new Producto();
            if (payload.get("nombre") != null) p.setNombre((String) payload.get("nombre"));
            if (payload.get("descripcion") != null) p.setDescripcion((String) payload.get("descripcion"));
            if (payload.get("precio") != null) p.setPrecioUnitario(new BigDecimal(payload.get("precio").toString()));
            if (payload.get("stock") != null) p.setStock(Integer.parseInt(payload.get("stock").toString()));
            if (payload.get("stockMinimo") != null) p.setStockMinimo(Integer.parseInt(payload.get("stockMinimo").toString()));

            Producto actualizado = productoService.actualizar(id, p);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Integer id) {
        productoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado correctamente"));
    }
}
