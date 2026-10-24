import { Component, computed, signal, effect, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CatalogoService, Producto } from '../../services/catalogo.service';

type CriterioOrden = 'relevancia' | 'precioAsc' | 'precioDesc' | 'nombreAsc';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './catalogo.component.html',
})
export class CatalogoComponent {
  // --- Filtros ---
  textoBusqueda = signal('');
  precioMin = signal<number | null>(null);
  precioMax = signal<number | null>(null);
  ordenSeleccionado = signal<CriterioOrden>('relevancia');
  categoriasSeleccionadas = signal<Set<string>>(new Set());

  // --- Modales admin ---
  modalAgregarAbierto = signal(false);
  modalAdministrarAbierto = signal(false);
  busquedaAdmin = signal('');

  // Formulario "agregar producto"
  nuevoNombre = '';
  nuevoPrecio: number | null = null;
  nuevaMarca = '';
  nuevaCategoria = '';
  nuevoStock = 1;
  nuevaFotoPreview: string | null = null;

  @ViewChild('fotoInput') fotoInputRef?: ElementRef<HTMLInputElement>;

  constructor(
    public auth: AuthService,
    public catalogo: CatalogoService,
    private router: Router
  ) {
    // Cuando aparece una categoría nueva (carga inicial o producto agregado),
    // se marca automáticamente como seleccionada -> mismo comportamiento que el script.js original.
    effect(() => {
      const disponibles = this.catalogo.categorias();
      const actuales = this.categoriasSeleccionadas();
      const combinado = new Set(actuales);
      let cambio = false;
      disponibles.forEach((c) => {
        if (!actuales.has(c)) {
          combinado.add(c);
          cambio = true;
        }
      });
      if (cambio) this.categoriasSeleccionadas.set(combinado);
    });
  }

  toggleCategoria(cat: string): void {
    const set = new Set(this.categoriasSeleccionadas());
    if (set.has(cat)) {
      set.delete(cat);
    } else {
      set.add(cat);
    }
    this.categoriasSeleccionadas.set(set);
  }

  productosFiltrados = computed<Producto[]>(() => {
    const texto = this.textoBusqueda().toLowerCase();
    const min = this.precioMin() ?? 0;
    const max = this.precioMax() ?? Infinity;
    const categorias = this.categoriasSeleccionadas();
    const orden = this.ordenSeleccionado();

    let lista = this.catalogo.productos().filter((p) => {
      const pasaCategoria = categorias.has(p.categoria.toLowerCase());
      const pasaBusqueda = p.nombre.toLowerCase().includes(texto);
      const pasaPrecio = p.precio >= min && p.precio <= max;
      return pasaCategoria && pasaBusqueda && pasaPrecio;
    });

    lista = [...lista].sort((a, b) => {
      if (orden === 'precioAsc') return a.precio - b.precio;
      if (orden === 'precioDesc') return b.precio - a.precio;
      if (orden === 'nombreAsc') return a.nombre.localeCompare(b.nombre);
      return 0;
    });

    return lista;
  });

  resultadosBusquedaAdmin = computed<Producto[]>(() => {
    const termino = this.busquedaAdmin().toLowerCase();
    if (!termino) return [];
    return this.catalogo.productos().filter((p) => p.nombre.toLowerCase().includes(termino));
  });

  // --- Acciones cliente ---
  irAReservaDesdeCatalogo(nombreProducto: string): void {
    this.router.navigate(['/reserva'], { queryParams: { producto: nombreProducto } });
  }

  // --- Modal agregar producto ---
  abrirModalAgregar(): void {
    this.modalAgregarAbierto.set(true);
  }

  cerrarModalAgregar(): void {
    this.modalAgregarAbierto.set(false);
  }

  onSeleccionarFoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files && input.files[0];

    if (!archivo) {
      this.nuevaFotoPreview = null;
      return;
    }

    if (!archivo.type.startsWith('image/')) {
      alert('Por favor selecciona un archivo de imagen válido.');
      input.value = '';
      this.nuevaFotoPreview = null;
      return;
    }

    const LIMITE_BYTES = 2 * 1024 * 1024; // 2MB: se guarda como base64 en localStorage
    if (archivo.size > LIMITE_BYTES) {
      alert('La imagen es muy pesada. Elige una de máximo 2MB.');
      input.value = '';
      this.nuevaFotoPreview = null;
      return;
    }

    const lector = new FileReader();
    lector.onload = () => {
      this.nuevaFotoPreview = lector.result as string;
    };
    lector.readAsDataURL(archivo);
  }

  guardarNuevoProducto(): void {
    if (!this.nuevoNombre.trim() || this.nuevoPrecio === null || isNaN(this.nuevoPrecio) || !this.nuevaCategoria.trim()) {
      alert('Por favor, ingresa al menos el nombre, precio y selecciona/escribe una categoría.');
      return;
    }

    const nuevoProducto: Producto = {
      nombre: this.nuevoNombre.trim(),
      precio: this.nuevoPrecio,
      marca: this.nuevaMarca || 'Genérica',
      categoria: this.nuevaCategoria.trim(),
      stock: this.nuevoStock && !isNaN(this.nuevoStock) ? this.nuevoStock : 1,
      // Si el usuario no sube una imagen, se usa el logo de XIMASAK como imagen genérica.
      foto: this.nuevaFotoPreview || 'assets/fotos/xima.png',
    };

    this.catalogo.agregarProducto(nuevoProducto);

    this.nuevoNombre = '';
    this.nuevoPrecio = null;
    this.nuevaMarca = '';
    this.nuevaCategoria = '';
    this.nuevoStock = 1;
    this.nuevaFotoPreview = null;
    if (this.fotoInputRef) {
      this.fotoInputRef.nativeElement.value = '';
    }

    this.cerrarModalAgregar();
    alert('¡Producto agregado con éxito!');
  }

  // --- Modal administrar productos ---
  abrirModalAdministrar(): void {
    this.modalAdministrarAbierto.set(true);
  }

  cerrarModalAdministrar(): void {
    this.modalAdministrarAbierto.set(false);
  }

  editarStock(nombreProducto: string): void {
    const producto = this.catalogo.productos().find((p) => p.nombre === nombreProducto);
    if (!producto) return;
    const nuevoStock = prompt(`Actualizar stock para "${producto.nombre}":`, String(producto.stock));
    if (nuevoStock !== null && nuevoStock.trim() !== '' && !isNaN(Number(nuevoStock))) {
      this.catalogo.editarStock(nombreProducto, parseInt(nuevoStock, 10));
    }
  }

  eliminarProducto(nombreProducto: string): void {
    if (confirm(`¿Estás seguro de que deseas eliminar permanentemente el producto "${nombreProducto}"?`)) {
      this.catalogo.eliminarProducto(nombreProducto);
      alert('Producto eliminado exitosamente.');
    }
  }
}
