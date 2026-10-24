import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth'; 

  constructor(private http: HttpClient) { }

  login(credenciales: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credenciales);
  }

  registrarCliente(usuario: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/registro`, usuario);
  }

  registrarVendedor(usuario: any, tokenAdmin: string): Observable<any> {
    return this.http.post(`http://localhost:8080/api/usuarios/crear-vendedor`, usuario, {
      headers: { 'Authorization': `Bearer ${tokenAdmin}` }
    });
  }

  // FUNCIÓN AGREGADA: Verifica si el rol guardado es de Administrador
  esAdminActivo(): boolean {
    return localStorage.getItem('rol') === 'ADMIN';
  }
  
  // FUNCIÓN EXTRA: Por si la necesitas para verificar si alguien inició sesión
  estaLogueado(): boolean {
    return !!localStorage.getItem('token');
  }
}