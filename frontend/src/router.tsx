import { createBrowserRouter } from 'react-router'
import AppLayout from './components/layout/AppLayout'
import AdminEditRequestsPage from './pages/AdminEditRequestsPage'
import CoursesPage from './pages/CoursesPage'
import LegalPage from './pages/LegalPage'
import MyEditRequestsPage from './pages/MyEditRequestsPage'
import OAuthCallbackPage from './pages/OAuthCallbackPage'
import ConsultPage from './pages/ConsultPage'
import CoursePage from './pages/CoursePage'
import CoursePlanPage from './pages/CoursePlanPage'
import DogFormPage from './pages/DogFormPage'
import FamilyPage from './pages/FamilyPage'
import HomePage from './pages/HomePage'
import InvitePage from './pages/InvitePage'
import LoginPage from './pages/LoginPage'
import MyPage from './pages/MyPage'
import MapPage from './pages/MapPage'
import NotFoundPage from './pages/NotFoundPage'
import NutritionPage from './pages/NutritionPage'
import OnboardingPage from './pages/OnboardingPage'
import QuestionsPage from './pages/QuestionsPage'
import RecordNewPage from './pages/RecordNewPage'
import RecordsPage from './pages/RecordsPage'
import ReportPage from './pages/ReportPage'

// 헤더·푸터가 있는 화면은 AppLayout 아래에 둡니다. (로그인·초대 수락·로그인 콜백은 레이아웃 밖)
export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/map', element: <MapPage /> },
      { path: '/course', element: <CoursePage /> },
      { path: '/course/plan', element: <CoursePlanPage /> },
      { path: '/courses', element: <CoursesPage /> },
      { path: '/consult', element: <ConsultPage /> },
      { path: '/consult/:consultId', element: <ConsultPage /> },
      { path: '/records', element: <RecordsPage /> },
      { path: '/records/new', element: <RecordNewPage /> },
      { path: '/report', element: <ReportPage /> },
      { path: '/onboarding', element: <OnboardingPage /> },
      { path: '/dogs/new', element: <DogFormPage /> },
      { path: '/dogs/:dogId/edit', element: <DogFormPage /> },
      { path: '/nutrition', element: <NutritionPage /> },
      { path: '/questions', element: <QuestionsPage /> },
      { path: '/my', element: <MyPage /> },
      { path: '/my/family', element: <FamilyPage /> },
      { path: '/my/edit-requests', element: <MyEditRequestsPage /> },
      { path: '/admin/edit-requests', element: <AdminEditRequestsPage /> },
      { path: '/terms', element: <LegalPage kind="terms" /> },
      { path: '/privacy', element: <LegalPage kind="privacy" /> },
    ],
  },
  { path: '/login', element: <LoginPage /> },
  { path: '/invite/:code', element: <InvitePage /> },
  { path: '/oauth/callback', element: <OAuthCallbackPage /> },
  { path: '*', element: <NotFoundPage /> },
])
