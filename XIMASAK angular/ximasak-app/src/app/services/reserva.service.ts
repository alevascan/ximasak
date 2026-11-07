import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Reserva {
id: number;
nombre: string;
dni: string;
fecha: string;
hora: string;
tipo: string;
detalle: string;
estado: 'Pendiente' | 'Atendida';
}

interface ReservaApi {
idReserva?: number;
id?: number;
nombre?: string;
dni?: string;
fecha: string;
hora: string;
tipo?: string;
detalle?: string;
descripcion?: string;
estado: string;
}


export interface NuevaReserva {
  nombre: string;
  dni: string;
  ruc: string;
  correo: string;
  telefono: string;
  fecha: string;
  hora: string;
  tipo: string;
  detalle: string;
}


@Injectable({ providedIn: 'root' })
export class ReservaService {
private readonly apiUrl = 'http://localhost:8080/api/reservas';

reservas = signal<Reserva[]>([]);

constructor(private http: HttpClient) {
this.cargarReservas();
}

private convertirReserva(r: ReservaApi): Reserva {
return {
id: r.idReserva ?? r.id ?? 0,
nombre: r.nombre ?? '',
dni: r.dni ?? '',
fecha: r.fecha,
hora: r.hora,
tipo: r.tipo ?? '',
detalle: r.detalle ?? r.descripcion ?? '',
estado:
r.estado?.toLowerCase() === 'atendida'
? 'Atendida'
: 'Pendiente',
};
}

cargarReservas(): void {
this.http.get<ReservaApi[]>(this.apiUrl).subscribe({
next: (datos) => {
this.reservas.set(datos.map((r) => this.convertirReserva(r)));
},
error: (error) => {
console.error('Error al cargar las reservas:', error);
},
});
}



agregarReserva(datos: NuevaReserva) {
  const payload = {
    nombre: datos.nombre,
    dni: datos.dni,
    ruc: datos.ruc,
    correo: datos.correo,
    telefono: datos.telefono,
    fecha: datos.fecha,
    hora: datos.hora,
    tipo: datos.tipo,
    detalle: datos.detalle,
  };

  return this.http.post(this.apiUrl, payload);
}

marcarAtendida(id: number): void {
  this.http.patch(`${this.apiUrl}/${id}/estado`, {
    estado: 'ATENDIDA',
  }).subscribe({
    next: () => this.cargarReservas(),
    error: (error) => {
      console.error('Error al actualizar la reserva:', error);
      alert('No se pudo actualizar el estado de la reserva.');
    },
  });
}

eliminarReserva(id: number): void {
  this.http.delete(`${this.apiUrl}/${id}`).subscribe({
    next: () => this.cargarReservas(),
    error: (error) => {
      console.error('Error al eliminar la reserva:', error);
      alert('No se pudo eliminar la reserva.');
    },
  });
}
  
}