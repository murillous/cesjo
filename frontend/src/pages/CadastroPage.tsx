import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { cadastrarBeneficiario, ErroApi } from '../api'
import Feedback from '../components/Feedback'
import PageHeader from '../components/PageHeader'
import type { CadastroDados } from '../types'

const hoje = new Date().toISOString().slice(0, 10)
const tiposAceitos = ['application/pdf', 'image/jpeg', 'image/png']
const tamanhoMaximo = 5 * 1024 * 1024
const formatoTelefone = /^(?:\d{10,11}|\(\d{2}\) ?\d{4,5}-\d{4})$/

const estadoInicial: CadastroDados = {
  nome: '',
  dataNascimento: '',
  responsavel: '',
  telefone: '',
  tipoDeficiencia: '',
  beneficiarioBpc: false,
  inscritoCadunico: false,
  origemEncaminhamento: '',
  dataEntrada: hoje,
}

function idadeNaData(nascimento: string, entrada: string) {
  const inicio = new Date(`${nascimento}T00:00:00`)
  const fim = new Date(`${entrada}T00:00:00`)
  let idade = fim.getFullYear() - inicio.getFullYear()
  if (fim.getMonth() < inicio.getMonth() || (fim.getMonth() === inicio.getMonth() && fim.getDate() < inicio.getDate())) {
    idade -= 1
  }
  return idade
}

function validarArquivo(arquivo: File | null, nome: string) {
  if (!arquivo) return `${nome} é obrigatório.`
  if (!tiposAceitos.includes(arquivo.type)) return `${nome} deve ser PDF, JPEG ou PNG.`
  if (arquivo.size > tamanhoMaximo) return `${nome} deve ter no máximo 5 MB.`
  return ''
}

