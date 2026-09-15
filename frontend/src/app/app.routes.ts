import { OperacaoFormComponent } from './pages/operacao-form/operacao-form.component';
import { OperacaoListComponent } from './pages/operacao-list/operacao-list.component';
import {CorretoraFormComponent} from './pages/corretora-form/corretora-form.component';
import {Routes} from '@angular/router';
import {HomeComponent} from './pages/home/home.component';
import {CorretoraListComponent} from './pages/corretora-list/corretora-list.component';
import {AcaoFormComponent} from './pages/acao-form/acao-form.component';
import {AcaoListComponent} from './pages/acao-list/acao-list.component';
import { CarteiraComponent } from './pages/carteira/carteira.component';

export const routes: Routes = [

  { path: '', component: HomeComponent },

  { path: 'carteira', component: CarteiraComponent },

  { path: 'corretoras/nova', component: CorretoraFormComponent },
  { path: 'corretoras', component: CorretoraListComponent },

  { path: 'acoes/nova', component: AcaoFormComponent },
  { path: 'acoes', component: AcaoListComponent },

  { path: 'operacoes/nova', component: OperacaoFormComponent },
  { path: 'operacoes', component: OperacaoListComponent }
];
