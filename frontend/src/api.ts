import type { BeneficiarioDetalhe, CadastroDados, ListaEsperaItem } from './types'

type ApiError = {
  mensagem?: string
  campos?: Record<string, string>
}

export class ErroApi extends Error {
  campos: Record<string, string>

  constructor(mensagem: string, campos: Record<string, string> = {}) {
    super(mensagem)
    this.campos = campos
  }
}

async function resposta<T>(response: Response): Promise<T> {
  if (response.ok) {
    return response.json() as Promise<T>
  }

  let erro: ApiError = {}
  try {
    erro = (await response.json()) as ApiError
  } catch {
    // Mantém a mensagem genérica quando a resposta não contém JSON.
  }
  throw new ErroApi(erro.mensagem ?? 'Não foi possível concluir a operação.', erro.campos)
}

export async function obterResumo(): Promise<{ entradasAtivas: number }> {
  return resposta(await fetch('/api/dashboard/resumo'))
}

export async function listarEspera(busca = ''): Promise<ListaEsperaItem[]> {
  const query = busca.trim() ? `?busca=${encodeURIComponent(busca.trim())}` : ''
  return resposta(await fetch(`/api/lista-espera${query}`))
}

export async function obterBeneficiario(id: string): Promise<BeneficiarioDetalhe> {
  return resposta(await fetch(`/api/beneficiarios/${encodeURIComponent(id)}`))
}

export async function cadastrarBeneficiario(
  dados: CadastroDados,
  laudo: File,
  comprovante: File,
): Promise<BeneficiarioDetalhe> {
  const form = new FormData()
  form.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
  form.append('laudo', laudo)
  form.append('comprovante', comprovante)

  return resposta(
    await fetch('/api/beneficiarios', {
      method: 'POST',
      body: form,
    }),
  )
}