export default function CadastroPage() {
  const navigate = useNavigate()
  const [dados, setDados] = useState(estadoInicial)
  const [laudo, setLaudo] = useState<File | null>(null)
  const [comprovante, setComprovante] = useState<File | null>(null)
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [enviado, setEnviado] = useState(false)

  const alterado = useMemo(
    () => JSON.stringify(dados) !== JSON.stringify(estadoInicial) || laudo !== null || comprovante !== null,
    [dados, laudo, comprovante],
  )

  useEffect(() => {
    document.title = 'Novo cadastro | CESJO SGI'
    const impedirSaida = (event: BeforeUnloadEvent) => {
      if (alterado && !enviado) event.preventDefault()
    }
    window.addEventListener('beforeunload', impedirSaida)
    return () => window.removeEventListener('beforeunload', impedirSaida)
  }, [alterado, enviado])

  useEffect(() => {
    const confirmarNavegacaoInterna = (event: MouseEvent) => {
      if (!alterado || enviado || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey) return
      if (!(event.target instanceof Element)) return
      const link = event.target.closest('a[href]')
      if (link && !window.confirm('Descartar os dados preenchidos?')) event.preventDefault()
    }
    document.addEventListener('click', confirmarNavegacaoInterna, true)
    return () => document.removeEventListener('click', confirmarNavegacaoInterna, true)
  }, [alterado, enviado])

  function atualizarCampo(event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    const { name, value, type } = event.target
    const checked = type === 'checkbox' ? (event.target as HTMLInputElement).checked : undefined
    setDados((atual) => ({ ...atual, [name]: checked ?? value }))
    setErros((atuais) => ({ ...atuais, [name]: '' }))
  }

  function validarFormulario() {
    const novosErros: Record<string, string> = {}
    if (!dados.nome.trim()) novosErros.nome = 'Informe o nome completo.'
    if (!dados.dataNascimento) novosErros.dataNascimento = 'Informe a data de nascimento.'
    if (!dados.responsavel.trim()) novosErros.responsavel = 'Informe o nome do responsável.'
    if (!dados.telefone.trim()) novosErros.telefone = 'Informe o telefone.'
    if (!dados.tipoDeficiencia) novosErros.tipoDeficiencia = 'Selecione o tipo de deficiência.'
    if (!dados.origemEncaminhamento) novosErros.origemEncaminhamento = 'Selecione a origem.'
    if (!dados.dataEntrada) novosErros.dataEntrada = 'Informe a data de entrada.'

    if (dados.dataNascimento && dados.dataEntrada) {
      const idade = idadeNaData(dados.dataNascimento, dados.dataEntrada)
      if (idade < 18 || idade > 59) novosErros.dataNascimento = 'A idade na entrada deve estar entre 18 e 59 anos.'
    }
    if (dados.dataNascimento > hoje) novosErros.dataNascimento = 'A data de nascimento não pode estar no futuro.'
    if (dados.telefone && !formatoTelefone.test(dados.telefone.trim())) {
      novosErros.telefone = 'Use 10 ou 11 dígitos com DDD, com ou sem máscara.'
    }
    const erroLaudo = validarArquivo(laudo, 'Laudo médico')
    const erroComprovante = validarArquivo(comprovante, 'Comprovante de residência')
    if (erroLaudo) novosErros.laudo = erroLaudo
    if (erroComprovante) novosErros.comprovante = erroComprovante

    setErros(novosErros)
    return Object.keys(novosErros).length === 0
  }

  async function enviar(event: FormEvent) {
    event.preventDefault()
    setErroGeral('')
    if (!validarFormulario() || !laudo || !comprovante) {
      document.getElementById('form-feedback')?.focus()
      return
    }

    setEnviando(true)
    try {
      const resultado = await cadastrarBeneficiario(dados, laudo, comprovante)
      setEnviado(true)
      navigate(`/beneficiarios/${resultado.id}`, { state: { cadastroConcluido: true } })
    } catch (erro) {
      if (erro instanceof ErroApi) {
        setErroGeral(erro.message)
        setErros((atuais) => ({ ...atuais, ...erro.campos }))
      } else {
        setErroGeral('Não foi possível concluir o cadastro.')
      }
      document.getElementById('form-feedback')?.focus()
    } finally {
      setEnviando(false)
    }
  }

  function cancelar() {
    if (!alterado || window.confirm('Descartar os dados preenchidos?')) navigate('/lista-espera')
  }

  return (
    <>
      <PageHeader
        eyebrow="RF001"
        title="Novo cadastro"
        description="Cadastre o beneficiário e sua entrada na lista de espera em uma única operação."
      />

      <div id="form-feedback" tabIndex={-1}>
        {erroGeral && <Feedback tipo="erro">{erroGeral}</Feedback>}
        {Object.keys(erros).length > 0 && !erroGeral && (
          <Feedback tipo="erro">Revise os campos destacados antes de continuar.</Feedback>
        )}
      </div>

      <form className="registration-form" onSubmit={enviar} noValidate>
        <section className="form-section">
          <div className="section-heading">
            <span>1</span>
            <div><h2>Dados do beneficiário</h2><p>Campos marcados com * são obrigatórios.</p></div>
          </div>
          <div className="form-grid">
            <div className="field field-wide">
              <label htmlFor="nome">Nome completo *</label>
              <input id="nome" name="nome" value={dados.nome} onChange={atualizarCampo} maxLength={150} aria-invalid={!!erros.nome} aria-describedby={erros.nome ? 'erro-nome' : undefined} />
              {erros.nome && <span className="field-error" id="erro-nome">{erros.nome}</span>}
            </div>
            <div className="field">
              <label htmlFor="dataNascimento">Data de nascimento *</label>
              <input id="dataNascimento" name="dataNascimento" type="date" max={hoje} value={dados.dataNascimento} onChange={atualizarCampo} aria-invalid={!!erros.dataNascimento} aria-describedby={erros.dataNascimento ? 'erro-nascimento' : undefined} />
              {erros.dataNascimento && <span className="field-error" id="erro-nascimento">{erros.dataNascimento}</span>}
            </div>
            <div className="field">
              <label htmlFor="telefone">Telefone *</label>
              <input id="telefone" name="telefone" inputMode="tel" placeholder="(98) 99999-9999" value={dados.telefone} onChange={atualizarCampo} aria-invalid={!!erros.telefone} aria-describedby={erros.telefone ? 'erro-telefone' : undefined} />
              {erros.telefone && <span className="field-error" id="erro-telefone">{erros.telefone}</span>}
            </div>
            <div className="field field-wide">
              <label htmlFor="responsavel">Nome completo do responsável *</label>
              <input id="responsavel" name="responsavel" value={dados.responsavel} onChange={atualizarCampo} maxLength={150} aria-invalid={!!erros.responsavel} aria-describedby={erros.responsavel ? 'erro-responsavel' : undefined} />
              {erros.responsavel && <span className="field-error" id="erro-responsavel">{erros.responsavel}</span>}
            </div>
            <div className="field">
              <label htmlFor="tipoDeficiencia">Tipo de deficiência *</label>
              <select id="tipoDeficiencia" name="tipoDeficiencia" value={dados.tipoDeficiencia} onChange={atualizarCampo} aria-invalid={!!erros.tipoDeficiencia} aria-describedby={erros.tipoDeficiencia ? 'erro-tipo-deficiencia' : undefined}>
                <option value="">Selecione</option>
                <option value="SENSORIAL">Sensorial</option><option value="FISICA">Física</option>
                <option value="TEA">TEA</option><option value="INTELECTUAL">Intelectual</option>
                <option value="MULTIPLA">Múltipla</option><option value="T21">T21</option>
              </select>
              {erros.tipoDeficiencia && <span className="field-error" id="erro-tipo-deficiencia">{erros.tipoDeficiencia}</span>}
            </div>
            <div className="field">
              <label htmlFor="origemEncaminhamento">Origem do encaminhamento *</label>
              <select id="origemEncaminhamento" name="origemEncaminhamento" value={dados.origemEncaminhamento} onChange={atualizarCampo} aria-invalid={!!erros.origemEncaminhamento} aria-describedby={erros.origemEncaminhamento ? 'erro-origem' : undefined}>
                <option value="">Selecione</option>
                <option value="CRAS">CRAS</option><option value="CREAS">CREAS</option>
                <option value="JUDICIARIO">Judiciário</option><option value="SAUDE">Saúde</option>
                <option value="EDUCACAO">Educação</option><option value="DEMANDA_ESPONTANEA">Demanda espontânea</option>
              </select>
              {erros.origemEncaminhamento && <span className="field-error" id="erro-origem">{erros.origemEncaminhamento}</span>}
            </div>
          </div>
          <fieldset className="checks-row">
            <legend>Indicadores sociais</legend>
            <label><input type="checkbox" name="beneficiarioBpc" checked={dados.beneficiarioBpc} onChange={atualizarCampo} /> Beneficiário do BPC</label>
            <label><input type="checkbox" name="inscritoCadunico" checked={dados.inscritoCadunico} onChange={atualizarCampo} /> Inscrito no CadÚnico</label>
          </fieldset>
        </section>

        <section className="form-section">
          <div className="section-heading">
            <span>2</span>
            <div><h2>Documentos</h2><p>PDF, JPEG ou PNG, com no máximo 5 MB por arquivo.</p></div>
          </div>
          <div className="form-grid">
            <div className="field">
              <label htmlFor="laudo">Laudo médico *</label>
              <input id="laudo" type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" onChange={(event) => { setLaudo(event.target.files?.[0] ?? null); setErros((e) => ({ ...e, laudo: '' })) }} aria-invalid={!!erros.laudo} aria-describedby={erros.laudo ? 'erro-laudo' : undefined} />
              {erros.laudo && <span className="field-error" id="erro-laudo">{erros.laudo}</span>}
            </div>
            <div className="field">
              <label htmlFor="comprovante">Comprovante de residência *</label>
              <input id="comprovante" type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" onChange={(event) => { setComprovante(event.target.files?.[0] ?? null); setErros((e) => ({ ...e, comprovante: '' })) }} aria-invalid={!!erros.comprovante} aria-describedby={erros.comprovante ? 'erro-comprovante' : undefined} />
              {erros.comprovante && <span className="field-error" id="erro-comprovante">{erros.comprovante}</span>}
            </div>
          </div>
        </section>

        <section className="form-section">
          <div className="section-heading">
            <span>3</span>
            <div><h2>Entrada na fila</h2><p>A posição será calculada cronologicamente.</p></div>
          </div>
          <div className="field compact-field">
            <label htmlFor="dataEntrada">Data de entrada *</label>
            <input id="dataEntrada" name="dataEntrada" type="date" value={dados.dataEntrada} onChange={atualizarCampo} aria-invalid={!!erros.dataEntrada} aria-describedby={erros.dataEntrada ? 'erro-data-entrada' : undefined} />
            {erros.dataEntrada && <span className="field-error" id="erro-data-entrada">{erros.dataEntrada}</span>}
          </div>
        </section>

        <div className="form-actions">
          <button className="button button-quiet" type="button" onClick={cancelar}>Cancelar</button>
          <button className="button button-primary" type="submit" disabled={enviando}>{enviando ? 'Salvando…' : 'Cadastrar na lista'}</button>
        </div>
      </form>
    </>
  )
}
