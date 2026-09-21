import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { CategoriaPrioridade, Paciente, Procedimento, UnidadeSaude } from '../api/types'

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

  const [pacientes, setPacientes] = useState<Paciente[]>([])
  const [procedimentos, setProcedimentos] = useState<Procedimento[]>([])
  const [unidades, setUnidades] = useState<UnidadeSaude[]>([])

  const [modoPaciente, setModoPaciente] = useState<'existente' | 'novo'>('existente')
  const [pacienteId, setPacienteId] = useState('')
  const [novoNome, setNovoNome] = useState('')
  const [novoCpf, setNovoCpf] = useState('')
  const [novoCns, setNovoCns] = useState('')
  const [novoNascimento, setNovoNascimento] = useState('')
  const [novoTelefone, setNovoTelefone] = useState('')
  const [novoEmail, setNovoEmail] = useState('')

  const [procedimentoId, setProcedimentoId] = useState('')
  const [unidadeId, setUnidadeId] = useState('')
  const [categoria, setCategoria] = useState<CategoriaPrioridade>('NORMAL')
  const [processoJudicial, setProcessoJudicial] = useState('')
  const [dataSolicitacao, setDataSolicitacao] = useState(hoje())

  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [sucesso, setSucesso] = useState<string | null>(null)

  useEffect(() => {
    api.get<Paciente[]>('/api/pacientes').then((res) => setPacientes(res.data))
    api.get<Procedimento[]>('/api/procedimentos').then((res) => setProcedimentos(res.data))
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setUnidades(res.data))
  }, [])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
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
        const resPaciente = await api.post<Paciente>('/api/pacientes', {
          nome: novoNome,
          cpf: novoCpf || null,
          cns: novoCns || null,
          dataNascimento: novoNascimento || null,
          telefone: novoTelefone || null,
          email: novoEmail || null,
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

      setSucesso(`Protocolo ${resProtocolo.data.numeroProtocolo} criado com sucesso.`)
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
              <input
                placeholder="CPF"
                value={novoCpf}
                onChange={(e) => setNovoCpf(e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <input
                placeholder="CNS"
                value={novoCns}
                onChange={(e) => setNovoCns(e.target.value)}
                className="rounded-lg border border-gray-300 px-3 py-2 text-sm"
              />
              <input
                type="date"
                placeholder="Data de nascimento"
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
          className="rounded-lg bg-brand-navy text-white px-5 py-2.5 text-sm font-semibold disabled:opacity-50"
        >
          {enviando ? 'Salvando…' : 'Incluir na fila'}
        </button>
      </form>
    </AdminLayout>
  )
}
