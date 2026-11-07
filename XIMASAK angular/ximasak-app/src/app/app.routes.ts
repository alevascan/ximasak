import { Routes } from '@angular/router';
import { InicioComponent } from './pages/inicio/inicio.component';
import { ClientesComponent } from './pages/clientes/clientes.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { ReservaComponent } from './pages/reserva/reserva.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { ResetearPasswordComponent } from './pages/resetear-password/resetear-password.component';

export const routes: Routes = [
  { path: '', component: InicioComponent },
  { path: 'clientes', component: ClientesComponent },
  { path: 'catalogo', component: CatalogoComponent },
  { path: 'reserva', component: ReservaComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'resetear-password', component: ResetearPasswordComponent },
  { path: '**', redirectTo: '' },
];
