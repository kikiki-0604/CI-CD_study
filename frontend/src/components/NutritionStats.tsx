import { FlameIcon, DropletIcon, CubeIcon, WheatIcon } from './icons'

type Props = {
  calories: number
  fat: number
  sugar: number
  carbs: number
}

export default function NutritionStats({ calories, fat, sugar, carbs }: Props) {
  const stats = [
    { key: 'calories', label: 'カロリー', value: calories, unit: 'kcal', icon: <FlameIcon />, tone: 'tone-flame' },
    { key: 'fat', label: '脂質', value: fat, unit: 'g', icon: <DropletIcon />, tone: 'tone-droplet' },
    { key: 'sugar', label: '糖質', value: sugar, unit: 'g', icon: <CubeIcon />, tone: 'tone-cube' },
    { key: 'carbs', label: '炭水化物', value: carbs, unit: 'g', icon: <WheatIcon />, tone: 'tone-wheat' }
  ]

  return (
    <div className="stat-grid">
      {stats.map((stat) => (
        <div className="stat-tile" key={stat.key}>
          <span className={`stat-icon ${stat.tone}`}>{stat.icon}</span>
          <div className="stat-body">
            <p className="stat-label">{stat.label}</p>
            <p className="stat-value">
              {stat.value}
              <span className="stat-unit">{stat.unit}</span>
            </p>
          </div>
        </div>
      ))}
    </div>
  )
}
