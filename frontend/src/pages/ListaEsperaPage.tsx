import { FormEvent, useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listarEspera } from '../api'
import Feedback from '../components/Feedback'
import PageHeader from '../components/PageHeader'
import type { ListaEsperaItem } from '../types'

function formatarData(data: string) {
  return new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(`${data}T00:00:00Z`))
}

export default function ListaEsperaPage() {
  const [itens, setItens] = useState<ListaEsperaItem[]>([])
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const carregar = useCallback(async (termo = '') => {
    setCarregando(true)
    setErro('')
    try {
      setItens(await listarEspera(termo))
    } catch {
      setErro('Não foi possível carregar a lista de espera.')
    } finally {
      setCarregando(false)
    }
  }, [])

  useEffect(() => {
    document.title = 'Lista de espera | CESJO SGI'
    void carregar()
  }, [carregar])

  function pesquisar(event: FormEvent) {
    event.preventDefault()
    void carregar(busca)
  }

  return (
    <>
      <PageHeader
        eyebrow="RF002"
        title="Lista de espera"
        description="A posição considera somente a data de entrada e o identificador como desempate."
        action={<Link className="button button-primary" to="/novo-cadastro">Novo cadastro</Link>}
      />

      <form className="search-bar" role="search" onSubmit={pesquisar}>
        <label htmlFor="busca">Buscar por nome</label>
        <div>
          <input
            id="busca"
            type="search"
            value={busca}
            onChange={(event) => setBusca(event.target.value)}
            placeholder="Digite o nome do beneficiário"
          />
          <button className="button button-secondary" type="submit">Buscar</button>
          {busca && (
            <button className="button button-quiet" type="button" onClick={() => { setBusca(''); void carregar() }}>
              Limpar
            </button>
          )}
        </div>
      </form>

      {erro && <Feedback tipo="erro">{erro}</Feedback>}
      {carregando && <div className="loading" role="status">Carregando lista…</div>}

      {!carregando && !erro && itens.length === 0 && (
        <div className="empty-state">
          <h2>Nenhuma entrada encontrada</h2>
          <p>{busca ? 'Tente buscar por outro nome.' : 'Cadastre o primeiro beneficiário para iniciar a fila.'}</p>
          {!busca && <Link className="button button-primary" to="/novo-cadastro">Novo cadastro</Link>}
        </div>
      )}

      {!carregando && itens.length > 0 && (
        <div className="table-region" role="region" aria-label="Entradas ativas da lista de espera" tabIndex={0}>
          <table>
            <thead>
              <tr>
                <th scope="col">Posição</th>
                <th scope="col">Beneficiário</th>
                <th scope="col">Deficiência</th>
                <th scope="col">Entrada</th>
                <th scope="col">BPC</th>
                <th scope="col">CadÚnico</th>
                <th scope="col">Origem</th>
                <th scope="col"><span className="sr-only">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {itens.map((item) => (
                <tr key={item.entradaId}>
                  <td><span className="position-badge">{item.posicao}º</span></td>
                  <td><strong>{item.nome}</strong></td>
                  <td>{item.tipoDeficiencia}</td>
                  <td>{formatarData(item.dataEntrada)}</td>
                  <td><span className={`status-pill ${item.beneficiarioBpc ? 'yes' : 'no'}`}>{item.beneficiarioBpc ? 'Sim' : 'Não'}</span></td>
                  <td><span className={`status-pill ${item.inscritoCadunico ? 'yes' : 'no'}`}>{item.inscritoCadunico ? 'Sim' : 'Não'}</span></td>
                  <td>{item.origemEncaminhamento}</td>
                  <td><Link className="table-link" to={`/beneficiarios/${item.beneficiarioId}`}>Ver detalhes</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
