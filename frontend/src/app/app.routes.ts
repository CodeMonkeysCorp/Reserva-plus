import { Routes } from '@angular/router';
import { BloqueiosComponent } from './components/bloqueios/bloqueios.component';
import { EspacosComponent } from './components/espacos/espacos.component';
import { HistoricoReservasComponent } from './components/historico-reservas/historico-reservas.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ReservasComponent } from './components/reservas/reservas.component';
import { UsuariosComponent } from './components/usuarios/usuarios.component';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';
import { userGuard } from './guards/user.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'home', component: HomeComponent, canActivate: [authGuard] },
  { path: 'espacos', component: EspacosComponent, canActivate: [authGuard, adminGuard] },
  { path: 'reservas', component: ReservasComponent, canActivate: [authGuard] },
  { path: 'minhas-reservas', component: HistoricoReservasComponent, canActivate: [authGuard, userGuard], data: { view: 'user' } },
  { path: 'gerenciar-reservas', component: HistoricoReservasComponent, canActivate: [authGuard, adminGuard], data: { view: 'admin' } },
  { path: 'usuarios', component: UsuariosComponent, canActivate: [authGuard, adminGuard] },
  { path: 'bloqueios', component: BloqueiosComponent, canActivate: [authGuard, adminGuard] },
  { path: '**', redirectTo: 'home' }
];
