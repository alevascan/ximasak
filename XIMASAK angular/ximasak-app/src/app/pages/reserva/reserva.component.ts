import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ReservaService } from '../../services/reserva.service';

const MESES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];
const DIAS_SEMANA = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

interface CeldaCalendario {
  dia: number | null;
  seleccionado: boolean;
}

@Component({
  selector: 'app-reserva',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reserva.component.html',
})
export class ReservaComponent implements OnInit {
  // --- Formulario ---
  nombre = '';
  dni = '';
  ruc = '';
  correo = '';
  telefono = '';
  hora = '';
  tipoEquipo = 'reserva_producto';
  falla = '';
  fechaSeleccionada = signal<string>('');
  notificacionVisible = signal(false);
  mensajeNotificacion = '';

  // --- Calendario ---
  fechaActual = signal(new Date());
  diaSeleccionado: Date | null = null;
  diasSemana = DIAS_SEMANA;

  tituloMesAnio = computed(() => {
    const f = this.fechaActual();
    return `${MESES[f.getMonth()]} ${f.getFullYear()}`;
  });

  celdas = computed<CeldaCalendario[]>(() => {
    const f = this.fechaActual();
    const anio = f.getFullYear();
    const mes = f.getMonth();

    const primerDiaMes = new Date(anio, mes, 1).getDay();
    const inicioSemanaAjustado = primerDiaMes === 0 ? 6 : primerDiaMes - 1;
    const diasEnMes = new Date(anio, mes + 1, 0).getDate();

    const resultado: CeldaCalendario[] = [];
    for (let i = 0; i < inicioSemanaAjustado; i++) {
      resultado.push({ dia: null, seleccionado: false });
    }
    for (let i = 1; i <= diasEnMes; i++) {
      const esSeleccionado = !!(
        this.diaSeleccionado &&
        this.diaSeleccionado.getDate() === i &&
        this.diaSeleccionado.getMonth() === mes &&
        this.diaSeleccionado.getFullYear() === anio
      );
      resultado.push({ dia: i, seleccionado: esSeleccionado });
    }
    return resultado;
  });

  // --- Admin ---
  reservasOrdenadas = computed(() => [...this.reservaService.reservas()].reverse());

  constructor(
    public auth: AuthService,
    public reservaService: ReservaService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Si venimos del catálogo con "Reservar Producto", precargamos la descripción.
    const producto = this.route.snapshot.queryParamMap.get('producto');
    if (producto) {
      this.tipoEquipo = 'reserva_producto';
      this.falla = `Deseo reservar el siguiente producto de su catálogo: ${producto}`;
    }
  }

  mesAnterior(): void {
    const f = new Date(this.fechaActual());
    f.setMonth(f.getMonth() - 1);
    this.fechaActual.set(f);
  }

  mesSiguiente(): void {
    const f = new Date(this.fechaActual());
    f.setMonth(f.getMonth() + 1);
    this.fechaActual.set(f);
  }

  seleccionarDia(dia: number | null): void {
    if (dia === null) return;
    const f = this.fechaActual();
    this.diaSeleccionado = new Date(f.getFullYear(), f.getMonth(), dia);
    const mesFormateado = String(f.getMonth() + 1).padStart(2, '0');
    const diaFormateado = String(dia).padStart(2, '0');
    this.fechaSeleccionada.set(`${f.getFullYear()}-${mesFormateado}-${diaFormateado}`);
  }



enviarReserva(): void {

    // Correo y teléfono son obligatorios.
  if (!this.correo.trim() || !this.telefono.trim()) {
    alert('El correo electrónico y el número de teléfono son obligatorios.');
    return;
  }

  // Validar el correo.
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.correo.trim())) {
    alert('Por favor, ingresa un correo electrónico válido.');
    return;
  }

  // Validar el teléfono: entre 7 y 15 dígitos.
  if (!/^\d{7,15}$/.test(this.telefono.trim())) {
    alert('Ingresa un número de teléfono válido, de 7 a 15 dígitos.');
    return;
  }

  if (!this.fechaSeleccionada()) {
    alert('Por favor, seleccione una fecha en el calendario.');
    return;
  }

  if (
    !this.nombre.trim() ||
    !this.correo.trim()||
    !this.correo.trim()||
    !this.dni.trim() ||
    !this.hora ||
    !this.falla.trim()
  ) {
    alert('Por favor, completa todos los campos obligatorios.');
    return;
  }

  if (!/^\d{8}$/.test(this.dni.trim())) {
    alert('El DNI debe tener exactamente 8 dígitos.');
    return;
  }

  // El RUC es opcional, pero debe tener 11 dígitos si se ingresa.
  if (this.ruc.trim() && !/^\d{11}$/.test(this.ruc.trim())) {
    alert('El RUC debe tener exactamente 11 dígitos.');
    return;
  }

  // Validar el correo solamente si se ha ingresado.
  if (
    this.correo.trim() &&
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.correo.trim())
  ) {
    alert('Por favor, ingresa un correo electrónico válido.');
    return;
  }

  const textoTipo: Record<string, string> = {
    mantenimiento: 'Mantenimiento preventivo',
    reparacion_laptop: 'Reparación de laptop',
    formateo_so: 'Formateo e instalación de SO',
    reserva_producto: 'Reserva de producto',
    redes: 'Instalación de Redes',
    otro: 'Otro',
  };

  const tipo = textoTipo[this.tipoEquipo] ?? this.tipoEquipo;

  this.reservaService.agregarReserva({
    nombre: this.nombre.trim(),
    dni: this.dni.trim(),
    ruc: this.ruc.trim(),
    correo: this.correo.trim(),
    telefono: this.telefono.trim(),
    fecha: this.fechaSeleccionada(),
    hora: this.hora,
    tipo: tipo,
    detalle: this.falla.trim(),
  }).subscribe({
    next: () => {
      this.mensajeNotificacion =
        `¡Reserva guardada correctamente para el ${this.fechaSeleccionada()}!`;

      this.notificacionVisible.set(true);
      this.reservaService.cargarReservas();

      setTimeout(() => {
        this.nombre = '';
        this.dni = '';
        this.ruc = '';
        this.correo = '';
        this.telefono = '';
        this.hora = '';
        this.falla = '';
        this.tipoEquipo = 'reserva_producto';
        this.diaSeleccionado = null;
        this.fechaSeleccionada.set('');
        this.notificacionVisible.set(false);
      }, 3000);
    },
    error: (error) => {
      console.error('Error al registrar la reserva:', error);

      alert(
        error.error?.mensaje ??
        error.error?.message ??
        'No se pudo guardar la reserva. Revisa el backend.'
      );
    }
  });
}


  marcarAtendida(id: number): void {
    this.reservaService.marcarAtendida(id);
  }

  eliminarReserva(id: number): void {
    if (confirm('¿Estás seguro de que deseas eliminar permanentemente esta reserva?')) {
      this.reservaService.eliminarReserva(id);
    }
  }
}
