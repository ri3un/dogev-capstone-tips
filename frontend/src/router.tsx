import { createBrowserRouter } from 'react-router'
import HomePage from './pages/HomePage'
import NotFoundPage from './pages/NotFoundPage'

// 화면을 만들 때 여기에 경로를 추가합니다. (Login, Map, Course, RecordInput, Consult, Report …)
export const router = createBrowserRouter([
  { path: '/', element: <HomePage /> },
  { path: '*', element: <NotFoundPage /> },
])
