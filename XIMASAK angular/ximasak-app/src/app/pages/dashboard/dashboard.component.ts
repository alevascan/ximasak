import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  moduloActivo: string = 'resultados';
  rolActual: string = '';
  nombreUsuario: string = '';

  // Campana de notificaciones (Reservas)
  mostrarNotificaciones: boolean = false;

  // Datos crudos de la BD
  clientes: any[] = [];
  trabajadores: any[] = [];
  roles: any[] = [];
  productos: any[] = [];
  marcas: any[] = [];
  categorias: any[] = [];
  ventas: any[] = [];
  ventasFiltradas: any[] = [];
  reservas: any[] = [];

  ventaSeleccionada: any = null;
  mostrarDetalleVenta = false;
  cargandoDetalleVenta = false;
  errorDetalleVenta = '';
  tipoComprobante: 'BOLETA' | 'FACTURA' = 'BOLETA';
  generandoComprobante = false;

  // Filtros de Ventas (All day, All week, All month, All year)
  filtroPeriodo: string = 'year';
  fechaInicioFiltro: string = '2026-01-01';
  fechaFinFiltro: string = '2026-12-31';

  // Buscadores reales para Clientes y Productos
  busquedaCliente: string = '';
  busquedaProducto: string = '';

  // Formulario de Clientes (Edita en la tabla usuarios de la BD)
  clienteForm: any = { idUsuario: null, dni: '', nombre: '', correo: '', password: '', rol: 'CLIENTE' };
  modoEdicionCliente: boolean = false;

  // Formulario de Productos
  productoForm: any = {
  idProducto: null,
  nombre: '',
  descripcion: '',
  idMarca: null,
  idCategoria: null,
  precio: null,
  stock: null,
  stockMinimo: 3,
  imagen: ''
};
  modoEdicionProducto: boolean = false;

  // Formulario de Trabajadores (Solo Admin)
  trabajadorForm: any = { idUsuario: null, dni: '', nombre: '', correo: '', password: '', rol: 'Vendedor' };
  modoEdicionTrabajador: boolean = false;

  constructor(private adminService: AdminService, private router: Router,private http: HttpClient) {}

  ngOnInit(): void {
    this.rolActual = localStorage.getItem('rol') || '';
    this.nombreUsuario = localStorage.getItem('nombreUsuario') || 'Usuario';
    this.cargarTodo();
  }

  // Verifica si el usuario logueado es Administrador (acepta 'ADMIN' o 'Administrador')
  esAdmin(): boolean {
    const r = this.rolActual.toUpperCase();
    return r === 'ADMIN' || r === 'ADMINISTRADOR';
  }

  cambiarModulo(modulo: string) {
    this.moduloActivo = modulo;
    this.mostrarNotificaciones = false;
  }

  toggleNotificaciones() {
    this.mostrarNotificaciones = !this.mostrarNotificaciones;
  }

  irAReservasDesdeNotif() {
    this.mostrarNotificaciones = false;
    this.moduloActivo = 'tickets';
  }

  cargarTodo() {
    this.cargarUsuariosYClientes();
  this.cargarProductos();
  this.cargarMarcas();
  this.cargarCategorias();
  this.cargarVentas();
  this.cargarReservas();
  this.cargarRoles();

  }

  // ================= 1. RESERVAS / CAMPANITA =================
  cargarReservas() {
    this.adminService.getReservas().subscribe({
      next: (data) => {
        const locales = JSON.parse(localStorage.getItem('reservas_ximasak') || '[]');
        this.reservas = data && data.length > 0 ? data : locales;
      },
      error: () => {
        this.reservas = JSON.parse(localStorage.getItem('reservas_ximasak') || '[]');
      }
    });
  }

  // ================= 2. VENTAS Y FILTROS DE TIEMPO REALES =================
  cargarVentas() {
    this.adminService.getVentas().subscribe({
      next: (data) => {
        this.ventas = data || [];
        this.aplicarFiltroPeriodo(this.filtroPeriodo);
      },
      error: () => {
        this.ventas = [];
        this.ventasFiltradas = [];
      }
    });
  }


