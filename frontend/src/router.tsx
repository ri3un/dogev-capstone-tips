import { createBrowserRouter } from 'react-router'
import AppLayout from './components/layout/AppLayout'
import ConsultPage from './pages/ConsultPage'
import CoursePage from './pages/CoursePage'
import CoursePlanPage from './pages/CoursePlanPage'
import DogFormPage from './pages/DogFormPage'
import FamilyPage from './pages/FamilyPage'
import HomePage from './pages/HomePage'
import InvitePage from './pages/InvitePage'
import LoginPage from './pages/LoginPage'
import MapPage from './pages/MapPage'
import NotFoundPage from './pages/NotFoundPage'
import OnboardingPage from './pages/OnboardingPage'
import RecordNewPage from './pages/RecordNewPage'
import RecordsPage from './pages/RecordsPage'
import ReportPage from './pages/ReportPage'

// 헤더·푸터가 있는 화면은 AppLayout 아래에 둡니다. (로그인·초대 수락은 레이아웃 밖)
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
      { path: '/records/new', element: <RecordNewPage /> },
      { path: '/report', element: <ReportPage /> },
      { path: '/onboarding', element: <OnboardingPage /> },
      { path: '/dogs/new', element: <DogFormPage /> },
      { path: '/dogs/:dogId/edit', element: <DogFormPage /> },
      { path: '/my/family', element: <FamilyPage /> },
    ],
  },
  { path: '/login', element: <LoginPage /> },
  { path: '/invite/:code', element: <InvitePage /> },
  { path: '*', element: <NotFoundPage /> },
])
