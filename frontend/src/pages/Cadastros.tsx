import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { api } from '../api/client'
import { AdminLayout } from '../components/AdminLayout'
import { ImportadorCsv } from '../components/ImportadorCsv'
import { useAuth } from '../context/AuthContext'
import type { Paciente, Papel, Procedimento, UnidadeSaude, Usuario } from '../api/types'

type Aba = 'procedimentos' | 'unidades' | 'pacientes' | 'equipe'

const PAPEIS: Papel[] = ['ACS', 'REGULADOR', 'ADMIN']

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
    } catch (err: any) {
      // Bug de revisão corrigido: ignorava a mensagem real do backend (ex.: 409 de
      // conflito, 400 de validação), sempre mostrava o mesmo texto genérico.
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível salvar o procedimento.')
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

      <div className="lg:col-span-2 space-y-4">
        <ImportadorCsv
          endpoint="/api/importacao/procedimentos"
          colunasEsperadas="nome, tipo (CONSULTA/EXAME/CIRURGIA), especialidade"
          onImportado={carregar}
        />
        <div className="rounded-2xl border bg-white shadow-sm overflow-hidden h-fit">
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
    </div>
  )
}

function AbaUnidades() {
  const [itens, setItens] = useState<UnidadeSaude[]>([])
  const [nome, setNome] = useState('')
  const [endereco, setEndereco] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [codigoCnes, setCodigoCnes] = useState('')
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
    if ((latitude && !longitude) || (!latitude && longitude)) {
      setErro('Informe latitude e longitude juntas, ou deixe ambas em branco.')
      return
    }
    try {
      await api.post('/api/unidades', {
        nome,
        endereco: endereco || null,
        latitude: latitude ? Number(latitude) : null,
        longitude: longitude ? Number(longitude) : null,
        codigoCnes: codigoCnes || null,
      })
      setNome('')
      setEndereco('')
      setLatitude('')
      setLongitude('')
      setCodigoCnes('')
      carregar()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível salvar a unidade.')
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
        <div className="grid grid-cols-2 gap-2">
          <input
            placeholder="Latitude"
            type="number"
            step="any"
            value={latitude}
            onChange={(e) => setLatitude(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
          <input
            placeholder="Longitude"
            type="number"
            step="any"
            value={longitude}
            onChange={(e) => setLongitude(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
          />
        </div>
        <p className="text-[11px] text-gray-400">
          Opcional. Copie do Google Maps (clique com o botão direito no local → clique nas coordenadas).
        </p>
        <input
          placeholder="Código CNES (opcional)"
          value={codigoCnes}
          onChange={(e) => setCodigoCnes(e.target.value)}
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
              <th className="text-left px-4 py-3">Localização</th>
              <th className="text-left px-4 py-3">CNES</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {itens.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-6 text-center text-gray-400">
                  Nenhuma unidade cadastrada.
                </td>
              </tr>
            )}
            {itens.map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-3 text-gray-900">{u.nome}</td>
                <td className="px-4 py-3 text-gray-600">{u.endereco ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600">
                  {u.latitude != null && u.longitude != null ? (
                    <a
                      href={`https://www.google.com/maps?q=${u.latitude},${u.longitude}`}
                      target="_blank"
                      rel="noreferrer"
                      className="text-brand-navy hover:underline"
                    >
                      Ver no mapa
                    </a>
                  ) : (
                    '—'
                  )}
                </td>
                <td className="px-4 py-3 text-gray-600">{u.codigoCnes ?? '—'}</td>
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
  const [acsLista, setAcsLista] = useState<Usuario[]>([])
  const [editandoId, setEditandoId] = useState<number | null>(null)
  const [erro, setErro] = useState<string | null>(null)

  function carregar() {
    api.get<Paciente[]>('/api/pacientes').then((res) => setItens(res.data))
  }
  useEffect(carregar, [])
  useEffect(() => {
    api.get<Usuario[]>('/api/usuarios', { params: { papel: 'ACS' } }).then((res) => setAcsLista(res.data))
  }, [])

  // Gap de revisão corrigido: antes não havia NENHUMA forma de atribuir/trocar
  // o ACS responsável depois do cadastro -- um paciente cadastrado sem ACS
  // (comum quando quem cadastra é Regulador/Admin) ficava "órfão" para sempre,
  // invisível na tela "Meus Pacientes" de qualquer ACS.
  async function reatribuirAcs(paciente: Paciente, novoAcsId: string) {
    setEditandoId(paciente.id)
    setErro(null)
    try {
      await api.patch(`/api/pacientes/${paciente.id}/acs-responsavel`, {
        acsResponsavelId: novoAcsId ? Number(novoAcsId) : null,
      })
      carregar()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível atualizar o ACS responsável.')
    } finally {
      setEditandoId(null)
    }
  }

  return (
    <div className="space-y-4">
      <ImportadorCsv
        endpoint="/api/importacao/pacientes"
        colunasEsperadas="nome, cpf, cns, dataNascimento (dd/mm/aaaa), telefone, email"
        onImportado={carregar}
      />
      {erro && <p className="text-xs text-red-600">{erro}</p>}
      <div className="rounded-2xl border bg-white shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-gray-500 text-xs uppercase">
            <tr>
              <th className="text-left px-4 py-3">Nome</th>
              <th className="text-left px-4 py-3">CPF</th>
              <th className="text-left px-4 py-3">CNS</th>
              <th className="text-left px-4 py-3">Telefone</th>
              <th className="text-left px-4 py-3">ACS responsável</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {itens.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-gray-400">
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
                <td className="px-4 py-3">
                  <select
                    value={p.acsResponsavelId ?? ''}
                    disabled={editandoId === p.id}
                    onChange={(e) => reatribuirAcs(p, e.target.value)}
                    className={`rounded-lg border px-2 py-1 text-xs ${
                      p.acsResponsavelId == null ? 'border-amber-300 bg-amber-50 text-amber-800' : 'border-gray-300'
                    }`}
                  >
                    <option value="">Sem ACS responsável</option>
                    {acsLista.map((acs) => (
                      <option key={acs.id} value={acs.id}>
                        {acs.nome}
                      </option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="px-4 py-3 text-xs text-gray-400 border-t">
          Para cadastrar um único paciente com protocolo, use a tela "Novo Protocolo".
        </p>
      </div>
    </div>
  )
}

function AbaEquipe() {
  const [itens, setItens] = useState<Usuario[]>([])
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [papel, setPapel] = useState<Papel>('ACS')
  const [erro, setErro] = useState<string | null>(null)
  const [editandoId, setEditandoId] = useState<number | null>(null)

  function carregar() {
    api.get<Usuario[]>('/api/usuarios').then((res) => setItens(res.data))
  }
  useEffect(carregar, [])

  async function salvar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    if (!nome.trim() || !email.trim() || senha.trim().length < 6) {
      setErro('Preencha nome, e-mail e uma senha com ao menos 6 caracteres.')
      return
    }
    try {
      await api.post('/api/usuarios', { nome, email, senha, papel })
      setNome('')
      setEmail('')
      setSenha('')
      setPapel('ACS')
      carregar()
    } catch (err: any) {
      setErro(err?.response?.data?.mensagem ?? 'Não foi possível cadastrar o usuário.')
    }
  }

  async function alternarAtivo(usuario: Usuario) {
    setEditandoId(usuario.id)
    try {
      await api.patch(`/api/usuarios/${usuario.id}`, {
        nome: usuario.nome,
        papel: usuario.papel,
        ativo: !usuario.ativo,
        novaSenha: null,
      })
      carregar()
    } catch {
      setErro('Não foi possível atualizar o usuário.')
    } finally {
      setEditandoId(null)
    }
  }

  async function alterarPapel(usuario: Usuario, novoPapel: Papel) {
    setEditandoId(usuario.id)
    try {
      await api.patch(`/api/usuarios/${usuario.id}`, {
        nome: usuario.nome,
        papel: novoPapel,
        ativo: usuario.ativo,
        novaSenha: null,
      })
      carregar()
    } catch {
      setErro('Não foi possível atualizar o usuário.')
    } finally {
      setEditandoId(null)
    }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <form onSubmit={salvar} className="rounded-2xl border bg-white shadow-sm p-5 space-y-3 h-fit">
        <h2 className="text-sm font-semibold text-gray-700">Novo membro da equipe</h2>
        <input
          placeholder="Nome *"
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          placeholder="E-mail *"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <input
          placeholder="Senha provisória *"
          type="password"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        />
        <select
          value={papel}
          onChange={(e) => setPapel(e.target.value as Papel)}
          className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm"
        >
          {PAPEIS.map((p) => (
            <option key={p} value={p}>
              {p}
            </option>
          ))}
        </select>
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
              <th className="text-left px-4 py-3">E-mail</th>
              <th className="text-left px-4 py-3">Papel</th>
              <th className="text-left px-4 py-3">Situação</th>
            </tr>
          </thead>
          <tbody className="divide-y">
            {itens.length === 0 && (
              <tr>
                <td colSpan={4} className="px-4 py-6 text-center text-gray-400">
                  Nenhum membro de equipe cadastrado.
                </td>
              </tr>
            )}
            {itens.map((u) => (
              <tr key={u.id}>
                <td className="px-4 py-3 text-gray-900">{u.nome}</td>
                <td className="px-4 py-3 text-gray-600">{u.email}</td>
                <td className="px-4 py-3">
                  <select
                    value={u.papel}
                    disabled={editandoId === u.id}
                    onChange={(e) => alterarPapel(u, e.target.value as Papel)}
                    className="rounded-lg border border-gray-300 px-2 py-1 text-xs"
                  >
                    {PAPEIS.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <button
                    onClick={() => alternarAtivo(u)}
                    disabled={editandoId === u.id}
                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                      u.ativo ? 'bg-teal-100 text-teal-800' : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {u.ativo ? 'Ativo' : 'Inativo'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export function Cadastros() {
  const { usuario } = useAuth()
  const ehAcs = usuario?.papel === 'ACS'
  const [aba, setAba] = useState<Aba>(ehAcs ? 'pacientes' : 'procedimentos')
  const ehAdmin = usuario?.papel === 'ADMIN'

  return (
    <AdminLayout>
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Cadastros</h1>

      {/* O ACS só cadastra pacientes da sua área (item 2 do levantamento de
          requisitos) -- procedimentos, unidades e equipe são geridos pelo
          Regulador/Admin. */}
      <div className="flex gap-2 mb-6">
        {!ehAcs && (
          <>
            <TabButton ativo={aba === 'procedimentos'} onClick={() => setAba('procedimentos')}>
              Procedimentos
            </TabButton>
            <TabButton ativo={aba === 'unidades'} onClick={() => setAba('unidades')}>
              Unidades de Saúde
            </TabButton>
          </>
        )}
        <TabButton ativo={aba === 'pacientes'} onClick={() => setAba('pacientes')}>
          Pacientes
        </TabButton>
        {ehAdmin && (
          <TabButton ativo={aba === 'equipe'} onClick={() => setAba('equipe')}>
            Equipe
          </TabButton>
        )}
      </div>

      {!ehAcs && aba === 'procedimentos' && <AbaProcedimentos />}
      {!ehAcs && aba === 'unidades' && <AbaUnidades />}
      {aba === 'pacientes' && <AbaPacientes />}
      {aba === 'equipe' && ehAdmin && <AbaEquipe />}
    </AdminLayout>
  )
}
