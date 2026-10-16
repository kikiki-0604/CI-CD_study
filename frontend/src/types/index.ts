export type RecipeLink = {
  site: string
  label: string
  url: string
}

export type Candidate = {
  name: string
  description: string
  calories: number
  fat: number
  sugar: number
  carbs: number
  recipeLinks: RecipeLink[]
}

export type CookingRecord = {
  id: number
  dishName: string
  recipeUrl: string
  cookedOn: string
}
