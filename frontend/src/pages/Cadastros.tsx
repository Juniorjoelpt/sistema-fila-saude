import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import type { Paciente, Procedimento, UnidadeSaude } from '../api/types'

type Aba = 'procedimentos' | 'unidades' | 'pacientes'

function TabButton({ ativo, onClick, children }: { ativo: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
        ativo ? 'bg-brand-navy text-white' : 'text-gray-600 hover:bg-gray-100'
      }`}
    >
      {children}
    </button>
  )
}

function AbaProcedimentos() {
  const [itens, setItens] = useState<Procedimento[]>([])
  const [nome, setNome] = useState('')
  const [tipo, setTipo] = useState<'CONSULTA' | 'EXAME' | 'CIRURGIA'>('CONSULTA')
  const [especialidade, setEspecialidade] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  function carregar() {
    api.get<Procedimento[]>('/api/procedimentos').then((res) => setItens(res.data))
  }
  useEffect(carregar, [])

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    if (!nome.trim()) {
      setErro('Informe o nome do procedimento.')
      return
    }
    try {
      await api.post('/api/procedimentos', { nome, tipo, especialidade: especialidade || null })
      setNome('')
      setEspecialidade('')
      carregar()
    } catch {
      setErro('Não foi possível salvar o procedimento.')
    }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <form onSubmit={salvar} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
        <h2 className="text-sm font-semibold text-gray-700">Novo procedimento</h2>
        <input
          placeholder="Nome *"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <select
          value={tipo}
          onChange={(e) => setTipo(e.target.value as typeof tipo)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          <option value="CONSULTA">Consulta</option>
          <option value="EXAME">Exame</option>
          <option value="CIRURGIA">Cirurgia</option>
        </select>
        <input
          placeholder="Especialidade"
          value={especialidade}
          onChange={(e) => setEspecialidade(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        {erro && <p className="text-xs text-red-600">{erro}</p>}
        <button type="submit" className="w-full rounded-lg bg-brand-navy text-white py-2 text-sm font-semibold">
          Adicionar
        </button>
      </form>

      <div className="lg:col-span-2 rounded-2xl border bg-white shadow-sm overflow-hidden h-fit">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
            <tr>
              <th className="text-left px-4 py-3">Nome</th>
              <th className="text-left px-4 py-3">Tipo</th>
              <th className="text-left px-4 py-3">Especialidade</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {itens.length === 0 && (
              <tr>
                <td colSpan={3} className="px-4 py-6 text-center text-gray-400">
                  Nenhum procedimento cadastrado.
                </td>
              </tr>
            )}
            {itens.map((p) => (
              <tr key={p.id}>
                <td className="px-4 py-3 text-gray-900">{p.nome}</td>
                <td className="px-4 py-3 text-gray-600">{p.tipo}</td>
                <td className="px-4 py-3 text-gray-600">{p.especialidade ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function AbaUnidades() {
  const [itens, setItens] = useState<UnidadeSaude[]>([])
  const [nome, setNome] = useState('')
  const [endereco, setEndereco] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  function carregar() {
    api.get<UnidadeSaude[]>('/api/unidades').then((res) => setItens(res.data))
  }
  useEffect(carregar, [])

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    if (!nome.trim()) {
      setErro('Informe o nome da unidade.')
      return
    }
    try {
      await api.post('/api/unidades', { nome, endereco: endereco || null })
      setNome('')
      setEndereco('')
      carregar()
    } catch {
      setErro('Não foi possível salvar a unidade.')
    }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <form onSubmit={salvar} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
        <h2 className="text-sm font-semibold text-gray-700">Nova unidade de saúde</h2>
        <input
          placeholder="Nome *"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          placeholder="Endereço"
          value={endereco}
          onChange={(e) => setEndereco(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        {erro && <p className="text-xs text-red-600">{erro}</p>}
        <button type="submit" className="w-full rounded-lg bg-brand-navy text-white py-2 text-sm font-semibold">
          Adicionar
        </button>
      </form>

      <div className="lg:col-span-2 rounded-2xl border bg-white shadow-sm overflow-hidden h-fit">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
            <tr>
              <th className="text-left px-4 py-3">Nome</th>
              <th className="text-left px-4 py-3">Endereço</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {itens.length === 0 && (
              <tr>
                <td colSpan={2} className="px-4 py-6 text-center text-gray-400">
                  Nenhuma unidade cadastrada.
                </td>
              </tr>
            )}
            {itens.map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-3 text-gray-900">{u.nome}</td>
                <td className="px-4 py-3 text-gray-600">{u.endereco ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function AbaPacientes() {
  const [itens, setItens] = useState<Paciente[]>([])

  useEffect(() => {
    api.get<Paciente[]>('/api/pacientes').then((res) => setItens(res.data))
  }, [])

  return (
    <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
          <tr>
            <th className="text-left px-4 py-3">Nome</th>
            <th className="text-left px-4 py-3">CPF</th>
            <th className="text-left px-4 py-3">CNS</th>
            <th className="text-left px-4 py-3">Telefone</th>
          </tr>
        </thead>
        <tbody className="divide-y">
          {itens.length === 0 && (
            <tr>
              <td colSpan={4} className="px-4 py-6 text-center text-gray-400">
                Nenhum paciente cadastrado ainda.
              </td>
            </tr>
          )}
          {itens.map((p) => (
            <tr key={p.id}>
              <td className="px-4 py-3 text-gray-900">{p.nome}</td>
              <td className="px-4 py-3 text-gray-600">{p.cpf ?? '—'}</td>
              <td className="px-4 py-3 text-gray-600">{p.cns ?? '—'}</td>
              <td className="px-4 py-3 text-gray-600">{p.telefone ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="px-4 py-3 text-xs text-gray-400 border-t">
        Para cadastrar um novo paciente, use a tela "Novo Protocolo".
      </p>
    </div>
  )
}

export function Cadastros() {
  const [aba, setAba] = useState<Aba>('procedimentos')

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Cadastros</h1>

      <div className="flex gap-2 mb-6">
        <TabButton ativo={aba === 'procedimentos'} onClick={() => setAba('procedimentos')}>
          Procedimentos
        </TabButton>
        <TabButton ativo={aba === 'unidades'} onClick={() => setAba('unidades')}>
          Unidades de Saúde
        </TabButton>
        <TabButton ativo={aba === 'pacientes'} onClick={() => setAba('pacientes')}>
          Pacientes
        </TabButton>
      </div>

      {aba === 'procedimentos' && <AbaProcedimentos />}
      {aba === 'unidades' && <AbaUnidades />}
      {aba === 'pacientes' && <AbaPacientes />}
    </AdminLayout>
  )
}
