import { NavLink, Route, Routes } from 'react-router-dom'
import CadastroPage from './pages/CadastroPage'
import DashboardPage from './pages/DashboardPage'
import DetalhePage from './pages/DetalhePage'
import ListaEsperaPage from './pages/ListaEsperaPage'
import NotFoundPage from './pages/NotFoundPage'

function App() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="header-inner">
          <NavLink className="brand" to="/" aria-label="CESJO SGI - Início">
            <img src="/brand/logo-cesjo.png" alt="Pobres Servos CESJO" />
            <span>
              <strong>SGI</strong>
              <small>Lista de espera</small>
            </span>
          </NavLink>
          <nav aria-label="Navegação principal">
            <NavLink to="/" end>Início</NavLink>
            <NavLink to="/lista-espera">Lista de espera</NavLink>
            <NavLink className="nav-action" to="/novo-cadastro">Novo cadastro</NavLink>
          </nav>
        </div>
      </header>

      <main id="conteudo" className="page-container">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/lista-espera" element={<ListaEsperaPage />} />
          <Route path="/novo-cadastro" element={<CadastroPage />} />
          <Route path="/beneficiarios/:id" element={<DetalhePage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>

      <footer>
        <div className="footer-inner">
          <span>CESJO SGI · MVP local</span>
          <span>Utilize somente dados fictícios</span>
        </div>
      </footer>
    </div>
  )
}

export default App

