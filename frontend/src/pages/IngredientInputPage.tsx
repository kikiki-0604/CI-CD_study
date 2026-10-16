import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import IngredientTagInput from '../components/IngredientTagInput'
import AppHeader from '../components/AppHeader'
import { fetchSuggestions } from '../api/client'
import { Candidate } from '../types'
import { SparkleIcon, ChefHatIcon } from '../components/icons'

type Props = {
  onSuggestions: (candidates: Candidate[]) => void
}

export default function IngredientInputPage({ onSuggestions }: Props) {
  const [ingredients, setIngredients] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleSubmit = async () => {
    if (ingredients.length === 0) {
      setError('食材を1つ以上入力してください')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const { candidates } = await fetchSuggestions(ingredients)
      onSuggestions(candidates)
      navigate('/candidates')
    } catch (e) {
      setError(e instanceof Error ? e.message : '候補の取得に失敗しました')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app-shell">
      <AppHeader title="今日ある食材は?" tagline="冷蔵庫の中身から、AIが今日の一品を提案します。" />

      <section className="hero-card">
        <div className="hero-glow" aria-hidden="true" />
        <span className="hero-kicker">
          <SparkleIcon />
          AIが献立を5件提案
        </span>
        <p className="section-lead">思いついたものから入力してください。Enterでも追加できます。</p>
        <IngredientTagInput ingredients={ingredients} onChange={setIngredients} />

        <div className="submit-row">
          <button type="button" className="btn btn-cta" onClick={handleSubmit} disabled={loading}>
            {loading ? (
              <>
                <span className="spinner spinner-light" />
                考え中...
              </>
            ) : (
              <>
                <SparkleIcon />
                候補を探す
              </>
            )}
          </button>
        </div>

        {loading && (
          <div className="ai-thinking">
            <span className="ai-thinking-icon">
              <ChefHatIcon />
            </span>
            AIが今日の献立を考えています
            <span className="thinking-dots">
              <span />
              <span />
              <span />
            </span>
          </div>
        )}

        {error && <p className="banner banner-error" role="alert">{error}</p>}
      </section>
    </div>
  )
}
