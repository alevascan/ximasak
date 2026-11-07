
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Cliente {
  idCliente: number;
  nombre: string;
  dni: string;
  ruc?: string;
  correo?: string;
  telefono?: string;
  direccion?: string;
  estado: boolean | number;
  fechaRegistro?: string;
}

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './clientes.component.html',
  styleUrls: ['./clientes.component.css']
})
export class ClientesComponent implements OnInit {

  private http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/clientes';

  clientes: Cliente[] = [];
  filtro = '';
  cargando = false;
  error = '';
  clienteSeleccionado: Cliente | null = null;

  ngOnInit(): void {
    this.listarClientes();
  }

  listarClientes(): void {
    this.cargando = true;
    this.error = '';

    this.http.get<Cliente[]>(this.apiUrl).subscribe({
      next: (respuesta) => {
        this.clientes = respuesta;
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al cargar clientes:', err);
        this.error =
          'No se pudieron cargar los clientes. Verifica que el backend esté funcionando.';
        this.cargando = false;
      }
    });
  }

  get clientesFiltrados(): Cliente[] {
    const termino = this.filtro.trim().toLowerCase();

    if (!termino) {
      return this.clientes;
    }

    return this.clientes.filter(cliente =>
      (cliente.nombre ?? '').toLowerCase().includes(termino) ||
      (cliente.dni ?? '').toLowerCase().includes(termino) ||
      (cliente.correo ?? '').toLowerCase().includes(termino) ||
      (cliente.ruc ?? '').toLowerCase().includes(termino)
    );
  }
  get totalActivos(): number {
  return this.clientes.filter(cliente => this.estaActivo(cliente)).length;
}

  estaActivo(cliente: Cliente): boolean {
    return cliente.estado === true ||
           cliente.estado === 1 ||
           String(cliente.estado).toLowerCase() === 'activo';
  }

  verCliente(cliente: Cliente): void {
    this.clienteSeleccionado = cliente;
  }

  cerrarDetalle(): void {
    this.clienteSeleccionado = null;
  }
}
