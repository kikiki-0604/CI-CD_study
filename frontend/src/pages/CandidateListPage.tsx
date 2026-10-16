import { useNavigate } from 'react-router-dom'
import CandidateCard from '../components/CandidateCard'
import AppHeader from '../components/AppHeader'
import { Candidate } from '../types'

type Props = {
  candidates: Candidate[]
  onSelect: (candidate: Candidate) => void
}

export default function CandidateListPage({ candidates, onSelect }: Props) {
  const navigate = useNavigate()

  const handleSelect = (candidate: Candidate) => {
    onSelect(candidate)
    navigate('/candidates/detail')
  }

  if (candidates.length === 0) {
    return (
      <div className="app-shell">
        <div className="empty-state">
          <p>候補がありません。食材入力からやり直してください。</p>
          <button type="button" className="btn btn-primary" onClick={() => navigate('/')}>
            入力画面に戻る
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <AppHeader title="今日のおすすめ" tagline="気になる一品を選んでください。" />

      <div className="candidate-grid">
        {candidates.map((candidate, index) => (
          <CandidateCard key={candidate.name} candidate={candidate} rank={index + 1} onSelect={handleSelect} />
        ))}
      </div>

      <div className="submit-row">
        <button type="button" className="btn btn-outline" onClick={() => navigate('/')}>
          もう一度探す
        </button>
      </div>
    </div>
  )
}
