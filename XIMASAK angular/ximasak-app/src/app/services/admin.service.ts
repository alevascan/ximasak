import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Envía el token JWT para que Spring Security autorice crear/editar/borrar en la BD
  private getHeaders(): { headers: HttpHeaders } {
    const token = localStorage.getItem('token') || '';
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      })
    };
  }


  // --- USUARIOS Y CLIENTES REGISTRADOS EN BD ---
  getUsuarios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/usuarios`, this.getHeaders());
  }

  crearUsuario(usuario: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/usuarios`, usuario, this.getHeaders());
  }

  actualizarUsuario(id: number, usuario: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/usuarios/${id}`, usuario, this.getHeaders());
  }

  eliminarUsuario(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/usuarios/${id}`, this.getHeaders());
  }


  // --- PRODUCTOS ---
  getProductos(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/productos`, this.getHeaders());
  }
  crearProducto(producto: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/productos`, producto, this.getHeaders());
  }
  actualizarProducto(id: number, producto: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/productos/${id}`, producto, this.getHeaders());
  }
  eliminarProducto(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/productos/${id}`, this.getHeaders());
  }

  // --- VENTAS Y RESERVAS ---
  getVentas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/ventas`, this.getHeaders());
  }
  getReservas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/reservas`, this.getHeaders());
  }
}