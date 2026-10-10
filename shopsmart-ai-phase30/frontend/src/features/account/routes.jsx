import { LoginPage, RegisterPage } from './AuthPages.jsx'
import ProfilePage from './ProfilePage.jsx'

export default [
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/profile', element: <ProfilePage />, auth: true },
]

export const nav = []
