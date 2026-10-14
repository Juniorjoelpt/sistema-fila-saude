import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { useAuth } from '../context/AuthContext'
import type {
  CategoriaPrioridade,
  ClassificacaoSugerida,
  Paciente,
  PacienteDuplicado,
  Procedimento,
  UnidadeSaude,
  Usuario,
  ValidacaoDocumentoResponse,
} from '../api/types'

// "1950-03-12" -> "12/03/1950" (a API devolve data ISO; o operador lê dd/mm/aaaa).
function formatarData(iso: string): string {
  const [ano, mes, dia] = iso.split('-')
  return ano && mes && dia ? `${dia}/${mes}/${ano}` : iso
}

const CATEGORIAS: { valor: CategoriaPrioridade; rotulo: string }[] = [
  { valor: 'URGENCIA', rotulo: 'Urgência' },
  { valor: 'JUDICIAL', rotulo: 'Judicial' },
  { valor: 'ESPECIAL', rotulo: 'Especial (80+)' },
  { valor: 'LEGAL', rotulo: 'Legal (60+, PCD, gestante)' },
  { valor: 'NORMAL', rotulo: 'Normal' },
]

function hoje(): string {
  return new Date().toISOString().slice(0, 10)
}

export function NovoProtocolo() {
  const navigate = useNavigate()
  const { usuario } = useAuth()
  const ehAcs = usuario?.papel === 'ACS'

  const [pacientes, setPacientes] = useState<Paciente[]>([])
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [unidades, setUnidades] = useState<UnidadeSaude[]>([])
  const [acsLista, setAcsLista] = useState<Usuario[]>([])

  const [modoPaciente, setModoPaciente] = useState<'existente' | 'novo'>('existente')
  const [pacienteId, setPacienteId] = useState('')
  const [novoNome, setNovoNome] = useState('')
  const [novoCpf, setNovoCpf] = useState('')
  const [novoCns, setNovoCns] = useState('')
  const [novoNascimento, setNovoNascimento] = useState('')
  const [novoTelefone, setNovoTelefone] = useState('')
  const [novoEmail, setNovoEmail] = useState('')
  const [novoAcsResponsavelId, setNovoAcsResponsavelId] = useState('')
  const [novoPcd, setNovoPcd] = useState(false)
  const [novoGestante, setNovoGestante] = useState(false)

  // Validação de CPF/CNS via CADSUS (item do roadmap) -- ação explícita do
  // operador, não bloqueia o cadastro mesmo quando a integração real ainda
  // não está disponível (ver backend: ValidacaoCadastralService).
  const [validandoCpf, setValidandoCpf] = useState(false)
  const [resultadoCpf, setResultadoCpf] = useState<ValidacaoDocumentoResponse | null>(null)
  const [validandoCns, setValidandoCns] = useState(false)
  const [resultadoCns, setResultadoCns] = useState<ValidacaoDocumentoResponse | null>(null)

  async function validarCpf() {
    const digitos = novoCpf.replace(/\D/g, '')
    if (!digitos) return
    setValidandoCpf(true)
    setResultadoCpf(null)
    try {
      const { data } = await api.get<ValidacaoDocumentoResponse>('/api/validacao-cadastral/cpf', {
        params: { valor: digitos },
      })
      setResultadoCpf(data)
      if (data.encontradoNoCadsus && data.nomeCadsus && !novoNome.trim()) {
        setNovoNome(data.nomeCadsus)
      }
      if (data.encontradoNoCadsus && data.dataNascimentoCadsus && !novoNascimento) {
        setNovoNascimento(data.dataNascimentoCadsus)
      }
    } catch {
      setResultadoCpf({
        tipoDocumento: 'CPF',
        documento: digitos,
        formatoValido: false,
        consultaCadsusDisponivel: false,
        encontradoNoCadsus: false,
        nomeCadsus: null,
        dataNascimentoCadsus: null,
        mensagem: 'Não foi possível validar o CPF agora.',
      })
    } finally {
      setValidandoCpf(false)
    }
  }

  async function validarCns() {
    const digitos = novoCns.replace(/\D/g, '')
    if (!digitos) return
    setValidandoCns(true)
    setResultadoCns(null)
    try {
      const { data } = await api.get<ValidacaoDocumentoResponse>('/api/validacao-cadastral/cns', {
        params: { valor: digitos },
      })
      setResultadoCns(data)
      if (data.encontradoNoCadsus && data.nomeCadsus && !novoNome.trim()) {
        setNovoNome(data.nomeCadsus)
      }
      if (data.encontradoNoCadsus && data.dataNascimentoCadsus && !novoNascimento) {
        setNovoNascimento(data.dataNascimentoCadsus)
      }
    } catch {
      setResultadoCns({
        tipoDocumento: 'CNS',
        documento: digitos,
        formatoValido: false,
        consultaCadsusDisponivel: false,
        encontradoNoCadsus: false,
        nomeCadsus: null,
        dataNascimentoCadsus: null,
        mensagem: 'Não foi possível validar o CNS agora.',
      })
    } finally {
      setValidandoCns(false)
    }
  }

  const [procedimentoId, setProcedimentoId] = useState('')
  const [unidadeId, setUnidadeId] = useState('')
  const [categoria, setCategoria] = useState<CategoriaPrioridade>('NORMAL')
  const [processoJudicial, setProcessoJudicial] = useState('')
  const [dataSolicitacao, setDataSolicitacao] = useState(hoje())

  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  // Detecção de duplicidade fuzzy de paciente (item do roadmap): checado
  // antes de criar um paciente "novo", para avisar sem bloquear -- o
  // operador decide usar o cadastro já existente ou confirmar que são
  // pessoas diferentes.
  const [duplicados, setDuplicados] = useState<PacienteDuplicado[]>([])
  const [verificandoDuplicados, setVerificandoDuplicados] = useState(false)

  // Classificação de risco assistida (item do roadmap): sugere Especial/Legal
  // a partir de idade/PCD/gestante do paciente (novo ou já cadastrado) para
  // reduzir o risco de um protocolo ficar sub-classificado por esquecimento
  // do operador -- só avisa, a escolha final no select continua livre.
  const [sugestaoCategoria, setSugestaoCategoria] = useState<ClassificacaoSugerida | null>(null)
  // Há um paciente (novo ou selecionado) cujos critérios já dá para avaliar? Sem isso,
  // não faz sentido avisar que "não há dados" antes de o operador escolher o paciente.
  const [pacienteDefinido, setPacienteDefinido] = useState(false)
  const [semDadosParaClassificar, setSemDadosParaClassificar] = useState(false)

  useEffect(() => {
    let dataNascimentoRef: string | null = null
    let pcdRef = false
    let gestanteRef = false

    if (modoPaciente === 'novo') {
      dataNascimentoRef = novoNascimento || null
      pcdRef = novoPcd
      gestanteRef = novoGestante
      setPacienteDefinido(true)
    } else {
      const selecionado = pacientes.find((p) => String(p.id) === pacienteId)
      setPacienteDefinido(!!selecionado)
      if (selecionado) {
        dataNascimentoRef = selecionado.dataNascimento
        pcdRef = selecionado.pcd
        gestanteRef = selecionado.gestante
      }
    }

    if (!dataNascimentoRef && !pcdRef && !gestanteRef) {
      setSugestaoCategoria(null)
      setSemDadosParaClassificar(true)
      return
    }
    setSemDadosParaClassificar(false)

    api
      .get<ClassificacaoSugerida>('/api/classificacao-risco/sugestao', {
        params: { dataNascimento: dataNascimentoRef || undefined, pcd: pcdRef, gestante: gestanteRef },
      })
      .then((res) => setSugestaoCategoria(res.data))
      .catch(() => setSugestaoCategoria(null))
  }, [modoPaciente, novoNascimento, novoPcd, novoGestante, pacienteId, pacientes])

  // Só vale a pena mostrar quando a sugestão é MAIS prioritária do que a
  // categoria já selecionada (posição mais baixa em CATEGORIAS) -- nunca
  // sugere "rebaixar" uma Urgência/Judicial já escolhida pelo operador, já
  // que essas duas dependem de julgamento clínico/jurídico que o critério
  // objetivo aqui não tem como avaliar.
  const indiceSugestao = sugestaoCategoria
    ? CATEGORIAS.findIndex((c) => c.valor === sugestaoCategoria.categoriaSugerida)
    : -1
  const indiceAtual = CATEGORIAS.findIndex((c) => c.valor === categoria)
  const mostrarSugestao =
    sugestaoCategoria !== null && indiceSugestao !== -1 && indiceSugestao < indiceAtual

  // Aviso inverso: Especial/Legal escolhida, mas o paciente NÃO atende ao critério
  // (o backend recusaria só depois de salvar). Sem nenhum dado para avaliar
  // (sem data de nascimento, PCD nem gestante), também avisa que não dá para confirmar.
  const categoriaComCriterio = categoria === 'ESPECIAL' || categoria === 'LEGAL'
  const criterioNaoAtendido =
    categoriaComCriterio &&
    pacienteDefinido &&
    ((sugestaoCategoria === null && semDadosParaClassificar) ||
      (sugestaoCategoria !== null && indiceSugestao !== -1 && indiceSugestao > indiceAtual))
  const rotuloCategoriaAtual = CATEGORIAS[indiceAtual]?.rotulo ?? categoria

  useEffect(() => {
    api.get<Paciente[]>('/api/pacientes').then((res) => setPacientes(res.data))
    api.get<Procedimento[]>('/api/procedimentos').then((res) => setProcedimentos(res.data))
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setUnidades(res.data))
    if (!ehAcs) {
      api.get<Usuario[]>('/api/usuarios', { params: { papel: 'ACS' } }).then((res) => setAcsLista(res.data))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    await realizarCadastro(false)
  }

  async function realizarCadastro(forcarApesarDeDuplicado = false) {
    setErro(null)
    setSucesso(null)

    if (categoria === 'JUDICIAL' && !processoJudicial.trim()) {
      setErro('Protocolos na categoria Judicial exigem o número do processo/mandado.')
      return
    }

    setEnviando(true)
    try {
      let idPaciente = pacienteId

      if (modoPaciente === 'novo') {
        if (!novoNome.trim()) {
          setErro('Informe o nome do paciente.')
          setEnviando(false)
          return
        }
        const cpfDigitos = novoCpf.replace(/\D/g, '')
        const cnsDigitos = novoCns.replace(/\D/g, '')
        if (!cpfDigitos && !cnsDigitos) {
          setErro('Informe ao menos um documento do paciente: CPF ou CNS.')
          setEnviando(false)
          return
        }
        if (!novoNascimento) {
          setErro('Informe a data de nascimento do paciente.')
          setEnviando(false)
          return
        }
        // Bug alto de revisão corrigido: não havia nenhuma validação de
        // formato de CPF/CNS neste formulário -- era possível cadastrar um
        // paciente com "123" no campo CPF sem nenhum aviso, tanto aqui
        // quanto no backend (agora também validado em PacienteService).
        if (cpfDigitos && cpfDigitos.length !== 11) {
          setErro('CPF inválido: deve conter 11 dígitos.')
          setEnviando(false)
          return
        }
        if (cnsDigitos && cnsDigitos.length !== 15) {
          setErro('CNS inválido: deve conter 15 dígitos.')
          setEnviando(false)
          return
        }

        // Detecção de duplicidade fuzzy de paciente (item do roadmap): só
        // avisa, não bloqueia -- se o operador confirmar que são pessoas
        // diferentes ("Cadastrar mesmo assim"), segue direto para o cadastro.
        if (!forcarApesarDeDuplicado) {
          setVerificandoDuplicados(true)
          try {
            const { data } = await api.get<PacienteDuplicado[]>('/api/pacientes/possiveis-duplicados', {
              params: {
                nome: novoNome,
                dataNascimento: novoNascimento || undefined,
                telefone: novoTelefone || undefined,
              },
            })
            if (data.length > 0) {
              setDuplicados(data)
              setEnviando(false)
              setVerificandoDuplicados(false)
              return
            }
          } catch {
            // checagem é só um aviso -- se falhar, não impede o cadastro
          } finally {
            setVerificandoDuplicados(false)
          }
        }
        setDuplicados([])

        const resPaciente = await api.post<Paciente>('/api/pacientes', {
          nome: novoNome,
          cpf: novoCpf || null,
          cns: novoCns || null,
          dataNascimento: novoNascimento || null,
          telefone: novoTelefone || null,
          email: novoEmail || null,
          acsResponsavelId: novoAcsResponsavelId ? Number(novoAcsResponsavelId) : null,
          pcd: novoPcd,
          gestante: novoGestante,
        })
        idPaciente = String(resPaciente.data.id)
      }

      if (!idPaciente) {
        setErro('Selecione ou cadastre um paciente.')
        setEnviando(false)
        return
      }
      if (!procedimentoId) {
        setErro('Selecione o procedimento.')
        setEnviando(false)
        return
      }

      const resProtocolo = await api.post('/api/fila', {
        pacienteId: Number(idPaciente),
        procedimentoId: Number(procedimentoId),
        unidadeSaudeId: unidadeId ? Number(unidadeId) : null,
        categoriaPrioridade: categoria,
        processoJudicial: processoJudicial || null,
        dataSolicitacao,
      })

      setSucesso(
        resProtocolo.data.categoriaSolicitada
          ? `Protocolo ${resProtocolo.data.numeroProtocolo} criado. A categoria ${rotuloCategoriaAtual} aguarda confirmação do regulador.`
          : `Protocolo ${resProtocolo.data.numeroProtocolo} criado com sucesso.`,
      )
      setTimeout(() => navigate(`/admin/fila/${resProtocolo.data.id}`), 900)
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível criar o protocolo. Verifique os dados.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Novo Protocolo</h1>

      <form onSubmit={handleSubmit} className="max-w-2xl space-y-6">
        <section className="rounded-2xl border bg-white shadow-sm p-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Paciente</h2>

          <div className="flex gap-4 mb-4 text-sm">
            <label className="flex items-center gap-2">
              <input
                type="radio"
                checked={modoPaciente === 'existente'}
                onChange={() => setModoPaciente('existente')}
              />
              Paciente já cadastrado
            </label>
            <label className="flex items-center gap-2">
              <input type="radio" checked={modoPaciente === 'novo'} onChange={() => setModoPaciente('novo')} />
              Novo paciente
            </label>
          </div>

          {modoPaciente === 'existente' ? (
            <select
              value={pacienteId}
              onChange={(e) => setPacienteId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            >
              <option value="">Selecione o paciente…</option>
              {pacientes.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome} {p.cpf ? `— CPF ${p.cpf}` : ''}
                </option>
              ))}
            </select>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <input
                placeholder="Nome completo *"
                value={novoNome}
                onChange={(e) => setNovoNome(e.target.value)}
                className="col-span-2 rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <div>
                <div className="flex gap-1.5">
                  <input
                    placeholder="CPF"
                    value={novoCpf}
                    onChange={(e) => {
                      setNovoCpf(e.target.value)
                      setResultadoCpf(null)
                    }}
                    className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
                  />
                  <button
                    type="button"
                    onClick={validarCpf}
                    disabled={validandoCpf || !novoCpf.replace(/\D/g, '')}
                    className="shrink-0 rounded-lg border border-gray-300 px-2.5 text-xs font-semibold text-gray-600 hover:bg-gray-50 disabled:opacity-40"
                  >
                    {validandoCpf ? '…' : 'Validar'}
                  </button>
                </div>
                {resultadoCpf && (
                  <p className={`mt-1 text-[11px] ${resultadoCpf.formatoValido ? 'text-gray-400' : 'text-red-600'}`}>
                    {resultadoCpf.mensagem}
                  </p>
                )}
              </div>
              <div>
                <div className="flex gap-1.5">
                  <input
                    placeholder="CNS"
                    value={novoCns}
                    onChange={(e) => {
                      setNovoCns(e.target.value)
                      setResultadoCns(null)
                    }}
                    className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm"
                  />
                  <button
                    type="button"
                    onClick={validarCns}
                    disabled={validandoCns || !novoCns.replace(/\D/g, '')}
                    className="shrink-0 rounded-lg border border-gray-300 px-2.5 text-xs font-semibold text-gray-600 hover:bg-gray-50 disabled:opacity-40"
                  >
                    {validandoCns ? '…' : 'Validar'}
                  </button>
                </div>
                {resultadoCns && (
                  <p className={`mt-1 text-[11px] ${resultadoCns.formatoValido ? 'text-gray-400' : 'text-red-600'}`}>
                    {resultadoCns.mensagem}
                  </p>
                )}
              </div>
              <input
                type="date"
                placeholder="Data de nascimento"
                aria-label="Data de nascimento (obrigatória)"
                title="Data de nascimento (obrigatória)"
                required
                max={new Date().toISOString().slice(0, 10)}
                value={novoNascimento}
                onChange={(e) => setNovoNascimento(e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <input
                placeholder="Telefone"
                value={novoTelefone}
                onChange={(e) => setNovoTelefone(e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <input
                placeholder="E-mail"
                value={novoEmail}
                onChange={(e) => setNovoEmail(e.target.value)}
                className="col-span-2 rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              {!ehAcs && (
                <select
                  value={novoAcsResponsavelId}
                  onChange={(e) => setNovoAcsResponsavelId(e.target.value)}
                  className="col-span-2 rounded-lg border border-gray-300 px-3 py-2 text-sm"
                >
                  <option value="">ACS responsável (opcional)</option>
                  {acsLista.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.nome}
                    </option>
                  ))}
                </select>
              )}
              {ehAcs && (
                <p className="col-span-2 text-xs text-gray-400">
                  Este paciente ficará vinculado a você automaticamente.
                </p>
              )}
              <label className="flex items-center gap-2 text-xs text-gray-600">
                <input type="checkbox" checked={novoPcd} onChange={(e) => setNovoPcd(e.target.checked)} />
                Pessoa com deficiência (PCD)
              </label>
              <label className="flex items-center gap-2 text-xs text-gray-600">
                <input
                  type="checkbox"
                  checked={novoGestante}
                  onChange={(e) => setNovoGestante(e.target.checked)}
                />
                Gestante
              </label>
              <p className="col-span-2 text-[11px] text-gray-400">
                Usado para validar a categoria de prioridade Legal (60+, PCD ou gestante).
              </p>
            </div>
          )}

          {duplicados.length > 0 && (
            <div className="mt-4 rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm">
              <p className="font-semibold text-amber-800">
                Encontramos {duplicados.length === 1 ? 'um paciente parecido' : 'pacientes parecidos'} já
                cadastrado{duplicados.length === 1 ? '' : 's'}. Confira antes de criar um novo cadastro:
              </p>
              <ul className="mt-3 space-y-2">
                {duplicados.map((d) => (
                  <li key={d.pacienteId} className="rounded-lg border border-amber-200 bg-white p-3">
                    <div className="flex items-center justify-between gap-2">
                      <div>
                        <p className="font-medium text-gray-800">{d.nome}</p>
                        <p className="text-[11px] text-gray-500">
                          {d.cpf ? `CPF ${d.cpf}` : d.cns ? `CNS ${d.cns}` : 'sem documento cadastrado'}
                          {d.dataNascimento ? ` • nasc. ${formatarData(d.dataNascimento)}` : ''}
                          {d.telefone ? ` • tel. ${d.telefone}` : ''}
                        </p>
                        <p className="text-[11px] text-amber-700 mt-0.5">
                          {d.motivo} ({d.pontuacaoSimilaridade}% de semelhança no nome)
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          setModoPaciente('existente')
                          setPacienteId(String(d.pacienteId))
                          setDuplicados([])
                        }}
                        className="shrink-0 rounded-lg border border-amber-400 px-2.5 py-1 text-xs font-semibold text-amber-800 hover:bg-amber-100"
                      >
                        Usar este
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
              <div className="mt-3 flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => realizarCadastro(true)}
                  disabled={enviando}
                  className="rounded-lg border border-gray-300 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  Cadastrar mesmo assim, são pessoas diferentes
                </button>
                <button
                  type="button"
                  onClick={() => setDuplicados([])}
                  className="text-xs font-medium text-gray-500 hover:text-gray-700"
                >
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </section>

        <section className="rounded-2xl border bg-white shadow-sm p-6 space-y-3">
          <h2 className="text-sm font-semibold text-gray-700 mb-1">Protocolo</h2>

          <select
            value={procedimentoId}
            onChange={(e) => setProcedimentoId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="">Selecione o procedimento…</option>
            {procedimentos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nome} {p.especialidade ? `(${p.especialidade})` : ''}
              </option>
            ))}
          </select>

          <select
            value={unidadeId}
            onChange={(e) => setUnidadeId(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          >
            <option value="">Unidade de saúde (opcional)</option>
            {unidades.map((u) => (
              <option key={u.id} value={u.id}>
                {u.nome}
              </option>
            ))}
          </select>

          <div className="grid grid-cols-2 gap-3">
            <select
              value={categoria}
              onChange={(e) => setCategoria(e.target.value as CategoriaPrioridade)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
            >
              {CATEGORIAS.map((c) => (
                <option key={c.valor} value={c.valor}>
                  {c.rotulo}
                </option>
              ))}
            </select>
            <input
              type="date"
              value={dataSolicitacao}
              onChange={(e) => setDataSolicitacao(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          </div>

          {usuario?.papel === 'ACS' && (categoria === 'URGENCIA' || categoria === 'JUDICIAL') && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900">
              Urgência e Judicial precisam da confirmação do regulador. O protocolo entra na fila como{' '}
              <strong>Normal</strong> e passa para <strong>{rotuloCategoriaAtual}</strong> assim que o regulador confirmar.
            </div>
          )}

          {mostrarSugestao && sugestaoCategoria && (
            <div className="rounded-lg border border-sky-200 bg-sky-50 px-3 py-2 text-xs text-sky-800 flex items-center justify-between gap-3">
              <span>
                Classificação de risco assistida: critérios do paciente indicam{' '}
                <strong>{sugestaoCategoria.descricaoCategoria}</strong> — {sugestaoCategoria.motivo}.
              </span>
              <button
                type="button"
                onClick={() => setCategoria(sugestaoCategoria.categoriaSugerida)}
                className="shrink-0 rounded border border-sky-400 px-2 py-1 font-semibold hover:bg-sky-100"
              >
                Usar sugestão
              </button>
            </div>
          )}

          {criterioNaoAtendido && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900 flex items-center justify-between gap-3">
              <span>
                {sugestaoCategoria ? (
                  <>
                    O paciente não atende ao critério de <strong>{rotuloCategoriaAtual}</strong> ({sugestaoCategoria.motivo}).
                    Categoria compatível: <strong>{sugestaoCategoria.descricaoCategoria}</strong>. O sistema recusará
                    o protocolo com a categoria atual.
                  </>
                ) : (
                  <>
                    Não há data de nascimento, PCD nem gestante informados para confirmar o critério de{' '}
                    <strong>{rotuloCategoriaAtual}</strong>. O sistema recusará o protocolo com esta categoria.
                  </>
                )}
              </span>
              {sugestaoCategoria && (
                <button
                  type="button"
                  onClick={() => setCategoria(sugestaoCategoria.categoriaSugerida)}
                  className="shrink-0 rounded border border-amber-500 px-2 py-1 font-semibold hover:bg-amber-100"
                >
                  Usar {sugestaoCategoria.descricaoCategoria}
                </button>
              )}
            </div>
          )}

          {categoria === 'JUDICIAL' && (
            <input
              placeholder="Número do processo/mandado *"
              value={processoJudicial}
              onChange={(e) => setProcessoJudicial(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
            />
          )}
        </section>

        {erro && <p className="text-sm text-red-600">{erro}</p>}
        {sucesso && <p className="text-sm text-brand-teal font-medium">{sucesso}</p>}

        <button
          type="submit"
          disabled={enviando}
          className="btn-primary rounded-lg text-white px-5 py-2.5 text-sm font-semibold disabled:opacity-50"
        >
          {verificandoDuplicados ? 'Verificando cadastros parecidos…' : enviando ? 'Salvando…' : 'Incluir na fila'}
        </button>
      </form>
    </AdminLayout>
  )
}
