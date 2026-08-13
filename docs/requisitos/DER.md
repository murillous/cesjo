# Diagrama Entidade-Relacionamento

## Situação do documento

Este arquivo é a versão editável e versionável do diagrama do projeto.

O diagrama abaixo representa o núcleo priorizado do sistema e corrige a ambiguidade do termo `Usuario`:

- `ContaAcesso` representa o profissional que entra no sistema.
- `Beneficiario` representa a pessoa candidata ou atendida pelo Centro-Dia.
- `ListaEspera` representa cada passagem do beneficiário pela fila.
- `VinculoServico` representa a admissão do beneficiário no serviço.

## DER consolidado do núcleo

```mermaid
erDiagram
    CONTA_ACESSO ||--o{ BENEFICIARIO : cadastra
    CONTA_ACESSO ||--o{ LISTA_ESPERA : registra
    CONTA_ACESSO ||--o{ VISITA_DOMICILIAR : registra
    CONTA_ACESSO ||--o{ ESCUTA : registra
    CONTA_ACESSO ||--o{ PIA : elabora
    CONTA_ACESSO ||--o{ ATIVIDADE : registra

    BENEFICIARIO ||--o{ LISTA_ESPERA : possui
    BENEFICIARIO ||--o{ VISITA_DOMICILIAR : recebe
    BENEFICIARIO ||--o{ ESCUTA : participa
    BENEFICIARIO ||--o{ PIA : possui
    BENEFICIARIO ||--o{ VINCULO_SERVICO : possui

    VINCULO_SERVICO ||--o{ DESLIGAMENTO : encerra
    VINCULO_SERVICO ||--o{ VIOLACAO_DIREITO : registra
    VINCULO_SERVICO }o--o{ ATIVIDADE : participa

    CONTA_ACESSO {
        bigint id_conta PK
        varchar nome
        varchar email UK
        varchar senha_hash
        varchar perfil
    }

    BENEFICIARIO {
        bigint id_beneficiario PK
        varchar nome
        date data_nascimento
        varchar responsavel
        varchar telefone
        varchar tipo_deficiencia
        boolean beneficiario_bpc
        boolean inscrito_cadunico
        bigint id_conta_cadastro FK
    }

    LISTA_ESPERA {
        bigint id_lista_espera PK
        bigint id_beneficiario FK
        date data_entrada
        date data_saida
        varchar motivo_saida
        text observacao
        bigint id_conta_cadastro FK
    }

    VISITA_DOMICILIAR {
        bigint id_visita PK
        bigint id_beneficiario FK
        timestamp data_hora
        varchar status
        bigint id_conta_registro FK
    }

    ESCUTA {
        bigint id_escuta PK
        bigint id_beneficiario FK
        date data_escuta
        text dinamica_familiar
        text necessidades
        text habilidades
        text potencialidades
        bigint id_conta_registro FK
    }

    PIA {
        bigint id_pia PK
        bigint id_beneficiario FK
        text descricao
        varchar status
        bigint id_conta_elaboracao FK
    }

    VINCULO_SERVICO {
        bigint id_vinculo PK
        bigint id_beneficiario FK
        date data_insercao
        varchar genero
        varchar crenca
        varchar bairro
        varchar turno
        varchar cuidador
    }

    DESLIGAMENTO {
        bigint id_desligamento PK
        bigint id_vinculo FK
        date data_desligamento
        varchar motivo
    }

    VIOLACAO_DIREITO {
        bigint id_violacao PK
        bigint id_vinculo FK
        varchar tipo
        text descricao
        date data_registro
    }

    ATIVIDADE {
        bigint id_atividade PK
        varchar tipo
        text descricao
        timestamp data_hora
        bigint id_conta_registro FK
    }
```

## Regras estruturais

1. Um beneficiário é cadastrado uma única vez e pode passar pela lista de espera mais de uma vez.
2. Um beneficiário não pode possuir duas entradas ativas simultâneas na lista.
3. A admissão no serviço não cria uma cópia do beneficiário; cria um `VinculoServico`.
4. A faixa etária é calculada a partir da data de nascimento e não deve ser armazenada de forma redundante.
5. Senhas pertencem somente a `ContaAcesso` e devem ser armazenadas como hash.
6. Relatórios e dashboards são projeções dos dados transacionais e não geram entidades próprias neste momento.

## Partes ainda pendentes de detalhamento

O documento de requisitos ainda não fornece informação suficiente para fechar:

- cardinalidade e segmentação de vagas;
- profissionais participantes de uma visita ou atividade;
- versionamento e aprovações do PIA;
- participantes de atividades coletivas;
- estrutura de contatos, ofícios e encaminhamentos;
- detalhes de parcerias, colaboradores e doações;
- estrutura de auditoria exigida pelo requisito não funcional.

Essas partes devem ser acrescentadas ao DER quando seus critérios de aceite forem definidos.
