export type ListaEsperaItem = {
  entradaId: number
  beneficiarioId: number
  posicao: number
  nome: string
  tipoDeficiencia: string
  dataEntrada: string
  beneficiarioBpc: boolean
  inscritoCadunico: boolean
  origemEncaminhamento: string
}

export type Documento = {
  nomeOriginal: string
  tipoConteudo: string
  tamanho: number
}

export type BeneficiarioDetalhe = {
  id: number
  entradaId: number
  posicao: number
  nome: string
  dataNascimento: string
  responsavel: string
  telefone: string
  tipoDeficiencia: string
  beneficiarioBpc: boolean
  inscritoCadunico: boolean
  origemEncaminhamento: string
  dataEntrada: string
  laudoMedico: Documento
  comprovanteResidencia: Documento
  dataCriacao: string
}

export type CadastroDados = {
  nome: string
  dataNascimento: string
  responsavel: string
  telefone: string
  tipoDeficiencia: string
  beneficiarioBpc: boolean
  inscritoCadunico: boolean
  origemEncaminhamento: string
  dataEntrada: string
}

