import { useState } from 'react'
import { PlusIcon, SearchIcon } from './icons'

type Props = {
  ingredients: string[]
  onChange: (ingredients: string[]) => void
}

const QUICK_INGREDIENTS = ['卵', '玉ねぎ', '豚こま', '鶏むね肉', 'キャベツ', 'にんじん', 'じゃがいも', '豆腐', 'もやし', 'ねぎ']

export default function IngredientTagInput({ ingredients, onChange }: Props) {
  const [text, setText] = useState('')

  const addIngredient = (value?: string) => {
    const trimmed = (value ?? text).trim()
    if (trimmed.length === 0) return
    if (ingredients.includes(trimmed)) {
      setText('')
      return
    }
    onChange([...ingredients, trimmed])
    setText('')
  }

  const removeIngredient = (index: number) => {
    onChange(ingredients.filter((_, i) => i !== index))
  }

  return (
    <div>
      <div className="ingredient-row">
        <div className="ingredient-field-wrap">
          <SearchIcon />
          <input
            className="ingredient-field"
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault()
                addIngredient()
              }
            }}
            placeholder="例: 卵、玉ねぎ、豚こま"
            aria-label="食材を入力"
          />
        </div>
        <button type="button" className="btn btn-primary" onClick={() => addIngredient()}>
          <PlusIcon />
          追加
        </button>
      </div>

      <div className="quick-chip-row">
        <span className="quick-chip-label">よく使う食材:</span>
        {QUICK_INGREDIENTS.filter((item) => !ingredients.includes(item)).map((item) => (
          <button type="button" key={item} className="quick-chip" onClick={() => addIngredient(item)}>
            <PlusIcon />
            {item}
          </button>
        ))}
      </div>

      {ingredients.length > 0 && (
        <div className="tag-list">
          {ingredients.map((ingredient, index) => (
            <span className="tag-chip" key={`${ingredient}-${index}`}>
              {ingredient}
              <button
                type="button"
                className="tag-chip-remove"
                onClick={() => removeIngredient(index)}
                aria-label={`${ingredient}を削除`}
              >
                ×
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  )
}
