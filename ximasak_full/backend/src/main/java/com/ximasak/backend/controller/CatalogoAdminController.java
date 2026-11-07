package com.ximasak.backend.controller;

import com.ximasak.backend.entity.Categoria;
import com.ximasak.backend.entity.Marca;
import com.ximasak.backend.entity.Rol;
import com.ximasak.backend.repository.CategoriaRepository;
import com.ximasak.backend.repository.MarcaRepository;
import com.ximasak.backend.repository.RolRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class CatalogoAdminController {

    private final MarcaRepository marcaRepository;
    private final CategoriaRepository categoriaRepository;
    private final RolRepository rolRepository;

    public CatalogoAdminController(
            MarcaRepository marcaRepository,
            CategoriaRepository categoriaRepository,
            RolRepository rolRepository) {
        this.marcaRepository = marcaRepository;
        this.categoriaRepository = categoriaRepository;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/marcas")
    public ResponseEntity<List<Marca>> listarMarcas() {
        return ResponseEntity.ok(marcaRepository.findAll());
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(categoriaRepository.findAll());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(rolRepository.findAll());
    }
}
