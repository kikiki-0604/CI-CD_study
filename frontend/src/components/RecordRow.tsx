import { CookingRecord } from '../types'
import { ExternalLinkIcon, CalendarIcon } from './icons'

type Props = {
  record: CookingRecord
}

function formatDate(dateStr: string): string {
  const [, month, day] = dateStr.split('-')
  return `${month}/${day}`
}

function siteLabel(url: string): string {
  try {
    const host = new URL(url).hostname
    if (host.includes('cookpad')) return 'クックパッド'
    if (host.includes('kurashiru')) return 'クラシル'
    if (host.includes('rakuten')) return '楽天レシピ'
    return host.replace('www.', '')
  } catch {
    return 'レシピサイト'
  }
}

export default function RecordRow({ record }: Props) {
  return (
    <li className="history-card">
      <span className="history-date">
        <CalendarIcon />
        {formatDate(record.cookedOn)}
      </span>
      <div className="history-body">
        <p className="history-name">{record.dishName}</p>
        <span className="history-site">{siteLabel(record.recipeUrl)}</span>
      </div>
      <a href={record.recipeUrl} target="_blank" rel="noreferrer">
        <button type="button" className="btn btn-outline btn-sm">
          <ExternalLinkIcon />
          レシピを見る
        </button>
      </a>
    </li>
  )
}