verDetalleVenta(venta: any): void {
  const id = venta.idVenta ?? venta.id;

  if (id == null) {
    this.errorDetalleVenta = 'La venta no tiene un identificador válido.';
    this.mostrarDetalleVenta = true;
    return;
  }

  this.ventaSeleccionada = null;
  this.errorDetalleVenta = '';
  this.cargandoDetalleVenta = true;
  this.mostrarDetalleVenta = true;

  this.http.get<any>(`http://localhost:8080/api/ventas/${id}`)
    .subscribe({
      next: (data) => {
        this.ventaSeleccionada = data;
        this.cargandoDetalleVenta = false;
      },
      error: (err) => {
        console.error('Error al cargar detalle de venta:', err);
        this.errorDetalleVenta =
          'No se pudo cargar el detalle. Verifica que la venta exista y que el backend esté funcionando.';
        this.cargandoDetalleVenta = false;
      }
    });
}

cerrarDetalleVenta(): void {
  this.mostrarDetalleVenta = false;
  this.ventaSeleccionada = null;
  this.errorDetalleVenta = '';
}

seleccionarTipoComprobante(tipo: 'BOLETA' | 'FACTURA'): void {
  this.tipoComprobante = tipo;
}

generarComprobante(): void {
  const venta = this.ventaSeleccionada;
  const id = venta?.idVenta ?? venta?.id;

  if (id == null) {
    alert('No se encontró el ID de la venta.');
    return;
  }

  if (
    this.tipoComprobante === 'FACTURA' &&
    !venta?.cliente?.ruc
  ) {
    alert('El cliente debe tener un RUC registrado para preparar la factura.');
    return;
  }

  if (this.generandoComprobante) {
    return;
  }

  this.generandoComprobante = true;

  this.http.get(
    `http://localhost:8080/api/ventas/${id}/comprobante`,
    {
      params: {
        tipo: this.tipoComprobante
      },
      responseType: 'blob'
    }
  ).subscribe({
    next: (pdf: Blob) => {
      this.generandoComprobante = false;

      if (!pdf || pdf.size === 0) {
        alert('El servidor devolvió un PDF vacío.');
        return;
      }

      const prefijo =
        this.tipoComprobante === 'FACTURA' ? 'F001' : 'B001';

      const numero = String(id).padStart(6, '0');
      const nombreArchivo = `${prefijo}-${numero}.pdf`;

      const url = window.URL.createObjectURL(pdf);
      const enlace = document.createElement('a');

      enlace.href = url;
      enlace.download = nombreArchivo;
      enlace.style.display = 'none';

      document.body.appendChild(enlace);
      enlace.click();
      enlace.remove();

      window.URL.revokeObjectURL(url);
    },

error: async (error) => {
  console.error('Error al generar el comprobante:', error);
  this.generandoComprobante = false;

  let mensaje = 'No se pudo generar el comprobante.';

  if (error.error instanceof Blob) {
    mensaje = await error.error.text();
  } else if (error.error?.message) {
    mensaje = error.error.message;
  }

  console.error('Estado HTTP:', error.status);
  console.error('Respuesta del servidor:', mensaje);

  alert(`Error ${error.status}: ${mensaje}`);
}
  });
}


  aplicarFiltroPeriodo(periodo: string) {
    this.filtroPeriodo = periodo;
    const hoy = new Date();

    if (periodo === 'day') {
      const hoyStr = hoy.toISOString().split('T')[0];
      this.fechaInicioFiltro = hoyStr;
      this.fechaFinFiltro = hoyStr;
    } else if (periodo === 'week') {
      const hace7Dias = new Date(hoy.getTime() - 7 * 24 * 60 * 60 * 1000);
      this.fechaInicioFiltro = hace7Dias.toISOString().split('T')[0];
      this.fechaFinFiltro = hoy.toISOString().split('T')[0];
    } else if (periodo === 'month') {
      const primerDiaMes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
      this.fechaInicioFiltro = primerDiaMes.toISOString().split('T')[0];
      this.fechaFinFiltro = hoy.toISOString().split('T')[0];
    } else if (periodo === 'year') {
      this.fechaInicioFiltro = `${hoy.getFullYear()}-01-01`;
      this.fechaFinFiltro = `${hoy.getFullYear()}-12-31`;
    }

    this.filtrarVentasPorRango();
  }

  filtrarVentasPorRango() {
    if (!this.ventas || this.ventas.length === 0) {
      this.ventasFiltradas = [];
      return;
    }
    const inicio = new Date(this.fechaInicioFiltro + 'T00:00:00');
    const fin = new Date(this.fechaFinFiltro + 'T23:59:59');

    this.ventasFiltradas = this.ventas.filter(v => {
      const fechaRaw = v.fecha || v.fechaVenta || v.fecha_venta || v.fechaRegistro;
      if (!fechaRaw) return true;
      const f = new Date(fechaRaw);
      return f >= inicio && f <= fin;
    });
  }

  calcularTotalIngresos(): number {
    return this.ventasFiltradas.reduce((acc, v) => acc + Number(v.total || v.montoTotal || 0), 0);
  }

  calcularVentasHoy(): number {
    const hoyStr = new Date().toISOString().split('T')[0];
    return this.ventas
      .filter(v => String(v.fecha || v.fechaVenta || '').startsWith(hoyStr))
      .reduce((acc, v) => acc + Number(v.total || v.montoTotal || 0), 0);
  }

  calcularAlturaTrimestre(trimestre: number): number {
    if (this.ventasFiltradas.length === 0) return 0;
    const ventasTrimestre = this.ventasFiltradas.filter(v => {
      const f = new Date(v.fecha || v.fechaVenta || new Date());
      const mes = f.getMonth(); // 0 a 11
      return Math.floor(mes / 3) + 1 === trimestre;
    });
    const totalT = ventasTrimestre.reduce((acc, v) => acc + Number(v.total || 0), 0);
    const totalGeneral = this.calcularTotalIngresos();
    return totalGeneral > 0 ? Math.min(100, Math.round((totalT / totalGeneral) * 100)) : 0;
  }
 // -------CARGAR ROLES -----------
 cargarRoles() {
  this.http.get<any[]>('http://localhost:8080/api/roles').subscribe({
    next: (data) => {
      this.roles = data || [];
      console.log('Roles cargados:', this.roles);
    },
    error: (err) => {
      console.error('Error al cargar roles:', err);
      this.roles = [];
    }
  });
}
// ================= CLIENTES DESDE LA TABLA clientes =================
cargarUsuariosYClientes() {
  // Obtener clientes reales desde Spring Boot
  this.http.get<any[]>('http://localhost:8080/api/clientes').subscribe({
    next: (data) => {
      this.clientes = (data || []).map(c => ({
        ...c,
        // Compatibilidad con los nombres que utiliza la tabla actual
        idUsuario: c.idCliente ?? c.idUsuario ?? c.id,
        nombre: c.nombre ?? '',
        dni: c.dni ?? '',
        correo: c.correo ?? '',
        estado: c.estado ?? true
      }));
    },
    error: (err) => {
      console.error('Error al cargar clientes desde /api/clientes:', err);
      this.clientes = [];
    }
  });

  // Mantener la carga de trabajadores desde el servicio existente
  this.adminService.getUsuarios().subscribe({
    next: (usuarios) => {
      const lista = usuarios || [];

      this.trabajadores = lista.filter(u => {
        const nombreRol = (u.rol?.nombre || u.rol || '').toUpperCase();
        return nombreRol !== 'CLIENTE';
      });
    },
    error: (err) => {
      console.error('Error al cargar trabajadores:', err);
    }
  });
}

  get clientesFiltrados(): any[] {
    const q = this.busquedaCliente.trim().toLowerCase();
    if (!q) return this.clientes;
    return this.clientes.filter(c =>
      (c.dni && c.dni.toLowerCase().includes(q)) ||
      (c.nombre && c.nombre.toLowerCase().includes(q)) ||
      (c.correo && c.correo.toLowerCase().includes(q))
    );
  }

  guardarCliente() {
    if (!this.clienteForm.dni || !this.clienteForm.nombre || !this.clienteForm.correo) {
      alert('Completa DNI, nombre y correo del cliente.');
      return;
    }

    if (this.modoEdicionCliente && this.clienteForm.idUsuario) {
      this.adminService.actualizarUsuario(this.clienteForm.idUsuario, this.clienteForm).subscribe({
        next: () => {
          alert('Datos del cliente actualizados en la Base de Datos.');
          this.resetClienteForm();
          this.cargarUsuariosYClientes();
        },
        error: (err) => alert(err.error?.error || 'Error al actualizar cliente')
      });
    } else {
      if (!this.clienteForm.password) {
        alert('Asigna una contraseña inicial para registrar al cliente.');
        return;
      }
      const payload = { ...this.clienteForm, rol: 'CLIENTE' };
      this.adminService.crearUsuario(payload).subscribe({
        next: () => {
          alert('Cliente registrado en la Base de Datos.');
          this.resetClienteForm();
          this.cargarUsuariosYClientes();
        },
        error: (err) => alert(err.error?.error || 'Error al registrar cliente')
      });
    }
  }

  seleccionarCliente(c: any) {
    this.modoEdicionCliente = true;
    this.clienteForm = {
      idUsuario: c.idUsuario,
      dni: c.dni,
      nombre: c.nombre,
      correo: c.correo,
      password: '', // En blanco por si no desea cambiar la contraseña
      rol: 'CLIENTE'
    };
  }

  borrarCliente(id: number) {
    if (confirm('¿Eliminar este cliente de la base de datos?')) {
      this.adminService.eliminarUsuario(id).subscribe(() => this.cargarUsuariosYClientes());
    }
  }

  resetClienteForm() {
    this.modoEdicionCliente = false;
    this.clienteForm = { idUsuario: null, dni: '', nombre: '', correo: '', password: '', rol: 'CLIENTE' };
  }

  // --- TRABAJADORES (SOLO ADMIN) ---
