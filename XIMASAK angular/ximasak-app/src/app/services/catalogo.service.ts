import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Producto {
  id?: number;
  nombre: string;
  precio: number;
  marca: string;
  categoria: string;
  stock: number;
  foto: string;
}

const CLAVE_STORAGE = 'catalogo_ximasak';

const PRODUCTOS_POR_DEFECTO: Producto[] = [
  { nombre: 'Memoria RAM 16GB', precio: 120.0, categoria: 'memorias', marca: 'Kingston', stock: 5, foto: 'assets/fotos/ram.jpeg' },
  { nombre: 'Router Wi-Fi Dual Band', precio: 180.0, categoria: 'redes', marca: 'tplink', stock: 10, foto: 'assets/fotos/router.jpeg' },
  { nombre: 'Case Gamer con RGB', precio: 250.0, categoria: 'cases', marca: 'Genérico', stock: 3, foto: 'assets/fotos/case.jpeg' },
  { nombre: 'Mouse Óptico Inalámbrico', precio: 45.0, categoria: 'perifericos', marca: 'Generico', stock: 15, foto: 'assets/fotos/mouse.jpeg' },
  { nombre: 'Teclado Mecánico RGB', precio: 190.0, categoria: 'perifericos', marca: 'Antryx', stock: 8, foto: 'assets/fotos/tecla.jpeg' },
  { nombre: 'Disco Duro SSD 500GB', precio: 150.0, categoria: 'memorias', marca: 'Western Digital', stock: 20, foto: 'assets/fotos/duro.jpeg' },
];

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/productos';

  productos = signal<Producto[]>(this.cargarDesdeStorage());

  // Categorías únicas derivadas automáticamente de los productos actuales
  categorias = computed(() =>
    [...new Set(this.productos().map((p) => (p.categoria || 'general').toLowerCase()))]
  );

  constructor() {
    this.cargarDesdeBackend();
  }

  cargarDesdeBackend(): void {
    this.http.get<any[]>(this.apiUrl).subscribe({
      next: (listaApi) => {
        if (listaApi && listaApi.length > 0) {
          const mapeados: Producto[] = listaApi.map((p) => {
            const catNombre = p.categoria?.nombre || p.categoria || 'Accesorios';
            const marcaNombre = p.marca?.nombre || p.marca || 'Ximasak';
            let foto = p.imagen || 'assets/fotos/repuesto.jpeg';
            if (p.nombre.toLowerCase().includes('ram')) foto = 'assets/fotos/ram.jpeg';
            else if (p.nombre.toLowerCase().includes('mouse')) foto = 'assets/fotos/mouse.jpeg';
            else if (p.nombre.toLowerCase().includes('ssd') || p.nombre.toLowerCase().includes('disco')) foto = 'assets/fotos/duro.jpeg';
            else if (p.nombre.toLowerCase().includes('router')) foto = 'assets/fotos/router.jpeg';
            else if (p.nombre.toLowerCase().includes('case')) foto = 'assets/fotos/case.jpeg';
            else if (p.nombre.toLowerCase().includes('teclado')) foto = 'assets/fotos/tecla.jpeg';

            return {
              id: p.idProducto,
              nombre: p.nombre,
              precio: Number(p.precioUnitario || p.precio || 0),
              marca: marcaNombre,
              categoria: catNombre,
              stock: Number(p.stock || 0),
              foto: foto
            };
          });

          this.productos.set(mapeados);
          localStorage.setItem(CLAVE_STORAGE, JSON.stringify(mapeados));
        }
      },
      error: () => {
        // Fallback a localStorage si el backend no responde
      }
    });
  }

  private cargarDesdeStorage(): Producto[] {
    const guardado = localStorage.getItem(CLAVE_STORAGE);
    if (guardado) {
      try {
        return JSON.parse(guardado) as Producto[];
      } catch {
        // Fallback
      }
    }
    return PRODUCTOS_POR_DEFECTO;
  }

  private guardar(lista: Producto[]): void {
    this.productos.set(lista);
    localStorage.setItem(CLAVE_STORAGE, JSON.stringify(lista));
  }

  agregarProducto(producto: Producto): void {
    this.guardar([...this.productos(), producto]);
  }

  editarStock(nombre: string, nuevoStock: number): void {
    const actualizado = this.productos().map((p) =>
      p.nombre === nombre ? { ...p, stock: nuevoStock } : p
    );
    this.guardar(actualizado);
  }

  eliminarProducto(nombre: string): void {
    this.guardar(this.productos().filter((p) => p.nombre !== nombre));
  }
}
