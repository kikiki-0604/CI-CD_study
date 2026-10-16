import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createRecord } from '../api/client'
import { Candidate, RecipeLink } from '../types'
import { ArrowLeftIcon, ArrowRightIcon, CheckIcon, ExternalLinkIcon } from '../components/icons'
import NutritionStats from '../components/NutritionStats'

type Props = {
  candidate: Candidate | null
}

export default function CandidateDetailPage({ candidate }: Props) {
  const [recorded, setRecorded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  if (!candidate) {
    return (
      <div className="app-shell">
        <div className="empty-state">
          <p>候補が選択されていません。食材入力からやり直してください。</p>
          <button type="button" className="btn btn-primary" onClick={() => navigate('/')}>
            入力画面に戻る
          </button>
        </div>
      </div>
    )
  }

  const handleOpenRecipe = async (link: RecipeLink) => {
    window.open(link.url, '_blank', 'noreferrer')
    if (recorded) return
    setError(null)
    try {
      const today = new Date().toISOString().slice(0, 10)
      await createRecord(candidate.name, link.url, today)
      setRecorded(true)
    } catch (e) {
      setError(e instanceof Error ? e.message : '記録に失敗しました')
    }
  }

  return (
    <div className="app-shell">
      <button type="button" className="btn-back" onClick={() => navigate('/candidates')}>
        <ArrowLeftIcon />
        候補一覧に戻る
      </button>

      <header className="app-header">
        <h1 className="app-title">{candidate.name}</h1>
        <p className="app-tagline">{candidate.description}</p>
      </header>

      <NutritionStats
        calories={candidate.calories}
        fat={candidate.fat}
        sugar={candidate.sugar}
        carbs={candidate.carbs}
      />
      <p className="nutrition-note">※AIによる1人前のおおよその概算値です</p>

      <section className="recipe-links-section">
        <h2 className="section-heading-sm">作り方を見る</h2>
        <p className="section-lead">気になるサイトのレシピを開いてください。開くと自動で履歴に記録されます。</p>
        <div className="recipe-link-grid">
          {candidate.recipeLinks.map((link) => (
            <button key={link.site} type="button" className="recipe-link-btn" onClick={() => handleOpenRecipe(link)}>
              <span className="recipe-link-label">
                <ExternalLinkIcon />
                {link.label}
              </span>
              <ArrowRightIcon />
            </button>
          ))}
        </div>
      </section>

      {recorded && (
        <p className="banner banner-success">
          <CheckIcon /> 履歴に記録しました。<Link to="/history">履歴を見る</Link>
        </p>
      )}
      {error && (
        <p className="banner banner-error" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
