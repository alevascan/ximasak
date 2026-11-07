import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  mostrarModalAuth = false;
  modoRegistro = false;
  usuarioLogueado = false;
  mensajeError = '';

  loginData = { correo: '', password: '' };
  registroData = { dni: '', nombre: '', correo: '', password: '' };

  

  abrirModalLogin() {
    this.mostrarModalAuth = true;
    this.mensajeError = '';
  }

  cerrarModal() {
    this.mostrarModalAuth = false;
  }

  alternarContraste() {
    document.body.classList.toggle('alto-contraste');
  }

  // Agrega Router en tu constructor de HeaderComponent:
  constructor(private authService: AuthService, private router: Router) {}

  hacerLogin() {
    this.authService.login(this.loginData).subscribe({
      next: (res: any) => {
        // Guardamos los datos en la memoria del navegador
        localStorage.setItem('token', res.token);
        localStorage.setItem('rol', res.rol);
        localStorage.setItem('nombreUsuario', res.nombre);
        
        this.usuarioLogueado = true;
        this.cerrarModal();

        // Transformamos el rol a mayúsculas para evitar errores (Ej: 'Administrador' -> 'ADMINISTRADOR')
        const rolAsignado = String(res.rol).toUpperCase();

        // Validamos todas las posibles formas en las que lo hayas escrito en tu BD
        if (rolAsignado === 'ADMIN' || rolAsignado === 'ADMINISTRADOR' || rolAsignado === 'VENDEDOR' || rolAsignado === 'TECNICO') {
          // Si es personal, lo mandamos al dashboard y forzamos la recarga de la vista
          this.router.navigate(['/dashboard']).then(() => {
            window.location.reload(); 
          });
        } else {
          // Si es cliente, se queda en la tienda
          alert(`¡Bienvenido, ${res.nombre}!`);
          window.location.reload();
        }
      },
      error: () => {
        this.mensajeError = 'Credenciales incorrectas';
      }
    });
  }

  hacerRegistro() {
    if(!this.registroData.dni || !this.registroData.correo || !this.registroData.password) {
      this.mensajeError = 'Por favor completa todos los campos';
      return;
  }

    this.authService.registrarCliente(this.registroData).subscribe({
      next: (res: any) => {
        alert('Cuenta creada con éxito. Ahora puedes iniciar sesión.');
        this.modoRegistro = false; 
        this.mensajeError = '';
      },
      error: (err: any) => {
        this.mensajeError = 'Error al registrar el usuario';
      }
    });
  }

  cerrarSesion() {
    localStorage.removeItem('token');
    localStorage.removeItem('rol');
    this.usuarioLogueado = false;
    alert('Sesión cerrada');
    window.location.reload();
  }
}