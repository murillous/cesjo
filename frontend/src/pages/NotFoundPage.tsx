import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="empty-state not-found">
      <span className="error-code">404</span>
      <h1>Página não encontrada</h1>
      <p>O endereço informado não pertence ao MVP do CESJO SGI.</p>
      <Link className="button button-primary" to="/">Voltar ao início</Link>
    </div>
  )
}