guardarTrabajador() {
    if (
      !this.trabajadorForm.dni ||
      !this.trabajadorForm.nombre ||
      !this.trabajadorForm.correo
    ) {
      alert('Completa DNI, nombre y correo del trabajador.');
      return;
    }

    if (this.modoEdicionTrabajador && this.trabajadorForm.idUsuario) {
      const datosActualizados = {
        dni: this.trabajadorForm.dni,
        nombre: this.trabajadorForm.nombre,
        correo: this.trabajadorForm.correo,
        password: this.trabajadorForm.password || '',
        idRol: Number(this.trabajadorForm.idRol)
      };

      if (!Number.isInteger(datosActualizados.idRol) || datosActualizados.idRol <= 0) {
        alert('Selecciona un rol válido.');
        return;
      }

      this.adminService.actualizarUsuario(
        this.trabajadorForm.idUsuario,
        datosActualizados
      ).subscribe({
        next: () => {
          alert('Trabajador actualizado correctamente.');
          this.resetTrabajadorForm();
          this.cargarUsuariosYClientes();
        },
        error: (err) => {
          alert(err.error?.error || 'Error al actualizar trabajador');
        }
      });
    } else {
      if (!this.trabajadorForm.password) {
        alert('Ingresa una contraseña para el trabajador.');
        return;
      }

      this.adminService.crearUsuario(this.trabajadorForm).subscribe({
        next: () => {
          alert(`Trabajador (${this.trabajadorForm.rol}) registrado en la BD.`);
          this.resetTrabajadorForm();
          this.cargarUsuariosYClientes();
        },
        error: (err) => {
          alert(err.error?.error || 'Error al crear trabajador');
        }
      });
    }
  }

  seleccionarTrabajador(t: any) {
    this.modoEdicionTrabajador = true;

    this.trabajadorForm = {
      idUsuario: t.idUsuario,
      dni: t.dni,
      nombre: t.nombre,
      correo: t.correo,
      password: '',
      rol: t.rol?.nombre || 'Vendedor',
      idRol: t.rol?.idRol ?? t.rol?.id
    };
  }

  resetTrabajadorForm() {
    this.modoEdicionTrabajador = false;

    this.trabajadorForm = {
      idUsuario: null,
      dni: '',
      nombre: '',
      correo: '',
      password: '',
      rol: 'Vendedor',
      idRol: null
    };
  }

  borrarTrabajador(id: number) {
    if (confirm('¿Dar de baja a este trabajador de la base de datos?')) {
      this.adminService.eliminarUsuario(id).subscribe(() => this.cargarUsuariosYClientes());
    }
  }



  // ================= 4. PRODUCTOS (BD) =================
  cargarProductos() {
    this.adminService.getProductos().subscribe({
      next: (data) => this.productos = data || [],
      error: (err) => console.error('Error cargando productos:', err)
    });
  }

  // Detecta el precio sin importar cómo se llame la variable en tu entidad Producto.java
  obtenerPrecio(p: any): number {
    return Number(p.precio ?? p.precioVenta ?? p.precio_venta ?? p.precioUnitario ?? p.costo ?? 0);
  }

  get productosFiltrados(): any[] {
    const q = this.busquedaProducto.trim().toLowerCase();
    if (!q) return this.productos;
    return this.productos.filter(p =>
      (p.nombre && p.nombre.toLowerCase().includes(q)) ||
      String(p.idProducto || p.id).includes(q)
    );
  }

  cargarMarcas() {
  this.http.get<any[]>('http://localhost:8080/api/marcas').subscribe({
    next: (data) => {
      this.marcas = data || [];
      console.log('Marcas cargadas:', this.marcas);
    },
    error: (err) => {
      console.error('Error al cargar marcas:', err);
      this.marcas = [];
    }
  });
}

