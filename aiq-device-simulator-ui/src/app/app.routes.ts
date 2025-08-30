import { Routes } from '@angular/router';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AddSimulatorComponent } from './add-simulator/add-simulator.component';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { 
    path: 'dashboard', 
    component: DashboardComponent
  },
  { 
    path: 'add-simulator', 
    component: AddSimulatorComponent
  },
  { path: '**', redirectTo: '/dashboard' }
];
