import { Candidate } from '../types'
import { ArrowRightIcon, FlameIcon, DropletIcon, CubeIcon, WheatIcon } from './icons'

type Props = {
  candidate: Candidate
  rank: number
  onSelect: (candidate: Candidate) => void
}

const RANK_CLASS: Record<number, string> = {
  1: 'rank-badge-gold',
  2: 'rank-badge-silver',
  3: 'rank-badge-bronze'
}

export default function CandidateCard({ candidate, rank, onSelect }: Props) {
  return (
    <article className="candidate-card" style={{ animationDelay: `${rank * 60}ms` }}>
      <span className={`rank-badge ${RANK_CLASS[rank] ?? ''}`}>No.{rank}</span>
      <h3 className="candidate-name">{candidate.name}</h3>
      <p className="candidate-description">{candidate.description}</p>

      <div className="mini-nutrition-row">
        <span className="mini-nutrition tone-flame">
          <FlameIcon />
          {candidate.calories}kcal
        </span>
        <span className="mini-nutrition tone-droplet">
          <DropletIcon />
          {candidate.fat}g
        </span>
        <span className="mini-nutrition tone-cube">
          <CubeIcon />
          {candidate.sugar}g
        </span>
        <span className="mini-nutrition tone-wheat">
          <WheatIcon />
          {candidate.carbs}g
        </span>
      </div>

      <div className="candidate-actions">
        <button type="button" className="btn btn-primary btn-sm" onClick={() => onSelect(candidate)}>
          これに決める
          <ArrowRightIcon />
        </button>
      </div>
    </article>
  )
}