cargarCategorias() {
  this.http.get<any[]>('http://localhost:8080/api/categorias').subscribe({
    next: (data) => {
      this.categorias = data || [];
      console.log('Categorías cargadas:', this.categorias);
    },
    error: (err) => {
      console.error('Error al cargar categorías:', err);
      this.categorias = [];
    }
  });
}

guardarProducto() {
  if (
    !this.productoForm.nombre?.trim() ||
    this.productoForm.precio === null ||
    this.productoForm.precio === '' ||
    this.productoForm.stock === null ||
    this.productoForm.stock === ''
  ) {
    alert('Completa el nombre, precio y stock del producto.');
    return;
  }

  const idMarca = Number(this.productoForm.idMarca);
  const idCategoria = Number(this.productoForm.idCategoria);
  const precio = Number(this.productoForm.precio);
  const stock = Number(this.productoForm.stock);
  const stockMinimo = Number(this.productoForm.stockMinimo ?? 3);

  if (!Number.isFinite(precio) || precio < 0 ||
      !Number.isInteger(stock) || stock < 0 ||
      !Number.isInteger(stockMinimo) || stockMinimo < 0) {
    alert('Verifica que el precio y las cantidades sean válidos.');
    return;
  }

  if (!Number.isInteger(idMarca) || idMarca <= 0) {
    alert('Selecciona una marca válida para el producto.');
    return;
  }

  if (!Number.isInteger(idCategoria) || idCategoria <= 0) {
    alert('Selecciona una categoría válida para el producto.');
    return;
  }

  const payload = {
    nombre: this.productoForm.nombre.trim(),
    descripcion: this.productoForm.descripcion || '',
    marca: { idMarca: idMarca },
    categoria: { idCategoria: idCategoria },
    precioUnitario: precio,
    stock: stock,
    stockMinimo: stockMinimo,
    imagen: this.productoForm.imagen || null,
    estado: true
  };

  const id = this.productoForm.idProducto || this.productoForm.id;

  const operacion = this.modoEdicionProducto && id
    ? this.adminService.actualizarProducto(id, payload)
    : this.adminService.crearProducto(payload);

  operacion.subscribe({
    next: () => {
      alert(
        this.modoEdicionProducto
          ? 'Producto actualizado en la Base de Datos.'
          : 'Producto agregado exitosamente a la Base de Datos.'
      );
      this.resetProductoForm();
      this.cargarProductos();
    },
    error: (err) => {
      console.error('Error al guardar producto:', err);
      console.error('Respuesta del backend:', err.error);

      const detalle =
        typeof err.error === 'string'
          ? err.error
          : err.error?.message ||
            err.error?.error ||
            'Revisa la consola de Spring Boot para conocer la causa exacta.';

      alert(`Error ${err.status}: ${detalle}`);
    }
  });
}


  seleccionarProducto(p: any) {
    this.modoEdicionProducto = true;
    this.productoForm = {
      ...p,
      idProducto: p.idProducto || p.id,
      nombre: p.nombre,
      precio: this.obtenerPrecio(p),
      stock: p.stock
    };
  }

  borrarProducto(p: any) {
    const id = p.idProducto || p.id;
    if (confirm('¿Eliminar este producto de la Base de Datos?')) {
      this.adminService.eliminarProducto(id).subscribe(() => this.cargarProductos());
    }
  }

