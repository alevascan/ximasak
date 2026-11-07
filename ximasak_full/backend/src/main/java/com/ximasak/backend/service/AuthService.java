package com.ximasak.backend.service;

import com.ximasak.backend.entity.Rol;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.repository.RolRepository;
import com.ximasak.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RolRepository rolRepository; 

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RolRepository rolRepository) { // <-- Agregado al constructor

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rolRepository = rolRepository; // <-- Agregado a la inicialización
    }

    @Transactional
    public void registrarNuevoCliente(String dni, String nombre, String correo, String password) {
        
        // 1. Validar que el correo no exista ya en la Base de Datos
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new RuntimeException("El correo ya se encuentra registrado.");
        }

        // 2. Buscar el Rol 'CLIENTE' en la base de datos
        Rol rolCliente = rolRepository.findByNombre("CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error: El rol CLIENTE no existe en la base de datos."));

       // 3. Crear la entidad Usuario y setear los datos
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setDni(dni); // <--- ESTA ES LA LÍNEA QUE FALTABA
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setEstado(true); 
        
        
        nuevoUsuario.setPassword(passwordEncoder.encode(password)); 
        nuevoUsuario.setRol(rolCliente);

        // 4. Guardar en la base de datos
        usuarioRepository.save(nuevoUsuario);
    }
    
    @Transactional
    public Usuario crearUsuarioConRol(String dni, String nombre, String correo, String password, String nombreRol) {
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new RuntimeException("El correo ya está registrado.");
        }
        Rol rolAsignado = rolRepository.findByNombre(nombreRol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("El rol " + nombreRol + " no existe en la BD."));

        Usuario u = new Usuario();
        u.setDni(dni);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setEstado(true);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rolAsignado);

        return usuarioRepository.save(u);
    }

    public Usuario buscarUsuarioPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Correo o contraseña incorrectos"));
    }

    private final java.util.concurrent.ConcurrentHashMap<String, Integer> intentosFallidos = new java.util.concurrent.ConcurrentHashMap<>();

    public String login(String correo, String password) {
        if (intentosFallidos.getOrDefault(correo, 0) >= 3) {
            throw new RuntimeException("Cuenta bloqueada temporalmente tras 3 intentos fallidos de autenticación (RNF-02). Contacte al administrador.");
        }

        Usuario usuario = buscarUsuarioPorCorreo(correo);

        if (!Boolean.TRUE.equals(usuario.getEstado())) {
            throw new RuntimeException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            int intentos = intentosFallidos.getOrDefault(correo, 0) + 1;
            intentosFallidos.put(correo, intentos);
            if (intentos >= 3) {
                throw new RuntimeException("Cuenta bloqueada temporalmente tras 3 intentos fallidos de autenticación (RNF-02). Contacte al administrador.");
            }
            throw new RuntimeException("Correo o contraseña incorrectos. Intentos restantes: " + (3 - intentos));
        }

        intentosFallidos.remove(correo);
        return jwtService.generarToken(usuario);
    }


// 1. Listar todos los usuarios de la BD
    public java.util.List<Usuario> listarTodosLosUsuarios() {
        return usuarioRepository.findAll();
    }

    // 2. Crear trabajador o cliente desde el Dashboard (busca el rol sin importar mayúsculas/minúsculas)
    @Transactional
    public Usuario crearUsuarioDesdeAdmin(String dni, String nombre, String correo, String password, String nombreRol) {
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw new RuntimeException("El correo ya está registrado.");
        }

        Rol rolAsignado = rolRepository.findAll().stream()
                .filter(r -> r.getNombre().equalsIgnoreCase(nombreRol) ||
                        (nombreRol.equalsIgnoreCase("ADMIN") && r.getNombre().toUpperCase().contains("ADMIN")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró el rol: " + nombreRol));

        Usuario u = new Usuario();
        u.setDni(dni);
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setEstado(true);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rolAsignado);

        return usuarioRepository.save(u);
    }

    // 3. Editar datos de un Cliente o Trabajador (incluyendo DNI, nombre, correo y contraseña opcional)
    @Transactional
    public Usuario actualizarUsuario(Integer id, String dni, String nombre, String correo, String nuevaPassword) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        u.setDni(dni);
        u.setNombre(nombre);
        u.setCorreo(correo);

        // Si escribiste una nueva contraseña en el formulario, la encripta y actualiza
        if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
            u.setPassword(passwordEncoder.encode(nuevaPassword));
        }

        return usuarioRepository.save(u);
    }

    // 4. Eliminar usuario de la BD
    @Transactional
    public void eliminarUsuario(Integer id) {
        usuarioRepository.deleteById(id); 
    }
}