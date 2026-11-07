import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-resetear-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './resetear-password.component.html',
  styleUrls: ['./resetear-password.component.css']
})
export class ResetearPasswordComponent implements OnInit {
  token = '';
  nuevaPassword = '';
  confirmarPassword = '';
  mensajeExito = '';
  mensajeError = '';
  cargando = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'] || '';
      if (!this.token) {
        this.mensajeError = 'Enlace inválido o sin token de recuperación.';
      }
    });
  }

  cambiarPassword(): void {
    this.mensajeError = '';
    this.mensajeExito = '';

    if (!this.token) {
      this.mensajeError = 'No se encontró un token válido en la URL.';
      return;
    }

    if (!this.nuevaPassword || this.nuevaPassword.trim().length < 4) {
      this.mensajeError = 'La contraseña debe tener al menos 4 caracteres.';
      return;
    }

    if (this.nuevaPassword !== this.confirmarPassword) {
      this.mensajeError = 'Las contraseñas no coinciden.';
      return;
    }

    this.cargando = true;
    const payload = {
      token: this.token,
      nuevaPassword: this.nuevaPassword.trim()
    };

    this.http.post<any>('http://localhost:8080/api/auth/resetear-password', payload)
      .subscribe({
        next: (res) => {
          this.cargando = false;
          this.mensajeExito = '¡Tu contraseña ha sido actualizada con éxito! Redirigiendo a inicio...';
          setTimeout(() => {
            this.router.navigate(['/']);
          }, 2500);
        },
        error: (err) => {
          this.cargando = false;
          this.mensajeError = err.error?.error || err.error?.mensaje || 'Error al restablecer la contraseña. El enlace puede haber expirado.';
        }
      });
  }
}
