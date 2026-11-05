import { createBrowserRouter } from 'react-router'
import AppLayout from './components/layout/AppLayout'
import ConsultPage from './pages/ConsultPage'
import CoursePage from './pages/CoursePage'
import CoursePlanPage from './pages/CoursePlanPage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import MapPage from './pages/MapPage'
import NotFoundPage from './pages/NotFoundPage'
import RecordsPage from './pages/RecordsPage'

// 헤더·푸터가 있는 화면은 AppLayout 아래에 둡니다. (로그인은 레이아웃 밖)
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/map', element: <MapPage /> },
      { path: '/course', element: <CoursePage /> },
      { path: '/course/plan', element: <CoursePlanPage /> },
      { path: '/consult', element: <ConsultPage /> },
      { path: '/records', element: <RecordsPage /> },
    ],
  },
  { path: '/login', element: <LoginPage /> },
  { path: '*', element: <NotFoundPage /> },
])
