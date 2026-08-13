import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { obterResumo } from '../api'
import Feedback from '../components/Feedback'
import PageHeader from '../components/PageHeader'

export default function DashboardPage() {
  const [quantidade, setQuantidade] = useState<number | null>(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    document.title = 'Início | CESJO SGI'
    obterResumo()
      .then((resumo) => setQuantidade(resumo.entradasAtivas))
      .catch(() => setErro('Não foi possível carregar o resumo da lista.'))
  }, [])

  return (
    <>
      <PageHeader
        eyebrow="Centro-Dia CESJO"
        title="Gestão da lista de espera"
        description="Cadastre beneficiários e acompanhe a ordem cronológica de entrada no serviço."
        action={<Link className="button button-primary" to="/novo-cadastro">Novo cadastro</Link>}
      />

      {erro && <Feedback tipo="erro">{erro}</Feedback>}

      <section className="dashboard-grid" aria-label="Resumo da lista de espera">
        <article className="summary-card">
          <span className="summary-label">Entradas ativas</span>
          <strong>{quantidade === null ? '—' : quantidade}</strong>
          <span>Pessoas aguardando atendimento</span>
        </article>

        <article className="next-step-card">
          <div>
            <p className="eyebrow">Acesso rápido</p>
            <h2>Consulte a fila atual</h2>
            <p>Veja posições, datas de entrada e indicadores do cadastro.</p>
          </div>
          <Link className="button button-secondary" to="/lista-espera">Abrir lista de espera</Link>
        </article>
      </section>

      <Feedback tipo="info">
        Este MVP é local, não possui autenticação e deve utilizar somente dados fictícios.
      </Feedback>
    </>
  )
}

