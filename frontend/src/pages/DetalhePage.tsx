import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { obterBeneficiario } from '../api'
import Feedback from '../components/Feedback'
import PageHeader from '../components/PageHeader'
import type { BeneficiarioDetalhe } from '../types'

const data = (valor: string) => new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(`${valor}T00:00:00Z`))
const dataHora = (valor: string) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor))
const telefone = (valor: string) => valor.length === 11 ? `(${valor.slice(0, 2)}) ${valor.slice(2, 7)}-${valor.slice(7)}` : `(${valor.slice(0, 2)}) ${valor.slice(2, 6)}-${valor.slice(6)}`
const tamanho = (bytes: number) => bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`

export default function DetalhePage() {
  const { id = '' } = useParams()
  const location = useLocation()
  const cadastroConcluido = Boolean((location.state as { cadastroConcluido?: boolean } | null)?.cadastroConcluido)
  const [item, setItem] = useState<BeneficiarioDetalhe | null>(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    document.title = 'Detalhes | CESJO SGI'
    obterBeneficiario(id).then(setItem).catch(() => setErro('Beneficiário não encontrado na lista de espera.'))
  }, [id])

  if (erro) return <><PageHeader title="Detalhes do beneficiário" /><Feedback tipo="erro">{erro}</Feedback><Link className="button button-secondary" to="/lista-espera">Voltar para a lista</Link></>
  if (!item) return <div className="loading" role="status">Carregando detalhes…</div>

  return (
    <>
      <PageHeader
        eyebrow={`Posição atual: ${item.posicao}º`}
        title={item.nome}
        description={`Entrada registrada em ${data(item.dataEntrada)}`}
        action={<Link className="button button-secondary" to="/lista-espera">Voltar para a lista</Link>}
      />
      {cadastroConcluido && <Feedback tipo="sucesso">Cadastro concluído e incluído na lista de espera.</Feedback>}

      <div className="detail-grid">
        <section className="detail-card detail-highlight">
          <p className="eyebrow">Fila atual</p>
          <strong className="detail-position">{item.posicao}º</strong>
          <span>posição cronológica</span>
        </section>
        <section className="detail-card">
          <h2>Dados pessoais</h2>
          <dl>
            <div><dt>Data de nascimento</dt><dd>{data(item.dataNascimento)}</dd></div>
            <div><dt>Responsável</dt><dd>{item.responsavel}</dd></div>
            <div><dt>Telefone</dt><dd>{telefone(item.telefone)}</dd></div>
            <div><dt>Tipo de deficiência</dt><dd>{item.tipoDeficiencia}</dd></div>
            <div><dt>Origem</dt><dd>{item.origemEncaminhamento}</dd></div>
          </dl>
        </section>
        <section className="detail-card">
          <h2>Indicadores sociais</h2>
          <div className="indicator-list">
            <span className={`indicator ${item.beneficiarioBpc ? 'positive' : ''}`}><strong>BPC</strong>{item.beneficiarioBpc ? 'Sim' : 'Não'}</span>
            <span className={`indicator ${item.inscritoCadunico ? 'positive' : ''}`}><strong>CadÚnico</strong>{item.inscritoCadunico ? 'Sim' : 'Não'}</span>
          </div>
          <p className="helper-text">Estes indicadores não alteram automaticamente a posição no MVP.</p>
        </section>
        <section className="detail-card detail-documents">
          <h2>Documentos</h2>
          <div className="document-row"><div><strong>Laudo médico</strong><span>{item.laudoMedico.nomeOriginal}</span></div><span>{tamanho(item.laudoMedico.tamanho)}</span></div>
          <div className="document-row"><div><strong>Comprovante de residência</strong><span>{item.comprovanteResidencia.nomeOriginal}</span></div><span>{tamanho(item.comprovanteResidencia.tamanho)}</span></div>
          <p className="helper-text">Somente os metadados são exibidos; o caminho físico permanece protegido.</p>
        </section>
      </div>
      <p className="record-meta">Registro criado em {dataHora(item.dataCriacao)}</p>
    </>
  )
}

