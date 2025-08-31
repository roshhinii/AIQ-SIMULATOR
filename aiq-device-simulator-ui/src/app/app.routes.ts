import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AddSimulatorComponent } from './add-simulator/add-simulator.component';
import { LoginComponent } from './login/login.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { 
    path: 'login', 
    component: LoginComponent
  },
  { 
    path: 'dashboard', 
    component: DashboardComponent
  },
  { 
    path: 'add-simulator', 
    component: AddSimulatorComponent
  },
  { path: '**', redirectTo: '/login' }
];