resetProductoForm() {
  this.modoEdicionProducto = false;
  this.productoForm = {
    idProducto: null,
    nombre: '',
    descripcion: '',
    idMarca: null,
    idCategoria: null,
    precio: null,
    stock: null,
    stockMinimo: 3,
    imagen: ''
  };
}
  obtenerProductosStockBajo(): any[] {
    return this.productos.filter(p => Number(p.stock) <= Number(p.stockMinimo || 3));
  }

  // --- NAVEGACIÓN ---

  // ==================== POS / PUNTO DE VENTA (HU-04, HU-05) ====================
  posBusquedaDni: string = '';
  posClienteSeleccionado: any = null;
  posMensajeCliente: string = '';
  posCarrito: any[] = [];
  posMedioPago: number = 1;
  posCargandoVenta: boolean = false;
  posFiltroProducto: string = '';

  get productosFiltradosPos(): any[] {
    const f = this.posFiltroProducto.trim().toLowerCase();
    if (!f) return this.productos;
    return this.productos.filter((p: any) => (p.nombre || '').toLowerCase().includes(f));
  }

  buscarClientePos(): void {
    this.posMensajeCliente = '';
    const doc = this.posBusquedaDni.trim();
    if (!doc) {
      this.posMensajeCliente = 'Por favor ingresa un número de DNI o RUC.';
      return;
    }
    this.http.get<any>(`http://localhost:8080/api/clientes/buscar?documento=${doc}`).subscribe({
      next: (cli) => {
        this.posClienteSeleccionado = cli;
        this.posMensajeCliente = `✓ Cliente verificado en BD: ${cli.nombre}`;
      },
      error: () => {
        const encontrado = this.clientes.find((c: any) => c.dni === doc || c.ruc === doc);
        if (encontrado) {
          this.posClienteSeleccionado = encontrado;
          this.posMensajeCliente = `✓ Cliente encontrado: ${encontrado.nombre}`;
        } else {
          this.posClienteSeleccionado = { idCliente: 1, nombre: `Cliente Mostrador (${doc})`, dni: doc };
          this.posMensajeCliente = `Cliente nuevo. Se asociará como Venta Mostrador.`;
        }
      }
    });
  }

  agregarAlCarritoPos(p: any): void {
    const id = p.idProducto || p.id;
    const existente = this.posCarrito.find((item: any) => (item.producto.idProducto || item.producto.id) === id);
    const precio = Number(this.obtenerPrecio(p));

    if (existente) {
      if (existente.cantidad >= p.stock) {
        alert(`No hay más existencias para ${p.nombre}. Stock disponible: ${p.stock}`);
        return;
      }
      existente.cantidad++;
      existente.subtotal = existente.cantidad * precio;
    } else {
      if (p.stock <= 0) {
        alert(`El producto ${p.nombre} está agotado.`);
        return;
      }
      this.posCarrito.push({
        producto: p,
        cantidad: 1,
        precioUnitario: precio,
        subtotal: precio
      });
    }
  }

  cambiarCantidadPos(item: any, delta: number): void {
    const nueva = item.cantidad + delta;
    if (nueva <= 0) {
      this.quitarDelCarritoPos(item);
      return;
    }
    if (nueva > item.producto.stock) {
      alert(`No puedes superar el stock disponible (${item.producto.stock})`);
      return;
    }
    item.cantidad = nueva;
    item.subtotal = item.cantidad * item.precioUnitario;
  }

  quitarDelCarritoPos(item: any): void {
    this.posCarrito = this.posCarrito.filter((i: any) => i !== item);
  }

  calcularTotalPos(): number {
    return this.posCarrito.reduce((acc: number, item: any) => acc + item.subtotal, 0);
  }

  calcularSubtotalSinIgvPos(): number {
    return this.calcularTotalPos() / 1.18;
  }

  calcularIgvPos(): number {
    return this.calcularTotalPos() - this.calcularSubtotalSinIgvPos();
  }

  procesarVentaPos(): void {
    if (this.posCarrito.length === 0) {
      alert('El carrito está vacío. Agrega al menos un producto.');
      return;
    }

    if (!this.posClienteSeleccionado) {
      alert('Por favor busca o selecciona un cliente por DNI/RUC antes de cobrar.');
      return;
    }

    const idUsuario = Number(localStorage.getItem('idUsuario') || 1);
    const idCliente = this.posClienteSeleccionado.idCliente || 1;

    const payload = {
      idCliente: idCliente,
      idUsuario: idUsuario,
      idMedioPago: Number(this.posMedioPago),
      items: this.posCarrito.map((item: any) => ({
        idProducto: item.producto.idProducto || item.producto.id,
        cantidad: item.cantidad
      }))
    };

    this.posCargandoVenta = true;
    this.http.post<any>('http://localhost:8080/api/ventas', payload).subscribe({
      next: (ventaRes) => {
        this.posCargandoVenta = false;
        alert(`¡Venta #${ventaRes.idVenta || ventaRes.id || ''} procesada con éxito!\n• Subtotal: S/ ${ventaRes.subtotal}\n• IGV (18%): S/ ${ventaRes.igv}\n• Total: S/ ${ventaRes.total}\n\nStock descontado automáticamente.`);
        this.posCarrito = [];
        this.posBusquedaDni = '';
        this.posClienteSeleccionado = null;
        this.cargarProductos();
        this.cargarVentas();
      },
      error: (err) => {
        this.posCargandoVenta = false;
        alert('Error al procesar la venta: ' + (err.error?.error || err.error?.mensaje || 'Error en el servidor.'));
      }
    });
  }

  atenderTicket(r: any): void {
    const id = r.idReserva || r.id;
    if (!confirm(`¿Confirmas marcar el ticket #${id} como ATENDIDA y enviar la notificación automática por correo al cliente?`)) {
      return;
    }
    this.http.patch<any>(`http://localhost:8080/api/reservas/${id}/estado`, { estado: 'ATENDIDA' }).subscribe({
      next: () => {
        r.estado = 'ATENDIDA';
        alert(`✓ Orden #${id} marcada como ATENDIDA. Se intentó notificar al cliente por correo (solo si tiene un correo válido registrado).`);
        this.cargarReservas();
      },
      error: (err) => {
        alert(`No se pudo actualizar la orden #${id}: ` + (err?.error?.error || err?.message || 'error de conexión con el servidor.') + ' El estado NO fue modificado.');
      }
    });
  }


  irATienda() {
    this.router.navigate(['/']);
  }

  cerrarSesion() {
    localStorage.clear();
    this.router.navigate(['/']);
  }
}