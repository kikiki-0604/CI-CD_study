import { useState } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import IngredientInputPage from './pages/IngredientInputPage'
import CandidateListPage from './pages/CandidateListPage'
import CandidateDetailPage from './pages/CandidateDetailPage'
import HistoryPage from './pages/HistoryPage'
import { Candidate } from './types'

export default function App() {
  const [candidates, setCandidates] = useState<Candidate[]>([])
  const [selectedCandidate, setSelectedCandidate] = useState<Candidate | null>(null)

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<IngredientInputPage onSuggestions={setCandidates} />} />
        <Route
          path="/candidates"
          element={<CandidateListPage candidates={candidates} onSelect={setSelectedCandidate} />}
        />
        <Route path="/candidates/detail" element={<CandidateDetailPage candidate={selectedCandidate} />} />
        <Route path="/history" element={<HistoryPage />} />
      </Routes>
    </BrowserRouter>
  )
}
