type FeedbackProps = {
  tipo: 'erro' | 'sucesso' | 'info'
  children: React.ReactNode
}

export default function Feedback({ tipo, children }: FeedbackProps) {
  const role = tipo === 'erro' ? 'alert' : 'status'
  return <div className={`feedback feedback-${tipo}`} role={role}>{children}</div>
}

