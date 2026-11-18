import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router'
import './index.css'
import AdminRushApprovalPage from './AdminRushApprovalPage.tsx'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/admin/quotes" element={<AdminRushApprovalPage />} />
                <Route path="/*" element={<App />} />
            </Routes>
        </BrowserRouter>
    </StrictMode>,
)
