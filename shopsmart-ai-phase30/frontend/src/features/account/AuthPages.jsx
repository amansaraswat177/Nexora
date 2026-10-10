import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button, Card, ErrorBox, Field, inputClass } from '../../shell/ui.jsx'
import { useAuth } from '../../shell/AuthContext.jsx'
import { errorMessage } from '../../api/client.js'

function Shell({ title, subtitle, children }) {
  return (
    <div className="mx-auto max-w-md py-8">
      <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
      <p className="mb-6 mt-1 text-sm text-slate-500">{subtitle}</p>
      <Card>{children}</Card>
    </div>
  )
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try {
      await login(form.email, form.password)
      navigate(location.state?.from || '/', { replace: true })
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  const demo = (email, password) => setForm({ email, password })

  return (
    <Shell title="Welcome back" subtitle="Log in to see your cart, orders and saved items.">
      <form onSubmit={submit} className="space-y-4">
        <ErrorBox>{error}</ErrorBox>
        <Field label="Email"><input type="email" required autoComplete="email" className={inputClass} value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
        <Field label="Password"><input type="password" required autoComplete="current-password" className={inputClass} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>
        <Button type="submit" className="w-full" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">New here? <Link to="/register" className="font-medium text-brand-600 hover:underline">Create an account</Link></p>
      <div className="mt-6 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
        <p className="mb-2 font-medium text-slate-600">Demo accounts (click to fill)</p>
        <div className="flex flex-wrap gap-2">
          {[['Basic', 'basic@shopsmart.test', 'Demo@1234'], ['Prime', 'prime@shopsmart.test', 'Demo@1234'], ['Premium', 'premium@shopsmart.test', 'Demo@1234'], ['Staff', 'admin@shopsmart.test', 'Admin@123']].map(([n, e, p]) => (
            <button key={n} type="button" onClick={() => demo(e, p)} className="rounded-full bg-white px-3 py-1 ring-1 ring-slate-300 hover:bg-brand-50">{n}</button>
          ))}
        </div>
      </div>
    </Shell>
  )
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true); setError('')
    try { await register(form); navigate('/', { replace: true }) } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  return (
    <Shell title="Create your account" subtitle="Start as a Basic member. Prime and Premium add free shipping and more.">
      <form onSubmit={submit} className="space-y-4">
        <ErrorBox>{error}</ErrorBox>
        <div className="grid grid-cols-2 gap-3">
          <Field label="First name"><input required className={inputClass} value={form.firstName} onChange={set('firstName')} autoComplete="given-name" /></Field>
          <Field label="Last name"><input className={inputClass} value={form.lastName} onChange={set('lastName')} autoComplete="family-name" /></Field>
        </div>
        <Field label="Email"><input type="email" required className={inputClass} value={form.email} onChange={set('email')} autoComplete="email" /></Field>
        <Field label="Password" hint="At least 8 characters"><input type="password" required minLength={8} className={inputClass} value={form.password} onChange={set('password')} autoComplete="new-password" /></Field>
        <Button type="submit" className="w-full" disabled={busy}>{busy ? 'Creating…' : 'Create account'}</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-500">Already registered? <Link to="/login" className="font-medium text-brand-600 hover:underline">Log in</Link></p>
    </Shell>
  )
}
